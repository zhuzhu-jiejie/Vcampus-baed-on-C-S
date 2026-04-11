package seu.vcampus.client.controller;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import javafx.animation.Animation;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.util.Callback;
import seu.vcampus.client.network.SocketManager;
import seu.vcampus.model.AdminDeleteRoomRequest;
import seu.vcampus.model.AdminRoomBookingRequest;
import seu.vcampus.model.AdminRoomInfoRequest;
import seu.vcampus.model.RoomBookingInfo;
import seu.vcampus.model.RoomInfo;

public class LibraryManage2Controller implements Initializable {

    @FXML
    private Label currentUserLabel;

    @FXML
    private Label currentDateLabel;

    @FXML
    private Button addRoomBtn;

    @FXML
    private Button refreshBtn;

    @FXML
    private TextField searchRoomField;

    @FXML
    private Button searchBtn;

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
    private TableView<RoomInfo> roomTableView;

    @FXML
    private TableColumn<RoomInfo, String> roomIdColumn;

    @FXML
    private TableColumn<RoomInfo, String> floorColumn;

    @FXML
    private TableColumn<RoomInfo, String> capacityColumn;

    @FXML
    private TableColumn<RoomInfo, String> statusColumn;

    @FXML
    private TableColumn<RoomInfo, String> reservationColumn;

    @FXML
    private TableColumn<RoomInfo, String> equipmentColumn;

