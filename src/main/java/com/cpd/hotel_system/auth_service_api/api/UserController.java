package com.cpd.hotel_system.auth_service_api.api;

import com.cpd.hotel_system.auth_service_api.config.JwtService;
import com.cpd.hotel_system.auth_service_api.dto.request.PasswordRequestDto;
import com.cpd.hotel_system.auth_service_api.dto.request.SystemUserRequestDto;
import com.cpd.hotel_system.auth_service_api.service.SystemUserService;
import com.cpd.hotel_system.auth_service_api.util.StandardResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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

    @PostMapping("/visitors/resend")
    public ResponseEntity<StandardResponseDto> resend(@RequestParam String email,@RequestParam String type) throws IOException {

        systemUserService.resend(email,type);
        return new ResponseEntity<>(new StandardResponseDto(200,"please check your email",null), HttpStatus.OK);

    }

    @PostMapping("/visitors/forgot-password-request-code")
    public ResponseEntity<StandardResponseDto> forgotPasswordRequest(@RequestParam String email) throws IOException {

        systemUserService.forgotPasswordSendVerificationCode(email);
        return new ResponseEntity<>(new StandardResponseDto(200,"please check your email",null), HttpStatus.OK);

    }


    @PostMapping("/visitors/verify-reset")
    public ResponseEntity<StandardResponseDto> verifyReset(@RequestParam String email,@RequestParam String otp) throws IOException {

        boolean isVerified = systemUserService.verifyReset(otp,email);
            return new ResponseEntity<>(new StandardResponseDto(isVerified?200:400,isVerified?"verified":"try again",isVerified), isVerified?HttpStatus.OK:HttpStatus.BAD_REQUEST);
    }

    @PostMapping("/visitors/reset-password")
    public ResponseEntity<StandardResponseDto> resetPassword(@RequestBody PasswordRequestDto dto) throws IOException {

        boolean isChanged = systemUserService.passwordReset(dto);
        return new ResponseEntity<>(new StandardResponseDto(isChanged?201:400,isChanged?"password changed":"try again",isChanged), isChanged?HttpStatus.CREATED:HttpStatus.BAD_REQUEST);
    }

}
