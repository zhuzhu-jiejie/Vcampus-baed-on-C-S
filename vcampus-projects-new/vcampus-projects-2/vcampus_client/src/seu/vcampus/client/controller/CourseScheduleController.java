package seu.vcampus.client.controller;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.lang.reflect.Type;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

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
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;
import seu.vcampus.client.network.SocketManager;
import seu.vcampus.model.CourseTime;
import seu.vcampus.model.TestCourse;

public class CourseScheduleController implements Initializable {

	private Integer currentSelectedWeekday = null;
	private Integer currentSelectedStart = null;
	
	private Set<Integer> courseScheduledWeekdays = new HashSet<>();
	
	private List<String> allClassrooms = Arrays.asList("J1-101", "J1-102", "J1-103", "J1-104", "J1-105",
											     		"J1-201", "J1-202", "J1-203", "J1-204", "J1-205");
	
	private static final Gson gson = new Gson();
	
	private List<TestCourse> courseList = new ArrayList<>();
	
	private List<CourseTime> TimeList = new ArrayList<>();

	@FXML
    private TableView<PublicedCourseItem> coursePubliced;
	
    @FXML
    private TableColumn<PublicedCourseItem, Integer> numColumn;

    @FXML
    private TableColumn<PublicedCourseItem, String> weekColumn;

    @FXML
    private ComboBox<String> academyBox;

    @FXML
    private TableColumn<PublicedCourseItem, String> nameColumn;

    @FXML
    private TableColumn<PublicedCourseItem, String> academyColumn;

    @FXML
    private TableColumn<PublicedCourseItem, String> majorColumn;

    @FXML
    private TableColumn<PublicedCourseItem, String> teacherColumn;

    @FXML
    private Button refresh;

    @FXML
    private ComboBox<String> natureBox;

    @FXML
    private ComboBox<Integer> numBox;

    @FXML
    private ComboBox<String> majorBox;
    
    //设置tableItem//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    
    public class PublicedCourseItem{
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
    	private final SimpleStringProperty cNature;
    	private final SimpleStringProperty cWeek;
    	private boolean expanded;
    	
    	public PublicedCourseItem(String CNAME, double CCREDIT, String CNATURE, String CDEPARTMENT, int STYPE, String ACADEMY,
    			String MAJOR, int FIRST, int LAST, int NUM, String NATURE) {
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
    		this.cNature = new SimpleStringProperty(NATURE); 
    		this.cWeek = new SimpleStringProperty(FIRST+"-"+LAST+"周"); 
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
     	public String getCNature() {return cNature.get();}
     	public String getCWeek() {return cWeek.get();}
    	public boolean isExpanded() {return expanded;};
    	public void setExpanded(boolean expanded) {this.expanded = expanded;}
    	@Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            PublicedCourseItem that = (PublicedCourseItem) o;
            return Objects.equals(getCName(), that.getCName()) &&
                   Objects.equals(getCTeacher(), that.getCTeacher());
        }

        @Override
        public int hashCode() {
            return Objects.hash(getCName(), getCTeacher());
        }
    }
    
    private PublicedCourseItem convertToPublicedCourseItem(TestCourse course) {
        return new PublicedCourseItem(
            course.getName(),         
            course.getCredit(),             
            course.getTeacherName(),   
            course.getDepartment(),        
            course.getStudentType(),      
            course.getAcademy(),      
            course.getMajor(),           
            course.getFirstWeek(),        
            course.getLastWeek(),           
            course.getStudentNum(),         
            course.getNature()
        );
    }
    
    //初始化/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    
    @Override
    public void initialize(URL location, ResourceBundle resources) {
    	getData();
    	initTableColumns();
    	coursePubliced.setOnMouseClicked(event -> handleTableClick(event));
    	addFilterListeners();
    }
    