    @FXML
    private TableColumn<RoomInfo, Void> actionColumn;

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
    private ObservableList<RoomInfo> allRooms = FXCollections.observableArrayList();
    private ObservableList<RoomInfo> filteredRooms = FXCollections.observableArrayList();
    private int currentFloor = 1;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        initUserData();
        initDate();
        initTableColumns();
        loadRoomDataFromServer();
        setupToggleButtons();
        setupEventListeners();
    }

    private void initUserData() {
        currentUserLabel.setText("管理员: admin");
    }

    private void initDate() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        currentDateLabel.setText(sdf.format(new Date()));

        javafx.animation.Timeline timeline = new javafx.animation.Timeline(
            new javafx.animation.KeyFrame(javafx.util.Duration.seconds(1),
            event -> currentDateLabel.setText(sdf.format(new Date())))
        );
        timeline.setCycleCount(Animation.INDEFINITE);
        timeline.play();
    }

    private void initTableColumns() {
        roomIdColumn.setCellValueFactory(new PropertyValueFactory<>("roomId"));
        floorColumn.setCellValueFactory(cellData ->
            new SimpleStringProperty(cellData.getValue().getFloor() + "楼"));
        capacityColumn.setCellValueFactory(cellData ->
            new SimpleStringProperty(String.valueOf(cellData.getValue().getCapacity())));
        statusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));
        equipmentColumn.setCellValueFactory(new PropertyValueFactory<>("facilities"));

        // 移除reservationColumn，因为RoomInfo中没有这个字段
        reservationColumn.setVisible(false);

        statusColumn.setCellFactory(column -> new TableCell<RoomInfo, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    // 将状态转换为中文显示
                    String statusText;
                    switch (item) {
                        case "available":
                            statusText = "可用";
                            setStyle("-fx-text-fill: #36D399; -fx-font-weight: bold;");
                            break;
                        case "reserved":
                            statusText = "已预约";
                            setStyle("-fx-text-fill: #F87272; -fx-font-weight: bold;");
                            break;
                        case "maintenance":
                            statusText = "维护中";
                            setStyle("-fx-text-fill: #FBBD23; -fx-font-weight: bold;");
                            break;
                        default:
                            statusText = item;
                            setStyle("");
                    }
                    setText(statusText);
                }
            }
        });

        addActionButtons();
    }

    private void addActionButtons() {
        Callback<TableColumn<RoomInfo, Void>, TableCell<RoomInfo, Void>> cellFactory = new Callback<>() {
            @Override
            public TableCell<RoomInfo, Void> call(final TableColumn<RoomInfo, Void> param) {
                final TableCell<RoomInfo, Void> cell = new TableCell<>() {

                    private final Button editBtn = new Button("编辑");
                    private final Button deleteBtn = new Button("删除");
                    private final Button viewBookingsBtn = new Button("查看预约"); // 新增按钮
                    private final HBox hbox = new HBox(editBtn, deleteBtn, viewBookingsBtn); // 添加新按钮

                    {
                        editBtn.setOnAction(event -> {
                            RoomInfo room = getTableView().getItems().get(getIndex());
                            handleEditRoom(room);
                        });

                        deleteBtn.setOnAction(event -> {
                            RoomInfo room = getTableView().getItems().get(getIndex());
                            handleDeleteRoom(room);
                        });

                        // 新增查看预约按钮事件
                        viewBookingsBtn.setOnAction(event -> {
                            RoomInfo room = getTableView().getItems().get(getIndex());
                            handleViewBookings(room);
                        });

                        // 设置按钮样式
                        editBtn.setStyle("-fx-background-color: #165DFF; -fx-text-fill: white; -fx-pref-width: 60;");
                        deleteBtn.setStyle("-fx-background-color: #F87272; -fx-text-fill: white; -fx-pref-width: 60;");
                        viewBookingsBtn.setStyle("-fx-background-color: #FBBD23; -fx-text-fill: white; -fx-pref-width: 80;");
                        hbox.setSpacing(5);
                    }

                    @Override
                    protected void updateItem(Void item, boolean empty) {
                        super.updateItem(item, empty);
                        if (empty) {
                            setGraphic(null);
                        } else {
                            setGraphic(hbox);
                        }
                    }
                };
                return cell;
            }
        };

        actionColumn.setCellFactory(cellFactory);
    }

    private void loadRoomDataFromServer() {
        try {
            DataOutputStream out = SocketManager.getInstance().getOut();
            DataInputStream in = SocketManager.getInstance().getIn();

            // 发送管理员研讨间信息请求
            out.writeUTF("AdminRoomInfoRequest");
            out.writeUTF(gson.toJson(new AdminRoomInfoRequest()));
            out.flush();

            // 接收响应
            String response = in.readUTF();
            if (response.startsWith("SUCCESS|")) {
                String jsonStr = response.substring(8);
                List<RoomInfo> roomList = gson.fromJson(jsonStr,
                    new TypeToken<List<RoomInfo>>(){}.getType());

                allRooms.clear();
                allRooms.addAll(roomList);

                updateFloorRooms();

                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("加载成功");
                alert.setHeaderText(null);
                alert.setContentText("成功加载" + roomList.size() + "个研讨间信息");
                alert.showAndWait();
            } else {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("加载失败");
                alert.setHeaderText(null);
                alert.setContentText("加载研讨间信息失败: " + response.substring(6));
                alert.showAndWait();
            }
        } catch (Exception e) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("错误");
            alert.setHeaderText(null);
            alert.setContentText("加载研讨间信息时发生错误: " + e.getMessage());
            alert.showAndWait();
            e.printStackTrace();
        }
    }

    private void updateFloorRooms() {
        filteredRooms.clear();

        for (RoomInfo room : allRooms) {
            if (room.getFloor() == currentFloor) {
                filteredRooms.add(room);
            }
        }

        roomTableView.setItems(filteredRooms);

        int total = filteredRooms.size();
        long available = filteredRooms.stream().filter(room -> "available".equals(room.getStatus())).count();
        long reserved = filteredRooms.stream().filter(room -> "reserved".equals(room.getStatus())).count();
        long maintenance = filteredRooms.stream().filter(room -> "maintenance".equals(room.getStatus())).count();

        floorInfoLabel.setText(
            String.format("%d楼共有研讨间 %d 间，其中：可用 %d 间，维护中 %d 间",
                currentFloor, total, available, reserved, maintenance)
        );
    }

    private void setupToggleButtons() {
        ToggleGroup floorGroup = new ToggleGroup();
        floor1Btn.setToggleGroup(floorGroup);
        floor2Btn.setToggleGroup(floorGroup);
        floor3Btn.setToggleGroup(floorGroup);
        floor4Btn.setToggleGroup(floorGroup);
        floor5Btn.setToggleGroup(floorGroup);

        floor1Btn.setSelected(true);

        floor1Btn.setOnAction(e -> { currentFloor = 1; updateFloorRooms(); });
        floor2Btn.setOnAction(e -> { currentFloor = 2; updateFloorRooms(); });
        floor3Btn.setOnAction(e -> { currentFloor = 3; updateFloorRooms(); });
        floor4Btn.setOnAction(e -> { currentFloor = 4; updateFloorRooms(); });
        floor5Btn.setOnAction(e -> { currentFloor = 5; updateFloorRooms(); });
    }

    private void setupEventListeners() {
        refreshBtn.setOnAction(e -> refreshRoomList());
        addRoomBtn.setOnAction(e -> handleAddRoom());
        searchBtn.setOnAction(e -> searchRooms());
        searchRoomField.setOnAction(e -> searchRooms());
    }

    private void refreshRoomList() {
        searchRoomField.clear();
        loadRoomDataFromServer();
    }

    private void searchRooms() {
        String searchText = searchRoomField.getText().trim().toLowerCase();

        if (searchText.isEmpty()) {
            updateFloorRooms();
            return;
        }

        ObservableList<RoomInfo> searchResult = FXCollections.observableArrayList();

        for (RoomInfo room : allRooms) {
            if (room.getFloor() == currentFloor &&
                room.getRoomId().toLowerCase().contains(searchText)) {
                searchResult.add(room);
            }
        }

        roomTableView.setItems(searchResult);
    }

    private void handleAddRoom() {
        // 创建对话框
        Dialog<RoomInfo> dialog = new Dialog<>();
        dialog.setTitle("添加研讨间");
        dialog.setHeaderText("请输入研讨间信息");

        // 设置按钮
        ButtonType addButtonType = new ButtonType("添加", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(addButtonType, ButtonType.CANCEL);

        // 创建表单
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        TextField roomIdField = new TextField();
        TextField floorField = new TextField();
        TextField roomNumberField = new TextField();
        TextField capacityField = new TextField();
        ComboBox<String> statusComboBox = new ComboBox<>();
        statusComboBox.getItems().addAll("available", "reserved", "maintenance");
        TextArea facilitiesArea = new TextArea();

        grid.add(new Label("房间ID:"), 0, 0);
        grid.add(roomIdField, 1, 0);
        grid.add(new Label("楼层:"), 0, 1);
        grid.add(floorField, 1, 1);
        grid.add(new Label("房间号:"), 0, 2);
        grid.add(roomNumberField, 1, 2);
        grid.add(new Label("容量:"), 0, 3);
        grid.add(capacityField, 1, 3);
        grid.add(new Label("状态:"), 0, 4);
        grid.add(statusComboBox, 1, 4);
        grid.add(new Label("设施:"), 0, 5);
        grid.add(facilitiesArea, 1, 5);

        dialog.getDialogPane().setContent(grid);

        // 转换结果为RoomInfo对象
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == addButtonType) {
                RoomInfo room = new RoomInfo();
                room.setRoomId(roomIdField.getText());
                room.setFloor(Integer.parseInt(floorField.getText()));
                room.setRoomNumber(Integer.parseInt(roomNumberField.getText()));
                room.setCapacity(Integer.parseInt(capacityField.getText()));
                room.setStatus(statusComboBox.getValue());
                room.setFacilities(facilitiesArea.getText());
                return room;
            }
            return null;
        });

        Optional<RoomInfo> result = dialog.showAndWait();
        result.ifPresent(room -> {
            try {
                DataOutputStream out = SocketManager.getInstance().getOut();
                DataInputStream in = SocketManager.getInstance().getIn();

                // 发送添加研讨间请求
                out.writeUTF("AdminAddRoomRequest");
                out.writeUTF(gson.toJson(room));
                out.flush();

                // 接收响应
                String response = in.readUTF();
                if (response.startsWith("SUCCESS|")) {
                    Alert alert = new Alert(Alert.AlertType.INFORMATION);
                    alert.setTitle("成功");
                    alert.setHeaderText(null);
                    alert.setContentText("添加研讨间成功");
                    alert.showAndWait();

                    // 刷新列表
                    loadRoomDataFromServer();
                } else {
                    // 添加更详细的错误信息
                    String errorMsg = response.substring(6);
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setTitle("错误");
                    alert.setHeaderText(null);
                    alert.setContentText("添加研讨间失败: " + errorMsg);
                    alert.showAndWait();

                    // 记录详细错误日志
                    System.err.println("添加研讨间失败 - 房间ID: " + room.getRoomId() +
                                      ", 错误信息: " + errorMsg);
                }
            } catch (Exception e) {
                // 添加更详细的错误日志
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("错误");
                alert.setHeaderText(null);
                alert.setContentText("添加研讨间时发生错误: " + e.getMessage());
                alert.showAndWait();

                // 记录详细错误日志
                System.err.println("添加研讨间时发生异常 - 房间ID: " + room.getRoomId());
                e.printStackTrace();
            }
        });
    }

    private void handleEditRoom(RoomInfo room) {
        // 创建编辑对话框
        Dialog<RoomInfo> dialog = new Dialog<>();
        dialog.setTitle("编辑研讨间");
        dialog.setHeaderText("编辑研讨间信息");

        // 设置按钮
        ButtonType saveButtonType = new ButtonType("保存", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        // 创建表单
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        TextField roomIdField = new TextField(room.getRoomId());
        roomIdField.setEditable(false); // 房间ID不可编辑
        TextField floorField = new TextField(String.valueOf(room.getFloor()));
        TextField roomNumberField = new TextField(String.valueOf(room.getRoomNumber()));
        TextField capacityField = new TextField(String.valueOf(room.getCapacity()));
        ComboBox<String> statusComboBox = new ComboBox<>();
        statusComboBox.getItems().addAll("available", "reserved", "maintenance");
        statusComboBox.setValue(room.getStatus());
        TextArea facilitiesArea = new TextArea(room.getFacilities());

        grid.add(new Label("房间ID:"), 0, 0);
        grid.add(roomIdField, 1, 0);
        grid.add(new Label("楼层:"), 0, 1);
        grid.add(floorField, 1, 1);
        grid.add(new Label("房间号:"), 0, 2);
        grid.add(roomNumberField, 1, 2);
        grid.add(new Label("容量:"), 0, 3);
        grid.add(capacityField, 1, 3);
        grid.add(new Label("状态:"), 0, 4);
        grid.add(statusComboBox, 1, 4);
        grid.add(new Label("设施:"), 0, 5);
        grid.add(facilitiesArea, 1, 5);

        dialog.getDialogPane().setContent(grid);

        // 转换结果为RoomInfo对象
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                RoomInfo updatedRoom = new RoomInfo();
                updatedRoom.setRoomId(roomIdField.getText());
                updatedRoom.setFloor(Integer.parseInt(floorField.getText()));
                updatedRoom.setRoomNumber(Integer.parseInt(roomNumberField.getText()));
                updatedRoom.setCapacity(Integer.parseInt(capacityField.getText()));
                updatedRoom.setStatus(statusComboBox.getValue());
                updatedRoom.setFacilities(facilitiesArea.getText());
                return updatedRoom;
            }
            return null;
        });

        Optional<RoomInfo> result = dialog.showAndWait();
        result.ifPresent(updatedRoom -> {
            try {
                DataOutputStream out = SocketManager.getInstance().getOut();
                DataInputStream in = SocketManager.getInstance().getIn();

                // 发送编辑研讨间请求
                out.writeUTF("AdminEditRoomRequest");
                out.writeUTF(gson.toJson(updatedRoom));
                out.flush();

                // 接收响应
                String response = in.readUTF();
                if (response.startsWith("SUCCESS|")) {
                    Alert alert = new Alert(Alert.AlertType.INFORMATION);
                    alert.setTitle("成功");
                    alert.setHeaderText(null);
                    alert.setContentText("编辑研讨间成功");
                    alert.showAndWait();

                    // 刷新列表
                    loadRoomDataFromServer();
                } else {
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setTitle("错误");
                    alert.setHeaderText(null);
                    alert.setContentText("编辑研讨间失败: " + response.substring(6));
                    alert.showAndWait();
                }
            } catch (Exception e) {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("错误");
                alert.setHeaderText(null);
                alert.setContentText("编辑研讨间时发生错误: " + e.getMessage());
                alert.showAndWait();
                e.printStackTrace();
            }
        });
    }

    private void handleDeleteRoom(RoomInfo room) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("确认删除");
        alert.setHeaderText(null);
        alert.setContentText("确定要删除研讨间 " + room.getRoomId() + " 吗？此操作不可恢复。");

        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    DataOutputStream out = SocketManager.getInstance().getOut();
                    DataInputStream in = SocketManager.getInstance().getIn();

                    // 创建删除请求
                    AdminDeleteRoomRequest req = new AdminDeleteRoomRequest();
                    req.setRoomId(room.getRoomId());

                    // 发送删除研讨间请求
                    out.writeUTF("AdminDeleteRoomRequest");
                    out.writeUTF(gson.toJson(req));
                    out.flush();

                    // 接收响应
                    String responseMsg = in.readUTF();
                    if (responseMsg.startsWith("SUCCESS|")) {
                        Alert successAlert = new Alert(Alert.AlertType.INFORMATION);
                        successAlert.setTitle("成功");
                        successAlert.setHeaderText(null);
                        successAlert.setContentText("删除研讨间成功");
                        successAlert.showAndWait();

                        // 刷新列表
                        loadRoomDataFromServer();
                    } else {
                        Alert errorAlert = new Alert(Alert.AlertType.ERROR);
                        errorAlert.setTitle("错误");
                        errorAlert.setHeaderText(null);
                        errorAlert.setContentText("删除研讨间失败: " + responseMsg.substring(6));
                        errorAlert.showAndWait();
                    }
                } catch (Exception e) {
                    Alert errorAlert = new Alert(Alert.AlertType.ERROR);
                    errorAlert.setTitle("错误");
                    errorAlert.setHeaderText(null);
                    errorAlert.setContentText("删除研讨间时发生错误: " + e.getMessage());
                    errorAlert.showAndWait();
                    e.printStackTrace();
                }
            }
        });
    }

    // 新增方法：处理查看预约记录
    private void handleViewBookings(RoomInfo room) {
        try {
            DataOutputStream out = SocketManager.getInstance().getOut();
            DataInputStream in = SocketManager.getInstance().getIn();

            // 发送查看预约记录请求
            out.writeUTF("AdminRoomBookingInfoRequest");
            AdminRoomBookingRequest req = new AdminRoomBookingRequest();
            req.setRoomId(room.getRoomId());
            out.writeUTF(gson.toJson(req));
            out.flush();

            // 接收响应
            String response = in.readUTF();
            if (response.startsWith("SUCCESS|")) {
                String jsonStr = response.substring(8);
                List<RoomBookingInfo> bookingList = gson.fromJson(jsonStr,
                    new TypeToken<List<RoomBookingInfo>>(){}.getType());

                // 创建并显示预约记录对话框
                showBookingDialog(room.getRoomId(), bookingList);
            } else {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("错误");
                alert.setHeaderText(null);
                alert.setContentText("获取预约记录失败: " + response.substring(6));
                alert.showAndWait();
            }
        } catch (Exception e) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("错误");
            alert.setHeaderText(null);
            alert.setContentText("获取预约记录时发生错误: " + e.getMessage());
            alert.showAndWait();
            e.printStackTrace();
        }
    }

 // 新增方法：显示预约记录对话框
    private void showBookingDialog(String roomId, List<RoomBookingInfo> bookingList) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("研讨间预约记录 - " + roomId);
        dialog.setHeaderText("房间 " + roomId + " 的预约记录");

        // 设置按钮
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);

        // 创建表格
        TableView<RoomBookingInfo> tableView = new TableView<>();
        ObservableList<RoomBookingInfo> data = FXCollections.observableArrayList(bookingList);

        // 创建列
        TableColumn<RoomBookingInfo, Integer> idColumn = new TableColumn<>("预约ID");
        idColumn.setCellValueFactory(new PropertyValueFactory<>("bookingId"));

        TableColumn<RoomBookingInfo, String> userIdColumn = new TableColumn<>("用户ID");
        userIdColumn.setCellValueFactory(new PropertyValueFactory<>("userId"));

        TableColumn<RoomBookingInfo, LocalDate> dateColumn = new TableColumn<>("预约日期");
        dateColumn.setCellValueFactory(new PropertyValueFactory<>("bookingDate"));

        TableColumn<RoomBookingInfo, String> timeColumn = new TableColumn<>("预约时段");
        timeColumn.setCellValueFactory(new PropertyValueFactory<>("timeSlot"));

        TableColumn<RoomBookingInfo, String> purposeColumn = new TableColumn<>("用途");
        purposeColumn.setCellValueFactory(new PropertyValueFactory<>("purpose"));

        TableColumn<RoomBookingInfo, LocalDate> createTimeColumn = new TableColumn<>("创建时间");
        createTimeColumn.setCellValueFactory(new PropertyValueFactory<>("createTime"));

        TableColumn<RoomBookingInfo, String> statusColumn = new TableColumn<>("状态");
        statusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));

        tableView.getColumns().addAll(idColumn, userIdColumn, dateColumn, timeColumn,
                                     purposeColumn, createTimeColumn, statusColumn);
        tableView.setItems(data);

        // 设置表格大小
        tableView.setPrefSize(800, 400);

        // 将表格添加到对话框
        VBox content = new VBox(tableView);
        content.setPadding(new Insets(10));
        dialog.getDialogPane().setContent(content);

        // 显示对话框
        dialog.showAndWait();
    }
}