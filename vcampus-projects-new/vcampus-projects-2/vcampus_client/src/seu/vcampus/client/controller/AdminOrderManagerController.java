package seu.vcampus.client.controller;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.URL;
import java.util.ResourceBundle;

import com.google.gson.Gson;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;
import seu.vcampus.client.network.SocketManager;
import seu.vcampus.model.Order;
import seu.vcampus.model.OrderHistoryRequest;
import seu.vcampus.model.OrderHistoryResponse;
import seu.vcampus.model.OrderItem;
import seu.vcampus.model.OrderStatusUpdateRequest;

public class AdminOrderManagerController implements Initializable {

    // 原有的FXML注入保持不变
    @FXML private TableView<Order> ordersTable;
    @FXML private TableColumn<Order, String> colOrderId;
    @FXML private TableColumn<Order, String> colOrderDate;
    @FXML private TableColumn<Order, String> colOrderStatus;
    @FXML private TableColumn<Order, Double> colOrderTotal;
    @FXML private TextField searchField;
    @FXML private ComboBox<String> statusFilter;
    @FXML private TextArea orderDetailsArea;
    @FXML private Button shipOrderButton;
    @FXML private Button completeOrderButton;
    @FXML private TableColumn<Order, String> colUserId;



    private ObservableList<Order> ordersList = FXCollections.observableArrayList();
    private Order selectedOrder = null;
    private Gson gson = new Gson();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // 初始化表格列
        colOrderId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colOrderDate.setCellValueFactory(new PropertyValueFactory<>("date"));
        colOrderStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        colOrderTotal.setCellValueFactory(new PropertyValueFactory<>("total"));
        colUserId.setCellValueFactory(new PropertyValueFactory<>("userId"));
        // 初始化状态筛选
        statusFilter.getItems().addAll("所有状态", "pending", "paid", "shipped", "completed", "cancelled");
        statusFilter.setValue("所有状态");

        // 加载订单数据
        loadOrders();

        // 设置表格选择监听
        ordersTable.getSelectionModel().selectedItemProperty().addListener(
            (observable, oldValue, newValue) -> selectOrder(newValue)
        );

