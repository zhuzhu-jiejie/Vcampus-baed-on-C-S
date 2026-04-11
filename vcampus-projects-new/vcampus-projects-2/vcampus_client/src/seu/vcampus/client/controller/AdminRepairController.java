// AdminRepairController.java
package seu.vcampus.client.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.List;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import javafx.application.Platform;
import seu.vcampus.client.network.SocketManager;

public class AdminRepairController {

    @FXML
    private TableView<RepairApplication> repairTable;

    @FXML
    private TableColumn<RepairApplication, Integer> idColumn;

    @FXML
    private TableColumn<RepairApplication, String> dormBuildingColumn;

    @FXML
    private TableColumn<RepairApplication, String> dormRoomColumn;

    @FXML
    private TableColumn<RepairApplication, String> descriptionColumn;

    @FXML
    private TableColumn<RepairApplication, String> statusColumn;

    @FXML
    private TableColumn<RepairApplication, String> dateColumn;

    @FXML
    private Button refreshBtn;

    @FXML
    private Button processBtn;

    private Gson gson = new Gson();

    public void refreshData() {
        loadDataFromServer();
    }
    @FXML
    private void initialize() {
        // 初始化表格列
        idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        dormBuildingColumn.setCellValueFactory(new PropertyValueFactory<>("dormBuilding"));
        dormRoomColumn.setCellValueFactory(new PropertyValueFactory<>("dormRoom"));
        descriptionColumn.setCellValueFactory(new PropertyValueFactory<>("description"));
        statusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));
        dateColumn.setCellValueFactory(new PropertyValueFactory<>("date"));

        // 从服务器加载数据
        loadDataFromServer();

        System.out.println("报修管理控制器初始化完成");
    }

    @FXML
    private void handleRefresh() {
        // 刷新表格数据
        loadDataFromServer();
        showAlert(Alert.AlertType.INFORMATION, "刷新成功", "报修数据已刷新");
    }

    @FXML
    private void handleProcessRepair() {
        RepairApplication selected = repairTable.getSelectionModel().getSelectedItem();

        if (selected == null) {
            showAlert(Alert.AlertType.WARNING, "操作失败", "请选择要处理的报修申请");
            return;
        }

        // 在新线程中发送处理请求到服务器
        new Thread(() -> {
            try {
                DataOutputStream out;
                DataInputStream in;
                synchronized (SocketManager.getInstance()) {
                    out = SocketManager.getInstance().getOut();
                    in = SocketManager.getInstance().getIn();
                    
                    // 发送请求类型
                    out.writeUTF("ProcessRepairRequest");
                    
                    // 发送维修ID
                    out.writeUTF(String.valueOf(selected.getId()));
                    out.flush();
                    
                    // 接收响应
                    String response = in.readUTF();
                    if (response.startsWith("SUCCESS")) {
                        Platform.runLater(() -> {
                            showAlert(Alert.AlertType.INFORMATION, "处理成功", "报修申请已标记为处理中");
                            // 刷新数据
                            loadDataFromServer();
                        });
                    } else {
                        String errorMsg = response.substring(response.indexOf("|") + 1);
                        Platform.runLater(() -> 
                            showAlert(Alert.AlertType.ERROR, "处理失败", errorMsg)
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

    private void loadDataFromServer() {
        // 在新线程中从服务器获取数据
        new Thread(() -> {
            try {
                DataOutputStream out;
                DataInputStream in;
                synchronized (SocketManager.getInstance()) {
                    out = SocketManager.getInstance().getOut();
                    in = SocketManager.getInstance().getIn();
                    
                    // 发送请求类型
                    out.writeUTF("RepairListRequest");
                    out.flush();
                    
                    // 接收响应
                    String response = in.readUTF();
                    if (response.startsWith("SUCCESS")) {
                        String jsonData = response.substring(8); // 跳过"SUCCESS|"
                        List<RepairApplication> repairs = gson.fromJson(jsonData, 
                            new TypeToken<List<RepairApplication>>(){}.getType());
                        
                        // 更新UI
                        Platform.runLater(() -> {
                            repairTable.getItems().clear();
                            repairTable.getItems().addAll(repairs);
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

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    // 内部类，表示报修申请
    public static class RepairApplication {
        private int id;
        private String dormBuilding;
        private String dormRoom;
        private String description;
        private String status;
        private String date;

        public RepairApplication() {}

        public RepairApplication(int id, String dormBuilding, String dormRoom, 
                                String description, String status, String date) {
            this.id = id;
            this.dormBuilding = dormBuilding;
            this.dormRoom = dormRoom;
            this.description = description;
            this.status = status;
            this.date = date;
        }

        public int getId() {
            return id;
        }

        public void setId(int id) {
            this.id = id;
        }

        public String getDormBuilding() {
            return dormBuilding;
        }

        public void setDormBuilding(String dormBuilding) {
            this.dormBuilding = dormBuilding;
        }

        public String getDormRoom() {
            return dormRoom;
        }

        public void setDormRoom(String dormRoom) {
            this.dormRoom = dormRoom;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public String getDate() {
            return date;
        }

        public void setDate(String date) {
            this.date = date;
        }
    }
}