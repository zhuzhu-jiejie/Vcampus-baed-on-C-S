package seu.vcampus.client.controller;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.lang.reflect.Type;
import java.math.BigDecimal;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.event.ActionEvent;
import javafx.fxml.Initializable;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.concurrent.Task;
import javafx.application.Platform;

import seu.vcampus.client.network.SocketManager;
import seu.vcampus.client.session.UserSession;
import seu.vcampus.client.session.CampusSession;
import seu.vcampus.model.StudentGrades;
import seu.vcampus.model.StudentProfile;
import seu.vcampus.client.util.SemesterConverter;
import seu.vcampus.client.util.GradeUtils;

public class StudentGradesController implements Initializable {
    @FXML private ComboBox<String> semesterSelectionComboBox;
	
    @FXML private Label semesterGPA;
    @FXML private Label semesterCreditsEarned;
    @FXML private Label totalGPA;
    @FXML private Label totalCreditsEarned;
    
    @FXML private TextField searchBox;
    @FXML private Button search;
    
    @FXML private TableView<GradesListEntry> gradesList;
    @FXML private TableColumn<GradesListEntry, String> courseNameCol;
    @FXML private TableColumn<GradesListEntry, String> courseIdCol;
    @FXML private TableColumn<GradesListEntry, Double> creditCol;
    @FXML private TableColumn<GradesListEntry, Double> gradesCol;
    @FXML private TableColumn<GradesListEntry, Double> gpaCol;
    @FXML private TableColumn<GradesListEntry, String> teacherCol;
    
    @FXML private Label loadingLabel;
    @FXML private Label errorLabel;
    
    private static final Gson gson = new Gson();
    private DataInputStream dataIn;
    private DataOutputStream dataOut;
    private String currentStudentId = UserSession.getInstance().getCurrentUserId();
    private CampusSession campusSession = CampusSession.getInstance();
    private ObservableList<GradesListEntry> allGrades = FXCollections.observableArrayList(); // 存储当前学期所有成绩，用于搜索
    
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        initNetworkConnections();
        
        initSemesterSelectionComboBox();
        initTotalStatistics();
        
