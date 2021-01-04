package br.com.guilhermealvessilve.reactor.exercice1;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.test.StepVerifier;

class Part01FluxTest {

    private Part01Flux part01Flux;

    @BeforeEach
    void setup() {
        this.part01Flux = new Part01Flux();
    }

    @Test
    void shouldEmptyFlux() {
        StepVerifier.create(part01Flux.emptyFlux())
                .expectSubscription()
                .verifyComplete();
    }

    @Test
    void shouldFooBarFluxFromValues() {
        StepVerifier.create(part01Flux.fooBarFluxFromValues())
                .expectSubscription()
                .expectNext("foo", "bar")
                .verifyComplete();
    }


    @Test
    void shouldFooBarFluxFromList() {
        StepVerifier.create(part01Flux.fooBarFluxFromList())
                .expectSubscription()
                .expectNext("foo", "bar")
                .verifyComplete();
    }

    @Test
    void shouldErrorFlux() {
        StepVerifier.create(part01Flux.errorFlux())
                .expectSubscription()
                .expectError(IllegalStateException.class)
                .verify();
    }

    @Test
    void shouldCounter() {
        StepVerifier.create(part01Flux.counter())
                .expectSubscription()
                .expectNext(0L, 1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L, 9L)
                .verifyComplete();
    }
}