package seu.vcampus.client.controller;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import com.google.gson.Gson;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import seu.vcampus.client.network.SocketManager;
import seu.vcampus.model.DormitoryExchangeRequest;

public class StudentExchangeController {

    private String studentId; // 添加studentId字段

    @FXML
    private TextArea returnReason;

    @FXML
    private TextArea exchangeReason;

    @FXML
    private Button returnApplyBtn;

    @FXML
    private Button exchangeApplyBtn;

    @FXML
    private void initialize() {
        System.out.println("退换舍控制器初始化完成");
    }

    // 添加设置studentId的方法
    public void setStudentId(String studentId) {
        this.studentId = studentId;
        System.out.println("StudentExchangeController: 设置学生ID为: " + studentId);
    }

    @FXML
    private void handleReturnApply() {
        if (studentId == null || studentId.isEmpty()) {
            showAlert(Alert.AlertType.ERROR, "申请失败", "学生ID未设置");
            return;
        }

        String reason = returnReason.getText().trim();

        if (reason.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "申请失败", "请填写退舍原因");
            return;
        }

        // 创建退宿请求对象
        DormitoryExchangeRequest request = new DormitoryExchangeRequest(studentId, 0, reason);

        // 在新线程中发送请求到服务器
        new Thread(() -> {
            try {
                DataOutputStream out = SocketManager.getInstance().getOut();
                DataInputStream in = SocketManager.getInstance().getIn();

                // 发送请求类型
                out.writeUTF("DormitoryExchangeRequest");

                // 发送请求数据
                Gson gson = new Gson();
                String jsonRequest = gson.toJson(request);
                out.writeUTF(jsonRequest);
                out.flush();

                // 接收响应
                String response = in.readUTF();
                if (response.startsWith("SUCCESS")) {
                    Platform.runLater(() -> {
                        showAlert(Alert.AlertType.INFORMATION, "申请提交成功", "您的退舍申请已提交，请等待审核");
                        returnReason.clear();
                    });
                } else {
                    String errorMsg = response.substring(response.indexOf("|") + 1);
                    Platform.runLater(() ->
                        showAlert(Alert.AlertType.ERROR, "申请提交失败", errorMsg)
                    );
                }
            } catch (IOException e) {
                Platform.runLater(() ->
                    showAlert(Alert.AlertType.ERROR, "网络错误", "无法连接到服务器: " + e.getMessage())
                );
            }
        }).start();
    }

    @FXML
    private void handleExchangeApply() {
        if (studentId == null || studentId.isEmpty()) {
            showAlert(Alert.AlertType.ERROR, "申请失败", "学生ID未设置");
            return;
        }

        String reason = exchangeReason.getText().trim();

        if (reason.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "申请失败", "请填写换舍原因");
            return;
        }

        // 创建换宿请求对象
        DormitoryExchangeRequest request = new DormitoryExchangeRequest(studentId, 1, reason);

        // 在新线程中发送请求到服务器
        new Thread(() -> {
            try {
                DataOutputStream out = SocketManager.getInstance().getOut();
                DataInputStream in = SocketManager.getInstance().getIn();

                // 发送请求类型
                out.writeUTF("DormitoryExchangeRequest");

                // 发送请求数据
                Gson gson = new Gson();
                String jsonRequest = gson.toJson(request);
                out.writeUTF(jsonRequest);
                out.flush();

                // 接收响应
                String response = in.readUTF();
                if (response.startsWith("SUCCESS")) {
                    Platform.runLater(() -> {
                        showAlert(Alert.AlertType.INFORMATION, "申请提交成功", "您的换舍申请已提交，请等待审核");
                        exchangeReason.clear();
                    });
                } else {
                    String errorMsg = response.substring(response.indexOf("|") + 1);
                    Platform.runLater(() ->
                        showAlert(Alert.AlertType.ERROR, "申请提交失败", errorMsg)
                    );
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
}