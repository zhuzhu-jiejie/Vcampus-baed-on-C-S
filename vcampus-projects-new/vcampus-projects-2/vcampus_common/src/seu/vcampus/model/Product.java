package seu.vcampus.model;

public class Product {
    private String id;
    private String name;
    private String brand;
    private double price;
    private double originalPrice;
    private double rating;
    private String category;
    private String description;
    private int stock;
    private String expirationDate;
    private String imageUrl;
    
    // 添加默认构造函数
    public Product() {
    }
    
    public Product(String id, String name, double price, double originalPrice, 
                  double rating, String category, String description) {
        this.id = id;
        this.name = name;
        this.price = price;
        this.originalPrice = originalPrice;
        this.rating = rating;
        this.category = category;
        this.description = description;
    }
    
    // 全参数构造函数
    public Product(String id, String name, String brand, double price, double originalPrice, 
                  double rating, String category, String description, int stock, 
                  String expirationDate, String imageUrl) {
        this.id = id;
        this.name = name;
        this.brand = brand;
        this.price = price;
        this.originalPrice = originalPrice;
        this.rating = rating;
        this.category = category;
        this.description = description;
        this.stock = stock;
        this.expirationDate = expirationDate;
        this.imageUrl = imageUrl;
    }
    
    // Getter和Setter方法
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public String getBrand() { return brand; }
    public void setBrand(String brand) { this.brand = brand; }
    
    public double getPrice() { return price; }
    public void setPrice(double price) { this.price = price; }
    
    public double getOriginalPrice() { return originalPrice; }
    public void setOriginalPrice(double originalPrice) { this.originalPrice = originalPrice; }
    
    public double getRating() { return rating; }
    public void setRating(double rating) { this.rating = rating; }
    
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public int getStock() { return stock; }
    public void setStock(int stock) { this.stock = stock; }
    
    public String getExpirationDate() { return expirationDate; }
    public void setExpirationDate(String expirationDate) { this.expirationDate = expirationDate; }
    
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    
    @Override
    public String toString() {
        return name + " (¥" + price + ")";
    }
}