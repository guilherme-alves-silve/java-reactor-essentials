package br.com.guilhermealvessilve.reactor;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.test.StepVerifier;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;

@Slf4j
public class OperatorsTest {

    @Test
    void shouldSubscribeOn() {

        Flux<Integer> flux = Flux.range(1, 5)
                .map(i -> {
                    log.info("Number {} on Thread {}", i, Thread.currentThread().getName());
                    return i;
                })
                .subscribeOn(Schedulers.single())
                .map(i -> {
                    log.info("Number {} on Thread {}", i, Thread.currentThread().getName());
                    return i;
                });

        StepVerifier.create(flux)
                .expectNext(1, 2, 3, 4, 5)
                .expectComplete()
                .verify();
    }

    @Test
    void shouldPublishOn() {

        Flux<Integer> flux = Flux.range(1, 5)
                .map(i -> {
                    log.info("Number {} on Thread {}", i, Thread.currentThread().getName());
                    return i;
                })
                .publishOn(Schedulers.single())
                .map(i -> {
                    log.info("Number {} on Thread {}", i, Thread.currentThread().getName());
                    return i;
                });

        StepVerifier.create(flux)
                .expectNext(1, 2, 3, 4, 5)
                .expectComplete()
                .verify();
    }

    @Test
    void shouldUseTwoPublishOn() {

        Flux<Integer> flux = Flux.range(1, 5)
                .publishOn(Schedulers.single())
                .map(i -> {
                    log.info("Number {} on Thread {}", i, Thread.currentThread().getName());
                    return i;
                })
                .publishOn(Schedulers.boundedElastic())
                .map(i -> {
                    log.info("Number {} on Thread {}", i, Thread.currentThread().getName());
                    return i;
                });

        StepVerifier.create(flux)
                .expectNext(1, 2, 3, 4, 5)
                .expectComplete()
                .verify();
    }

    @Test
    void shouldIgnoreSubscribeOnButUseJustPublishOn() {

        Flux<Integer> flux = Flux.range(1, 5)
                .publishOn(Schedulers.boundedElastic())
                .map(i -> {
                    log.info("Number {} on Thread {}", i, Thread.currentThread().getName());
                    return i;
                })
                .subscribeOn(Schedulers.single())
                .map(i -> {
                    log.info("Number {} on Thread {}", i, Thread.currentThread().getName());
                    return i;
                });

        StepVerifier.create(flux)
                .expectNext(1, 2, 3, 4, 5)
                .expectComplete()
                .verify();
    }

    @Test
    void shouldProcessIOInAnotherThread() {
        Mono<String> stringMono = Mono.fromCallable(() -> Files.readAllBytes(Paths.get("text-file.txt")))
                .map(String::new)
                .map(String::trim)
                .subscribeOn(Schedulers.boundedElastic());

        StepVerifier.create(stringMono)
                .expectSubscription()
                .consumeNextWith(value -> assertEquals("This text is read from some program", value))
                .verifyComplete();
    }

    @Test
    void shouldSwitchIfEmpty() {
        Flux<Object> flux = Flux.empty()
                .switchIfEmpty(Mono.just("Just an empty data"));

        StepVerifier.create(flux)
                .expectNext("Just an empty data")
                .verifyComplete();
    }

    @Test
    void shouldDefer() throws InterruptedException {

        Mono<Long> deferred = Mono.defer(() -> Mono.just(System.currentTimeMillis()));

        AtomicLong atomic = new AtomicLong();

        deferred.subscribe(atomic::set);

        Thread.sleep(50);

        long first = atomic.get();

        deferred.subscribe(atomic::set);

        Thread.sleep(50);

        long second = atomic.get();

        assertTrue(first > 0);
        assertTrue(second > 0);
        assertNotEquals(first, second);
    }

    @Test
    void shouldConcat() {
        Flux<String> flux1 = Flux.just("a", "b");
        Flux<String> flux2 = Flux.just("c", "d");
        Flux<String> fluxConcat = Flux.concat(flux1, flux2);

        StepVerifier.create(fluxConcat)
                .expectSubscription()
                .expectNext("a", "b", "c", "d")
                .verifyComplete();
    }

