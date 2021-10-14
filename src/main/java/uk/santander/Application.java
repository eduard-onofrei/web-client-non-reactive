package uk.santander;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

@Slf4j
@SpringBootApplication
public class Application implements CommandLineRunner {

    private static final String SERVICE1 = "http://localhost:8080";
    private static final String SERVICE2 = "http://localhost:8085";
    private static final int NUMBER_OF_THREADS = 100;

    @Autowired
    private WebClient webClient;

    private ConcurrentMap<String, ResponseEntity<Account>> createdAccounts = new ConcurrentHashMap<>();

    public static void main(String[] args) {
        SpringApplication application = new SpringApplication(Application.class);
        ConfigurableApplicationContext context = application.run(args);
        context.close();
    }

    @Override
    public void run(String... args) throws Exception {
        long t0 = System.currentTimeMillis();
        initialLoad();
        log.info("Created {}", createdAccounts);
        log.info("Initial load duration: {}", System.currentTimeMillis()-t0);
        process();
        log.info("Tiempo total de ejecución: {}", System.currentTimeMillis()- t0);
    }

    private void initialLoad() throws InterruptedException {
        ExecutorService executor = Executors.newFixedThreadPool(2*NUMBER_OF_THREADS);

        Stream.concat(createCallers(SERVICE1, "Pepe"), createCallers(SERVICE2, "Juan"))
            .forEach(task -> {
                executor.submit(task);
                executor.shutdown();
            });
        final boolean allRequestsFinished = executor.awaitTermination(30, TimeUnit.SECONDS);
        log.info("AllRequestsFinished: {}", allRequestsFinished);
    }

    private Stream<Runnable> createCallers(String host, String ownerName) {
        return IntStream.range(0, NUMBER_OF_THREADS).parallel()
                .mapToObj(n -> () -> {
                    final ResponseEntity<Account> accountResponseEntity = webClient.post()
                            .uri(host)
                            .bodyValue(Account.builder().owner(ownerName + getRandom()).value((double) n).build())
                            .retrieve()
                            .toEntity(Account.class)
                            .map(peekedAccount -> {
                                log.info(peekedAccount.toString());
                                return peekedAccount;
                            })
                            .block();
                    createdAccounts.put(ownerName+n, accountResponseEntity);
                });
    }

    private double getRandom() {
        Random rn = new Random();
        return rn.nextInt(5) + 1;
    }

    private void process() {
        final List<Flux<Account>> accountMonoList = createdAccounts.keySet().stream()
                .parallel()
                .map(owner -> webClient.get()
                        .uri(SERVICE1 + "?owner=" + owner)
                        .retrieve()
                        .bodyToFlux(Account.class)
                        .flatMap(account -> webClient.post().uri(SERVICE1) //todo loguear
                                .bodyValue(account.toBuilder().owner(account.getOwner() + "new").build()).retrieve().bodyToMono(Account.class)))
                .collect(Collectors.toList());
        Flux.fromIterable(accountMonoList).flatMap(Function.identity()).blockLast();
    }
}
