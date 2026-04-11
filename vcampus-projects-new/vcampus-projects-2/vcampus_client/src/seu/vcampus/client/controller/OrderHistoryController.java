package seu.vcampus.client.controller;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import seu.vcampus.client.network.SocketManager;
import seu.vcampus.model.Order;
import seu.vcampus.model.OrderDetailRequest;
import seu.vcampus.model.OrderDetailResponse;
import seu.vcampus.model.OrderHistoryRequest;
import seu.vcampus.model.OrderHistoryResponse;
import seu.vcampus.model.OrderItem;
import seu.vcampus.model.OrderStatusUpdateRequest;
import seu.vcampus.model.PaymentRequest;
import seu.vcampus.model.PaymentResponse;

public class OrderHistoryController implements Initializable {

    @FXML
    private VBox ordersContainer;

    @FXML
    private Label ordersCount;

    private List<Order> orders = new ArrayList<>();
    private List<Order> filteredOrders = new ArrayList<>();
    private Gson gson = new Gson();
    private String userId;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        System.out.println("订单管理控制器初始化完成");
    }
    
    public void setUserId(String userId) {
        this.userId = userId;
        loadOrdersFromServer("all");
    }

    private void loadOrdersFromServer(String statusFilter) {
        new Thread(() -> {
            try {
                DataOutputStream out = SocketManager.getInstance().getOut();
                DataInputStream in = SocketManager.getInstance().getIn();

                out.writeUTF("OrderHistoryRequest");

                OrderHistoryRequest request = new OrderHistoryRequest(statusFilter);
                String jsonRequest = gson.toJson(request);
                out.writeUTF(jsonRequest);
                out.flush();

                String response = in.readUTF();
                System.out.println("[Debug] 收到订单历史响应: " + response);
                
                if (response.startsWith("SUCCESS|")) {
                    String jsonData = response.substring(8);
                    
                    if (!jsonData.trim().startsWith("{")) {
                        Platform.runLater(() -> {
                            showErrorAlert("数据格式错误", "服务器返回无效的订单数据");
                        });
                        return;
                    }
                    
                    try {
                        OrderHistoryResponse orderResponse = gson.fromJson(jsonData, OrderHistoryResponse.class);

                        if (orderResponse.getStatus() == 1) {
                            orders.clear();
                            orders.addAll(orderResponse.getOrders());
                            filteredOrders.clear();
                            filteredOrders.addAll(orders);

                            Platform.runLater(() -> {
                                displayOrders();
                                ordersCount.setText(orders.size() + "个订单");
                            });
                        } else {
                            Platform.runLater(() -> {
                                showErrorAlert("加载失败", orderResponse.getMessage());
                            });
                        }
                    } catch (JsonSyntaxException e) {
                        Platform.runLater(() -> {
                            showErrorAlert("解析错误", "无法解析订单数据: " + e.getMessage());
                        });
                        e.printStackTrace();
                    }
                } else {
                    Platform.runLater(() -> {
                        showErrorAlert("加载失败", response.substring(6));
                    });
                }
            } catch (IOException e) {
                Platform.runLater(() -> {
                    showErrorAlert("网络错误", "无法连接到服务器");
                });
            }
        }).start();
    }

    private void displayOrders() {
        ordersContainer.getChildren().clear();

        if (filteredOrders.isEmpty()) {
            VBox emptyOrders = new VBox(20);
            emptyOrders.getStyleClass().add("empty-orders");
            emptyOrders.setAlignment(Pos.CENTER);

            ImageView emptyImage = new ImageView();
            emptyImage.setFitWidth(100);
            emptyImage.setFitHeight(100);
            emptyImage.setStyle("-fx-background-color: #ecf0f1; -fx-background-radius: 50;");

            Label emptyLabel = new Label("暂无订单记录");
            emptyLabel.getStyleClass().add("empty-orders-label");

            emptyOrders.getChildren().addAll(emptyImage, emptyLabel);
            ordersContainer.getChildren().add(emptyOrders);
        } else {
            for (Order order : filteredOrders) {
                VBox orderCard = createOrderCard(order);
                ordersContainer.getChildren().add(orderCard);
            }
        }
    }

    private VBox createOrderCard(Order order) {
        VBox card = new VBox(10);
        card.getStyleClass().add("order-card");

        // 订单头部 - 订单编号、日期和状态
        HBox header = new HBox();
        header.getStyleClass().add("order-header");
        header.setAlignment(Pos.CENTER_LEFT);

        VBox orderInfo = new VBox(5);
        Label orderId = new Label("订单号: " + order.getId());
        orderId.getStyleClass().add("order-id");

        Label orderDate = new Label("下单时间: " + order.getDate());
        orderDate.getStyleClass().add("order-date");

        orderInfo.getChildren().addAll(orderId, orderDate);

        // 订单状态
        Label status = new Label(getStatusText(order.getStatus()));
        status.getStyleClass().add("order-status");
        status.getStyleClass().add("status-" + order.getStatus());

        HBox.setMargin(status, new Insets(0, 0, 0, 20));
        header.getChildren().addAll(orderInfo, status);

        // 订单商品列表
        VBox itemsContainer = new VBox(5);
        for (OrderItem item : order.getItems()) {
            HBox itemRow = createOrderItemRow(item);
            itemsContainer.getChildren().add(itemRow);
        }

        // 订单底部 - 总价和操作按钮
        HBox footer = new HBox(10);
        footer.getStyleClass().add("order-footer");
        footer.setAlignment(Pos.CENTER_RIGHT);

        Label total = new Label("合计: ¥" + String.format("%.2f", order.getTotal()));
        total.getStyleClass().add("order-total");

        // 根据订单状态显示不同的操作按钮
        Button primaryBtn = new Button();
        Button secondaryBtn = new Button("查看详情");
        secondaryBtn.getStyleClass().addAll("order-action-btn", "btn-secondary");
        secondaryBtn.setOnAction(e -> viewOrderDetails(order));

        switch (order.getStatus()) {
            case "pending":
                primaryBtn.setText("立即支付");
                primaryBtn.getStyleClass().addAll("order-action-btn", "btn-primary");
                primaryBtn.setOnAction(e -> payOrder(order));
                break;
            case "shipped":
                primaryBtn.setText("确认收货");
                primaryBtn.getStyleClass().addAll("order-action-btn", "btn-primary");
                primaryBtn.setOnAction(e -> confirmReceipt(order));
                break;
            default:
                primaryBtn.setVisible(false);
        }

        footer.getChildren().addAll(total, primaryBtn, secondaryBtn);

        // 组装订单卡片
        card.getChildren().addAll(header, itemsContainer, footer);

        return card;
    }

    private HBox createOrderItemRow(OrderItem item) {
        HBox itemRow = new HBox(10);
        itemRow.getStyleClass().add("order-item");
        itemRow.setAlignment(Pos.CENTER_LEFT);

        // 商品图片
        ImageView imageView = new ImageView();
        imageView.setFitWidth(60);
        imageView.setFitHeight(60);
        imageView.getStyleClass().add("item-image");

        try {
            Image image = new Image(getClass().getResourceAsStream("/seu/vcampus/client/images/default_product.png"));
            imageView.setImage(image);
        } catch (Exception e) {
            imageView.setStyle("-fx-background-color: #ecf0f1;");
        }

        // 商品信息
        VBox itemInfo = new VBox(5);
        Label name = new Label(item.getName());
        name.getStyleClass().add("item-name");

        HBox priceBox = new HBox(5);
        Label price = new Label("¥" + item.getPrice());
        price.getStyleClass().add("item-price");

        Label quantity = new Label("×" + item.getQuantity());
        quantity.getStyleClass().add("item-quantity");

        priceBox.getChildren().addAll(price, quantity);
        itemInfo.getChildren().addAll(name, priceBox);

        itemRow.getChildren().addAll(imageView, itemInfo);

        return itemRow;
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

    private void viewOrderDetails(Order order) {
        System.out.println("查看订单详情: " + order.getId());
        loadOrderDetailFromServer(order.getId());
    }

    private void loadOrderDetailFromServer(String orderId) {
        new Thread(() -> {
            try {
                DataOutputStream out = SocketManager.getInstance().getOut();
                DataInputStream in = SocketManager.getInstance().getIn();

                out.writeUTF("OrderDetailRequest");

                OrderDetailRequest request = new OrderDetailRequest(orderId);
                String jsonRequest = gson.toJson(request);
                out.writeUTF(jsonRequest);
                out.flush();

                String response = in.readUTF();
                if (response.startsWith("SUCCESS|")) {
                    String jsonData = response.substring(8);
                    OrderDetailResponse orderResponse = gson.fromJson(jsonData, OrderDetailResponse.class);

                    if (orderResponse.getStatus() == 1) {
                        Platform.runLater(() -> {
                            showOrderDetailDialog(orderResponse.getOrder());
                        });
                    }
                } else {
                    Platform.runLater(() -> {
                        showErrorAlert("加载失败", response.substring(6));
                    });
                }
            } catch (IOException e) {
                Platform.runLater(() -> {
                    showErrorAlert("网络错误", "无法连接到服务器");
                });
            }
        }).start();
    }

    private void showOrderDetailDialog(Order order) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("订单详情");
        alert.setHeaderText("订单号: " + order.getId());

        VBox content = new VBox(10);
        content.setPadding(new Insets(10));

        Label dateLabel = new Label("下单时间: " + order.getDate());
        Label statusLabel = new Label("订单状态: " + getStatusText(order.getStatus()));
        Label totalLabel = new Label("订单总额: ¥" + String.format("%.2f", order.getTotal()));

        VBox itemsBox = new VBox(5);
        for (OrderItem item : order.getItems()) {
            HBox itemBox = new HBox(10);
            itemBox.setAlignment(Pos.CENTER_LEFT);

            Label nameLabel = new Label(item.getName());
            Label priceLabel = new Label("¥" + item.getPrice());
            Label quantityLabel = new Label("×" + item.getQuantity());
            Label subtotalLabel = new Label("小计: ¥" + String.format("%.2f", item.getSubtotal()));

            itemBox.getChildren().addAll(nameLabel, priceLabel, quantityLabel, subtotalLabel);
            itemsBox.getChildren().add(itemBox);
        }

        content.getChildren().addAll(dateLabel, statusLabel, totalLabel, new Label("商品列表:"), itemsBox);
        alert.getDialogPane().setContent(content);

        alert.showAndWait();
    }

    private void payOrder(Order order) {
        System.out.println("支付订单: " + order.getId());

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("订单支付");
        alert.setHeaderText("订单号: " + order.getId());
        alert.setContentText("订单金额: ¥" + String.format("%.2f", order.getTotal()) + "\n请选择支付方式:");

        ComboBox<String> paymentMethod = new ComboBox<>();
        paymentMethod.getItems().addAll("余额支付", "微信支付", "支付宝支付");
        paymentMethod.setValue("余额支付");

        VBox content = new VBox(10);
        content.getChildren().addAll(new Label("请选择支付方式:"), paymentMethod);
        alert.getDialogPane().setContent(content);

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            new Thread(() -> {
                try {
                    DataOutputStream out = SocketManager.getInstance().getOut();
                    DataInputStream in = SocketManager.getInstance().getIn();

                    out.writeUTF("PaymentRequest");

                    PaymentRequest request = new PaymentRequest(
                        order.getId(),
                        order.getTotal(),
                        paymentMethod.getValue()
                    );
                    System.out.println("[前端] 传递的支付方式: " + paymentMethod.getValue());
                    String jsonRequest = gson.toJson(request);
                    out.writeUTF(jsonRequest);
                    out.flush();

                    String response = in.readUTF();
                    if (response.startsWith("SUCCESS|")) {
                        String jsonData = response.substring(8);
                        PaymentResponse paymentResponse = gson.fromJson(jsonData, PaymentResponse.class);

                        Platform.runLater(() -> {
                            if (paymentResponse.getStatus() == 1) {
                                showSuccessMessage("支付成功", "订单已支付成功，交易记录ID: " + paymentResponse.getTransactionId());
                                loadOrdersFromServer("all");
                            } else {
                                showErrorAlert("支付失败", paymentResponse.getMessage());
                            }
                        });
                    } else {
                        Platform.runLater(() -> {
                            showErrorAlert("支付失败", response.substring(6));
                        });
                    }
                } catch (IOException e) {
                    Platform.runLater(() -> {
                        showErrorAlert("网络错误", "无法连接到服务器");
                    });
                }
            }).start();
        }
    }

    private void confirmReceipt(Order order) {
        System.out.println("确认收货: " + order.getId());
        
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("确认收货");
        alert.setHeaderText("订单号: " + order.getId());
        alert.setContentText("确认已收到商品吗？此操作不可撤销。");
        
        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            new Thread(() -> {
                try {
                    DataOutputStream out = SocketManager.getInstance().getOut();
                    DataInputStream in = SocketManager.getInstance().getIn();

                    out.writeUTF("OrderStatusUpdateRequest");

                    OrderStatusUpdateRequest request = new OrderStatusUpdateRequest(order.getId(), "completed");
                    String jsonRequest = gson.toJson(request);
                    out.writeUTF(jsonRequest);
                    out.flush();

                    String response = in.readUTF();
                    if (response.startsWith("SUCCESS|")) {
                        Platform.runLater(() -> {
                            showSuccessMessage("确认收货成功", "订单状态已更新为已完成");
                            loadOrdersFromServer("all");
                        });
                    } else {
                        Platform.runLater(() -> {
                            showErrorAlert("确认收货失败", response.substring(6));
                        });
                    }
                } catch (IOException e) {
                    Platform.runLater(() -> {
                        showErrorAlert("网络错误", "无法连接到服务器");
                    });
                }
            }).start();
        }
    }

    @FXML
    private void handleFilterOrders(javafx.event.ActionEvent event) {
        Button button = (Button) event.getSource();
        String filter = button.getText();
        System.out.println("筛选订单: " + filter);

        String statusFilter = "all";

        switch (filter) {
            case "全部订单": statusFilter = "all"; break;
            case "待付款": statusFilter = "pending"; break;
            case "待发货": statusFilter = "paid"; break;
            case "已发货": statusFilter = "shipped"; break;
            case "已完成": statusFilter = "completed"; break;
        }

        loadOrdersFromServer(statusFilter);
    }

    private void showErrorAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("错误");
        alert.setHeaderText(title);
        alert.setContentText(message);
        alert.showAndWait();
    }
    
    private void showSuccessMessage(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("成功");
        alert.setHeaderText(title);
        alert.setContentText(message);
        alert.showAndWait();
    }
}