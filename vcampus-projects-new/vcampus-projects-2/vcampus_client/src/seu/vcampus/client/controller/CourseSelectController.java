package seu.vcampus.client.controller;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.lang.reflect.Type;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

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
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TableRow;
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
import seu.vcampus.model.TestCourse;
import seu.vcampus.model.CourseTime;
import seu.vcampus.model.CourseToTime;
import seu.vcampus.model.FinalCourse;
import seu.vcampus.model.StudentSC;

public class CourseSelectController implements Initializable {
	
	private static final Gson gson = new Gson();
	
	private List<StudentSC> allScList = new ArrayList<>();
	
	private List<FinalCourse> courseList = new ArrayList<>();
	
	private List<StudentSC> studentList = new ArrayList<>();
	
	private List<CourseToTime> sTimeList = new ArrayList<>();
	
	private List<CourseToTime> cttList = new ArrayList<>();

    @FXML
    private TableColumn<SelectedCourseItem, String> courseName;

    @FXML
    private Button search;

    @FXML
    private ComboBox<String> courseDepartment;

    @FXML
    private TableColumn<SelectedCourseItem, String> nature;

    @FXML
    private ComboBox<String> courseNature;

    @FXML
    private Button refresh;

    @FXML
    private TableColumn<SelectedCourseItem, Double> credit;

    @FXML
    private TableColumn<SelectedCourseItem, String> department;

    @FXML
    private TextField nameText;

    @FXML
    private TableColumn<SelectedCourseItem, String> courseID;

    @FXML
    private TableView<SelectedCourseItem> course_scheduled;
    
    public String sId;
    
    public int sType;
    
    public String sMajor;
    
    private Map<String, Map<String, Map<Integer,MapItem>>> ctMap = new HashMap<>();
    
    private Map<String, FinalCourse> cfMap = new HashMap<>();
    
    private Map<CourseIndex, List<TimeClass>> timeMap = new HashMap<>();
    
    private Map<CourseIndex, Integer> numMap = new HashMap<>();
    
    private Map<String, List<CourseIndex>> nameMap = new HashMap<>();
    
    private Map<String, Integer> rNumMap = new HashMap<>();
    
