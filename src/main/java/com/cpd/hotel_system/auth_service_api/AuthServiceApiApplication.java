package com.cpd.hotel_system.auth_service_api;

import com.cpd.hotel_system.auth_service_api.dto.request.SystemUserRequestDto;
import com.cpd.hotel_system.auth_service_api.service.SystemUserService;
import com.cpd.hotel_system.auth_service_api.util.PasswordGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

import java.util.Arrays;

@SpringBootApplication
@EnableDiscoveryClient
@RequiredArgsConstructor
public class AuthServiceApiApplication implements CommandLineRunner {
    private final SystemUserService service;
	private final PasswordGenerator generator;
	public static void main(String[] args) {
		SpringApplication.run(AuthServiceApiApplication.class, args);
	}

	@Override
	public void run(String... args) throws Exception {
		SystemUserRequestDto user1 = new SystemUserRequestDto("Abc","Xyz","abc@gmail.com",generator.generatePassword(),"0778336188");
		SystemUserRequestDto user2 = new SystemUserRequestDto("STY","WTY","sty@gmail.com",generator.generatePassword(),"0778626994");
		service.initilizeHosts(Arrays.asList(user1,user2));

	}
}
