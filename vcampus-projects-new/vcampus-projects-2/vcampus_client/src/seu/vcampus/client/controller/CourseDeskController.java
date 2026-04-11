package seu.vcampus.client.controller;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.lang.reflect.Type;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.ResourceBundle;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import javafx.animation.ScaleTransition;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.Border;
import javafx.scene.layout.BorderStroke;
import javafx.scene.layout.BorderStrokeStyle;
import javafx.scene.layout.BorderWidths;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;
import seu.vcampus.client.controller.CourseSelectController.CourseIndex;
import seu.vcampus.client.network.SocketManager;
import seu.vcampus.model.DeskCourse;
import seu.vcampus.model.FinalCourse;

public class CourseDeskController implements Initializable {
	
	private final String[] WEEK_DAYS = {"", "星期一", "星期二", "星期三", "星期四", "星期五", "星期六", "星期日"};
    private final String[] COURSE_SESSIONS = {"", "第一节", "第二节", "第三节", "第四节", "第五节", "第六节", "第七节", "第八节", "第九节", "第十节", "第十一节", "第十二节", "第十三节"};
    String[] weeks = {"第一周", "第二周", "第三周", "第四周", "第五周", "第六周", "第七周", "第八周", "第九周", "第十周", "第十一周", "第十二周", "第十三周", "第十四周", "第十五周", "第十六周", "第十七周", "第十八周"};
    
	private static final Gson gson = new Gson();

	private String userId = "20230001";
	
	private List<FinalCourse> courseList = new ArrayList<>();
	
	private List<DeskCourse> sTimeList = new ArrayList<>();
	
    @FXML
    private Button refresh;

    @FXML
    private GridPane courseDesk;

    @FXML
    private ComboBox<String> weekCombo;
    
    private Map<CourseIndex, List<TimeClass>> timeMap = new HashMap<>();

    private int TYPE = 1;
    
    private String TNAME = "泥扣";
    
