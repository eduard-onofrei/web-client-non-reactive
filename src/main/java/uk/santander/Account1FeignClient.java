package uk.santander;

import org.springframework.cloud.openfeign.FeignClient;

@FeignClient(
        value = "Account1FeignClient",
        url = "http://localhost:8080",
        configuration = FeignConfig.class
)
public interface Account1FeignClient extends AccountFeignClient {

}