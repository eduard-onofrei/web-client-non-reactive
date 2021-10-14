package uk.santander;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Clock;

import static java.time.ZoneId.of;

@Configuration
public class AppConfig {

    @Bean
    public WebClient webClient() {
        return WebClient.create();
    }
}
