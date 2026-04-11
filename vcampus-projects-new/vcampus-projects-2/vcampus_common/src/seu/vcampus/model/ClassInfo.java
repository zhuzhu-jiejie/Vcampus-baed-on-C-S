package seu.vcampus.model;

import java.io.Serializable;

public class ClassInfo implements Serializable {
	private static final long serialVersionUID = -6819348781808038432L;
	private String classId;
	private String className;
	private String grade;
	private String educationLevel;
	private String majorId;		// 冗余字段
	
	public ClassInfo() {}
	
	public ClassInfo(String classId, String className, String grade, String educationLevel, String majorId) {
		this.classId = classId;
		this.className = className;
		this.grade = grade;
		this.educationLevel = educationLevel;
		this.majorId = majorId;
	}
	
	public void setClassId(String classId) { this.classId = classId; }
	public void setClassName(String className) { this.className = className; }
	public void setGrade(String grade) { this.grade = grade; }
	public void setEducationLevel(String educationLevel) { this.educationLevel = educationLevel; }
	public void setMajorId(String majorId) { this.majorId = majorId; }
	
	public String getClassId() { return classId; }
	public String getClassName() { return className; }
	public String getGrade() { return grade; }
	public String getEducationLevel() { return educationLevel; }
	public String getMajorId() { return majorId; }

}
