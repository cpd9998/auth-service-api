package com.cpd.hotel_system.auth_service_api.api;

import com.cpd.hotel_system.auth_service_api.config.JwtService;
import com.cpd.hotel_system.auth_service_api.dto.request.SystemUserRequestDto;
import com.cpd.hotel_system.auth_service_api.service.SystemUserService;
import com.cpd.hotel_system.auth_service_api.util.StandardResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

@RestController
@RequestMapping("/user-service/api/v1/users")
@RequiredArgsConstructor
public class UserController {
    private final SystemUserService systemUserService;
    private final JwtService jwtService;

    @PostMapping("/visitors/signup")
    public ResponseEntity<StandardResponseDto> createUser(@RequestBody SystemUserRequestDto dto) throws IOException {

        systemUserService.createUser(dto);
        return new ResponseEntity<>(new StandardResponseDto(201,"user account was created",null), HttpStatus.CREATED);

    }

}
