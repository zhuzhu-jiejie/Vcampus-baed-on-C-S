package seu.vcampus.client.controller;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.cell.PropertyValueFactory;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.List;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import javafx.application.Platform;
import seu.vcampus.client.network.SocketManager;

public class AdminExchangeController {
    @FXML
    private TableView<ExchangeApplication> returnTable;
    @FXML
    private TableColumn<ExchangeApplication, String> returnApplicantCol;
    @FXML
    private TableColumn<ExchangeApplication, String> returnRoomCol;
    @FXML
    private TableColumn<ExchangeApplication, String> returnTimeCol;
    @FXML
    private TableColumn<ExchangeApplication, String> returnStatusCol;
    @FXML
    private Label returnApplicant;
    @FXML
    private Label returnRoom;
    @FXML
    private Label returnTime;
    @FXML
    private TextArea returnDetail;
    @FXML
    private TextArea returnFeedback;
    @FXML
    private Button returnApproveBtn;
    @FXML
    private Button returnRejectBtn;

    // 换舍申请相关控件
    @FXML
    private TableView<ExchangeApplication> exchangeTable;
    @FXML
    private TableColumn<ExchangeApplication, String> exchangeApplicantCol;
    @FXML
    private TableColumn<ExchangeApplication, String> exchangeOriginalRoomCol;
    @FXML
    private TableColumn<ExchangeApplication, String> exchangeTimeCol;
    @FXML
    private TableColumn<ExchangeApplication, String> exchangeStatusCol;
    @FXML
    private Label exchangeApplicant;
    @FXML
    private Label exchangeOriginalRoom;
    @FXML
    private Label exchangeTime;
    @FXML
    private TextArea exchangeDetail;
    @FXML
    private TextArea exchangeFeedback;
    @FXML
    private Button exchangeApproveBtn;
    @FXML
    private Button exchangeRejectBtn;

    // 数据列表
    private ObservableList<ExchangeApplication> returnApplications = FXCollections.observableArrayList();
    private ObservableList<ExchangeApplication> exchangeApplications = FXCollections.observableArrayList();
    
    private Gson gson = new Gson();
 // 在AdminExchangeController中添加
    public void refreshData() {
        // 刷新退宿申请数据
        loadReturnApplications();
        // 刷新换宿申请数据
        loadExchangeApplications();
        // 清空详情面板
        clearDetailPanels();
    }

    // 辅助方法：清空详情面板内容
    private void clearDetailPanels() {
        // 清空退宿详情
        returnApplicant.setText("");
        returnRoom.setText("");
        returnTime.setText("");
        returnDetail.setText("");
        returnFeedback.setText("");
        
        // 清空换宿详情
        exchangeApplicant.setText("");
        exchangeOriginalRoom.setText("");
        exchangeTime.setText("");
        exchangeDetail.setText("");
        exchangeFeedback.setText("");
    }
    @FXML
    private void initialize() {
        System.out.println("管理员退换舍控制器初始化完成");

        // 初始化表格列
        initializeReturnTable();
        initializeExchangeTable();

        // 从服务器加载数据
        loadReturnApplications();
        loadExchangeApplications();

        // 设置表格选择监听
        setupTableSelectionListeners();
    }

    private void initializeReturnTable() {
        returnApplicantCol.setCellValueFactory(new PropertyValueFactory<>("applicantName"));
        returnRoomCol.setCellValueFactory(new PropertyValueFactory<>("roomInfo"));
        returnTimeCol.setCellValueFactory(new PropertyValueFactory<>("applyTimeFormatted"));
        returnStatusCol.setCellValueFactory(new PropertyValueFactory<>("statusText"));
        returnTable.setItems(returnApplications);
    }

    private void initializeExchangeTable() {
        exchangeApplicantCol.setCellValueFactory(new PropertyValueFactory<>("applicantName"));
        exchangeOriginalRoomCol.setCellValueFactory(new PropertyValueFactory<>("roomInfo"));
        exchangeTimeCol.setCellValueFactory(new PropertyValueFactory<>("applyTimeFormatted"));
        exchangeStatusCol.setCellValueFactory(new PropertyValueFactory<>("statusText"));
        exchangeTable.setItems(exchangeApplications);
    }

