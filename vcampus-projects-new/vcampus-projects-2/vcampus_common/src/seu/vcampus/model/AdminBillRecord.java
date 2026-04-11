package seu.vcampus.model;

public class AdminBillRecord {
    private String building;
    private String room;
    private String month;
    private double electricityFee;
    private double waterFee;
    private double totalFee;
    private String status;
    private String deadline;
    private String reminderStatus;
    private String lastReminderTime;
    
    // 构造函数
    public AdminBillRecord() {}
    
    // Getter和Setter方法
    public String getBuilding() {
        return building;
    }
    
    public void setBuilding(String building) {
        this.building = building;
    }
    
    public String getRoom() {
        return room;
    }
    
    public void setRoom(String room) {
        this.room = room;
    }
    
    public String getMonth() {
        return month;
    }
    
    public void setMonth(String month) {
        this.month = month;
    }
    
    public double getElectricityFee() {
        return electricityFee;
    }
    
    public void setElectricityFee(double electricityFee) {
        this.electricityFee = electricityFee;
    }
    
    public double getWaterFee() {
        return waterFee;
    }
    
    public void setWaterFee(double waterFee) {
        this.waterFee = waterFee;
    }
    
    public double getTotalFee() {
        return totalFee;
    }
    
    public void setTotalFee(double totalFee) {
        this.totalFee = totalFee;
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
    
    public String getDeadline() {
        return deadline;
    }
    
    public void setDeadline(String deadline) {
        this.deadline = deadline;
    }
    
    public String getReminderStatus() {
        return reminderStatus;
    }
    
    public void setReminderStatus(String reminderStatus) {
        this.reminderStatus = reminderStatus;
    }
    
    public String getLastReminderTime() {
        return lastReminderTime;
    }
    
    public void setLastReminderTime(String lastReminderTime) {
        this.lastReminderTime = lastReminderTime;
    }
}