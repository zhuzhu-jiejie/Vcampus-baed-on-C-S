package seu.vcampus.client.controller;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.URL;
import java.time.LocalDate;
import java.util.List;
import java.util.ResourceBundle;
import java.lang.reflect.Type;
import java.math.BigDecimal;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TableView;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TextField;
import javafx.beans.property.StringProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.event.ActionEvent;
import javafx.concurrent.Task;

import seu.vcampus.client.session.CampusSession;
import seu.vcampus.client.util.GradeUtils;
import seu.vcampus.client.util.SemesterConverter;
import seu.vcampus.model.StudentGrades;
import seu.vcampus.client.network.SocketManager;

public class AdminGradesController implements Initializable {
	@FXML private ScrollPane scrollPane;
	
    @FXML private ComboBox<String> bySemester;
    @FXML private TextField byCourseId;
    @FXML private TextField byCourseName;
    @FXML private TextField byTeacher;
    @FXML private ComboBox<String> byStatus;
    @FXML private Button retrieveCourse;
    @FXML private Button conditionClearButton;
    
    @FXML private Label loadingCourseLabel;
    @FXML private Label errorCourseLabel;
    @FXML private TableView<CourseListEntry> courseList;
    @FXML private TableColumn<CourseListEntry, String> semesterCol;
    @FXML private TableColumn<CourseListEntry, String> courseIdCol;
    @FXML private TableColumn<CourseListEntry, String> courseNameCol;
    @FXML private TableColumn<CourseListEntry, String> teacherCol;
    @FXML private TableColumn<CourseListEntry, String> scheduleCol;
    @FXML private TableColumn<CourseListEntry, String> statusCol;
    
    @FXML private Label selectedCourse;
    @FXML private Label selectedTeacher;
    @FXML private TextField name;
    @FXML private TextField studentId;
    @FXML private TextField regularGrades;
    @FXML private TextField finalGrades;
    @FXML private Button passButton;
    @FXML private Button clearButton;
    @FXML private Button reviseGrade;

    @FXML private Label loadingGradesLabel;
    @FXML private Label errorGradesLabel;
    @FXML private TableView<GradesListEntry> gradesList;
    @FXML private TableColumn<GradesListEntry, String> studentIdCol;
    @FXML private TableColumn<GradesListEntry, String> nameCol;
    @FXML private TableColumn<GradesListEntry, String> majorCol;
    @FXML private TableColumn<GradesListEntry, Double> regularGradesCol;
    @FXML private TableColumn<GradesListEntry, Double> finalGradesCol;
    @FXML private TableColumn<GradesListEntry, Double> totalGradesCol;
    @FXML private Button deselectButton;
    
    private Gson gson = new Gson();
    private DataOutputStream dataOut = SocketManager.getInstance().getOut();
    private DataInputStream dataIn = SocketManager.getInstance().getIn();
    
    // 新增：维护当前选中的教学班ID（双击课程表项后赋值）
    private Integer currentSectionId = null;
    // 新增：维护当前选中的课程名和教师名（用于UI显示）
    private String currentCourseName = null;
    private String currentTeacherName = null;
    // 新增：维护当前选中的成绩ID（双击成绩表项后赋值）
    private String currentGradeId = null;
    // 新增：维护当前教学班的成绩占比（平时/期末占比，用于计算总成绩）
    private Integer currentFinalRatio = null;
    

    @Override
    public void initialize(URL location, ResourceBundle resources) {
    	initCourseList();
    	initGradesList();
    	initSemesterComboBox();
    	initStatusComboBox();
    	bindCourseListDoubleClick();
    	bindGradesListDoubleClick();
    	name.setDisable(true);
    	studentId.setDisable(true);
    	passButton.setDisable(true);
    }
    
