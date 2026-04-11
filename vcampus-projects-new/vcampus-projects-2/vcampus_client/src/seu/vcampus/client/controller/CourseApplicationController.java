package seu.vcampus.client.controller;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import com.google.gson.Gson;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.event.ActionEvent;
import java.net.URL;
import java.util.ResourceBundle;
import java.util.function.UnaryOperator;
import java.util.regex.Pattern;

import javafx.scene.shape.Rectangle;
import javafx.geometry.Insets; 
import javafx.scene.control.ListView; 
import javafx.scene.control.ListCell; 
import java.lang.reflect.Field;
import javafx.scene.Node;
import javafx.collections.ObservableList;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.scene.paint.Color;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.animation.ScaleTransition;
import javafx.application.Platform;
import javafx.util.Duration;
import seu.vcampus.client.network.SocketManager;
import seu.vcampus.model.LoginRequest;
import seu.vcampus.model.LoginResponse;
import seu.vcampus.model.TestCourse;

public class CourseApplicationController implements Initializable{
	
	public String tId = "Xia"; 
	
	public String tName = "Niko";
	
	public String tDepartment = "育苗学院";
	
	private static final Pattern NUMBER_PATTERN = Pattern.compile("\\d*");
	
	private static final Gson gson = new Gson();

    @FXML
    private ComboBox<String> studentType;

    @FXML
    private TextField courseName;

    @FXML
    private TextField proportion;

    @FXML
    private ComboBox<String> major;

    @FXML
    private Button applyBtn;

    @FXML
    private TextField firstWeek;

    @FXML
    private ComboBox<String> credit;

    @FXML
    private TextField studentNum;

    @FXML
    private TextField lastWeek;

    @FXML
    private ComboBox<String> academy;
    
    public void setUserId(String TTID) {
    	tId = TTID;
    	System.out.println(tId);
    	getApplicationInfo();
    	initComboBox();
    	initComboMajor();
    	UnaryOperator<TextFormatter.Change> filter = change -> {
            String newText = change.getControlNewText();
            if (NUMBER_PATTERN.matcher(newText).matches()) {
                return change; 
            }
            return null;
        };
        studentNum.setTextFormatter(new TextFormatter<>(filter));
        firstWeek.setTextFormatter(new TextFormatter<>(filter));
        lastWeek.setTextFormatter(new TextFormatter<>(filter));
    }
    
    @Override
    public void initialize(URL location, ResourceBundle resources) {
    	
        
    }
    
    private void getApplicationInfo() {
    	new Thread(() -> {
            try {
                DataOutputStream out = SocketManager.getInstance().getOut();
                out.writeUTF("ApplyCourseInfo");
                out.writeUTF(tId);
                out.flush();
                DataInputStream in = SocketManager.getInstance().getIn();
                tName = in.readUTF();
                tDepartment = in.readUTF();
            } catch (Exception e) {
                Platform.runLater(() ->
                	showErrorDialog("请检查服务器是否启动或网络是否正常：" + e.getMessage())
                );
                e.printStackTrace();
            }
        }).start();
    }
    
    private void initComboBox() {
    	
    	academy.getItems().addAll(
                "计软智学院",
                "机械工程学院",
                "电子科学与工程学院"
            );
            academy.getSelectionModel().selectFirst();
            
            studentType.getItems().addAll(
                    "大一学生",
                    "大二学生",
                    "大三学生",
                    "大四学生"
                );
            studentType.getSelectionModel().selectFirst();
            
            major.getItems().addAll(
                    "计算机科学与技术",
                    "软件工程",
                    "人工智能",
                    "网络安全"
                );
    	major.getSelectionModel().selectFirst();
    	
    	credit.getItems().addAll("0.25","0.5","1","2","3","4");
    	credit.getSelectionModel().selectFirst();
    }
    
    public void initComboMajor() {
    	academy.valueProperty().addListener((observable,oldvalue,newvalue)->{
            ObservableList<String> majors = major.getItems();
            majors.clear();
            if(newvalue!=null) {
                switch(newvalue) {
                case("计软智学院"):
                    majors.addAll("计算机科学与工程","软件工程","网络安全");
                break;
                case("机械工程学院"):
                    majors.addAll("机械工程");
                break;
                case("电子科学与工程学院"):
                    majors.addAll("电子信息工程");
                }
                major.setValue(majors.get(0));
            }
        });
    }

