package seu.vcampus.client.controller;

import java.net.URL;
import java.util.ResourceBundle;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TablePosition;
import javafx.scene.control.TableRow;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.geometry.Pos;

import java.util.stream.Collectors;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.net.HttpURLConnection;
import java.util.Set;

import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Callback;
import javafx.util.Duration;
import javafx.scene.input.MouseEvent;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import javafx.animation.ScaleTransition;
import javafx.application.Platform;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventTarget;
import seu.vcampus.client.network.SocketManager;
import seu.vcampus.model.TestCourse;

public class CoursePublicController implements Initializable{
	
	private static final Gson gson = new Gson();
	
	private List<TestCourse> courseList = new ArrayList<>();

    @FXML
    private ComboBox<String> studentType;

    @FXML
    private TableView<AppliedCourseItem> courseApplied;
    
    @FXML
    private TableColumn<AppliedCourseItem, String> nameColumn;
    
    @FXML
    private TableColumn<AppliedCourseItem, Double> creditColumn;
    
    @FXML
    private TableColumn<AppliedCourseItem, String> departmentColumn;
    
    @FXML
    private TableColumn<AppliedCourseItem, String> typeColumn;
    
    @FXML
    private TableColumn<AppliedCourseItem, String> teacherColumn;
    
    @FXML
    private TableColumn<AppliedCourseItem, Integer> numColumn;
    
    @FXML
    private Button refresh;

    @FXML
    private ComboBox<String> courseNature;

    @FXML
    private ComboBox<String> department;

    @FXML
    private ComboBox<Double> credit;
    
    //列表类/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    
    public class AppliedCourseItem{
    	private final SimpleStringProperty cName;
    	private final SimpleDoubleProperty cCredit;
    	private final SimpleStringProperty cTeacher;
    	private final SimpleStringProperty cDepartment;
    	private final SimpleIntegerProperty sType;
    	private final SimpleStringProperty cAcademy;
    	private final SimpleStringProperty cMajor;
    	private final SimpleIntegerProperty cFirst;
    	private final SimpleIntegerProperty cLast;
    	private final SimpleIntegerProperty cNum;
    	private boolean expanded;
    	
    	public AppliedCourseItem(String CNAME, double CCREDIT, String CNATURE, String CDEPARTMENT, int STYPE, String ACADEMY,
    			String MAJOR, int FIRST, int LAST, int NUM) {
    		this.cName = new SimpleStringProperty(CNAME);
    		this.cCredit = new SimpleDoubleProperty(CCREDIT);
    		this.cTeacher = new SimpleStringProperty(CNATURE);
    		this.cDepartment = new SimpleStringProperty(CDEPARTMENT);
    		this.sType = new SimpleIntegerProperty(STYPE);
    		this.cAcademy = new SimpleStringProperty(ACADEMY);
    		this.cMajor = new SimpleStringProperty(MAJOR);
    		this.cFirst = new SimpleIntegerProperty(FIRST);
    		this.cLast = new SimpleIntegerProperty(LAST);
    		this.cNum = new SimpleIntegerProperty(NUM);
    		this.expanded = false;
    	}
    	
    	public String getCName() {return cName.get();}
    	public double getCCredit() {return cCredit.get();}
    	public String getCTeacher() {return cTeacher.get();}
    	public String getCDepartment() {return cDepartment.get();}
    	public int getSType() {return sType.get();}
    	public String getSTypeS() {
    		switch (getSType()) {
            case 1: return "大一学生";
            case 2: return "大二学生";  
            case 3: return "大三学生";  
            case 4: return "大四学生";
            default: return "未知类型";
        }
    	}
    	public String getCAcademy() {return cAcademy.get();}
     	public String getCMajor() {return cMajor.get();}
     	public int getCFirst() {return cFirst.get();}
     	public int getCLast() {return cLast.get();}
     	public int getCNum() {return cNum.get();}
    	public boolean isExpanded() {return expanded;};
    	public void setExpanded(boolean expanded) {this.expanded = expanded;}
    	@Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            AppliedCourseItem that = (AppliedCourseItem) o;
            // 以课程名和教师名作为唯一标识（根据你的业务调整）
            return Objects.equals(getCName(), that.getCName()) &&
                   Objects.equals(getCTeacher(), that.getCTeacher());
        }

