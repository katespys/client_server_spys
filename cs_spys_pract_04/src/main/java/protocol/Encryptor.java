package protocol;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class Encryptor implements Runnable {
    private final BlockingQueue<Packet> inputQueue;
    private final BlockingQueue<byte[]> outputQueue;
    private final AtomicBoolean running;

    public Encryptor(BlockingQueue<Packet> inputQueue, BlockingQueue<byte[]> outputQueue, AtomicBoolean running) {
        this.inputQueue = inputQueue;
        this.outputQueue = outputQueue;
        this.running = running;
    }

    @Override
    public void run() {
        while (running.get() || !inputQueue.isEmpty()) {
            try {
                Packet packet = inputQueue.poll(50, TimeUnit.MILLISECONDS);
                if (packet != null) {
                    try {
                        outputQueue.put(ProtocolHandler.encode(packet));
                    } catch (Exception e) {
                        System.err.println("[Encriptor] Failed to encode packet: " + e.getMessage());
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }
}
