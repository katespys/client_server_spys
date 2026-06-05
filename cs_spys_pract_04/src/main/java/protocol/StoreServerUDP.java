package protocol;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class StoreServerUDP {
    private final int port;
    private final StoreProcessor processor;
    private final ExecutorService threadPool = Executors.newFixedThreadPool(20);

    private final Object sendLock = new Object();

    public StoreServerUDP(int port, Store store) {
        this.port = port;
        this.processor = new StoreProcessor(store);
    }

    public void start() {
        System.out.println("[UDP] запуск на порту " + port);
        try (DatagramSocket socket = new DatagramSocket(port)) {
            while (true) {
                byte[] buffer = new byte[8192];
                DatagramPacket receivePacket = new DatagramPacket(buffer, buffer.length);
                socket.receive(receivePacket);

                final byte[] data = Arrays.copyOf(receivePacket.getData(), receivePacket.getLength());
                final InetAddress clientAddress = receivePacket.getAddress();
                final int clientPort = receivePacket.getPort();

                threadPool.submit(() -> {
                    try {
                        Packet request  = ProtocolHandler.decode(data);
                        System.out.println("[UDP] запит від " + clientAddress + ":" + clientPort
                                + " pktId=" + request.getPktId()
                                + " type=" + request.getMessage().getType());

                        Packet response     = processor.process(request);
                        byte[] responseData = ProtocolHandler.encode(response);

                        DatagramPacket sendPacket = new DatagramPacket(
                                responseData, responseData.length,
                                clientAddress, clientPort
                        );

                        synchronized (sendLock) {
                            socket.send(sendPacket);
                        }

                        System.out.println("[UDP] відповідь відправлена " + clientAddress + ":" + clientPort);

                    } catch (IllegalArgumentException e) {
                        System.err.println("[UDP] пошкоджений пакет від "
                                + clientAddress + ":" + clientPort + " — " + e.getMessage());
                    } catch (Exception e) {
                        System.err.println("[UDP] помилка обробки від "
                                + clientAddress + ":" + clientPort + ": " + e.getMessage());
                    }
                });
            }
        } catch (Exception e) {
            System.err.println("[UDP] критична помилка: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
