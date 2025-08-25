package com.cpd.hotel_system.auth_service_api.service.impl;

import com.cpd.hotel_system.auth_service_api.entity.Otp;
import com.cpd.hotel_system.auth_service_api.exception.BadRequestException;
import com.cpd.hotel_system.auth_service_api.config.KeycloakSecurityUtil;
import com.cpd.hotel_system.auth_service_api.dto.request.SystemUserRequestDto;
import com.cpd.hotel_system.auth_service_api.entity.SystemUser;
import com.cpd.hotel_system.auth_service_api.exception.DuplicateEntryException;
import com.cpd.hotel_system.auth_service_api.repo.OtpRepo;
import com.cpd.hotel_system.auth_service_api.repo.SystemUserRepo;
import com.cpd.hotel_system.auth_service_api.service.SystemUserService;
import com.cpd.hotel_system.auth_service_api.util.OtpGenerator;
import jakarta.ws.rs.core.Response;
import lombok.RequiredArgsConstructor;
import org.keycloak.admin.client.Keycloak;

import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;

@Service
@RequiredArgsConstructor
public class SystemUserServiceImpl implements SystemUserService {

    @Value("${keycloak.config.realm}")
    private String realm;

    private final SystemUserRepo systemUserRepo;
    private final OtpRepo    otpRepo;
    private final KeycloakSecurityUtil keycloakUtil;
    private final OtpGenerator otpGenerator;



    @Override
    public void createUser(SystemUserRequestDto dto) {
      if (dto.getFirstName() == null || dto.getFirstName().trim().isEmpty()){
          throw  new BadRequestException("First name is required");
      }

        if (dto.getLastName() == null || dto.getLastName().trim().isEmpty()){
            throw  new BadRequestException("Last name is required");
        }

        if(dto.getEmail() == null || dto.getEmail().trim().isEmpty()){
            throw  new BadRequestException("Email is required");
        }

        String userId = "";
        String otp = "";
        Keycloak keycloak =  null;

        UserRepresentation exsistingUser = null;
        keycloak = keycloakUtil.getKeycloakInstance();

        exsistingUser =keycloak.realm(realm).users().search(dto.getEmail()).stream()
                .findFirst().orElse(null);

        if(exsistingUser != null){
            Optional<SystemUser> selectedSystemUserFromAuthService = systemUserRepo.findByEmail(dto.getEmail());

            if(selectedSystemUserFromAuthService.isEmpty()){
              keycloak.realm(realm).users().delete(exsistingUser.getId());
            }else{
                throw new DuplicateEntryException("Email already exists");
            }
        }else{
            Optional<SystemUser> selectedSystemUserFromAuthService = systemUserRepo.findByEmail(dto.getEmail());
            if(selectedSystemUserFromAuthService.isPresent()){
                Optional<Otp> selectedOtp = otpRepo.findBySystemUserId(selectedSystemUserFromAuthService.get().getUserId());

                if(selectedOtp.isPresent()){
                    // remove otp
               otpRepo.deleteById(selectedOtp.get().getPropertyId());
                }

                systemUserRepo.deleteById(selectedSystemUserFromAuthService.get().getUserId());

            }
        }


        //

        UserRepresentation userRepresentation = mapUserRepo(dto);
        Response response = keycloak.realm(realm).users().create(userRepresentation);
        if(response.getStatus() == Response.Status.CREATED.getStatusCode()){
            RoleRepresentation userRole = keycloak.realm(realm).roles().get("user").toRepresentation();
            userId =  response.getLocation().getPath().replaceAll(".*/([^/])+$","$1");
            keycloak.realm(realm).users().get(userId).roles().realmLevel().add(Arrays.asList(userRole));
            UserRepresentation createdUser = keycloak.realm(realm).users().get(userId).toRepresentation();
            SystemUser sUser = SystemUser.builder()
                    .userId(userId)
                    .keycloakId(createdUser.getId())
                    .firstName(dto.getFirstName())
                    .lastName(dto.getLastName())
                    .email(dto.getEmail())
                    .contact(dto.getContact())
                    .isActive(false)
                    .isAccountNonExpired(true)
                    .isAccountNonLocked(true)
                    .isCredentialsNonExpired(true)
                    .isEnabled(false)
                    .isEmailVerified(false)
                    .createdAt(new Date().toInstant())
                    .updatedAt(new Date().toInstant())
                    .build();

            SystemUser saveUser = systemUserRepo.save(sUser);
            Otp creatdOtp = Otp.builder()
                    .propertyId(UUID.randomUUID().toString())
                    .code(otpGenerator.generateOtp(5))
                    .createdAt(Instant.now())
                    .updatedAt(Instant.now())
                    .isVerified(false)
                    .attempts(0)
                    .build();

            otpRepo.save(creatdOtp);
            // send email



        }


    }
    private  UserRepresentation mapUserRepo(SystemUserRequestDto dto ){
        UserRepresentation user = new UserRepresentation();
        user.setEmail(dto.getEmail());
        user.setFirstName(dto.getFirstName());
        user.setLastName(dto.getLastName());
        user.setUsername(dto.getEmail());
        user.setEnabled(false);
        user.setEmailVerified(false);
        List<CredentialRepresentation> credList = new ArrayList<>();
        CredentialRepresentation cred = new CredentialRepresentation();
        cred.setTemporary(false);
        cred.setValue(dto.getPassword());
        credList.add(cred);
        user.setCredentials(credList);
        return user;

    }
}