    private void initCourseList() {
    	semesterCol.setCellValueFactory(new PropertyValueFactory<>("semester"));
    	courseIdCol.setCellValueFactory(new PropertyValueFactory<>("courseId"));
    	courseNameCol.setCellValueFactory(new PropertyValueFactory<>("courseName"));
    	teacherCol.setCellValueFactory(new PropertyValueFactory<>("teacher"));
    	scheduleCol.setCellValueFactory(new PropertyValueFactory<>("schedule"));
    	statusCol.setCellValueFactory(new PropertyValueFactory<>("status"));
 
    	courseList.setItems(FXCollections.observableArrayList());
    }
    
    private void initGradesList() {
    	studentIdCol.setCellValueFactory(new PropertyValueFactory<>("studentId"));
    	nameCol.setCellValueFactory(new PropertyValueFactory<>("name"));
    	majorCol.setCellValueFactory(new PropertyValueFactory<>("major"));
    	regularGradesCol.setCellValueFactory(new PropertyValueFactory<>("regularGrades"));
    	finalGradesCol.setCellValueFactory(new PropertyValueFactory<>("finalGrades"));
    	totalGradesCol.setCellValueFactory(new PropertyValueFactory<>("totalGrades"));

    	gradesList.setItems(FXCollections.observableArrayList());
    }
    
    private void initSemesterComboBox() {
        LocalDate currentDate = CampusSession.getInstance().getServerDate();
        if (currentDate == null) {
            currentDate = LocalDate.now();
            System.out.println("CampusSession未获取到时间，使用系统当前时间：" + currentDate);
        }
        
        int currentYear = currentDate.getYear(); // 当前年份（如2024）
        int startYear = currentYear - 3;
        ObservableList<String> semesterList = FXCollections.observableArrayList();

        // 3. 遍历每个学年，生成3个学期（第一/第二/第三学期）
        for (int year = startYear; year <= currentYear; year++) {
            String schoolYear = year + "-" + (year + 1);
            semesterList.add(schoolYear + "-第一学期");
            semesterList.add(schoolYear + "-第二学期");
            semesterList.add(schoolYear + "-第三学期");
        }


        bySemester.setItems(semesterList);

        String defaultSemester = SemesterConverter.toChinese(getCurrentSemester(CampusSession.getInstance().getServerDate()));
        if (semesterList.contains(defaultSemester)) {
            bySemester.setValue(defaultSemester);
        }
    }
    
    private void initStatusComboBox() {
    	ObservableList<String> statusList = FXCollections.observableArrayList(
    		"待审核",
    		"已通过",
    		"未提交"
    	);
    	
    	byStatus.setItems(statusList);
    }
    
