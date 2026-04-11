package seu.vcampus.model;

import java.io.Serializable;
import java.math.BigDecimal;

public class StudentGrades implements Serializable {
    private static final long serialVersionUID = 1L; // 序列化必须（避免版本不一致问题）

    // 课程信息（对应course表）
    private String courseId;
    private String courseName;
    private BigDecimal credit; // 对应数据库DECIMAL(4,2)
    private int finalRatio;    // 期末占比（数据库INT）

    // 教学班信息（对应course_section表）
    private Integer sectionId; // 数据库INT → 模型类用Integer（避免String类型转换）
    private String teacherId; // 数据库INT → Integer
    private String teacherName;
    private String semester;
    private String classTime;
    private String sectionStatus;
    

    // 成绩信息（对应student_grade表）
    private Integer gradeId;   // 数据库INT → Integer
    private String studentId;  // 学生ID（数据库VARCHAR）
    private String studentName;// 可选：若有StudentProfile类，可删除（避免冗余）
    private String major;      // 可选：同上
    private BigDecimal regularGrade; // 平时成绩（DECIMAL(5,2)）
    private BigDecimal finalGrade;   // 期末成绩（DECIMAL(5,2)）
    private BigDecimal totalGrade;   // 总成绩（DECIMAL(5,2)）

    public StudentGrades() {}

    public String getCourseId() { return courseId; }
    public void setCourseId(String courseId) { this.courseId = courseId; }
    public String getCourseName() { return courseName; }
    public void setCourseName(String courseName) { this.courseName = courseName; }
    public BigDecimal getCredit() { return credit; }
    public void setCredit(BigDecimal credit) { this.credit = credit; }
    public int getFinalRatio() { return finalRatio; }
    public void setFinalRatio(int finalRatio) { this.finalRatio = finalRatio; }

    public Integer getSectionId() { return sectionId; }
    public void setSectionId(Integer sectionId) { this.sectionId = sectionId; }
    public String getTeacherId() { return teacherId; }
    public void setTeacherId(String teacherId) { this.teacherId = teacherId; }
    public String getTeacherName() { return teacherName; }
    public void setTeacherName(String teacherName) { this.teacherName = teacherName; }
    public String getSemester() { return semester; }
    public void setSemester(String semester) { this.semester = semester; }
    public String getClassTime() { return classTime; }
    public void setClassTime(String classTime) { this.classTime = classTime; }
    public String getSectionStatus() { return sectionStatus; }
    public void setSectionStatus(String sectionStatus) { this.sectionStatus = sectionStatus; }

    public Integer getGradeId() { return gradeId; }
    public void setGradeId(Integer gradeId) { this.gradeId = gradeId; }
    public String getStudentId() { return studentId; }
    public void setStudentId(String studentId) { this.studentId = studentId; }
    public String getStudentName() { return studentName; }
    public void setStudentName(String studentName) { this.studentName = studentName; }
    public String getMajor() { return major; }
    public void setMajor(String major) { this.major = major; }
    public BigDecimal getRegularGrade() { return regularGrade; }
    public void setRegularGrade(BigDecimal regularGrade) { this.regularGrade = regularGrade; }
    public BigDecimal getFinalGrade() { return finalGrade; }
    public void setFinalGrade(BigDecimal finalGrade) { this.finalGrade = finalGrade; }
    public BigDecimal getTotalGrade() { return totalGrade; }
    public void setTotalGrade(BigDecimal totalGrade) { this.totalGrade = totalGrade; }
}