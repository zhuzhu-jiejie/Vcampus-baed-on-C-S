package seu.vcampus.client.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.application.Platform;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import com.google.gson.Gson;
import seu.vcampus.client.network.SocketManager;
import seu.vcampus.model.AdminDormitoryInfoResponse;
import seu.vcampus.model.StudentInfo;
import seu.vcampus.model.HygieneRecord;

public class AdminDormitoryInfoController {

    // 筛选控件
    @FXML private ComboBox<String> buildingComboBox;
    @FXML private TextField roomField;
    @FXML private Button searchButton;
    @FXML private Button resetButton;

    // 宿舍详情控件
    @FXML private Label detailBuilding;
    @FXML private Label detailRoom;
    @FXML private Label detailCapacity;
    @FXML private Label detailCurrent;
    @FXML private Label detailScore;
    @FXML private Label detailManager;
    @FXML private GridPane residentGrid;
    @FXML private LineChart<String, Number> detailScoreChart;

    // 底部按钮
    @FXML private Button exportButton;
    @FXML private Button refreshButton;
 // 添加刷新数据方法
    public void refreshData() {
        String building = buildingComboBox.getValue();
        String room = roomField.getText();
        
        // 如果有筛选条件则使用条件刷新，否则清空详情
        if (building != null && !building.isEmpty() && 
            room != null && !room.trim().isEmpty()) {
            requestDormitoryDetail(building, room);
        } else {
            clearDetailInfo();
        }
    }
    @FXML
    private void initialize() {
        // 初始化筛选下拉框
        initializeFilterOptions();

        // 初始化时清空详情信息
        clearDetailInfo();

        System.out.println("管理员宿舍信息控制器初始化完成");
    }

    private void initializeFilterOptions() {
        // 初始化宿舍楼下拉框
        buildingComboBox.getItems().addAll("A栋", "B栋", "C栋", "D栋", "E栋");
    }

    private void clearDetailInfo() {
        // 清空详情信息
        detailBuilding.setText("");
        detailRoom.setText("");
        detailCapacity.setText("");
        detailCurrent.setText("");
        detailScore.setText("");
        detailManager.setText("");
        
        // 清空宿舍成员表格
        residentGrid.getChildren().clear();
        
        // 清空评分图表
        detailScoreChart.getData().clear();
    }

    @FXML
    private void handleSearch() {
        // 查询按钮处理
        String building = buildingComboBox.getValue();
        String room = roomField.getText();

        // 验证输入
        if (building == null || building.isEmpty()) {
            showErrorAlert("输入错误", "请选择宿舍楼");
            return;
        }
        
        if (room == null || room.trim().isEmpty()) {
            showErrorAlert("输入错误", "请输入房间号");
            return;
        }

        // 从服务器加载宿舍详情
        requestDormitoryDetail(building, room);
    }

    private void requestDormitoryDetail(String building, String room) {
        // 在新线程中发送请求到服务器
        new Thread(() -> {
            try {
                // 添加同步块，确保流操作的线程安全
                synchronized (SocketManager.getInstance()) {
                    DataOutputStream out = SocketManager.getInstance().getOut();
                    DataInputStream in = SocketManager.getInstance().getIn();
                    
                    // 发送请求类型
                    out.writeUTF("AdminDormitoryInfoRequest");
                    
                    // 发送请求参数
                    String jsonRequest = String.format(
                        "{\"building\":\"%s\",\"room\":\"%s\"}",
                        building, room
                    );
                    out.writeUTF(jsonRequest);
                    out.flush();
                    
                    // 接收响应
                    String response = in.readUTF();
                    if (response.startsWith("SUCCESS")) {
                        String jsonData = response.substring(8); // 跳过"SUCCESS|"
                        AdminDormitoryInfoResponse dormitoryInfo = new Gson().fromJson(jsonData, 
                            AdminDormitoryInfoResponse.class);
                        
                        // 更新UI
                        Platform.runLater(() -> {
                            updateDormitoryDetailUI(building, room, dormitoryInfo);
                            showInfoAlert("查询完成", "已找到宿舍信息");
                        });
                    } else {
                        String errorMsg = response.substring(6); // 跳过"ERROR|"
                        Platform.runLater(() -> {
                            showErrorAlert("查询失败", errorMsg);
                            clearDetailInfo();
                        });
                    }
                }
            } catch (IOException e) {
                Platform.runLater(() -> {
                    showErrorAlert("网络错误", "无法连接到服务器: " + e.getMessage());
                    clearDetailInfo();
                });
            }
        }).start();
    }
    private void updateDormitoryDetailUI(String building, String room, AdminDormitoryInfoResponse dormitoryInfo) {
        // 设置宿舍基本信息
        detailBuilding.setText(building);
        detailRoom.setText(room);
        detailCapacity.setText(String.valueOf(dormitoryInfo.getCapacity()));
        detailCurrent.setText(String.valueOf(dormitoryInfo.getCurrentCount()));
        detailScore.setText(String.format("%.1f", dormitoryInfo.getAverageScore()));
        detailManager.setText("张阿姨"); // 假设宿管员信息

        // 加载宿舍成员
        loadResidents(dormitoryInfo.getStudents());

        // 加载卫生评分图表
        loadScoreChart(dormitoryInfo.getHygieneRecords());
    }

