package seu.vcampus.model;

public class PaymentRequest {
    private String orderId;
    private double amount;
    private String paymentMethod; // 支付方式，如"余额支付"、"微信支付"等
    
    public PaymentRequest() {}
    
    public PaymentRequest(String orderId, double amount, String paymentMethod) {
        this.orderId = orderId;
        this.amount = amount;
        this.paymentMethod = paymentMethod;
    }
    
    // Getter和Setter方法
    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }
    
    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }
    
    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }
}