package seu.vcampus.model;

import java.io.Serializable;

public class StudentSC implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -312430372919621785L;

	private String studentId;
	private String courseName;
	private String teacherName;
	private int classId;
	
	public StudentSC() {}
	
	public StudentSC(String STUDENTID, String COURSENAME, String TEACHERNAME, int CLASSID) {
		this.classId = CLASSID;
		this.courseName = COURSENAME;
		this.studentId = STUDENTID;
		this.teacherName = TEACHERNAME;
	}
	
	public String getStudentId() {
    	return studentId;
    }
    
    public void setStudentId(String studentId) {
    	this.studentId=studentId;
    }
    
    public String getCourseName() {
    	return courseName;
    }
    
    public void setCourseName(String courseName) {
    	this.courseName=courseName;
    }
    
    public String getTeacherName() {
    	return teacherName;
    }
    
    public void setTeacherName(String teacherName) {
    	this.teacherName=teacherName;
    }
    
    public int getClassId() {
    	return classId;
    }
    
    public void setClassId(int classId) {
    	this.classId=classId;
    }
	
}