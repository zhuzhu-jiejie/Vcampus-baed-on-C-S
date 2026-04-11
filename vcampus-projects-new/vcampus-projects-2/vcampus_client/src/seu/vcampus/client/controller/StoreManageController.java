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
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;
import seu.vcampus.client.network.SocketManager;
import seu.vcampus.model.Product;
import seu.vcampus.model.ProductAddRequest;
import seu.vcampus.model.ProductDeleteRequest;
import seu.vcampus.model.ProductInfoRequest;
import seu.vcampus.model.ProductInfoResponse;
import seu.vcampus.model.ProductUpdateRequest;

public class StoreManageController implements Initializable {

    // 原有的FXML注入保持不变
    @FXML private TableView<Product> productsTable;
    @FXML private TableColumn<Product, String> colId;
    @FXML private TableColumn<Product, String> colName;
    @FXML private TableColumn<Product, Double> colPrice;
    @FXML private TableColumn<Product, String> colCategory;
    @FXML private TextField searchField;
    @FXML private ComboBox<String> categoryFilter;
    @FXML private TextField productIdField;
    @FXML private TextField productNameField;
    @FXML private TextField productPriceField;
    @FXML private TextField productOriginalPriceField;
    @FXML private ComboBox<String> productCategoryField;
    @FXML private TextArea productDescriptionField;
    @FXML private Button addButton;
    @FXML private Button updateButton;
    @FXML private Button deleteButton;

    private ObservableList<Product> productsList = FXCollections.observableArrayList();
    private Product selectedProduct = null;
    private Gson gson = new Gson();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // 初始化表格列
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colPrice.setCellValueFactory(new PropertyValueFactory<>("price"));
        colCategory.setCellValueFactory(new PropertyValueFactory<>("category"));

        // 初始化分类筛选
        categoryFilter.getItems().addAll("所有分类", "stationery", "digital", "beverage", "snacks", "personal", "daily");
        categoryFilter.setValue("所有分类");

        // 初始化商品分类下拉框
        productCategoryField.getItems().addAll("stationery", "digital", "beverage", "snacks", "personal", "daily");

        // 加载商品数据
        loadProducts();

        // 设置表格选择监听
        productsTable.getSelectionModel().selectedItemProperty().addListener(
            (observable, oldValue, newValue) -> selectProduct(newValue)
        );

