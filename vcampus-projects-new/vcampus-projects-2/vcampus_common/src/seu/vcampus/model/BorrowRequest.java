// BorrowRequest.java
package seu.vcampus.model;

public class BorrowRequest {
    private String userId;
    private String callNumber;
    private String operationType; // BORROW/RETURN/RENEW

    // Getter和Setter方法
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    
    public String getCallNumber() { return callNumber; }
    public void setCallNumber(String callNumber) { this.callNumber = callNumber; }
    
    public String getOperationType() { return operationType; }
    public void setOperationType(String operationType) { this.operationType = operationType; }
}