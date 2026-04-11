package seu.vcampus.model;

public class OrderDetailResponse {
    private int status;
    private String message;
    private Order order;
    
    public OrderDetailResponse() {}
    
    public OrderDetailResponse(int status, String message, Order order) {
        this.status = status;
        this.message = message;
        this.order = order;
    }
    
    // Getter和Setter方法
    public int getStatus() { return status; }
    public void setStatus(int status) { this.status = status; }
    
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    
    public Order getOrder() { return order; }
    public void setOrder(Order order) { this.order = order; }
}