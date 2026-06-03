package protocol;

import java.net.InetAddress;

public interface Sender extends Runnable {
    void sendMessage(byte[] mess, InetAddress target);
}
