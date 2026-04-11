package seu.vcampus.model;

import java.io.Serializable;
import java.math.BigDecimal; // 导入BigDecimal类，用于精确存储小数学分

public class MajorInfo implements Serializable{
	private static final long serialVersionUID = 2108034727470686844L;
	private String majorId;
	private String majorName;
	private String undergraduateDuration; // 本科学制（如"4年"）
	private String postgraduateDuration;  // 研究生学制（如"3年"）
	private BigDecimal undergraduateCredits; // 本科学分（对应数据库DECIMAL(5,2)） 
	private BigDecimal postgraduateCredits;  // 研究生学分（对应数据库DECIMAL(5,2)）
	private String collegeId;	// 冗余字段，方便反向查询学院
	
	public MajorInfo() {}
	
	public MajorInfo(String majorId, String majorName, 
	                 String undergraduateDuration, String postgraduateDuration,
	                 BigDecimal undergraduateCredits, BigDecimal postgraduateCredits,
	                 String collegeId) {
		this.majorId = majorId;
		this.majorName = majorName;
		this.undergraduateDuration = undergraduateDuration;
		this.postgraduateDuration = postgraduateDuration;
		this.undergraduateCredits = undergraduateCredits;
		this.postgraduateCredits = postgraduateCredits;
		this.collegeId = collegeId;
	}
	
	public void setMajorId(String majorId) { this.majorId = majorId; }
	public void setMajorName(String majorName) { this.majorName = majorName; }
	public void setUndergraduateDuration(String undergraduateDuration) { this.undergraduateDuration = undergraduateDuration; }
	public void setPostgraduateDuration(String postgraduateDuration) { this.postgraduateDuration = postgraduateDuration; }
	public void setUndergraduateCredits(BigDecimal undergraduateCredits) { this.undergraduateCredits = undergraduateCredits; }
	public void setPostgraduateCredits(BigDecimal postgraduateCredits) { this.postgraduateCredits = postgraduateCredits; }
	public void setCollegeId(String collegeId) { this.collegeId = collegeId; }
	
	public String getMajorId() { return majorId; }
	public String getMajorName() { return majorName; }
	public String getUndergraduateDuration() { return undergraduateDuration; }
	public String getPostgraduateDuration() { return postgraduateDuration; }
	public BigDecimal getUndergraduateCredits() { return undergraduateCredits; }
	public BigDecimal getPostgraduateCredits() { return postgraduateCredits; }
	public String getCollegeId() { return collegeId; }
}
