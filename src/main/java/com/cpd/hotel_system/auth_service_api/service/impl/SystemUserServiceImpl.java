package com.cpd.hotel_system.auth_service_api.service.impl;

import com.cpd.hotel_system.auth_service_api.entity.Otp;
import com.cpd.hotel_system.auth_service_api.exception.BadRequestException;
import com.cpd.hotel_system.auth_service_api.config.KeycloakSecurityUtil;
import com.cpd.hotel_system.auth_service_api.dto.request.SystemUserRequestDto;
import com.cpd.hotel_system.auth_service_api.entity.SystemUser;
import com.cpd.hotel_system.auth_service_api.exception.DuplicateEntryException;
import com.cpd.hotel_system.auth_service_api.exception.EntryNotFoundException;
import com.cpd.hotel_system.auth_service_api.repo.OtpRepo;
import com.cpd.hotel_system.auth_service_api.repo.SystemUserRepo;
import com.cpd.hotel_system.auth_service_api.service.EmailService;
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

import java.io.IOException;
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
    private final EmailService emailService;



    @Override
    public void createUser(SystemUserRequestDto dto) throws IOException {
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

        UserRepresentation userRepresentation = mapUserRepo(dto,false,false);
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
            emailService.sendUserSignupVerficationCode(dto.getEmail(),"Verify your email",creatdOtp.getCode(),dto.getFirstName());
        }


    }

    @Override
    public void initilizeHosts(List<SystemUserRequestDto> users) throws IOException {
      for(SystemUserRequestDto dto : users){
          Optional<SystemUser> selectedUser = systemUserRepo.findByEmail(dto.getEmail());

          if(selectedUser.isPresent()){
              continue;
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

          UserRepresentation userRepresentation = mapUserRepo(dto,true,true);
          Response response = keycloak.realm(realm).users().create(userRepresentation);
          if(response.getStatus() == Response.Status.CREATED.getStatusCode()){
              RoleRepresentation userRole = keycloak.realm(realm).roles().get("host").toRepresentation();
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
                      .isActive(true)
                      .isAccountNonExpired(true)
                      .isAccountNonLocked(true)
                      .isCredentialsNonExpired(true)
                      .isEnabled(true)
                      .isEmailVerified(true)
                      .createdAt(new Date().toInstant())
                      .updatedAt(new Date().toInstant())
                      .build();

              SystemUser saveUser = systemUserRepo.save(sUser);
              emailService.sendHostPassword(dto.getEmail(),"access system by using above password", dto.getPassword(), dto.getFirstName());
          }

      }
    }

    @Override
    public void resend(String email,String type) {
        try{
            Optional<SystemUser> selectedUser = systemUserRepo.findByEmail(email);
            if(selectedUser.isEmpty()){
                throw new EntryNotFoundException("Unable to find any user associated with this email ");
            }
            SystemUser systemUser = selectedUser.get();
            if(type.equalsIgnoreCase("SIGNUP")){

                if(systemUser.isEmailVerified()){
                    throw new DuplicateEntryException("This email is already activated");
                }
            }

            Otp selectedOtpobj = systemUser.getOtp();
            if(selectedOtpobj.getAttempts() >=5){
                String code =otpGenerator.generateOtp(5);
                emailService.sendUserSignupVerficationCode(systemUser.getEmail(),"Verify your email",code,systemUser.getFirstName());
                selectedOtpobj.setAttempts(0);
                selectedOtpobj.setCode(code);
                selectedOtpobj.setIsVerified(false);
                selectedOtpobj.setUpdatedAt(new Date().toInstant());
                otpRepo.save(selectedOtpobj);

            }


        }catch (Exception e){
        throw new RuntimeException(e.getMessage());
        }
    }

    @Override
    public void forgotPasswordSendVerificationCode(String email) {
        try{
            Optional<SystemUser> selectedUser = systemUserRepo.findByEmail(email);
            if(selectedUser.isEmpty()){
                throw new EntryNotFoundException("Unable to find any user associated with this email ");
            }
            SystemUser systemUser = selectedUser.get();
            Keycloak keycloak = null;
            keycloak = keycloakUtil.getKeycloakInstance();
            UserRepresentation exsistingUser = keycloak.realm(realm).users().search(email).stream().findFirst().orElse(null);
            if(exsistingUser == null){
                throw new EntryNotFoundException("Unable to find any user associated with this email ");
            }

            Otp selectedOtpobj = systemUser.getOtp();
            if(selectedOtpobj.getAttempts() >=5){
                String code =otpGenerator.generateOtp(5);
                selectedOtpobj.setAttempts(0);
                selectedOtpobj.setCode(code);
                selectedOtpobj.setIsVerified(false);
                selectedOtpobj.setUpdatedAt(new Date().toInstant());
                otpRepo.save(selectedOtpobj);
                emailService.sendUserSignupVerficationCode(systemUser.getEmail(),"Verify your email",code,systemUser.getFirstName());

            }


        }catch (Exception e){
            throw new RuntimeException(e.getMessage());
        }
    }

    @Override
    public boolean verifyRest(String otp, String email) {

        try{
            Optional<SystemUser> selectedUser = systemUserRepo.findByEmail(email);
            if(selectedUser.isEmpty()){
                throw new EntryNotFoundException("Unable to find any user associated with this email ");
            }
            SystemUser systemUserOb = selectedUser.get();
            Otp otpOb = systemUserOb.getOtp();
            if(otpOb.getCode().equals(otp)){
                otpRepo.deleteById(otpOb.getPropertyId());
                return true;
            }else{
                if(otpOb.getAttempts() >=5){
                    resend(email,"PASSWORD");
                    throw new BadRequestException("you have a new verification code ");

                }
                otpOb.setAttempts(otpOb.getAttempts() + 1);
                otpOb.setUpdatedAt(new Date().toInstant());
                otpRepo.save(otpOb);
                return false;

            }
        }catch (Exception e){
            return false;
        }

    }

    private  UserRepresentation mapUserRepo(SystemUserRequestDto dto,boolean isEmailVerified,boolean isEnabled ){
        UserRepresentation user = new UserRepresentation();
        user.setEmail(dto.getEmail());
        user.setFirstName(dto.getFirstName());
        user.setLastName(dto.getLastName());
        user.setUsername(dto.getEmail());
        user.setEnabled(isEnabled);
        user.setEmailVerified(isEmailVerified);
        List<CredentialRepresentation> credList = new ArrayList<>();
        CredentialRepresentation cred = new CredentialRepresentation();
        cred.setTemporary(false);
        cred.setValue(dto.getPassword());
        credList.add(cred);
        user.setCredentials(credList);
        return user;

    }
}
