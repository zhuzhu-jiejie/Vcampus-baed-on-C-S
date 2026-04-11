package seu.vcampus.model;

import java.util.List;

public class AdminDormitoryInfoResponse {
    private int status;
    private String message;
    private List<StudentInfo> students;
    private List<HygieneRecord> hygieneRecords;
    private double averageScore;
    private int capacity;
    private int currentCount;
    
    // 构造函数
    public AdminDormitoryInfoResponse() {}
    
    // Getter和Setter方法
    public int getStatus() {
        return status;
    }
    
    public void setStatus(int status) {
        this.status = status;
    }
    
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
    
    public List<StudentInfo> getStudents() {
        return students;
    }
    
    public void setStudents(List<StudentInfo> students) {
        this.students = students;
    }
    
    public List<HygieneRecord> getHygieneRecords() {
        return hygieneRecords;
    }
    
    public void setHygieneRecords(List<HygieneRecord> hygieneRecords) {
        this.hygieneRecords = hygieneRecords;
    }
    
    public double getAverageScore() {
        return averageScore;
    }
    
    public void setAverageScore(double averageScore) {
        this.averageScore = averageScore;
    }
    
    public int getCapacity() {
        return capacity;
    }
    
    public void setCapacity(int capacity) {
        this.capacity = capacity;
    }
    
    public int getCurrentCount() {
        return currentCount;
    }
    
    public void setCurrentCount(int currentCount) {
        this.currentCount = currentCount;
    }
}