package seu.vcampus.model;

public class RepairInfo {
    private int id;
    private String studentId;  // 新增字段
    private String dormBuilding;
    private String dormRoom;
    private String description;
    private String status;
    private String date;
    
    // 默认构造函数
    public RepairInfo() {}
    
    // 带参数的构造函数
    public RepairInfo(int id, String studentId, String dormBuilding, String dormRoom, 
                     String description, String status, String date) {
        this.id = id;
        this.studentId = studentId;
        this.dormBuilding = dormBuilding;
        this.dormRoom = dormRoom;
        this.description = description;
        this.status = status;
        this.date = date;
    }
    
    // Getter和Setter方法
    public int getId() {
        return id;
    }
    
    public void setId(int id) {
        this.id = id;
    }
    
    public String getStudentId() {
        return studentId;
    }
    
    public void setStudentId(String studentId) {
        this.studentId = studentId;
    }
    
    public String getDormBuilding() {
        return dormBuilding;
    }
    
    public void setDormBuilding(String dormBuilding) {
        this.dormBuilding = dormBuilding;
    }
    
    public String getDormRoom() {
        return dormRoom;
    }
    
    public void setDormRoom(String dormRoom) {
        this.dormRoom = dormRoom;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
    
    public String getDate() {
        return date;
    }
    
    public void setDate(String date) {
        this.date = date;
    }
    
    @Override
    public String toString() {
        return "RepairInfo{" +
                "id=" + id +
                ", studentId='" + studentId + '\'' +
                ", dormBuilding='" + dormBuilding + '\'' +
                ", dormRoom='" + dormRoom + '\'' +
                ", description='" + description + '\'' +
                ", status='" + status + '\'' +
                ", date='" + date + '\'' +
                '}';
    }
}