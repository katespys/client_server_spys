package protocol;

public interface Receiver extends Runnable {
    void receiveMessage(byte[] data);
}