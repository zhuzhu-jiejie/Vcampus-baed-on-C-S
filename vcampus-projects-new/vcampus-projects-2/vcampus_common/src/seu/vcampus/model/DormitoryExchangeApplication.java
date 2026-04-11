// DormitoryExchangeApplication.java
package seu.vcampus.model;

import java.sql.Timestamp;

public class DormitoryExchangeApplication {
    private int id;
    private String studentId;
    private String studentName; // 新增字段，学生姓名
    private String building;
    private String room;
    private String bed;
    private String targetBuilding; // 新增字段，目标楼栋（换宿用）
    private String targetRoom;     // 新增字段，目标房间（换宿用）
    private int exchangeType;
    private String reason;
    private Timestamp applyTime;
    private int status;
    private String feedback; // 新增字段

    // 构造函数
    public DormitoryExchangeApplication() {}

    // Getter和Setter方法
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getStudentId() { return studentId; }
    public void setStudentId(String studentId) { this.studentId = studentId; }

    public String getStudentName() { return studentName; }
    public void setStudentName(String studentName) { this.studentName = studentName; }

    public String getBuilding() { return building; }
    public void setBuilding(String building) { this.building = building; }

    public String getRoom() { return room; }
    public void setRoom(String room) { this.room = room; }

    public String getBed() { return bed; }
    public void setBed(String bed) { this.bed = bed; }

    public String getTargetBuilding() { return targetBuilding; }
    public void setTargetBuilding(String targetBuilding) { this.targetBuilding = targetBuilding; }

    public String getTargetRoom() { return targetRoom; }
    public void setTargetRoom(String targetRoom) { this.targetRoom = targetRoom; }

    public int getExchangeType() { return exchangeType; }
    public void setExchangeType(int exchangeType) { this.exchangeType = exchangeType; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public Timestamp getApplyTime() { return applyTime; }
    public void setApplyTime(Timestamp applyTime) { this.applyTime = applyTime; }

    public int getStatus() { return status; }
    public void setStatus(int status) { this.status = status; }

    public String getFeedback() { return feedback; }
    public void setFeedback(String feedback) { this.feedback = feedback; }

    // 状态文字描述
    public String getStatusText() {
        switch (status) {
            case 0: return "待审核";
            case 1: return "已通过";
            case 2: return "已拒绝";
            default: return "未知状态";
        }
    }

    // 类型文字描述
    public String getTypeText() {
        return exchangeType == 0 ? "退宿" : "换宿";
    }
}