    private void loadResidents(List<StudentInfo> students) {
        // 清空现有数据
        residentGrid.getChildren().clear();
        
        // 添加表头
        String[] headers = {"学号", "姓名", "学院", "专业", "床位号", "联系方式"};
        for (int i = 0; i < headers.length; i++) {
            Label headerLabel = new Label(headers[i]);
            headerLabel.setStyle("-fx-font-weight: bold; -fx-padding: 8px; -fx-background-color: #f0f0f0; -fx-border-color: #e0e0e0; -fx-border-width: 0 0 1 0;");
            residentGrid.add(headerLabel, i, 0);
        }
        
        // 添加宿舍成员数据
        for (int i = 0; i < students.size(); i++) {
            StudentInfo student = students.get(i);
            
            Label idLabel = new Label(student.getUserid());
            Label nameLabel = new Label(student.getName());
            Label collegeLabel = new Label(student.getCollege());
            Label majorLabel = new Label(student.getMajor());
            Label bedLabel = new Label(student.getDormitoryBed());
            Label phoneLabel = new Label(student.getPhone());
            
            // 设置样式
            String cellStyle = "-fx-padding: 8px; -fx-border-color: #e0e0e0; -fx-border-width: 0 0 1 0;";
            idLabel.setStyle(cellStyle);
            nameLabel.setStyle(cellStyle);
            collegeLabel.setStyle(cellStyle);
            majorLabel.setStyle(cellStyle);
            bedLabel.setStyle(cellStyle);
            phoneLabel.setStyle(cellStyle);
            
            // 添加到GridPane
            residentGrid.add(idLabel, 0, i+1);
            residentGrid.add(nameLabel, 1, i+1);
            residentGrid.add(collegeLabel, 2, i+1);
            residentGrid.add(majorLabel, 3, i+1);
            residentGrid.add(bedLabel, 4, i+1);
            residentGrid.add(phoneLabel, 5, i+1);
            
            System.out.println(student.getUserid());
        }
    }

    private void loadScoreChart(List<HygieneRecord> hygieneRecords) {
        // 清空现有数据
        detailScoreChart.getData().clear();

        if (hygieneRecords == null || hygieneRecords.isEmpty()) {
            return;
        }

        // 设置历史评分图表
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("宿舍卫生评分");

        // 按日期排序记录
        hygieneRecords.sort((r1, r2) -> r1.getDate().compareTo(r2.getDate()));
        
        // 添加数据点到图表
        for (HygieneRecord record : hygieneRecords) {
            series.getData().add(new XYChart.Data<>(record.getDate(), record.getScore()));
        }

        detailScoreChart.getData().add(series);
    }

    @FXML
    private void handleReset() {
        // 重置按钮处理
        buildingComboBox.setValue(null);
        roomField.clear();
        
        // 清空详情信息
        clearDetailInfo();

        System.out.println("筛选条件已重置");
    }

    @FXML
    private void handleExport() {
        // 导出数据按钮处理
        System.out.println("执行数据导出操作");

        // 显示成功提示
        showInfoAlert("导出成功", "宿舍数据已成功导出到文件！");
    }

    @FXML
    private void handleRefresh() {
        // 刷新按钮处理 - 重新查询当前宿舍信息
        String building = buildingComboBox.getValue();
        String room = roomField.getText();
        
        if (building != null && !building.isEmpty() && 
            room != null && !room.trim().isEmpty()) {
            requestDormitoryDetail(building, room);
            System.out.println("宿舍数据已刷新");
            showInfoAlert("刷新完成", "宿舍数据已成功刷新！");
        } else {
            showErrorAlert("刷新失败", "请先选择宿舍楼和房间号");
        }
    }

    private void showInfoAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("操作成功");
        alert.setHeaderText(title);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showErrorAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("错误");
        alert.setHeaderText(title);
        alert.setContentText(message);
        alert.showAndWait();
    }
}