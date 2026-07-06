package com.cuscatlan.coworking;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

// excluyo el UserDetailsService por defecto: la auth la maneja el filtro JWT, no
// quiero el usuario "user" con password generado que loguea Spring al arrancar
@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
@ConfigurationPropertiesScan
public class CoworkingApplication {

    public static void main(String[] args) {
        SpringApplication.run(CoworkingApplication.class, args);
    }
}