    private void bindCourseListDoubleClick() {
        // 给课程表设置行双击事件
        courseList.setRowFactory(tv -> {
            TableRow<CourseListEntry> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                // 双击（点击次数=2）且行非空（有选中项）
                if (event.getClickCount() == 2 && !row.isEmpty()) {
                    CourseListEntry selectedCourse = row.getItem();
                    // 调用双击处理逻辑，传入选中的课程项
                    handleCourseListDoubleClick(selectedCourse);
                }
            });
            return row;
        });
    }
    
    private void bindGradesListDoubleClick() {
        gradesList.setRowFactory(tv -> {
            TableRow<GradesListEntry> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                // 双击（点击次数=2）且行非空（有选中项）
                if (event.getClickCount() == 2 && !row.isEmpty()) {
                    GradesListEntry selectedGrade = row.getItem();
                    // 调用双击处理逻辑，传入选中的成绩项
                    handleGradesListDoubleClick(selectedGrade);
                }
            });
            return row;
        });
    }
    
    private void handleGradesListDoubleClick(GradesListEntry selectedGrade) {
        // 1. 校验选中项的gradeId（避免空值或格式错误）
        if (selectedGrade == null || selectedGrade.getGradeId() == null || selectedGrade.getGradeId().trim().isEmpty()) {
            showGradesError("选中的成绩信息异常，请重新选择");
            return;
        }

        // 2. 保存当前选中的成绩ID（后续修改成绩用）
        currentGradeId = selectedGrade.getGradeId().trim();

        // 3. 更新UI输入框（显示选中学生的成绩信息）
        Platform.runLater(() -> {
            studentId.setText(selectedGrade.getStudentId() == null ? "" : selectedGrade.getStudentId());
            name.setText(selectedGrade.getName() == null ? "" : selectedGrade.getName());
            // 处理Double转String（避免科学计数法，保留1位小数）
            regularGrades.setText(selectedGrade.getRegularGrades() != null ? 
                String.format("%.1f", selectedGrade.getRegularGrades()) : "");
            finalGrades.setText(selectedGrade.getFinalGrades() != null ? 
                String.format("%.1f", selectedGrade.getFinalGrades()) : "");
        });
    }
    
    private void handleCourseListDoubleClick(CourseListEntry selectedCourse) {
        // 1. 校验选中项的 sectionId（避免空值或格式错误）
        if (selectedCourse == null || selectedCourse.getSectionId() == null || selectedCourse.getSectionId().trim().isEmpty()) {
            showGradesError("选中的课程信息异常，无法加载学生");
            return;
        }

        // 2. 转换 sectionId 为 Integer（数据库中通常为整数类型）
        Integer sectionId;
        try {
            sectionId = Integer.parseInt(selectedCourse.getSectionId().trim());
        } catch (NumberFormatException e) {
            showGradesError("教学班ID格式错误：" + selectedCourse.getSectionId());
            return;
        }

        // 3. 保存当前选中的课程信息（用于UI显示和后续操作）
        currentSectionId = sectionId;
        currentCourseName = selectedCourse.getCourseName();
        currentTeacherName = selectedCourse.getTeacher();

        // 4. 更新UI：显示当前选中的课程和教师
        Platform.runLater(() -> {
        	scrollPane.setVvalue(0.95);
            this.selectedCourse.setText((currentCourseName == null ? "未知" : (currentCourseName + " " + selectedCourse.getCourseName())));
            this.selectedTeacher.setText((currentTeacherName == null ? "未知" : currentTeacherName));
            // 清空之前的学生成绩和输入框
            gradesList.setItems(FXCollections.observableArrayList());
            handleClear(null);
        });

        // 5. 显示加载状态，准备请求服务端获取学生成绩
        showGradesLoading(true);

        // 6. 封装请求参数（用 StudentGrades 打包，仅需 sectionId 作为核心条件）
        StudentGrades request = new StudentGrades();
        request.setSectionId(sectionId); // 核心查询条件：教学班ID
        // 冗余字段（可选，用于服务端二次校验）
        request.setCourseName(currentCourseName);
        request.setTeacherName(currentTeacherName);

        // 7. 异步请求服务端（Task 返回 List<StudentGrades>，与之前风格一致）
        Task<List<StudentGrades>> loadStudentsTask = new Task<>() {
            @Override
            protected List<StudentGrades> call() throws Exception {
                try {
                    // 7.1 发送指令（与服务端约定：AdminGetStudentsBySectionId）
                    dataOut.writeUTF("AdminGetStudentsBySectionId");
                    dataOut.flush();

                    // 7.2 发送请求参数（StudentGrades 序列化）
                    String jsonRequest = gson.toJson(request);
                    dataOut.writeUTF(jsonRequest);
                    dataOut.flush();

                    // 7.3 接收服务端响应
                    String jsonResponse = dataIn.readUTF();
                    if (jsonResponse.contains("\"error\"")) {
                        throw new Exception("加载学生失败：" + jsonResponse);
                    }

                    // 7.4 解析响应为 List<StudentGrades>
                    Type listType = new TypeToken<List<StudentGrades>>() {}.getType();
                    return gson.fromJson(jsonResponse, listType);
                } catch (Exception e) {
                    // 捕获异常并包装（便于UI层提示）
                    throw new Exception("网络请求异常：" + e.getMessage());
                }
            }

            @Override
            protected void succeeded() {
                super.succeeded();
                showGradesLoading(false);
                List<StudentGrades> studentGradesList = getValue();

                // 8. 处理查询结果：显示学生成绩
                if (studentGradesList == null || studentGradesList.isEmpty()) {
                    showGradesError("该教学班暂无学生成绩记录", false);
                    return;
                }
                
                // 新增：获取当前教学班的成绩占比（同一个教学班占比相同，取第一个即可）
                currentFinalRatio = studentGradesList.getFirst().getFinalRatio();

                // 8.1 转换为 GradesListEntry（适配表格显示）
                ObservableList<GradesListEntry> studentEntries = FXCollections.observableArrayList();              
                for (StudentGrades grade : studentGradesList) {
                    // 处理空值（避免表格显示 null）
                    Double regular = grade.getRegularGrade() != null ? grade.getRegularGrade().doubleValue() : 0.0;
                    Double finalG = grade.getFinalGrade() != null ? grade.getFinalGrade().doubleValue() : 0.0;
                    Double total = grade.getTotalGrade() != null ? grade.getTotalGrade().doubleValue() : 0.0;

                    studentEntries.add(new GradesListEntry(
                    	grade.getGradeId().toString(),
                        grade.getStudentId(),          // 学号
                        grade.getStudentName(),        // 姓名
                        grade.getMajor(),              // 专业
                        regular,                       // 平时成绩
                        finalG,                        // 期末成绩
                        total                          // 总成绩
                    ));
                }
                
                if (selectedCourse.getStatus().equals("已通过")) {
                	passButton.setDisable(true);
                } else {
                	passButton.setDisable(false);
                }

                // 8.2 更新学生成绩表格
                gradesList.setItems(studentEntries);
                showGradesError("成功加载 " + studentEntries.size() + " 名学生成绩", false);
            }

            @Override
            protected void failed() {
                super.failed();
                showGradesLoading(false);
                currentFinalRatio = null; // 失败时清空占比
                // 显示错误信息
                Throwable error = getException();
                showGradesError(error.getMessage() != null ? error.getMessage() : "加载学生成绩失败");
            }
        };

        // 8. 启动异步任务
        new Thread(loadStudentsTask).start();
    }
    
    @FXML
    void handleRetrieveCourse(ActionEvent event) {
        // 1. 获取查询条件并封装到StudentGrades对象
        StudentGrades queryCondition = new StudentGrades();
        
        // 学期转换为服务器格式（如"2023-2024-1"）
        if (bySemester.getValue() != null) {
            queryCondition.setSemester(SemesterConverter.toNumber(bySemester.getValue()));
        }
        queryCondition.setCourseId(byCourseId.getText().trim());
        queryCondition.setCourseName(byCourseName.getText().trim());
        queryCondition.setTeacherName(byTeacher.getText().trim()); // 教师姓名
        queryCondition.setSectionStatus(byStatus.getValue()); // 教学班状态
        
        // 2. 显示加载状态
        showCourseLoading(true);
        errorCourseLabel.setVisible(false);

        // 3. 创建后台任务，返回类型为List<StudentGrades>
        Task<List<StudentGrades>> retrieveTask = new Task<>() {
            @Override
            protected List<StudentGrades> call() throws Exception {
                try {
                    // 发送指令
                    dataOut.writeUTF("AdminRetrieveCourses");
                    dataOut.flush();

                    // 发送查询条件（StudentGrades对象）
                    String jsonCondition = gson.toJson(queryCondition);
                    dataOut.writeUTF(jsonCondition);
                    dataOut.flush();

                    // 接收响应
                    String jsonResponse = dataIn.readUTF();
                    if (jsonResponse.contains("\"error\"")) {
                        throw new Exception("查询失败：" + jsonResponse);
                    }

                    // 解析为List<StudentGrades>
                    Type listType = new TypeToken<List<StudentGrades>>() {}.getType();
                    return gson.fromJson(jsonResponse, listType);
                } catch (Exception e) {
                    throw new Exception("检索课程失败：" + e.getMessage());
                }
            }

            @Override
            protected void succeeded() {
                super.succeeded();
                showCourseLoading(false);
                List<StudentGrades> result = getValue();
                
                if (result == null || result.isEmpty()) {
                    showCourseError("未找到符合条件的课程", false);
                    courseList.setItems(FXCollections.observableArrayList());
                    return;
                }

                // 转换为CourseListEntry显示在表格
                ObservableList<CourseListEntry> courseEntries = FXCollections.observableArrayList();
                for (StudentGrades grade : result) {
                    courseEntries.add(new CourseListEntry(
                    	grade.getSectionId().toString(),
                        SemesterConverter.toChinese(grade.getSemester()), // 转回中文显示
                        grade.getCourseId(),
                        grade.getCourseName(),
                        grade.getTeacherName(),
                        grade.getClassTime(), // 对应数据库class_time字段
                        grade.getSectionStatus()
                    ));
                }
                courseList.setItems(courseEntries);
                showCourseError("找到 " + result.size() + " 条记录", false);
            }

            @Override
            protected void failed() {
                super.failed();
                showCourseLoading(false);
                showCourseError(getException().getMessage());
            }
        };

        new Thread(retrieveTask).start();
    }
    @FXML
    void handleConditionClear(ActionEvent event) {
    	byCourseId.clear();
    	byCourseName.clear();
    	byTeacher.clear();
    	byStatus.setValue(null);
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
    void handleRevise(ActionEvent event) {
    	 // 1. 基础校验（确保选中成绩+教学班信息完整）
        if (currentGradeId == null || currentGradeId.trim().isEmpty()) {
            showGradesError("请先双击成绩表选择需要修改的学生");
            return;
        }
        if (currentSectionId == null) {
            showGradesError("未获取到教学班信息，请重新加载课程");
            return;
        }
        if (currentFinalRatio == null) {
            showGradesError("未获取到成绩占比规则，无法计算总成绩");
            return;
        }

        // 2. 获取输入框成绩并校验格式
        String regularStr = regularGrades.getText().trim();
        String finalStr = finalGrades.getText().trim();

        // 非空校验
        if (regularStr.isEmpty()) {
            showGradesError("请输入平时成绩");
            return;
        }
        if (finalStr.isEmpty()) {
            showGradesError("请输入期末成绩");
            return;
        }

        // 数字格式+范围校验（0-100）
        Double regularDouble = null;
        Double finalDouble = null;
        
        try {
            regularDouble = Double.parseDouble(regularStr);
            if (regularDouble < 0 || regularDouble > 100) {
                throw new IllegalArgumentException("平时成绩需在0-100之间");
            }

            finalDouble = Double.parseDouble(finalStr);
            if (finalDouble < 0 || finalDouble > 100) {
                throw new IllegalArgumentException("期末成绩需在0-100之间");
            }
        } catch (NumberFormatException e) {
            showGradesError("成绩格式错误，请输入数字（如85.5）");
            return;
        } catch (IllegalArgumentException e) {
            showGradesError(e.getMessage());
            return;
        }

        // 3. 计算总成绩（BigDecimal避免精度丢失，复用GradeUtils工具类）
        BigDecimal regularGrade = BigDecimal.valueOf(regularDouble);
        BigDecimal finalGrade = BigDecimal.valueOf(finalDouble);
        BigDecimal totalGrade = GradeUtils.calculateTotalGrade(regularGrade, finalGrade, currentFinalRatio);

        // 4. 封装请求参数（用StudentGrades打包，含管理员修改所需字段）
        StudentGrades request = new StudentGrades();
        request.setGradeId(Integer.parseInt(currentGradeId)); // 核心：成绩ID（更新依据）
        request.setSectionId(currentSectionId);              // 教学班ID（权限校验）
        request.setStudentId(studentId.getText().trim());    // 学生ID（二次校验）
        request.setRegularGrade(regularGrade);               // 新平时成绩
        request.setFinalGrade(finalGrade);                   // 新期末成绩
        request.setTotalGrade(totalGrade);                   // 计算后的总成绩
        // 冗余字段（服务端校验用）
        request.setCourseName(currentCourseName);
        request.setTeacherName(currentTeacherName);

        // 5. 禁用UI组件，显示加载状态
        reviseGrade.setDisable(true);
        deselectButton.setDisable(true);
        showGradesLoading(true);

        // 6. 异步发送修改请求
        Task<Boolean> reviseTask = new Task<>() {
            @Override
            protected Boolean call() throws Exception {
                try {
                    // 发送管理员修改成绩指令（与服务端约定）
                    dataOut.writeUTF("AdminUpdateStudentGrade");
                    dataOut.flush();

                    // 序列化请求参数
                    String jsonRequest = gson.toJson(request);
                    dataOut.writeUTF(jsonRequest);
                    dataOut.flush();

                    // 接收服务端响应
                    String jsonResponse = dataIn.readUTF();
                    if (jsonResponse.contains("\"error\"")) {
                        throw new Exception("修改成绩失败：" + jsonResponse);
                    }

                    return true; // 修改成功
                } catch (Exception e) {
                    throw new Exception("网络异常：" + e.getMessage());
                }
            }

            @Override
            protected void succeeded() {
                super.succeeded();
                showGradesLoading(false);
                // 恢复UI组件
                reviseGrade.setDisable(false);
                deselectButton.setDisable(false);

                // 成功反馈：清空输入框+刷新成绩表
                handleClear(null);
                showGradesError("成绩修改成功！", false); // 绿色成功提示

                // 重新加载当前教学班成绩，更新表格显示
                if (currentSectionId != null) {
                    // 复用课程双击逻辑，重新加载学生成绩
                    CourseListEntry dummyCourse = new CourseListEntry(
                        currentSectionId.toString(), "", "", currentCourseName, 
                        currentTeacherName, "", ""
                    );
                    handleCourseListDoubleClick(dummyCourse);
                }
            }

            @Override
            protected void failed() {
                super.failed();
                showGradesLoading(false);
                // 恢复UI组件，允许重试
                reviseGrade.setDisable(false);
                deselectButton.setDisable(false);

                // 失败反馈
                Throwable error = getException();
                showGradesError(error.getMessage() != null ? error.getMessage() : "修改成绩失败");
            }
        };

        new Thread(reviseTask).start();
    }

    @FXML
    void handleDeselect(ActionEvent event) {
    	currentGradeId = null; 	// 清空成绩ID
        handleClear(null);      // 清空输入框
        gradesList.getSelectionModel().clearSelection(); // 取消表格选中
    }

    @FXML
    void handlePass(ActionEvent event) {
    	// 1. 基础校验（确保已加载教学班）
        if (currentSectionId == null) {
            showGradesError("请先双击课程表选择需要审核的教学班");
            return;
        }
        if (currentCourseName == null) {
            showGradesError("未获取到课程名称，无法审核");
            return;
        }

        // 2. 二次确认（避免误操作）
        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("成绩审核确认");
        confirmAlert.setHeaderText("确认审核当前教学班成绩？");
        confirmAlert.setContentText("审核后，该教学班成绩状态将改为“已通过”，学生可查看成绩，且不可再修改！");
        
        // 等待用户确认
        confirmAlert.showAndWait().ifPresent(buttonType -> {
            if (buttonType == ButtonType.OK) {
                // 用户确认后，执行审核逻辑
                executePassLogic();
            }
        });
    }
    
 // 辅助方法：执行审核核心逻辑
    private void executePassLogic() {
        // 1. 禁用UI组件，显示加载状态
        passButton.setDisable(true);
        reviseGrade.setDisable(true);
        deselectButton.setDisable(true);
        showGradesLoading(true);

        // 2. 封装审核请求（用StudentGrades打包）
        StudentGrades request = new StudentGrades();
        request.setSectionId(currentSectionId);          // 核心：教学班ID（审核依据）
        request.setCourseName(currentCourseName);        // 课程名（校验用）
        request.setTeacherName(currentTeacherName);      // 教师名（校验用）
        request.setSectionStatus("已通过");              // 目标状态

        // 3. 异步发送审核请求
        Task<Boolean> passTask = new Task<>() {
            @Override
            protected Boolean call() throws Exception {
                try {
                    // 发送管理员审核指令（与服务端约定）
                    dataOut.writeUTF("AdminPassCourseSection");
                    dataOut.flush();

                    // 序列化请求参数
                    String jsonRequest = gson.toJson(request);
                    dataOut.writeUTF(jsonRequest);
                    dataOut.flush();

                    // 接收服务端响应
                    String jsonResponse = dataIn.readUTF();
                    if (jsonResponse.contains("\"error\"")) {
                        throw new Exception("审核失败：" + jsonResponse);
                    }

                    return true; // 审核成功
                } catch (Exception e) {
                    throw new Exception("网络异常：" + e.getMessage());
                }
            }

            @Override
            protected void succeeded() {
                super.succeeded();
                showGradesLoading(false);
                // 恢复部分UI组件（审核按钮保持禁用，避免重复审核）
                reviseGrade.setDisable(true); // 审核后不可再修改成绩
                deselectButton.setDisable(false);

                // 成功反馈：清空输入框
                handleClear(null);
                
                // 新增：清空当前选中的sectionId和成绩列表
                currentSectionId = null; // 重置教学班ID
                gradesList.setItems(FXCollections.observableArrayList()); // 清空成绩表格
                
                showGradesError("成绩审核成功！该教学班成绩已开放给学生查看", false);

                // 重新加载课程列表，更新状态显示
                handleRetrieveCourse(null);
            }

            @Override
            protected void failed() {
                super.failed();
                showGradesLoading(false);
                // 恢复UI组件，允许重试
                passButton.setDisable(false);
                reviseGrade.setDisable(false);
                deselectButton.setDisable(false);

                // 失败反馈
                Throwable error = getException();
                showGradesError(error.getMessage() != null ? error.getMessage() : "审核成绩失败");
            }
        };

        new Thread(passTask).start();
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
    
    private void showCourseLoading(boolean show) {
        Platform.runLater(() -> loadingCourseLabel.setVisible(show));
    }

    private void showCourseError(String msg) {
        showCourseError(msg, true);
    }

    private void showCourseError(String msg, boolean isError) {
        Platform.runLater(() -> {
            errorCourseLabel.setText(msg);
            errorCourseLabel.setStyle(isError ? "-fx-text-fill: #d32f2f;" : "-fx-text-fill: #388e3c;");
            errorCourseLabel.setVisible(true);
            new Thread(() -> {
                try { Thread.sleep(3000); } catch (InterruptedException e) {}
                Platform.runLater(() -> errorCourseLabel.setVisible(false));
            }).start();
        });
    }
    
    private void showGradesLoading(boolean show) {
        Platform.runLater(() -> loadingGradesLabel.setVisible(show));
    }

    private void showGradesError(String msg) {
        showGradesError(msg, true);
    }

    private void showGradesError(String msg, boolean isError) {
        Platform.runLater(() -> {
            errorGradesLabel.setText(msg);
            errorGradesLabel.setStyle(isError ? "-fx-text-fill: #d32f2f;" : "-fx-text-fill: #388e3c;");
            errorGradesLabel.setVisible(true);
            new Thread(() -> {
                try { Thread.sleep(3000); } catch (InterruptedException e) {}
                Platform.runLater(() -> errorGradesLabel.setVisible(false));
            }).start();
        });
    }
    
    public static class CourseListEntry {
    	private final StringProperty sectionId;
        private final StringProperty semester;
        private final StringProperty courseId;
        private final StringProperty courseName;
        private final StringProperty teacher;
        private final StringProperty schedule; // 数据库中叫class_time 注意！
        private final StringProperty status;
        
        public CourseListEntry(String sectionId,String semester, String courseId, String courseName,
                            String teacher, String schedule, String status) {
        	this.sectionId = new SimpleStringProperty(sectionId);
            this.semester = new SimpleStringProperty(semester);
            this.courseId = new SimpleStringProperty(courseId);
            this.courseName = new SimpleStringProperty(courseName);
            this.teacher = new SimpleStringProperty(teacher);
            this.schedule = new SimpleStringProperty(schedule);
            this.status = new SimpleStringProperty(status);
        }

        // Getter 方法
        public String getSectionId() { return sectionId.get(); }
        public String getSemester() { return semester.get(); }
        public String getCourseId() { return courseId.get(); }
        public String getCourseName() { return courseName.get(); }
        public String getTeacher() { return teacher.get(); }
        public String getSchedule() { return schedule.get(); }
        public String getStatus() { return status.get(); }
        
        // Setter 方法
        public void setSectionId(String sectionId) { this.sectionId.set(sectionId); }
        public void setSemester(String semester) { this.semester.set(semester); }
        public void setCourseId(String courseId) { this.courseId.set(courseId); }
        public void setCourseName(String courseName) { this.courseName.set(courseName); }
        public void setTeacher(String teacher) { this.teacher.set(teacher); }
        public void setSchedule(String schedule) { this.schedule.set(schedule); }
        public void setStatus(String status) { this.status.set(status); }
        
        // Property 访问器
        public StringProperty sectionIdProperty() { return sectionId; }
        public StringProperty semesterProperty() { return semester; }
        public StringProperty courseIdProperty() { return courseId; }
        public StringProperty courseNameProperty() { return courseName; }
        public StringProperty teacherProperty() { return teacher; }
        public StringProperty scheduleProperty() { return schedule; }
        public StringProperty statusProperty() { return status; }
    }
    
    public static class GradesListEntry {
    	private final StringProperty gradeId;
        private final StringProperty studentId;
        private final StringProperty name;
        private final StringProperty major;
        private final DoubleProperty regularGrades;
        private final DoubleProperty finalGrades;
        private final DoubleProperty totalGrades;

        public GradesListEntry(String gradeId, String studentId, String name, String major, 
                               Double regularGrades, Double finalGrades, Double totalGrades) {
        	this.gradeId = new SimpleStringProperty(gradeId);
            this.studentId = new SimpleStringProperty(studentId);
            this.name = new SimpleStringProperty(name);
            this.major = new SimpleStringProperty(major);
            this.regularGrades = new SimpleDoubleProperty(regularGrades);
            this.finalGrades = new SimpleDoubleProperty(finalGrades);
            this.totalGrades = new SimpleDoubleProperty(totalGrades);
        }
        
        public String getGradeId() { return gradeId.get(); }
        public String getStudentId() { return studentId.get(); }
        public String getName() { return name.get(); }
        public String getMajor() { return major.get(); }
        public Double getRegularGrades() { return regularGrades.get(); }
        public Double getFinalGrades() { return finalGrades.get(); }
        public Double getTotalGrades() { return totalGrades.get(); }
        
        public void setGradeId(String gradeId) { this.gradeId.setValue(gradeId); }
        public void setStudentId(String studentId) { this.studentId.setValue(studentId); }
        public void setName(String name)  { this.name.setValue(name); }
        public void setMajor(String major) { this.major.setValue(major); }
        public void setRegularGrades(Double regularGrades) { this.regularGrades.setValue(regularGrades); }
        public void setFinalGrades(Double finalGrades) { this.finalGrades.setValue(finalGrades); }
        
        public StringProperty gradeIdProperty() { return gradeId; }
        public StringProperty studentIdProperty() { return studentId; }
        public StringProperty nameProperty() { return name; }
        public StringProperty majorProperty() { return major; }
        public DoubleProperty regularGradesProperty() { return regularGrades; } 
        public DoubleProperty finalGradesProperty() { return finalGrades; }
        public DoubleProperty totalGradesProperty() { return totalGrades; }
    }
}