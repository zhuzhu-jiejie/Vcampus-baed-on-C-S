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
import seu.vcampus.model.RepairRequest;

public class StudentRepairController {

    private String studentId; // 添加studentId字段

    @FXML
    private TextArea repairDescription;

    @FXML
    private Button repairApplyBtn;

    @FXML
    private void initialize() {
        System.out.println("报修控制器初始化完成");
    }

    // 添加设置studentId的方法
    public void setStudentId(String studentId) {
        this.studentId = studentId;
        System.out.println("StudentRepairController: 设置学生ID为: " + studentId);
    }

    @FXML
    private void handleRepairApply() {
        if (studentId == null || studentId.isEmpty()) {
            showAlert(Alert.AlertType.ERROR, "提交失败", "学生ID未设置");
            return;
        }

        String description = repairDescription.getText().trim();

        if (description.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "提交失败", "请填写报修描述");
            return;
        }

        // 创建报修请求对象
        RepairRequest request = new RepairRequest(studentId, description);

        // 在新线程中发送请求到服务器
        new Thread(() -> {
            try {
                DataOutputStream out = SocketManager.getInstance().getOut();
                DataInputStream in = SocketManager.getInstance().getIn();

                // 发送请求类型
                out.writeUTF("RepairRequest");

                // 发送请求数据
                Gson gson = new Gson();
                String jsonRequest = gson.toJson(request);
                out.writeUTF(jsonRequest);
                out.flush();

                // 接收响应
                String response = in.readUTF();
                if (response.startsWith("SUCCESS")) {
                    Platform.runLater(() -> {
                        showAlert(Alert.AlertType.INFORMATION, "提交成功", "您的报修申请已提交，工作人员将尽快处理");
                        repairDescription.clear();
                    });
                } else {
                    String errorMsg = response.substring(response.indexOf("|") + 1);
                    Platform.runLater(() ->
                        showAlert(Alert.AlertType.ERROR, "提交失败", errorMsg)
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