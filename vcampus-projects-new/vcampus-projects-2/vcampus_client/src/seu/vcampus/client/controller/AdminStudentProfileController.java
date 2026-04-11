package seu.vcampus.client.controller;

import java.net.URL;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.stream.Collectors;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.lang.reflect.Type;

import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.util.StringConverter;
import javafx.event.ActionEvent;
import javafx.concurrent.Task;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import seu.vcampus.model.StudentProfile;
import seu.vcampus.model.CollegeInfo;
import seu.vcampus.model.MajorInfo;
import seu.vcampus.model.ClassInfo;
import seu.vcampus.model.StudentRetrieveCondition;
import seu.vcampus.client.network.SocketManager;
import seu.vcampus.client.session.CampusSession;
import seu.vcampus.client.util.GradeConverter;

public class AdminStudentProfileController implements Initializable {
    // -------------------------- 界面组件定义 --------------------------
    @FXML private ScrollPane scrollPane;
	
	// 检索用组件
    @FXML private TextField byStudentId;
    @FXML private TextField byName;
    @FXML private ComboBox<String> byCollege;
    @FXML private ComboBox<String> byMajor;
    @FXML private ComboBox<String> byGrade;
    @FXML private ComboBox<String> byClassName;
    @FXML private Button retrieveButton;
    @FXML private Button conditionClearButton;

    // 表格组件
    @FXML private Label loadingLabel;
    @FXML private Label errorLabel;
    @FXML private TableView<StudentListEntry> studentList;
    @FXML private TableColumn<StudentListEntry, String> studentIdCol;
    @FXML private TableColumn<StudentListEntry, String> nameCol;
    @FXML private TableColumn<StudentListEntry, String> collegeCol;
    @FXML private TableColumn<StudentListEntry, String> majorCol;
    @FXML private TableColumn<StudentListEntry, String> gradeCol;
    @FXML private TableColumn<StudentListEntry, String> classNameCol;

    // 编辑用组件
    @FXML private TextField name;
    @FXML private TextField studentId;
    @FXML private ComboBox<String> college;
    @FXML private ComboBox<String> major;
    @FXML private ComboBox<String> grade;
    @FXML private ComboBox<String> className;
    @FXML private TextField mentor;
    @FXML private TextField mentorId;
    @FXML private TextField counsellor;
    @FXML private TextField counsellorId;
    @FXML private ComboBox<String> levelOfStudy;
    @FXML private TextField duration;
    @FXML private ComboBox<String> studentStatus;
    @FXML private DatePicker admissionDate;
    @FXML private DatePicker expectedGraduationDate;
    @FXML private Label loadingInfoLabel;
    @FXML private Label errorInfoLabel;
    @FXML private Button addButton;
    @FXML private Button deselectButton;
    @FXML private Button confirmButton;
    
    // 网络交互
    private DataInputStream dataIn;
    private DataOutputStream dataOut;
    private static final Gson gson = new Gson();
    private static CampusSession campusSession = CampusSession.getInstance();
    private String currentSelectedStudentId = null;

    // -------------------------- 初始化方法 --------------------------
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        initNetworkConnections();
        
    	initRetrieveComboBoxes();
        initStudentList();
        
        initBasicComboBoxes();
        initEditComboBoxes();
        initDatePickers();
        disableTextField();
        