    private void loadReturnApplications() {
        // 在新线程中从服务器获取退宿申请数据
        new Thread(() -> {
            try {
                // 添加同步块，确保流操作的线程安全
                synchronized (SocketManager.getInstance()) {
                    DataOutputStream out = SocketManager.getInstance().getOut();
                    DataInputStream in = SocketManager.getInstance().getIn();
                    
                    // 发送请求类型和参数
                    out.writeUTF("ExchangeListRequest");
                    out.writeUTF("{\"exchangeType\":0}"); // 0表示退宿
                    out.flush();
                    
                    // 接收响应
                    String response = in.readUTF();
                    if (response.startsWith("SUCCESS")) {
                        String jsonData = response.substring(8); // 跳过"SUCCESS|"
                        List<ExchangeApplication> applications = gson.fromJson(jsonData, 
                            new TypeToken<List<ExchangeApplication>>(){}.getType());
                        
                        // 更新UI
                        Platform.runLater(() -> {
                            returnApplications.clear();
                            returnApplications.addAll(applications);
                            returnTable.refresh();
                        });
                    } else {
                        String errorMsg = response.substring(6); // 跳过"ERROR|"
                        Platform.runLater(() -> 
                            showAlert(Alert.AlertType.ERROR, "加载失败", errorMsg)
                        );
                    }
                }
            } catch (IOException e) {
                Platform.runLater(() -> 
                    showAlert(Alert.AlertType.ERROR, "网络错误", "无法连接到服务器: " + e.getMessage())
                );
            }
        }).start();
    }
    private void loadExchangeApplications() {
        // 在新线程中从服务器获取换宿申请数据
        new Thread(() -> {
            try {
                // 添加同步块，确保流操作的线程安全
                synchronized (SocketManager.getInstance()) {
                    DataOutputStream out = SocketManager.getInstance().getOut();
                    DataInputStream in = SocketManager.getInstance().getIn();
                    
                    // 发送请求类型和参数
                    out.writeUTF("ExchangeListRequest");
                    out.writeUTF("{\"exchangeType\":1}"); // 1表示换宿
                    out.flush();
                    
                    // 接收响应
                    String response = in.readUTF();
                    if (response.startsWith("SUCCESS")) {
                        String jsonData = response.substring(8); // 跳过"SUCCESS|"
                        List<ExchangeApplication> applications = gson.fromJson(jsonData, 
                            new TypeToken<List<ExchangeApplication>>(){}.getType());
                        
                        // 更新UI
                        Platform.runLater(() -> {
                            exchangeApplications.clear();
                            exchangeApplications.addAll(applications);
                            exchangeTable.refresh();
                        });
                    } else {
                        String errorMsg = response.substring(6); // 跳过"ERROR|"
                        Platform.runLater(() -> 
                            showAlert(Alert.AlertType.ERROR, "加载失败", errorMsg)
                        );
                    }
                }
            } catch (IOException e) {
                Platform.runLater(() -> 
                    showAlert(Alert.AlertType.ERROR, "网络错误", "无法连接到服务器: " + e.getMessage())
                );
            }
        }).start();
    }
    private void setupTableSelectionListeners() {
        // 退舍申请表格选择监听
        returnTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            if (newSelection != null) {
                returnApplicant.setText(newSelection.getApplicantName());
                returnRoom.setText(newSelection.getRoomInfo());
                returnTime.setText(newSelection.getApplyTimeFormatted());
                returnDetail.setText(newSelection.getReason());
            }
        });

        // 换舍申请表格选择监听
        exchangeTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            if (newSelection != null) {
                exchangeApplicant.setText(newSelection.getApplicantName());
                exchangeOriginalRoom.setText(newSelection.getRoomInfo());
                exchangeTime.setText(newSelection.getApplyTimeFormatted());
                exchangeDetail.setText(newSelection.getReason());
            }
        });
    }

    @FXML
    private void handleReturnApprove() {
        ExchangeApplication selected = returnTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert(Alert.AlertType.WARNING, "操作失败", "请先选择一个申请");
            return;
        }

        String feedback = returnFeedback.getText().trim();
        if (feedback.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "操作失败", "请填写处理意见");
            return;
        }

        // 发送批准请求到服务器
        updateApplicationStatus(selected.getId(), 1, feedback, "退宿");
    }

    @FXML
    private void handleReturnReject() {
        ExchangeApplication selected = returnTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert(Alert.AlertType.WARNING, "操作失败", "请先选择一个申请");
            return;
        }

        String feedback = returnFeedback.getText().trim();
        if (feedback.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "操作失败", "请填写处理意见");
            return;
        }

        // 发送拒绝请求到服务器
        updateApplicationStatus(selected.getId(), 2, feedback, "退宿");
    }

    @FXML
    private void handleExchangeApprove() {
        ExchangeApplication selected = exchangeTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert(Alert.AlertType.WARNING, "操作失败", "请先选择一个申请");
            return;
        }

        String feedback = exchangeFeedback.getText().trim();
        if (feedback.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "操作失败", "请填写处理意见");
            return;
        }

        // 发送批准请求到服务器
        updateApplicationStatus(selected.getId(), 1, feedback, "换宿");
    }

    @FXML
    private void handleExchangeReject() {
        ExchangeApplication selected = exchangeTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert(Alert.AlertType.WARNING, "操作失败", "请先选择一个申请");
            return;
        }

        String feedback = exchangeFeedback.getText().trim();
        if (feedback.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "操作失败", "请填写处理意见");
            return;
        }

        // 发送拒绝请求到服务器
        updateApplicationStatus(selected.getId(), 2, feedback, "换宿");
    }

    private void updateApplicationStatus(int applicationId, int newStatus, String feedback, String type) {
        // 在新线程中发送状态更新请求到服务器
        new Thread(() -> {
            try {
                // 添加同步块，确保流操作的线程安全
                synchronized (SocketManager.getInstance()) {
                    DataOutputStream out = SocketManager.getInstance().getOut();
                    DataInputStream in = SocketManager.getInstance().getIn();
                    
                    // 发送请求类型
                    out.writeUTF("ExchangeStatusUpdate");
                    
                    // 发送请求参数
                    String jsonRequest = String.format(
                        "{\"applicationId\":%d,\"newStatus\":%d,\"feedback\":\"%s\"}",
                        applicationId, newStatus, feedback
                    );
                    out.writeUTF(jsonRequest);
                    out.flush();
                    
                    // 接收响应
                    String response = in.readUTF();
                    if (response.startsWith("SUCCESS")) {
                        Platform.runLater(() -> {
                            showAlert(Alert.AlertType.INFORMATION, "操作成功", 
                                     type + "申请已" + (newStatus == 1 ? "批准" : "拒绝"));
                            
                            // 清空反馈框
                            if (type.equals("退宿")) {
                                returnFeedback.clear();
                                loadReturnApplications(); // 刷新退宿申请列表
                            } else {
                                exchangeFeedback.clear();
                                loadExchangeApplications(); // 刷新换宿申请列表
                            }
                        });
                    } else {
                        String errorMsg = response.substring(response.indexOf("|") + 1);
                        Platform.runLater(() -> 
                            showAlert(Alert.AlertType.ERROR, "操作失败", errorMsg)
                        );
                    }
                }
            } catch (IOException e) {
                Platform.runLater(() -> 
                    showAlert(Alert.AlertType.ERROR, "网络错误", "无法连接到服务器: " + e.getMessage())
                );
            }
        }).start();
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    // 申请数据模型类
    public static class ExchangeApplication {
        private int id;
        private String studentId;
        private String studentName;
        private String building;
        private String room;
        private String bed;
        private String targetBuilding;
        private String targetRoom;
        private int exchangeType;
        private String reason;
        private String applyTime;
        private int status;
        
        // Getter和Setter方法
        public int getId() { return id; }
        public void setId(int id) { this.id = id; }
        
        public String getStudentId() { return studentId; }
        public void setStudentId(String studentId) { this.studentId = studentId; }
        
        public String getStudentName() { return studentName; }
        public void setStudentName(String studentName) { this.studentName = studentName; }
        
        public String getBuilding() { return building; }
        public void setBuilding(String building) { this.building = building; }
        
        public String getRoom() { return room; }
        public void setRoom(String room) { this.room = room; }
        
        public String getBed() { return bed; }
        public void setBed(String bed) { this.bed = bed; }
        
        public String getTargetBuilding() { return targetBuilding; }
        public void setTargetBuilding(String targetBuilding) { this.targetBuilding = targetBuilding; }
        
        public String getTargetRoom() { return targetRoom; }
        public void setTargetRoom(String targetRoom) { this.targetRoom = targetRoom; }
        
        public int getExchangeType() { return exchangeType; }
        public void setExchangeType(int exchangeType) { this.exchangeType = exchangeType; }
        
        public String getReason() { return reason; }
        public void setReason(String reason) { this.reason = reason; }
        
        public String getApplyTime() { return applyTime; }
        public void setApplyTime(String applyTime) { this.applyTime = applyTime; }
        
        public int getStatus() { return status; }
        public void setStatus(int status) { this.status = status; }
        
        // 辅助方法
        public String getApplicantName() {
            return studentName + " (" + studentId + ")";
        }
        
        public String getRoomInfo() {
            return building + "栋 " + room + "室 " + bed + "床";
        }
        
        public String getTargetRoomInfo() {
            if (targetBuilding != null && targetRoom != null) {
                return targetBuilding + "栋 " + targetRoom + "室";
            }
            return "未指定";
        }
        
        public String getApplyTimeFormatted() {
            return applyTime; // 服务器返回的已经是格式化后的时间字符串
        }
        
        public String getStatusText() {
            switch (status) {
                case 0: return "待审核";
                case 1: return "已通过";
                case 2: return "已拒绝";
                default: return "未知状态";
            }
        }
        
        public String getTypeText() {
            return exchangeType == 0 ? "退宿" : "换宿";
        }
    }
}