package seu.vcampus.client.controller;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.URL;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.ResourceBundle;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import seu.vcampus.client.network.SocketManager;
import seu.vcampus.model.RoomBookingRequest;
import seu.vcampus.model.RoomInfo;


public class RoomManageController implements Initializable {

    @FXML
    private Label currentDateLabel;

    @FXML
    private DatePicker bookingDatePicker;

    @FXML
    private ToggleButton floor1Btn;

    @FXML
    private ToggleButton floor2Btn;

    @FXML
    private ToggleButton floor3Btn;

    @FXML
    private ToggleButton floor4Btn;

    @FXML
    private ToggleButton floor5Btn;

    @FXML
    private Label floorInfoLabel;

    @FXML
    private FlowPane roomFlowPane;

    @FXML
    private DialogPane bookingDialog;

    @FXML
    private Label roomNumberLabel;

    @FXML
    private Label dialogDateLabel;

    @FXML
    private Label dialogTimeLabel;

    @FXML
    private VBox timeSlotContainer;

    @FXML
    private TextArea purposeTextArea;

    @FXML
    private ButtonType cancelBtn;

    @FXML
    private ButtonType confirmBtn;

    @FXML
    private ComboBox<String> timeSlotComboBox;

    // 当前选中的楼层
    private int currentFloor = 1;

    // 当前选中的房间
    private String selectedRoom;

    // 当前登录用户ID
    private String currentUserId;

    // 每个楼层的房间列表
    private final Map<Integer, List<RoomInfo>> floorRooms = new HashMap<>();

    // 房间状态映射
    private final Map<String, String> roomStatus = new HashMap<>();

    // 时间段选项
    private final String[] timeSlots = {
        "08:00-10:00", "10:00-12:00", "12:00-14:00",
        "14:00-16:00", "16:00-18:00", "18:00-20:00"
    };

    // 在 RoomManageController 类中添加这个内部类
    private static class LocalDateAdapter extends com.google.gson.TypeAdapter<LocalDate> {
        private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        @Override
        public void write(com.google.gson.stream.JsonWriter out, LocalDate value) throws IOException {
            if (value == null) {
                out.nullValue();
            } else {
                out.value(formatter.format(value));
            }
        }

