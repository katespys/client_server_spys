package protocol;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

class MultiThreadedStoreTest {

    private PipelineManager manager;

    @BeforeEach
    void setUp() {
        manager = new PipelineManager();
        manager.startPipeline(2, 2, 4, 3, 5);
    }

    @AfterEach
    void tearDown() {
        manager.shutdown();
    }

    @Test
    void shouldHandleConcurrentDeductionsWithoutRaceCondition() throws Exception {
        Store store = manager.getStore();
        store.addQuantity("гречка", 100);

        ExecutorService clientSimulators = Executors.newFixedThreadPool(10);

        int packetCount = 50;
        CountDownLatch latch = new CountDownLatch(packetCount);
        AtomicInteger injected = new AtomicInteger(0);

        List<byte[]> packagedRequests = new ArrayList<>();
        for (int i = 0; i < packetCount; i++) {
            byte[] payload = ("гречка,2").getBytes(StandardCharsets.UTF_8);
            packagedRequests.add(
                    ProtocolHandler.encode(new Packet((byte) 1, i, new Message(2, 777, payload)))
            );
        }

        for (byte[] rawPacket : packagedRequests) {
            clientSimulators.submit(() -> {
                manager.injectNetworkPacket(rawPacket);
                injected.incrementAndGet();
                latch.countDown();
            });
        }

        clientSimulators.shutdown();
        boolean allInjected = clientSimulators.awaitTermination(5, TimeUnit.SECONDS);
        Assertions.assertThat(allInjected).as("All packets should be injected within 5s").isTrue();

        long deadline = System.currentTimeMillis() + 5_000;
        while (System.currentTimeMillis() < deadline) {
            if (store.getQuantity("гречка") == 0) break;
            Thread.sleep(50);
        }

        Assertions.assertThat(store.getQuantity("гречка"))
                .as("Expected 0 after 50 deductions of 2 from initial stock of 100")
                .isEqualTo(0);
    }

    @Test
    void shouldHandleConcurrentAdditionsCorrectly() throws Exception {
        Store store = manager.getStore();
        store.addQuantity("рис", 0);

        int threadCount = 20;
        int addPerThread = 5;
        ExecutorService pool = Executors.newFixedThreadPool(threadCount);

        for (int i = 0; i < threadCount; i++) {
            byte[] payload = ("рис," + addPerThread).getBytes(StandardCharsets.UTF_8);
            byte[] rawPacket = ProtocolHandler.encode(new Packet((byte) 1, i, new Message(3, 1, payload)));
            pool.submit(() -> manager.injectNetworkPacket(rawPacket));
        }

        pool.shutdown();
        pool.awaitTermination(5, TimeUnit.SECONDS);

        long deadline = System.currentTimeMillis() + 5_000;
        int expected = threadCount * addPerThread;
        while (System.currentTimeMillis() < deadline) {
            if (store.getQuantity("рис") == expected) break;
            Thread.sleep(50);
        }

        Assertions.assertThat(store.getQuantity("рис"))
                .as("Expected " + expected + " after concurrent additions")
                .isEqualTo(expected);
    }

    @Test
    void shouldNotDeductBelowZero() throws Exception {
        Store store = manager.getStore();
        store.addQuantity("цукор", 10);

        int attempts = 20;
        ExecutorService pool = Executors.newFixedThreadPool(attempts);
        for (int i = 0; i < attempts; i++) {
            byte[] payload = "цукор,1".getBytes(StandardCharsets.UTF_8);
            byte[] rawPacket = ProtocolHandler.encode(new Packet((byte) 1, i, new Message(2, 1, payload)));
            pool.submit(() -> manager.injectNetworkPacket(rawPacket));
        }
        pool.shutdown();
        pool.awaitTermination(5, TimeUnit.SECONDS);

        long deadline = System.currentTimeMillis() + 5_000;
        while (System.currentTimeMillis() < deadline) {
            if (store.getQuantity("цукор") == 0) break;
            Thread.sleep(50);
        }

        Assertions.assertThat(store.getQuantity("цукор"))
                .as("Stock should never go below zero")
                .isGreaterThanOrEqualTo(0);
    }
}
