package br.com.guilhermealvessilve.reactor;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.reactivestreams.Subscription;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@Slf4j
public class MonoTest {


    @Test
    void shouldMonoExpectStringValue() {

        Mono<String> mono = Mono.just("Nani")
                .log();

        StepVerifier.create(mono)
                .expectNext("Nani")
                .verifyComplete();
    }

    @Test
    void shouldMonoExpectError() {

        Mono<String> mono = Mono.just("Nani")
                .map(value -> {
                    if (true) {
                        throw new RuntimeException();
                    }

                    return value;
                })
                .log();

        StepVerifier.create(mono)
                .expectError(RuntimeException.class)
                .verify();
    }

    @Test
    void shouldMonoSubscribe() {

        Mono<String> mono = Mono.just("Nani!?!!?")
                .log();

        mono.subscribe(log::info);

        StepVerifier.create(mono)
                .expectNext("Nani!?!!?")
                .verifyComplete();
    }

    @Test
    void shouldMonoSubscribeAndExpectError() {

        Mono<String> mono = Mono.just("Nani")
                .map(value -> {
                    if (true) {
                        throw new RuntimeException();
                    }

                    return value;
                })
                .log();

        mono.subscribe(log::info, Throwable::printStackTrace);

        StepVerifier.create(mono)
                .expectError(RuntimeException.class)
                .verify();
    }

    @Test
    void shouldMonoSubscribeAndDoOnCompleteAndCancelSubscription() {

        Mono<String> mono = Mono.just("Nani")
                .map(String::toUpperCase)
                .log();

        mono.subscribe(log::info, Throwable::printStackTrace, () -> log.info("Finished!"), Subscription::cancel);

        StepVerifier.create(mono)
                .expectNext("NANI")
                .verifyComplete();
    }

    @Test
    void shouldDoOnMethods() {

        Mono<Object> mono = Mono.just("Nani")
                .log()
                .map(String::toLowerCase)
                .doOnSubscribe(subscription -> log.info("Subscribed: {}", subscription))
                .doOnRequest(requests -> log.info("Requests made: {}", requests))
                .doOnNext(value -> log.info("Next value: {}", value))
                .flatMap(value -> Mono.empty())
                .doOnNext(none -> log.info("[This won't be executed] Next value after flatMap: {}", none))
                .doOnSuccess(none -> log.info("Success: {}", none));

        mono.subscribe(value -> log.info("Value: {}" , value),
                Throwable::printStackTrace,
                () -> log.info("Finished!"),
                subscription -> subscription.request(1L));

        log.info("**************************************************");

        StepVerifier.create(mono)
                .expectComplete()
                .verify();
    }

    @Test
    void shouldDoOnMethodsContinuedFlow() {

        Mono<String> mono = Mono.just("Nani")
                .log()
                .map(String::toLowerCase)
                .doOnSubscribe(subscription -> log.info("Subscribed: {}", subscription))
                .doOnRequest(requests -> log.info("Requests made: {}", requests))
                .doOnNext(value -> log.info("Next value: {}", value))
                .flatMap(value -> Mono.just("Another Nani!?!?!?"))
                .doOnNext(value -> log.info("[This will execute] Next value after flatMap: {}", value))
                .doOnSuccess(value -> log.info("Success: {}", value));

        mono.subscribe(value -> log.info("Value: {}" , value),
                Throwable::printStackTrace,
                () -> log.info("Finished!"),
                subscription -> subscription.request(1L));

        log.info("**************************************************");

        StepVerifier.create(mono)
                .expectNext("Another Nani!?!?!?")
                .expectComplete()
                .verify();
    }

    @Test
    void shouldDoOnErrorMethods() {

        Mono<Object> mono = Mono.error(new IllegalArgumentException("Error message!!!"))
                .doOnError(error -> log.error("Error : ", error))
                .log();

        StepVerifier.create(mono)
                .expectError(IllegalArgumentException.class)
                .verify();
    }

    @Test
    void shouldDoOnErrorMethodsButRecover() {

        Mono<Object> mono = Mono.error(new IllegalArgumentException("Error message!!!"))
                .doOnError(error -> log.error("Error : ", error))
                .onErrorResume(error -> Mono.just("Recovered from error"))
                .log();

        StepVerifier.create(mono)
                .expectNext("Recovered from error")
                .verifyComplete();
    }

    @Test
    void shouldDoOnErrorMethodsButRecover2() {

        Mono<Object> mono = Mono.error(new IllegalArgumentException("Error message!!!"))
                .doOnError(error -> log.error("Error : ", error))
                .onErrorReturn("Fallback used because of error")
                .log();

        StepVerifier.create(mono)
                .expectNext("Fallback used because of error")
                .verifyComplete();
    }
}
