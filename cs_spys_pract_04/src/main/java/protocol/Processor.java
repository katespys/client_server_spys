package protocol;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class Processor implements Runnable {
    private final BlockingQueue<Packet> inputQueue;
    private final BlockingQueue<Packet> outputQueue;
    private final StoreProcessor storeProcessor;
    private final AtomicBoolean running;

    public Processor(BlockingQueue<Packet> inputQueue, BlockingQueue<Packet> outputQueue,
                     Store store, AtomicBoolean running) {
        this.inputQueue = inputQueue;
        this.outputQueue = outputQueue;
        this.storeProcessor = new StoreProcessor(store);
        this.running = running;
    }

    public Processor(BlockingQueue<Packet> inputQueue, BlockingQueue<Packet> outputQueue,
                     Store store, ProductService productService, AtomicBoolean running) {
        this.inputQueue = inputQueue;
        this.outputQueue = outputQueue;
        this.storeProcessor = new StoreProcessor(store, productService);
        this.running = running;
    }

    @Override
    public void run() {
        while (running.get() || !inputQueue.isEmpty()) {
            try {
                Packet packet = inputQueue.poll(50, TimeUnit.MILLISECONDS);
                if (packet != null) {
                    Packet response = storeProcessor.process(packet);
                    try {
                        outputQueue.put(response);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }
}
