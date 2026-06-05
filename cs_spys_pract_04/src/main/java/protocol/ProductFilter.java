package protocol;

public class ProductFilter {
    private String name;
    private String category;
    private Integer minQuantity;
    private Integer maxQuantity;
    private Double minPrice;
    private Double maxPrice;
    private int page;
    private int pageSize;

    private ProductFilter() {}

    public String getName()         { return name; }
    public String getCategory()     { return category; }
    public Integer getMinQuantity() { return minQuantity; }
    public Integer getMaxQuantity() { return maxQuantity; }
    public Double getMinPrice()     { return minPrice; }
    public Double getMaxPrice()     { return maxPrice; }
    public int getPage()            { return page; }
    public int getPageSize()        { return pageSize; }

    public boolean matches(Product p) {
        if (name != null && !p.getName().toLowerCase().contains(name.toLowerCase()))
            return false;
        if (category != null && !p.getCategory().equalsIgnoreCase(category))
            return false;
        if (minQuantity != null && p.getQuantity() < minQuantity)
            return false;
        if (maxQuantity != null && p.getQuantity() > maxQuantity)
            return false;
        if (minPrice != null && p.getPrice() < minPrice)
            return false;
        if (maxPrice != null && p.getPrice() > maxPrice)
            return false;
        return true;
    }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private final ProductFilter f = new ProductFilter();

        public Builder() {
            f.page = 0;
            f.pageSize = 20;
        }

        public Builder name(String name)              { f.name = name; return this; }
        public Builder category(String category)      { f.category = category; return this; }
        public Builder minQuantity(int min)           { f.minQuantity = min; return this; }
        public Builder maxQuantity(int max)           { f.maxQuantity = max; return this; }
        public Builder minPrice(double min)           { f.minPrice = min; return this; }
        public Builder maxPrice(double max)           { f.maxPrice = max; return this; }
        public Builder page(int page)                 { f.page = page; return this; }
        public Builder pageSize(int size)             { f.pageSize = size; return this; }

        public ProductFilter build() { return f; }
    }
}