    public void setUserId(String GETID) {
    	userId = GETID;
    	getType();
    	System.out.println("可以："+userId+TYPE);
    	initGridHeaders();
    	weekCombo.setItems(FXCollections.observableArrayList(weeks));
    	weekCombo.setValue("第一周");
    	weekCombo.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
            	clearOldCourseData();
            	initGridHeaders();
            	fillCourseData();
            }
        });
    }
    
    private void getType() {
    	new Thread(() -> {
            try {
                DataOutputStream out = SocketManager.getInstance().getOut();
                
                out.writeUTF("CourseDeskInfo");
                out.writeUTF(userId);
                out.flush();
                
                DataInputStream in = SocketManager.getInstance().getIn();
                
                TYPE = in.readInt();
                
                getData();
                
            } catch (Exception e) {
                Platform.runLater(() ->
                	showErrorDialog("请检查服务器是否启动或网络是否正常：" + e.getMessage())
                );
                e.printStackTrace();
            }
        }).start();
    }
    
    //初始化//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    @Override
    public void initialize(URL location, ResourceBundle resources) {
    	
    }
    
    //类定义/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    
    public class TimeClass{
    	private int fWeek;
    	private int eWeek;
    	private int day;
    	private int fTime;
    	private int eTime;
    	private String room;
    	
    	public TimeClass(int FWEEK, int EWEEK, int DAY, int FTIME, int ETIME, String ROOM) {
    		this.fWeek = FWEEK;
    		this.eWeek = EWEEK;
    		this.day = DAY;
    		this.fTime = FTIME;
    		this.eTime = ETIME;
    		this.room = ROOM;
    	}
    	
    	public int getFWeek() {return fWeek;}
    	public int getEWeek() {return eWeek;}
    	public int getDay() {return day;}
    	public int getFTime() {return fTime;}
    	public int getETime() {return eTime;}
    	public String getRoom() {return room;}
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
    
    //初始化表头//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    
    private void initGridHeaders() {
    	courseDesk.setHgap(0);  // 水平间距为 0
    	courseDesk.setVgap(0); 
        // 设置第一行（星期）
        for (int col = 0; col < WEEK_DAYS.length; col++) {
            Label dayLabel = new Label(WEEK_DAYS[col]);
            dayLabel.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
            dayLabel.setStyle("-fx-background-color: #fdfdff; -fx-font-weight: bold; -fx-padding: 5px; -fx-alignment: center;  -fx-font-size: 14px;");
            dayLabel.setPrefSize(courseDesk.getColumnConstraints().get(col).getPrefWidth(), 30);
            dayLabel.setBorder(new Border(new BorderStroke(
            	    Color.LIGHTGRAY,  // 边框颜色
            	    BorderStrokeStyle.SOLID,  // 实线
            	    CornerRadii.EMPTY,  // 无圆角
            	    new BorderWidths(0, 1, 1, 0)  // 上0、右1、下1、左0（只显示右下边框）
            	)));
            courseDesk.add(dayLabel, col, 0);
        }

        // 设置第一列（节次）
        for (int row = 1; row < COURSE_SESSIONS.length; row++) {
            Label sessionLabel = new Label(COURSE_SESSIONS[row]);
            sessionLabel.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
            sessionLabel.setStyle("-fx-background-color: #fdfdff; -fx-font-weight: bold; -fx-padding: 5px; -fx-alignment: center;  -fx-font-size: 14px;");
            sessionLabel.setPrefSize(100, 30);
            sessionLabel.setBorder(new Border(new BorderStroke(
            	    Color.LIGHTGRAY,
            	    BorderStrokeStyle.SOLID,
            	    CornerRadii.EMPTY,
            	    new BorderWidths(0, 1, 1, 0)  // 同样只显示右下边框
            	)));
            courseDesk.add(sessionLabel, 0, row);
        }
    }
    
    //填充课程内容/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    
    private void fillCourseData() {
    	int WEEK = getWeek(weekCombo.getValue());
        for (FinalCourse course : courseList) {
        	if(course.getFirstWeek() <= WEEK && course.getLastWeek() >= WEEK) {
                
                List<TimeClass> timelist = getTimeList(new CourseIndex(course.getName(), course.getTeacherName(), course.getClassID()));
                
                for(TimeClass item : timelist) {
                	Label courseLabel = new Label(course.getName());
                    courseLabel.setStyle(" -fx-font-size: 14px; -fx-text-fill: #116ecd;");
                    Label roomLabel = new Label(item.getRoom());
                    roomLabel.setStyle(" -fx-font-size: 14px; -fx-text-fill: #116ecd;");
                    VBox labelBox = new VBox(5, courseLabel, roomLabel);
                    labelBox.setStyle("-fx-background-color: #fdfdff; -fx-border-color: #116ecd; -fx-border-width: 1px; -fx-alignment: center;");
                    labelBox.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
                    courseLabel.setOnMouseClicked(event -> {
                        if (event.getClickCount() == 2) {
                        	if(TYPE == 1) {
                        	showDetailBox(course.getName(), course.getTeacherName(), course.getSchedule(), 
                        			course.getStudentNum(), course.getFirstWeek(), course.getLastWeek(), course.getCredit());
                        	}
                        	else {
                        		showTeacherBox(course.getName(), course.getTeacherName(), course.getSchedule(), 
                            			course.getStudentNum(), course.getFirstWeek(), course.getLastWeek(), course.getCredit());
                        	}
                        }
                    });
                    int column = item.getDay();
                    int startRow = item.getFTime();
                    int endRow = item.getETime();
                    int rowSpan = endRow - startRow + 1;  
                    Platform.runLater(() -> {
                    	courseDesk.add(labelBox, column, startRow);
                    GridPane.setRowSpan(labelBox, rowSpan);
                    	setGridCellStyles(column, startRow, rowSpan);
                    });
                }
            }
        }
    }
    
    private void setGridCellStyles(int column, int startRow, int rowSpan) {
        for (int i = 0; i < rowSpan; i++) {
        	final int currentI = i;
            Region cell = new Region();
            cell.setBorder(new Border(new BorderStroke(
                    Color.web("#116ecd"), 
                    BorderStrokeStyle.SOLID, 
                    CornerRadii.EMPTY,  
                    new BorderWidths(1)  
                )));
            Platform.runLater(() -> {
            	courseDesk.add(cell, column, startRow + currentI);
            GridPane.setRowIndex(cell, startRow + currentI);  
            GridPane.setColumnIndex(cell, column);
            courseDesk.getChildren().remove(cell); 
            });
        }
    }
    
    private void showDetailBox(String COURSENAME, String TEACHERNAME, String SCHEDULE, int SNUM, int FWEEK, int EWEEK, double CREDIT) {
   	 	Stage SetBox = new Stage();
   	 
   	 	SetBox.initStyle(StageStyle.UNDECORATED);
   	 	SetBox.initModality(Modality.APPLICATION_MODAL);
   	 	SetBox.initStyle(StageStyle.TRANSPARENT);
   	 	
   	 	SCHEDULE = SCHEDULE.replace("、", "\n");
   	 	
   	 	Label coursename = new Label(COURSENAME);
   	 	coursename.setStyle("-fx-font-size: 14px; -fx-text-fill: #116ecd;");
   	 	Label teachername = new Label(TEACHERNAME);
   	 	Label schedule = new Label(SCHEDULE);
   	 	Label snum = new Label("课容量：" + SNUM);
   	 	Label week = new Label(FWEEK + "-" + EWEEK + "周");
   	 	Label credit = new Label("学分：" + CREDIT);
   	 	teachername.setStyle("-fx-font-size: 14px; -fx-text-fill: #333e48;");
   	 	schedule.setStyle("-fx-font-size: 14px; -fx-text-fill: #333e48; -fx-min-height: 50px");
   	 	snum.setStyle("-fx-font-size: 14px; -fx-text-fill: #333e48;");
   	 	week.setStyle("-fx-font-size: 14px; -fx-text-fill: #333e48;");
   	 	credit.setStyle("-fx-font-size: 14px; -fx-text-fill: #333e48;");
   	 	Button OkButton = new Button("确定");
	 	OkButton.getStyleClass().add("my-button");
	 	OkButton.setStyle("-fx-min-width: 100px");
	 	OkButton.setOnAction(e->SetBox.close());
	 	VBox newBox = new VBox(10,OkButton);
	 	VBox labelBox = new VBox(5, teachername, schedule, snum, week, credit);
	 	labelBox.getStyleClass().add("card-background");
	 	labelBox.setStyle("-fx-alignment: CENTER; -fx-padding: 10px");
	 	newBox.getStyleClass().add("white-background");
	 	newBox.setStyle("-fx-alignment: CENTER;");
   	  
   	 	VBox root = new VBox(10, coursename, labelBox, newBox);
   	 	root.setStyle("-fx-alignment: center;");
   	 	Scene scene = new Scene(root);
   	 	scene.setFill(Color.TRANSPARENT); 
   	 	root.setStyle("-fx-padding: 20px; -fx-alignment: CENTER; -fx-min-width: 300px;-fx-background-color: #fdfdff; -fx-background-radius: 5px; -fx-border-color: #116ecc; -fx-border-width: 2px; -fx-border-radius: 5px;");
   	 	scene.getStylesheets().add(getClass().getResource("/seu/vcampus/client/css/edu_admin.css").toExternalForm());
   	 	
   	 	SetBox.setX(780);
   	 	SetBox.setY(380);
         
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
    
    private void showTeacherBox(String COURSENAME, String TEACHERNAME, String SCHEDULE, int SNUM, int FWEEK, int EWEEK, double CREDIT) {
   	 	Stage SetBox = new Stage();
   	 
   	 	SetBox.initStyle(StageStyle.UNDECORATED);
   	 	SetBox.initModality(Modality.APPLICATION_MODAL);
   	 	SetBox.initStyle(StageStyle.TRANSPARENT);
   	 	
   	 	SCHEDULE = SCHEDULE.replace("、", "\n");
   	 	
   	 	Label coursename = new Label(COURSENAME);
   	 	coursename.setStyle("-fx-font-size: 14px; -fx-text-fill: #116ecd;");
   	 	Label schedule = new Label(SCHEDULE);
   	 	Label snum = new Label("课容量：" + SNUM);
   	 	Label week = new Label(FWEEK + "-" + EWEEK + "周");
   	 	Label credit = new Label("学分：" + CREDIT);
   	 	schedule.setStyle("-fx-font-size: 14px; -fx-text-fill: #333e48; -fx-min-height: 50px");
   	 	snum.setStyle("-fx-font-size: 14px; -fx-text-fill: #333e48;");
   	 	week.setStyle("-fx-font-size: 14px; -fx-text-fill: #333e48;");
   	 	credit.setStyle("-fx-font-size: 14px; -fx-text-fill: #333e48;");
   	 	Button OkButton = new Button("确定");
	 	OkButton.getStyleClass().add("my-button");
	 	OkButton.setStyle("-fx-min-width: 100px");
	 	OkButton.setOnAction(e->SetBox.close());
	 	VBox newBox = new VBox(10,OkButton);
	 	VBox labelBox = new VBox(5, schedule, snum, week, credit);
	 	labelBox.getStyleClass().add("card-background");
	 	labelBox.setStyle("-fx-alignment: CENTER; -fx-padding: 10px");
	 	newBox.getStyleClass().add("white-background");
	 	newBox.setStyle("-fx-alignment: CENTER;");
   	  
   	 	VBox root = new VBox(10, coursename, labelBox, newBox);
   	 	root.setStyle("-fx-alignment: center;");
   	 	Scene scene = new Scene(root);
   	 	scene.setFill(Color.TRANSPARENT); 
   	 	root.setStyle("-fx-padding: 20px; -fx-alignment: CENTER; -fx-min-width: 300px;-fx-background-color: #fdfdff; -fx-background-radius: 5px; -fx-border-color: #116ecc; -fx-border-width: 2px; -fx-border-radius: 5px;");
   	 	scene.getStylesheets().add(getClass().getResource("/seu/vcampus/client/css/edu_admin.css").toExternalForm());
   	 	
   	 	SetBox.setX(780);
   	 	SetBox.setY(380);
         
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
    
    //清除操作////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    
    private void clearOldCourseData() {
        List<javafx.scene.Node> oldNodes = new ArrayList<>();
        for (javafx.scene.Node node : courseDesk.getChildren()) {
        	oldNodes.add(node);
        }
        Platform.runLater(() -> {
            courseDesk.getChildren().removeAll(oldNodes);
        });
    }
    
    //获取数据////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    
    private void getData() {
    	try {
            DataOutputStream out = SocketManager.getInstance().getOut();
            DataInputStream in = SocketManager.getInstance().getIn();
            
            if(TYPE == 1) {
            	out.writeUTF("SelectedCourseRequire");
            }
            else {
            	out.writeUTF("getTForC");
            	out.writeUTF(userId);
            	out.flush();
            	TNAME = in.readUTF();
            	out.writeUTF("TeacherDeskRequire");
            }
            if(TYPE == 1) {
            	out.writeUTF(userId);
            }else {
            	out.writeUTF(TNAME);
            }
            out.flush();
            
            String jsonData = in.readUTF();
            String sTimeData = in.readUTF();
            Type type = new TypeToken<List<FinalCourse>>(){}.getType();
            Type sType = new TypeToken<List<DeskCourse>>(){}.getType();
            courseList = gson.fromJson(jsonData, type);
            sTimeList = gson.fromJson(sTimeData, sType);
            
            initTimeMap();
            fillCourseData();
            
        } catch (Exception e) {
            Platform.runLater(() ->
            	showErrorDialog("请检查服务器是否启动或网络是否正常：" + e.getMessage())
            );
            e.printStackTrace();
        }
    }
    
    @FXML
    void onRefreshClick(ActionEvent event) {
    	clearOldCourseData();
    	initGridHeaders();
    	getType();
    }
    
    //星期转int///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    
    private int getWeek(String WEEK) {
    	switch(WEEK) {
    	case "第一周":
    		return 1;
    	case "第二周":
    		return 2;
    	case "第三周":
    		return 3;
    	case "第四周":
    		return 4;
    	case "第五周":
    		return 5;
    	case "第六周":
    		return 6;
    	case "第七周":
    		return 7;
    	case "第八周":
    		return 8;
    	case "第九周":
    		return 9;
    	case "第十周":
    		return 10;
    	case "第十一周":
    		return 11;
    	case "第十二周":
    		return 12;
    	case "第十三周":
    		return 13;
    	case "第十四周":
    		return 14;
    	case "第十五周":
    		return 15;
    	case "第十六周":
    		return 16;
    	case "第十七周":
    		return 17;
    	case "第十八周":
    		return 18;
    	default:
    		return 0;
    	}
    }
    
    //Map操作函数/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    
    public void addTimeToCourse(CourseIndex courseIndex, TimeClass timeClass) {
        if (!timeMap.containsKey(courseIndex)) {
        	timeMap.put(courseIndex, new ArrayList<>());
        }
        timeMap.get(courseIndex).add(timeClass);
    }
    
    public List<TimeClass> getTimeList(CourseIndex courseIndex) {
        return timeMap.getOrDefault(courseIndex, new ArrayList<>());
    }
    
    private void initTimeMap() {
    	for(DeskCourse item : sTimeList) {
    		CourseIndex courseindex = new CourseIndex(item.getCourseName(), item.getTeacherName(), item.getClassId());
    		TimeClass timaclass = new TimeClass(item.getFWeek(), item.getEWeek(), item.getDay(), item.getFTime(), item.getETime(), item.getRoom());
    		addTimeToCourse(courseindex, timaclass);
    	}
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
        contentLabel.setStyle("-fx-text-fill: #333e48; -fx-font-size: 14px;");

        // 关闭按钮
        Button okButton = new Button("确定");
        okButton.getStyleClass().add("my-button");
        okButton.setOnAction(e -> errorStage.close()); // 点击关闭窗口

        VBox root = new VBox(15, contentLabel, okButton);
        root.setStyle("-fx-padding: 20px; -fx-alignment: CENTER; -fx-min-width: 300px;-fx-background-color: #fdfdff; -fx-background-radius: 5px; -fx-border-color: #116ecc; -fx-border-width: 2px; -fx-border-radius: 5px;");

        Scene scene = new Scene(root);
        scene.setFill(Color.TRANSPARENT); 
        
        scene.getStylesheets().add(getClass().getResource("/seu/vcampus/client/css/edu_admin.css").toExternalForm());
        
        errorStage.setX(780);
        errorStage.setY(485);
        
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
