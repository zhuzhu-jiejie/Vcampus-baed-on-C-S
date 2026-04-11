// ElectricityWaterBillResponse.java
package seu.vcampus.model;

import java.util.List;

public class ElectricityWaterBillResponse {
    private int status;
    private String message;
    private String building;
    private String room;
    private CurrentBill currentBill;
    private List<HistoryBill> historyBills;
    
    public ElectricityWaterBillResponse() {
        // 默认构造函数
    }
    
    public ElectricityWaterBillResponse(int status, String message, String building, String room, 
                                      CurrentBill currentBill, List<HistoryBill> historyBills) {
        this.status = status;
        this.message = message;
        this.building = building;
        this.room = room;
        this.currentBill = currentBill;
        this.historyBills = historyBills;
    }
    
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
    
    public CurrentBill getCurrentBill() {
        return currentBill;
    }
    
    public void setCurrentBill(CurrentBill currentBill) {
        this.currentBill = currentBill;
    }
    
    public List<HistoryBill> getHistoryBills() {
        return historyBills;
    }
    
    public void setHistoryBills(List<HistoryBill> historyBills) {
        this.historyBills = historyBills;
    }
}