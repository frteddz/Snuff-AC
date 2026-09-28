package dev.snuffac.core.violation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ViolationBufferTest {

    @Test
    @DisplayName("the buffer crosses only after enough evidence accumulates")
    void crossesAfterThreshold() {
        ViolationBuffer buffer = new ViolationBuffer(20.0, 0.0, 100.0);
        assertFalse(buffer.crossed());
        buffer.add(5.0);
        assertFalse(buffer.crossed());
        buffer.add(15.0);
        assertTrue(buffer.crossed());
    }

    @Test
    @DisplayName("evidence is capped at the configured maximum")
    void respectsMaximum() {
        ViolationBuffer buffer = new ViolationBuffer(20.0, 0.0, 25.0);
        buffer.add(1000.0);
        assertEquals(25.0, buffer.value(), 1.0E-9);
    }

    @Test
    @DisplayName("decay bleeds evidence away over time")
    void decaysOverTime() {
        ViolationBuffer buffer = new ViolationBuffer(20.0, 0.5, 100.0);
        buffer.add(10.0);
        buffer.tick();
        assertEquals(9.5, buffer.value(), 1.0E-9);
        buffer.tick();
        assertEquals(9.0, buffer.value(), 1.0E-9);
    }

    @Test
    @DisplayName("the buffer never decays below zero")
    void decaysToZeroFloor() {
        ViolationBuffer buffer = new ViolationBuffer(20.0, 5.0, 100.0);
        buffer.add(3.0);
        for (int i = 0; i < 10; i++) {
            buffer.tick();
        }
        assertEquals(0.0, buffer.value(), 1.0E-9);
    }

    @Test
    @DisplayName("draining resets evidence immediately")
    void drainResets() {
        ViolationBuffer buffer = new ViolationBuffer(20.0, 0.0, 100.0);
        buffer.add(50.0);
        buffer.drain();
        assertEquals(0.0, buffer.value(), 1.0E-9);
        assertFalse(buffer.crossed());
    }

    @Test
    @DisplayName("negative contributions are ignored so a check cannot unflag itself")
    void ignoresNegative() {
        ViolationBuffer buffer = new ViolationBuffer(20.0, 0.0, 100.0);
        buffer.add(10.0);
        buffer.add(-5.0);
        assertEquals(10.0, buffer.value(), 1.0E-9);
    }

    @Test
    @DisplayName("progress reports how close the buffer is to its threshold")
    void progressIsNormalised() {
        ViolationBuffer buffer = new ViolationBuffer(20.0, 0.0, 100.0);
        buffer.add(10.0);
        assertEquals(0.5, buffer.progress(), 1.0E-9);
        buffer.add(50.0);
        assertEquals(1.0, buffer.progress(), 1.0E-9);
    }

    @Test
    @DisplayName("proportional adds scale evidence by the size of the excess")
    void proportionalAdds() {
        ViolationBuffer buffer = new ViolationBuffer(20.0, 0.0, 100.0);
        buffer.addProportional(0.1, 10.0);
        assertEquals(1.0, buffer.value(), 1.0E-9);
        buffer.addProportional(0.2, 10.0);
        assertEquals(3.0, buffer.value(), 1.0E-9);
    }
}
