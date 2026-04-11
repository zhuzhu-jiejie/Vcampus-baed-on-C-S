package seu.vcampus.model;

import java.io.Serializable;

public class AbsenceInfo implements Serializable{
	private static final long serialVersionUID = 136090837799113145L;
	private String startDate;
	private String endDate;
	
	public AbsenceInfo() {}
	
	public AbsenceInfo(String startDate, String endDate) {
		this.startDate = startDate;
		this.endDate = endDate;
	}
	
	public String getStartDate() { return startDate; }
	public String getEndDate() { return endDate; }
	
	public void setStartDate(String startDate) {
		this.startDate = startDate;
	}
	public void setEndDate(String endDate) {
		this.endDate = endDate;
	}
	
}
