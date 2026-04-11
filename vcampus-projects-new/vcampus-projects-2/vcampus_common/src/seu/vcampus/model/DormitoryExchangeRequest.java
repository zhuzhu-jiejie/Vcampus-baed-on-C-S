package seu.vcampus.model;

public class DormitoryExchangeRequest {
    private String userid;
    private int exchangeType; // 0:退宿, 1:换宿
    private String reason;
    
    // 构造函数
    public DormitoryExchangeRequest() {}
    
    public DormitoryExchangeRequest(String userid, int exchangeType, String reason) {
        this.userid = userid;
        this.exchangeType = exchangeType;
        this.reason = reason;
    }
    
    // Getter和Setter方法
    public String getUserid() {
        return userid;
    }
    
    public void setUserid(String userid) {
        this.userid = userid;
    }
    
    public int getExchangeType() {
        return exchangeType;
    }
    
    public void setExchangeType(int exchangeType) {
        this.exchangeType = exchangeType;
    }
    
    public String getReason() {
        return reason;
    }
    
    public void setReason(String reason) {
        this.reason = reason;
    }
}