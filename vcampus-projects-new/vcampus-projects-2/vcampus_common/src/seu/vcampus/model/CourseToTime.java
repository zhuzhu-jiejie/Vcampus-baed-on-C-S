package seu.vcampus.model;

import java.io.Serializable;

public class CourseToTime implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 6352964345839316228L;

	private String courseName;
	private String teacherName;
	private int classId;
	private int fWeek;
	private int eWeek;
	private int day;
	private int fTime;
	private int eTime;
	
	public CourseToTime() {}
	
	public CourseToTime(String COURSENAME, String TEACHERNAME, int CLASSID, int FWEEK, int EWEEK, int DAY, int FTIME, int ETIME) {
		this.courseName = COURSENAME;
		this.teacherName = TEACHERNAME;
		this.classId = CLASSID;
		this.fWeek = FWEEK;
		this.eWeek = EWEEK;
		this.day = DAY;
		this.fTime = FTIME;
		this.eTime = ETIME;
	}
	
	public String getCourseName() {return courseName;}
	public void setCourseName(String courseName) {this.courseName = courseName;}
	
	public String getTeacherName() {return teacherName;}
	public void setTeacherName(String teacherName) {this.teacherName = teacherName;}
	
	public int getClassId() {return classId;}
	public void setClassId(int classId) {this.classId = classId;}
	
	public int getFWeek() {return fWeek;}
	public void setFWeek(int fWeek) {this.fWeek = fWeek;}
	
	public int getEWeek() {return eWeek;}
	public void setEWeek(int eWeek) {this.eWeek = eWeek;}
	
	public int getDay() {return day;}
	public void setDay(int day) {this.day = day;}
	
	public int getFTime() {return fTime;}
	public void setFTime(int fTime) {this.fTime = fTime;}
	
	public int getETime() {return eTime;}
	public void setETime(int eTime) {this.eTime = eTime;}
}