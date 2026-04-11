package seu.vcampus.model;

import java.io.Serializable;

public class StudentRetrieveCondition implements Serializable {
	private static final long serialVersionUID = 382658920286023462L;
	private String studentId;    // 对应byStudentId
    private String name;         // 对应byName
    private String college;      // 对应byCollege
    private String major;        // 对应byMajor
    private String grade;        // 对应byGrade
    private String className;    // 对应byClassName

    // 无参构造+getter+setter
    public StudentRetrieveCondition() {}

    public String getStudentId() { return studentId; }
    public void setStudentId(String studentId) { this.studentId = studentId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getCollege() { return college; }
    public void setCollege(String college) { this.college = college; }
    public String getMajor() { return major; }
    public void setMajor(String major) { this.major = major; }
    public String getGrade() { return grade; }
    public void setGrade(String grade) { this.grade = grade; }
    public String getClassName() { return className; }
    public void setClassName(String className) { this.className = className; }
}