        @Override
        public int hashCode() {
            return Objects.hash(getCName(), getCTeacher());
        }
    }
    
    //初始化/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    
    @Override
    public void initialize(URL location, ResourceBundle resources) {
    	getData();
    	initTableColumns();
    	courseApplied.setOnMouseClicked(event -> handleTableClick(event));
    	addFilterListeners();
    }
    
    //列表功能///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    
    private void initTableColumns() {
    	nameColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getCName()));
    	creditColumn.setCellValueFactory(cellData -> new SimpleDoubleProperty(cellData.getValue().getCCredit()).asObject());
    	teacherColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getCTeacher()));
    	departmentColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getCDepartment()));
    	typeColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getSTypeS()));
    	numColumn.setCellValueFactory(cellData -> new SimpleIntegerProperty(cellData.getValue().getCNum()).asObject());
    	nameColumn.setSortable(false);
    	numColumn.setSortable(false);
    }
    
    
    private void handleTableClick(MouseEvent event) {
    	if (courseApplied.getSelectionModel().getSelectedItem() == null) {
            return;
        }
    	else if (event.getClickCount() == 2) {
    		showOkBox();
        }
    }
    
    private void showOkBox() {
    	 Stage OkBox = new Stage();
    	 
    	  OkBox.initStyle(StageStyle.UNDECORATED);
    	  OkBox.initModality(Modality.APPLICATION_MODAL);
    	  OkBox.initStyle(StageStyle.TRANSPARENT);
    	  
    	  VBox root = CreatOkBox(OkBox);
    	  Scene scene = new Scene(root);
    	  scene.setFill(Color.TRANSPARENT); 
    	  scene.getStylesheets().add(getClass().getResource("/seu/vcampus/client/css/edu_admin.css").toExternalForm());
    	  root.setStyle("-fx-padding: 20px; -fx-alignment: CENTER; -fx-min-width: 300px;-fx-background-color: #fdfdff; -fx-background-radius: 5px; -fx-border-color: #116ecc; -fx-border-width: 2px; -fx-border-radius: 5px;");
    	  
    	  OkBox.setX(635);
    	  OkBox.setY(500);
          
          root.setScaleX(0.5);
          root.setScaleY(0.5);
          ScaleTransition scaleIn = new ScaleTransition(Duration.millis(100), root);
          scaleIn.setFromX(0.5);
          scaleIn.setFromY(0.5);
          scaleIn.setToX(1);   // 恢复到100%大小
          scaleIn.setToY(1);
          scaleIn.play();

          OkBox.setScene(scene);
          OkBox.showAndWait(); 
    	  
    }
    
    private void getData() {
    	new Thread(() -> {
            try {
                // 使用SocketManager的out流发送登录请求
                DataOutputStream out = SocketManager.getInstance().getOut();
                
                out.writeUTF("CourseAppliedRequire");
                
                // 使用SocketManager的in流接收响应
                DataInputStream in = SocketManager.getInstance().getIn();
                
                String jsonData = in.readUTF();
                Type type = new TypeToken<List<TestCourse>>(){}.getType(); // 泛型类型
                courseList = gson.fromJson(jsonData, type);
                
                Platform.runLater(() -> {
                    // 3. 将List<TestCourse>转换为ObservableList<AppliedCourseItem>
                    ObservableList<AppliedCourseItem> tableData = FXCollections.observableArrayList();
                    for (TestCourse course : courseList) {
                        tableData.add(convertToAppliedCourseItem(course));
                    }
                    
                    courseApplied.setItems(tableData);
                    initComboBoxes();
                });
                
            } catch (Exception e) {
                Platform.runLater(() ->
                	showErrorDialog("请检查服务器是否启动或网络是否正常：" + e.getMessage())
                );
                e.printStackTrace();
            }
        }).start();
    }
    	
    private VBox CreatOkBox(Stage STAGE) {
    	Label COURSENAME = new Label(courseApplied.getSelectionModel().getSelectedItem().getCName());
    	Button NikoButton = new Button("同意");
    	Button RefuseButton = new Button("拒绝");
    	Button CloseButton = new Button("取消");
    	TextField courseID = new TextField();
    	ComboBox<String> sNature = new ComboBox<>();
    	courseID.setPromptText("设置课程ID");
    	ComboBox<Double> sCredit = new ComboBox<>();
    	ObservableList<String> items = FXCollections.observableArrayList(
    		    "必修", "任选", "限选", "通选"
    		);
    	ObservableList<Double> credits = FXCollections.observableArrayList(
    		    0.25, 0.5, 1.0, 2.0, 3.0, 4.0
    		);
    	HBox hbox = new HBox(10,courseID,sNature,sCredit,NikoButton,RefuseButton,CloseButton);
    	hbox.setPadding(new javafx.geometry.Insets(10));
    	hbox.setAlignment(Pos.CENTER);
    	sNature.setItems(items);
    	sCredit.setItems(credits);
    	sNature.getSelectionModel().selectFirst();
    	sNature.getStyleClass().add("error-combobox");
    	sCredit.getStyleClass().add("error-combobox");
    	sCredit.getSelectionModel().selectFirst();
    	COURSENAME.setStyle("-fx-text-fill: #116ecd; -fx-alignment: center; -fx-font-size: 14px; -fx-font-weight: 500;");
    	NikoButton.getStyleClass().add("stage-button");
    	RefuseButton.getStyleClass().add("my-button");
    	courseID.getStyleClass().add("textField");
    	hbox.getStyleClass().add("table-hbox");
    	CloseButton.getStyleClass().add("my-button");
    	NikoButton.setMinWidth(50);
    	RefuseButton.setMinWidth(50);
    	CloseButton.setMinWidth(50);
    	
    	NikoButton.setOnAction(e->{
    		if(courseID.getText().isEmpty()) {
    			showErrorDialog("课程ID不能为空！");
    		}
    		else {
    			String SNature = sNature.getValue();
    			String CourseID = courseID.getText();
    			double SCredit = sCredit.getValue();
    			AppliedCourseItem thisItem = courseApplied.getSelectionModel().getSelectedItem();
    			TestCourse coursePubliced = new TestCourse(thisItem.getCName(),thisItem.getCTeacher(),SCredit,
    					thisItem.getCAcademy(),thisItem.getCMajor(),thisItem.getCFirst(),thisItem.getCLast(),thisItem.getSType(),
    					thisItem.getCNum(),CourseID,SNature,thisItem.getCDepartment());
    			
    			new Thread(() -> {
    	            try {
    	                // 使用SocketManager的out流发送登录请求
    	                DataOutputStream out = SocketManager.getInstance().getOut();
    	                
    	                out.writeUTF("CoursePublicAgree");
    	                
    	                String jsonReq = gson.toJson(coursePubliced);
    	                out.writeUTF(jsonReq);
    	                out.flush();

    	                // 使用SocketManager的in流接收响应
    	                DataInputStream in = SocketManager.getInstance().getIn();
    	                
    	                String Response = in.readUTF();
    	                
    	                Platform.runLater(() -> {
    	                    showOkDialog(Response); // 现在在正确的线程中显示对话框
    	                });
    	                
    	                if(courseApplied.getSelectionModel().getSelectedItem() != null)
    	                courseApplied.getItems().remove(courseApplied.getSelectionModel().getSelectedItem() );

    	            } catch (Exception ex) {
    	                Platform.runLater(() ->
    	                	showErrorDialog("请检查服务器是否启动或网络是否正常：" + ex.getMessage())
    	                );
    	                ex.printStackTrace();
    	            }
    	        }).start();
    			
    			STAGE.close();
    		}
    	});
    	
    	RefuseButton.setOnAction(e->{
    		String SNature = sNature.getValue();
			String CourseID = courseID.getText();
			AppliedCourseItem thisItem = courseApplied.getSelectionModel().getSelectedItem();
			TestCourse coursePubliced = new TestCourse(thisItem.getCName(),thisItem.getCTeacher(),thisItem.getCCredit(),
					thisItem.getCAcademy(),thisItem.getCMajor(),thisItem.getCFirst(),thisItem.getCLast(),thisItem.getSType(),
					thisItem.getCNum(),CourseID,SNature,thisItem.getCDepartment());
			
    		new Thread(() -> {
	            try {
	                // 使用SocketManager的out流发送登录请求
	                DataOutputStream out = SocketManager.getInstance().getOut();
	                
	                out.writeUTF("CoursePublicDisagree");
	                
	                String jsonReq = gson.toJson(coursePubliced);
	                out.writeUTF(jsonReq);
	                out.flush();

	                // 使用SocketManager的in流接收响应
	                DataInputStream in = SocketManager.getInstance().getIn();
	                
	                String Response = in.readUTF();
	                
	                Platform.runLater(() -> {
	                    showOkDialog(Response); // 现在在正确的线程中显示对话框
	                });
	                
	                if(courseApplied.getSelectionModel().getSelectedItem() != null)
    	                courseApplied.getItems().remove(courseApplied.getSelectionModel().getSelectedItem() );

	            } catch (Exception ex) {
	                Platform.runLater(() ->
	                	showErrorDialog("请检查服务器是否启动或网络是否正常：" + ex.getMessage())
	                );
	                ex.printStackTrace();
	            }
	        }).start();
    		
    		STAGE.close();
    	});
    	
    	CloseButton.setOnAction(e->STAGE.close());
    	
    	return new VBox(20,COURSENAME,hbox);
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
        
        errorStage.setX(780);
        errorStage.setY(512);
        
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
        
        errorStage.setX(780);
        errorStage.setY(512);
        
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
    
    private AppliedCourseItem convertToAppliedCourseItem(TestCourse course) {
        return new AppliedCourseItem(
            course.getName(),                // 课程名
            course.getCredit(),              // 学分
            course.getTeacherName(),         // 教师名
            course.getDepartment(),          // 开课单位
            course.getStudentType(),         // 学生类型
            course.getAcademy(),             // 面向学院
            course.getMajor(),               // 面向专业
            course.getFirstWeek(),           // 第一周
            course.getLastWeek(),            // 最后一周
            course.getStudentNum()           // 授课人数
        );
    }
    
    @FXML
    void OnRefeshClick(ActionEvent event) {
    	new Thread(() -> {
            try {
                // 使用SocketManager的out流发送登录请求
                DataOutputStream out = SocketManager.getInstance().getOut();
                
                out.writeUTF("CourseAppliedRequire");
                
                // 使用SocketManager的in流接收响应
                DataInputStream in = SocketManager.getInstance().getIn();
                
                String jsonData = in.readUTF();
                Type type = new TypeToken<List<TestCourse>>(){}.getType(); // 泛型类型
                courseList = gson.fromJson(jsonData, type);
                
                Platform.runLater(() -> {
                    // 3. 将List<TestCourse>转换为ObservableList<AppliedCourseItem>
                    ObservableList<AppliedCourseItem> tableData = FXCollections.observableArrayList();
                    for (TestCourse course : courseList) {
                        tableData.add(convertToAppliedCourseItem(course));
                    }
                    
                    courseApplied.getItems().clear();
                    courseApplied.setItems(tableData);
                    initComboBoxes();
                });
                
            } catch (Exception e) {
                Platform.runLater(() ->
                	showErrorDialog("请检查服务器是否启动或网络是否正常：" + e.getMessage())
                );
                e.printStackTrace();
            }
        }).start();
    }
    
    //筛选功能///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    
    private void addFilterListeners() {
        courseNature.setOnAction(e -> filterCourses());
        credit.setOnAction(e -> filterCourses());
        department.setOnAction(e -> filterCourses());
        studentType.setOnAction(e -> filterCourses());
    }
    
    private void initComboBoxes() {
        //课程性质 - 从courseList提取不重复的性质
        List<String> teachers = courseList.stream()
                .map(TestCourse::getTeacherName)
                .distinct()
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        // 添加"全部"选项
        teachers.add(0, "全部");
        courseNature.setItems(FXCollections.observableArrayList(teachers));
        courseNature.getSelectionModel().selectFirst();
        
        //学分
        List<Double> credits = courseList.stream()
                .map(TestCourse::getCredit)
                .distinct()
                .collect(Collectors.toList());
        credits.add(0, -1.0);  // 用-1代表全部
        credit.setItems(FXCollections.observableArrayList(credits));
        credit.getSelectionModel().selectFirst();
        
        //开课单位
        List<String> departments = courseList.stream()
                .map(TestCourse::getDepartment)
                .distinct()
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        departments.add(0, "全部");
        department.setItems(FXCollections.observableArrayList(departments));
        department.getSelectionModel().selectFirst();
        
        //授课对象
        List<String> studentTypes = courseList.stream()
                .map(course -> {
                    switch (course.getStudentType()) {
                        case 1: return "大一学生";
                        case 2: return "大二学生";
                        case 3: return "大三学生";
                        case 4: return "大四学生";
                        default: return "未知类型";
                    }
                })
                .distinct()
                .collect(Collectors.toList());
        studentTypes.add(0, "全部");
        studentType.setItems(FXCollections.observableArrayList(studentTypes));
        studentType.getSelectionModel().selectFirst();
    }
    
    private void filterCourses() {
    	String selectedTeacher = courseNature.getValue();
        Double selectedCredit = credit.getValue();
        String selectedDept = department.getValue();
        String selectedStuType = studentType.getValue();
        
        ObservableList<AppliedCourseItem> tableData = FXCollections.observableArrayList();
        for (TestCourse course : courseList) {
            tableData.add(convertToAppliedCourseItem(course));
        }
        
        ObservableList<AppliedCourseItem> filtered = tableData.filtered(course -> {
            // 课程性质筛选
            if (selectedTeacher != null && !"全部".equals(selectedTeacher) && !selectedTeacher.equals(course.getCTeacher())) {
                return false;
            }
            
            // 学分筛选
            if (selectedCredit != null && selectedCredit != -1.0 && course.getCCredit() != selectedCredit) {
                return false;
            }
            
            // 开课单位筛选
            if (selectedDept != null && !"全部".equals(selectedDept) && !selectedDept.equals(course.getCDepartment())) {
                return false;
            }
            
            // 授课对象筛选
            if (selectedStuType != null && !"全部".equals(selectedStuType) && !selectedStuType.equals(course.getSTypeS())) {
                return false;
            }
            
            return true;
        });
        
        ObservableList<AppliedCourseItem> Realfiltered = FXCollections.observableArrayList(filtered);
        
        // 更新表格数据
        courseApplied.setItems(Realfiltered);
    }
    
}