        @Override
        public LocalDate read(com.google.gson.stream.JsonReader in) throws IOException {
            if (in.peek() == com.google.gson.stream.JsonToken.NULL) {
                in.nextNull();
                return null;
            }
            String dateStr = in.nextString();
            return LocalDate.parse(dateStr, formatter);
        }
    }
    private final Gson gson = new GsonBuilder()
    	    .registerTypeAdapter(LocalDate.class, new LocalDateAdapter())
    	    .create();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // 初始化当前日期
        LocalDate today = LocalDate.now();
        currentDateLabel.setText(today.format(DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.CHINA)));

        // 设置日期选择器默认值为今天
        bookingDatePicker.setValue(today);

        // 初始化时段选择框
        timeSlotComboBox.getItems().addAll(timeSlots);
        // 设置默认时段（例如第一个时段）
        if (!(timeSlots == null || timeSlots.length == 0)) {
            timeSlotComboBox.setValue(timeSlots[0]);
        }

        // 默认选择1楼
        floor1Btn.setSelected(true);

        // 初始化UI
        updateFloorInfo();

        // 添加事件监听器
        setupEventHandlers();

        // 从服务器加载房间信息
        loadRoomInfoFromServer();
    }

    // 设置当前用户ID
    public void setUserId(String userId) {
        this.currentUserId = userId;

    }

    private void loadRoomInfoFromServer() {
        try {
            DataOutputStream out = SocketManager.getInstance().getOut();
            DataInputStream in = SocketManager.getInstance().getIn();

         // 获取选中的日期和时段，确保不为空
            LocalDate selectedDate = bookingDatePicker.getValue();
            if (selectedDate == null) {
                selectedDate = LocalDate.now(); // 默认今天
                bookingDatePicker.setValue(selectedDate);
            }
            String selectedTimeSlot = timeSlotComboBox.getValue();
            if (selectedTimeSlot == null || selectedTimeSlot.isEmpty()) {
                selectedTimeSlot = timeSlots[0]; // 默认第一个时段
                timeSlotComboBox.setValue(selectedTimeSlot);
            }

            // 创建请求对象，包含日期和时段信息
            Map<String, Object> requestData = new HashMap<>();
            requestData.put("date", selectedDate);
            requestData.put("timeSlot", selectedTimeSlot);

            // 发送研讨间信息请求
            out.writeUTF("RoomInfoRequest");
            out.writeUTF(gson.toJson(requestData));
            out.flush();

            // 接收响应
            String response = in.readUTF();
            if (response.startsWith("SUCCESS|")) {
                String jsonStr = response.substring(8);
                List<RoomInfo> roomList = gson.fromJson(jsonStr,
                    new TypeToken<List<RoomInfo>>(){}.getType());

                // 清空现有数据
                floorRooms.clear();
                roomStatus.clear();

                // 按楼层组织房间
                for (RoomInfo room : roomList) {
                    int floor = room.getFloor();
                    if (!floorRooms.containsKey(floor)) {
                        floorRooms.put(floor, new ArrayList<>());
                    }
                    floorRooms.get(floor).add(room);
                    roomStatus.put(room.getRoomId(), room.getStatus());
                }

                // 更新UI
                updateFloorInfo();
                displayRooms();
            } else {
                showAlert("错误", "获取研讨间信息失败: " + response.substring(6));
            }
        } catch (Exception e) {
            showAlert("错误", "获取研讨间信息失败: " + e.getMessage());
            // 使用模拟数据作为后备
            initializeMockData();
        }
    }

    private void initializeMockData() {
        // 模拟数据作为后备
        for (int floor = 1; floor <= 5; floor++) {
            int roomCount = 2;
            if (floor == 4 || floor == 5) {
				roomCount = 1;
			}

            List<RoomInfo> rooms = new ArrayList<>();
            for (int i = 1; i <= roomCount; i++) {
                RoomInfo room = new RoomInfo();
                room.setRoomId(floor + "-" + i);
                room.setFloor(floor);
                room.setRoomNumber(i);
                room.setStatus("available");
                rooms.add(room);
                roomStatus.put(room.getRoomId(), room.getStatus());
            }
            floorRooms.put(floor, rooms);
        }

        updateFloorInfo();
        displayRooms();
    }

    private void setupEventHandlers() {
        // 楼层切换事件
        floor1Btn.setOnAction(e -> switchFloor(1));
        floor2Btn.setOnAction(e -> switchFloor(2));
        floor3Btn.setOnAction(e -> switchFloor(3));
        floor4Btn.setOnAction(e -> switchFloor(4));
        floor5Btn.setOnAction(e -> switchFloor(5));

        // 日期选择事件
        bookingDatePicker.valueProperty().addListener((observable, oldValue, newValue) -> {
            // 重新加载房间信息，因为状态可能随日期变化
            loadRoomInfoFromServer();
        });

        // 时段选择事件
        timeSlotComboBox.valueProperty().addListener((observable, oldValue, newValue) -> {
            // 重新加载房间信息，因为状态可能随时段变化
            loadRoomInfoFromServer();
        });
    }

    private void switchFloor(int floor) {
        currentFloor = floor;
        updateFloorInfo();
        displayRooms();
    }

    private void updateFloorInfo() {
        int roomCount = floorRooms.containsKey(currentFloor) ? floorRooms.get(currentFloor).size() : 0;
        floorInfoLabel.setText(currentFloor + "楼共有 " + roomCount + " 个研讨间");
    }

    private void displayRooms() {
        roomFlowPane.getChildren().clear();

        if (!floorRooms.containsKey(currentFloor)) {
            return;
        }

        List<RoomInfo> rooms = floorRooms.get(currentFloor);
        for (RoomInfo room : rooms) {
            String status = roomStatus.getOrDefault(room.getRoomId(), "available");

            // 创建房间按钮
            Button roomBtn = new Button(room.getRoomId());
            roomBtn.setPrefSize(120, 100);
            roomBtn.setStyle(getRoomStyle(status));

            // 设置房间点击事件
            roomBtn.setOnAction(e -> showBookingDialog(room.getRoomId()));

            roomFlowPane.getChildren().add(roomBtn);
        }
    }

    private String getRoomStyle(String status) {
        switch (status) {
            case "available":
                return "-fx-background-color: #36D399; -fx-text-fill: white; -fx-font-size: 16px; -fx-font-weight: bold;";
            case "reserved":
                return "-fx-background-color: #FBBD23; -fx-text-fill: white; -fx-font-size: 16px; -fx-font-weight: bold;";
            case "maintenance":
                return "-fx-background-color: #F87272; -fx-text-fill: white; -fx-font-size: 16px; -fx-font-weight: bold;";
            default:
                return "-fx-background-color: #36D399; -fx-text-fill: white; -fx-font-size: 16px; -fx-font-weight: bold;";
        }
    }

    private void showBookingDialog(String roomId) {
        String status = roomStatus.get(roomId);

        // 只有可用房间可以预约
        if (!"available".equals(status)) {
            showAlert("提示", "该研讨间当前不可预约");
            return;
        }

        selectedRoom = roomId;
        roomNumberLabel.setText(roomId);

        // 设置弹窗中的日期和时段标签
        dialogDateLabel.setText(bookingDatePicker.getValue().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
        dialogTimeLabel.setText(timeSlotComboBox.getValue());

        purposeTextArea.clear();

        // 创建对话框
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setDialogPane(bookingDialog);
        dialog.setTitle("预约研讨间");

        // 处理对话框结果
        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isPresent() && result.get() == confirmBtn) {
            // 用户点击了确认预约
            handleBookingConfirmation();
        }
    }

    private void handleBookingConfirmation() {
        // 获取主界面选择的时段
        String selectedTimeSlot = timeSlotComboBox.getValue();
        List<String> selectedTimeSlots = new ArrayList<>();
        selectedTimeSlots.add(selectedTimeSlot);

        if (purposeTextArea.getText().trim().isEmpty()) {
            showAlert("错误", "请输入预约用途");
            return;
        }

        if (currentUserId == null || currentUserId.isEmpty()) {
            showAlert("错误", "用户未登录，无法预约");
            return;
        }

        // 创建预约请求
        RoomBookingRequest req = new RoomBookingRequest();
        req.setUserId(currentUserId);
        req.setRoomId(selectedRoom);
        req.setBookingDate(bookingDatePicker.getValue());
        req.setTimeSlots(selectedTimeSlots);
        req.setPurpose(purposeTextArea.getText());

        try {
            DataOutputStream out = SocketManager.getInstance().getOut();
            DataInputStream in = SocketManager.getInstance().getIn();
            // 发送预约请求
            out.writeUTF("RoomBookingRequest");
            out.writeUTF(gson.toJson(req));
            out.flush();

            // 接收响应
            String response = in.readUTF();
            if (response.startsWith("SUCCESS|")) {
                // 预约成功，重新加载房间信息以更新状态
                loadRoomInfoFromServer();
                showAlert("成功", "预约成功！");
            } else {
                showAlert("错误", response.substring(6));
            }
        } catch (Exception e) {
            showAlert("错误", "预约请求失败: " + e.getMessage());
            System.out.println(e.getMessage());
        }
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}