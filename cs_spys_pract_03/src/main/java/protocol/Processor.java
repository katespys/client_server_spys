package protocol;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class Processor implements Runnable {
    private final BlockingQueue<Packet> inputQueue;
    private final BlockingQueue<Packet> outputQueue;
    private final Store store;
    private final AtomicBoolean running;

    public Processor(BlockingQueue<Packet> inputQueue, BlockingQueue<Packet> outputQueue, Store store, AtomicBoolean running) {
        this.inputQueue = inputQueue;
        this.outputQueue = outputQueue;
        this.store = store;
        this.running = running;
    }

    @Override
    public void run() {
        while (running.get() || !inputQueue.isEmpty()) {
            try {
                Packet packet = inputQueue.poll(50, TimeUnit.MILLISECONDS);
                if (packet != null) {
                    Message message = packet.getMessage();
                    String payload = new String(message.getData(), StandardCharsets.UTF_8);
                    String responseText = "OK";
                    try {
                        switch (message.getType()) {
                            case 1 -> responseText = "QUANTITY:" + store.getQuantity(payload);
                            case 2 -> {
                                String[] args = payload.split(",");
                                responseText = store.deductQuantity(args[0], Integer.parseInt(args[1]))
                                        ? "OK" : "ERROR:NOT_ENOUGH_STOCK";
                            }
                            case 3 -> {
                                String[] args = payload.split(",");
                                store.addQuantity(args[0], Integer.parseInt(args[1]));
                            }
                            case 4 -> store.addGroup(payload);
                            case 5 -> {
                                String[] args = payload.split(",");
                                store.addProductToGroup(args[0], args[1]);
                            }
                            case 6 -> {
                                String[] args = payload.split(",");
                                store.setPrice(args[0], Double.parseDouble(args[1]));
                            }
                            default -> responseText = "ERROR:UNKNOWN_COMMAND";
                        }
                    } catch (Exception e) {
                        responseText = "ERROR:" + e.getMessage();
                    }

                    Message resMsg = new Message(200, message.getUserId(),
                            responseText.getBytes(StandardCharsets.UTF_8));
                    try {
                        outputQueue.put(new Packet(packet.getSrc(), packet.getPktId(), resMsg));
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }
}
