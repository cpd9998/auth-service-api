package com.cpd.hotel_system.auth_service_api.util;

import org.springframework.stereotype.Component;

import java.util.Random;

@Component
public class PasswordGenerator {
    private static  final String UPPERCASE="ABCDEFGHIJKLMNOPKRST";
    private static  final String LOWERCASE="abcdeefghijklm";
    private static  final String DIGIT="0123456789";
    private static  final String SPECIAL_CHARS="!@$#%&";

    private static final String ALL_CHARS =  UPPERCASE + LOWERCASE + DIGIT + SPECIAL_CHARS;

    public String generatePassword(){
        StringBuilder password = new StringBuilder(6);
        Random random = new Random();
        password.append(UPPERCASE.charAt(random.nextInt(UPPERCASE.length())));
        password.append(LOWERCASE.charAt(random.nextInt(LOWERCASE.length())));
        password.append(DIGIT.charAt(random.nextInt(DIGIT.length())));
        password.append(SPECIAL_CHARS.charAt(random.nextInt(SPECIAL_CHARS.length())));
        for (int i = 0; i < 6; i++) {
            password.append(ALL_CHARS.charAt(random.nextInt(SPECIAL_CHARS.length())));
        }
        return shuffleString(password.toString(),random);

    }

    private String shuffleString(String input,Random rand){
        char[] chars = input.toCharArray();
        for (int i = chars.length -1; i>0; i--) {
            int j = rand.nextInt(i+1);
            char temp = chars[i];
            chars[i] = chars[j];
            chars[j] = temp;
        }

        return new String(chars);
    }

}