package protocol;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class Decriptor implements Runnable {
    private final BlockingQueue<byte[]> inputQueue;
    private final BlockingQueue<Packet> outputQueue;
    private final AtomicBoolean running;

    public Decriptor(BlockingQueue<byte[]> inputQueue, BlockingQueue<Packet> outputQueue, AtomicBoolean running) {
        this.inputQueue = inputQueue;
        this.outputQueue = outputQueue;
        this.running = running;
    }

    @Override
    public void run() {
        while (running.get() || !inputQueue.isEmpty()) {
            try {
                byte[] data = inputQueue.poll(50, TimeUnit.MILLISECONDS);
                if (data != null) {
                    try {
                        Packet packet = ProtocolHandler.decode(data);
                        outputQueue.put(packet);
                    } catch (IllegalArgumentException e) {
                        System.err.println("[Decriptor] Skipping malformed packet: " + e.getMessage());
                    } catch (Exception e) {
                        System.err.println("[Decriptor] Unexpected decode error: " + e.getMessage());
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }
}
