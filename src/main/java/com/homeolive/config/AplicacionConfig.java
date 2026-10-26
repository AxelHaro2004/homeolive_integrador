package com.homeolive.config;

import com.homeolive.modelo.Zona;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Clock;

@Configuration
public class AplicacionConfig {

    @Bean
    public Clock reloj() {
        return Clock.system(Zona.LIMA);
    }

    @Bean
    public PasswordEncoder codificadorClaves() {
        return new BCryptPasswordEncoder();
    }
}
