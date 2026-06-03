package protocol;
import java.net.InetAddress;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class FakeSender implements Sender {
    private final BlockingQueue<byte[]> inputQueue;
    private final AtomicBoolean running;

    public FakeSender(BlockingQueue<byte[]> inputQueue, AtomicBoolean running) {
        this.inputQueue = inputQueue;
        this.running = running;
    }

    @Override
    public void sendMessage(byte[] mess, InetAddress target) {
        System.out.println("[FakeSender] Sending " + mess.length + " bytes to " + target.getHostAddress());
    }

    @Override
    public void run() {
        try {
            InetAddress target = InetAddress.getByName("127.0.0.1");
            while (running.get() || !inputQueue.isEmpty()) {
                byte[] data = inputQueue.poll(50, TimeUnit.MILLISECONDS);
                if (data != null) {
                    sendMessage(data, target);
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            System.err.println("[FakeSender] Error: " + e.getMessage());
            Thread.currentThread().interrupt();
            System.err.println("[FakeSender] Interrupted, stopping.");
        }
    }
}
