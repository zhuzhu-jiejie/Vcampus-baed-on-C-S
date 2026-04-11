package seu.vcampus.client.controller;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

import com.google.gson.JsonObject;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import javafx.application.Platform;
import seu.vcampus.client.network.SocketManager;
import seu.vcampus.model.AdminBillRecord;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.CheckBox;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ObservableValue;
import javafx.util.Callback;

public class AdminReminderController {

    // 筛选控件
    @FXML private ComboBox<String> buildingCombo;
    @FXML private TextField roomField;
    @FXML private ComboBox<String> monthCombo;
    @FXML private ComboBox<String> statusCombo;
    @FXML private ComboBox<String> reminderStatusCombo;
    @FXML private Button searchButton;
    @FXML private Button resetButton;

    // 表格控件 - 添加FXML注入的列
    @FXML private TableView<BillRecord> reminderTable;
    @FXML private TableColumn<BillRecord, Boolean> selectColumn;
    @FXML private TableColumn<BillRecord, String> buildingColumn;
    @FXML private TableColumn<BillRecord, String> roomColumn;
    @FXML private TableColumn<BillRecord, String> monthColumn;
    @FXML private TableColumn<BillRecord, Double> electricityColumn;
    @FXML private TableColumn<BillRecord, Double> waterColumn;
    @FXML private TableColumn<BillRecord, Double> totalColumn;
    @FXML private TableColumn<BillRecord, String> paymentStatusColumn;
    @FXML private TableColumn<BillRecord, String> deadlineColumn;
    @FXML private TableColumn<BillRecord, String> reminderStatusColumn;
    @FXML private TableColumn<BillRecord, String> lastReminderTimeColumn;

    // 操作按钮
    @FXML private Button selectAllButton;
    @FXML private Button remindButton;
    @FXML private Button exportButton;

    // 状态标签
    @FXML private Label statusLabel;

    // 数据集合
    private ObservableList<BillRecord> billData = FXCollections.observableArrayList();
    
    private Gson gson = new Gson();
 // 添加刷新数据方法
    public void refreshData() {
        updateStatus("正在加载数据...");
        loadBillDataFromServer();
    }
    @FXML
    private void initialize() {
        // 初始化筛选条件
        initializeFilters();
        
        // 初始化表格
        initializeTables();
        
        // 从服务器加载数据
        loadBillDataFromServer();
        
        // 更新状态
        updateStatus("就绪");
        
        System.out.println("催缴管理控制器初始化完成");
    }
    
    private void initializeFilters() {
        // 初始化宿舍楼选项
        buildingCombo.getItems().addAll("全部", "A栋", "B栋", "C栋", "D栋", "E栋");
        buildingCombo.setValue("全部");

        // 初始化月份选项
        monthCombo.getItems().addAll("全部", "2023-10", "2023-09", "2023-08", "2023-07", "2023-06");
        monthCombo.setValue("全部");

        // 初始化缴费状态选项
        statusCombo.getItems().addAll("全部", "未缴费", "已缴费");
        statusCombo.setValue("全部");

        // 初始化催缴状态选项
        reminderStatusCombo.getItems().addAll("全部", "未催缴", "已催缴");
        reminderStatusCombo.setValue("全部");
    }

