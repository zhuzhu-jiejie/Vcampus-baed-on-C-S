// CurrentBill.java
package seu.vcampus.model;



public class CurrentBill {
    private double electricityFee;
    private double waterFee;
    private String status;
    private String month;
    private String deadline;
    
 
    
    public CurrentBill(double electricityFee, double waterFee, String status, String month, String deadline) {
        this.electricityFee = electricityFee;
        this.waterFee = waterFee;
        this.status = status;
        this.month = month;
        this.deadline = deadline;
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
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
    
    public String getMonth() {
        return month;
    }
    
    public void setMonth(String month) {
        this.month = month;
    }
    
    public String getDeadline() {
        return deadline;
    }
    
    public void setDeadline(String deadline) {
        this.deadline = deadline;
    }
}