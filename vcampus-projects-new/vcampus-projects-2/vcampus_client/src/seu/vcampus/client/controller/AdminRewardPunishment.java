package seu.vcampus.client.controller;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.lang.reflect.Type;
import java.net.URL;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableView;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TextField;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.event.ActionEvent;

import seu.vcampus.client.network.SocketManager;
import seu.vcampus.client.session.CampusSession;
import seu.vcampus.client.util.GradeConverter;
import seu.vcampus.model.ClassInfo;
import seu.vcampus.model.CollegeInfo;
import seu.vcampus.model.MajorInfo;
import seu.vcampus.model.StudentProfile;
import seu.vcampus.model.RewardPunishment;
import seu.vcampus.model.StudentRetrieveCondition;

public class AdminRewardPunishment implements Initializable{
	@FXML private ScrollPane scrollPane;
	
    @FXML private TextField byStudentId;
    @FXML private TextField byName;
    @FXML private ComboBox<String> byCollege;
    @FXML private ComboBox<String> byMajor;
    @FXML private ComboBox<String> byGrade;
    @FXML private ComboBox<String> byClassName;
    @FXML private Button retrieve;
    @FXML private Button conditionClearButton;
    @FXML private Label loadingStudentLabel;
    @FXML private Label errorStudentLabel;
    @FXML private TableView<StudentListEntry> studentList;
    @FXML private TableColumn<StudentListEntry, String> studentIdCol;
    @FXML private TableColumn<StudentListEntry, String> nameCol;
    @FXML private TableColumn<StudentListEntry, String> collegeCol;
    @FXML private TableColumn<StudentListEntry, String> majorCol;
    @FXML private TableColumn<StudentListEntry, String> gradeCol;
    @FXML private TableColumn<StudentListEntry, String> classNameCol;
    
    @FXML private TextField name;
    @FXML private TextField studentId;
    @FXML private ComboBox<String> type;
    @FXML private TextField title;
    @FXML private TextField reason;
    @FXML private ComboBox<String> awardingOrganization;
    @FXML private Button deselect;
    @FXML private Button clear;
    @FXML private Button add;
    @FXML private Button revoke;
    @FXML private Label loadingRewardPunishmentLabel;
    @FXML private Label errorRewardPunishmentLabel;
    @FXML private TableView<RewardPunishmentListEntry> rewardPunishmentList;
    @FXML private TableColumn<RewardPunishmentListEntry, String> typeCol;
    @FXML private TableColumn<RewardPunishmentListEntry, String> titleCol;
    @FXML private TableColumn<RewardPunishmentListEntry, String> reasonCol;
    @FXML private TableColumn<RewardPunishmentListEntry, String> awardingOrganizationCol;
    @FXML private TableColumn<RewardPunishmentListEntry, String> effectiveDateCol;
    @FXML private TableColumn<RewardPunishmentListEntry, String> statusCol;
    
    private DataInputStream dataIn;
    private DataOutputStream dataOut;
    private static final Gson gson = new Gson();
    private static CampusSession campusSession = CampusSession.getInstance();
    private String currentSelectedRPId = null;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
    	initNetworkConnections();
    	
    	initRetrieveComboBoxes();
        initStudentList();
        
        initEditComboBoxes();
        initRewardPunishmentList();
        disableUI();
        
