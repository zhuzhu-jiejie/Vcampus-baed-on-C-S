package seu.vcampus.model;

public class CartRequest {
    private String action; // "get", "add", "update", "remove", "clear"
    private String productId;
    private int quantity;
    
    public CartRequest() {}
    
    public CartRequest(String action, String productId, int quantity) {
        this.action = action;
        this.productId = productId;
        this.quantity = quantity;
    }
    
    // Getter和Setter方法
    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }
    
    public String getProductId() { return productId; }
    public void setProductId(String productId) { this.productId = productId; }
    
    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
}