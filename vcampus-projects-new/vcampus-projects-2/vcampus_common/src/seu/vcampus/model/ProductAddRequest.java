package seu.vcampus.model;

public class ProductAddRequest {
    private Product product;
    
    public ProductAddRequest() {}
    
    public ProductAddRequest(Product product) {
        this.product = product;
    }
    
    public Product getProduct() { return product; }
    public void setProduct(Product product) { this.product = product; }
}