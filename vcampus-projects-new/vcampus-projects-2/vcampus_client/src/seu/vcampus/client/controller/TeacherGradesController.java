package seu.vcampus.client.controller;

import java.net.URL;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

import com.google.gson.reflect.TypeToken;
import com.google.gson.Gson;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import seu.vcampus.client.util.GradeUtils;
import seu.vcampus.client.network.SocketManager;
import seu.vcampus.client.session.UserSession;
import seu.vcampus.model.StudentGrades;

public class TeacherGradesController implements Initializable {

    // -------------------------- UI 组件 --------------------------
    @FXML private ComboBox<String> selectedCourse;
    @FXML private ComboBox<String> selectedTeachingClass;
    @FXML private TextField name;
    @FXML private TextField studentId;
    @FXML private TextField regularGrades; 
    @FXML private TextField finalGrades;
    @FXML private Button retrieve;
    @FXML private Button inputGrades;
    @FXML private Button clear;
    
    @FXML private TableView<StudentGradeListEntry> studentList;
    @FXML private TableColumn<StudentGradeListEntry, String> studentIdCol;
    @FXML private TableColumn<StudentGradeListEntry, String> nameCol;
    @FXML private TableColumn<StudentGradeListEntry, String> majorCol;
    @FXML private TableColumn<StudentGradeListEntry, String> regularGradesCol;
    @FXML private TableColumn<StudentGradeListEntry, String> finalGradesCol;
    @FXML private TableColumn<StudentGradeListEntry, String> totalGradesCol;
    
    @FXML private Button submit;
    @FXML private Label loadingLabel;
    @FXML private Label errorLabel;
    
    // -------------------------- 数据容器 --------------------------
    private Map<String, List<String>> courseToSections = new HashMap<>();
    private Gson gson = new Gson();
    private DataInputStream dataIn;
    private DataOutputStream dataOut;
    private String currentTeacherId;
    private String currentGradeId = null; // 因为表格内容是String 所以这里也是
    private Integer currentSectionId = null;
    private Integer currentFinalRatio = null; 

    // -------------------------- 初始化方法 --------------------------
    @Override
    public void initialize(URL location, ResourceBundle resources) {
    	currentTeacherId = UserSession.getInstance().getCurrentUserId();
    	
        initNetworkConnections();
        initTableColumns();
                
        initCourseSectionsMap();
        bindCourseSelectionListener();
        setUpDoubleClick();
        
        disableTextField();
    }
    
    private void initNetworkConnections() {        
        try {
            dataIn = SocketManager.getInstance().getIn();
            dataOut = SocketManager.getInstance().getOut();
        } catch (Exception e) {
            showError("获取 Socket 流失败：" + e.getMessage());
        }
    }

    private void initTableColumns() {
        studentIdCol.setCellValueFactory(new PropertyValueFactory<>("studentId"));
        nameCol.setCellValueFactory(new PropertyValueFactory<>("name"));
        majorCol.setCellValueFactory(new PropertyValueFactory<>("major"));
        regularGradesCol.setCellValueFactory(new PropertyValueFactory<>("regularGrade"));
        finalGradesCol.setCellValueFactory(new PropertyValueFactory<>("finalGrade"));
        totalGradesCol.setCellValueFactory(new PropertyValueFactory<>("totalGrade"));
        
        studentList.setItems(FXCollections.observableArrayList());
    }

    private void initCourseSectionsMap() {
        showLoading(true);

        Task<Map<String, List<String>>> initTask = new Task<>() {
            @Override
            protected Map<String, List<String>> call() throws Exception {
            	if (currentTeacherId == null || currentTeacherId.trim().isEmpty()) {
                    throw new Exception("当前教师未登录，无法获取课程信息");
                }
            	
                dataOut.writeUTF("GetCourseSectionsMap");
                dataOut.flush();
                dataOut.writeUTF(currentTeacherId);
                dataOut.flush();
                
                String json = dataIn.readUTF();
                
                if (json.contains("\"error\"")) {
                	throw new Exception("获取课程到教学班的映射失败：" + json);
                }
                
                Type stringToStringListMapType = new TypeToken<Map<String, List<String>>>() {}.getType();
                Map<String, List<String>> result = gson.fromJson(json, stringToStringListMapType);
                
                return result;
            }

            @Override
            protected void succeeded() {
                super.succeeded();
                courseToSections = getValue();
                selectedCourse.setItems(FXCollections.observableArrayList(courseToSections.keySet()));
                selectedCourse.setDisable(false);
                showLoading(false);
            }

            @Override
            protected void failed() {
                super.failed();
                Throwable error = getException();
                showError("加载课程失败：" + (error.getMessage() != null ? error.getMessage() : "未知错误"));
                showLoading(false);
            }
        };

        new Thread(initTask).start();
    }