        // 初始禁用编辑按钮
        updateButton.setDisable(true);
        deleteButton.setDisable(true);
    }

    private void loadProducts() {
        new Thread(() -> {
            try {
                DataOutputStream out = SocketManager.getInstance().getOut();
                DataInputStream in = SocketManager.getInstance().getIn();

                // 发送请求类型
                out.writeUTF("ProductInfoRequest");

                // 发送获取商品信息的请求
                ProductInfoRequest req = new ProductInfoRequest("", "");
                out.writeUTF(gson.toJson(req));
                out.flush();

                // 接收服务器响应
                String response = in.readUTF();
                System.out.println("收到服务器响应: " + response);

                if (response.startsWith("ERROR|")) {
                    // 处理错误信息
                    String errorMsg = response.substring(6);
                    Platform.runLater(() -> showAlert("错误", "获取商品信息失败: " + errorMsg));
                } else if (response.startsWith("SUCCESS|")) {
                    // 处理成功响应
                    String jsonStr = response.substring(8);
                    ProductInfoResponse info = gson.fromJson(jsonStr, ProductInfoResponse.class);

                    // 在UI线程中更新界面
                    Platform.runLater(() -> {
                        productsList.clear();
                        productsList.addAll(info.getProducts());
                        productsTable.setItems(productsList);
                    });
                } else {
                    Platform.runLater(() ->
                        showAlert("错误", "服务器返回了未知格式的响应: " + response)
                    );
                }

            } catch (Exception e) {
                Platform.runLater(() ->
                    showAlert("错误", "获取商品信息失败: " + e.getMessage())
                );
                e.printStackTrace();
            }
        }).start();
    }

    private void selectProduct(Product product) {
        selectedProduct = product;

        if (product != null) {
            // 填充表单
            productIdField.setText(product.getId());
            productNameField.setText(product.getName());
            productPriceField.setText(String.valueOf(product.getPrice()));
            productOriginalPriceField.setText(String.valueOf(product.getOriginalPrice()));
            productCategoryField.setValue(product.getCategory());
            productDescriptionField.setText(product.getDescription());

            // 启用编辑和删除按钮
            updateButton.setDisable(false);
            deleteButton.setDisable(false);
        } else {
            // 清空表单
            clearForm();

            // 禁用编辑和删除按钮
            updateButton.setDisable(true);
            deleteButton.setDisable(true);
        }
    }

    private void clearForm() {
        productIdField.clear();
        productNameField.clear();
        productPriceField.clear();
        productOriginalPriceField.clear();
        productCategoryField.setValue(null);
        productDescriptionField.clear();
    }

    @FXML
    private void handleAddProduct() {
        try {
            // 验证输入
            if (productIdField.getText().isEmpty() || productNameField.getText().isEmpty() ||
                productPriceField.getText().isEmpty() || productCategoryField.getValue() == null) {
                showAlert("错误", "请填写所有必填字段");
                return;
            }

            // 创建新商品
            Product newProduct = new Product(
                productIdField.getText(),
                productNameField.getText(),
                Double.parseDouble(productPriceField.getText()),
                Double.parseDouble(productOriginalPriceField.getText()),
                0.0, // 默认评分
                productCategoryField.getValue(),
                productDescriptionField.getText()
            );

            // 发送添加请求
            new Thread(() -> {
                try {
                    DataOutputStream out = SocketManager.getInstance().getOut();
                    DataInputStream in = SocketManager.getInstance().getIn();

                    // 发送请求类型
                    out.writeUTF("ProductAddRequest");

                    // 发送添加商品请求
                    ProductAddRequest req = new ProductAddRequest(newProduct);
                    out.writeUTF(gson.toJson(req));
                    out.flush();

                    // 接收服务器响应
                    String response = in.readUTF();
                    System.out.println("收到服务器响应: " + response);

                    if (response.startsWith("ERROR|")) {
                        // 处理错误信息
                        String errorMsg = response.substring(6);
                        Platform.runLater(() -> showAlert("错误", "添加商品失败: " + errorMsg));
                    } else if (response.startsWith("SUCCESS|")) {
                        // 处理成功响应
                        Platform.runLater(() -> {
                            showAlert("成功", "商品添加成功");
                            clearForm();
                            loadProducts(); // 重新加载商品列表
                        });
                    } else {
                        Platform.runLater(() ->
                            showAlert("错误", "服务器返回了未知格式的响应: " + response)
                        );
                    }

                } catch (Exception e) {
                    Platform.runLater(() ->
                        showAlert("错误", "添加商品失败: " + e.getMessage())
                    );
                    e.printStackTrace();
                }
            }).start();

        } catch (NumberFormatException e) {
            showAlert("错误", "价格格式不正确");
        }
    }

    @FXML
    private void handleUpdateProduct() {
        if (selectedProduct == null) {
            showAlert("错误", "请先选择一个商品");
            return;
        }

        try {
            // 更新商品信息
            selectedProduct.setId(productIdField.getText());
            selectedProduct.setName(productNameField.getText());
            selectedProduct.setPrice(Double.parseDouble(productPriceField.getText()));
            selectedProduct.setOriginalPrice(Double.parseDouble(productOriginalPriceField.getText()));
            selectedProduct.setCategory(productCategoryField.getValue());
            selectedProduct.setDescription(productDescriptionField.getText());

            // 发送更新请求
            new Thread(() -> {
                try {
                    DataOutputStream out = SocketManager.getInstance().getOut();
                    DataInputStream in = SocketManager.getInstance().getIn();

                    // 发送请求类型
                    out.writeUTF("ProductUpdateRequest");

                    // 发送更新商品请求
                    ProductUpdateRequest req = new ProductUpdateRequest(selectedProduct);
                    out.writeUTF(gson.toJson(req));
                    out.flush();

                    // 接收服务器响应
                    String response = in.readUTF();
                    System.out.println("收到服务器响应: " + response);

                    if (response.startsWith("ERROR|")) {
                        // 处理错误信息
                        String errorMsg = response.substring(6);
                        Platform.runLater(() -> showAlert("错误", "更新商品失败: " + errorMsg));
                    } else if (response.startsWith("SUCCESS|")) {
                        // 处理成功响应
                        Platform.runLater(() -> {
                            showAlert("成功", "商品更新成功");
                            productsTable.refresh();
                        });
                    } else {
                        Platform.runLater(() ->
                            showAlert("错误", "服务器返回了未知格式的响应: " + response)
                        );
                    }

                } catch (Exception e) {
                    Platform.runLater(() ->
                        showAlert("错误", "更新商品失败: " + e.getMessage())
                    );
                    e.printStackTrace();
                }
            }).start();

        } catch (NumberFormatException e) {
            showAlert("错误", "价格格式不正确");
        }
    }

    @FXML
    private void handleDeleteProduct() {
        if (selectedProduct == null) {
            showAlert("错误", "请先选择一个商品");
            return;
        }

        // 确认删除
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("确认删除");
        alert.setHeaderText("删除商品");
        alert.setContentText("确定要删除商品 '" + selectedProduct.getName() + "' 吗？");

        if (alert.showAndWait().get() == ButtonType.OK) {
            // 发送删除请求
            new Thread(() -> {
                try {
                    DataOutputStream out = SocketManager.getInstance().getOut();
                    DataInputStream in = SocketManager.getInstance().getIn();

                    // 发送请求类型
                    out.writeUTF("ProductDeleteRequest");

                    // 发送删除商品请求
                    ProductDeleteRequest req = new ProductDeleteRequest(selectedProduct.getId());
                    out.writeUTF(gson.toJson(req));
                    out.flush();

                    // 接收服务器响应
                    String response = in.readUTF();
                    System.out.println("收到服务器响应: " + response);

                    if (response.startsWith("ERROR|")) {
                        // 处理错误信息
                        String errorMsg = response.substring(6);
                        Platform.runLater(() -> showAlert("错误", "删除商品失败: " + errorMsg));
                    } else if (response.startsWith("SUCCESS|")) {
                        // 处理成功响应
                        Platform.runLater(() -> {
                            showAlert("成功", "商品删除成功");
                            clearForm();
                            loadProducts(); // 重新加载商品列表
                        });
                    } else {
                        Platform.runLater(() ->
                            showAlert("错误", "服务器返回了未知格式的响应: " + response)
                        );
                    }

                } catch (Exception e) {
                    Platform.runLater(() ->
                        showAlert("错误", "删除商品失败: " + e.getMessage())
                    );
                    e.printStackTrace();
                }
            }).start();
        }
    }

    @FXML
    private void handleSearch() {
        String keyword = searchField.getText().toLowerCase();
        String category = categoryFilter.getValue();
        String searchCategory = "所有分类".equals(category) ? "" : category;

        new Thread(() -> {
            try {
                DataOutputStream out = SocketManager.getInstance().getOut();
                DataInputStream in = SocketManager.getInstance().getIn();

                // 发送请求类型
                out.writeUTF("ProductInfoRequest");

                // 发送搜索商品请求
                ProductInfoRequest req = new ProductInfoRequest(searchCategory, keyword);
                out.writeUTF(gson.toJson(req));
                out.flush();

                // 接收服务器响应
                String response = in.readUTF();
                System.out.println("收到服务器响应: " + response);

                if (response.startsWith("ERROR|")) {
                    // 处理错误信息
                    String errorMsg = response.substring(6);
                    Platform.runLater(() -> showAlert("错误", "搜索商品失败: " + errorMsg));
                } else if (response.startsWith("SUCCESS|")) {
                    // 处理成功响应
                    String jsonStr = response.substring(8);
                    ProductInfoResponse info = gson.fromJson(jsonStr, ProductInfoResponse.class);

                    // 在UI线程中更新界面
                    Platform.runLater(() -> {
                        ObservableList<Product> filteredList = FXCollections.observableArrayList(info.getProducts());
                        productsTable.setItems(filteredList);
                    });
                } else {
                    Platform.runLater(() ->
                        showAlert("错误", "服务器返回了未知格式的响应: " + response)
                    );
                }

            } catch (Exception e) {
                Platform.runLater(() ->
                    showAlert("错误", "搜索商品失败: " + e.getMessage())
                );
                e.printStackTrace();
            }
        }).start();
    }

    @FXML
    private void handleClearFilter() {
        searchField.clear();
        categoryFilter.setValue("所有分类");
        loadProducts();
    }

    // 其他导航方法保持不变...
    @FXML
    private void handleNavigateToOrders() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("AdminOrderManager.fxml"));
            Stage stage = (Stage) productsTable.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("订单管理 - 商店管理系统");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleNavigateToUsers() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("AdminUserManager.fxml"));
            Stage stage = (Stage) productsTable.getScene().getWindow();
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
            Stage stage = (Stage) productsTable.getScene().getWindow();
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
            Stage stage = (Stage) productsTable.getScene().getWindow();
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