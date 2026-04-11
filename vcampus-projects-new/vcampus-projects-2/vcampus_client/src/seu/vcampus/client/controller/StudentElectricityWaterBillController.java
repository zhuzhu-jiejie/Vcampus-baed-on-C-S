package seu.vcampus.client.controller;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.Gson;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.util.Callback;
import seu.vcampus.client.network.SocketManager;
import seu.vcampus.model.CurrentBill;
import seu.vcampus.model.ElectricityWaterBillRequest;
import seu.vcampus.model.ElectricityWaterBillResponse;
import seu.vcampus.model.HistoryBill;

public class StudentElectricityWaterBillController {

    @FXML private Label buildingLabel;
    @FXML private Label roomLabel;
    @FXML private Label monthLabel;
    @FXML private Label statusLabel;
    @FXML private Label electricityLabel;
    @FXML private Label waterLabel;
    @FXML private Label totalLabel;
    @FXML private Label deadlineLabel;

    @FXML private TableView<Map<String, String>> historyTable;
    @FXML private Button refreshButton;

    private ObservableList<Map<String, String>> historyData = FXCollections.observableArrayList();
    private Gson gson = new Gson();
    private String studentIdValue;

    @FXML
    private void initialize() {
        setupTableColumns();
        System.out.println("水电费控制器初始化完成");
    }

    private void setupTableColumns() {
        // 设置历史记录表格列
        if (historyTable.getColumns().size() >= 6) {
            TableColumn<Map<String, String>, String> monthColumn = (TableColumn<Map<String, String>, String>) historyTable.getColumns().get(0);
            TableColumn<Map<String, String>, String> electricityColumn = (TableColumn<Map<String, String>, String>) historyTable.getColumns().get(1);
            TableColumn<Map<String, String>, String> waterColumn = (TableColumn<Map<String, String>, String>) historyTable.getColumns().get(2);
            TableColumn<Map<String, String>, String> totalColumn = (TableColumn<Map<String, String>, String>) historyTable.getColumns().get(3);
            TableColumn<Map<String, String>, String> statusColumn = (TableColumn<Map<String, String>, String>) historyTable.getColumns().get(4);
            TableColumn<Map<String, String>, String> payDateColumn = (TableColumn<Map<String, String>, String>) historyTable.getColumns().get(5);

            monthColumn.setCellValueFactory(new Callback<TableColumn.CellDataFeatures<Map<String, String>, String>, ObservableValue<String>>() {
                @Override
                public ObservableValue<String> call(TableColumn.CellDataFeatures<Map<String, String>, String> param) {
                    return new SimpleStringProperty(param.getValue().get("month"));
                }
            });

            electricityColumn.setCellValueFactory(new Callback<TableColumn.CellDataFeatures<Map<String, String>, String>, ObservableValue<String>>() {
                @Override
                public ObservableValue<String> call(TableColumn.CellDataFeatures<Map<String, String>, String> param) {
                    return new SimpleStringProperty(param.getValue().get("electricity"));
                }
            });

            waterColumn.setCellValueFactory(new Callback<TableColumn.CellDataFeatures<Map<String, String>, String>, ObservableValue<String>>() {
                @Override
                public ObservableValue<String> call(TableColumn.CellDataFeatures<Map<String, String>, String> param) {
                    return new SimpleStringProperty(param.getValue().get("water"));
                }
            });

            totalColumn.setCellValueFactory(new Callback<TableColumn.CellDataFeatures<Map<String, String>, String>, ObservableValue<String>>() {
                @Override
                public ObservableValue<String> call(TableColumn.CellDataFeatures<Map<String, String>, String> param) {
                    return new SimpleStringProperty(param.getValue().get("total"));
                }
            });

            statusColumn.setCellValueFactory(new Callback<TableColumn.CellDataFeatures<Map<String, String>, String>, ObservableValue<String>>() {
                @Override
                public ObservableValue<String> call(TableColumn.CellDataFeatures<Map<String, String>, String> param) {
                    return new SimpleStringProperty(param.getValue().get("status"));
                }
            });

            payDateColumn.setCellValueFactory(new Callback<TableColumn.CellDataFeatures<Map<String, String>, String>, ObservableValue<String>>() {
                @Override
                public ObservableValue<String> call(TableColumn.CellDataFeatures<Map<String, String>, String> param) {
                    return new SimpleStringProperty(param.getValue().get("payDate"));
                }
            });
        }
    }

