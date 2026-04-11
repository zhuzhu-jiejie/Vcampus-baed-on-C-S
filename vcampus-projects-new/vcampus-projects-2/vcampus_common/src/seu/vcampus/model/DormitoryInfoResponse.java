package seu.vcampus.model;

import java.util.List;

public class DormitoryInfoResponse {
    private int status; // 1成功，0失败
    private String message;
    private StudentInfo studentInfo;
    private List<StudentInfo> roommates;
    private List<HygieneRecord> hygieneRecords; // 新增卫生评分记录
    

 // 在DormitoryInfoResponse.java中添加
 public DormitoryInfoResponse() {
     // 默认构造函数
 }
 public DormitoryInfoResponse(int status, String message, StudentInfo studentInfo, 
         List<StudentInfo> roommates, List<HygieneRecord> hygieneRecords) {
this.status = status;
this.message = message;
this.studentInfo = studentInfo;
this.roommates = roommates;
this.hygieneRecords = hygieneRecords;
}
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
    
    public StudentInfo getStudentInfo() {
        return studentInfo;
    }
    
    public void setStudentInfo(StudentInfo studentInfo) {
        this.studentInfo = studentInfo;
    }
    
    public List<StudentInfo> getRoommates() {
        return roommates;
    }
    
    public void setRoommates(List<StudentInfo> roommates) {
        this.roommates = roommates;
    }
    public List<HygieneRecord> getHygieneRecords() {
        return hygieneRecords;
    }
    
    public void setHygieneRecords(List<HygieneRecord> hygieneRecords) {
        this.hygieneRecords = hygieneRecords;
    }
}