package seu.vcampus.model;

import java.time.LocalDate;
import java.time.LocalTime;

public class RoomBookingInfo {
    private int bookingId;       // 预约ID
    private String roomId;       // 房间ID
    private String userId;       // 用户ID
    private LocalDate bookingDate; // 预约日期
    private String timeSlot;     // 预约时段（如"08:00-10:00"）
    private String purpose;      // 预约用途
    private LocalDate createTime; // 创建时间
    private String status;       // 预约状态（confirmed/cancelled/completed）

    // Getter和Setter方法
    public int getBookingId() {
        return bookingId;
    }

    public void setBookingId(int bookingId) {
        this.bookingId = bookingId;
    }

    public String getRoomId() {
        return roomId;
    }

    public void setRoomId(String roomId) {
        this.roomId = roomId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public LocalDate getBookingDate() {
        return bookingDate;
    }

    public void setBookingDate(LocalDate bookingDate) {
        this.bookingDate = bookingDate;
    }

    public String getTimeSlot() {
        return timeSlot;
    }

    public void setTimeSlot(String timeSlot) {
        this.timeSlot = timeSlot;
    }

    public String getPurpose() {
        return purpose;
    }

    public void setPurpose(String purpose) {
        this.purpose = purpose;
    }

    public LocalDate getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDate createTime) {
        this.createTime = createTime;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}