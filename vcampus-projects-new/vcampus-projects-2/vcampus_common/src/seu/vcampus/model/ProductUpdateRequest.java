package seu.vcampus.model;

public class ProductUpdateRequest {
    private Product product;
    
    public ProductUpdateRequest() {}
    
    public ProductUpdateRequest(Product product) {
        this.product = product;
    }
    
    public Product getProduct() { return product; }
    public void setProduct(Product product) { this.product = product; }
}