    private void bindCourseSelectionListener() {
        selectedCourse.setOnAction(e -> {
            String selectedCourseName = selectedCourse.getValue();
            if (selectedCourseName == null) {
                selectedTeachingClass.setItems(FXCollections.observableArrayList());
                return;
            }
            List<String> classes = courseToSections.get(selectedCourseName);
            selectedTeachingClass.setItems(FXCollections.observableArrayList(classes));
            currentSectionId = null;
            handleClear(null); // 假如发生改变 那么取消选中
        });
        
        selectedTeachingClass.setOnAction(e -> {
        	currentSectionId = null; // 以防出现 点击了combobox忘记点检索 发现可以提交 就提交了的情况
        	handleClear(null);
        });
    }
    
    private void disableTextField() {
    	studentId.setDisable(true);
    	name.setDisable(true);
    }
    
    private void setUpDoubleClick() {
        studentList.setRowFactory(tv -> {
            TableRow<StudentGradeListEntry> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty()) {
                    handleDoubleClick(row.getItem());
                }
            });
            return row;
        });
    }
    
    private void handleDoubleClick(StudentGradeListEntry entry) {
    	currentGradeId = entry.getGradeId();
        name.setText(entry.getName() == null ? "" : entry.getName());
        studentId.setText(entry.getStudentId() == null ? "" : entry.getStudentId());
        regularGrades.setText(entry.getRegularGrade() == null ? "" : entry.getRegularGrade());
        finalGrades.setText(entry.getFinalGrade() == null ? "" : entry.getFinalGrade());
    }
    
    @FXML
    void handleRetrieve(ActionEvent event) {
        String selectedCourseName = selectedCourse.getValue();
        String selectedClassName = selectedTeachingClass.getValue();
        showLoading(true);
        retrieve.setDisable(true);        
        
        Task<List<StudentGrades>> loadTask = new Task<>() {
        	@Override
        	protected List<StudentGrades> call() throws Exception {
        		StudentGrades request = new StudentGrades();
        		request.setTeacherId(currentTeacherId);
        		request.setCourseName(selectedCourseName);
        		request.setClassTime(selectedClassName);
        		String jsonRequest = gson.toJson(request);
        		
        		dataOut.writeUTF("GetStudentGradeListByTeacher");
        		dataOut.flush();
        		dataOut.writeUTF(jsonRequest);
        		
        		String jsonResponse = dataIn.readUTF();
        		
        		if (jsonResponse.contains("\"error\"")) {
        			throw new Exception("从服务端获取学生列表失败" + jsonResponse);
        		}
        		
        		Type studentGradeListType = new TypeToken<List<StudentGrades>>() {}.getType();
        		List<StudentGrades> response = gson.fromJson(jsonResponse, studentGradeListType);
        		
        		return response;
        	}
        	
        	@Override
        	protected void succeeded() {
        		super.succeeded();
        		showLoading(false);
                retrieve.setDisable(false);
        		List<StudentGrades> entries = getValue();
        		currentFinalRatio = entries.isEmpty() ? null : entries.getFirst().getFinalRatio();
        		if (entries.isEmpty()) {
        			submit.setDisable(true);
        			inputGrades.setDisable(true);
        			currentSectionId = null;
        		} else {
        			StudentGrades tmp = entries.getFirst();
        			if (tmp.getSectionStatus().equals("未提交")) {
        				submit.setDisable(false);
        				inputGrades.setDisable(false);
        				currentSectionId = tmp.getSectionId();
        			} else {
        				submit.setDisable(true);
        				inputGrades.setDisable(true);
        				currentSectionId = tmp.getSectionId();
        			}
        		}
        		ObservableList<StudentGradeListEntry> content = FXCollections.observableArrayList();
        		for (StudentGrades entry : entries) {
        			String regularGrade = GradeUtils.formatBigDecimal(entry.getRegularGrade());
        			String finalGrade = GradeUtils.formatBigDecimal(entry.getFinalGrade());
        			String totalGrade = GradeUtils.formatBigDecimal(entry.getTotalGrade());
        			
        			content.add( new StudentGradeListEntry(
        				entry.getGradeId().toString(),
        				entry.getStudentId(),
        				entry.getStudentName(),
        				entry.getMajor(),
        				regularGrade != null ? regularGrade : "",
        				finalGrade != null ? finalGrade : "",
        				totalGrade != null ? totalGrade : ""
        			));
        		}
        		
        		studentList.setItems(content);
        	}
        	
        	@Override
        	protected void failed() {
        		super.failed();
        		Throwable error = getException();
                showError("加载课程失败：" + (error.getMessage() != null ? error.getMessage() : "未知错误"));
                showLoading(false);
                retrieve.setDisable(false);
        	}
        };
        
        new Thread(loadTask).start();
    }

    @FXML
    void handleClear(ActionEvent event) {
    	currentGradeId = null;
        name.clear();
        studentId.clear();
        regularGrades.clear();
        finalGrades.clear();
    }

    @FXML
    void handleInputGrades(ActionEvent event) {
    	 if (currentGradeId == null || currentGradeId.trim().isEmpty()) {
    		 showError("请先双击表格选择需要录入成绩的学生");
    		 return;
    	 }
    	 if (studentId.getText() == null || studentId.getText().trim().isEmpty()) {
    		 showError("学生ID异常，请重新选择学生");
    	     return;
    	 }
    	 if (currentFinalRatio == null) {
    	     showError("未获取到课程评分规则，请重新查询课程");
    	     return;
    	 }
    	
    	 String regularGradeStr = regularGrades.getText().trim();
    	 String finalGradeStr = finalGrades.getText().trim();
    	 
    	 if (regularGradeStr.isEmpty()) {
    		 showError("请输入平时成绩");
    		 return;
    	 }
    	 if (finalGradeStr.isEmpty()) {
    		 showError("请输入期末成绩");
    		 return;
    	 }
    	 
    	 Double regularGradeDouble = null;
    	 Double finalGradeDouble = null;
    	 try {
    		 // 平时成绩转换+范围校验
    		 regularGradeDouble = Double.parseDouble(regularGradeStr);
    		 if (regularGradeDouble < 0 || regularGradeDouble > 100) {
    			 throw new IllegalArgumentException("平时成绩需在0-100之间");
    		 }
    		 // 期末成绩转换+范围校验
    		 finalGradeDouble = Double.parseDouble(finalGradeStr);
    		 if (finalGradeDouble < 0 || finalGradeDouble > 100) {
    			 throw new IllegalArgumentException("期末成绩需在0-100之间");
    		 }
    	 } catch (NumberFormatException e) {
    		 showError("成绩格式错误，请输入数字（如85.5）");
    		 return;
    	 } catch (IllegalArgumentException e) {
    		 showError(e.getMessage());
    		 return;
    	 }
    	 
    	 BigDecimal regularGrade = BigDecimal.valueOf(regularGradeDouble);
    	 BigDecimal finalGrade = BigDecimal.valueOf(finalGradeDouble);
    	 BigDecimal totalGrade = GradeUtils.calculateTotalGrade(regularGrade, finalGrade, currentFinalRatio);
    
    	 StudentGrades request = new StudentGrades();
    	 request.setGradeId(Integer.parseInt(currentGradeId)); // 成绩记录ID（更新依据）
    	 request.setStudentId(studentId.getText().trim());     // 学生ID（二次校验）
    	 request.setRegularGrade(regularGrade);               // 平时成绩（BigDecimal）
    	 request.setFinalGrade(finalGrade);                   // 期末成绩（BigDecimal）
    	 request.setTotalGrade(totalGrade);                   // 总成绩（自动计算）
    	 // 冗余校验
    	 request.setTeacherId(currentTeacherId);
    	 request.setCourseName(selectedCourse.getValue());
    	 request.setClassTime(selectedTeachingClass.getValue()); 
    
    	 inputGrades.setDisable(true);
    	 selectedCourse.setDisable(true);
    	 selectedTeachingClass.setDisable(true);
    	 showLoading(true);
    	 
    	 Task<Boolean> updateTask = new Task<>() {
    		 @Override
    		 protected Boolean call() throws Exception {
    			 dataOut.writeUTF("UpdateStudentGrade");
    			 dataOut.flush();
    			 
    			 String jsonRequest = gson.toJson(request);
    			 dataOut.writeUTF(jsonRequest);
    			 dataOut.flush();
    			 
    			 String jsonResponse = dataIn.readUTF();
    			 if (jsonResponse.contains("\"error\"")) {
                     throw new Exception("成绩录入失败：" + jsonResponse);
                 }
    			 return true;
    		 }
    		 
    		 @Override
    		 protected void succeeded() {
    			 super.succeeded();
    			 showLoading(false);
    			 inputGrades.setDisable(false);
    	    	 selectedCourse.setDisable(false);
    	    	 selectedTeachingClass.setDisable(false);
    			 
    			 handleClear(null);
    			 showError("成绩录入成功", false);
    			 
    			 if (selectedCourse.getValue() != null && selectedTeachingClass.getValue() != null) {
    				 handleRetrieve(null); // 无事件参数，直接调用查询逻辑
    	         } 
    		 }
    		 
    		 @Override
    	     protected void failed() {
    	         super.failed();
    	         showLoading(false);
    	         inputGrades.setDisable(false);
    	    	 selectedCourse.setDisable(false);
    	    	 selectedTeachingClass.setDisable(false);

    	         // 失败后提示错误
    	         Throwable error = getException();
    	         String errorMsg = error.getMessage() != null ? error.getMessage() : "成绩录入失败，未知错误";
    	         showError(errorMsg);
    	     }
    	 };
    	 
    	 new Thread(updateTask).start();
    }

    @FXML
    void handleSubmit(ActionEvent event) {
        if (currentSectionId == null) {
        	showError("未获取到教学班信息，请重新查询课程");
        	return;
        }
        
        ObservableList<StudentGradeListEntry> studentEntries = studentList.getItems();
        if (studentEntries.isEmpty()) {
            showError("当前教学班无学生成绩数据，无需提交");
            return;
        }
        
        if (submit.isDisabled()) {
            showError("当前教学班已提交，不可重复操作");
            return;
        }
        
        List<String> missingTotalGradeStudents = new ArrayList<>();
        for (StudentGradeListEntry entry : studentEntries) {
            String totalGrade = entry.getTotalGrade();
            // 判定条件：总成绩为空字符串、null，或内容为"null"（避免序列化残留）
            if (totalGrade == null || totalGrade.trim().isEmpty() || "null".equals(totalGrade.trim())) {
                // 记录缺少总成绩的学生（学号+姓名，便于用户定位）
                missingTotalGradeStudents.add(entry.getStudentId() + "（" + entry.getName() + "）");
            }
        }
        
        if (!missingTotalGradeStudents.isEmpty()) {
            // showError("以下学生缺少总成绩，请先补录：\n" + String.join("、", missingTotalGradeStudents));
        	showError("部分学生缺少总成绩，请先补录");
            return;
        }
        
        submit.setDisable(true);
        inputGrades.setDisable(true);
        selectedCourse.setDisable(true);
        selectedTeachingClass.setDisable(true);
        showLoading(true);
        
        StudentGrades submitRequest = new StudentGrades();
        submitRequest.setSectionId(currentSectionId);          // 目标教学班ID（核心更新依据）
        submitRequest.setTeacherId(currentTeacherId); // 提交教师ID（权限校验）
        submitRequest.setCourseName(selectedCourse.getValue()); // 课程名（冗余校验，避免篡改）
        submitRequest.setClassTime(selectedTeachingClass.getValue()); // 上课时段（冗余校验）
        
        Task<Boolean> submitTask = new Task<>() {
            @Override
            protected Boolean call() throws Exception {
                try {
                    // 发送"提交教学班成绩"指令（与服务端约定指令名）
                    dataOut.writeUTF("SubmitCourseSectionStatus");
                    dataOut.flush();

                    // 序列化请求参数为JSON
                    String jsonRequest = gson.toJson(submitRequest);
                    dataOut.writeUTF(jsonRequest);
                    dataOut.flush();

                    // 接收服务端响应
                    String jsonResponse = dataIn.readUTF();
                    if (jsonResponse.contains("\"error\"")) {
                        // 提取服务端具体错误信息（如"无权限提交"）
                        throw new Exception("成绩提交失败：" + jsonResponse);
                    }

                    // 无错误则表示提交成功
                    return true;
                } catch (IOException e) {
                    // 单独捕获网络异常，提示更明确
                    throw new Exception("网络连接中断：" + e.getMessage());
                }
            }

            @Override
            protected void succeeded() {
                super.succeeded();
                showLoading(false);
                // 恢复部分UI组件（课程选择可重新操作，但提交/录入按钮保持禁用）
                selectedCourse.setDisable(false);
                selectedTeachingClass.setDisable(false);

                // 提交成功后UI处理：提示+清空输入+刷新表格（更新按钮状态）
                handleClear(null);
                showError("成绩提交成功！当前教学班已锁定，不可再修改成绩", false); // 绿色成功提示

                // 重新查询数据，让表格按钮状态同步为"已提交"（禁用inputGrades和submit）
                if (selectedCourse.getValue() != null && selectedTeachingClass.getValue() != null) {
                    handleRetrieve(null);
                }
            }

            @Override
            protected void failed() {
                super.failed();
                showLoading(false);
                // 提交失败，恢复所有UI组件（允许用户重新尝试）
                submit.setDisable(false);
                inputGrades.setDisable(false);
                selectedCourse.setDisable(false);
                selectedTeachingClass.setDisable(false);

                // 提示失败原因
                Throwable error = getException();
                String errorMsg = error.getMessage() != null ? error.getMessage() : "成绩提交失败，未知错误";
                showError(errorMsg);
            }
        };

        // 启动异步任务
        new Thread(submitTask).start();
        
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

    // -------------------------- 学生成绩实体类（不变） --------------------------
    public static class StudentGradeListEntry {
    	private final StringProperty gradeId;
        private final StringProperty studentId;
        private final StringProperty name;
        private final StringProperty major;
        private final StringProperty regularGrade;
        private final StringProperty finalGrade;
        private final StringProperty totalGrade;

        public StudentGradeListEntry(String gradeId, String studentId, String name, String major, 
        		String regularGrades, String finalGrades, String totalGrades) {
        	this.gradeId = new SimpleStringProperty(gradeId);
            this.studentId = new SimpleStringProperty(studentId);
            this.name = new SimpleStringProperty(name);
            this.major = new SimpleStringProperty(major);
            this.regularGrade = new SimpleStringProperty(regularGrades);
            this.finalGrade = new SimpleStringProperty(finalGrades);
            this.totalGrade = new SimpleStringProperty(totalGrades);
        }
        
        public String getGradeId() { return gradeId.get(); }
        public String getStudentId() { return studentId.get(); }
        public String getName() { return name.get(); }
        public String getMajor() { return major.get(); }
        public String getRegularGrade() { return regularGrade.get(); }
        public String getFinalGrade() { return finalGrade.get(); }
        public String getTotalGrade() { return totalGrade.get(); }
        
        public void setGradeId(String gradeId) { this.gradeId.setValue(gradeId); }
        public void setStudentId(String studentId) { this.studentId.setValue(studentId); }
        public void setName(String name)  { this.name.setValue(name); }
        public void setMajor(String major) { this.major.setValue(major); }
        public void setRegularGrade(String regularGrades) { this.regularGrade.setValue(regularGrades); }
        public void setFinalGrade(String finalGrades) { this.finalGrade.setValue(finalGrades); }
        public void setTotalGrade(String totalGrades) { this.totalGrade.setValue(totalGrades); }
        
        public StringProperty gradeIdProperty() { return gradeId; }
        public StringProperty studentIdProperty() { return studentId; }
        public StringProperty nameProperty() { return name; }
        public StringProperty majorProperty() { return major; }
        public StringProperty regularGradeProperty() { return regularGrade; } 
        public StringProperty finalGradeProperty() { return finalGrade; }
        public StringProperty totalGradeProperty() { return totalGrade; }
    }
}