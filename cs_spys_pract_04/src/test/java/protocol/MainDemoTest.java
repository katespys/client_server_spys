package protocol;

import java.nio.charset.StandardCharsets;

public class MainDemoTest {

    public static void main(String[] args) throws InterruptedException {
        System.out.println("ДЕМО: TCP");
        demoTCP();

        Thread.sleep(2000);

        System.out.println("ДЕМО: UDP");
        demoUDP();
    }

    private static void demoTCP() throws InterruptedException {
        Store store = new Store();
        store.addQuantity("гречка", 500);

        // запускаємо сервер
        Thread serverThread = new Thread(() -> new StoreServerTCP(8888, store).start());
        serverThread.setDaemon(true);
        serverThread.start();
        Thread.sleep(500);

        int clientCount = 5;

        // запускаємо кількох клієнтів
        Thread[] clients = new Thread[clientCount];
        for (int i = 0; i < clientCount; i++) {
            final int clientId = i;
            clients[i] = new Thread(() -> {
                StoreClientTCP client = new StoreClientTCP("127.0.0.1", 8888);
                try {
                    Message msg = new Message(2, clientId, "гречка,10".getBytes(StandardCharsets.UTF_8));
                    Packet request = new Packet((byte) 1, clientId, msg);

                    Packet response = client.sendRequest(request);
                    String resText  = new String(response.getMessage().getData(), StandardCharsets.UTF_8);
                    System.out.println("[TCP] Клієнт " + clientId + " отримав: " + resText);

                    Message qMsg     = new Message(1, clientId, "гречка".getBytes(StandardCharsets.UTF_8));
                    Packet qRequest  = new Packet((byte) 1, clientId + 100L, qMsg);
                    Packet qResponse = client.sendRequest(qRequest);
                    String qText     = new String(qResponse.getMessage().getData(), StandardCharsets.UTF_8);
                    System.out.println("[TCP] Клієнт " + clientId + " залишок: " + qText);

                } finally {
                    client.close();
                }
            });
            clients[i].setDaemon(true);
            clients[i].start();
        }

        // чекаємо завершення всіх клієнтів
        for (Thread c : clients) c.join(10_000);

        System.out.println("[TCP] Всі клієнти завершили роботу. Залишок гречки: " + store.getQuantity("гречка"));

        System.out.println("\n[TCP] Тест обриву: запускаємо клієнт, сервер стартує пізніше...");
        Thread lateClient = new Thread(() -> {
            StoreClientTCP client = new StoreClientTCP("127.0.0.1", 9999); // інший порт, сервер ще не запущений
            try {
                Message msg = new Message(1, 99, "гречка".getBytes(StandardCharsets.UTF_8));
                Packet request = new Packet((byte) 1, 9999L, msg);
                Packet response = client.sendRequest(request);
                String resText  = new String(response.getMessage().getData(), StandardCharsets.UTF_8);
                System.out.println("[TCP] Клієнт відновлення отримав: " + resText);
            } finally {
                client.close();
            }
        });
        lateClient.setDaemon(true);
        lateClient.start();

        System.out.println("[TCP] Чекаємо 6 секунд перш ніж запустити сервер на порту 9999...");
        Thread.sleep(6000);
        Store store2 = new Store();
        store2.addQuantity("гречка", 999);
        Thread lateServer = new Thread(() -> new StoreServerTCP(9999, store2).start());
        lateServer.setDaemon(true);
        lateServer.start();

        lateClient.join(10_000);
        System.out.println("[TCP] Тест відновлення завершено.");
    }

    private static void demoUDP() throws InterruptedException {
        Store store = new Store();
        store.addQuantity("борошно", 1000);

        Thread serverThread = new Thread(() -> new StoreServerUDP(8889, store).start());
        serverThread.setDaemon(true);
        serverThread.start();
        Thread.sleep(500);

        int clientCount = 5;
        Thread[] clients = new Thread[clientCount];

        for (int i = 0; i < clientCount; i++) {
            final int clientId = i;
            clients[i] = new Thread(() -> {
                try {
                    StoreClientUDP client = new StoreClientUDP("127.0.0.1", 8889);
                    try {
                        // зняти 20 одиниць борошна
                        Message msg = new Message(2, clientId,
                                "борошно,20".getBytes(StandardCharsets.UTF_8));
                        Packet request  = new Packet((byte) 2, clientId * 1000L, msg);
                        Packet response = client.sendRequest(request);

                        if (response != null) {
                            String resText = new String(response.getMessage().getData(), StandardCharsets.UTF_8);
                            System.out.println("[UDP] Клієнт " + clientId + " отримав: " + resText);
                        } else {
                            System.out.println("[UDP] Клієнт " + clientId + " — відповідь не отримана після MAX_RETRIES.");
                        }

                        // дізнатись залишок
                        Message qMsg     = new Message(1, clientId, "борошно".getBytes(StandardCharsets.UTF_8));
                        Packet qRequest  = new Packet((byte) 2, clientId * 1000L + 1, qMsg);
                        Packet qResponse = client.sendRequest(qRequest);
                        if (qResponse != null) {
                            String qText = new String(qResponse.getMessage().getData(), StandardCharsets.UTF_8);
                            System.out.println("[UDP] Клієнт " + clientId + " залишок: " + qText);
                        }
                    } finally {
                        client.close();
                    }
                } catch (Exception e) {
                    System.err.println("[UDP] Клієнт " + clientId + " помилка: " + e.getMessage());
                }
            });
            clients[i].setDaemon(true);
            clients[i].start();
        }

        for (Thread c : clients) c.join(30_000);
        System.out.println("[UDP] Всі клієнти завершили роботу. Залишок борошна: " + store.getQuantity("борошно"));
    }
}
