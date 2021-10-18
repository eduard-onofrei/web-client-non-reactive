package uk.santander;

import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableFeignClients(clients = {Account1FeignClient.class, Account2FeignClient.class})
public class FeignClientsConfig {
}
