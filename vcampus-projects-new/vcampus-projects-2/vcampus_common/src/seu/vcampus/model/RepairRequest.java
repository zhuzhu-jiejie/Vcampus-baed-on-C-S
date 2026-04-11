package seu.vcampus.model;

public class RepairRequest {
    private String userid;
    private String description;
    
    // 构造函数
    public RepairRequest() {}
    
    public RepairRequest(String userid, String description) {
        this.userid = userid;
        this.description = description;
    }
    
    // Getter和Setter方法
    public String getUserid() {
        return userid;
    }
    
    public void setUserid(String userid) {
        this.userid = userid;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
}