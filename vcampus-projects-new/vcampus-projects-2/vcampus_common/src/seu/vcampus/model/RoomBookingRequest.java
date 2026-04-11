package seu.vcampus.model;

import java.time.LocalDate;
import java.util.List;

public class RoomBookingRequest {
    private String userId;           // 用户ID
    private String roomId;           // 房间ID
    private LocalDate bookingDate;   // 预约日期
    private List<String> timeSlots;  // 预约时段列表
    private String purpose;          // 预约用途

    // Getter和Setter方法
    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getRoomId() {
        return roomId;
    }

    public void setRoomId(String roomId) {
        this.roomId = roomId;
    }

    public LocalDate getBookingDate() {
        return bookingDate;
    }

    public void setBookingDate(LocalDate bookingDate) {
        this.bookingDate = bookingDate;
    }

    public List<String> getTimeSlots() {
        return timeSlots;
    }

    public void setTimeSlots(List<String> timeSlots) {
        this.timeSlots = timeSlots;
    }

    public String getPurpose() {
        return purpose;
    }

    public void setPurpose(String purpose) {
        this.purpose = purpose;
    }


}