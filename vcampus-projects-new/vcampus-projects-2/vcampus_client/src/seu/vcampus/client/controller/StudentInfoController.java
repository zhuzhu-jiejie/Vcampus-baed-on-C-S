package seu.vcampus.client.controller;

import java.io.DataInputStream;
import java.io.DataOutputStream;

import com.google.gson.Gson;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.chart.BarChart;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.TableView;
import seu.vcampus.client.network.SocketManager;
import seu.vcampus.model.StudentInfo;
import seu.vcampus.model.StudentInfoRequest;

public class StudentInfoController {

    private String studentid;
    private Gson gson = new Gson();

    /* 学生个人信息 */
    @FXML private Label studentId;
    @FXML private Label studentName;
    @FXML private Label studentGender;
    @FXML private Label studentBirth;
    @FXML private Label studentIdCard;
    @FXML private Label studentPhone;
    @FXML private Label studentEmail;
    @FXML private Label studentPolitics;

    /* 学生学籍信息 */
    @FXML private Label studentCollege;
    @FXML private Label studentMajor;
    @FXML private Label studentClass;
    @FXML private Label admissionYear;
    @FXML private Label educationSystem;
    @FXML private Label studentStatus;
    @FXML private Label educationLevel;
    @FXML private Label graduationTime;

    /* 学生学业信息 */
    @FXML private Label totalGPA;
    @FXML private Label totalCredits;
    @FXML private Label completedCredits;
    @FXML private Label avgScore;
    @FXML private TableView<?> courseScoreTable;
    @FXML private BarChart<?, ?> scoreDistChart;

    @FXML
    private void initialize() {
        // 初始化时不加载数据，等待setStudentId调用
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
    	System.out.println("StudentInfoController: 加载个人信息，学生ID: " + studentid); // 添加调试信息
        if (studentid == null || studentid.isEmpty()) {
            showErrorAlert("错误", "学生ID未设置");
            return;
        }

        // 从数据库中拉取信息
        new Thread(() -> {
            try {
                DataOutputStream out = SocketManager.getInstance().getOut();
                DataInputStream in = SocketManager.getInstance().getIn();

                // 发送请求类型
                out.writeUTF("StudentInfoRequest");

                // 发送获取学生信息的请求
                StudentInfoRequest req = new StudentInfoRequest();
                req.setUserid(studentid);
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
                    StudentInfo info = gson.fromJson(jsonStr, StudentInfo.class);

                    // 在UI线程中更新界面
                    Platform.runLater(() -> {
                        studentId.setText(info.getUserid());
                        studentName.setText(info.getName());
                        studentGender.setText(info.getGender());
                        studentBirth.setText(info.getBirth());
                        studentIdCard.setText(info.getIdCard());
                        studentPhone.setText(info.getPhone());
                        studentEmail.setText(info.getEmail());
                        studentPolitics.setText(info.getPolitics());
                        studentCollege.setText(info.getCollege());
                        studentMajor.setText(info.getMajor());
                        studentClass.setText(info.getClassName());
                        admissionYear.setText(info.getAdmissionYear());
                        educationSystem.setText(info.getEducationSystem());
                        studentStatus.setText(info.getStudentStatus());
                        educationLevel.setText(info.getEducationLevel());
                        graduationTime.setText(info.getGraduationTime());
                        totalGPA.setText(info.getTotalGPA());
                        totalCredits.setText(info.getTotalCredits());
                        completedCredits.setText(info.getCompletedCredits());
                        avgScore.setText(info.getAvgScore());
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

    public void setStudentId(String studentid) {
        this.studentid = studentid;
        System.out.println("StudentInfoController: 设置学生ID为: " + studentid);
        // 设置学生ID后立即加载数据
        loadPersonalInfo();
    }
}