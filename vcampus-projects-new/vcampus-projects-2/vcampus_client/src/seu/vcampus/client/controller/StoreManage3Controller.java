package seu.vcampus.client.controller;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.URL;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.TreeMap;
import java.util.stream.Collectors;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Alert;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.FileChooser;
import seu.vcampus.client.network.SocketManager;
import seu.vcampus.model.Order;
import seu.vcampus.model.OrderHistoryRequest;
import seu.vcampus.model.OrderHistoryResponse;
import seu.vcampus.model.OrderItem;
import seu.vcampus.model.Product;
import seu.vcampus.model.ProductInfoRequest;
import seu.vcampus.model.ProductInfoResponse;
import seu.vcampus.model.Transaction;
import seu.vcampus.model.TransactionRequest;
import seu.vcampus.model.TransactionResponse;

public class StoreManage3Controller implements Initializable {

    // 原有的FXML注入保持不变
    @FXML private ComboBox<String> timeRangeCombo;
    @FXML private DatePicker startDatePicker;
    @FXML private DatePicker endDatePicker;
    @FXML private Label totalSalesLabel;
    @FXML private Label totalOrdersLabel;
    @FXML private Label avgOrderValueLabel;
    @FXML private Label topProductLabel;
    @FXML private BarChart<String, Number> salesChart;
    @FXML private TableView<ProductSalesData> productRankingTable;
    @FXML private PieChart categoryPieChart;
    @FXML private PieChart paymentPieChart;

    private List<Order> allOrders = new ArrayList<>();
    private List<Transaction> allTransactions = new ArrayList<>();
    private Gson gson = new Gson();
    private String userid;

 // 在StoreManage3Controller中定义回调接口
    @FunctionalInterface
    private interface RequestCallback {
        void onComplete();
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // 初始化时间范围选择框
        timeRangeCombo.getItems().addAll("今天", "本周", "本月", "本季度", "本年", "自定义");
        timeRangeCombo.getSelectionModel().select("本月");

        // 设置默认日期范围
        startDatePicker.setValue(LocalDate.now().withDayOfMonth(1));
        endDatePicker.setValue(LocalDate.now());



        // 初始化商品销售排行表格
        initializeProductTable();

        loadProductCategories(() -> {
            loadData(() -> {
                loadTransactions(() -> {
                    // 所有数据加载完成后生成报表
                    Platform.runLater(() -> generateReport());
                });
            });
        });
        // 加载商品分类信息
        //loadProductCategories();

        // 加载订单数据
       // loadData();

        System.out.println("财务报表控制器初始化完成");
    }

