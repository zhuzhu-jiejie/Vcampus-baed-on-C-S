// HistoryBill.java
package seu.vcampus.model;



public class HistoryBill {
    private String month;
    private double electricityFee;
    private double waterFee;
    private double totalFee;
    private String status;
    private String payDate;
    
    public HistoryBill(String month, double electricityFee, double waterFee, 
            double totalFee, String status, String payDate) {
this.month = month;
this.electricityFee = electricityFee;
this.waterFee = waterFee;
this.totalFee = totalFee;
this.status = status;
this.payDate = payDate;
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
    
    public String getPayDate() {
        return payDate;
    }
    
    public void setPayDate(String payDate) {
        this.payDate = payDate;
    }
}