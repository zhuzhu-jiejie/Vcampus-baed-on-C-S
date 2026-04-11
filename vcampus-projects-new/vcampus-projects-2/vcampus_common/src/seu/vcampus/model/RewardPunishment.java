package seu.vcampus.model;

import java.io.Serializable;

public class RewardPunishment implements Serializable {
	private static final long serialVersionUID = 5019008840813509495L;
	private String id;
	private String type;
    private String title;
    private String reason;
    private String awardingOrganization;
    private String effectiveDate;
    private String status;

    // 无参构造函数
    public RewardPunishment() {}

    // 有参构造函数
    public RewardPunishment(String id, String type, String title, String reason, String awardingOrganization, String effectiveDate, String status) {
        this.id = id;
    	this.type = type;
        this.title = title;
        this.reason = reason;
        this.awardingOrganization = awardingOrganization;
        this.effectiveDate = effectiveDate;
        this.status = status;
    }

    // Getter 和 Setter 方法
    public String getId() {
    	return id;
    }
    
    public void setId(String id) {
    	this.id = id;
    }
    
    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getAwardingOrganization() {
        return awardingOrganization;
    }

    public void setAwardingOrganization(String awardingOrganization) {
        this.awardingOrganization = awardingOrganization;
    }

    public String getEffectiveDate() {
        return effectiveDate;
    }

    public void setEffectiveDate(String effectiveDate) {
        this.effectiveDate = effectiveDate;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
