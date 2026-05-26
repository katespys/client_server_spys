package protocol;

import java.nio.charset.StandardCharsets;
import java.util.Random;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

public class FakeReceiver implements Receiver {
    private final BlockingQueue<byte[]> outputQueue;
    private final AtomicBoolean running;
    private final Random random = new Random();

    public FakeReceiver(BlockingQueue<byte[]> outputQueue, AtomicBoolean running) {
        this.outputQueue = outputQueue;
        this.running = running;
    }

    @Override
    public void receiveMessage(byte[] data) {
        try {
            outputQueue.put(data);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @Override
    public void run() {
        while (running.get() && !Thread.currentThread().isInterrupted()) {
            try {
                if (random.nextInt(100) < 5) {
                    Message msg = new Message(1, random.nextInt(1000), "Random".getBytes(StandardCharsets.UTF_8));
                    Packet pkt = new Packet((byte) 1, random.nextLong(), msg);
                    receiveMessage(ProtocolHandler.encode(pkt));
                }
                Thread.sleep(50);
            } catch (Exception e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }
}