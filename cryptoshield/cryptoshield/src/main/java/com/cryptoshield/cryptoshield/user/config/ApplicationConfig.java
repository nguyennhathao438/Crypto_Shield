package com.cryptoshield.cryptoshield.user.config;

import com.cryptoshield.cryptoshield.user.entity.User;
import com.cryptoshield.cryptoshield.user.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
@Slf4j
public class ApplicationConfig {
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Bean
    ApplicationRunner applicationRunner(UserRepository userRepository){
        return args -> {
            if(userRepository.findByEmail("admin@gmail.com").isEmpty()){
                User user = User.builder()
                        .email("admin@gmail.com")
                        .password(passwordEncoder.encode("123456"))
                        .username("admin")
                        .build();
                userRepository.save(user);
                log.warn("Create user success with account:admin@gmail.com password:123456");
            }
        };
    }
}
