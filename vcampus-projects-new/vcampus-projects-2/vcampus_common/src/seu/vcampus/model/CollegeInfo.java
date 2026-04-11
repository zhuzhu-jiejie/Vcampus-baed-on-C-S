package seu.vcampus.model;

import java.io.Serializable;

public class CollegeInfo implements Serializable {
	private static final long serialVersionUID = -1842612528450926391L;
	private String collegeId;
	private String collegeName;
	
	public CollegeInfo() {}
	
	public CollegeInfo(String collegeId, String collegeName) {
		this.collegeId = collegeId;
		this.collegeName = collegeName;
	}
	
	public void setCollegeId(String collegeId) { this.collegeId = collegeId; }
	public void setCollegeName(String collegeName) { this.collegeName = collegeName; }
	
	public String getCollegeId() { return collegeId; }
	public String getCollegeName() { return collegeName; }
}