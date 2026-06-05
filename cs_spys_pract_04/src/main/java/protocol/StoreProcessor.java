package protocol;

import java.nio.charset.StandardCharsets;
import java.util.Optional;

public class StoreProcessor {
    private final Store store;
    private final ProductService productService;

    public StoreProcessor(Store store) {
        this.store = store;
        this.productService = new ProductService();
    }

    public StoreProcessor(Store store, ProductService productService) {
        this.store = store;
        this.productService = productService;
    }

    public Packet process(Packet request) {
        Message message = request.getMessage();
        String payload = new String(message.getData(), StandardCharsets.UTF_8);
        String responseText;

        try {
            responseText = handleMessage(message, payload);
        } catch (Exception e) {
            responseText = "ERROR:" + e.getMessage();
        }

        Message resMsg = new Message(200, message.getUserId(),
                responseText.getBytes(StandardCharsets.UTF_8));
        return new Packet(request.getSrc(), request.getPktId(), resMsg);
    }

    private String handleMessage(Message message, String payload) {
        return switch (message.getType()) {
            case 1 -> "QUANTITY:" + store.getQuantity(payload);
            case 2 -> {
                String[] args = payload.split(",");
                yield store.deductQuantity(args[0], Integer.parseInt(args[1]))
                        ? "OK" : "ERROR:NOT_ENOUGH_STOCK";
            }
            case 3 -> {
                String[] args = payload.split(",");
                store.addQuantity(args[0], Integer.parseInt(args[1]));
                yield "OK";
            }
            case 4 -> { store.addGroup(payload); yield "OK"; }
            case 5 -> {
                String[] args = payload.split(",");
                store.addProductToGroup(args[0], args[1]);
                yield "OK";
            }
            case 6 -> {
                String[] args = payload.split(",");
                store.setPrice(args[0], Double.parseDouble(args[1]));
                yield "OK";
            }
            case 10 -> {
                Product parsed = ProductSerializer.productFromJson(payload);
                yield ProductSerializer.productToJson(
                        productService.create(parsed.getName(), parsed.getCategory(),
                                parsed.getQuantity(), parsed.getPrice()));
            }
            case 11 -> {
                Optional<Product> p = productService.findById(payload.trim());
                yield p.map(ProductSerializer::productToJson).orElse("ERROR:NOT_FOUND");
            }
            case 12 -> {
                Product parsed = ProductSerializer.productFromJson(payload);
                Integer quantityToUpdate = ProductSerializer.integerOrNull(payload, "quantity");
                Double priceToUpdate = ProductSerializer.decimalOrNull(payload, "price");
                yield ProductSerializer.productToJson(
                        productService.update(parsed.getId(), parsed.getName(), parsed.getCategory(),
                                quantityToUpdate, priceToUpdate));
            }
            case 13 -> productService.delete(payload.trim()) ? "OK" : "ERROR:NOT_FOUND";
            case 14 -> {
                ProductFilter filter = ProductSerializer.filterFromJson(payload);
                PageResult<Product> result = productService.search(filter);
                yield ProductSerializer.pageResultToJson(result);
            }
            case 15 -> {
                StringBuilder sb = new StringBuilder("[");
                var all = productService.getAll();
                for (int i = 0; i < all.size(); i++) {
                    sb.append(ProductSerializer.productToJson(all.get(i)));
                    if (i < all.size() - 1) sb.append(",");
                }
                sb.append("]");
                yield sb.toString();
            }
            default -> "ERROR:UNKNOWN_COMMAND";
        };
    }
}