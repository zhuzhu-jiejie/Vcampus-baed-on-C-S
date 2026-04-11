package seu.vcampus.model;

import java.io.Serializable;

public class StatusChangeApplication implements Serializable {
	private static final long serialVersionUID = 1224960349459243022L;
	private Integer id;
	private String type;
	private String date;
	private String reason;
	private String status;
	
	public StatusChangeApplication() {}
	
	public StatusChangeApplication(Integer id, String type, String date, String reason, String status) {
		this.id = id;
		this.type = type;
		this.date = date;
		this.reason = reason;
		this.status = status;
	}
	
	public Integer getId() { return id; }
	public String getType() { return type; }
	public String getDate() { return date; }
	public String getReason() { return reason; }
	public String getStatus() { return status; }
	
	public void setId(Integer id) { this.id = id; }
	public void setType(String type) { this.type = type; }
	public void setDate(String date) { this.date = date; }
	public void setReason(String reason) { this.reason = reason; }
	public void setStatus(String status) { this.status = status; }
}
