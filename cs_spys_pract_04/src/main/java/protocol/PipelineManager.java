package protocol;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class PipelineManager {
    private final Store store = new Store();
    private final AtomicBoolean running = new AtomicBoolean(false);

    private final BlockingQueue<byte[]> rawQueue      = new ArrayBlockingQueue<>(1000);
    private final BlockingQueue<Packet> decryptQueue  = new ArrayBlockingQueue<>(1000);
    private final BlockingQueue<Packet> responseQueue = new ArrayBlockingQueue<>(1000);
    private final BlockingQueue<byte[]> encryptQueue  = new ArrayBlockingQueue<>(1000);

    private ExecutorService rxPool, decPool, procPool, encPool, txPool;
    private Receiver mainReceiver;

    public void startPipeline(int receivers, int decriptors, int processors, int encriptors, int senders) {
        running.set(true);
        rxPool   = Executors.newFixedThreadPool(receivers);
        decPool  = Executors.newFixedThreadPool(decriptors);
        procPool = Executors.newFixedThreadPool(processors);
        encPool  = Executors.newFixedThreadPool(encriptors);
        txPool   = Executors.newFixedThreadPool(senders);

        for (int i = 0; i < receivers; i++) {
            FakeReceiver fake = new FakeReceiver(rawQueue, running);
            if (this.mainReceiver == null) this.mainReceiver = fake;
            rxPool.submit(fake);
        }
        for (int i = 0; i < decriptors; i++)
            decPool.submit(new Decryptor(rawQueue, decryptQueue, running));
        for (int i = 0; i < processors; i++)
            procPool.submit(new Processor(decryptQueue, responseQueue, store, running));
        for (int i = 0; i < encriptors; i++)
            encPool.submit(new Encryptor(responseQueue, encryptQueue, running));
        for (int i = 0; i < senders; i++)
            txPool.submit(new FakeSender(encryptQueue, running));
    }

    public void injectNetworkPacket(byte[] packetBytes) {
        if (mainReceiver != null) mainReceiver.receiveMessage(packetBytes);
    }

    public Store getStore() { return store; }

    public void shutdown() {
        running.set(false);
        shutdownAndAwaitTermination(rxPool);
        shutdownAndAwaitTermination(decPool);
        shutdownAndAwaitTermination(procPool);
        shutdownAndAwaitTermination(encPool);
        shutdownAndAwaitTermination(txPool);
        System.out.println("[Pipeline] Graceful shutdown accomplished.");
    }

    private void shutdownAndAwaitTermination(ExecutorService pool) {
        if (pool == null) return;
        pool.shutdown();
        try {
            if (!pool.awaitTermination(2, TimeUnit.SECONDS)) {
                pool.shutdownNow();
                if (!pool.awaitTermination(1, TimeUnit.SECONDS)) {
                    System.err.println("Pool did not terminate");
                }
            }
        } catch (InterruptedException ie) {
            pool.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