        setupForwardListeners();
        setUpDoubleClick();
    }
    
	private void initNetworkConnections() {
		try {
			dataIn = SocketManager.getInstance().getIn();
			dataOut = SocketManager.getInstance().getOut();
		} catch (Exception e) {
			showError("网络连接失败，无法连接至服务器，请检查网络设置");
			e.printStackTrace();
		}
	}
	
    private void initRetrieveComboBoxes() {
    	// 初始化检索栏的ComboBox
    	List<String> colleges = campusSession.getAllColleges().stream()
    			.map(CollegeInfo::getCollegeName)
    			.collect(Collectors.toList());
    	
    	
        byCollege.setItems(FXCollections.observableArrayList(colleges));
        byCollege.setValue(null);

        byMajor.setItems(FXCollections.observableArrayList());
        byMajor.setValue(null);

        byGrade.setItems(FXCollections.observableArrayList());
        byGrade.setValue(null);

        byClassName.setItems(FXCollections.observableArrayList());
        byClassName.setValue(null);
    }
    
    private void initStudentList() {
        studentIdCol.setCellValueFactory(new PropertyValueFactory<>("studentId"));
        nameCol.setCellValueFactory(new PropertyValueFactory<>("name"));
        collegeCol.setCellValueFactory(new PropertyValueFactory<>("college"));
        majorCol.setCellValueFactory(new PropertyValueFactory<>("major"));
        gradeCol.setCellValueFactory(new PropertyValueFactory<>("grade"));
        classNameCol.setCellValueFactory(new PropertyValueFactory<>("classId"));
        
        studentList.setItems(FXCollections.observableArrayList());
    }
    
    private void initBasicComboBoxes() {
    	// 初始化培养层级、学生状态这类无关数据库信息的ComboBox
        ObservableList<String> levels = FXCollections.observableArrayList("本科", "研究生", "博士");
        levelOfStudy.setItems(levels);
        levelOfStudy.setValue(null);

        ObservableList<String> statuses = FXCollections.observableArrayList("在读", "休学", "毕业", "退学");
        studentStatus.setItems(statuses);
        studentStatus.setValue(null);
    }
    
    private void initDatePickers() {
    	// 初始化日期选择栏 使得其能够无视国家地区 强制选择yyyy-MM-dd
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        initSingleDatePicker(admissionDate, formatter);
        initSingleDatePicker(expectedGraduationDate, formatter);
    }

    private void initSingleDatePicker(DatePicker datePicker, DateTimeFormatter formatter) {
        datePicker.setConverter(new StringConverter<LocalDate>() {
            @Override
            public String toString(LocalDate date) {
                return date != null ? formatter.format(date) : "";
            }

            @Override
            public LocalDate fromString(String string) {
                return string != null && !string.isEmpty() ? LocalDate.parse(string, formatter) : null;
            }
        });
    }

    private void initEditComboBoxes() {
    	// 初始化和数据库内容有关的ComboBox
    	List<String> colleges = campusSession.getAllColleges().stream()
    			.map(CollegeInfo::getCollegeName)
    			.collect(Collectors.toList());
    	    	
        college.setItems(FXCollections.observableArrayList(colleges));
        college.setValue(null);

        major.setItems(FXCollections.observableArrayList());
        major.setValue(null);

        grade.setItems(FXCollections.observableArrayList(colleges));
        grade.setValue(null);

        className.setItems(FXCollections.observableArrayList());
        className.setValue(null);
    }
    
    private void disableTextField() {
    	duration.setDisable(true);
    	mentor.setDisable(true);
    	counsellor.setDisable(true);
    	levelOfStudy.setDisable(true);
    	expectedGraduationDate.setDisable(true);
    	confirmButton.setDisable(true);
    	deselectButton.setDisable(true);
    }


    private void setupForwardListeners() {
        byCollege.setOnAction(e -> {
            String selectedCollegeName = byCollege.getValue();
            String selectedCollegeId = campusSession.getCollegeIdByName(selectedCollegeName);
            
            List<String> majors = campusSession.getMajorsByCollege(selectedCollegeId).stream()
            		.map(MajorInfo::getMajorName)
            		.collect(Collectors.toList());
            
            ObservableList<String> newMajors = FXCollections.observableArrayList(majors);
            byMajor.setItems(newMajors);
            byMajor.setValue(null);
            
            byGrade.setItems(FXCollections.observableArrayList());
            byGrade.setValue(null);
            byClassName.setItems(FXCollections.observableArrayList());
            byClassName.setValue(null);
        });
        
        byMajor.setOnAction(e->{
        	String selectedCollegeName = byCollege.getValue();
        	String selectedMajorName = byMajor.getValue();

            if (selectedMajorName == null) {
                byGrade.setItems(FXCollections.observableArrayList());
                byGrade.setValue(null);
                byClassName.setItems(FXCollections.observableArrayList());
                byClassName.setValue(null);
                return;
            }
        	
        	MajorInfo majorInfo = campusSession.getMajorInfoByNames(selectedCollegeName, selectedMajorName);
        	
        	if (majorInfo == null) {
        		byGrade.setItems(FXCollections.observableArrayList());
        	}
        	
        	byGrade.setItems(FXCollections.observableArrayList(
        			GradeConverter.durationToGrades(
        				majorInfo.getUndergraduateDuration(), 
        				majorInfo.getPostgraduateDuration()
        			)
        		)
        	);
        	
        	byGrade.setValue(null);
            byClassName.setItems(FXCollections.observableArrayList());
            byClassName.setValue(null);
        });
        
        byGrade.setOnAction(e->{
        	String selectedCollegeName = byCollege.getValue();
        	String selectedMajorName = byMajor.getValue();
        	String selectedGrade = byGrade.getValue();
        	LocalDate currentDate = campusSession.getServerDate();
        	        	
        	if (selectedGrade == null) {
                byClassName.setItems(FXCollections.observableArrayList());
                byClassName.setValue(null);
                return;
            }
        	
        	String educationLevel;
        	
        	if (selectedGrade.charAt(0) == '大') {
        		educationLevel = "本科";
        	} else {
        		educationLevel = "研究生";
        	}
        
        	List<ClassInfo> classInfoList = campusSession.getClassesByMajor(
        		campusSession.getMajorIdByNames(selectedCollegeName, selectedMajorName)
        	);
        	
        	int enrollmentYearNum = GradeConverter.calculateEnrollmentYear(currentDate, selectedGrade);
        	String enrollmentYearStr = enrollmentYearNum + "级";
        	
        	ObservableList<String> classObList = FXCollections.observableArrayList();
        	
        	
        	
        	for (ClassInfo classInfo : classInfoList) {
        		if (classInfo.getGrade().equals(enrollmentYearStr) 
        			&& classInfo.getEducationLevel().equals(educationLevel)) {
        			classObList.add(classInfo.getClassName()); // 在这边多加一个判断语句即可 && 为本科生
        		}
        	}
        	byClassName.setItems(classObList);
        	byClassName.setValue(null);
        });
        
        college.setOnAction(e -> {
            String selectedCollegeName = college.getValue();
            String selectedCollegeId = campusSession.getCollegeIdByName(selectedCollegeName);
            
            List<String> majors = campusSession.getMajorsByCollege(selectedCollegeId).stream()
            		.map(MajorInfo::getMajorName)
            		.collect(Collectors.toList());
            
            ObservableList<String> newMajors = FXCollections.observableArrayList(majors);
            major.setItems(newMajors);
            major.setValue(null);
            
            grade.setItems(FXCollections.observableArrayList());
            grade.setValue(null);
            className.setItems(FXCollections.observableArrayList());
            className.setValue(null);
        });
        
        major.setOnAction(e->{
        	String selectedCollegeName = college.getValue();
        	String selectedMajorName = major.getValue();

            if (selectedMajorName == null) {
                grade.setItems(FXCollections.observableArrayList());
                grade.setValue(null);
                className.setItems(FXCollections.observableArrayList());
                className.setValue(null);
                return;
            }
        	
        	MajorInfo majorInfo = campusSession.getMajorInfoByNames(selectedCollegeName, selectedMajorName);
        	
        	if (majorInfo == null) {
        		grade.setItems(FXCollections.observableArrayList());
        	}
        	
        	grade.setItems(FXCollections.observableArrayList(
        			GradeConverter.durationToGrades(
        				majorInfo.getUndergraduateDuration(), 
        				majorInfo.getPostgraduateDuration()
        			)
        		)
        	);
        	
        	grade.setValue(null);
            className.setItems(FXCollections.observableArrayList());
            className.setValue(null);
        });
        
        grade.setOnAction(e->{
        	String selectedCollegeName = college.getValue();
        	String selectedMajorName = major.getValue();
        	String selectedGrade = grade.getValue();
        	LocalDate currentDate = campusSession.getServerDate();
        	
        	if (selectedGrade == null) {
                className.setItems(FXCollections.observableArrayList());
                className.setValue(null);
                return;
            }
        	
        	String educationLevel;
        	
        	if (selectedGrade.charAt(0) == '大') {
        		educationLevel = "本科";
        	} else {
        		educationLevel = "研究生";
        	}
        	
        	levelOfStudy.setValue(educationLevel);
        
        	List<ClassInfo> classInfoList = campusSession.getClassesByMajor(
        		campusSession.getMajorIdByNames(selectedCollegeName, selectedMajorName)
        	);
        	
        	int enrollmentYearNum = GradeConverter.calculateEnrollmentYear(currentDate, selectedGrade);
        	String enrollmentYearStr = enrollmentYearNum + "级";
        	
        	ObservableList<String> classObList = FXCollections.observableArrayList();
        	
        	for (ClassInfo classInfo : classInfoList) {
        		if (classInfo.getGrade().equals(enrollmentYearStr) 
        			&& classInfo.getEducationLevel().equals(educationLevel)) {
        			classObList.add(classInfo.getClassName());
        		}
        	}
        	className.setItems(classObList);
        	className.setValue(null);
        });
    }

    private void setUpDoubleClick() {
        studentList.setRowFactory(tv -> {
            TableRow<StudentListEntry> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty()) {
                    handleDoubleClick(row.getItem());
                }
            });
            return row;
        });
    }
    
    // -------------------------- 自定义事件响应函数 --------------------------
    private void handleDoubleClick(StudentListEntry entry) {
    	showInfoLoading(true);
    	scrollPane.setVvalue(1.0);
    	
    	name.setDisable(true);
    	this.studentId.setDisable(true);
    	confirmButton.setDisable(false);
    	deselectButton.setDisable(false);
    	addButton.setDisable(true);
    	
    	String studentId = entry.getStudentId();
    	Task<StudentProfile> fetchTask = new Task<>() {
    		@Override
    		protected StudentProfile call() throws Exception {
    			dataOut.writeUTF("GetStudentById");
    			dataOut.flush();
    			dataOut.writeUTF(studentId);
    			
    			String json = dataIn.readUTF();
    			StudentProfile rtn = gson.fromJson(json, StudentProfile.class);

    			return rtn;
    		}
    		
    		@Override
    		protected void succeeded() {
    			super.succeeded();
    			showInfoLoading(false);
    			fillTheBlank(getValue());
    		}
    		
    		@Override
    		protected void failed() {
    			// 发出错误信息
    			super.failed();
    			showInfoLoading(false);
    			showInfoError("加载单个学生信息错误");
    		}
    	};
    	new Thread(fetchTask).start();
    }
    
    private void fillTheBlank(StudentProfile sp) {
    	String theGrade = GradeConverter.enrollmentYearToGrade(sp.getGrade(), campusSession.getServerDate(), sp.getLevelOfStudy());
    	
    	name.setText(sp.getName());
    	studentId.setText(sp.getStudentId());
    	college.setValue(sp.getCollege());
    	major.setValue(sp.getMajor());
    	grade.setValue(theGrade);
    	className.setValue(sp.getClassName());
    	mentor.setText(sp.getMentor());
    	mentorId.setText(sp.getMentorId());
    	counsellor.setText(sp.getCounsellor());
    	counsellorId.setText(sp.getCounsellorId());
    	levelOfStudy.setValue(sp.getLevelOfStudy());
    	duration.setText(sp.getDuration());
    	studentStatus.setValue(sp.getStudentStatus());
        LocalDate admissionDateValue = LocalDate.parse(sp.getAdmissionDate(), DateTimeFormatter.ISO_DATE);
        LocalDate graduationDateValue = LocalDate.parse(sp.getExpectedGraduationDate(), DateTimeFormatter.ISO_DATE);
        admissionDate.setValue(admissionDateValue);
        expectedGraduationDate.setValue(graduationDateValue);
        
		currentSelectedStudentId = sp.getStudentId();
		addButton.setDisable(true);
		deselectButton.setDisable(false);
		confirmButton.setDisable(false);
    }
    
    // -------------------------- 注入事件响应函数 --------------------------
    @FXML
    void handleRetrieve(ActionEvent event) { // 就直接把学生拉过来吧
    	showLoading(true);
    	
    	String selectedGrade = byGrade.getValue();
    	String enrollmentYearStr = null; 
    	if (selectedGrade != null) {
        	int enrollmentYearNum = GradeConverter.calculateEnrollmentYear(campusSession.getServerDate(), selectedGrade);
        	enrollmentYearStr = enrollmentYearNum + "级";
    	}

    	StudentRetrieveCondition condition = new StudentRetrieveCondition();
        condition.setStudentId(byStudentId.getText().trim().isEmpty() ? null : byStudentId.getText().trim());
        condition.setName(byName.getText().trim().isEmpty() ? null : byName.getText().trim());
        condition.setCollege(byCollege.getValue());
        condition.setMajor(byMajor.getValue());
        condition.setGrade(enrollmentYearStr);
        condition.setClassName(byClassName.getValue());
    	
    	Task<List<StudentProfile>> loadTask = new Task<>() {
    		@Override
    		protected List<StudentProfile> call() throws Exception {
    			dataOut.writeUTF("GetStudentsByConditions");
    			dataOut.flush();
    			dataOut.writeUTF(gson.toJson(condition));
    			dataOut.flush();
    			
    			String json = dataIn.readUTF();
    			
                if (json.contains("error")) {
                    throw new Exception("查询失败：" + json);
                }
                
    			Type studentProfileListType = new TypeToken<List<StudentProfile>>() {}.getType();
    			List<StudentProfile> profiles = gson.fromJson(json, studentProfileListType);
    			
    			return profiles;
    		}
    		
    		@Override
    		protected void succeeded() {
    			super.succeeded();
    			showLoading(false);
    			
    			List<StudentProfile> profiles = getValue();
    			
    			ObservableList<StudentListEntry> entries = FXCollections.observableArrayList();
    			for (StudentProfile sp : profiles) {
    				entries.add(new StudentListEntry(
    					sp.getStudentId(),
    	                sp.getName(),
    	                sp.getCollege(),
    	                sp.getMajor(),
    	                sp.getGrade(),
    	                sp.getClassName()
    				));
    			}
    			
    			studentList.setItems(entries);
                if (entries.isEmpty()) {
                    showError("未找到符合条件的学生", false); // 非错误提示（绿色）
                }
    			
    		}
    		
    		@Override 
    		protected void failed() {
    			super.failed();
    			showLoading(false);
    			showError(getException().getMessage());
    		}
    	};
    	
    	new Thread(loadTask).start();
    }
    
    @FXML
    void handleConditionClear(ActionEvent event) {
        byStudentId.clear();
        byName.clear();
        byCollege.setValue(null);
        byMajor.setItems(FXCollections.observableArrayList());
        byMajor.setValue(null);
        byGrade.setValue(null);
        byClassName.setItems(FXCollections.observableArrayList());
        byClassName.setValue(null);
    }
    
	// 从student_info表中检索id 名字必须一样 如果能找到那么就添加信息到student_archive
    @FXML
    void handleAdd(ActionEvent event) {
    	if (currentSelectedStudentId != null) {
			showInfoError("请取消选中的学生");
			return;
		}
		
		String selectedCollegeName = college.getValue();
		String selectedMajorName = major.getValue();
		String selectedClassName = className.getValue();
		
		if (selectedCollegeName == null || selectedCollegeName.isEmpty()) {
			showInfoError("请选择学院");
			return;
		}
		if (selectedMajorName == null || selectedMajorName.isEmpty()) {
			showInfoError("请选择专业");
			return;
		}
		if (selectedClassName == null || selectedClassName.isEmpty()) {
			showInfoError("请选择班级");
			return;
		}
		
		String studentName = this.name.getText().trim();
		String studentId = this.studentId.getText().trim();
		String selectedClassId = campusSession.getClassIdByNames(selectedCollegeName, selectedMajorName, selectedClassName);
		String mentorId = this.mentorId.getText().trim();
		String counsellorId = this.counsellorId.getText().trim();
		LocalDate tmpDate = this.admissionDate.getValue();
		if (tmpDate == null) {
			showInfoError("请选择入学日期");
			return;
		}
		String admissionDate = tmpDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
		String studentStatus = this.studentStatus.getValue();
		
		if (studentName == null || studentName.isEmpty()) {
			showInfoError("请输入正确的学生姓名");
			return;
		}
		if (studentId == null || studentId.isEmpty()) {
			showInfoError("请输入正确的学生Id");
			return;
		}
		if (selectedClassId == null || selectedClassId.isEmpty()) {
			showInfoError("请输入正确的班级");
			return;
		}
		if (mentorId == null || mentorId.isEmpty()) {
			showInfoError("请输入导师ID");
			return;
		}
		if (counsellorId == null || counsellorId.isEmpty()) {
			showInfoError("请输入辅导员ID");
		}
		if (admissionDate == null || admissionDate.isEmpty()) {
			showInfoError("请输入正确的入学年份");
		}
		if (studentStatus == null || studentStatus.isEmpty()) {
			showInfoError("请输入学籍状态");
		}
		
		StudentProfile request = new StudentProfile();
		request.setName(studentName);
		request.setStudentId(studentId);
		request.setClassId(selectedClassId);
		request.setMentorId(mentorId);
		request.setCounsellorId(counsellorId);
		request.setAdmissionDate(admissionDate);
		request.setStudentStatus(studentStatus);
		
		showInfoLoading(true);
		
		Task<StudentProfile> addTask = new Task<>() {
			@Override
			protected StudentProfile call() throws Exception {
				dataOut.writeUTF("AddStudentProfile");
				dataOut.flush();
				
				String jsonRequest = gson.toJson(request);
				dataOut.writeUTF(jsonRequest);
				
				String jsonResponse = dataIn.readUTF();
				if (jsonResponse.contains("\"error\"")) {
					throw new Exception("学生学籍信息添加失败！" + jsonResponse);
				}
				
				StudentProfile response = gson.fromJson(jsonResponse, StudentProfile.class);
				return response;
			}
			
			@Override
			protected void succeeded() {
				super.succeeded();
				showInfoLoading(false);
		
				fillTheBlank(getValue());
				
				showInfoError("学生学籍信息添加成功！", false);
			}
			
			@Override
			protected void failed() {
				super.failed();
				showInfoLoading(false);
				showInfoError(getException().getMessage());
			}
		};
		
		new Thread(addTask).start();
    }

    @FXML
    void handleDeselect(ActionEvent event) {
    	
    	if (currentSelectedStudentId != null) {
        	name.setDisable(false);
        	this.studentId.setDisable(false);
        	confirmButton.setDisable(true);
        	deselectButton.setDisable(true);
        	addButton.setDisable(false);
        	
        	name.clear();
        	studentId.clear();
        	college.setValue(null);
        	major.setValue(null);
        	grade.setValue(null);
        	className.setValue(null);
        	mentor.clear();
        	mentorId.clear();
        	counsellor.clear();
        	counsellorId.clear();
        	levelOfStudy.setValue(null);
        	duration.clear();
        	studentStatus.setValue(null);
            admissionDate.setValue(null);
            expectedGraduationDate.setValue(null);
    	}
    	
    	currentSelectedStudentId = null;
    }

    @FXML
    void handleConfirmChange(ActionEvent event) {
		if (currentSelectedStudentId == null || currentSelectedStudentId.isEmpty() ) {
			showInfoError("当前并未选中学生");
			return;
		}
		if (!currentSelectedStudentId.equals(studentId.getText())) {
			showInfoError("选中学生ID异常！");
			return;
		}
		
		String selectedCollegeName = college.getValue();
		String selectedMajorName = major.getValue();
		String selectedClassName = className.getValue();
		
		if (selectedCollegeName == null || selectedCollegeName.isEmpty()) {
			showInfoError("请选择学院");
			return;
		}
		if (selectedMajorName == null || selectedMajorName.isEmpty()) {
			showInfoError("请选择专业");
			return;
		}
		if (selectedClassName == null || selectedClassName.isEmpty()) {
			showInfoError("请选择班级");
			return;
		}
		
		String selectedClassId = campusSession.getClassIdByNames(selectedCollegeName, selectedMajorName, selectedClassName);
		String mentorId = this.mentorId.getText().trim();
		String counsellorId = this.counsellorId.getText().trim();
		LocalDate tmpDate = this.admissionDate.getValue();
		if (tmpDate == null) {
			showInfoError("请选择入学日期");
			return;
		}
		String admissionDate = tmpDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
		String studentStatus = this.studentStatus.getValue();
		
		if (selectedClassId == null || selectedClassId.isEmpty()) {
			showInfoError("请输入正确的班级");
			return;
		}
		if (mentorId == null || mentorId.isEmpty()) {
			showInfoError("请输入导师ID");
			return;
		}
		if (counsellorId == null || counsellorId.isEmpty()) {
			showInfoError("请输入辅导员ID");
		}
		if (admissionDate == null || admissionDate.isEmpty()) {
			showInfoError("请输入正确的入学年份");
		}
		if (studentStatus == null || studentStatus.isEmpty()) {
			showInfoError("请输入学籍状态");
		}
		
		StudentProfile request = new StudentProfile();
		request.setStudentId(currentSelectedStudentId);
		request.setClassId(selectedClassId);
		request.setMentorId(mentorId);
		request.setCounsellorId(counsellorId);
		request.setAdmissionDate(admissionDate);
		request.setStudentStatus(studentStatus);
		
		showInfoLoading(true);
		
		Task<StudentProfile> updateTask = new Task<>() {
			@Override
			protected StudentProfile call() throws Exception {
				dataOut.writeUTF("UpdateStudentProfile");
				dataOut.flush();
				
				String jsonRequest = gson.toJson(request);
				dataOut.writeUTF(jsonRequest);
				
				String jsonResponse = dataIn.readUTF();
				if (jsonResponse.contains("\"error\"")) {
					throw new Exception("修改失败！" + jsonResponse);
				}
				
				StudentProfile response = gson.fromJson(jsonResponse, StudentProfile.class);
				return response;
			}
			
			@Override
			protected void succeeded() {
				super.succeeded();
				showInfoLoading(false);
				
				fillTheBlank(getValue());
				
				showInfoError("学生学籍信息修改成功！", false);
			}
			
			@Override
			protected void failed() {
				super.failed();
				showInfoLoading(false);
				showInfoError(getException().getMessage());
			}
		};
		
		new Thread(updateTask).start();
    }
        
    // -------------------------- UI 辅助方法 --------------------------
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
    
    private void showInfoLoading(boolean show) {
        Platform.runLater(() -> loadingInfoLabel.setVisible(show));
    }

    private void showInfoError(String msg) {
        showInfoError(msg, true);
    }

    private void showInfoError(String msg, boolean isError) {
        Platform.runLater(() -> {
            errorInfoLabel.setText(msg);
            errorInfoLabel.setStyle(isError ? "-fx-text-fill: #d32f2f;" : "-fx-text-fill: #388e3c;");
            errorInfoLabel.setVisible(true);
            new Thread(() -> {
                try { Thread.sleep(3000); } catch (InterruptedException e) {}
                Platform.runLater(() -> errorInfoLabel.setVisible(false));
            }).start();
        });
    }

    // -------------------------- 学生表格实体类 --------------------------
    public static class StudentListEntry {
    	 // 是不是应该把所有内容都放在这个实体类里面 因为双击的时候需要所有信息
        private final StringProperty studentId;
        private final StringProperty name;
        private final StringProperty college;
        private final StringProperty major;
        private final StringProperty grade;
        private final StringProperty className;

        public StudentListEntry(String studentId, String name, String college, String major, String grade, String className) {
            this.studentId = new SimpleStringProperty(studentId);
            this.name = new SimpleStringProperty(name);
            this.college = new SimpleStringProperty(college);
            this.major = new SimpleStringProperty(major);
            this.grade = new SimpleStringProperty(grade);
            this.className = new SimpleStringProperty(className);
        }

        // Getter（表格绑定用）
        public String getStudentId() { return studentId.get(); }
        public String getName() { return name.get(); }
        public String getCollege() { return college.get(); }
        public String getMajor() { return major.get(); }
        public String getGrade() { return grade.get(); }
        public String getClassId() { return className.get(); }
        
        // Setter
        public void setStudentId(String studentId) { this.studentId.setValue(studentId); }
        public void setName(String name) { this.name.setValue(name); }
        public void setCollege(String college) { this.college.setValue(college); }
        public void setMajor(String major) { this.major.setValue(major); }
        public void setGrade(String grade) { this.grade.setValue(grade); }
        public void setClassName(String className) { this.className.setValue(className); }
        
        // Property方法（预留扩展）
        public StringProperty studentIdProperty() { return studentId; }
        public StringProperty nameProperty() { return name; }
        public StringProperty collegeProperty() { return college; }
        public StringProperty majorProperty() { return major; }
        public StringProperty gradeProperty() { return grade; }
        public StringProperty classIdProperty() { return className; }
    }
}