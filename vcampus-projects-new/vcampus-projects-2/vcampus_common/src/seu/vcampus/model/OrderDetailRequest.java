package seu.vcampus.model;

public class OrderDetailRequest {
    private String orderId;
    
    public OrderDetailRequest() {}
    
    public OrderDetailRequest(String orderId) {
        this.orderId = orderId;
    }
    
    // Getter和Setter方法
    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }
}