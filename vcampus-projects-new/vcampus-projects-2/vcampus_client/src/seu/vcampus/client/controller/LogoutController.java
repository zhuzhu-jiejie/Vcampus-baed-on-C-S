package seu.vcampus.client.controller;

import java.io.DataOutputStream;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import seu.vcampus.client.network.SocketManager;

public class LogoutController {

    @FXML
    private Button confirmButton;

    @FXML
    private Button cancelButton;

    @FXML
    private VBox confirmationCard; // 添加对卡片的引用

    // 假设teacherid是从其他地方注入的，这里使用示例值
    private String teacherid = "T00123";

    // 初始化方法
    @FXML
    public void initialize() {
        // 为确认按钮绑定事件处理器
        confirmButton.setOnAction(this::handleLogout);

        // 为取消按钮绑定事件 - 关闭当前窗口
        cancelButton.setOnAction(event -> {
            Stage stage = (Stage) cancelButton.getScene().getWindow();
            stage.close();
        });

        // 添加动画效果
        Platform.runLater(() -> {
            confirmationCard.getStyleClass().add("show");
        });
    }

    // 设置教师ID的方法
    public void setTeacherId(String teacherId) {
        this.teacherid = teacherId;
    }

    @FXML
    void handleLogout(ActionEvent event) {
        // 登出逻辑
        try {
            DataOutputStream out = SocketManager.getInstance().getOut();
            out.writeUTF("LogoutRequest");
            out.writeUTF(teacherid);
            out.flush();

            System.out.println("用户已登出: " + teacherid);

            // 在UI线程执行场景切换
            Platform.runLater(() -> {
                try {
                    // 加载登录页面
                    Parent root = FXMLLoader.load(getClass().getResource("/seu/vcampus/client/view/login.fxml"));

                    // 获取当前舞台
                    Stage stage = (Stage) confirmButton.getScene().getWindow();

                    // 创建新场景，设置尺寸与FXML根容器的minWidth/minHeight一致
                    Scene scene = new Scene(root, 800, 600);

                    // 添加样式表
                    scene.getStylesheets().add(getClass().getResource("/seu/vcampus/client/css/login.css").toExternalForm());

                    // 设置舞台属性
                    stage.setMinWidth(800.0);  // 与FXML根容器minWidth一致
                    stage.setMinHeight(600.0); // 与FXML根容器minHeight一致

                    stage.setTitle("身份认证系统"); // 设置标题
                    stage.setScene(scene);
                    stage.show();

                } catch (Exception e) {
                    showErrorAlert("页面跳转错误", "无法加载登录页面: " + e.getMessage());
                    e.printStackTrace();
                }
            });

        } catch (Exception e) {
            Platform.runLater(() ->
                showErrorAlert("登出错误", "登出过程中发生错误：" + e.getMessage())
            );
            e.printStackTrace();
        }
    }

    // 显示错误提示框
    private void showErrorAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}