    public void setStudentId(String studentId) {
        this.studentIdValue = studentId;
        loadBillData();
    }

    private void loadBillData() {
        new Thread(() -> {
            try {
                DataOutputStream out = SocketManager.getInstance().getOut();
                DataInputStream in = SocketManager.getInstance().getIn();

                // 发送请求类型
                out.writeUTF("ElectricityWaterBillRequest");

                // 发送获取水电费信息的请求
                ElectricityWaterBillRequest req = new ElectricityWaterBillRequest();
                req.setUserid(studentIdValue);
                out.writeUTF(gson.toJson(req));
                out.flush();

                // 接收服务器响应
                String response = in.readUTF();
                System.out.println("收到服务器响应: " + response);

                if (response.startsWith("ERROR|")) {
                    String errorMsg = response.substring(6);
                    javafx.application.Platform.runLater(() ->
                        showErrorAlert("获取失败", errorMsg));
                } else if (response.startsWith("SUCCESS|")) {

                    String jsonStr = response.substring(8);
                    ElectricityWaterBillResponse billResponse = gson.fromJson(jsonStr, ElectricityWaterBillResponse.class);

                    // 在UI线程中更新界面
                    javafx.application.Platform.runLater(() -> {
                        // 设置当前费用信息
                        buildingLabel.setText(billResponse.getBuilding());
                        roomLabel.setText(billResponse.getRoom());

                        CurrentBill currentBill = billResponse.getCurrentBill();
                        if (currentBill != null) {
                            monthLabel.setText(currentBill.getMonth());
                            statusLabel.setText(currentBill.getStatus());
                            electricityLabel.setText(String.format("%.2f", currentBill.getElectricityFee()));
                            waterLabel.setText(String.format("%.2f", currentBill.getWaterFee()));

                            double total = currentBill.getElectricityFee() + currentBill.getWaterFee();
                            totalLabel.setText(String.format("%.2f", total));

                            // 检查deadline是否为null，如果是则设置为空字符串或其他默认值
                            if (currentBill.getDeadline() != null) {
                                deadlineLabel.setText(currentBill.getDeadline().toString());
                            } else {
                                deadlineLabel.setText(""); // 或者设置为"无截止日期"等默认文本
                            }

                            // 根据缴费状态设置样式
                            if ("未缴费".equals(statusLabel.getText())) {
                                statusLabel.getStyleClass().add("status-unpaid");
                            } else {
                                statusLabel.getStyleClass().add("status-paid");
                            }
                        }

                        // 设置历史缴费记录
                        List<HistoryBill> historyBills = billResponse.getHistoryBills();
                        if (historyBills != null) {
                            loadHistoryData(historyBills);
                        }
                    });
                } else {
                    javafx.application.Platform.runLater(() ->
                        showErrorAlert("响应格式错误", "服务器返回了未知格式的响应: " + response));
                }
            } catch (Exception e) {
                javafx.application.Platform.runLater(() ->
                    showErrorAlert("连接错误", "获取水电费信息失败：" + e.getMessage()));
                e.printStackTrace();
            }
        }).start();
    }

    private void loadHistoryData(List<HistoryBill> historyBills) {
        // 清空现有数据
        historyData.clear();

        // 添加历史缴费记录
        for (HistoryBill bill : historyBills) {
            Map<String, String> record = createBillRecord(
                bill.getMonth(),
                String.format("%.2f", bill.getElectricityFee()),
                String.format("%.2f", bill.getWaterFee()),
                String.format("%.2f", bill.getTotalFee()),
                bill.getStatus(),
                bill.getPayDate() != null ? bill.getPayDate().toString() : ""
            );
            historyData.add(record);
        }

        // 设置表格数据
        historyTable.setItems(historyData);
    }

    private Map<String, String> createBillRecord(String month, String electricity, String water,
                                                String total, String status, String payDate) {
        Map<String, String> record = new HashMap<>();
        record.put("month", month);
        record.put("electricity", electricity);
        record.put("water", water);
        record.put("total", total);
        record.put("status", status);
        record.put("payDate", payDate);
        return record;
    }

    @FXML
    private void handleRefresh() {
        loadBillData();
        System.out.println("水电费数据已刷新");

        // 显示成功提示
        showAlert(Alert.AlertType.INFORMATION, "操作成功", "水电费数据已成功刷新！");
    }

    private void showErrorAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("错误");
        alert.setHeaderText(title);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}