package br.com.guilhermealvessilve.reactor.exercice2;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.test.StepVerifier;

import java.time.Duration;

class Part02MonoTest {

    private Part02Mono part02Mono;

    @BeforeEach
    void setup() {
        this.part02Mono = new Part02Mono();
    }

    @Test
    void shouldEmptyMono() {
        StepVerifier.create(part02Mono.emptyMono())
                .expectSubscription()
                .verifyComplete();
    }

    @Test
    void shouldMonoWithNoSignal() {
        StepVerifier.create(part02Mono.monoWithNoSignal())
                .expectSubscription()
                .expectNoEvent(Duration.ofDays(1))
                .expectComplete();
    }

    @Test
    void shouldFooMono() {
        StepVerifier.create(part02Mono.fooMono())
                .expectNext("foo")
                .verifyComplete();
    }

    @Test
    void shouldErrorMono() {
        StepVerifier.create(part02Mono.errorMono())
                .expectError(IllegalStateException.class)
                .verify();
    }
}