package seu.vcampus.model;

import java.io.Serializable;

public class FinalCourse implements Serializable {
	
    /**
	 * 
	 */
	private static final long serialVersionUID = 7871217926329061935L;
	
	private String name;                    // 课程名（如：Java程序设计）
    private String teacherName;             // 授课老师
    private double credit;                  // 学分（如：3.0）
    private String academy;                 //面向学院
    private String major;                   //面向专业
    private int firstWeek;                  //第一周
    private int lastWeek;                   //最后一周
    private int studentType;                //授课对象
    private int studentNum;                 //授课人数
    private String courseID;                //课程ID（唯一标识符，默认-1表示未设置ID）
    private String nature;                  //课程性质（-1代表未设置）
    private String department;              //开课单位
    private String schedule;                //排课信息
    private int isS;                        //是否排课
    private int classID;                    //教学班ID
    
    public FinalCourse() {}
    
    public FinalCourse(String NAME, String TEACHERNAME, double CREDIT, String ACADEMY, String MAJOR,
    		int FIRSTWEEK, int LASTWEEK, int STUDENTTYPE, int STUDENTNUM, String ID, String NATURE, String DEPARTMENT, String SCHEDULE, int ISS, int CLASSID) {
    	name = NAME;             
        teacherName = TEACHERNAME;             
        credit = CREDIT;               
        academy =ACADEMY;               
        major = MAJOR;                 
        firstWeek = FIRSTWEEK;                 
        lastWeek = LASTWEEK;  
        studentType = STUDENTTYPE;
        studentNum = STUDENTNUM;
        courseID = ID;
        nature = NATURE; 
        department = DEPARTMENT;
        schedule = SCHEDULE;
        isS = ISS;
        classID = CLASSID;
    }
    
    // Getter和Setter
    public String getName() {
    	return name;
    }
    
    public void setName(String name) {
    	this.name=name;
    }
    
    public String getTeacherName() {
    	return teacherName;
    }
    
    public void setTeacherName(String teacherName) {
    	this.teacherName=teacherName;
    }
    
    public double getCredit() {
    	return credit;
    }
    
    public void setCredit(double credit) {
    	this.credit=credit;
    }
    
    public String getAcademy() {
    	return academy;
    }
    
    public void setAcademy(String academy) {
    	this.academy=academy;
    }
    
    public String getMajor() {
    	return major;
    }
    
    public void setMajor(String major) {
    	this.major=major;
    }
    
    public int getFirstWeek() {
    	return firstWeek;
    }
    
    public void setFirstWeek(int firstWeek) {
    	this.firstWeek=firstWeek;
    }
    
    public int getLastWeek() {
    	return lastWeek;
    }
    
    public void setLastWeek(int lastWeek) {
    	this.lastWeek=lastWeek;
    }
    
    public int getStudentType() {
    	return studentType;
    }
    
    public void setStudentType(int studentType) {
    	this.studentType=studentType;
    }
    
    public int getStudentNum() {
    	return studentNum;
    }
    
    public void setStudentNum(int studentNum) {
    	this.studentNum=studentNum;
    }
    
    public String getCourseID() {
    	return courseID;
    }
    
    public void setCourseID(String courseID) {
    	this.courseID=courseID;
    }
    
    public String getNature() {
    	return nature;
    }
    
    public void setNature(String nature) {
    	this.nature=nature;
    }
    
    public String getDepartment() {
    	return department;
    }
    
    public void setDepartment(String department) {
    	this.department=department;
    }
    
    public String getSchedule() {
    	return schedule;
    }
    
    public void setSchedule(String schedule) {
    	this.schedule=schedule;
    }
    
    public int getIsS() {
    	return isS;
    }
    
    public void setIsS(int isS) {
    	this.isS=isS;
    }
    
    public int getClassID() {
    	return classID;
    }
    
    public void setClassID(int classID) {
    	this.classID=classID;
    }
    
}