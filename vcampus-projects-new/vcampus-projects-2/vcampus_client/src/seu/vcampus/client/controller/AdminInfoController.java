package seu.vcampus.client.controller;

import java.io.DataInputStream;
import java.io.DataOutputStream;

import com.google.gson.Gson;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import seu.vcampus.client.network.SocketManager;
import seu.vcampus.model.AdminInfo;
import seu.vcampus.model.AdminInfoRequest;

public class AdminInfoController {

    private String adminid;
    private Gson gson = new Gson();

    /* 管理员个人信息 */
    @FXML private Label adminId;
    @FXML private Label adminName;
    @FXML private Label adminGender;
    @FXML private Label adminBirth;
    @FXML private Label adminIdCard;
    @FXML private Label adminPhone;
    @FXML private Label adminEmail;
    @FXML private Label adminPolitics;
    @FXML private Label adminPosition;


    @FXML
    private void initialize() {
        // 初始化时不加载数据，等待setAdminId调用
    }

    @FXML
    void handleRefresh(ActionEvent event) {
        // 刷新数据
        loadPersonalInfo();
    }

    @FXML
    void handleBack(ActionEvent event) {
        // 返回逻辑
    }

    private void loadPersonalInfo() {
    	System.out.println("StudentInfoController: 加载个人信息，管理员ID: " + adminid); // 添加调试信息
        if (adminid == null || adminid.isEmpty()) {
            showErrorAlert("错误", "管理员ID未设置");
            return;
        }

        // 从数据库中拉取信息
        new Thread(() -> {
            try {
                DataOutputStream out = SocketManager.getInstance().getOut();
                DataInputStream in = SocketManager.getInstance().getIn();

                // 发送请求类型
                out.writeUTF("AdminInfoRequest");

                // 发送获取学生信息的请求
                AdminInfoRequest req = new AdminInfoRequest();
                req.setUserid(adminid);
                out.writeUTF(gson.toJson(req));
                out.flush();

                // 接收服务器响应
                String response = in.readUTF();
                System.out.println("收到服务器响应: " + response); // 调试信息

                if (response.startsWith("ERROR|")) {
                    // 处理错误信息
                    String errorMsg = response.substring(6); // 去掉"ERROR|"前缀
                    Platform.runLater(() -> showErrorAlert("获取失败", errorMsg));
                } else if (response.startsWith("SUCCESS|")) {
                    // 处理成功响应
                    String jsonStr = response.substring(8); // 去掉"SUCCESS|"前缀
                    AdminInfo info = gson.fromJson(jsonStr, AdminInfo.class);

                    // 在UI线程中更新界面
                    Platform.runLater(() -> {
                        adminId.setText(info.getUserid());
                        adminName.setText(info.getName());
                        adminGender.setText(info.getGender());
                        adminBirth.setText(info.getBirth());
                        adminIdCard.setText(info.getIdCard());
                        adminPhone.setText(info.getPhone());
                        adminEmail.setText(info.getEmail());
                        adminPolitics.setText(info.getPolitics());
                        adminPosition.setText(info.getPosition());

                    });
                } else {
                    Platform.runLater(() ->
                        showErrorAlert("响应格式错误", "服务器返回了未知格式的响应: " + response)
                    );
                }

            } catch (Exception e) {
                Platform.runLater(() ->
                    showErrorAlert("连接错误", "获取信息失败：" + e.getMessage())
                );
                e.printStackTrace();
            }
        }).start();
    }



    private void showErrorAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("错误");
        alert.setHeaderText(title);
        alert.setContentText(message);
        alert.showAndWait();
    }

    public void setAdminId(String adminid) {
        this.adminid = adminid;
        System.out.println("AdminInfoController: 设置管理员ID为: " + adminid);
        // 设置管理员ID后立即加载数据
        loadPersonalInfo();
    }
}