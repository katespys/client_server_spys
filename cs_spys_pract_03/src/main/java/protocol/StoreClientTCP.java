package protocol;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.nio.ByteBuffer;

public class StoreClientTCP {
    private final String host;
    private final int port;
    private volatile Socket socket;
    private volatile DataInputStream in;
    private volatile DataOutputStream out;

    private static final int RECONNECT_DELAY_MS = 3000;

    public StoreClientTCP(String host, int port) {
        this.host = host;
        this.port = port;
    }

    private void connectSafely() {
        while (true) {
            if (socket != null && !socket.isClosed() && socket.isConnected()) {
                return;
            }
            closeQuietly();
            try {
                System.out.println("[TCP] підключення до " + host + ":" + port + "...");
                socket = new Socket(host, port);
                in  = new DataInputStream(socket.getInputStream());
                out = new DataOutputStream(socket.getOutputStream());
                System.out.println("[TCP] з'єднання встановлено");
                return;
            } catch (IOException e) {
                System.out.println("[TCP] сервер недоступний (" + e.getMessage()
                        + "), повтор через " + RECONNECT_DELAY_MS / 1000 + "с...");
                try {
                    Thread.sleep(RECONNECT_DELAY_MS);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("клієнт перерваний під час очікування з'єднання", ie);
                }
            }
        }
    }

    public synchronized Packet sendRequest(Packet request) {
        while (true) {
            connectSafely();
            try {
                byte[] requestData = ProtocolHandler.encode(request);
                out.write(requestData);
                out.flush();

                byte[] header = new byte[14];
                in.readFully(header);

                ByteBuffer buffer = ByteBuffer.wrap(header);
                buffer.position(10);
                int wLen = buffer.getInt();

                byte[] remainder = new byte[2 + wLen + 2];
                in.readFully(remainder);

                byte[] fullPacket = new byte[14 + remainder.length];
                System.arraycopy(header,    0, fullPacket,  0, 14);
                System.arraycopy(remainder, 0, fullPacket, 14, remainder.length);

                return ProtocolHandler.decode(fullPacket);

            } catch (Exception e) {
                System.out.println("[TCP] помилка зв'язку: " + e.getMessage() + ", відновлюю з'єднання...");
                closeQuietly();
            }
        }
    }

    private void closeQuietly() {
        try {
            if (socket != null) socket.close();
        } catch (IOException ignored) {}
        socket = null;
        in  = null;
        out = null;
    }

    public void close() {
        closeQuietly();
    }
}
