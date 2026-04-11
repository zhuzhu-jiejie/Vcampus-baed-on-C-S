package seu.vcampus.model;

public class ProductDeleteRequest {
    private String productId;
    
    public ProductDeleteRequest() {}
    
    public ProductDeleteRequest(String productId) {
        this.productId = productId;
    }
    
    public String getProductId() { return productId; }
    public void setProductId(String productId) { this.productId = productId; }
}