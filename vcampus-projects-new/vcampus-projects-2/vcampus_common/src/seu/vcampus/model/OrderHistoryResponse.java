package seu.vcampus.model;

import java.util.List;

public class OrderHistoryResponse {
    private int status;
    private String message;
    private List<Order> orders;
    
    public OrderHistoryResponse() {}
    
    public OrderHistoryResponse(int status, String message, List<Order> orders) {
        this.status = status;
        this.message = message;
        this.orders = orders;
    }
    
    // Getter和Setter方法
    public int getStatus() { return status; }
    public void setStatus(int status) { this.status = status; }
    
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    
    public List<Order> getOrders() { return orders; }
    public void setOrders(List<Order> orders) { this.orders = orders; }
}