    private void initializeProductTable() {
        // 初始化商品销售排行表格列
        TableColumn<ProductSalesData, Integer> rankColumn = new TableColumn<>("排名");
        rankColumn.setCellValueFactory(new PropertyValueFactory<>("rank"));
        rankColumn.setPrefWidth(60);

        TableColumn<ProductSalesData, String> nameColumn = new TableColumn<>("商品名称");
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        nameColumn.setPrefWidth(200);

        TableColumn<ProductSalesData, Integer> quantityColumn = new TableColumn<>("销售量");
        quantityColumn.setCellValueFactory(new PropertyValueFactory<>("quantity"));
        quantityColumn.setPrefWidth(80);

        TableColumn<ProductSalesData, Double> salesColumn = new TableColumn<>("销售额");
        salesColumn.setCellValueFactory(new PropertyValueFactory<>("sales"));
        salesColumn.setPrefWidth(100);
        salesColumn.setCellFactory(column -> new TableCell<ProductSalesData, Double>() {
            @Override
            protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(String.format("¥%.2f", item));
                }
            }
        });

        TableColumn<ProductSalesData, String> percentageColumn = new TableColumn<>("占比");
        percentageColumn.setCellValueFactory(new PropertyValueFactory<>("percentage"));
        percentageColumn.setPrefWidth(80);

        productRankingTable.getColumns().setAll(rankColumn, nameColumn, quantityColumn, salesColumn, percentageColumn);
    }

    /*private void loadData() {
        // 加载订单数据
        new Thread(() -> {
            try {
                DataOutputStream out = SocketManager.getInstance().getOut();
                DataInputStream in = SocketManager.getInstance().getIn();

                // 发送请求类型
                out.writeUTF("OrderHistoryRequest");

                // 发送获取订单历史的请求
                OrderHistoryRequest req = new OrderHistoryRequest("completed");
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

                    // 更新订单数据
                    allOrders = info.getOrders();

                    // 加载交易数据
                    loadTransactions();
                    Platform.runLater(() -> {
                        if (!productCategoryMap.isEmpty()) {
                            generateReport();
                        }
                        // 如果商品分类信息尚未加载完成，报表将在商品分类加载完成后生成
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
    }*/

   /* private void loadTransactions() {
        // 加载交易数据
        new Thread(() -> {
            try {
                DataOutputStream out = SocketManager.getInstance().getOut();
                DataInputStream in = SocketManager.getInstance().getIn();

                // 发送请求类型
                out.writeUTF("TransactionHistoryRequest");

                // 发送获取交易记录的请求
                TransactionRequest req = new TransactionRequest();
                req.setUserId(userid);
                out.writeUTF(gson.toJson(req));
                out.flush();

                // 接收服务器响应
                String response = in.readUTF();
                System.out.println("收到服务器响应: " + response);

                if (response.startsWith("ERROR|")) {
                    // 处理错误信息
                    String errorMsg = response.substring(6);
                    System.out.println("获取交易记录失败: " + errorMsg);
                } else if (response.startsWith("SUCCESS|")) {
                    // 处理成功响应
                    String jsonStr = response.substring(8);
                    TransactionResponse info = gson.fromJson(jsonStr, TransactionResponse.class);

                    // 更新交易数据
                    allTransactions = info.getTransactions();
                } else {
                    System.out.println("服务器返回了未知格式的响应: " + response);
                }

                // 生成报表
                Platform.runLater(() -> generateReport());

            } catch (Exception e) {
                System.out.println("获取交易记录失败: " + e.getMessage());
                e.printStackTrace();

                // 即使交易记录获取失败，也生成报表
                Platform.runLater(() -> generateReport());
            }
        }).start();
    }*/

    private void loadData(RequestCallback callback) {
        new Thread(() -> {
            try {
                DataOutputStream out = SocketManager.getInstance().getOut();
                DataInputStream in = SocketManager.getInstance().getIn();

                // 1. 发送请求类型
                out.writeUTF("OrderHistoryRequest");
                out.flush();

                // 2. 发送请求参数
                OrderHistoryRequest req = new OrderHistoryRequest("completed");
                out.writeUTF(gson.toJson(req));
                out.flush();

                // 3. 接收响应
                String response = in.readUTF();
                System.out.println("收到订单响应: " + response);

                if (response.startsWith("ERROR|")) {
                    String errorMsg = response.substring(6);
                    Platform.runLater(() -> showAlert("错误", "获取订单信息失败: " + errorMsg));
                } else if (response.startsWith("SUCCESS|")) {
                    String jsonStr = response.substring(8);
                    OrderHistoryResponse info = gson.fromJson(jsonStr, OrderHistoryResponse.class);
                    allOrders = info.getOrders();
                    System.out.println("订单数据加载完成，共" + allOrders.size() + "条");
                } else {
                    Platform.runLater(() -> showAlert("错误", "订单响应格式错误: " + response));
                }

            } catch (Exception e) {
                Platform.runLater(() -> showAlert("错误", "获取订单信息失败: " + e.getMessage()));
                e.printStackTrace();
            } finally {
                // 执行回调，触发下一个请求
                if (callback != null) {
                    callback.onComplete();
                }
            }
        }).start();
    }

    private void loadTransactions(RequestCallback callback) {
        new Thread(() -> {
            try {
                DataOutputStream out = SocketManager.getInstance().getOut();
                DataInputStream in = SocketManager.getInstance().getIn();

                // 1. 发送请求类型
                out.writeUTF("TransactionHistoryRequest");
                out.flush();

                // 2. 发送请求参数
                TransactionRequest req = new TransactionRequest();
                req.setUserId(userid);
                out.writeUTF(gson.toJson(req));
                out.flush();

                // 3. 接收响应
                String response = in.readUTF();
                System.out.println("收到交易记录响应: " + response);

                if (response.startsWith("ERROR|")) {
                    String errorMsg = response.substring(6);
                    System.out.println("获取交易记录失败: " + errorMsg);
                } else if (response.startsWith("SUCCESS|")) {
                    String jsonStr = response.substring(8);
                    TransactionResponse info = gson.fromJson(jsonStr, TransactionResponse.class);
                    allTransactions = info.getTransactions();
                    System.out.println("交易记录加载完成，共" + allTransactions.size() + "条");
                } else {
                    System.out.println("交易记录响应格式错误: " + response);
                }

            } catch (Exception e) {
                System.out.println("获取交易记录失败: " + e.getMessage());
                e.printStackTrace();
            } finally {
                // 执行回调
                if (callback != null) {
                    callback.onComplete();
                }
            }
        }).start();
    }


    @FXML
    private void handleGenerateReport() {
        generateReport();
    }

    private Map<String, String> productCategoryMap = new HashMap<>();

    /*private void loadProductCategories() {
        new Thread(() -> {
            try {
                DataOutputStream out = SocketManager.getInstance().getOut();
                DataInputStream in = SocketManager.getInstance().getIn();

                // 发送请求类型
                out.writeUTF("ProductInfoRequest");

                // 发送获取商品信息的请求
                ProductInfoRequest req = new ProductInfoRequest("all", "");
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

                 // 构建商品ID到分类的映射
                    for (Product product : info.getProducts()) {
                        productCategoryMap.put(product.getId(), product.getCategory());
                    }

                    System.out.println("商品分类信息加载完成，共加载" + info.getProducts().size() + "个商品");

                    // 重新生成报表以使用新的分类信息
                    Platform.runLater(() -> generateReport());
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
    }*/
    private void loadProductCategories(RequestCallback callback) {
        new Thread(() -> {
            try {
                DataOutputStream out = SocketManager.getInstance().getOut();
                DataInputStream in = SocketManager.getInstance().getIn();

                // 1. 先发送请求类型
                out.writeUTF("ProductInfoRequest");
                out.flush(); // 强制发送，避免数据滞留

                // 2. 再发送请求参数
                ProductInfoRequest req = new ProductInfoRequest("", "");
                out.writeUTF(gson.toJson(req));
                out.flush();

                // 3. 接收响应
                String response = in.readUTF();
                System.out.println("收到商品信息响应: " + response);

                if (response.startsWith("ERROR|")) {
                    String errorMsg = response.substring(6);
                    Platform.runLater(() -> showAlert("错误", "获取商品信息失败: " + errorMsg));
                } else if (response.startsWith("SUCCESS|")) {
                    String jsonStr = response.substring(8);
                    ProductInfoResponse info = gson.fromJson(jsonStr, ProductInfoResponse.class);
                    for (Product product : info.getProducts()) {
                        productCategoryMap.put(product.getId(), product.getCategory());
                    }
                    System.out.println("商品分类信息加载完成，共" + info.getProducts().size() + "个商品");
                } else {
                    Platform.runLater(() -> showAlert("错误", "商品信息响应格式错误: " + response));
                }

            } catch (Exception e) {
                Platform.runLater(() -> showAlert("错误", "获取商品信息失败: " + e.getMessage()));
                e.printStackTrace();
            } finally {
                // 无论成功失败，都执行回调，确保后续流程继续
                if (callback != null) {
                    callback.onComplete();
                }
            }
        }).start();
    }

   /* private void loadProductCategories() {
        new Thread(() -> {
            try {
                DataOutputStream out = SocketManager.getInstance().getOut();
                DataInputStream in = SocketManager.getInstance().getIn();

                // 发送请求类型
                out.writeUTF("ProductInfoRequest");

                // 发送获取所有商品信息的请求
                ProductInfoRequest req = new ProductInfoRequest("", ""); // 空参数获取所有商品
                out.writeUTF(gson.toJson(req));
                out.flush();

                // 接收服务器响应
                String response = in.readUTF();
                System.out.println("收到商品信息响应: " + response);

                if (response.startsWith("ERROR|")) {
                    String errorMsg = response.substring(6);
                    Platform.runLater(() -> showAlert("错误", "获取商品信息失败: " + errorMsg));
                } else if (response.startsWith("SUCCESS|")) {
                    String jsonStr = response.substring(8);
                    ProductInfoResponse info = gson.fromJson(jsonStr, ProductInfoResponse.class);

                    // 构建商品ID到分类的映射
                    for (Product product : info.getProducts()) {
                        productCategoryMap.put(product.getId(), product.getCategory());
                    }

                    System.out.println("商品分类信息加载完成，共加载" + info.getProducts().size() + "个商品");

                    // 重新生成报表以使用新的分类信息
                    Platform.runLater(() -> generateReport());
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
    }*/

    private void generateReport() {
        // 获取筛选条件
        LocalDate startDate = startDatePicker.getValue();
        LocalDate endDate = endDatePicker.getValue();
        System.out.println("筛选日期范围: " + startDate + " 到 " + endDate);
        // 筛选订单
        List<Order> filteredOrders = allOrders.stream()
                .filter(order -> {
                    LocalDate orderDate = LocalDate.parse(order.getDate(), DateTimeFormatter.ofPattern("yyyy-MM-dd"));
                    return !orderDate.isBefore(startDate) && !orderDate.isAfter(endDate);
                })
                .collect(Collectors.toList());
        System.out.println("筛选后订单数量: " + filteredOrders.size());
        // 计算销售统计数据
        double totalSales = filteredOrders.stream().mapToDouble(Order::getTotal).sum();
        int totalOrders = filteredOrders.size();
        double avgOrderValue = totalOrders > 0 ? totalSales / totalOrders : 0;

        // 更新销售概览
        totalSalesLabel.setText(String.format("¥%.2f", totalSales));
        totalOrdersLabel.setText(String.valueOf(totalOrders));
        avgOrderValueLabel.setText(String.format("¥%.2f", avgOrderValue));

        // 计算热门商品
        Map<String, ProductSalesData> productSalesMap = new HashMap<>();
        for (Order order : filteredOrders) {
            for (OrderItem item : order.getItems()) {
                ProductSalesData data = productSalesMap.getOrDefault(item.getId(),
                        new ProductSalesData(item.getId(), item.getName(), 0, 0));
                data.setQuantity(data.getQuantity() + item.getQuantity());
                data.setSales(data.getSales() + item.getPrice() * item.getQuantity());
                productSalesMap.put(item.getId(), data);
            }
        }

        // 计算占比
        for (ProductSalesData data : productSalesMap.values()) {
            data.setPercentage(totalSales > 0 ? (data.getSales() / totalSales) * 100 : 0);
        }

        // 获取热门商品排行
        List<ProductSalesData> productSalesList = new ArrayList<>(productSalesMap.values());
        productSalesList.sort((a, b) -> Double.compare(b.getSales(), a.getSales()));

        // 设置热门商品
        if (!productSalesList.isEmpty()) {
            topProductLabel.setText(productSalesList.get(0).getName());
        } else {
            topProductLabel.setText("暂无数据");
        }

        // 更新商品销售排行表格
        ObservableList<ProductSalesData> tableData = FXCollections.observableArrayList();
        for (int i = 0; i < Math.min(10, productSalesList.size()); i++) {
            ProductSalesData data = productSalesList.get(i);
            data.setRank(i + 1);
            tableData.add(data);
        }
        productRankingTable.setItems(tableData);

        // 更新销售趋势图表
        updateSalesChart(filteredOrders);

        // 更新分类销售占比图表
        updateCategoryPieChart(productSalesList);
        loadPaymentMethods(startDate, endDate,null);
        // 更新支付方式分布图表
    }

    // 其他方法保持不变...
    private void updateSalesChart(List<Order> orders) {
        salesChart.getData().clear();

        // 按日期分组计算销售额
        Map<String, Double> dailySales = new TreeMap<>();
        for (Order order : orders) {
            String date = order.getDate();
            dailySales.put(date, dailySales.getOrDefault(date, 0.0) + order.getTotal());
        }

        // 创建图表数据
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("每日销售额");

        for (Map.Entry<String, Double> entry : dailySales.entrySet()) {
            series.getData().add(new XYChart.Data<>(entry.getKey(), entry.getValue()));
        }

        salesChart.getData().add(series);
    }

    private void updateCategoryPieChart(List<ProductSalesData> productSales) {
        categoryPieChart.getData().clear();

        // 按商品分类计算销售额
        Map<String, Double> categorySales = new HashMap<>();
        for (ProductSalesData data : productSales) {
            // 这里需要根据商品ID获取分类信息，简化处理
            String category = getCategoryById(data.getId());
            categorySales.put(category, categorySales.getOrDefault(category, 0.0) + data.getSales());
        }

        // 创建饼图数据
        ObservableList<PieChart.Data> pieChartData = FXCollections.observableArrayList();
        for (Map.Entry<String, Double> entry : categorySales.entrySet()) {
            pieChartData.add(new PieChart.Data(entry.getKey(), entry.getValue()));
        }

        categoryPieChart.setData(pieChartData);
    }

   /* private void updatePaymentPieChart(List<Order> orders) {
        paymentPieChart.getData().clear();

        // 模拟支付方式数据
        Map<String, Integer> paymentMethods = new HashMap<>();
        paymentMethods.put("支付宝", 60);
        paymentMethods.put("微信支付", 30);
        paymentMethods.put("银行卡", 10);

        // 创建饼图数据
        ObservableList<PieChart.Data> pieChartData = FXCollections.observableArrayList();
        for (Map.Entry<String, Integer> entry : paymentMethods.entrySet()) {
            pieChartData.add(new PieChart.Data(entry.getKey(), entry.getValue()));
        }

        paymentPieChart.setData(pieChartData);
    }*/
    private void updatePaymentPieChart(Map<String, Double> paymentMethods) {
        paymentPieChart.getData().clear();

        // 创建饼图数据
        ObservableList<PieChart.Data> pieChartData = FXCollections.observableArrayList();
        for (Map.Entry<String, Double> entry : paymentMethods.entrySet()) {
            pieChartData.add(new PieChart.Data(entry.getKey(), entry.getValue()));
        }

        // 如果没有数据，显示提示
        if (pieChartData.isEmpty()) {
            pieChartData.add(new PieChart.Data("暂无数据", 1));
        }

        paymentPieChart.setData(pieChartData);
    }

    private void loadPaymentMethods(LocalDate startDate, LocalDate endDate, RequestCallback callback) {
        new Thread(() -> {
            try {
                DataOutputStream out = SocketManager.getInstance().getOut();
                DataInputStream in = SocketManager.getInstance().getIn();

                // 1. 发送请求类型
                out.writeUTF("PaymentMethodStatsRequest");
                out.flush();

                // 2. 发送请求参数（建议将Map<String, Object>改为Map<String, String>，更明确）
                Map<String, String> request = new HashMap<>();
                request.put("startDate", startDate.toString());
                request.put("endDate", endDate.toString());
                out.writeUTF(gson.toJson(request));
                out.flush();

                // 3. 接收响应
                String response = in.readUTF();
                System.out.println("收到支付方式统计响应: " + response);

                if (response.startsWith("ERROR|")) {
                    String errorMsg = response.substring(6);
                    System.out.println("获取支付方式统计失败: " + errorMsg);
                } else if (response.startsWith("SUCCESS|")) {
                    String jsonStr = response.substring(8);
                    Map<String, Double> paymentMethods = gson.fromJson(jsonStr, 
                        new TypeToken<Map<String, Double>>(){}.getType());
                    Platform.runLater(() -> updatePaymentPieChart(paymentMethods));
                } else {
                    System.out.println("支付方式响应格式错误: " + response);
                }

            } catch (Exception e) {
                System.out.println("获取支付方式统计失败: " + e.getMessage());
                e.printStackTrace();
            } finally {
                // 执行回调（若有后续逻辑需串行）
                if (callback != null) {
                    callback.onComplete();
                }
            }
        }).start();
    }


    private String getCategoryById(String productId) {
        // 从商品分类映射中获取分类，如果找不到则返回"其他"
        String category = productCategoryMap.get(productId);

        if (category != null) {
            // 将英文分类转换为中文显示
            switch (category) {
                case "stationary": return "文具办公";
                case "digital": return "3C数码";
                case "beverage": return "饮品饮料";
                case "snacks": return "休闲零食";
                case "personal": return "个人洗护";
                case "daily": return "日用百货";
                default: return "其他";
            }
        }

        return "其他";
    }

    @FXML
    private void handleExport() {
        // 创建文件选择器
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("导出财务报表");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Excel Files", "*.xlsx"));

        // 显示保存对话框
        java.io.File file = fileChooser.showSaveDialog(null);

        if (file != null) {
            try {
                // 这里应该实现导出Excel的逻辑
                // 可以使用Apache POI或其他库来生成Excel文件
                System.out.println("导出报表到: " + file.getAbsolutePath());

                // 显示导出成功提示
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("导出成功");
                alert.setHeaderText(null);
                alert.setContentText("财务报表已成功导出到: " + file.getName());
                alert.showAndWait();
            } catch (Exception e) {
                e.printStackTrace();

                // 显示导出失败提示
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("导出失败");
                alert.setHeaderText(null);
                alert.setContentText("导出财务报表时发生错误: " + e.getMessage());
                alert.showAndWait();
            }
        }
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    public String getUserid() {
		return userid;
	}

	public void setUserid(String userid) {
		this.userid = userid;
	}

	// 商品销售数据内部类保持不变...
    public static class ProductSalesData {
        private String id;
        private String name;
        private int quantity;
        private double sales;
        private double percentage;
        private int rank;

        public ProductSalesData(String id, String name, int quantity, double sales) {
            this.id = id;
            this.name = name;
            this.quantity = quantity;
            this.sales = sales;
        }

        // Getter和Setter方法
        public String getId() { return id; }
        public void setId(String id) { this.id = id; }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public int getQuantity() { return quantity; }
        public void setQuantity(int quantity) { this.quantity = quantity; }

        public double getSales() { return sales; }
        public void setSales(double sales) { this.sales = sales; }

        public double getPercentage() { return percentage; }
        public void setPercentage(double percentage) { this.percentage = percentage; }

        public int getRank() { return rank; }
        public void setRank(int rank) { this.rank = rank; }

        public String getPercentageFormatted() {
            return String.format("%.1f%%", percentage);
        }
    }
}