package seu.vcampus.model;

import java.util.List;

public class ProductInfoResponse {
    private int status;
    private String message;
    private List<Product> products;
    
    public ProductInfoResponse() {}
    
    public ProductInfoResponse(int status, List<Product> products) {
        this.status = status;
        this.products = products;
    }
    
    public ProductInfoResponse(int status, String message, List<Product> products) {
        this.status = status;
        this.message = message;
        this.products = products;
    }
    
    public int getStatus() { return status; }
    public void setStatus(int status) { this.status = status; }
    
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    
    public List<Product> getProducts() { return products; }
    public void setProducts(List<Product> products) { this.products = products; }
}