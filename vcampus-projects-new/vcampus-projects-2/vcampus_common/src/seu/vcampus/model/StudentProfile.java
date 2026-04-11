package seu.vcampus.model;

import java.io.Serializable;

public class StudentProfile implements Serializable {
	
	private static final long serialVersionUID = -1142002876322042528L;
	
	private String name;                	// 姓名
    private String studentId;           	// 学号
    private String college;             	// 学院
    private String className;           	// 班级
    private String classId;
    private String counsellor;          	// 辅导员
    private String counsellorId;			// 辅导员Id
    private String major;               	// 专业
    private String mentor;              	// 导师
    private String mentorId;				// 导师Id
    private String levelOfStudy;        	// 培养层级
    private String studentStatus;       	// 学籍状态
    private String admissionDate;       	// 入学日期 yyyy-MM-dd
    private String duration;            	// 学制
    private String grade;               	// 就读年级
    private String expectedGraduationDate; 	// 预期毕业日期
    
    public StudentProfile() {}
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getStudentId() { return studentId; }
    public void setStudentId(String studentId) { this.studentId = studentId; }
    public String getCollege() { return college; }
    public void setCollege(String college) { this.college = college; }
    public String getClassName() { return className; }
    public void setClassName(String className) { this.className = className; }
    public String getClassId() { return classId; }
    public void setClassId(String classId) { this.classId = classId; }
    public String getCounsellor() { return counsellor; }
    public void setCounsellor(String counsellor) { this.counsellor = counsellor; }
    public String getCounsellorId() { return counsellorId; }
    public void setCounsellorId(String counsellorId) { this.counsellorId = counsellorId; }
    public String getMajor() { return major; }
    public void setMajor(String major) { this.major = major; }
    public String getMentor() { return mentor; }    
    public void setMentor(String mentor) { this.mentor = mentor; }
    public String getMentorId() { return mentorId; }
    public void setMentorId(String mentorId) { this.mentorId = mentorId; }
    public String getLevelOfStudy() { return levelOfStudy; }
    public void setLevelOfStudy(String levelOfStudy) { this.levelOfStudy = levelOfStudy; }
    public String getStudentStatus() { return studentStatus; }
    public void setStudentStatus(String studentStatus) { this.studentStatus = studentStatus; }
    public String getAdmissionDate() { return admissionDate; }
    public void setAdmissionDate(String admissionDate) { this.admissionDate = admissionDate; }
    public String getDuration() { return duration; }
    public void setDuration(String duration) { this.duration = duration; }
    public String getGrade() { return grade; }
    public void setGrade(String grade) { this.grade = grade; }
    public String getExpectedGraduationDate() { return expectedGraduationDate; }
    public void setExpectedGraduationDate(String expectedGraduationDate) { this.expectedGraduationDate = expectedGraduationDate; }
}
