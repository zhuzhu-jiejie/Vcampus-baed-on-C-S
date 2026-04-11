package seu.vcampus.client.controller;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

import com.google.gson.Gson;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import seu.vcampus.client.network.SocketManager;
import seu.vcampus.model.CartRequest;
import seu.vcampus.model.Product;
import seu.vcampus.model.ProductInfoRequest;
import seu.vcampus.model.ProductInfoResponse;

public class ProductBrowseController implements Initializable {

    @FXML
    private VBox categoryNav;

    @FXML
    private TextField searchField;

    @FXML
    private Label categoryTitle;

    @FXML
    private FlowPane productsPane;

    @FXML
    private Button stationeryBtn, digitalBtn, beverageBtn, snacksBtn, personalBtn, dailyBtn;

    private List<Product> allProducts = new ArrayList<>();
    private List<Product> filteredProducts = new ArrayList<>();
    private Gson gson = new Gson();
    private String userid;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // 初始化时加载所有商品数据（在新线程中执行网络操作）
        loadProductsFromServer("all", "");
        System.out.println("商店浏览控制器初始化完成");
    }

    private void loadProductsFromServer(String category, String keyword) {
        // 网络操作放在新线程，避免阻塞UI
        new Thread(() -> {
            try {
                // 获取Socket流（与教师控制器保持一致）
                DataOutputStream out = SocketManager.getInstance().getOut();
                DataInputStream in = SocketManager.getInstance().getIn();

                // 发送请求类型
                out.writeUTF("ProductInfoRequest");

                // 构建并发送请求对象
                ProductInfoRequest request = new ProductInfoRequest(category, keyword);
                String jsonRequest = gson.toJson(request);
                out.writeUTF(jsonRequest);
                out.flush();

                // 接收服务器响应
                String response = in.readUTF();
                System.out.println("商品服务器响应: " + response);

                // 处理响应（UI操作需在主线程）
                if (response.startsWith("SUCCESS|")) {
                    String jsonData = response.substring(8);
                    ProductInfoResponse productResponse = gson.fromJson(jsonData, ProductInfoResponse.class);

                    if (productResponse.getStatus() == 1) {
                        allProducts = productResponse.getProducts();
                        filteredProducts = new ArrayList<>(allProducts);
                        // 主线程更新商品展示
                        Platform.runLater(() -> {
                            displayProducts(filteredProducts);
                            categoryTitle.setText(getCategoryName(category)); // 更新标题
                        });
                        System.out.println("成功加载" + allProducts.size() + "件商品");
                    } else {
                        Platform.runLater(() ->
                            showErrorAlert("加载失败", productResponse.getMessage())
                        );
                    }
                } else if (response.startsWith("ERROR|")) {
                    String errorMsg = response.substring(6);
                    Platform.runLater(() ->
                        showErrorAlert("获取失败", errorMsg)
                    );
                } else {
                    Platform.runLater(() ->
                        showErrorAlert("响应格式错误", "服务器返回未知格式: " + response)
                    );
                }

            } catch (Exception e) {
                Platform.runLater(() -> {
                    showErrorAlert("连接错误", "获取商品失败：" + e.getMessage());
                    // 网络错误时加载本地数据
                    initializeLocalProducts();
                    displayProducts(allProducts);
                });
                e.printStackTrace();
            }
        }).start();
    }

    // 根据分类ID获取显示名称
    private String getCategoryName(String categoryId) {
        switch (categoryId) {
            case "stationery": return "文具";
            case "digital": return "数码产品";
            case "beverage": return "饮品";
            case "snacks": return "零食";
            case "personal": return "个人护理";
            case "daily": return "日用品";
            default: return "所有商品";
        }
    }

    private void initializeLocalProducts() {
        // 本地示例数据（仅作后备使用）
        allProducts.clear();
        allProducts.add(new Product("P001", "M&G晨光中性笔0.5mm", "晨光", 2.50, 3.00, 4.5,
                                   "stationery", "晨光经典中性笔，书写流畅，不漏墨", 500,
                                   null, "/images/stationery/pen1.jpg"));
        allProducts.add(new Product("P002", "PILOT百乐果汁笔", "百乐", 6.80, 8.00, 4.8,
                                   "stationery", "日本百乐果汁笔，颜色鲜艳，书写顺滑", 300,
                                   null, "/images/stationery/pen2.jpg"));
        // 可添加更多示例商品
        filteredProducts = new ArrayList<>(allProducts);
    }

    @FXML
    private void handleCategorySelect(javafx.event.ActionEvent event) {
        Button clickedButton = (Button) event.getSource();
        String categoryId = "";

        // 确定分类ID
        if (clickedButton == stationeryBtn) {
            categoryId = "stationery";
        } else if (clickedButton == digitalBtn) {
            categoryId = "digital";
        } else if (clickedButton == beverageBtn) {
            categoryId = "beverage";
        } else if (clickedButton == snacksBtn) {
            categoryId = "snacks";
        } else if (clickedButton == personalBtn) {
            categoryId = "personal";
        } else if (clickedButton == dailyBtn) {
            categoryId = "daily";
        }

        // 加载对应分类商品
        loadProductsFromServer(categoryId, "");
    }

    @FXML
    private void handleSearch() {
        String keyword = searchField.getText().trim();
        if (keyword.isEmpty()) {
            showAllProducts();
            return;
        }
        // 搜索商品
        loadProductsFromServer("all", keyword);
    }

    @FXML
    private void handleFilter() {
        handleSearch(); // 复用搜索逻辑
    }

    private void showAllProducts() {
        loadProductsFromServer("all", "");
    }

    private void displayProducts(List<Product> products) {
        productsPane.getChildren().clear();

        for (Product product : products) {
            VBox productCard = createProductCard(product);
            productsPane.getChildren().add(productCard);
        }
    }

    private VBox createProductCard(Product product) {
        // 创建卡片容器
        VBox card = new VBox();
        card.getStyleClass().add("product-card");
        card.setPrefWidth(220);
        card.setPrefHeight(320);
        card.setSpacing(10);
        card.setPadding(new Insets(15));
        card.setStyle("-fx-background-color: white; -fx-border-radius: 8; -fx-background-radius: 8; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 10, 0, 0, 2);");

        // 商品图片展示区域
        ImageView imageView = new ImageView();
        imageView.setFitWidth(180);  // 图片宽度
        imageView.setFitHeight(150); // 图片高度
        imageView.setPreserveRatio(true); // 保持图片比例
        imageView.setStyle("-fx-background-color: #f5f5f5; -fx-border-radius: 4; -fx-background-radius: 4;");

        // 加载商品图片（核心修改部分）
        try {
            // 从商品对象获取Nginx提供的HTTP图片URL
            String imageUrl = product.getImageUrl();

            // 检查URL是否有效
            if (imageUrl == null || imageUrl.trim().isEmpty()) {
                // 无URL时显示默认图
                imageView.setImage(new Image(getClass().getResourceAsStream("/seu/vcampus/client/images/default_product.png")));
            } else {
                // 异步加载图片（第二个参数true表示异步，避免阻塞UI）
                Image image = new Image(imageUrl, true);

                // 监听图片加载状态：失败时显示默认图
                image.errorProperty().addListener((observable, oldValue, newValue) -> {
                    if (newValue) { // 加载失败
                        System.err.println("图片加载失败: " + imageUrl);
                        imageView.setImage(new Image(getClass().getResourceAsStream("/seu/vcampus/client/images/default_product.png")));
                    }
                });

                // 监听图片加载完成（可选：可添加淡入效果等）
                image.progressProperty().addListener((observable, oldValue, newValue) -> {
                    if (newValue.doubleValue() == 1.0) {
                        // 图片加载完成
                        imageView.setImage(image);
                    }
                });
            }
        } catch (Exception e) {
            // 任何异常都显示默认图
            System.err.println("图片加载异常: " + e.getMessage());
            imageView.setImage(new Image(getClass().getResourceAsStream("/default_product.png")));
        }

        // 商品名称
        Label nameLabel = new Label(product.getName());
        nameLabel.getStyleClass().add("product-name");
        nameLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: #333;");
        nameLabel.setWrapText(true); // 自动换行

        // 商品价格
        Label priceLabel = new Label("¥" + String.format("%.2f", product.getPrice()));
        priceLabel.getStyleClass().add("product-price");
        priceLabel.setStyle("-fx-font-size: 16px; -fx-text-fill: #e74c3c; -fx-font-weight: bold;");

        // 原价（如果有折扣）
        Label originalPriceLabel = new Label();
        if (product.getOriginalPrice() > product.getPrice()) {
            originalPriceLabel.setText("¥" + String.format("%.2f", product.getOriginalPrice()));
            originalPriceLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #999; -fx-strikethrough: true;");
        } else {
            originalPriceLabel.setVisible(false); // 无折扣时隐藏
        }

        // 评分
        HBox ratingBox = new HBox(2);
        Label ratingLabel = new Label(String.format("%.1f", product.getRating()));
        ratingLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #666;");
        Image starImage = new Image(getClass().getResourceAsStream("/seu/vcampus/client/images/star.png")); // 星星图标（需提前准备）
        ImageView starView = new ImageView(starImage);
        starView.setFitWidth(12);
        starView.setFitHeight(12);
        ratingBox.getChildren().addAll(starView, ratingLabel);

        // 组装信息区域
        VBox infoBox = new VBox(5);
        infoBox.getChildren().addAll(nameLabel, priceLabel, originalPriceLabel, ratingBox);

        // 加入购物车按钮
        Button addToCartBtn = new Button("加入购物车");
        addToCartBtn.setStyle("-fx-background-color: #3498db; -fx-text-fill: white; -fx-font-size: 12px; -fx-padding: 5 10; -fx-border-radius: 4; -fx-background-radius: 4;");
        addToCartBtn.setOnAction(e -> {
            // 加入购物车逻辑（根据你的需求实现）
            System.out.println("加入购物车: " + product.getName());
            addToCart(product);

        });

        // 组装完整卡片
        card.getChildren().addAll(imageView, infoBox, addToCartBtn);

        return card;
    }

    private void addToCart(Product product) {
        if (product.getStock() <= 0) {
            showErrorAlert("库存不足", "该商品已售罄");
            return;
        }

        // 发送添加到购物车请求
        new Thread(() -> {
            try {
                DataOutputStream out = SocketManager.getInstance().getOut();
                DataInputStream in = SocketManager.getInstance().getIn();

                out.writeUTF("CartRequest");

                CartRequest request = new CartRequest("add", product.getId(), 1);
                String jsonRequest = gson.toJson(request);
                out.writeUTF(jsonRequest);
                out.flush();

                String response = in.readUTF();
                if (response.startsWith("SUCCESS")) {
                    Platform.runLater(() -> {
                        showSuccessMessage("添加成功", "已将 '" + product.getName() + "' 加入购物车");
                    });
                } else {
                    Platform.runLater(() -> {
                        showErrorAlert("添加失败", response.substring(6));
                    });
                }
            } catch (IOException e) {
                Platform.runLater(() -> {
                    showErrorAlert("网络错误", "无法连接到服务器");
                });
            }
        }).start();
    }

    // 成功提示对话框
    private void showSuccessMessage(String title, String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("成功");
            alert.setHeaderText(title);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }

    // 错误提示对话框（与教师控制器保持一致）
    private void showErrorAlert(String title, String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("错误");
            alert.setHeaderText(title);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }

	public void setUserId(String userid) {
		this.userid=userid;
	}
}