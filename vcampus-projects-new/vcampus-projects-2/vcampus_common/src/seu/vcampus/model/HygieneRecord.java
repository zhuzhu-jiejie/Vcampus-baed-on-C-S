package seu.vcampus.model;

public class HygieneRecord {
    private int score;
    private String date;
    private String inspector;
    private String remark;
    
    // 构造函数
    public HygieneRecord() {}
    
    public HygieneRecord(int score, String date, String inspector, String remark) {
        this.score = score;
        this.date = date;
        this.inspector = inspector;
        this.remark = remark;
    }
    
    // Getter和Setter方法
    public int getScore() {
        return score;
    }
    
    public void setScore(int score) {
        this.score = score;
    }
    
    public String getDate() {
        return date;
    }
    
    public void setDate(String date) {
        this.date = date;
    }
    
    public String getInspector() {
        return inspector;
    }
    
    public void setInspector(String inspector) {
        this.inspector = inspector;
    }
    
    public String getRemark() {
        return remark;
    }
    
    public void setRemark(String remark) {
        this.remark = remark;
    }
}