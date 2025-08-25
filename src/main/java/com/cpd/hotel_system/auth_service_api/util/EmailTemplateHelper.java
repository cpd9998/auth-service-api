package com.cpd.hotel_system.auth_service_api.util;

import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class EmailTemplateHelper {
    public String loadHtmlTemplate(String templateName) throws IOException {
         try {
            ClassPathResource resource = new ClassPathResource(templateName);
            byte[] fileData = resource.getInputStream().readAllBytes();
             return new String(fileData, StandardCharsets.UTF_8);

        }catch (IOException e){
          e.printStackTrace();
          return "";
         }
          }
}