    @FXML
    void onApplyClick(ActionEvent event) {
    	String tip;
    	if(courseName.getText().isEmpty()) {
    		tip = "课程名称不能为空！";
    		showErrorDialog(tip);
    	}
    	else if(firstWeek.getText().isEmpty()||lastWeek.getText().isEmpty()) {
    		tip = "周次不能为空！";
    		showErrorDialog(tip);
    	}
    	else if(Integer.parseInt(firstWeek.getText()) > Integer.parseInt(lastWeek.getText())) {
    		tip = "周次设置有误！";
    		showErrorDialog(tip);
    	}
    	else if(proportion.getText().isEmpty()) {
    		tip = "期末占比不能为空！";
    		showErrorDialog(tip);
    	}
    	else if(studentNum.getText().isEmpty()) {
    		tip = "预计人数不能为空！";
    		showErrorDialog(tip);
    	}
    	else if(Integer.parseInt(firstWeek.getText()) > Integer.parseInt(lastWeek.getText())) {
    		tip = "周次设置错误！";
    		showErrorDialog(tip);
    	}
    	else {
    		int FirstWeek = Integer.parseInt(firstWeek.getText());
    		int LastWeek = Integer.parseInt(lastWeek.getText());
    		String Niko = major.getSelectionModel().getSelectedItem();
    		if(Niko == null) {
    			tip = "专业不能为空！";
        		showErrorDialog(tip);
    		}
    		else {
    			String cName = courseName.getText();
    			double Credit = Double.parseDouble(credit.getValue());
    			int StudentType;
    			if(studentType.getValue() == "大一学生")
    				StudentType = 1;
    			else if(studentType.getValue() == "大二学生")
    				StudentType = 2;
    			else if(studentType.getValue() == "大三学生")
    				StudentType = 3;
    			else
    				StudentType = 4;
    			int StudentNum = Integer.parseInt(studentNum.getText());
    			String Academy = academy.getValue();
    			String Major = major.getValue();
    			
    			new Thread(() -> {
    	            try {
    	                // 使用SocketManager的out流发送登录请求
    	                DataOutputStream out = SocketManager.getInstance().getOut();
    	                
    	                out.writeUTF("CourseApplication");
    	                
    	                TestCourse NewCourse = new TestCourse(cName,tName,Credit,Academy,Major,FirstWeek,LastWeek,StudentType,StudentNum,"-1","-1",tDepartment);
    	                
    	                String jsonReq = gson.toJson(NewCourse);
    	                out.writeUTF(jsonReq);
    	                out.flush();

    	                // 使用SocketManager的in流接收响应
    	                DataInputStream in = SocketManager.getInstance().getIn();
    	                
    	                String Response = in.readUTF();
    	                
    	                Platform.runLater(() -> {
    	                    showOkDialog(Response); // 现在在正确的线程中显示对话框
    	                });

    	            } catch (Exception e) {
    	                Platform.runLater(() ->
    	                	showErrorDialog("请检查服务器是否启动或网络是否正常：" + e.getMessage())
    	                );
    	                e.printStackTrace();
    	            }
    	        }).start();
    		}
    	}
    }
    
    public void showErrorDialog(String tip) {
        Stage errorStage = new Stage();
        //设置窗口样式为无装饰（无标题栏、无边框）
        errorStage.initStyle(StageStyle.UNDECORATED);
        // 设置模态（阻塞其他窗口交互，类似Alert）
        errorStage.initModality(Modality.APPLICATION_MODAL);
        
        errorStage.initStyle(StageStyle.TRANSPARENT);

        Label contentLabel = new Label(tip);
        contentLabel.setStyle("-fx-text-fill: #ce2111; -fx-font-size: 14px;");

        // 关闭按钮
        Button okButton = new Button("确定");
        okButton.getStyleClass().add("error-button");
        okButton.setOnAction(e -> errorStage.close()); // 点击关闭窗口

        VBox root = new VBox(15, contentLabel, okButton);
        root.setStyle("-fx-padding: 20px; -fx-alignment: CENTER; -fx-min-width: 300px;-fx-background-color: #fdfdff; -fx-background-radius: 5px; -fx-border-color: #ce2111; -fx-border-width: 2px; -fx-border-radius: 5px;");

        Scene scene = new Scene(root);
        scene.setFill(Color.TRANSPARENT); 
        
        scene.getStylesheets().add(getClass().getResource("/seu/vcampus/client/css/edu_admin.css").toExternalForm());
        
        errorStage.setX(750);
        errorStage.setY(400);
        
        root.setScaleX(0.5);
        root.setScaleY(0.5);
        ScaleTransition scaleIn = new ScaleTransition(Duration.millis(100), root);
        scaleIn.setFromX(0.5);
        scaleIn.setFromY(0.5);
        scaleIn.setToX(1);   // 恢复到100%大小
        scaleIn.setToY(1);
        scaleIn.play();

        errorStage.setScene(scene);
        errorStage.showAndWait(); 
    }
    
    public void showOkDialog(String tip) {
        Stage errorStage = new Stage();
        //设置窗口样式为无装饰（无标题栏、无边框）
        errorStage.initStyle(StageStyle.UNDECORATED);
        // 设置模态（阻塞其他窗口交互，类似Alert）
        errorStage.initModality(Modality.APPLICATION_MODAL);
        
        errorStage.initStyle(StageStyle.TRANSPARENT);

        Label contentLabel = new Label(tip);
        contentLabel.setStyle("-fx-text-fill: #0f64b8; -fx-font-size: 14px;");

        // 关闭按钮
        Button okButton = new Button("确定");
        okButton.getStyleClass().add("my-button");
        okButton.setOnAction(e -> errorStage.close()); // 点击关闭窗口

        VBox root = new VBox(15, contentLabel, okButton);
        root.setStyle("-fx-padding: 20px; -fx-alignment: CENTER; -fx-min-width: 300px;-fx-background-color: #fdfdff; -fx-background-radius: 5px; -fx-border-color: #0f64b8; -fx-border-width: 2px; -fx-border-radius: 5px;");

        Scene scene = new Scene(root);
        scene.setFill(Color.TRANSPARENT); 
        
        scene.getStylesheets().add(getClass().getResource("/seu/vcampus/client/css/edu_admin.css").toExternalForm());
        
        errorStage.setX(750);
        errorStage.setY(400);
        
        root.setScaleX(0.5);
        root.setScaleY(0.5);
        ScaleTransition scaleIn = new ScaleTransition(Duration.millis(100), root);
        scaleIn.setFromX(0.5);
        scaleIn.setFromY(0.5);
        scaleIn.setToX(1);   // 恢复到100%大小
        scaleIn.setToY(1);
        scaleIn.play();

        errorStage.setScene(scene);
        errorStage.showAndWait(); 
    }

}
