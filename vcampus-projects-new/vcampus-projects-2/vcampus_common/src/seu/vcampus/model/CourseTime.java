package seu.vcampus.model;

import java.io.Serializable;

public class CourseTime implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -4667741362485866024L;
	
	private String courseName;
	private String teacherName;
	private int classNum;//教学班编号
	private int weekDay;//周几上课
	private int startTime;
	private int endTime;
	private String classroom;
	
	public CourseTime() {}
	
	public CourseTime(String COURSENAME, String TEACHERNAME, int CLASSNUM, int WEEKDAY, int STARTTIME, int ENDTIME, String CLASSROOM) {
		this.courseName = COURSENAME;
		this.teacherName = TEACHERNAME;
		this.classNum = CLASSNUM;
		this.weekDay = WEEKDAY;
		this.startTime = STARTTIME;
		this.endTime = ENDTIME;
		this.classroom = CLASSROOM;
	}
	
	public String getCourseName() {
		return courseName;
	}
	
	public void setCourseName(String courseName) {
		this.courseName = courseName;
	}
	
	public String getTeacherName() {
		return teacherName;
	}
	
	public void setTeacherName(String teacherName) {
		this.teacherName = teacherName;
	}
	
	public int getClassNum() {
		return classNum;
	}
	
	public void setClassNum(int classNum) {
		this.classNum = classNum;
	}
	
	public int getWeekDay() {
		return weekDay;
	}
	
	public void setWeekDay(int weekDay) {
		this.weekDay = weekDay;
	}
	
	public int getStartTime() {
		return startTime;
	}
	
	public void setStartTime(int startTime) {
		this.startTime = startTime;
	}
	
	public int getEndTime() {
		return endTime;
	}
	
	public void setEndTime(int endTime) {
		this.endTime = endTime;
	}
	
	public String getClassroom() {
		return classroom;
	}
	
	public void setClassroom(String classroom) {
		this.classroom = classroom;
	}
	
}