    //获取数据///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    
    private void getData() {
    	new Thread(() -> {
            try {
                // 使用SocketManager的out流发送登录请求
                DataOutputStream out = SocketManager.getInstance().getOut();
                
                out.writeUTF("CoursePublicedRequire");
                out.flush();
                
                // 使用SocketManager的in流接收响应
                DataInputStream in = SocketManager.getInstance().getIn();
                
                String jsonData = in.readUTF();
                Type type = new TypeToken<List<TestCourse>>(){}.getType(); // 泛型类型
                courseList = gson.fromJson(jsonData, type);
                
                Platform.runLater(() -> {
                    ObservableList<PublicedCourseItem> tableData = FXCollections.observableArrayList();
                    for (TestCourse course : courseList) {
                        tableData.add(convertToPublicedCourseItem(course));
                    }
                    
                    coursePubliced.setItems(tableData);
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
    
    //列表设置///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    
    private void initTableColumns() {
    	nameColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getCName()));
    	academyColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getCAcademy()));
    	majorColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getCMajor()));
    	weekColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getCWeek()));
    	teacherColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getCTeacher()));
    	numColumn.setCellValueFactory(cellData -> new SimpleIntegerProperty(cellData.getValue().getCNum()).asObject());
    	nameColumn.setSortable(false);
    }
    
    private void handleTableClick(MouseEvent event) {
    	if (coursePubliced.getSelectionModel().getSelectedItem() == null) {
            return;
        }
    	else if (event.getClickCount() == 2) {
    		showSetBox();
        }
    }
    
    private void showSetBox() {
   	 	Stage SetBox = new Stage();
   	 
   	 	SetBox.initStyle(StageStyle.UNDECORATED);
   	 	SetBox.initModality(Modality.APPLICATION_MODAL);
   	 	SetBox.initStyle(StageStyle.TRANSPARENT);
   	  
   	 	VBox root = CreatSetBox(SetBox);
   	 	Scene scene = new Scene(root);
   	 	scene.setFill(Color.TRANSPARENT); 
   	 	scene.getStylesheets().add(getClass().getResource("/seu/vcampus/client/css/edu_admin.css").toExternalForm());
   	 	root.setStyle("-fx-padding: 20px; -fx-alignment: CENTER; -fx-min-width: 300px;-fx-background-color: #fdfdff; -fx-background-radius: 5px; -fx-border-color: #116ecc; -fx-border-width: 2px; -fx-border-radius: 5px;");
   	  
   	 	SetBox.setX(770);
   	 	SetBox.setY(270);
         
        root.setScaleX(0.5);
        root.setScaleY(0.5);
        ScaleTransition scaleIn = new ScaleTransition(Duration.millis(100), root);
        scaleIn.setFromX(0.5);
        scaleIn.setFromY(0.5);
        scaleIn.setToX(1);   // 恢复到100%大小
        scaleIn.setToY(1);
        scaleIn.play();

        SetBox.setScene(scene);
        SetBox.showAndWait(); 
   }
    
    private VBox CreatSetBox(Stage STAGE) {
    	
    	PublicedCourseItem selectedCourse = coursePubliced.getSelectionModel().getSelectedItem();
        String targetCourseName = selectedCourse.getCName();
        String targetTeacherName = selectedCourse.getCTeacher();
        
    	new Thread(() -> {
            try {
                // 使用SocketManager的out流发送登录请求
                DataOutputStream out = SocketManager.getInstance().getOut();
                
                out.writeUTF("CourseScheduledRequire");
                
                // 使用SocketManager的in流接收响应
                DataInputStream in = SocketManager.getInstance().getIn();
                
                String jsonData = in.readUTF();
                Type type = new TypeToken<List<CourseTime>>(){}.getType(); // 泛型类型
                TimeList = gson.fromJson(jsonData, type);
                
            } catch (Exception e) {
                Platform.runLater(() ->
                	showErrorDialog("请检查服务器是否启动或网络是否正常：" + e.getMessage())
                );
                e.printStackTrace();
            }
        }).start();
    	
    	Label COURSENAME = new Label(coursePubliced.getSelectionModel().getSelectedItem().getCName());
    	COURSENAME.getStyleClass().add("my-label");
    	COURSENAME.setStyle("-fx-alignment: CENTER; -fx-text-fill: #116ecc;  -fx-font-size: 14px; -fx-font-weight: 500; -fx-alignment: center; ");
    	
    	TextField classID = new TextField();
    	classID.setPromptText("设置教学班编号");
    	classID.getStyleClass().add("textField");
    	
    	ComboBox<String> week = new ComboBox<>();
    	week.getStyleClass().add("error-combobox");
    	week.setStyle("-fx-min-width: 130px;");
    	initWeekBox(week, targetCourseName);
    	Label weekLabel = new Label("星期");
    	weekLabel.getStyleClass().add("my-label");
    	HBox weekHbox = new HBox(5,weekLabel,week);
    	weekHbox.getStyleClass().add("white-background");
    	weekHbox.setStyle("-fx-alignment: CENTER_LEFT;");
    	
    	Label startLabel = new Label("开始节次");
    	startLabel.getStyleClass().add("my-label");
    	ComboBox<Integer> startBox = new ComboBox<>();
    	startBox.getStyleClass().add("error-combobox");
    	startBox.setStyle("-fx-min-width: 105px;");
    	HBox startHbox = new HBox(5,startLabel,startBox);
    	startHbox.getStyleClass().add("white-background");
    	startHbox.setStyle("-fx-alignment: CENTER_LEFT;");
    	
    	Label endLabel = new Label("结束节次");
    	endLabel.getStyleClass().add("my-label");
    	ComboBox<Integer> endBox = new ComboBox<>();
    	endBox.getStyleClass().add("error-combobox");
    	endBox.setStyle("-fx-min-width: 105px;");
    	HBox endHbox = new HBox(5,endLabel,endBox);
    	endHbox.getStyleClass().add("white-background");
    	endHbox.setStyle("-fx-alignment: CENTER_LEFT;");
    	
    	Label classLabel = new Label("教室");
    	classLabel.getStyleClass().add("my-label");
    	ComboBox<String> classBox = new ComboBox<>();
    	classBox.getStyleClass().add("error-combobox");
    	classBox.setStyle("-fx-min-width: 130px;");
    	HBox classHbox = new HBox(5,classLabel,classBox);
    	classHbox.getStyleClass().add("white-background");
    	classHbox.setStyle("-fx-alignment: CENTER_LEFT;");
    	
    	VBox Detial = new VBox(10,weekHbox,startHbox,endHbox,classHbox);
    	Detial.getStyleClass().add("card-background");
    	Detial.setStyle("-fx-padding: 10px;");
    	
    	CheckBox otherClass = new CheckBox("存在其他教学班");
    	otherClass.getStyleClass().add("my-check");
    	
    	VBox Total = new VBox(10,classID,Detial,otherClass);
    	Total.getStyleClass().add("blue-background");
    	Total.setStyle("-fx-padding: 10px; -fx-alignment: CENTER; -fx-border-color: #3d96ef;");
    	
    	Button SubButton = new Button("提交");
    	SubButton.getStyleClass().add("big-button");
    	Button OkButton = new Button("确认");
    	OkButton.getStyleClass().add("big-button");
    	Button CancelButton = new Button("取消");
    	CancelButton.getStyleClass().add("my-button");
    	
    	HBox BtnBox = new HBox(10,SubButton,OkButton,CancelButton);
    	BtnBox.getStyleClass().add("blue-background");
    	BtnBox.setStyle("-fx-padding: 10px; -fx-alignment: CENTER; -fx-border-color: #3d96ef;");
    	
    	VBox Final = new VBox(10,COURSENAME,Total,BtnBox);
    	Final.setStyle("-fx-padding: 20px; -fx-alignment: CENTER; -fx-min-width: 300px;-fx-background-color: #fdfdff; -fx-background-radius: 5px; -fx-border-color: #116ecc; -fx-border-width: 2px; -fx-border-radius: 5px;-fx-padding:10px;");
    	
    	startBox.getItems().clear();
        endBox.getItems().clear();
        classBox.getItems().clear();
        startBox.setDisable(true);
        endBox.setDisable(true);
        classBox.setDisable(true);
    	
    	week.setOnAction(e -> {
            String weekStr = week.getValue();
            if (weekStr == null) {
                startBox.getItems().clear();
                endBox.getItems().clear();
                classBox.getItems().clear();
                endBox.setDisable(true);
                classBox.setDisable(true);
                currentSelectedWeekday = null;
                currentSelectedStart = null;
                return;
            }
            // 转换星期字符串为数字（如"周一"→1，"周二"→2...）
            currentSelectedWeekday = convertWeekStrToNum(weekStr);
            // 初始化开始节次（过滤当前日期已占用的节次）
            initStartBox(startBox, currentSelectedWeekday, targetTeacherName);
            // 重置后续下拉框
            startBox.setDisable(false);
            endBox.getItems().clear();
            classBox.getItems().clear();
            endBox.setDisable(true);
            classBox.setDisable(true);
            currentSelectedStart = null;
        });
    	
    	 startBox.setOnAction(e -> {
    	        currentSelectedStart = startBox.getValue();
    	        if (currentSelectedStart == null || currentSelectedWeekday == null) {
    	            endBox.getItems().clear();
    	            classBox.getItems().clear();
    	            endBox.setDisable(true);
    	            classBox.setDisable(true);
    	            return;
    	        }
    	        // 初始化结束节次（仅显示合法节次）
    	        initEndBox(endBox, currentSelectedWeekday, currentSelectedStart, targetTeacherName);
    	        endBox.setDisable(false);
    	        // 重置教室下拉框
    	        classBox.getItems().clear();
    	        classBox.setDisable(true);
    	 });
    	 
    	 endBox.setOnAction(e -> {
    	        Integer selectedEnd = endBox.getValue();
    	        if (selectedEnd == null || currentSelectedWeekday == null || currentSelectedStart == null) {
    	            classBox.getItems().clear();
    	            classBox.setDisable(true);
    	            return;
    	        }
    	        // 初始化教室（过滤当前时段已占用的教室）
    	        initClassBox(classBox, currentSelectedWeekday.intValue(), currentSelectedStart.intValue(), selectedEnd.intValue());
    	        classBox.setDisable(false);
    	 });
    	 
    	 SubButton.setOnAction(e -> {
    		 
    		String tip;
    		if(classID.getText().isEmpty()) {
    			tip = "教学班编号不能为空！";
    			showErrorDialog(tip);
    		}
    		else if(week.getValue() == null) {
    			tip = "星期不能为空！";
    			showErrorDialog(tip);
    		}
    		else if(startBox.getValue() == null) {
    			tip = "开始节次不能为空！";
    			showErrorDialog(tip);
    		}
    		else if(endBox.getValue() == null) {
    			tip = "结束节次不能为空！";
    			showErrorDialog(tip);
    		}
    		else if(classBox.getValue() == null) {
    			tip = "教室不能为空！";
    			showErrorDialog(tip);
    		}
    		else {
    			
    			int CLASSID = Integer.parseInt(classID.getText());
    			String WEEKS = week.getValue();
    			int WEEK;
    			switch(WEEKS) {
    			case "星期一":
    				WEEK = 1;
    				break;
    			case "星期二":
    				WEEK = 2;
    				break;
    			case "星期三":
    				WEEK = 3;
    				break;
    			case "星期四":
    				WEEK = 4;
    				break;
    			case "星期五":
    				WEEK = 5;
    				break;
    			case "星期六":
    				WEEK = 6;
    				break;
    			 default:
                     WEEK = 0;
                     break;
    			}
    			int STARTBOX = startBox.getValue();
    			int ENDBOX = endBox.getValue();
    			String CLASSBOX = classBox.getValue();
    			
    			final AtomicReference<String> rsRef = new AtomicReference<>();
    			
    			new Thread(() -> {
    				try {
    	                // 使用SocketManager的out流发送登录请求
    	                DataOutputStream out = SocketManager.getInstance().getOut();
    	                out.writeUTF("TimeSubmit");
    	                
    	                PublicedCourseItem selectedItem = coursePubliced.getSelectionModel().getSelectedItem();
    	                CourseTime NewTime = new CourseTime(selectedItem.getCName(),selectedItem.getCTeacher(),CLASSID,WEEK,STARTBOX,ENDBOX,CLASSBOX);
    	                
    	                String jsonTime = gson.toJson(NewTime);
    	                out.writeUTF(jsonTime);
    	                out.flush();
    	                
    	                DataInputStream in = SocketManager.getInstance().getIn();
    	                String Response = in.readUTF();
    	                Platform.runLater(() -> {
    	                    showOkDialog(Response); // 现在在正确的线程中显示对话框
    	                });
    	                if(Response.equals("已提交！")) {
    	                	try {
        		                
        		                out.writeUTF("CourseScheduledRequire");
        		                out.flush();
        		               
        		                String jsonData = in.readUTF();
        		                Type type = new TypeToken<List<CourseTime>>(){}.getType(); // 泛型类型
        		                TimeList = gson.fromJson(jsonData, type);
        		                
        		            } catch (Exception exx) {
        		                Platform.runLater(() ->
        		                	showErrorDialog("请检查服务器是否启动或网络是否正常：" + exx.getMessage())
        		                );
        		                exx.printStackTrace();
        		            }
    	                	initWeekBox(week, targetCourseName);
    	                	 Platform.runLater(() -> {
    	    				startBox.getItems().clear();
    	    		        endBox.getItems().clear();
    	    		        classBox.getItems().clear();
    	    		        startBox.setDisable(true);
    	    		        endBox.setDisable(true);
    	    		        classBox.setDisable(true);
    	                	 });
    	                }
    	            } catch (Exception ex) {
    	                Platform.runLater(() ->
    	                	showErrorDialog("请检查服务器是否启动或网络是否正常：" + ex.getMessage())
    	                );
    	                ex.printStackTrace();
    	            }
    		 	}).start();
    		}
    	 });
    	 
    	 OkButton.setOnAction(e->{
    		 
    		 PublicedCourseItem selectedItem = coursePubliced.getSelectionModel().getSelectedItem();
    		 
    		 if(!otherClass.isSelected()) {
    			 
    			 final AtomicReference<String> rsRef = new AtomicReference<>();
    			 
    			 new Thread(() -> {
    				 try {
    					 // 使用SocketManager的out流发送登录请求
    					 DataOutputStream out = SocketManager.getInstance().getOut();
    					 out.writeUTF("TimeOk");
    					 out.writeUTF(selectedItem.getCName());
    					 out.writeUTF(selectedItem.getCTeacher());
    					 out.flush();
 	                
 	                	DataInputStream in = SocketManager.getInstance().getIn();
 	                	String Response = in.readUTF();
 	                	Platform.runLater(() -> {
 	                		showOkDialog(Response); // 现在在正确的线程中显示对话框
 	                	});
 	                	if(Response.equals("已完成排课！")) {
 	                		try {
 	        					 out.writeUTF("SetIsSchedule");
 	        					 out.writeUTF(selectedItem.getCName());
 	        					 out.writeUTF(selectedItem.getCTeacher());
 	        					 out.flush();
 	        				 } catch (Exception ex) {
 	        					 Platform.runLater(() ->
 	        					 	showErrorDialog("请检查服务器是否启动或网络是否正常：" + ex.getMessage())
 	        							 );
 	        					 ex.printStackTrace();
 	        				 }
 	                		try {
        		                
        		                out.writeUTF("CourseScheduledRequire");
        		                out.flush();
        		               
        		                String jsonData = in.readUTF();
        		                Type type = new TypeToken<List<CourseTime>>(){}.getType(); // 泛型类型
        		                TimeList = gson.fromJson(jsonData, type);
        		                refresh.fire();
        		                
        		            } catch (Exception exx) {
        		                Platform.runLater(() ->
        		                	showErrorDialog("请检查服务器是否启动或网络是否正常：" + exx.getMessage())
        		                );
        		                exx.printStackTrace();
        		            }
 	                		Platform.runLater(() -> {
 	                			STAGE.close();
 	                		});
 	                	}
    				 } catch (Exception ex) {
    					 Platform.runLater(() ->
    					 	showErrorDialog("请检查服务器是否启动或网络是否正常：" + ex.getMessage())
    							 );
    					 ex.printStackTrace();
    				 }
    			 }).start();
    		 }
    		 else if(otherClass.isSelected()) {
    			 Platform.runLater(() -> {
    				 STAGE.close();
    			 });
    		 }
    	 });

    	 CancelButton.setOnAction(e ->STAGE.close());
    	
    	 return Final;
    }
    
    private void initWeekBox(ComboBox<String> weekBox, String courseName) {
        Map<Integer, String> weekMap = new LinkedHashMap<>();
        weekMap.put(1, "星期一");
        weekMap.put(2, "星期二");
        weekMap.put(3, "星期三");
        weekMap.put(4, "星期四");
        weekMap.put(5, "星期五");
        weekMap.put(6, "星期六");

        ObservableList<String> availableWeeks = FXCollections.observableArrayList();
        for (Map.Entry<Integer, String> entry : weekMap.entrySet()) {
                availableWeeks.add(entry.getValue());
        }
        Platform.runLater(() -> {
        	weekBox.setItems(availableWeeks);
        });
        if (availableWeeks.isEmpty()) {
            weekBox.setPromptText("当前课程已排满所有日期");
            weekBox.setDisable(true);
        }
    }
    
    private void initStartBox(ComboBox<Integer> startBox, int weekday, String teacherName) {
        // 1. 所有可能的节次（1-13）
        List<Integer> allSections = IntStream.rangeClosed(1, 13).boxed().collect(Collectors.toList());
        // 2. 过滤已占用的节次（教师/教室在当前日期占用的节次）
        Set<Integer> occupiedSections = new HashSet<>();
        for (CourseTime sc : TimeList) {
            if (sc.getWeekDay() == weekday) {
                // 规则1：同一教师在当前日期的所有节次都占用
                if (sc.getTeacherName().equals(teacherName)) {
                    for (int s = sc.getStartTime(); s <= sc.getEndTime(); s++) {
                        occupiedSections.add(s);
                    }
                }
            }
        }

        // 3. 筛选可用节次（排除已占用）
        ObservableList<Integer> availableStarts = FXCollections.observableArrayList();
        for (int s : allSections) {
            if (!occupiedSections.contains(s)) {
                availableStarts.add(s);
            }
        }

        // 4. 设置下拉框选项
        startBox.setItems(availableStarts);
        if (availableStarts.isEmpty()) {
            startBox.setPromptText("无可用节次");
            startBox.setDisable(true);
        } else {
        	startBox.setPromptText("");
            startBox.setDisable(false);
        }
    }
    
    private void initEndBox(ComboBox<Integer> endBox, int weekday, int start, String teacherName) {
        // 1. 所有可能的结束节次（大于start，且≤13）
        List<Integer> possibleEnds = IntStream.rangeClosed(start, 13).boxed().collect(Collectors.toList());
        // 2. 过滤冲突节次（结束节次需满足：[start, end]不与任何已占用时段重叠）
        List<Integer> availableEnds = new ArrayList<>();
        for (int end : possibleEnds) {
            boolean isConflict = false;
            // 检查与已排课的冲突
            for (CourseTime sc : TimeList) {
                if (sc.getWeekDay() == weekday) {
                	if(teacherName == sc.getTeacherName()) {
                		// 冲突条件：当前时段[start,end]与已排时段[scStart,scEnd]有重叠
                        boolean timeOverlap = !(end < sc.getStartTime() || start > sc.getEndTime());
                        // 冲突场景：同一教师 或 同一教室 存在时间重叠
                        if (timeOverlap && (sc.getTeacherName().equals(teacherName) || true)) {
                            isConflict = true;
                            break;
                        }
                	}
                }
            }
            if (!isConflict) {
                availableEnds.add(end);
            }
        }

        // 3. 设置下拉框选项
        endBox.setItems(FXCollections.observableArrayList(availableEnds));
        if (availableEnds.isEmpty()) {
            endBox.setPromptText("无合法结束节次");
            endBox.setDisable(true);
        } else {
            endBox.setDisable(false);
        }
    }
    
    private void initClassBox(ComboBox<String> classBox, int weekday, int start, int end) {
        // 1. 过滤已占用的教室（当前时段[start,end]被使用的教室）
        Set<String> occupiedClassrooms = new HashSet<>();
        for (CourseTime sc : TimeList) {
            if (sc.getWeekDay() == weekday) {
                // 时间重叠：当前时段与已排时段有交集
                boolean timeOverlap = !(end < sc.getStartTime() || start > sc.getEndTime());
                if (timeOverlap) {
                    occupiedClassrooms.add(sc.getClassroom());
                }
            }
        }
        
        ObservableList<String> availableClassrooms = FXCollections.observableArrayList();
        for (String room : allClassrooms) {
            if (!occupiedClassrooms.contains(room)) {
                availableClassrooms.add(room);
            }
        }

        // 3. 设置下拉框选项
        classBox.setItems(availableClassrooms);
        if (availableClassrooms.isEmpty()) {
            classBox.setPromptText("当前时段无可用教室");
            classBox.setDisable(true);
        } else {
            classBox.setDisable(false);
        }
    }
    
    
    
    private Integer convertWeekStrToNum(String weekStr) {
        switch (weekStr) {
            case "星期一":
                return 1;
            case "星期二":
                return 2;
            case "星期三":
                return 3;
            case "星期四":
                return 4;
            case "星期五":
                return 5;
            case "星期六":
                return 6;
            default:
                return null;
        }
    }
    
    //Combo设置/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    
    private void addFilterListeners() {
        numBox.setOnAction(e -> filterCourses());
        natureBox.setOnAction(e -> filterCourses());
        academyBox.setOnAction(e -> filterCourses());
        majorBox.setOnAction(e -> filterCourses());
    }
    
    private void filterCourses() {
    	Integer courseNum = numBox.getValue();
    	String courseNature = natureBox.getValue();
    	String courseAcademy = academyBox.getValue();
    	String courseMajor = majorBox.getValue();
    	
    	ObservableList<PublicedCourseItem> tableData = FXCollections.observableArrayList();
        for (TestCourse course : courseList) {
            tableData.add(convertToPublicedCourseItem(course));
        }
        
        ObservableList<PublicedCourseItem> filtered = tableData.filtered(course->{
        	if (courseNum != null && courseNum != -1 && courseNum != course.getCNum()) {
                return false;
            }
        	
        	if (courseNature != null && !"全部".equals(courseNature) && !courseNature.equals(course.getCNature())) {
                return false;
            }
        	
        	if (courseAcademy != null && !"全部".equals(courseAcademy) && !courseAcademy.equals(course.getCAcademy())) {
                return false;
            }
        	
        	if (courseMajor != null && !"全部".equals(courseMajor) && !courseMajor.equals(course.getCMajor())) {
                return false;
            }
        	
        	return true;
        });
        
        ObservableList<PublicedCourseItem> Realfiltered = FXCollections.observableArrayList(filtered);
        
        coursePubliced.setItems(Realfiltered);
    }
    
    private void initComboBoxes() {
    	List<String> natures = courseList.stream()
                .map(TestCourse::getNature)
                .distinct()
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        // 添加"全部"选项
        natures.add(0, "全部");
        natureBox.setItems(FXCollections.observableArrayList(natures));
        natureBox.getSelectionModel().selectFirst();
        
        List<Integer> nums = courseList.stream()
                .map(TestCourse::getStudentNum)
                .distinct()
                .collect(Collectors.toList());
        nums.add(0, -1);  // 用-1代表全部
        numBox.setItems(FXCollections.observableArrayList(nums));
        numBox.getSelectionModel().selectFirst();
        
        List<String> academies = courseList.stream()
                .map(TestCourse::getAcademy)
                .distinct()
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        // 添加"全部"选项
        academies.add(0, "全部");
        academyBox.setItems(FXCollections.observableArrayList(academies));
        academyBox.getSelectionModel().selectFirst();
        
        List<String> majors = courseList.stream()
                .map(TestCourse::getMajor)
                .distinct()
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        // 添加"全部"选项
        majors.add(0, "全部");
        majorBox.setItems(FXCollections.observableArrayList(majors));
        majorBox.getSelectionModel().selectFirst();
    }
    
    //刷新按钮///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    @FXML
    void OnRefreshClick(ActionEvent event) {
    	new Thread(() -> {
            try {
                // 使用SocketManager的out流发送登录请求
                DataOutputStream out = SocketManager.getInstance().getOut();
                
                out.writeUTF("CoursePublicedRequire");
                
                // 使用SocketManager的in流接收响应
                DataInputStream in = SocketManager.getInstance().getIn();
                
                String jsonData = in.readUTF();
                Type type = new TypeToken<List<TestCourse>>(){}.getType(); // 泛型类型
                courseList = gson.fromJson(jsonData, type);
                
                Platform.runLater(() -> {
                    ObservableList<PublicedCourseItem> tableData = FXCollections.observableArrayList();
                    for (TestCourse course : courseList) {
                        tableData.add(convertToPublicedCourseItem(course));
                    }
                    
                    coursePubliced.getItems().clear();
                    coursePubliced.setItems(tableData);
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
    
    //弹窗//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    
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
        
        errorStage.setX(770);
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
        
        errorStage.setX(770);
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

