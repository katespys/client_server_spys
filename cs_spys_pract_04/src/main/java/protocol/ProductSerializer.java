package protocol;

import java.util.List;

public class ProductSerializer {

    public static String productToJson(Product p) {
        return "{\"id\":\"" + esc(p.getId()) + "\""
                + ",\"name\":\"" + esc(p.getName()) + "\""
                + ",\"category\":\"" + esc(p.getCategory()) + "\""
                + ",\"quantity\":" + p.getQuantity()
                + ",\"price\":" + p.getPrice()
                + "}";
    }

    public static Product productFromJson(String json) {
        String id       = str(json, "id");
        String name     = str(json, "name");
        String category = str(json, "category");
        int quantity    = integer(json, "quantity");
        double price    = decimal(json, "price");

        if (id != null && !id.isBlank()) {
            return new Product(id, name, category, quantity, price);
        }
        return new Product(name, category, quantity, price);
    }

    public static String pageResultToJson(PageResult<Product> result) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\"page\":").append(result.getPage())
                .append(",\"pageSize\":").append(result.getPageSize())
                .append(",\"totalItems\":").append(result.getTotalItems())
                .append(",\"totalPages\":").append(result.getTotalPages())
                .append(",\"items\":[");
        List<Product> items = result.getItems();
        for (int i = 0; i < items.size(); i++) {
            sb.append(productToJson(items.get(i)));
            if (i < items.size() - 1) sb.append(",");
        }
        sb.append("]}");
        return sb.toString();
    }

    public static ProductFilter filterFromJson(String json) {
        ProductFilter.Builder b = ProductFilter.builder();
        String name     = str(json, "name");
        String category = str(json, "category");
        Integer minQty  = integerOrNull(json, "minQuantity");
        Integer maxQty  = integerOrNull(json, "maxQuantity");
        Double minP     = decimalOrNull(json, "minPrice");
        Double maxP     = decimalOrNull(json, "maxPrice");
        Integer page    = integerOrNull(json, "page");
        Integer size    = integerOrNull(json, "pageSize");

        if (name != null)     b.name(name);
        if (category != null) b.category(category);
        if (minQty != null)   b.minQuantity(minQty);
        if (maxQty != null)   b.maxQuantity(maxQty);
        if (minP != null)     b.minPrice(minP);
        if (maxP != null)     b.maxPrice(maxP);
        if (page != null)     b.page(page);
        if (size != null)     b.pageSize(size);
        return b.build();
    }

    private static String esc(String s) {
        return s == null ? "" : s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private static String str(String json, String field) {
        String key = "\"" + field + "\":\"";
        int start = json.indexOf(key);
        if (start < 0) return null;
        start += key.length();
        int end = start;
        while (end < json.length()) {
            if (json.charAt(end) == '"' && json.charAt(end - 1) != '\\') break;
            end++;
        }
        return json.substring(start, end).replace("\\\"", "\"").replace("\\\\", "\\");
    }

    private static int integer(String json, String field) {
        Integer v = integerOrNull(json, field);
        return v == null ? 0 : v;
    }

    public static Integer integerOrNull(String json, String field) {
        String key = "\"" + field + "\":";
        int start = json.indexOf(key);
        if (start < 0) return null;
        start += key.length();
        while (start < json.length() && json.charAt(start) == ' ') start++;
        if (start < json.length() && json.charAt(start) == '"') return null;
        int end = start;
        while (end < json.length() && (Character.isDigit(json.charAt(end)) || json.charAt(end) == '-')) end++;
        if (start == end) return null;
        try { return Integer.parseInt(json.substring(start, end)); }
        catch (NumberFormatException e) { return null; }
    }

    private static double decimal(String json, String field) {
        Double v = decimalOrNull(json, field);
        return v == null ? 0.0 : v;
    }

    public static Double decimalOrNull(String json, String field) {
        String key = "\"" + field + "\":";
        int start = json.indexOf(key);
        if (start < 0) return null;
        start += key.length();
        while (start < json.length() && json.charAt(start) == ' ') start++;
        if (start < json.length() && json.charAt(start) == '"') return null;
        int end = start;
        while (end < json.length() && (Character.isDigit(json.charAt(end))
                || json.charAt(end) == '.' || json.charAt(end) == '-')) end++;
        if (start == end) return null;
        try { return Double.parseDouble(json.substring(start, end)); }
        catch (NumberFormatException e) { return null; }
    }
}