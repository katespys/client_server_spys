package protocol;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

public class StoreServerTCP {
    private final int port;
    private final StoreProcessor processor;
    private final ExecutorService threadPool = Executors.newCachedThreadPool();
    private final AtomicInteger clientCounter = new AtomicInteger(0);

    public StoreServerTCP(int port, Store store) {
        this.port = port;
        this.processor = new StoreProcessor(store);
    }

    public void start() {
        System.out.println("[TCP] запуск на порту " + port);
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            while (true) {
                Socket clientSocket = serverSocket.accept();
                int clientId = clientCounter.incrementAndGet();
                System.out.println("[TCP] новий клієнт #" + clientId
                        + " підключився: " + clientSocket.getRemoteSocketAddress());
                threadPool.submit(() -> handleClient(clientSocket, clientId));
            }
        } catch (IOException e) {
            System.err.println("[TCP] помилка ServerSocket: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void handleClient(Socket socket, int clientId) {
        System.out.println("[TCP] клієнт #" + clientId + " — початок обробки");
        try (DataInputStream in   = new DataInputStream(socket.getInputStream());
             DataOutputStream out = new DataOutputStream(socket.getOutputStream())) {

            while (!socket.isClosed()) {
                byte[] header = new byte[14];
                try {
                    in.readFully(header);
                } catch (EOFException e) {
                    System.out.println("[TCP] клієнт #" + clientId + " закрив з'єднання");
                    break;
                }

                ByteBuffer buffer = ByteBuffer.wrap(header);
                buffer.position(10);
                int wLen = buffer.getInt();

                byte[] remainder = new byte[2 + wLen + 2];
                in.readFully(remainder);

                byte[] fullPacket = new byte[14 + remainder.length];
                System.arraycopy(header,    0, fullPacket,  0, 14);
                System.arraycopy(remainder, 0, fullPacket, 14, remainder.length);

                try {
                    Packet request  = ProtocolHandler.decode(fullPacket);
                    System.out.println("[TCP] клієнт #" + clientId
                            + " — запит pktId=" + request.getPktId()
                            + " type=" + request.getMessage().getType());
                    Packet response  = processor.process(request);
                    byte[] responseData = ProtocolHandler.encode(response);
                    out.write(responseData);
                    out.flush();
                } catch (IllegalArgumentException e) {
                    System.err.println("[TCP] клієнт #" + clientId
                            + " — пошкоджений пакет: " + e.getMessage());
                } catch (Exception e) {
                    System.err.println("[TCP] клієнт #" + clientId
                            + " — помилка обробки: " + e.getMessage());
                }
            }

        } catch (IOException e) {
            System.out.println("[TCP] клієнт #" + clientId + " — обрив з'єднання: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("[TCP] клієнт #" + clientId + " — неочікувана помилка: " + e.getMessage());
        } finally {
            try { socket.close(); } catch (IOException ignored) {}
            System.out.println("[TCP] клієнт #" + clientId + " — з'єднання закрито");
        }
    }
}
