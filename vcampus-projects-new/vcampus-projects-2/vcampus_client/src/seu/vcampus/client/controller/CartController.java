package seu.vcampus.client.controller;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
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
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import seu.vcampus.client.network.SocketManager;
import seu.vcampus.model.CartItem;
import seu.vcampus.model.CartRequest;
import seu.vcampus.model.CartResponse;
import seu.vcampus.model.CheckoutRequest;
import seu.vcampus.model.CheckoutResponse;
import seu.vcampus.model.Product;



public class CartController implements Initializable {

    @FXML
    private VBox cartItemsContainer;

    @FXML
    private Label cartItemsCount;

    @FXML
    private Label totalPriceLabel;

    @FXML
    private Button checkoutBtn;

    private List<CartItem> cartItems = new ArrayList<>();
    private double totalPrice = 0.0;
    private Gson gson = new Gson();
    private String userId;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // 从全局购物车获取数据
        System.out.println("购物车控制器初始化完成");
    }


	public void setUserId(String userId) {
		this.userId = userId;
		loadCartItemsFromServer();
        updateCartDisplay();
	}

    private void loadCartItemsFromServer() {
        new Thread(() -> {
            try {
                DataOutputStream out = SocketManager.getInstance().getOut();
                DataInputStream in = SocketManager.getInstance().getIn();

                out.writeUTF("CartRequest");

                CartRequest request = new CartRequest("get", null, 0);
                String jsonRequest = gson.toJson(request);
                out.writeUTF(jsonRequest);
                out.flush();

                String response = in.readUTF();
                if (response.startsWith("SUCCESS|")) {
                    String jsonData = response.substring(8);
                    CartResponse cartResponse = gson.fromJson(jsonData, CartResponse.class);

                    if (cartResponse.getStatus() == 1) {
                        cartItems.clear();
                        cartItems.addAll(cartResponse.getItems());

                        // 计算总价
                        totalPrice = 0.0;
                        for (CartItem item : cartItems) {
                            totalPrice += item.getSubtotal();
                        }

                        Platform.runLater(() -> {
                            updateCartDisplay();
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

    private void updateCartDisplay() {
        cartItemsContainer.getChildren().clear();

        if (cartItems.isEmpty()) {
            // 显示空购物车提示
            VBox emptyCart = new VBox(20);
            emptyCart.getStyleClass().add("empty-cart");
            emptyCart.setAlignment(Pos.CENTER);

            ImageView emptyImage = new ImageView();
            emptyImage.setFitWidth(100);
            emptyImage.setFitHeight(100);
            emptyImage.setStyle("-fx-background-color: #ecf0f1; -fx-background-radius: 50;");

            Label emptyLabel = new Label("购物车空空如也");
            emptyLabel.getStyleClass().add("empty-cart-label");

            emptyCart.getChildren().addAll(emptyImage, emptyLabel);
            cartItemsContainer.getChildren().add(emptyCart);

            // 更新商品数量和总价
            cartItemsCount.setText("0件商品");
            totalPriceLabel.setText("¥0.00");
            checkoutBtn.setDisable(true);
        } else {
            // 显示购物车商品
            for (CartItem item : cartItems) {
                HBox cartItem = createCartItem(item);
                cartItemsContainer.getChildren().add(cartItem);
            }

            // 更新商品数量和总价
            cartItemsCount.setText(cartItems.size() + "件商品");
            totalPriceLabel.setText("¥" + String.format("%.2f", totalPrice));
            checkoutBtn.setDisable(false);
        }
    }

    private HBox createCartItem(CartItem item) {
        HBox itemBox = new HBox(15);
        itemBox.getStyleClass().add("cart-item");
        itemBox.setAlignment(Pos.CENTER_LEFT);
        itemBox.setPadding(new Insets(15));

        // 商品图片
        ImageView imageView = new ImageView();
        imageView.setFitWidth(80);
        imageView.setFitHeight(80);
        imageView.getStyleClass().add("cart-item-image");
        imageView.setStyle("-fx-background-color: #ecf0f1;");

        // 商品信息
        VBox infoBox = new VBox(5);
        infoBox.setAlignment(Pos.CENTER_LEFT);

        Product product = item.getProduct(); // 这里使用 getProduct() 方法
        Label nameLabel = new Label(product.getName());
        nameLabel.getStyleClass().add("cart-item-name");

        Label priceLabel = new Label("¥" + product.getPrice());
        priceLabel.getStyleClass().add("cart-item-price");

        infoBox.getChildren().addAll(nameLabel, priceLabel);

        // 数量控制
        HBox quantityBox = new HBox(5);
        quantityBox.getStyleClass().add("quantity-control");
        quantityBox.setAlignment(Pos.CENTER);
        quantityBox.setPadding(new Insets(5, 10, 5, 10));

        Button decreaseBtn = new Button("-");
        decreaseBtn.getStyleClass().add("quantity-btn");
        decreaseBtn.setOnAction(e -> decreaseQuantity(item));

        Label quantityLabel = new Label(String.valueOf(item.getQuantity()));
        quantityLabel.getStyleClass().add("quantity-label");

        Button increaseBtn = new Button("+");
        increaseBtn.getStyleClass().add("quantity-btn");
        increaseBtn.setOnAction(e -> increaseQuantity(item));

        quantityBox.getChildren().addAll(decreaseBtn, quantityLabel, increaseBtn);

        // 小计
        Label subtotalLabel = new Label("¥" + String.format("%.2f", product.getPrice() * item.getQuantity()));
        subtotalLabel.setStyle("-fx-text-fill: #2c3e50; -fx-font-weight: bold; -fx-font-size: 16;");

        // 移除按钮
        Button removeBtn = new Button("移除");
        removeBtn.getStyleClass().add("remove-btn");
        removeBtn.setOnAction(e -> removeItem(item));

        // 添加到商品项
        itemBox.getChildren().addAll(imageView, infoBox, quantityBox, subtotalLabel, removeBtn);

        return itemBox;
    }

    private void decreaseQuantity(CartItem item) {
        if (item.getQuantity() > 1) {
            updateCartItemOnServer(item.getProduct().getId(), item.getQuantity() - 1);
        }
    }

    private void increaseQuantity(CartItem item) {
        updateCartItemOnServer(item.getProduct().getId(), item.getQuantity() + 1);
    }

    private void removeItem(CartItem item) {
        removeCartItemFromServer(item.getProduct().getId());
    }


 // 添加服务器更新方法
    private void updateCartItemOnServer(String productId, int quantity) {
        new Thread(() -> {
            try {
                DataOutputStream out = SocketManager.getInstance().getOut();
                DataInputStream in = SocketManager.getInstance().getIn();

                out.writeUTF("CartRequest");

                CartRequest request = new CartRequest("update", productId, quantity);
                String jsonRequest = gson.toJson(request);
                out.writeUTF(jsonRequest);
                out.flush();

                String response = in.readUTF();
                if (response.startsWith("SUCCESS")) {
                    loadCartItemsFromServer(); // 重新加载购物车数据
                } else {
                    Platform.runLater(() -> {
                        showErrorAlert("更新失败", response.substring(6));
                    });
                }
            } catch (IOException e) {
                Platform.runLater(() -> {
                    showErrorAlert("网络错误", "无法连接到服务器");
                });
            }
        }).start();
    }

    // 添加服务器移除方法
    private void removeCartItemFromServer(String productId) {
        new Thread(() -> {
            try {
                DataOutputStream out = SocketManager.getInstance().getOut();
                DataInputStream in = SocketManager.getInstance().getIn();

                out.writeUTF("CartRequest");

                CartRequest request = new CartRequest("remove", productId, 0);
                String jsonRequest = gson.toJson(request);
                out.writeUTF(jsonRequest);
                out.flush();

                String response = in.readUTF();
                if (response.startsWith("SUCCESS")) {
                    loadCartItemsFromServer(); // 重新加载购物车数据
                } else {
                    Platform.runLater(() -> {
                        showErrorAlert("移除失败", response.substring(6));
                    });
                }
            } catch (IOException e) {
                Platform.runLater(() -> {
                    showErrorAlert("网络错误", "无法连接到服务器");
                });
            }
        }).start();
    }


    @FXML
    private void handleCheckout() {
        new Thread(() -> {
            try {
                // 1. 获取Socket流
                DataOutputStream out = SocketManager.getInstance().getOut();
                DataInputStream in = SocketManager.getInstance().getIn();
                if (out == null || in == null) {
                    Platform.runLater(() -> showErrorAlert("连接错误", "未连接到服务器"));
                    return;
                }

                // 2. 构建并发送结算请求
                CheckoutRequest request = new CheckoutRequest(totalPrice, cartItems);
                String jsonRequest = gson.toJson(request);
                out.writeUTF("CheckoutRequest"); // 发送请求类型
                out.writeUTF(jsonRequest);       // 发送请求数据
                out.flush();
                System.out.println("【客户端】发送结算请求: " + jsonRequest);

                // 3. 读取服务器响应
                String response = in.readUTF();
                System.out.println("【客户端】收到结算响应: " + response); // 关键调试信息

                // 4. 解析响应
                if (response.startsWith("SUCCESS|")) {
                    String jsonData = response.substring(8); // 截取JSON部分
                    try {
                        // 解析为CheckoutResponse对象
                        CheckoutResponse checkoutResp = gson.fromJson(jsonData, CheckoutResponse.class);
                        
                        Platform.runLater(() -> {
                            if (checkoutResp != null && checkoutResp.getStatus() == 1) {
                                // 结算成功：显示订单号并清空购物车
                                showSuccessMessage(
                                    "结算成功", 
                                    "订单已创建！\n订单号: " + checkoutResp.getOrderId()
                                );
                                cartItems.clear();
                                totalPrice = 0.0;
                                updateCartDisplay(); // 刷新购物车界面
                            } else {
                                showErrorAlert(
                                    "结算失败", 
                                    checkoutResp != null ? checkoutResp.getMessage() : "未知错误"
                                );
                            }
                        });

                    } catch (JsonSyntaxException e) {
                        // JSON解析失败（格式错误）
                        Platform.runLater(() -> {
                            showErrorAlert(
                                "解析失败", 
                                "服务器响应格式错误:\n" + e.getMessage() + "\n原始响应: " + response
                            );
                        });
                    }

                } else if (response.startsWith("ERROR|")) {
                    // 服务器返回明确错误
                    String errorMsg = response.substring(6);
                    Platform.runLater(() -> showErrorAlert("结算失败", errorMsg));

                } else {
                    // 响应格式完全不符合预期
                    Platform.runLater(() -> {
                        showErrorAlert("响应异常", "服务器返回无效格式: " + response);
                    });
                }

            } catch (Exception e) {
                // 捕获所有异常（如网络中断）
                Platform.runLater(() -> {
                    showErrorAlert("网络错误", "结算请求失败: " + e.getMessage());
                });
                e.printStackTrace();
            }
        }).start();
    }

    private void showErrorAlert(String title, String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("错误");
            alert.setHeaderText(title);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }

    private void showSuccessMessage(String title, String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("成功");
            alert.setHeaderText(title);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }



}