    @Test
    void shouldConcatAndDelayError() {
        Flux<String> flux1 = Flux.just("a", "b")
                .map(value -> {

                    if ("b".equals(value)) {
                        throw new IllegalArgumentException();
                    }

                    return value;
                });
        Flux<String> flux2 = Flux.just("c", "d");
        Flux<String> fluxConcat = Flux.concatDelayError(flux1, flux2, flux1);

        StepVerifier.create(fluxConcat)
                .expectSubscription()
                .expectNext("a", "c", "d", "a")
                .expectError()
                .verify();
    }

    @Test
    void shouldConcatWith() {
        Flux<String> flux1 = Flux.just("a", "b");
        Flux<String> flux2 = Flux.just("c", "d");
        Flux<String> fluxConcat = flux1.concatWith(flux2);

        StepVerifier.create(fluxConcat)
                .expectSubscription()
                .expectNext("a", "b", "c", "d")
                .verifyComplete();
    }

    @Test
    void shouldCombineLatest() {
        Flux<String> flux1 = Flux.just("a", "b");
        Flux<String> flux2 = Flux.just("c", "d");
        Flux<String> fluxCombineLatest = Flux.combineLatest(flux1, flux2,
                (value1, value2) -> value1.toUpperCase() + value2.toUpperCase())
                .log();

        StepVerifier.create(fluxCombineLatest)
                .expectSubscription()
                .expectNext("BC", "BD")
                .verifyComplete();
    }

    @Test
    void shouldCombineLatestWithDelay() {
        Flux<String> flux1 = Flux.just("a", "b").delayElements(Duration.ofSeconds(1));
        Flux<String> flux2 = Flux.just("c", "d");
        Flux<String> fluxCombineLatest = Flux.combineLatest(flux1, flux2,
                (value1, value2) -> value1.toUpperCase() + value2.toUpperCase())
                .log();

        StepVerifier.create(fluxCombineLatest)
                .expectSubscription()
                .expectNext("AD", "BD")
                .verifyComplete();
    }

    @Test
    void shouldMerge() {
        Flux<String> flux1 = Flux.just("a", "b");
        Flux<String> flux2 = Flux.just("c", "d");
        Flux<String> fluxMerged = Flux.merge(flux1, flux2);

        StepVerifier.create(fluxMerged)
                .expectNext("a", "b", "c", "d")
                .verifyComplete();
    }

    @Test
    void shouldMergeAndGetCDEagerlyFirstThenAB() {
        Flux<String> flux1 = Flux.just("a", "b").delayElements(Duration.ofSeconds(1));
        Flux<String> flux2 = Flux.just("c", "d");
        Flux<String> fluxMerged = Flux.merge(flux1, flux2);

        StepVerifier.create(fluxMerged)
                .expectSubscription()
                .expectNext("c", "d", "a", "b")
                .verifyComplete();
    }

    @Test
    void shouldMergeWith() {
        Flux<String> flux1 = Flux.just("a", "b");
        Flux<String> flux2 = Flux.just("c", "d");
        Flux<String> fluxMerged = flux1.mergeWith(flux2);

        StepVerifier.create(fluxMerged)
                .expectNext("a", "b", "c", "d")
                .verifyComplete();
    }

    @Test
    void shouldMergeSequential() {
        Flux<String> flux1 = Flux.just("a", "b").delayElements(Duration.ofSeconds(1));
        Flux<String> flux2 = Flux.just("c", "d");
        Flux<String> fluxMerged = Flux.mergeSequential(flux1, flux2);

        StepVerifier.create(fluxMerged)
                .expectNext("a", "b", "c", "d")
                .verifyComplete();
    }

    @Test
    void shouldMergeAndGetElementFromErrorList() {
        Flux<String> flux1 = Flux.just("a", "b")
                .map(value -> {

                    if ("a".equals(value)) {
                        throw new IllegalArgumentException();
                    }

                    return value;
                }).onErrorReturn("a");
        Flux<String> flux2 = Flux.just("c", "d");
        Flux<String> fluxMerged = Flux.mergeSequential(1, flux1, flux2);

        StepVerifier.create(fluxMerged)
                .expectNext("a", "c", "d")
                .expectComplete()
                .verify();
    }

