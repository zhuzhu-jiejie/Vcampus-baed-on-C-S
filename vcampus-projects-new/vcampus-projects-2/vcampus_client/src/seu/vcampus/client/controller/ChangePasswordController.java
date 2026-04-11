package seu.vcampus.client.controller;

import java.io.DataInputStream;
import java.io.DataOutputStream;

import com.google.gson.Gson;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import seu.vcampus.client.network.SocketManager;
import seu.vcampus.model.ChangePasswordRequest;

public class ChangePasswordController {
    @FXML private PasswordField oldPasswordField;
    @FXML private PasswordField newPasswordField;
    @FXML private PasswordField confirmPasswordField;
    @FXML private Button submitButton;
    @FXML private Button resetButton;
    @FXML private Label statusLabel;

    private String userId;
    private Gson gson = new Gson();

    @FXML
    public void initialize() {
        // 设置按钮事件
        submitButton.setOnAction(e -> handleSubmit());
        resetButton.setOnAction(e -> handleReset());

        // 初始状态
        statusLabel.setText("");
    }

    private void handleSubmit() {
        String oldPassword = oldPasswordField.getText();
        String newPassword = newPasswordField.getText();
        String confirmPassword = confirmPasswordField.getText();

        // 验证输入
        if (oldPassword.isEmpty() || newPassword.isEmpty() || confirmPassword.isEmpty()) {
            statusLabel.setText("请填写所有密码字段");
            return;
        }

        if (!newPassword.equals(confirmPassword)) {
            statusLabel.setText("新密码与确认密码不一致");
            return;
        }

        if (newPassword.length() < 6) {
            statusLabel.setText("新密码长度至少为6位");
            return;
        }

        if (oldPassword.equals(newPassword)) {
            statusLabel.setText("新密码不能与旧密码相同");
            return;
        }

        // 发送密码修改请求到服务器
        new Thread(() -> {
            try {
                DataOutputStream out = SocketManager.getInstance().getOut();
                DataInputStream in = SocketManager.getInstance().getIn();

                // 发送请求类型
                out.writeUTF("ChangePasswordRequest");

                // 发送密码修改请求
                ChangePasswordRequest req = new ChangePasswordRequest(userId, oldPassword, newPassword);
                out.writeUTF(gson.toJson(req));
                out.flush();


                // 接收服务器响应
                String response = in.readUTF(); // 挂起

                Platform.runLater(() -> {
                    if (response.startsWith("SUCCESS|")) {
                        statusLabel.setText("密码修改成功");
                        handleReset();
                    } else if (response.startsWith("ERROR|")) {
                        String errorMsg = response.substring(6); // 去掉"ERROR|"前缀
                        statusLabel.setText(errorMsg);
                    } else {
                        statusLabel.setText("未知响应格式: " + response);
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    statusLabel.setText("连接错误: " + e.getMessage());
                    showErrorAlert("连接错误", "密码修改失败: " + e.getMessage());
                });
                e.printStackTrace();
            }

            try {
            	Thread.sleep(3000);
            } catch(Exception e) {
            	e.printStackTrace();
            }

            Platform.runLater(()->{
            	statusLabel.setText("");
            });
        }).start();


    }

    private void handleReset() {
        oldPasswordField.clear();
        newPasswordField.clear();
        confirmPasswordField.clear();
//        statusLabel.setText("");
    }

    private void showErrorAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("错误");
        alert.setHeaderText(title);
        alert.setContentText(message);
        alert.showAndWait();
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }
}