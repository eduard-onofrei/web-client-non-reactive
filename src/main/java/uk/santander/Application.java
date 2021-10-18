package uk.santander;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.http.ResponseEntity;

import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;
import java.util.stream.Stream;

@Slf4j
@SpringBootApplication
public class Application implements CommandLineRunner {

    private static final int NUMBER_OF_THREADS = 2000;

    @Autowired
    private Account1FeignClient account1FeignClient;

    @Autowired
    private Account2FeignClient account2FeignClient;


    private ConcurrentMap<String, Object> createdAccounts = new ConcurrentHashMap<>();

    public static void main(String[] args) {
        SpringApplication application = new SpringApplication(Application.class);
        ConfigurableApplicationContext context = application.run(args);
        context.close();
    }

    @Override
    public void run(String... args) throws Exception {
        long t0 = System.currentTimeMillis();
        initialLoad();
        log.info("Initial load duration: {}", System.currentTimeMillis()-t0);
        process(account1FeignClient);
        process(account2FeignClient);
        log.info("Tiempo total de ejecución: {}", System.currentTimeMillis()- t0);
    }

    private void initialLoad() throws InterruptedException {
        ExecutorService executor = Executors.newFixedThreadPool(2*NUMBER_OF_THREADS);

        Stream.concat(createCallers(account1FeignClient, "Pepe"), createCallers(account2FeignClient, "Juan"))
            .forEach(executor::submit);
        executor.shutdown();
        final boolean allRequestsFinished = executor.awaitTermination(90, TimeUnit.SECONDS);
        log.info("AllRequestsFinished: {}", allRequestsFinished);
    }

    private Stream<Runnable> createCallers(AccountFeignClient feignClient, String ownerName) {
        return IntStream.range(0, NUMBER_OF_THREADS)
                .mapToObj(n -> () -> {
                    try {
                        final String randomOwnerName = ownerName + getRandom();
                        feignClient.create(Account.builder().owner(randomOwnerName).value((double) n).build());
                        createdAccounts.put(randomOwnerName, new Object());
                    }catch(Exception e){
                        log.error("Error al crear account", e);
                    }
                });
    }

    private double getRandom() {
        Random rn = new Random();
        return rn.nextInt(5) + 1;
    }

    private void process(AccountFeignClient feignClient) {
        createdAccounts.keySet().stream()
                .parallel()
                .forEach(owner ->
                        feignClient.get(owner).getBody()
                        .forEach(account -> feignClient.create(account.toBuilder().owner(account.getOwner() + "new"+feignClient.getClass().toString()).build())));

    }
}
