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
import seu.vcampus.model.TeacherInfo;
import seu.vcampus.model.TeacherInfoRequest;
public class TeacherInfoController {

    private String teacherid;
    private Gson gson = new Gson();

    /* 教师个人信息 */
    @FXML private Label teacherId;
    @FXML private Label teacherName;
    @FXML private Label teacherGender;
    @FXML private Label teacherBirth;
    @FXML private Label teacherIdCard;
    @FXML private Label teacherPhone;
    @FXML private Label teacherEmail;
    @FXML private Label teacherPolitics;
    @FXML private Label teacherCollege;
    @FXML private Label teacherDepartment;
    @FXML private Label teacherTitle;
    @FXML private Label teacherEntryYear;
    @FXML private Label teacherMajor;
    @FXML private Label teacherEducation;
    @FXML private Label teacherDegree;

    @FXML
    private void initialize() {
        // 初始化时不加载数据，等待setTeacherId调用
    }

    @FXML
    void handleRefresh(ActionEvent event) {
        // 刷新数据
        loadPersonalInfo();
    }

    @FXML
    void handleBack(ActionEvent event) {
        // 返回上一级（可根据实际导航逻辑实现）
    }

    private void loadPersonalInfo() {
        System.out.println("TeacherInfoController: 加载个人信息，教师ID: " + teacherid);
        if (teacherid == null || teacherid.isEmpty()) {
            showErrorAlert("错误", "教师ID未设置");
            return;
        }

        // 从数据库拉取信息（新线程执行网络操作）
        new Thread(() -> {
            try {
                DataOutputStream out = SocketManager.getInstance().getOut();
                DataInputStream in = SocketManager.getInstance().getIn();

                // 发送请求类型
                out.writeUTF("TeacherInfoRequest");

                // 发送教师信息请求
                TeacherInfoRequest req = new TeacherInfoRequest();
                req.setUserid(teacherid);
                out.writeUTF(gson.toJson(req));
                out.flush();

                // 接收服务器响应
                String response = in.readUTF();
                System.out.println("收到服务器响应: " + response);

                if (response.startsWith("ERROR|")) {
                    String errorMsg = response.substring(6);
                    Platform.runLater(() -> showErrorAlert("获取失败", errorMsg));
                } else if (response.startsWith("SUCCESS|")) {
                    String jsonStr = response.substring(8);
                    TeacherInfo info = gson.fromJson(jsonStr, TeacherInfo.class);

                    // 在UI线程更新界面
                    Platform.runLater(() -> {
                        teacherId.setText(info.getUserid());
                        teacherName.setText(info.getName());
                        teacherGender.setText(info.getGender());
                        teacherBirth.setText(info.getBirth());
                        teacherIdCard.setText(info.getIdCard());
                        teacherPhone.setText(info.getPhone());
                        teacherEmail.setText(info.getEmail());
                        teacherPolitics.setText(info.getPolitics());
                        teacherCollege.setText(info.getCollege());
                        teacherDepartment.setText(info.getDepartment());
                        teacherTitle.setText(info.getTitle());
                        teacherEntryYear.setText(info.getEntryYear());
                        teacherMajor.setText(info.getMajor());
                        teacherEducation.setText(info.getEducation());
                        teacherDegree.setText(info.getDegree());
                    });
                } else {
                    Platform.runLater(() ->
                        showErrorAlert("响应格式错误", "服务器返回未知格式: " + response)
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

    // 设置教师ID并触发数据加载
    public void setTeacherId(String teacherId) {
        this.teacherid = teacherId;
        System.out.println("TeacherInfoController: 设置教师ID为: " + teacherId);
        loadPersonalInfo();
    }
}