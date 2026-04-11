package seu.vcampus.model;

import java.util.List;

public class CheckoutRequest {
    private double totalAmount;
    private List<CartItem> items;
    
    public CheckoutRequest() {}
    
    public CheckoutRequest(double totalAmount, List<CartItem> items) {
        this.totalAmount = totalAmount;
        this.items = items;
    }
    
    // Getter和Setter方法
    public double getTotalAmount() { return totalAmount; }
    public void setTotalAmount(double totalAmount) { this.totalAmount = totalAmount; }
    
    public List<CartItem> getItems() { return items; }
    public void setItems(List<CartItem> items) { this.items = items; }
}