        setupForwardListeners();
        setUpDoubleClick();
    }
    
    private void initNetworkConnections() {
    	try {
			dataIn = SocketManager.getInstance().getIn();
			dataOut = SocketManager.getInstance().getOut();
		} catch (Exception e) {
			showStudentError("网络连接失败，无法连接至服务器，请检查网络设置");
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
    
    private void initEditComboBoxes() {
    	ObservableList<String> types = FXCollections.observableArrayList(
    		"学业表彰", "行为表彰", "创新表彰", "社会贡献表彰",
    		"学业惩罚", "行为惩罚", "纪律惩罚", "道德惩罚"
    	);
    	
    	type.setItems(types);
    	type.setValue(null);
    	
    	List<String> colleges = campusSession.getAllColleges().stream()
    			.map(CollegeInfo::getCollegeName)
    			.collect(Collectors.toList());
    	
    	
        awardingOrganization.setItems(FXCollections.observableArrayList(colleges));
        awardingOrganization.setValue(null);
    }
    
    private void initRewardPunishmentList() {
    	 typeCol.setCellValueFactory(new PropertyValueFactory<>("type"));
    	 titleCol.setCellValueFactory(new PropertyValueFactory<>("title"));
    	 reasonCol.setCellValueFactory(new PropertyValueFactory<>("reason"));
    	 awardingOrganizationCol.setCellValueFactory(new PropertyValueFactory<>("awardingOrganization"));
    	 effectiveDateCol.setCellValueFactory(new PropertyValueFactory<>("effectiveDate"));
    	 statusCol.setCellValueFactory(new PropertyValueFactory<>("status"));
    
    	 rewardPunishmentList.setItems(FXCollections.observableArrayList());
    }
    
    private void disableUI() {
    	name.setDisable(true);
    	studentId.setDisable(true);
    	revoke.setDisable(true);
    	add.setDisable(true);    	
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
        
        	List<ClassInfo> classInfoList = campusSession.getClassesByMajor(
        		campusSession.getMajorIdByNames(selectedCollegeName, selectedMajorName)
        	);
        	
        	int enrollmentYearNum = GradeConverter.calculateEnrollmentYear(currentDate, selectedGrade);
        	String enrollmentYearStr = enrollmentYearNum + "级";
        	
        	ObservableList<String> classObList = FXCollections.observableArrayList();
        	
        	for (ClassInfo classInfo : classInfoList) {
        		if (classInfo.getGrade().equals(enrollmentYearStr)) {
        			classObList.add(classInfo.getClassName());
        		}
        	}
        	byClassName.setItems(classObList);
        	byClassName.setValue(null);
        });
    }
    
    private void setUpDoubleClick() {
    	studentList.setRowFactory(tv -> {
            TableRow<StudentListEntry> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty()) {
                    handleStudentListDoubleClick(row.getItem());
                }
            });
            return row;
        });
    	
    	rewardPunishmentList.setRowFactory(tv -> {
    		TableRow<RewardPunishmentListEntry> row = new TableRow<>();
    		row.setOnMouseClicked(event->{
    			if (event.getClickCount() == 2 && !row.isEmpty()) {
    				handleRewardPunishmentListDoubleClick(row.getItem());
    			}
    		});
    		return row;
    	});
    }
    
    private void handleRewardPunishmentListDoubleClick(RewardPunishmentListEntry entry) {
    	currentSelectedRPId = entry.getId();
    	add.setDisable(false);
    	add.setText("修改");
    	revoke.setDisable(false);
    	
    	type.setValue(entry.getType());
    	title.setText(entry.getTitle());
    	reason.setText(entry.getReason());
    	awardingOrganization.setValue(entry.getAwardingOrganization());
    }
    
    private void handleStudentListDoubleClick(StudentListEntry entry) {
    	// 1. 滚动到奖惩记录区域，提升用户体验
        scrollPane.setVvalue(1.0);
        add.setDisable(false);
        add.setText("添加");
        // 显示加载状态，隐藏错误提示
        showRewardPunishmentLoading(true);
        errorRewardPunishmentLabel.setVisible(false);

        // 2. 获取选中学生的核心信息
        String selectedStudentId = entry.getStudentId();

        Task<List<RewardPunishment>> fetchTask = new Task<>() {
            @Override
            protected List<RewardPunishment> call() throws Exception {
                try {
                    // 3. 向服务端发送请求：指令 + 学生ID
                    dataOut.writeUTF("GetStudentRewardPunishmentById");
                    dataOut.flush();
                    dataOut.writeUTF(selectedStudentId);
                    dataOut.flush();

                    // 4. 接收服务端响应
                    String json = dataIn.readUTF();

                    // 5. 检查响应是否包含错误信息
                    if (json.contains("\"error\"")) {
                        throw new Exception("服务端查询失败：" + json);
                    }

                    // 6. 反序列化（修复原代码TypeToken错误：将StudentProfile改为RewardPunishment）
                    Type rewardPunishmentListType = new TypeToken<List<RewardPunishment>>() {}.getType();
                    List<RewardPunishment> result = gson.fromJson(json, rewardPunishmentListType);

                    // 7. 校验反序列化结果
                    if (result == null) {
                        throw new Exception("未获取到学生奖惩记录");
                    }
                    return result;

                } catch (IOException e) {
                    throw new Exception("网络异常：" + e.getMessage());
                } catch (Exception e) {
                    // 捕获反序列化等其他异常
                    throw new Exception("数据处理失败：" + e.getMessage());
                }
            }

            @Override
            protected void succeeded() {
                super.succeeded();
                // 8. 隐藏加载状态
                showRewardPunishmentLoading(false);

                // 9. 填充学生基本信息（学号、姓名）
                studentId.setText(entry.getStudentId());
                name.setText(entry.getName());
            	type.setValue(null);
            	title.clear();
            	reason.clear();
            	awardingOrganization.setValue(null);
            	
            	revoke.setDisable(true);
            	currentSelectedRPId = null;
            	
                // 10. 转换奖惩记录为表格条目，更新表格
                List<RewardPunishment> rewardList = getValue();
                ObservableList<RewardPunishmentListEntry> tableEntries = FXCollections.observableArrayList();
                
                for (RewardPunishment rp : rewardList) {
                    tableEntries.add(new RewardPunishmentListEntry(
                    	rp.getId(),
                        rp.getType(),          // 奖惩类型（奖励/惩罚）
                        rp.getTitle(),         // 奖惩名称
                        rp.getReason(),        // 奖惩原因
                        rp.getAwardingOrganization(), // 颁发组织（学院名/校级组织）
                        rp.getEffectiveDate(), // 生效日期
                        rp.getStatus()         // 状态（通过/撤销/待审核）
                    ));
                }

                // 11. 设置表格数据，处理空记录场景
                rewardPunishmentList.setItems(tableEntries);
                if (tableEntries.isEmpty()) {
                    showRewardPunishmentError("该学生暂无奖惩记录", false); // 非错误提示（绿色）
                }
            }

            @Override
            protected void failed() {
                super.failed();
                // 12. 异常处理：隐藏加载状态，显示错误信息
                showRewardPunishmentLoading(false);
                showRewardPunishmentError(getException().getMessage());

                // 13. 清空学生信息和表格（避免残留旧数据）
                studentId.clear();
                name.clear();
                rewardPunishmentList.setItems(FXCollections.observableArrayList());
            }
        };

        new Thread(fetchTask).start();
    }
    
    @FXML
    void handleRetrieve(ActionEvent event) {
    	showStudentLoading(true);
    	
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
    			showStudentLoading(false);
    			
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
                    showStudentError("未找到符合条件的学生", false); // 非错误提示（绿色）
                }
    			
    		}
    		
    		@Override 
    		protected void failed() {
    			super.failed();
    			showStudentLoading(false);
    			showStudentError(getException().getMessage());
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

    @FXML
    void handleDeselect(ActionEvent event) {
    	// scrollPane.setVvalue(0.0);
    	currentSelectedRPId = null;
    	add.setDisable(true);
    	add.setText("添加");
    	revoke.setDisable(true);
    	
    	name.clear();
    	studentId.clear();
    	type.setValue(null);
    	title.clear();
    	reason.clear();
    	awardingOrganization.setValue(null);
    	rewardPunishmentList.setItems(FXCollections.observableArrayList());
    }

    @FXML
    void handleClear(ActionEvent event) {
    	currentSelectedRPId = null;
    	add.setText("添加");
    	revoke.setDisable(true);
    	
    	type.setValue(null);
    	title.clear();
    	reason.clear();
    	awardingOrganization.setValue(null);
    }

    @FXML
    void handleAdd(ActionEvent event) {
    	// 1. 输入验证（检查必填字段）
        if (type.getValue() == null || type.getValue().isEmpty()) {
            showRewardPunishmentError("请选择奖惩类型");
            return;
        }
        if (title.getText().trim().isEmpty()) {
            showRewardPunishmentError("请输入奖惩名称");
            return;
        }
        if (reason.getText().trim().isEmpty()) {
            showRewardPunishmentError("请输入奖惩原因");
            return;
        }
        if (awardingOrganization.getValue() == null) {
            showRewardPunishmentError("请选择颁发组织");
            return;
        }
        if (studentId.getText().trim().isEmpty()) { // 这个不太可能发生
            showRewardPunishmentError("请先选择学生");
            return;
        }
        
        // 2. 构建奖惩记录对象（区分添加/修改）
        RewardPunishment request = new RewardPunishment();
        String operation;
        
        if (currentSelectedRPId == null || currentSelectedRPId.isEmpty()) {
        	operation = "AddRewardPunishment";
        	request.setId(null);
        	request.setStatus("通过");
        } else {
        	operation = "UpdateRewardPunishment";
        	request.setId(currentSelectedRPId);
        }
        
        // 3. 填充公共字段
        request.setType(type.getValue());
        request.setTitle(title.getText().trim());
        request.setReason(reason.getText().trim());
        request.setAwardingOrganization(awardingOrganization.getValue());
        // 生效日期使用当前服务器日期
        request.setEffectiveDate(
            campusSession.getServerDate().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
        );
        
        showRewardPunishmentLoading(true);
        Task<Boolean> submitTask = new Task<>() {
            @Override
            protected Boolean call() throws Exception {
                try {
                    // 发送指令
                    dataOut.writeUTF(operation);
                    dataOut.flush();

                    // 发送学生ID（用于关联学生）
                    dataOut.writeUTF(studentId.getText().trim());
                    dataOut.flush();

                    // 序列化并发送奖惩记录对象
                    String json = gson.toJson(request);
                    dataOut.writeUTF(json);
                    dataOut.flush();

                    // 接收服务端响应
                    String response = dataIn.readUTF();
                    if (response.contains("error")) {
                        throw new Exception("操作失败：" + response);
                    }
                    return true; // 操作成功
                } catch (IOException e) {
                    throw new Exception("网络错误：" + e.getMessage());
                }
            }

            @Override
            protected void succeeded() {
                super.succeeded();
                showRewardPunishmentLoading(false);
                // 操作成功：刷新当前学生的奖惩记录列表
                refreshRewardPunishmentList(studentId.getText().trim());
                // 清空输入框（可选）
                clearInputFields();
                showRewardPunishmentError("操作成功", false); // 成功提示（绿色）
            }

            @Override
            protected void failed() {
                super.failed();
                showRewardPunishmentLoading(false);
                showRewardPunishmentError(getException().getMessage());
            }
        };

        new Thread(submitTask).start();
    }
    
    // 辅助方法：刷新当前学生的奖惩记录列表
    private void refreshRewardPunishmentList(String studentId) {
        // 复用双击查询的逻辑，重新加载当前学生的奖惩记录
        Task<List<RewardPunishment>> refreshTask = new Task<>() {
            @Override
            protected List<RewardPunishment> call() throws Exception {
                dataOut.writeUTF("GetStudentRewardPunishmentById");
                dataOut.flush();
                dataOut.writeUTF(studentId);
                dataOut.flush();

                String json = dataIn.readUTF();
                Type listType = new TypeToken<List<RewardPunishment>>() {}.getType();
                return gson.fromJson(json, listType);
            }

            @Override
            protected void succeeded() {
                List<RewardPunishment> newList = getValue();
                ObservableList<RewardPunishmentListEntry> entries = FXCollections.observableArrayList();
                for (RewardPunishment rp : newList) {
                    entries.add(new RewardPunishmentListEntry(
                    	rp.getId(),
                        rp.getType(), rp.getTitle(), rp.getReason(),
                        rp.getAwardingOrganization(), rp.getEffectiveDate(), rp.getStatus()
                    ));
                }
                rewardPunishmentList.setItems(entries);
            }
        };
        new Thread(refreshTask).start();
    }

    @FXML
    void handleRevoke(ActionEvent event) {
        // 1. 检查是否有选中的奖惩记录
        RewardPunishmentListEntry selectedEntry = rewardPunishmentList.getSelectionModel().getSelectedItem();
        if (selectedEntry == null || currentSelectedRPId == null || currentSelectedRPId.isEmpty()) {
            showRewardPunishmentError("请先选中一条奖惩记录");
            return;
        }

        // 2. 确认是否撤销（可选：增加确认弹窗提升安全性）
        String recordId = selectedEntry.getId();
        if (recordId == null || recordId.isEmpty()) {
            showRewardPunishmentError("选中的记录无效");
            return;
        }
        if (!currentSelectedRPId.equals(recordId)) {
        	showRewardPunishmentError("可能误触其它表项，请使用双击选中");
            return;
        }
        

        // 3. 显示加载状态
        showRewardPunishmentLoading(true);

        // 4. 异步发送撤销请求
        Task<Boolean> revokeTask = new Task<>() {
            @Override
            protected Boolean call() throws Exception {
                try {
                    // 发送指令
                    dataOut.writeUTF("RevokeRewardPunishment");
                    dataOut.flush();

                    // 发送要撤销的记录ID
                    dataOut.writeUTF(recordId);
                    dataOut.flush();

                    // 接收响应
                    String response = dataIn.readUTF();
                    if (response.contains("error")) {
                        throw new Exception("撤销失败：" + response);
                    }
                    return true;
                } catch (IOException e) {
                    throw new Exception("网络错误：" + e.getMessage());
                }
            }

            @Override
            protected void succeeded() {
                super.succeeded();
                showRewardPunishmentLoading(false);
                refreshRewardPunishmentList(studentId.getText().trim());
                clearInputFields(); // 清空当前框框 并将当前选中Id置为null
                showRewardPunishmentError("已成功撤销该记录", false); // 绿色成功提示
            }

            @Override
            protected void failed() {
                super.failed();
                showRewardPunishmentLoading(false);
                showRewardPunishmentError(getException().getMessage());
            }
        };

        new Thread(revokeTask).start();
    }

    // 辅助方法：清空输入框
    private void clearInputFields() { // 在点击提交、撤销、添加的时候调用
        type.setValue(null);
        title.clear();
        reason.setText("");
        awardingOrganization.setValue(null);
        currentSelectedRPId = null; // 重置选中状态
        
        add.setText("添加");
        revoke.setDisable(true);
    }
    
    private void showStudentLoading(boolean show) {
        Platform.runLater(() -> loadingStudentLabel.setVisible(show));
    }

    private void showStudentError(String msg) {
        showStudentError(msg, true);
    }

    private void showStudentError(String msg, boolean isError) {
        Platform.runLater(() -> {
            errorStudentLabel.setText(msg);
            errorStudentLabel.setStyle(isError ? "-fx-text-fill: #d32f2f;" : "-fx-text-fill: #388e3c;");
            errorStudentLabel.setVisible(true);
            new Thread(() -> {
                try { Thread.sleep(3000); } catch (InterruptedException e) {}
                Platform.runLater(() -> errorStudentLabel.setVisible(false));
            }).start();
        });
    }
    
    private void showRewardPunishmentLoading(boolean show) {
        Platform.runLater(() -> loadingRewardPunishmentLabel.setVisible(show));
    }

    private void showRewardPunishmentError(String msg) {
        showRewardPunishmentError(msg, true);
    }

    private void showRewardPunishmentError(String msg, boolean isError) {
        Platform.runLater(() -> {
            errorRewardPunishmentLabel.setText(msg);
            errorRewardPunishmentLabel.setStyle(isError ? "-fx-text-fill: #d32f2f;" : "-fx-text-fill: #388e3c;");
            errorRewardPunishmentLabel.setVisible(true);
            new Thread(() -> {
                try { Thread.sleep(3000); } catch (InterruptedException e) {}
                Platform.runLater(() -> errorRewardPunishmentLabel.setVisible(false));
            }).start();
        });
    }
    
    public static class StudentListEntry {
        private final StringProperty studentId;
        private final StringProperty name;
        private final StringProperty college;
        private final StringProperty major;
        private final StringProperty grade;
        private final StringProperty className;

        public StudentListEntry(String studentId, String name, String college, 
        						String major, String grade, String className) {
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
        public void setClassName(String classId) { this.className.setValue(classId); }
        
        // Property方法（预留扩展）
        public StringProperty studentIdProperty() { return studentId; }
        public StringProperty nameProperty() { return name; }
        public StringProperty collegeProperty() { return college; }
        public StringProperty majorProperty() { return major; }
        public StringProperty gradeProperty() { return grade; }
        public StringProperty classNameProperty() { return className; }
    }
    
    
    public static class RewardPunishmentListEntry {
        private final StringProperty id;
        private final StringProperty type;
        private final StringProperty title;
        private final StringProperty reason;
        private final StringProperty awardingOrganization;
        private final StringProperty effectiveDate;
        private final StringProperty status;

        public RewardPunishmentListEntry(String id, String type, String title, String reason, 
                                        String awardingOrganization, String effectiveDate, 
                                        String status) {
        	this.id = new SimpleStringProperty(id);
            this.type = new SimpleStringProperty(type);
            this.title = new SimpleStringProperty(title);
            this.reason = new SimpleStringProperty(reason);
            this.awardingOrganization = new SimpleStringProperty(awardingOrganization);
            this.effectiveDate = new SimpleStringProperty(effectiveDate);
            this.status = new SimpleStringProperty(status);
        }

        // Getter 方法
        public String getId() { return id.get(); }
        public String getType() { return type.get(); }
        public String getTitle() { return title.get(); }
        public String getReason() { return reason.get(); }
        public String getAwardingOrganization() { return awardingOrganization.get(); }
        public String getEffectiveDate() { return effectiveDate.get(); }
        public String getStatus() { return status.get(); }
        
        // Setter 方法
        public void setId(String id) { this.id.setValue(id); }
        public void setType(String type) { this.type.set(type); }
        public void setTitle(String title) { this.title.set(title); }
        public void setReason(String reason) { this.reason.set(reason); }
        public void setAwardingOrganization(String awardingOrganization) { this.awardingOrganization.set(awardingOrganization); }
        public void setEffectiveDate(String effectiveDate) { this.effectiveDate.set(effectiveDate); }
        public void setStatus(String status) { this.status.set(status); }
        
        // Property 访问器
        public StringProperty idProperty() { return id; }
        public StringProperty typeProperty() { return type; }
        public StringProperty titleProperty() { return title; }
        public StringProperty reasonProperty() { return reason; }
        public StringProperty awardingOrganizationProperty() { return awardingOrganization; }
        public StringProperty effectiveDateProperty() { return effectiveDate; }
        public StringProperty statusProperty() { return status; }
    }
}