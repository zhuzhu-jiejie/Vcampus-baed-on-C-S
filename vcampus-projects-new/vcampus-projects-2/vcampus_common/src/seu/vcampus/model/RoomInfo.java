package seu.vcampus.model;

import java.time.LocalDate;
import java.time.LocalTime;

public class RoomInfo {
    private String roomId;        // 房间ID（格式：楼层-房间号，如"1-101"）
    private int floor;           // 楼层
    private int roomNumber;      // 房间号
    private int capacity;        // 容纳人数
    private String status;       // 状态（available/reserved/maintenance）
    private String facilities;   // 设施描述

    // Getter和Setter方法
    public String getRoomId() {
        return roomId;
    }

    public void setRoomId(String roomId) {
        this.roomId = roomId;
    }

    public int getFloor() {
        return floor;
    }

    public void setFloor(int floor) {
        this.floor = floor;
    }

    public int getRoomNumber() {
        return roomNumber;
    }

    public void setRoomNumber(int roomNumber) {
        this.roomNumber = roomNumber;
    }

    public int getCapacity() {
        return capacity;
    }

    public void setCapacity(int capacity) {
        this.capacity = capacity;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getFacilities() {
        return facilities;
    }

    public void setFacilities(String facilities) {
        this.facilities = facilities;
    }
}