    @Test
    void shouldMergeAndDelayError() {
        Flux<String> flux1 = Flux.just("a", "b")
                .map(value -> {

                    if ("a".equals(value)) {
                        throw new IllegalArgumentException();
                    }

                    return value;
                });
        Flux<String> flux2 = Flux.just("c", "d");
        Flux<String> fluxMerged = Flux.mergeDelayError(1, flux1, flux2).log();

        StepVerifier.create(fluxMerged)
                .expectNext("c", "d")
                .expectError()
                .verify();
    }

    @Test
    void shouldFlattenTheMap() {

        Function<String, Flux<String>> functorFindByName = (value) -> ("a".equals(value))
                ? Flux.just("name a1", "name a2")
                : Flux.just("name b1", "name b2");

        Flux<String> flux = Flux.just("a", "b")
                .flatMap(functorFindByName);

        StepVerifier.create(flux)
                .expectNext("name a1", "name a2", "name b1", "name b2")
                .verifyComplete();
    }

    @Test
    void shouldFlattenTheMapButHasDelay() {

        Function<String, Flux<String>> functorFindByName = (value) -> ("a".equals(value))
                ? Flux.just("name a1", "name a2").delayElements(Duration.ofMillis(100))
                : Flux.just("name b1", "name b2");

        Flux<String> flux = Flux.just("a", "b")
                .flatMap(functorFindByName);

        StepVerifier.create(flux)
                .expectNext("name b1", "name b2", "name a1", "name a2")
                .verifyComplete();
    }

    @Test
    void shouldFlattenTheMapThatHasDelayButMaintainSequence() {

        Function<String, Flux<String>> functorFindByName = (value) -> ("a".equals(value))
                ? Flux.just("name a1", "name a2").delayElements(Duration.ofMillis(100))
                : Flux.just("name b1", "name b2");

        Flux<String> flux = Flux.just("a", "b")
                .flatMapSequential(functorFindByName);

        StepVerifier.create(flux)
                .expectNext("name a1", "name a2", "name b1", "name b2")
                .verifyComplete();
    }

    @Test
    void shouldZip() {

        Flux<String> titleFlux = Flux.just("Hacksaw Ridge", "Saint Seiya: A Lenda dos Defensores de Atena");
        Flux<String> studioFlux = Flux.just("Fox Studios Australia", "Toei Animation");
        Flux<Integer> minutesFlux = Flux.just(139, 75);

        Flux<Movie> movieFlux = Flux.zip(titleFlux, studioFlux, minutesFlux)
                .flatMap(tuple -> Mono.just(new Movie(tuple.getT1(), tuple.getT2(), tuple.getT3())));

        StepVerifier.create(movieFlux)
                .expectSubscription()
                .expectNext(
                        new Movie("Hacksaw Ridge", "Fox Studios Australia", 139),
                        new Movie("Saint Seiya: A Lenda dos Defensores de Atena", "Toei Animation", 75)
                )
                .verifyComplete();
    }

    @Test
    void shouldZipWith() {

        Flux<String> titleFlux = Flux.just("Hacksaw Ridge", "Saint Seiya: A Lenda dos Defensores de Atena");
        Flux<String> studioFlux = Flux.just("Fox Studios Australia", "Toei Animation");

        Flux<Movie> movieFlux = titleFlux.zipWith(studioFlux)
                .flatMap(tuple -> Mono.just(new Movie(tuple.getT1(), tuple.getT2(), null)));

        StepVerifier.create(movieFlux)
                .expectSubscription()
                .expectNext(
                        new Movie("Hacksaw Ridge", "Fox Studios Australia", null),
                        new Movie("Saint Seiya: A Lenda dos Defensores de Atena", "Toei Animation", null)
                )
                .verifyComplete();
    }

    @Getter
    @ToString
    @EqualsAndHashCode
    @RequiredArgsConstructor
    private static class Movie {

        private final String title;
        private final String studio;
        private final Integer min;
    }
}
