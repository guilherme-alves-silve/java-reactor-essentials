package br.com.guilhermealvessilve.reactor;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import reactor.core.publisher.BaseSubscriber;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CountDownLatch;

@Slf4j
public class FlexTest {

    @Test
    void shouldFluxSend3Names() {
        Flux<String> flux = Flux.just("Guilherme", "Nani", "Test")
                .log();

        flux.subscribe(name -> log.info("Name: {}", name));

        StepVerifier.create(flux)
                .expectNext("Guilherme", "Nani", "Test")
                .verifyComplete();
    }

    @Test
    void shouldFluxRangeSend5Numbers() {
        Flux<Integer> flux = Flux.range(1, 5)
                .log();

        flux.subscribe(number -> log.info("Number: {}", number));

        StepVerifier.create(flux)
                .expectNext(1, 2, 3, 4, 5)
                .verifyComplete();
    }

    @Test
    void shouldFluxWorkWithList() {
        Flux<Integer> flux = Flux.fromIterable(List.of(1, 2, 3, 4))
                .log();

        flux.subscribe(number -> log.info("Number: {}", number));

        StepVerifier.create(flux)
                .expectNext(1, 2, 3, 4)
                .verifyComplete();
    }

    @Test
    void shouldFluxSendError() {
        Integer[] ints = {1, 2, 3, 4, 5};
        Flux<Integer> flux = Flux.fromArray(ints)
                .map(i -> {
                    if (i == 4) {
                        throw new IllegalArgumentException("Failed!");
                    }

                    return i * 2;
                })
                .log();

        flux.subscribe(number -> log.info("Number: {}", number));

        StepVerifier.create(flux)
                .expectNext(2, 4, 6)
                .expectError(IllegalArgumentException.class)
                .verify();
    }

    @Test
    void shouldFluxWorkWithComplexSubscriberBackPressure() {
        Flux<Integer> flux = Flux.fromIterable(List.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10))
                .log();

        flux.subscribe(new Subscriber<>() {

            private final int requestsPerTime = 2;
            private int requests = 0;
            private Subscription subscription;

            @Override
            public void onSubscribe(final Subscription subscription) {
                this.subscription = subscription;
                requests += requestsPerTime;
                subscription.request(requestsPerTime);
            }

            @Override
            public void onNext(Integer integer) {
                if (++requests >= requestsPerTime) {
                    requests = 0;
                    subscription.request(requestsPerTime);
                }
            }

            @Override
            public void onError(Throwable throwable) {
                throwable.printStackTrace();
            }

            @Override
            public void onComplete() {
                log.info("COMPLETE!");
            }
        });

        StepVerifier.create(flux)
                .expectNext(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
                .verifyComplete();
    }

    @Test
    void shouldFluxWorkWithSubscriberBackPressure() {
        Flux<Integer> flux = Flux.fromIterable(List.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10))
                .log();

        flux.subscribe(new Subscriber<>() {

            private final int requestsPerTime = 2;
            private int requests = 0;
            private Subscription subscription;

            @Override
            public void onSubscribe(final Subscription subscription) {
                this.subscription = subscription;
                requests += requestsPerTime;
                subscription.request(requestsPerTime);
            }

            @Override
            public void onNext(Integer integer) {
                if (++requests >= requestsPerTime) {
                    requests = 0;
                    subscription.request(requestsPerTime);
                }
            }

            @Override
            public void onError(Throwable throwable) {
                throwable.printStackTrace();
            }

            @Override
            public void onComplete() {
                log.info("COMPLETE!");
            }
        });

        StepVerifier.create(flux)
                .expectNext(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
                .verifyComplete();
    }

    @Test
    void shouldFluxWorkWithBaseSubscriberBackPressure() {
        Flux<Integer> flux = Flux.fromIterable(List.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10))
                .log();

        flux.subscribe(new BaseSubscriber<>() {

            private final int requestsPerTime = 2;
            private int requests = 0;
            private Subscription subscription;

            @Override
            protected void hookOnSubscribe(Subscription subscription) {
                this.subscription = subscription;
                requests += requestsPerTime;
                subscription.request(requestsPerTime);
            }

            @Override
            protected void hookOnNext(Integer value) {
                if (++requests >= requestsPerTime) {
                    requests = 0;
                    subscription.request(requestsPerTime);
                }
            }
        });

        StepVerifier.create(flux)
                .expectNext(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
                .verifyComplete();
    }

    @Test
    void shouldFluxWorkWithPrettyBackPressure() {
        Flux.just(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
            .log()
            .limitRate(3)
            .subscribe(i -> log.info("Number: {}", i));
    }

    @Test
    void shouldTestInterval() throws InterruptedException {

        final var latch = new CountDownLatch(10);
        Flux.interval(Duration.ofMillis(100))
                .log()
                .subscribe(i -> {
                    log.info("Number: {}", i);
                    latch.countDown();
                });

        latch.await();
    }

    @Test
    void shouldCheckIntervalInDays() {
        StepVerifier.withVirtualTime(this::createInterval)
                    .expectSubscription()
                    .expectNoEvent(Duration.ofHours(24))
                    .thenAwait(Duration.ofDays(1))
                    .expectNext(0L)
                    .thenAwait(Duration.ofDays(1))
                    .expectNext(1L)
                    .thenCancel()
                    .verify();
    }

    private Flux<Long> createInterval() {
        return Flux.interval(Duration.ofDays(1))
                .log();
    }
}
