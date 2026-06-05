package protocol;

import java.util.Objects;
import java.util.UUID;

public class Product {
    private String id;
    private String name;
    private String category;
    private int quantity;
    private double price;

    public Product(String name, String category, int quantity, double price) {
        this.id = UUID.randomUUID().toString();
        this.name = name;
        this.category = category;
        this.quantity = quantity;
        this.price = price;
    }

    public Product(String id, String name, String category, int quantity, double price) {
        this.id = id;
        this.name = name;
        this.category = category;
        this.quantity = quantity;
        this.price = price;
    }

    public String getId()           { return id; }
    public String getName()         { return name; }
    public String getCategory()     { return category; }
    public int getQuantity()        { return quantity; }
    public double getPrice()        { return price; }

    public void setName(String name)          { this.name = name; }
    public void setCategory(String category)  { this.category = category; }
    public void setQuantity(int quantity)     { this.quantity = quantity; }
    public void setPrice(double price)        { this.price = price; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Product p)) return false;
        return Objects.equals(id, p.id);
    }

    @Override
    public int hashCode() { return Objects.hash(id); }

    @Override
    public String toString() {
        return "Product{id='" + id + "', name='" + name + "', category='" + category
                + "', quantity=" + quantity + ", price=" + price + '}';
    }
}
