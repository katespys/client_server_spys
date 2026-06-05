package protocol;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.util.Arrays;

public class StoreClientUDP {
    private final String host;
    private final int port;
    private final DatagramSocket socket;

    private static final int MAX_RETRIES = 5;
    private static final int TIMEOUT_MS  = 2000;
    private static final int BUFFER_SIZE = 8192;

    public StoreClientUDP(String host, int port) throws Exception {
        this.host   = host;
        this.port   = port;
        this.socket = new DatagramSocket();
        this.socket.setSoTimeout(TIMEOUT_MS);
    }

    public Packet sendRequest(Packet request) {
        try {
            InetAddress address = InetAddress.getByName(host);
            byte[] sendData = ProtocolHandler.encode(request);
            DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, address, port);

            int staleCount = 0;
            for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
                try {
                    System.out.println("[UDP] відправка pktId=" + request.getPktId()
                            + ", спроба " + attempt + "/" + MAX_RETRIES);
                    socket.send(sendPacket);

                    byte[] receiveBuffer = new byte[BUFFER_SIZE];
                    DatagramPacket receivePacket = new DatagramPacket(receiveBuffer, receiveBuffer.length);
                    socket.receive(receivePacket);

                    byte[] actualData = Arrays.copyOf(receivePacket.getData(), receivePacket.getLength());
                    Packet response = ProtocolHandler.decode(actualData);

                    if (response.getPktId() != request.getPktId()) {
                        System.out.println("[UDP] стара відповідь pktId=" + response.getPktId()
                                + ", очікувалось " + request.getPktId() + ", ігнорую");
                        if (++staleCount >= MAX_RETRIES) break;
                        attempt--;
                        continue;
                    }

                    System.out.println("[UDP] відповідь отримана за " + attempt + " спроб(и)");
                    return response;

                } catch (SocketTimeoutException e) {
                    System.out.println("[UDP] таймаут, залишилось спроб: " + (MAX_RETRIES - attempt));
                } catch (Exception e) {
                    System.out.println("[UDP] помилка отримання відповіді: " + e.getMessage());
                }
            }

            System.err.println("[UDP] не вдалося отримати відповідь після " + MAX_RETRIES + " спроб");

        } catch (Exception e) {
            System.err.println("[UDP] критична помилка: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

    public void close() {
        if (socket != null && !socket.isClosed()) {
            socket.close();
        }
    }
}
