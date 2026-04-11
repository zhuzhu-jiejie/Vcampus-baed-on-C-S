package seu.vcampus.model;

public class OrderHistoryRequest {
    private String statusFilter; // "all", "pending", "paid", "shipped", "completed", "cancelled"
    
    public OrderHistoryRequest() {}
    
    public OrderHistoryRequest(String statusFilter) {
        this.statusFilter = statusFilter;
    }
    
    // Getter和Setter方法
    public String getStatusFilter() { return statusFilter; }
    public void setStatusFilter(String statusFilter) { this.statusFilter = statusFilter; }
}