    private void initializeTables() {
        // 设置表格可编辑
        reminderTable.setEditable(true);
        
        // 初始化选择列
        selectColumn.setCellValueFactory(new Callback<TableColumn.CellDataFeatures<BillRecord, Boolean>, ObservableValue<Boolean>>() {
            @Override
            public ObservableValue<Boolean> call(TableColumn.CellDataFeatures<BillRecord, Boolean> param) {
                return param.getValue().selectedProperty();
            }
        });
        selectColumn.setCellFactory(CheckBoxTableCell.forTableColumn(selectColumn));
        selectColumn.setEditable(true);

        // 初始化其他列
        buildingColumn.setCellValueFactory(new PropertyValueFactory<>("building"));
        roomColumn.setCellValueFactory(new PropertyValueFactory<>("room"));
        monthColumn.setCellValueFactory(new PropertyValueFactory<>("month"));
        electricityColumn.setCellValueFactory(new PropertyValueFactory<>("electricity"));
        waterColumn.setCellValueFactory(new PropertyValueFactory<>("water"));
        totalColumn.setCellValueFactory(new PropertyValueFactory<>("total"));
        paymentStatusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));
        deadlineColumn.setCellValueFactory(new PropertyValueFactory<>("deadline"));
        reminderStatusColumn.setCellValueFactory(new PropertyValueFactory<>("reminderStatus"));
        lastReminderTimeColumn.setCellValueFactory(new PropertyValueFactory<>("lastReminderTime"));

        reminderTable.setItems(billData);
    }

    private void loadBillDataFromServer() {
        // 构建筛选条件
        JsonObject filters = new JsonObject();
        String building = buildingCombo.getValue();
        String room = roomField.getText();
        String month = monthCombo.getValue();
        String status = statusCombo.getValue();
        String reminderStatus = reminderStatusCombo.getValue();
        
        if (!"全部".equals(building)) filters.addProperty("building", building);
        if (!room.isEmpty()) filters.addProperty("room", room);
        if (!"全部".equals(month)) filters.addProperty("month", month);
        if (!"全部".equals(status)) filters.addProperty("status", status);
        if (!"全部".equals(reminderStatus)) filters.addProperty("reminderStatus", reminderStatus);
        
        // 在新线程中从服务器获取数据
        new Thread(() -> {
            try {
                // 添加同步块，确保流操作的线程安全
                synchronized (SocketManager.getInstance()) {
                    DataOutputStream out = SocketManager.getInstance().getOut();
                    DataInputStream in = SocketManager.getInstance().getIn();
                    
                    // 发送请求类型
                    out.writeUTF("AdminBillListRequest");
                    
                    // 发送筛选条件
                    String jsonFilters = gson.toJson(filters);
                    out.writeUTF(jsonFilters);
                    out.flush();
                    
                    // 接收响应
                    String response = in.readUTF();

                    if (response.startsWith("SUCCESS")) {
                        String jsonData = response.substring(8); // 跳过"SUCCESS|"
                        List<AdminBillRecord> bills = gson.fromJson(jsonData, 
                            new TypeToken<List<AdminBillRecord>>(){}.getType());
                        
                        // 更新UI
                        Platform.runLater(() -> {
                            billData.clear();
                            for (AdminBillRecord bill : bills) {
                                billData.add(new BillRecord(
                                    false,
                                    bill.getBuilding(),
                                    bill.getRoom(),
                                    bill.getMonth(),
                                    bill.getElectricityFee(),
                                    bill.getWaterFee(),
                                    bill.getTotalFee(),
                                    bill.getStatus(),
                                    bill.getDeadline(),
                                    bill.getReminderStatus() != null ? bill.getReminderStatus() : "未催缴",
                                    bill.getLastReminderTime() != null ? bill.getLastReminderTime() : ""
                                ));
                            }
                            reminderTable.refresh();
                            updateStatus("就绪，共 " + billData.size() + " 条记录");
                        });
                    } else {
                        String errorMsg = response.substring(6); // 跳过"ERROR|"
                        Platform.runLater(() -> {
                            showAlert(Alert.AlertType.ERROR, "加载失败", errorMsg);
                            updateStatus("加载失败: " + errorMsg);
                        });
                    }
                }
            } catch (IOException e) {
                Platform.runLater(() -> {
                    showAlert(Alert.AlertType.ERROR, "网络错误", "无法连接到服务器: " + e.getMessage());
                    updateStatus("网络错误: " + e.getMessage());
                });
            }
        }).start();
    }

    @FXML
    private void handleSearch() {
        // 处理查询操作
        updateStatus("正在查询...");
        loadBillDataFromServer();
    }

    @FXML
    private void handleReset() {
        // 处理重置操作
        buildingCombo.setValue("全部");
        roomField.clear();
        monthCombo.setValue("全部");
        statusCombo.setValue("全部");
        reminderStatusCombo.setValue("全部");

        updateStatus("筛选条件已重置");
        loadBillDataFromServer();
    }

    @FXML
    private void handleSelectAll() {
        // 处理全选操作
        boolean allSelected = billData.stream().allMatch(BillRecord::isSelected);

        for (BillRecord record : billData) {
            record.setSelected(!allSelected);
        }

        reminderTable.refresh();
        updateStatus(allSelected ? "已取消全选" : "已全选所有记录");
    }

    @FXML
    private void handleRemind() {
        // 处理催缴操作
        List<BillRecord> selectedRecords = billData.stream()
                .filter(BillRecord::isSelected)
                .collect(Collectors.toList());

        if (selectedRecords.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "操作提示", "请先选择要催缴的记录");
            return;
        }

        updateStatus("正在发送催缴通知...");

        // 在新线程中发送催缴请求
        new Thread(() -> {
            try {
                int successCount = 0;
                int failCount = 0;
                
                for (BillRecord record : selectedRecords) {
                    // 每个请求都添加同步块，确保流操作的线程安全
                    synchronized (SocketManager.getInstance()) {
                        DataOutputStream out = SocketManager.getInstance().getOut();
                        DataInputStream in = SocketManager.getInstance().getIn();
                        
                        // 发送请求类型
                        out.writeUTF("AdminRemindRequest");
                        
                        // 发送催缴信息
                        JsonObject remindRequest = new JsonObject();
                        remindRequest.addProperty("building", record.getBuilding());
                        remindRequest.addProperty("room", record.getRoom());
                        remindRequest.addProperty("month", record.getMonth());
                        
                        String jsonRequest = gson.toJson(remindRequest);
                        out.writeUTF(jsonRequest);
                        out.flush();
                        
                        // 接收响应
                        String response = in.readUTF();
                        if (response.startsWith("SUCCESS")) {
                            successCount++;
                        } else {
                            failCount++;
                        }
                    }
                }
                
                final int finalSuccessCount = successCount;
                final int finalFailCount = failCount;
                
                Platform.runLater(() -> {
                    // 刷新数据
                    loadBillDataFromServer();
                    updateStatus("催缴通知发送完成，成功: " + finalSuccessCount + " 条，失败: " + finalFailCount + " 条");
                    showAlert(Alert.AlertType.INFORMATION, "操作完成", 
                             "催缴通知发送完成，成功: " + finalSuccessCount + " 条，失败: " + finalFailCount + " 条");
                });
                
            } catch (IOException e) {
                Platform.runLater(() -> {
                    showAlert(Alert.AlertType.ERROR, "网络错误", "无法连接到服务器: " + e.getMessage());
                    updateStatus("网络错误: " + e.getMessage());
                });
            }
        }).start();
    }

    @FXML
    private void handleExport() {
        // 处理导出操作
        updateStatus("正在导出数据...");

        // 模拟导出耗时
        new java.util.Timer().schedule(
            new java.util.TimerTask() {
                @Override
                public void run() {
                    Platform.runLater(() -> {
                        updateStatus("数据导出完成，共导出 " + billData.size() + " 条记录");
                        showAlert(Alert.AlertType.INFORMATION, "导出成功", "数据已成功导出到文件");
                    });
                }
            },
            2000
        );
    }

    private void updateStatus(String message) {
        statusLabel.setText(message);
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    // 内部类：账单记录
    public static class BillRecord {
        private final BooleanProperty selected;
        private final String building;
        private final String room;
        private final String month;
        private final double electricity;
        private final double water;
        private final double total;
        private final String status;
        private final String deadline;
        private String reminderStatus;
        private String lastReminderTime;

        public BillRecord(boolean selected, String building, String room, String month,
                         double electricity, double water, double total,
                         String status, String deadline, String reminderStatus, String lastReminderTime) {
            this.selected = new SimpleBooleanProperty(selected);
            this.building = building;
            this.room = room;
            this.month = month;
            this.electricity = electricity;
            this.water = water;
            this.total = total;
            this.status = status;
            this.deadline = deadline;
            this.reminderStatus = reminderStatus;
            this.lastReminderTime = lastReminderTime;
        }

        // Getter和Setter方法
        public boolean isSelected() { return selected.get(); }
        public void setSelected(boolean selected) { this.selected.set(selected); }
        public BooleanProperty selectedProperty() { return selected; }

        public String getBuilding() { return building; }
        public String getRoom() { return room; }
        public String getMonth() { return month; }
        public double getElectricity() { return electricity; }
        public double getWater() { return water; }
        public double getTotal() { return total; }
        public String getStatus() { return status; }
        public String getDeadline() { return deadline; }
        public String getReminderStatus() { return reminderStatus; }
        public void setReminderStatus(String reminderStatus) { this.reminderStatus = reminderStatus; }
        public String getLastReminderTime() { return lastReminderTime; }
        public void setLastReminderTime(String lastReminderTime) { this.lastReminderTime = lastReminderTime; }
    }
}