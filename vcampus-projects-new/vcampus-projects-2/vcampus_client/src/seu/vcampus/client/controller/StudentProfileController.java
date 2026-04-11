package seu.vcampus.client.controller;

import java.io.DataInputStream;
import java.io.DataOutputStream;
//import java.io.IOException;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.fxml.Initializable;
import java.net.URL;
import java.util.ResourceBundle;
import javafx.concurrent.Task;
//import javafx.application.Platform;
//import java.time.LocalDate;
//import java.time.format.DateTimeFormatter;

import com.google.gson.Gson;

import seu.vcampus.client.network.SocketManager;
import seu.vcampus.client.session.UserSession;
import seu.vcampus.model.StudentProfile;

public class StudentProfileController implements Initializable {

    @FXML private Label name;                	// 姓名
    @FXML private Label studentId;           	// 学号
    @FXML private Label college;             	// 学院
    @FXML private Label classId;           		// 班级
    @FXML private Label counsellor;          	// 辅导员
    @FXML private Label major;               	// 专业
    @FXML private Label mentor;              	// 导师
    @FXML private Label levelOfStudy;        	// 培养层级
    @FXML private Label studentStatus;       	// 学籍状态
    @FXML private Label admissionDate;        	// 入学日期
    @FXML private Label duration;            	// 学制
    @FXML private Label grade;               	// 就读年级
    @FXML private Label expectedGraduationDate; // 预期毕业日期
    
    @FXML private Label loadingLabel;
    @FXML private Label errorLabel;
    
    private static final Gson gson = new Gson();
    private DataInputStream dataIn;
    private DataOutputStream dataOut;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
    	initNetwork();
    	loadStudentProfile();
    }
    
    private void initNetwork() {
    	try {
            dataIn = SocketManager.getInstance().getIn();
            dataOut = SocketManager.getInstance().getOut();
        } catch (Exception e) {
            showError("获取 Socket 流失败：" + e.getMessage());
        }
    }
    
    private void loadStudentProfile() {
    	showLoading(true);
    	
    	Task<StudentProfile> loadTask = new Task<>() {
    		@Override
    		protected StudentProfile call() throws Exception {	
    			dataOut.writeUTF("GetStudentProfile");
    			dataOut.flush();
    			String  jsonRsp = dataIn.readUTF();
    			StudentProfile studentProfile = gson.fromJson(jsonRsp, StudentProfile.class);
    			
    			return studentProfile;
    		}
    		
    		@Override
    		protected void succeeded() {
    			super.succeeded();
    			StudentProfile studentProfile = getValue();
    			updateStudentProfile(studentProfile);
    			showLoading(false);
    		}
    		
    		@Override
    		protected void failed() {
    			super.failed();
    			Throwable error = getException(); 
    			showError("加载学生学籍信息失败" + (error.getMessage() != null ? error.getMessage() : "未知错误"));
    			showLoading(false);
    		}
    	};
    	new Thread(loadTask).start();
    }
    
    public void updateStudentProfile(StudentProfile studentProfile) {
        if (studentProfile == null) {
            showError("学生档案为空！");
            return;
        }
        
        name.setText(studentProfile.getName());
        studentId.setText(studentProfile.getStudentId());
        college.setText(studentProfile.getCollege());
        classId.setText(studentProfile.getClassName());
        counsellor.setText(studentProfile.getCounsellor());
        major.setText(studentProfile.getMajor());
        mentor.setText(studentProfile.getMentor());
        levelOfStudy.setText(studentProfile.getLevelOfStudy());
        studentStatus.setText(studentProfile.getStudentStatus());
        admissionDate.setText(studentProfile.getAdmissionDate());
        duration.setText(studentProfile.getDuration());
        grade.setText(studentProfile.getGrade());
        expectedGraduationDate.setText(studentProfile.getExpectedGraduationDate());  
    }
    
    private void showLoading(boolean show) {
        Platform.runLater(() -> loadingLabel.setVisible(show));
    }

    private void showError(String msg) {
        showError(msg, true);
    }

    private void showError(String msg, boolean isError) {
        Platform.runLater(() -> {
            errorLabel.setText(msg);
            errorLabel.setStyle(isError ? "-fx-text-fill: #d32f2f;" : "-fx-text-fill: #388e3c;");
            errorLabel.setVisible(true);
            new Thread(() -> {
                try { Thread.sleep(3000); } catch (InterruptedException e) {}
                Platform.runLater(() -> errorLabel.setVisible(false));
            }).start();
        });
    }
}