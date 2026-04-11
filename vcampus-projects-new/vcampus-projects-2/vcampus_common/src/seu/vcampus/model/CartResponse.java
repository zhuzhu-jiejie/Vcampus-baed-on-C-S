package seu.vcampus.model;

import java.util.List;

public class CartResponse {
    private int status;
    private String message;
    private List<CartItem> items;
    
    public CartResponse() {}
    
    public CartResponse(int status, String message, List<CartItem> items) {
        this.status = status;
        this.message = message;
        this.items = items;
    }
    
    // Getter和Setter方法
    public int getStatus() { return status; }
    public void setStatus(int status) { this.status = status; }
    
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    
    public List<CartItem> getItems() { return items; }
    public void setItems(List<CartItem> items) { this.items = items; }
}