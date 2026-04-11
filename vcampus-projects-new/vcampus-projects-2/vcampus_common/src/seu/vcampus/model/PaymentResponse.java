package seu.vcampus.model;

public class PaymentResponse {
    private int status;
    private String message;
    private String transactionId; // 交易记录ID
    
    public PaymentResponse() {}
    
    public PaymentResponse(int status, String message, String transactionId) {
        this.status = status;
        this.message = message;
        this.transactionId = transactionId;
    }
    
    // Getter和Setter方法
    public int getStatus() { return status; }
    public void setStatus(int status) { this.status = status; }
    
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    
    public String getTransactionId() { return transactionId; }
    public void setTransactionId(String transactionId) { this.transactionId = transactionId; }
}