    //表格Item类/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public void setUserId(String STUDENTID) {
    	sId = STUDENTID;
    	System.out.println(sId);
    	getInfo();
    	addFilterListeners();
    	initTableColumns();
    	course_scheduled.setOnMouseClicked(event -> handleTableClick(event));
    }
    
    public class SelectedCourseItem{
    	private final SimpleStringProperty courseID;
    	private final SimpleStringProperty courseName;
    	private final SimpleDoubleProperty credit;
    	private final SimpleStringProperty department;
    	private final SimpleStringProperty nature;
    	private boolean expanded;
    	private boolean selected;
    	private boolean conflict;
    	//private final SimpleIntegerProperty studentType;
    	//private final SimpleStringProperty academy;
    	//private final SimpleStringProperty major;
    	//private final SimpleIntegerProperty first;
    	//private final SimpleIntegerProperty last;
    	//private final SimpleIntegerProperty studentNum;
    	//private final SimpleStringProperty teacher;
    	//private final SimpleStringProperty schedule_info;
    	//private final SimpleIntegerProperty classID;
    	
    	public SelectedCourseItem(String COURSEID, String COURSENAME, double CREDIT, String TEACHER, String DEPARTMENT, int STUDENTTYPE, String ACADEMY, String MAJOR,
    			 int FIRST, int LAST, int STUDENTNUM, String NATURE, String SCHEDULE, int CLASSID) {
    		this.courseID = new SimpleStringProperty(COURSEID);
    		this.courseName = new SimpleStringProperty(COURSENAME);
    		this.credit = new SimpleDoubleProperty(CREDIT);
    		this.department = new SimpleStringProperty(DEPARTMENT);
    		this.nature = new SimpleStringProperty(NATURE);
     		//this.teacher = new SimpleStringProperty(TEACHER);
    		//this.academy = new SimpleStringProperty(ACADEMY);
    		//this.major = new SimpleStringProperty(MAJOR);
    		//this.schedule_info = new SimpleStringProperty(SCHEDULE);
    		//this.studentType = new SimpleIntegerProperty(STUDENTTYPE);
    		//this.first = new SimpleIntegerProperty(FIRST);
    		//this.last = new SimpleIntegerProperty(LAST);
    		//this.studentNum = new SimpleIntegerProperty(STUDENTNUM);
    		//this.classID = new SimpleIntegerProperty(CLASSID);
    		this.expanded = false;
    		this.selected = false;
    		this.conflict = false;
    	}
    	
    	public String getCourseID() {return courseID.get();}
    	public String getCourseName() {return courseName.get();}
    	public double getCredit() {return credit.get();}
    	public String getDepartment() {return department.get();}
     	public String getNature() {return nature.get();}
    	public boolean isExpanded() {return expanded;};
    	public void setExpanded(boolean expanded) {this.expanded = expanded;}
    	public boolean isSelected() {return selected;};
    	public void setSelected(boolean selected) {this.selected = selected;}
    	public boolean isConflict() {return conflict;}
    	public void setConflict(boolean conflict) {this.conflict = conflict;}
    	@Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            SelectedCourseItem that = (SelectedCourseItem) o;
            return Objects.equals(getCourseName(), that.getCourseName());
        }

        @Override
        public int hashCode() {
            return Objects.hash(getCourseName(), getCourseName());
        }
    	//public String getTeacher() {return teacher.get();}
     	//public String getSchedule() {return schedule_info.get();}
    	//public int getStudentType() {return studentType.get();}
    	//public int getClassID() {return classID.get();}
    	//public String getAcademy() {return academy.get();}
     	//public String getMajor() {return major.get();}
     	//public int getFirst() {return first.get();}
     	//public int getLast() {return last.get();}
     	//public int getStudentNum() {return studentNum.get();}
        /*public String getSringType() {
		switch (getStudentType()) {
        case 1: return "大一学生";
        case 2: return "大二学生";  
        case 3: return "大三学生";  
        case 4: return "大四学生";
        default: return "未知类型";
		}
	}*/
    }
    
    private SelectedCourseItem convertToSelectedCourseItem(FinalCourse course) {
        return new SelectedCourseItem(
        	course.getCourseID(),
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
            course.getNature(),
            course.getSchedule(),
            course.getClassID()
        );
    }
    
    //Map存储类//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    
    public class MapItem{
    	private String scheduleInfoMap;
    	private int studentNum;
    	
    	public MapItem(String SCH, int STU) {
    		this.scheduleInfoMap = SCH;
    		this.studentNum = STU;
    	}
    	
    	public String getS() {return this.scheduleInfoMap;}
    	public int getC() {return this.studentNum;}
    }
    
    public class CourseIndex{
    	private String courseName;
    	private String teacherName;
    	private int classId;
    	
    	public CourseIndex(String COURSENAME, String TEACHERNAME, int CLASSID) {
    		this.courseName = COURSENAME;
    		this.teacherName = TEACHERNAME;
    		this.classId = CLASSID;
    	}
    	
    	public String getCourseName() {return courseName;}
    	public String getTeacherName() {return teacherName;}
    	public int getClassId() {return classId;}
    	@Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            CourseIndex that = (CourseIndex) o;
            // 修正：正确比较三个属性，并用Objects.equals处理null
            return Objects.equals(courseName, that.courseName) &&
                   Objects.equals(teacherName, that.teacherName) &&
                   classId == that.classId;
        }
        
        @Override
        public int hashCode() {
        	return Objects.hash(courseName, teacherName, classId);
        }
    }
    
    public class TimeClass{
    	private int fWeek;
    	private int eWeek;
    	private int day;
    	private int fTime;
    	private int eTime;
    	
    	public TimeClass(int FWEEK, int EWEEK, int DAY, int FTIME, int ETIME) {
    		this.fWeek = FWEEK;
    		this.eWeek = EWEEK;
    		this.day = DAY;
    		this.fTime = FTIME;
    		this.eTime = ETIME;
    	}
    	
    	public int getFWeek() {return fWeek;}
    	public int getEWeek() {return eWeek;}
    	public int getDay() {return day;}
    	public int getFTime() {return fTime;}
    	public int getETime() {return eTime;}
    }
    
    //初始化/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    
    @Override
    public void initialize(URL location, ResourceBundle resources) {
    	
    }
    
    //获取数据///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    
    private void getInfo() {
    	new Thread(() -> {
            try {
                DataOutputStream out = SocketManager.getInstance().getOut();
                out.writeUTF("SelectCourseInfo");
                out.writeUTF(sId);
                out.flush();
                DataInputStream in = SocketManager.getInstance().getIn();
                sMajor = in.readUTF();
                sType = 2025 - in.readInt() + 1;
            } catch (Exception e) {
                Platform.runLater(() ->
                	showErrorDialog("请检查服务器是否启动或网络是否正常：" + e.getMessage())
                );
                e.printStackTrace();
            }
            getData();
        }).start();
    }
    
    private void getData() {
    	new Thread(() -> {
            try {
                DataOutputStream out = SocketManager.getInstance().getOut();
                out.writeUTF("DateToSelectRequire");
                out.writeUTF(sMajor);
                out.writeInt(sType);
                out.writeUTF(sId);
                out.flush();
                
                DataInputStream in = SocketManager.getInstance().getIn();
                
                String jsonData = in.readUTF();
                Type type = new TypeToken<List<FinalCourse>>(){}.getType(); // 泛型类型
                courseList = gson.fromJson(jsonData, type);
                String sData = in.readUTF();
                Type sType = new TypeToken<List<StudentSC>>() {}.getType();
                studentList = gson.fromJson(sData, sType);
                String scData = in.readUTF();
                String ctData = in.readUTF();
                Type ctType = new TypeToken<List<CourseToTime>>() {}.getType();
                cttList = gson.fromJson(ctData, ctType);
                sTimeList = gson.fromJson(scData, ctType);
                String allData = in.readUTF();
                allScList = gson.fromJson(allData, sType);
                
                course_scheduled.setRowFactory(tv -> new TableRow<SelectedCourseItem>() {
            		// 重写updateItem方法，定义行样式逻辑
            		@Override
            		protected void updateItem(SelectedCourseItem item, boolean empty) {
                        super.updateItem(item, empty); // 必须调用父类方法

                        // 清除之前的样式
                        getStyleClass().removeAll("selected-row", "conflict-row");

                        // 处理空行
                        if (empty || item == null) {
                            return;
                        }

                        // 根据item属性设置样式类
                        if (item.isSelected()) {
                            getStyleClass().add("selected-row");
                            setTextFill(Color.web("#0f64b8"));
                        } else if (item.isConflict()) {
                            getStyleClass().add("conflict-row");
                            setTextFill(Color.web("#ce2111"));
                        }
                    }
                	});
                
                initRNumMap();
                initNameMap();
                initNumMap();
                initTimeMap();
                initTableRow();
                initComboBoxes();
                
            } catch (Exception e) {
                Platform.runLater(() ->
                	showErrorDialog("请检查服务器是否启动或网络是否正常：" + e.getMessage())
                );
                e.printStackTrace();
            }
        }).start();
    }
    
    //列表设置///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    
    private void initTableRow(){
    	Platform.runLater(() -> {
            ObservableList<SelectedCourseItem> tableData = FXCollections.observableArrayList();
            for(FinalCourse course : courseList) {
            	String COURSENAME = course.getName();
            	cfMap.putIfAbsent(COURSENAME, course);
            }
            for (FinalCourse course : cfMap.values()) {
            	SelectedCourseItem currentItem = convertToSelectedCourseItem(course);
                for(StudentSC currentStudent : studentList) {
                	if(currentStudent.getCourseName().equals(course.getName())) {
                		currentItem.setSelected(true);
                	}
                	else if(noItem(currentItem.getCourseName())) {
                		currentItem.setConflict(true);
                	}
                }
                tableData.add(currentItem);
            }
            course_scheduled.getItems().clear();
            course_scheduled.setItems(tableData);
        });
    }
    
    private void initTableColumns() {
    	courseName.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getCourseName()));
    	courseID.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getCourseID()));
    	credit.setCellValueFactory(cellData -> new SimpleDoubleProperty(cellData.getValue().getCredit()).asObject());
    	nature.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getNature()));
    	department.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getDepartment()));
    	courseName.setSortable(false);
    	courseID.setSortable(false);
    }
    
    private void handleTableClick(MouseEvent event) {
    	if (course_scheduled.getSelectionModel().getSelectedItem() == null) {
            return;
        }
    	else if (event.getClickCount() == 2) {
    		SelectedCourseItem selectedItem = course_scheduled.getSelectionModel().getSelectedItem();
    		if(selectedItem.isSelected()) {
    			showCancelBox();
    		}
    		else if(!course_scheduled.getSelectionModel().getSelectedItem().isConflict()) {
    			showSelectBox();
    		}
    		else {
    			String tip = "课程冲突！";
    			showErrorDialog(tip);
    		}
        }
    }
    
    private void showSelectBox() {
   	 	Stage SetBox = new Stage();
   	 
   	 	SetBox.initStyle(StageStyle.UNDECORATED);
   	 	SetBox.initModality(Modality.APPLICATION_MODAL);
   	 	SetBox.initStyle(StageStyle.TRANSPARENT);
   	  
   	 	VBox root = CreatSelectBox(SetBox);
   	 	Scene scene = new Scene(root);
   	 	scene.setFill(Color.TRANSPARENT); 
   	 	scene.getStylesheets().add(getClass().getResource("/seu/vcampus/client/css/edu_admin.css").toExternalForm());
   	 	root.setStyle("-fx-padding: 20px; -fx-alignment: CENTER; -fx-min-width: 300px;-fx-background-color: #fdfdff; -fx-background-radius: 5px; -fx-border-color: #116ecc; -fx-border-width: 2px; -fx-border-radius: 5px;");
   	  
   	 	SetBox.setX(780);
   	 	SetBox.setY(455);
         
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
    
    private void showCancelBox() {
    	Stage SetBox = new Stage();
      	 
   	 	SetBox.initStyle(StageStyle.UNDECORATED);
   	 	SetBox.initModality(Modality.APPLICATION_MODAL);
   	 	SetBox.initStyle(StageStyle.TRANSPARENT);
   	  
   	 	VBox root = CreatCancelBox(SetBox);
   	 	Scene scene = new Scene(root);
   	 	scene.setFill(Color.TRANSPARENT); 
   	 	scene.getStylesheets().add(getClass().getResource("/seu/vcampus/client/css/edu_admin.css").toExternalForm());
   	 	root.setStyle("-fx-padding: 20px; -fx-alignment: CENTER; -fx-min-width: 300px;-fx-background-color: #fdfdff; -fx-background-radius: 5px; -fx-border-color: #116ecc; -fx-border-width: 2px; -fx-border-radius: 5px;");
   	  
   	 	SetBox.setX(780);
   	 	SetBox.setY(505);
         
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
    
    private VBox CreatSelectBox(Stage STAGE) {
    	SelectedCourseItem selectedItem = course_scheduled.getSelectionModel().getSelectedItem();
    	Label Title = new Label(selectedItem.getCourseName());
    	Title.getStyleClass().add("my-label");
    	Title.setStyle("-fx-alignment: CENTER; -fx-text-fill: #116ecc;  -fx-font-size: 14px; -fx-font-weight: 500; -fx-alignment: center; ");
    	
    	ComboBox<String> teachers = new ComboBox<>();
    	teachers.getStyleClass().add("error-combobox");
    	teachers.setStyle("-fx-min-width: 70px;");
    	ComboBox<Integer> classes = new ComboBox<>();
    	classes.getStyleClass().add("error-combobox");
    	classes.setStyle("-fx-min-width: 70px;");
    	classes.setDisable(true);
    	classes.getItems().clear();
    	HBox COMBOS = new HBox(5,teachers,classes);
    	COMBOS.setStyle("-fx-alignment: CENTER;");
    	
    	
    	Label scheduleInfo = new Label("暂无排课信息");
    	scheduleInfo.setWrapText(true);
    	scheduleInfo.getStyleClass().add("my-label");
    	scheduleInfo.setStyle("-fx-min-height: 50px");
    	
    	Label MaxText = new Label("课容量：");
    	Label MaxNum = new Label("0");
    	Label R = new Label("人");
    	MaxText.getStyleClass().add("my-label");
    	MaxNum.getStyleClass().add("my-label");
    	R.getStyleClass().add("my-label");
    	HBox MaxBox = new HBox(5,MaxText,MaxNum,R);
    	MaxBox.getStyleClass().add("white-background");
    	MaxBox.setStyle("-fx-alignment: CENTER;");
    	
    	Button OkButton = new Button("选择");
    	OkButton.getStyleClass().add("big-button");
    	OkButton.setStyle("-fx-min-width: 70px;");
    	Button CancelButton = new Button("取消");
    	CancelButton.getStyleClass().add("my-button");
    	CancelButton.setStyle("-fx-min-width: 70px;");
    	HBox BtnBox = new HBox(10,OkButton,CancelButton);
    	BtnBox.getStyleClass().add("white-background");
    	BtnBox.setStyle("-fx-alignment: CENTER;");
    	
    	VBox Total = new VBox(10,Title,COMBOS,scheduleInfo,MaxBox,BtnBox);
    	
    	initCourseMap();
    	
    	String targetCourseName = selectedItem.getCourseName(); 
    	
    	teachers.getItems().clear();
        
        // 从映射中获取该课程对应的所有教师名
        Map<String, Map<Integer,MapItem>> teacherScheduleMap = ctMap.get(targetCourseName);
        if (teacherScheduleMap != null && !teacherScheduleMap.isEmpty()) {
            // 将教师名添加到下拉框（按自然顺序排序）
            List<String> teacherNames = new ArrayList<>(teacherScheduleMap.keySet());
            Collections.sort(teacherNames);
            teachers.getItems().addAll(teacherNames);
        } else {
            // 没有找到对应课程的教师
            teachers.getItems().add("无匹配教师");
            scheduleInfo.setText("无排课信息");
        }
        
        teachers.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) ->{
        	Map<Integer,MapItem> classItemMap = teacherScheduleMap.get(newVal);
        	if (classItemMap != null && !classItemMap.isEmpty()) {
                // 将教师名添加到下拉框（按自然顺序排序）
        		List<Integer> classList = new ArrayList<>(classItemMap.keySet());
                Collections.sort(classList);
                classes.getItems().addAll(classList);
            } else {
            	classes.getItems().add(-1);
                scheduleInfo.setText("无排课信息");
            }
        	classes.setDisable(false);
        });
        
        classes.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal)->{
        	String TargetTeacherName = teachers.getValue();
        	if(newVal != null) {
        		MapItem mapitem = ctMap.getOrDefault(targetCourseName, new HashMap<>()).getOrDefault(TargetTeacherName, new HashMap<>()).getOrDefault(newVal, new MapItem("暂无排课信息",0));
        		String SCHEDULETEXT = mapitem.getS();
        		SCHEDULETEXT = SCHEDULETEXT.replace("、", "\n");
        		scheduleInfo.setText(SCHEDULETEXT);
        		int studentnum = mapitem.getC();
        		MaxNum.setText(String.valueOf(studentnum));
        	}
        	else {
        		scheduleInfo.setText("请选择老师");
        	}
        });
        
        OkButton.setOnAction(e->{
        	String tip;
        	if(teachers.getValue() == null || teachers.getValue().isEmpty()) {
        		tip = "请选择任课老师！";
        		showErrorDialog(tip);
        	}
        	else if(classes.getItems() == null || classes.getItems().isEmpty()) {
        		tip = "请选择教学班！";
        		showErrorDialog(tip);
        	}
        	else {
        		new Thread(() -> {
                    try {
                        // 使用SocketManager的out流发送登录请求
                        DataOutputStream out = SocketManager.getInstance().getOut();
                        out.writeUTF("CourseSelectedRequire");
                        String jsonReq = gson.toJson(new StudentSC(sId,targetCourseName,teachers.getValue(),classes.getValue().intValue()));
    	                out.writeUTF(jsonReq);
                        out.flush();
                        
                        DataInputStream in = SocketManager.getInstance().getIn();
    	                String Response = in.readUTF();
    	                Platform.runLater(() -> {
    	                    showOkDialog(Response); 
    	                });
                        if(Response.equals("选课成功！")) {
                        	selectedItem.setSelected(!selectedItem.isSelected());
                        	refresh.fire();
                        	//List<TimeClass> TIMECLASS = getTimeList(new CourseIndex(targetCourseName, teachers.getValue(), classes.getValue().intValue()));
                        	//for(TimeClass time : TIMECLASS) {
                        		//sTimeList.add(new CourseToTime(targetCourseName, teachers.getValue(), classes.getValue().intValue(), time.getFWeek(), time.getEWeek(), 
                        			//	time.getDay(), time.getFTime(), time.getETime()));
                        	//}
                        	initTableRow();
                        }
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
        
        CancelButton.setOnAction(e->STAGE.close());
    	
    	return Total;
    }
    
    private VBox CreatCancelBox(Stage STAGE) {
    	SelectedCourseItem selectedItem = course_scheduled.getSelectionModel().getSelectedItem();
    	Label Title = new Label(selectedItem.getCourseName());
    	Title.getStyleClass().add("my-label");
    	Title.setStyle("-fx-alignment: CENTER; -fx-text-fill: #116ecc;  -fx-font-size: 14px; -fx-font-weight: 500; -fx-alignment: center; ");
    	
    	Label sTitle = new Label("是否要退选课程？");
    	sTitle.getStyleClass().add("my-label");
    	sTitle.setStyle("-fx-alignment: CENTER; -fx-font-size: 14px");
    	
    	Button OkButton = new Button("确定");
    	OkButton.getStyleClass().add("big-button");
    	Button CancelButton = new Button("取消");
    	CancelButton.getStyleClass().add("my-button");
    	HBox BtnBox = new HBox(10,OkButton,CancelButton);
    	BtnBox.setStyle("-fx-alignment: CENTER;");
    	
    	VBox ContainV = new VBox(10,sTitle,BtnBox);
    	ContainV.setStyle("-fx-alignment: CENTER;");
    	
    	VBox Final = new VBox(10,Title,ContainV);
    	
    	OkButton.setOnAction(e->{
    		new Thread(() -> {
                try {
                    // 使用SocketManager的out流发送登录请求
                    DataOutputStream out = SocketManager.getInstance().getOut();
                    out.writeUTF("CourseDropRequire");
                    String jsonReq = gson.toJson(new StudentSC(sId,selectedItem.getCourseName(),"",-1));
	                out.writeUTF(jsonReq);
                    out.flush();
                    
                    DataInputStream in = SocketManager.getInstance().getIn();
	                String Response = in.readUTF();
	                Platform.runLater(() -> {
	                    showOkDialog(Response); 
	                });
                    if(Response.equals("已退选")) {
                    	selectedItem.setSelected(!selectedItem.isSelected());
                    	/*out.writeUTF(sId);
                    	out.flush();
                    	String sTimeJson = in.readUTF();
                    	Type sType = new TypeToken<List<StudentSC>>() {}.getType();
                        studentList = gson.fromJson(sTimeJson, sType);
                    	initTableRow();*/
                    	refresh.fire();
                    }
                } catch (Exception ex) {
                    Platform.runLater(() ->
                    	showErrorDialog("请检查服务器是否启动或网络是否正常：" + ex.getMessage())
                    );
                    ex.printStackTrace();
                }
            }).start();
    		
    		STAGE.close();
    	});
    	
    	CancelButton.setOnAction(e->STAGE.close());
    	
    	return Final;
    }
    
    private void initCourseMap() {
        for (FinalCourse course : courseList) {
            String courseName = course.getName();
            String teacherName = course.getTeacherName();
            String schedule = course.getSchedule();
            int classId = course.getClassID();
            int studentNum = course.getStudentNum();
            
            // 如果课程名不在映射中，先初始化
            ctMap.putIfAbsent(courseName, new HashMap<>());
            List<TimeClass> newTime = getTimeList(new CourseIndex(courseName, teacherName, classId));
            int curNum = numMap.getOrDefault(new CourseIndex(courseName, teacherName, classId), 0);
            if(!conflict(newTime) && curNum < studentNum) {
            	 // 存储教师名与排课信息的对应关系
                ctMap.get(courseName).putIfAbsent(teacherName, new HashMap<>());
                ctMap.get(courseName).get(teacherName).put(classId, new MapItem(schedule,studentNum));
            }
        }
    }
    
    private void initRNumMap() {
    	 for (FinalCourse course : courseList) {
             String courseName = course.getName();
             int studentNum = course.getStudentNum();
             rNumMap.put(courseName, studentNum);
         }
    }
    
    public void addTimeToCourse(CourseIndex courseIndex, TimeClass timeClass) {
        // 如果CourseIndex不存在，创建新的List
        if (!timeMap.containsKey(courseIndex)) {
        	timeMap.put(courseIndex, new ArrayList<>());
        }
        // 将TimeClass添加到对应的List中
        timeMap.get(courseIndex).add(timeClass);
    }
    
    // 通过CourseIndex获取对应的TimeClass列表
    public List<TimeClass> getTimeList(CourseIndex courseIndex) {
        // 如果不存在对应的值，返回空列表而不是null，避免空指针异常
        return timeMap.getOrDefault(courseIndex, new ArrayList<>());
    }
    
    private void initNumMap() {
    	for (StudentSC sc : allScList) {
    		
            String courseNAME = sc.getCourseName();
            String teacherNAME =sc.getTeacherName();
            int CLASSID = sc.getClassId();
            // 如果课程已存在，数量+1；否则设置为1
            numMap.put(new CourseIndex(courseNAME, teacherNAME, CLASSID), numMap.getOrDefault(new CourseIndex(courseNAME, teacherNAME, CLASSID), 0) + 1);
        }
    }
    
    private void initNameMap() {
    	for(CourseToTime sc : cttList) {
    		CourseIndex courseindex = new CourseIndex(sc.getCourseName(), sc.getTeacherName(), sc.getClassId());
    		String COURSENAME = sc.getCourseName();
    		addIndexToName(courseindex, COURSENAME);
    	}
    }
    
    public void addIndexToName(CourseIndex courseIndex, String NAME) {
        // 如果CourseIndex不存在，创建新的List
        if (!nameMap.containsKey(NAME)) {
        	nameMap.put(NAME, new ArrayList<>());
        }
        // 将TimeClass添加到对应的List中
        nameMap.get(NAME).add(courseIndex);
    }
    
    public List<CourseIndex> getIndexList(String NAME) {
        // 如果不存在对应的值，返回空列表而不是null，避免空指针异常
        return nameMap.getOrDefault(NAME, new ArrayList<>());
    }
    
    private boolean noItem(String NAME) {
    	List<CourseIndex> indexes = getIndexList(NAME);
    	for(CourseIndex curIndex : indexes) {
    		int curNum = numMap.getOrDefault(curIndex, 0);
    		int rNum = rNumMap.getOrDefault(curIndex, 1000);
    		if(curNum > rNum) {
    			continue;
    		}
    		List<TimeClass> times = getTimeList(curIndex);
    		if(!conflict(times)) {
    			return false;
    		}
    	}
    	return true;
    }
    
    private void initTimeMap() {
    	for(CourseToTime item : cttList) {
    		CourseIndex courseindex = new CourseIndex(item.getCourseName(), item.getTeacherName(), item.getClassId());
    		TimeClass timaclass = new TimeClass(item.getFWeek(), item.getEWeek(), item.getDay(), item.getFTime(), item.getETime());
    		addTimeToCourse(courseindex, timaclass);
    	}
    }
    
    private boolean conflict(List<TimeClass> times) {
    	for(TimeClass time : times) {
    		for(CourseToTime sTime : sTimeList) {
    			if(!(time.getEWeek() < sTime.getFWeek() || time.getFWeek() > sTime.getEWeek()) && time.getDay() == sTime.getDay() && 
    					!(time.getETime() < sTime.getFTime() || time.getFTime() > sTime.getETime())) {
    				return true;
    			}
    		}
    	}
    	return false;
    }
    
    @FXML
    void OnSearchClick(ActionEvent event) {
    	String NAME = nameText.getText();
    	if(NAME.isEmpty()) {
    		initTableRow();
    	}
    	else {
    		boolean IN = false; 
    		SelectedCourseItem ITEM;
    		for(FinalCourse course : courseList) {
    			if(course.getName().equals(NAME)) {
    				IN = true;
    				ITEM = convertToSelectedCourseItem(course);
    				for(StudentSC currentStudent : studentList) {
                    	if(currentStudent.getCourseName().equals(course.getName())) {
                    		ITEM.setSelected(true);
                    	}
                    	else if(noItem(ITEM.getCourseName())) {
                    		ITEM.setConflict(true);
                    	}
    				}
    				course_scheduled.getItems().clear();
    	            course_scheduled.getItems().add(ITEM);
    				break;
    			}
    		}
    		if(!IN) {
    			course_scheduled.getItems().clear();
    		}
    	}
    }

    //刷新操作///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    
    @FXML
    void OnRefreshClick(ActionEvent event) {
    	new Thread(() -> {
            try {
                DataOutputStream out = SocketManager.getInstance().getOut();
                out.writeUTF("DateToSelectRequire");
                out.writeUTF(sMajor);
                out.writeInt(sType);
                out.writeUTF(sId);
                out.flush();
                
                DataInputStream in = SocketManager.getInstance().getIn();
                
                String jsonData = in.readUTF();
                Type type = new TypeToken<List<FinalCourse>>(){}.getType(); // 泛型类型
                courseList = gson.fromJson(jsonData, type);
                String sData = in.readUTF();
                Type sType = new TypeToken<List<StudentSC>>() {}.getType();
                studentList = gson.fromJson(sData, sType);
                String scData = in.readUTF();
                String ctData = in.readUTF();
                Type ctType = new TypeToken<List<CourseToTime>>() {}.getType();
                cttList = gson.fromJson(ctData, ctType);
                sTimeList = gson.fromJson(scData, ctType);
                String allData = in.readUTF();
                allScList = gson.fromJson(allData, sType);
                
                initRNumMap();
                initNameMap();
                initNumMap();
                initTimeMap();
                initTableRow();
                initComboBoxes();
                
            } catch (Exception e) {
                Platform.runLater(() ->
                	showErrorDialog("请检查服务器是否启动或网络是否正常：" + e.getMessage())
                );
                e.printStackTrace();
            }
        }).start();
    }
    
    //筛选设置///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
      
    private void addFilterListeners() {
        courseNature.setOnAction(e -> filterCourses());
        courseDepartment.setOnAction(e -> filterCourses());
    }
    
    private void filterCourses() {
    	String NATURE = courseNature.getValue();
    	String DEPARTMENT = courseDepartment.getValue();
    	
    	ObservableList<SelectedCourseItem> tableData = FXCollections.observableArrayList();
        for(FinalCourse course : courseList) {
        	String COURSENAME = course.getName();
        	cfMap.putIfAbsent(COURSENAME, course);
        }
        for (FinalCourse course : cfMap.values()) {
        	SelectedCourseItem currentItem = convertToSelectedCourseItem(course);
            for(StudentSC currentStudent : studentList) {
            	if(currentStudent.getCourseName().equals(course.getName())) {
            		currentItem.setSelected(true);
            	}
            	else if(noItem(currentItem.getCourseName())) {
            		currentItem.setConflict(true);
            	}
            }
            tableData.add(currentItem);
        }
        
        ObservableList<SelectedCourseItem> filtered = tableData.filtered(course -> {

            if (NATURE != null && !"全部".equals(NATURE) && !NATURE.equals(course.getNature())) {
                return false;
            }
            
            if (DEPARTMENT != null && !"全部".equals(DEPARTMENT) && !DEPARTMENT.equals(course.getDepartment())) {
                return false;
            }
            
            return true;
        });
        
        ObservableList<SelectedCourseItem> Realfiltered = FXCollections.observableArrayList(filtered);

        course_scheduled.setItems(Realfiltered);
    }
    
    private void initComboBoxes() {
        List<String> Natures = courseList.stream()
                .map(FinalCourse::getNature)
                .distinct()
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        Natures.add(0, "全部");
        Platform.runLater(() -> {
        courseNature.setItems(FXCollections.observableArrayList(Natures));
        courseNature.getSelectionModel().selectFirst();
        });
        
        List<String> departments = courseList.stream()
                .map(FinalCourse::getDepartment)
                .distinct()
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        departments.add(0, "全部");
        Platform.runLater(() -> {
        courseDepartment.setItems(FXCollections.observableArrayList(departments));
        courseDepartment.getSelectionModel().selectFirst();
        });
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
        
        errorStage.setX(780);
        errorStage.setY(515);
        
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
        errorStage.setY(515);
        
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