        initSearchBar();
        initGradesList();   
    }
        
    private void initNetworkConnections() { 
        try {
            dataIn = SocketManager.getInstance().getIn();
            dataOut = SocketManager.getInstance().getOut();
        } catch (Exception e) {
            showError("获取 Socket 流失败：" + e.getMessage());
        }
    }
    
    private void initSemesterSelectionComboBox() {
        Task<StudentProfile> loadTask = new Task<>() {
    		@Override
    		protected StudentProfile call() throws Exception {	
    			semesterSelectionComboBox.setDisable(true);
    			
    			dataOut.writeUTF("GetStudentProfile");
    			dataOut.flush();
    			
    			String  jsonRsp = dataIn.readUTF(); // TODO 这边的读取有问题！
    			
    			if (jsonRsp.contains("\"error\"")) {
                    throw new Exception("获取学生信息失败：" + jsonRsp);
                }
    			
    			StudentProfile studentProfile = gson.fromJson(jsonRsp, StudentProfile.class);
    			
    			return studentProfile;
    		}
    		
    		@Override
    		protected void succeeded() {
    			super.succeeded();
    			semesterSelectionComboBox.setDisable(false);
    			StudentProfile studentProfile = getValue();
    			ObservableList<String> semesters = FXCollections.observableArrayList();
    			LocalDate admissionDateValue = LocalDate.parse(studentProfile.getAdmissionDate(), DateTimeFormatter.ISO_DATE);
    			int beginYear = admissionDateValue.getYear();
    			for (int i = beginYear; i <= campusSession.getServerDate().getYear(); ++i) {
    				semesters.add(i + "-" + (i + 1) + "-" + "第一学期");
    				semesters.add(i + "-" + (i + 1) + "-" + "第二学期");
    				semesters.add(i + "-" + (i + 1) + "-" + "第三学期");
    			}
    			semesterSelectionComboBox.setItems(semesters);
    			semesterSelectionComboBox.setValue(SemesterConverter.toChinese(getCurrentSemester(campusSession.getServerDate())));
    		}
    		
    		@Override
    		protected void failed() {
    			super.failed();
    			semesterSelectionComboBox.setDisable(false);
    			Throwable error = getException(); 
    			System.out.println("加载学生学籍信息失败" + (error.getMessage() != null ? error.getMessage() : "未知错误"));
    		}
    	};
    	new Thread(loadTask).start();
    }
    
    private void initTotalStatistics() {
    	showLoading(true);
        StudentGrades request = new StudentGrades();
        request.setStudentId(currentStudentId);
        
        Task<List<StudentGrades>> totalTask = new Task<>() {
            @Override
            protected List<StudentGrades> call() throws Exception {
                dataOut.writeUTF("GetStudentGradeAllSemester"); // 获取改名学生所有 审核“已通过”的成绩
                dataOut.flush();
                
                String jsonRequest = gson.toJson(request);
                dataOut.writeUTF(jsonRequest);
                dataOut.flush();
                
                String jsonResponse = dataIn.readUTF();
                if (jsonResponse.contains("\"error\"")) {
                    throw new Exception("获取所有成绩失败：" + jsonResponse);
                }
                
                Type studentGradesListType = new TypeToken<List<StudentGrades>>() {}.getType();
                return gson.fromJson(jsonResponse, studentGradesListType);
            }
            
            @Override
            protected void succeeded() {
                super.succeeded();
                List<StudentGrades> allSemesterGrades = getValue();
                
                if (allSemesterGrades == null || allSemesterGrades.isEmpty()) {
                    totalGPA.setText("0.00");
                    totalCreditsEarned.setText("0.0");
                    showLoading(false);
                    return;
                }
                
                // 计算总GPA和总获得学分
                double totalQualityPoints = 0.0;
                double totalCredits = 0.0;
                
                for (StudentGrades grade : allSemesterGrades) {
                    // 只统计已通过的课程（成绩>=60分）
                    if (grade.getTotalGrade() != null && grade.getTotalGrade().compareTo(new BigDecimal(60)) >= 0) {
                        BigDecimal credit = grade.getCredit() != null ? grade.getCredit() : BigDecimal.ZERO;
                        String gpa = gradeToGpa(grade.getTotalGrade());
                        
                        totalQualityPoints += Double.parseDouble(gpa) * credit.doubleValue();
                        totalCredits += credit.doubleValue();
                    }
                }
                
                // 计算总GPA（学分加权平均）
                double overallGPA = totalCredits > 0 ? totalQualityPoints / totalCredits : 0.0;
                
                // 更新UI
                totalGPA.setText(String.format("%.2f", overallGPA));
                totalCreditsEarned.setText(String.format("%.1f", totalCredits));
                
                showLoading(false);
            }
            
            @Override
            protected void failed() {
                super.failed();
                showError("获取总统计信息失败：" + getException().getMessage());
                showLoading(false);
            }
        };
        
        new Thread(totalTask).start();
    }
    
    private void initSearchBar() {
        searchBox.setDisable(true);
        search.setDisable(true);
    }
    
    private void initGradesList() {
        courseNameCol.setCellValueFactory(new PropertyValueFactory<>("courseName"));
        courseIdCol.setCellValueFactory(new PropertyValueFactory<>("courseId"));
        creditCol.setCellValueFactory(new PropertyValueFactory<>("credit"));
        gradesCol.setCellValueFactory(new PropertyValueFactory<>("grades"));
        gpaCol.setCellValueFactory(new PropertyValueFactory<>("gpa"));
        teacherCol.setCellValueFactory(new PropertyValueFactory<>("teacher"));
        
        gradesList.setItems(FXCollections.observableArrayList());
    }

    // 获取当前学期（格式：YYYY-YYYY-X，如2025-2026-1
    private String getCurrentSemester(LocalDate nowDate) {
        int year = nowDate.getYear();
        int month = nowDate.getMonthValue();
        int day = nowDate.getDayOfMonth();

        // 第一学期（暑期短学期）：8月20日 - 9月30日（含4周教学+10天过渡用于老师填成绩）
        if (month == 8 && day >= 20) return year + "-" + (year + 1) + "-1";
        if (month == 9) return year + "-" + (year + 1) + "-1";

        // 第二学期（秋季学期）：10月1日 - 次年2月28日（含教学+寒假过渡期用于老师填成绩）
        if (month == 10 || month == 11 || month == 12) 
            return year + "-" + (year + 1) + "-2";
        if (month == 1 || month == 2) 
            return (year - 1) + "-" + year + "-2";

        // 第三学期（春季学期）：3月1日 - 8月19日（含教学+暑假过渡期用于老师填成绩）
        if (month >= 3 && month <= 7) 
            return (year - 1) + "-" + year + "-3";
        if (month == 8 && day < 20) 
            return (year - 1) + "-" + year + "-3";

        // 默认返回（理论上不会执行）
        return (year - 1) + "-" + year + "-3"; 
    }
    
    // 计算并显示学期统计信息
    private void calculateAndShowSemesterStatistics(List<GradesListEntry> grades) {
    	 if (grades == null || grades.isEmpty()) {
             semesterGPA.setText("0.00");
             semesterCreditsEarned.setText("0.0");
             return;
         }
         
         double totalQualityPoints = 0.0;
         double totalCredits = 0.0;
         
         for (GradesListEntry entry : grades) {
             try {
                 // 只统计已通过的课程（成绩>=60分）
                 double grade = Double.parseDouble(entry.getGrades());
                 if (grade >= 60) {
                     double credit = Double.parseDouble(entry.getCredit());
                     double gpa = Double.parseDouble(entry.getGpa());
                     
                     totalQualityPoints += gpa * credit;
                     totalCredits += credit;
                 }
             } catch (NumberFormatException e) {
                 // String转Double可能出现格式错误
                 System.err.println("成绩格式错误: " + e.getMessage());
             }
         }
         
         // 计算学期GPA（学分加权平均）
         double semesterGPAValue = totalCredits > 0 ? totalQualityPoints / totalCredits : 0.0;
         
         // 更新UI显示，保留两位小数
         semesterGPA.setText(String.format("%.2f", semesterGPAValue));
         semesterCreditsEarned.setText(String.format("%.1f", totalCredits));
    }
    
    // 清空成绩数据和统计信息
    private void clearGradeData() {
        gradesList.getItems().clear();
        allGrades.clear();
        semesterGPA.setText("0.00");
        semesterCreditsEarned.setText("0.0");
    }

    @FXML
    void handleSemesterSelection(ActionEvent event) {
        String selectedSemester = semesterSelectionComboBox.getValue();
        if (selectedSemester == null || selectedSemester.isEmpty()) {
            showError("请选择一个学期");
            return;
        }
        
        showLoading(true);
        clearGradeData(); // 清空之前的数据
        
        StudentGrades request = new StudentGrades();
        request.setStudentId(UserSession.getInstance().getCurrentUserId());
        request.setSemester(SemesterConverter.toNumber(selectedSemester));
        
        // 根据年级和学生id将所有当前年级的所有通过审核的成绩的列表
        Task<List<StudentGrades>> gradesTask = new Task<>() {
            @Override
            protected List<StudentGrades> call() throws Exception {
            	dataOut.writeUTF("GetStudentSemesterGrade");
            	dataOut.flush();
            	
            	String jsonRequest = gson.toJson(request);
            	dataOut.writeUTF(jsonRequest);
            	dataOut.flush();
            	
            	String jsonResponse = dataIn.readUTF();
            	if (jsonResponse.contains("\"error\"")) {
            		throw new Exception("获取本人学期成绩失败" + jsonResponse);
            	}
            	
            	Type studentGradesListType = new TypeToken<List<StudentGrades>>() {}.getType();
            	List<StudentGrades> response = gson.fromJson(jsonResponse, studentGradesListType);
            	
            	return response;
            }
            
            @Override
            protected void succeeded() {
                super.succeeded();
                search.setDisable(false);
                searchBox.setDisable(false);
                
                List<StudentGrades> serverGrades = getValue();
                if (serverGrades == null || serverGrades.isEmpty()) {
                    showError("该学期暂无成绩记录", false);
                    showLoading(false);
                    return;
                }
                
                allGrades = FXCollections.observableArrayList();
                for (StudentGrades grade : serverGrades) {
                	String gpa = gradeToGpa(grade.getTotalGrade());
                	allGrades.add(
                		new GradesListEntry(
                			grade.getCourseName(),
                			grade.getCourseId(),
                			GradeUtils.formatBigDecimal(grade.getCredit()),
                			GradeUtils.formatBigDecimal(grade.getTotalGrade()),
                			gpa,
                			grade.getTeacherName()
                		)	
                	);
                }
 
                gradesList.setItems(allGrades);
                calculateAndShowSemesterStatistics(allGrades);
                
                showLoading(false);
            }
            
            @Override
            protected void failed() {
                super.failed();
                showError("获取成绩失败：" + getException().getMessage());
                showLoading(false);
            }
        };
         
        new Thread(gradesTask).start();
    }
    
    private String gradeToGpa(BigDecimal score) {
        if (score == null) {
            return "0.0";
        }
        
        double grade = score.doubleValue();
        
        if (grade >= 96) {
            return "4.8";
        } else if (grade >= 93) {
            return "4.5";
        } else if (grade >= 90) {
            return "4.0";
        } else if (grade >= 86) {
            return "3.8";
        } else if (grade >= 83) {
            return "3.5";
        } else if (grade >= 80) {
            return "3.0";
        } else if (grade >= 76) {
            return "2.8";
        } else if (grade >= 73) {
            return "2.5";
        } else if (grade >= 70) {
            return "2.0";
        } else if (grade >= 66) {
        	return "1.8";
        } else if (grade >= 63) {
        	return "1.5";
        } else if (grade >= 60) {
        	return "1.0";
        } else {
            return "0.0"; // 不及格
        }
    }
    
    @FXML
    void handleSearchButton(ActionEvent event) {
    	String searchText = searchBox.getText().trim().toLowerCase();
        
        if (searchText.isEmpty()) {
            // 搜索内容为空时显示所有成绩
            gradesList.setItems(allGrades);
            // 重新计算统计信息
            calculateAndShowSemesterStatistics(allGrades);
            return;
        }
        
        // 根据课程名或课程号搜索
        ObservableList<GradesListEntry> filtered = allGrades.stream()
            .filter(entry -> entry.getCourseName().toLowerCase().contains(searchText) || 
                           entry.getCourseId().toLowerCase().contains(searchText))
            .collect(Collectors.toCollection(FXCollections::observableArrayList));
        
        gradesList.setItems(filtered);
        // 计算搜索结果的统计信息
        // calculateAndShowSemesterStatistics(filtered);
        
        if (filtered.isEmpty()) {
            showError("未找到匹配的课程", false);
        }
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
            
            // 3秒后自动隐藏错误信息
            new Thread(() -> {
                try { Thread.sleep(3000); } 
                catch (InterruptedException e) { Thread.currentThread().interrupt(); }
                
                Platform.runLater(() -> errorLabel.setVisible(false));
            }).start();
        });
    }

    // 表格数据实体类
    public static class GradesListEntry {
        private final StringProperty credit;
        private final StringProperty grades;
        private final StringProperty gpa;
        private final StringProperty courseName;
        private final StringProperty courseId;
        private final StringProperty teacher;
        
        public GradesListEntry(String courseName, String courseId, String credit, 
        		String grades, String gpa, String teacher) {
            this.courseName = new SimpleStringProperty(courseName);
            this.courseId = new SimpleStringProperty(courseId);
            this.credit = new SimpleStringProperty(credit);
            this.grades = new SimpleStringProperty(grades);
            this.gpa = new SimpleStringProperty(gpa);
            this.teacher = new SimpleStringProperty(teacher);
        }

        // Getter 方法（JavaFX 属性绑定需要）
        public String getCredit() { return credit.get(); }
        public String getGrades() { return grades.get(); }
        public String getGpa() { return gpa.get(); }
        public String getCourseName() { return courseName.get(); }
        public String getCourseId() { return courseId.get(); }
        public String getTeacher() { return teacher.get(); }
        
        // Setter
        public void setCredit(String credit) { this.credit.setValue(credit); }
        public void setGrades(String grades) { this.grades.setValue(grades); }
        public void setGpa(String gpa) { this.gpa.setValue(gpa); }
        public void setCourseName(String courseName) { this.courseName.setValue(courseName); }
        public void setCourseId(String courseId) { this.courseId.setValue(courseId); }
        public void setTeacher(String teacher) { this.teacher.setValue(teacher); }
        
        // 属性访问方法（用于高级绑定）
        public StringProperty creditProperty() { return credit; }
        public StringProperty gradesProperty() { return grades; }
        public StringProperty gpaProperty() { return gpa; }
        public StringProperty courseNameProperty() { return courseName; }
        public StringProperty courseIdProperty() { return courseId; }
        public StringProperty teacherProperty() { return teacher; }
    }    
}
    