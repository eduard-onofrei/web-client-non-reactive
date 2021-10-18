package uk.santander;

import feign.Logger;
import feign.Retryer;
import feign.slf4j.Slf4jLogger;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class FeignConfig {

    @Bean
    public Slf4jLogger logger() {
        return new Slf4jLogger();
    }

    @Bean
    public Logger.Level feingLoggerLevel() {
        return Logger.Level.FULL;
    }

    @Bean
    public Retryer retryer() {
        return new Retryer.Default(1000, 60000, 10);
    }
}