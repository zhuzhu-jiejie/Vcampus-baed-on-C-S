package seu.vcampus.model;

import java.time.LocalDate;

public class RoomInfoRequest {

	private String userId;           // 用户ID
    private int floor;           // 房间ID
    private LocalDate bookingDate;   // 预约日期

    // Getter和Setter方法
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

	public int getFloor() {
		return floor;
	}

	public void setFloor(int floor) {
		this.floor = floor;
	}

}