        // 初始禁用操作按钮
        updateOrderButtons();
    }

    private void loadOrders() {
        new Thread(() -> {
            try {
                DataOutputStream out = SocketManager.getInstance().getOut();
                DataInputStream in = SocketManager.getInstance().getIn();

                // 发送请求类型
                out.writeUTF("OrderHistoryRequest");

                // 发送获取订单历史的请求
                OrderHistoryRequest req = new OrderHistoryRequest("all");
                out.writeUTF(gson.toJson(req));
                out.flush();

                // 接收服务器响应
                String response = in.readUTF();
                System.out.println("收到服务器响应: " + response);

                if (response.startsWith("ERROR|")) {
                    // 处理错误信息
                    String errorMsg = response.substring(6);
                    Platform.runLater(() -> showAlert("错误", "获取订单信息失败: " + errorMsg));
                } else if (response.startsWith("SUCCESS|")) {
                    // 处理成功响应
                    String jsonStr = response.substring(8);
                    OrderHistoryResponse info = gson.fromJson(jsonStr, OrderHistoryResponse.class);

                    // 在UI线程中更新界面
                    Platform.runLater(() -> {
                        ordersList.clear();
                        ordersList.addAll(info.getOrders());
                        ordersTable.setItems(ordersList);
                    });
                } else {
                    Platform.runLater(() ->
                        showAlert("错误", "服务器返回了未知格式的响应: " + response)
                    );
                }

            } catch (Exception e) {
                Platform.runLater(() ->
                    showAlert("错误", "获取订单信息失败: " + e.getMessage())
                );
                e.printStackTrace();
            }
        }).start();
    }

    private void selectOrder(Order order) {
        selectedOrder = order;

        if (order != null) {
            // 显示订单详情
            StringBuilder details = new StringBuilder();
            details.append("订单号: ").append(order.getId()).append("\n");
            details.append("用户ID: ").append(order.getUserId()).append("\n");
            details.append("下单时间: ").append(order.getDate()).append("\n");
            details.append("订单状态: ").append(getStatusText(order.getStatus())).append("\n");
            details.append("订单总额: ¥").append(String.format("%.2f", order.getTotal())).append("\n\n");
            details.append("商品列表:\n");

            for (OrderItem item : order.getItems()) {
                details.append("  - ").append(item.getName())
                      .append(" × ").append(item.getQuantity())
                      .append(" = ¥").append(String.format("%.2f", item.getSubtotal()))
                      .append("\n");
            }

            orderDetailsArea.setText(details.toString());
        } else {
            orderDetailsArea.clear();
        }

        // 更新按钮状态
        updateOrderButtons();
    }

    private String getStatusText(String status) {
        switch (status) {
            case "pending": return "待付款";
            case "paid": return "已付款";
            case "shipped": return "已发货";
            case "completed": return "已完成";
            case "cancelled": return "已取消";
            default: return "未知状态";
        }
    }

    private void updateOrderButtons() {
        if (selectedOrder == null) {
            shipOrderButton.setDisable(true);
            completeOrderButton.setDisable(true);
            return;
        }

        String status = selectedOrder.getStatus();

        // 只能对已付款的订单进行发货操作
        shipOrderButton.setDisable(!status.equals("paid"));
        // 只能对已发货的订单进行完成操作
        completeOrderButton.setDisable(!status.equals("shipped"));
    }

    @FXML
    private void handleShipOrder() {
        if (selectedOrder != null && selectedOrder.getStatus().equals("paid")) {
            // 发送发货请求
            new Thread(() -> {
                try {
                    DataOutputStream out = SocketManager.getInstance().getOut();
                    DataInputStream in = SocketManager.getInstance().getIn();

                    // 发送请求类型
                    out.writeUTF("OrderStatusUpdateRequest");

                    // 发送更新订单状态请求
                    OrderStatusUpdateRequest req = new OrderStatusUpdateRequest(selectedOrder.getId(), "shipped");
                    out.writeUTF(gson.toJson(req));
                    out.flush();

                    // 接收服务器响应
                    String response = in.readUTF();
                    System.out.println("收到服务器响应: " + response);

                    if (response.startsWith("ERROR|")) {
                        // 处理错误信息
                        String errorMsg = response.substring(6);
                        Platform.runLater(() -> showAlert("错误", "更新订单状态失败: " + errorMsg));
                    } else if (response.startsWith("SUCCESS|")) {
                        // 处理成功响应
                        Platform.runLater(() -> {
                            selectedOrder.setStatus("shipped");
                            ordersTable.refresh();
                            selectOrder(selectedOrder);
                            showAlert("成功", "订单已标记为已发货");
                        });
                    } else {
                        Platform.runLater(() ->
                            showAlert("错误", "服务器返回了未知格式的响应: " + response)
                        );
                    }

                } catch (Exception e) {
                    Platform.runLater(() ->
                        showAlert("错误", "更新订单状态失败: " + e.getMessage())
                    );
                    e.printStackTrace();
                }
            }).start();
        }
    }

    @FXML
    private void handleCompleteOrder() {
        if (selectedOrder != null && selectedOrder.getStatus().equals("shipped")) {
            // 发送完成订单请求
            new Thread(() -> {
                try {
                    DataOutputStream out = SocketManager.getInstance().getOut();
                    DataInputStream in = SocketManager.getInstance().getIn();

                    // 发送请求类型
                    out.writeUTF("OrderStatusUpdateRequest");

                    // 发送更新订单状态请求
                    OrderStatusUpdateRequest req = new OrderStatusUpdateRequest(selectedOrder.getId(), "completed");
                    out.writeUTF(gson.toJson(req));
                    out.flush();

                    // 接收服务器响应
                    String response = in.readUTF();
                    System.out.println("收到服务器响应: " + response);

                    if (response.startsWith("ERROR|")) {
                        // 处理错误信息
                        String errorMsg = response.substring(6);
                        Platform.runLater(() -> showAlert("错误", "更新订单状态失败: " + errorMsg));
                    } else if (response.startsWith("SUCCESS|")) {
                        // 处理成功响应
                        Platform.runLater(() -> {
                            selectedOrder.setStatus("completed");
                            ordersTable.refresh();
                            selectOrder(selectedOrder);
                            showAlert("成功", "订单已标记为已完成");
                        });
                    } else {
                        Platform.runLater(() ->
                            showAlert("错误", "服务器返回了未知格式的响应: " + response)
                        );
                    }

                } catch (Exception e) {
                    Platform.runLater(() ->
                        showAlert("错误", "更新订单状态失败: " + e.getMessage())
                    );
                    e.printStackTrace();
                }
            }).start();
        }
    }

    @FXML
    private void handleSearch() {
        String keyword = searchField.getText().toLowerCase();
        String status = statusFilter.getValue();

        new Thread(() -> {
            try {
                DataOutputStream out = SocketManager.getInstance().getOut();
                DataInputStream in = SocketManager.getInstance().getIn();

                // 发送请求类型
                out.writeUTF("OrderHistoryRequest");

                // 发送获取订单历史的请求
                String searchStatus = "所有状态".equals(status) ? "all" : status;
                OrderHistoryRequest req = new OrderHistoryRequest(searchStatus);
                out.writeUTF(gson.toJson(req));
                out.flush();

                // 接收服务器响应
                String response = in.readUTF();
                System.out.println("收到服务器响应: " + response);

                if (response.startsWith("ERROR|")) {
                    // 处理错误信息
                    String errorMsg = response.substring(6);
                    Platform.runLater(() -> showAlert("错误", "搜索订单失败: " + errorMsg));
                } else if (response.startsWith("SUCCESS|")) {
                    // 处理成功响应
                    String jsonStr = response.substring(8);
                    OrderHistoryResponse info = gson.fromJson(jsonStr, OrderHistoryResponse.class);

                    // 本地筛选关键词
                    if (keyword != null && !keyword.isEmpty()) {
                        info.getOrders().removeIf(order ->
                            !order.getId().toLowerCase().contains(keyword)
                        );
                    }

                    // 在UI线程中更新界面
                    Platform.runLater(() -> {
                        ObservableList<Order> filteredList = FXCollections.observableArrayList(info.getOrders());
                        ordersTable.setItems(filteredList);
                    });
                } else {
                    Platform.runLater(() ->
                        showAlert("错误", "服务器返回了未知格式的响应: " + response)
                    );
                }

            } catch (Exception e) {
                Platform.runLater(() ->
                    showAlert("错误", "搜索订单失败: " + e.getMessage())
                );
                e.printStackTrace();
            }
        }).start();
    }

    @FXML
    private void handleClearFilter() {
        searchField.clear();
        statusFilter.setValue("所有状态");
        loadOrders();
    }

    // 其他导航方法保持不变...
    @FXML
    private void handleNavigateToProducts() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("AdminProductManager.fxml"));
            Stage stage = (Stage) ordersTable.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("商品管理 - 商店管理系统");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleNavigateToUsers() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("AdminUserManager.fxml"));
            Stage stage = (Stage) ordersTable.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("用户管理 - 商店管理系统");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleNavigateToReports() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("AdminReports.fxml"));
            Stage stage = (Stage) ordersTable.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("报表统计 - 商店管理系统");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleLogout() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("AdminLogin.fxml"));
            Stage stage = (Stage) ordersTable.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("管理员登录 - 商店管理系统");
        } catch (Exception e) {
            e.printStackTrace();
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