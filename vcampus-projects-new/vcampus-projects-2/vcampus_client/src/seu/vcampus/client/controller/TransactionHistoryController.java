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
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import seu.vcampus.client.network.SocketManager;
import seu.vcampus.model.Transaction;
import seu.vcampus.model.TransactionRequest;
import seu.vcampus.model.TransactionResponse;

public class TransactionHistoryController implements Initializable {

    @FXML
    private VBox transactionsContainer;

    @FXML
    private Label totalAmount;

    @FXML
    private Label totalIncome;

    @FXML
    private Label totalExpense;

    @FXML
    private Label transactionCount;

    @FXML
    private TextField searchField;

    private List<Transaction> transactions = new ArrayList<>();
    private List<Transaction> filteredTransactions = new ArrayList<>();
    private String userid;
    private Gson gson = new Gson(); // 用于JSON解析


    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // 初始化时不加载模拟数据，等待用户ID设置后从服务器加载
        System.out.println("消费记录控制器初始化完成");
    }


    public void setUserId(String userid) {
		this.userid = userid;
        // 拿到用户ID后，从服务器加载真实交易记录
        loadTransactionsFromServer();
	}


    // 从服务器加载交易记录（替换原来的模拟数据方法）
    public void loadTransactionsFromServer() {
        new Thread(() -> {
            try {
                // 获取服务器连接流（复用你的SocketManager）
                DataOutputStream out = SocketManager.getInstance().getOut();
                DataInputStream in = SocketManager.getInstance().getIn();

                // 1. 发送请求类型
                out.writeUTF("TransactionHistoryRequest");

                // 2. 构建请求对象（包含当前用户ID）
                TransactionRequest request = new TransactionRequest();
                request.setUserId(userid);
                String jsonRequest = gson.toJson(request);
                out.writeUTF(jsonRequest);
                out.flush();

                // 3. 接收服务器响应
                String response = in.readUTF();
                if (response.startsWith("SUCCESS|")) {
                    // 解析响应数据
                    String jsonData = response.substring(8);
                    TransactionResponse transactionResponse = gson.fromJson(jsonData, TransactionResponse.class);

                    // 更新交易列表（在UI线程中执行）
                    Platform.runLater(() -> {
                    	if (transactionResponse.getTransactions() != null) {
                    	    transactions.addAll(transactionResponse.getTransactions());
                    	    filteredTransactions.addAll(transactions);
                    	} else {
                    	    // 处理空集合的情况
                    	    transactions.clear();
                    	    filteredTransactions.clear();
                    	}

                        // 刷新显示
                        displayTransactions();
                    });
                } else {
                    // 处理错误响应
                    Platform.runLater(() -> {
                        showErrorAlert("加载失败", response.substring(6));
                    });
                }
            } catch (IOException e) {
                // 网络错误处理
                Platform.runLater(() -> {
                    showErrorAlert("网络错误", "无法连接到服务器：" + e.getMessage());
                });
            }
        }).start();
    }

    private void updateStatistics() {
        double income = 0;
        double expense = 0;
        int count = filteredTransactions.size();

        for (Transaction transaction : filteredTransactions) {
            if ("income".equals(transaction.getType())) {
                income += transaction.getAmount();
            } else if ("expense".equals(transaction.getType())) {
                expense += transaction.getAmount();
            }
        }

        double netAmount = income - expense;

        // 更新统计信息
        totalAmount.setText("总金额: ¥" + String.format("%.2f", netAmount));
        totalIncome.setText("¥" + String.format("%.2f", income));
        totalExpense.setText("¥" + String.format("%.2f", expense));
        transactionCount.setText(count + "笔");
    }

    private void displayTransactions() {
        transactionsContainer.getChildren().clear();

        if (filteredTransactions.isEmpty()) {
            // 显示空交易提示
            VBox emptyTransactions = new VBox(20);
            emptyTransactions.getStyleClass().add("empty-transactions");
            emptyTransactions.setAlignment(Pos.CENTER);

            Circle emptyCircle = new Circle(50);
            emptyCircle.setFill(javafx.scene.paint.Paint.valueOf("#ecf0f1"));

            Label emptyLabel = new Label("暂无交易记录");
            emptyLabel.getStyleClass().add("empty-transactions-label");

            emptyTransactions.getChildren().addAll(emptyCircle, emptyLabel);
            transactionsContainer.getChildren().add(emptyTransactions);
        } else {
            // 显示交易列表
            for (Transaction transaction : filteredTransactions) {
                HBox transactionCard = createTransactionCard(transaction);
                transactionsContainer.getChildren().add(transactionCard);
            }
        }

        // 更新统计信息
        updateStatistics();
    }

    private HBox createTransactionCard(Transaction transaction) {
        HBox card = new HBox(15);
        card.getStyleClass().add("transaction-card");
        card.setAlignment(Pos.CENTER_LEFT);
        card.setPadding(new Insets(15));

        // 交易图标
        Circle icon = new Circle(25);

        if ("income".equals(transaction.getType())) {
            icon.setFill(javafx.scene.paint.Paint.valueOf("#2ecc71"));
        } else {
            icon.setFill(javafx.scene.paint.Paint.valueOf("#e74c3c"));
        }

        // 交易信息
        VBox infoBox = new VBox(5);
        infoBox.setAlignment(Pos.CENTER_LEFT);

        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);

        Label idLabel = new Label(transaction.getId());
        idLabel.getStyleClass().add("transaction-id");

        Label dateLabel = new Label(transaction.getDate());
        dateLabel.getStyleClass().add("transaction-date");

        header.getChildren().addAll(idLabel, dateLabel);

        Label detailLabel = new Label(transaction.getDescription());
        detailLabel.getStyleClass().add("transaction-detail");

        infoBox.getChildren().addAll(header, detailLabel);

        // 交易金额和类型
        VBox amountBox = new VBox(5);
        amountBox.setAlignment(Pos.CENTER_RIGHT);

        Label amountLabel = new Label((transaction.getType().equals("income") ? "+" : "-") + "¥" + transaction.getAmount());
        amountLabel.getStyleClass().add("transaction-amount");
        amountLabel.getStyleClass().add(transaction.getType().equals("income") ? "amount-income" : "amount-expense");

        HBox typeBox = new HBox(10);
        typeBox.setAlignment(Pos.CENTER_RIGHT);

        Label typeLabel = new Label(transaction.getCategory());
        typeLabel.getStyleClass().add("transaction-type");

        // 根据交易类型设置不同的样式类
        switch (transaction.getCategory()) {
            case "支付": typeLabel.getStyleClass().add("type-payment"); break;
            case "退款": typeLabel.getStyleClass().add("type-refund"); break;
            case "充值": typeLabel.getStyleClass().add("type-recharge"); break;
            case "提现": typeLabel.getStyleClass().add("type-withdraw"); break;
        }

        Label statusLabel = new Label(getStatusText(transaction.getStatus()));
        statusLabel.getStyleClass().add("transaction-status");
        statusLabel.getStyleClass().add("status-" + transaction.getStatus());

        typeBox.getChildren().addAll(typeLabel, statusLabel);
        amountBox.getChildren().addAll(amountLabel, typeBox);

        // 组装交易卡片
        card.getChildren().addAll(icon, infoBox, amountBox);

        return card;
    }

    private String getStatusText(String status) {
        switch (status) {
            case "success": return "成功";
            case "pending": return "处理中";
            case "failed": return "失败";
            default: return "未知";
        }
    }

    @FXML
    private void handleSearch() {
        String keyword = searchField.getText().toLowerCase();

        // 筛选交易
        filteredTransactions.clear();

        if (keyword.isEmpty()) {
            filteredTransactions.addAll(transactions);
        } else {
            for (Transaction transaction : transactions) {
                if (transaction.getId().toLowerCase().contains(keyword) ||
                    transaction.getDescription().toLowerCase().contains(keyword) ||
                    transaction.getCategory().toLowerCase().contains(keyword)) {
                    filteredTransactions.add(transaction);
                }
            }
        }

        // 更新显示
        displayTransactions();
    }

    @FXML
    private void handleTimeFilter(javafx.event.ActionEvent event) {
        javafx.scene.control.Button button = (javafx.scene.control.Button) event.getSource();
        String filter = button.getText();
        System.out.println("时间筛选: " + filter);

        // 这里可以实现按时间筛选的逻辑（例如调用服务器接口按时间范围查询）
        // 示例：loadTransactionsFromServerWithFilter(userid, filter);
    }

    @FXML
    private void handleTypeFilter(javafx.event.ActionEvent event) {
        javafx.scene.control.Button button = (javafx.scene.control.Button) event.getSource();
        String filter = button.getText();

        // 筛选交易类型
        filteredTransactions.clear();

        if ("全部".equals(filter)) {
            filteredTransactions.addAll(transactions);
        } else if ("收入".equals(filter)) {
            for (Transaction transaction : transactions) {
                if ("income".equals(transaction.getType())) {
                    filteredTransactions.add(transaction);
                }
            }
        } else if ("支出".equals(filter)) {
            for (Transaction transaction : transactions) {
                if ("expense".equals(transaction.getType())) {
                    filteredTransactions.add(transaction);
                }
            }
        }

        // 更新显示
        displayTransactions();
    }

    // 错误提示对话框（复用你的UI风格）
    private void showErrorAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("错误");
        alert.setHeaderText(title);
        alert.setContentText(message);
        alert.showAndWait();
    }

}