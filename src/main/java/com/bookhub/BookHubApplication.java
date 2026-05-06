package com.bookhub;

import com.bookhub.config.AdminSeedProperties;
import com.bookhub.config.CorsProperties;
import com.bookhub.config.JwtProperties;
import com.bookhub.config.StorageProperties;
import com.bookhub.config.TelegramBotProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(exclude = {UserDetailsServiceAutoConfiguration.class})
@EnableScheduling
@EnableConfigurationProperties({
        JwtProperties.class,
        StorageProperties.class,
        AdminSeedProperties.class,
        CorsProperties.class,
        TelegramBotProperties.class
})
public class BookHubApplication {
    public static void main(String[] args) {
        SpringApplication.run(BookHubApplication.class, args);
    }
}
