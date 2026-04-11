package seu.vcampus.model;

public class CheckoutResponse {
    private int status;       // 1=成功，0=失败
    private String message;   // 提示信息
    private String orderId;   // 订单号（成功时返回）

    // 无参构造（Gson必须）
    public CheckoutResponse() {}

    // 有参构造
    public CheckoutResponse(int status, String message, String orderId) {
        this.status = status;
        this.message = message;
        this.orderId = orderId;
    }

    // Getter和Setter（必须实现）
    public int getStatus() { return status; }
    public void setStatus(int status) { this.status = status; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }
}