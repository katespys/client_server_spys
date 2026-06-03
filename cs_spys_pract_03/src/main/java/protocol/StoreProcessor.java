package protocol;

import java.nio.charset.StandardCharsets;

public class StoreProcessor {
    private final Store store;

    public StoreProcessor(Store store) {
        this.store = store;
    }

    public Packet process(Packet request) {
        Message message = request.getMessage();
        String payload = new String(message.getData(), StandardCharsets.UTF_8);
        String responseText = "OK";

        try {
            switch (message.getType()) {
                case 1 -> responseText = "QUANTITY:" + store.getQuantity(payload);
                case 2 -> {
                    String[] args = payload.split(",");
                    responseText = store.deductQuantity(args[0], Integer.parseInt(args[1])) ? "OK" : "ERROR:NOT_ENOUGH_STOCK";
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

        Message resMsg = new Message(200, message.getUserId(), responseText.getBytes(StandardCharsets.UTF_8));
        return new Packet(request.getSrc(), request.getPktId(), resMsg);
    }
}