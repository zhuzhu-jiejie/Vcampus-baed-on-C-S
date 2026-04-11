package seu.vcampus.client.controller;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.URL;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ResourceBundle;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.TableView;
import javafx.scene.layout.VBox;
import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.fxml.Initializable;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.StringProperty;
import javafx.application.Platform;
import javafx.concurrent.Task;

import seu.vcampus.client.network.SocketManager;
import seu.vcampus.client.session.UserSession;
import seu.vcampus.model.StatusChangeApplication;
import seu.vcampus.model.AbsenceInfo;

//TODO 这个真的好难！！！之后再做

public class StudentStatusChangeController implements Initializable {
	// 表单
	private Parent absenceForm;
	private Parent majorChangeForm;
	private Parent dropoutForm;
	
	private Parent currentForm; // 记住当前表单 服务于关闭表单操作
	
    @FXML private Button absenceButton;
    @FXML private Button majorChangeButton;
    @FXML private Button dropoutButton;
    
    @FXML private VBox applicationForm;
    
    @FXML private Button submitButton;
    @FXML private Button closeFormButton;
    @FXML private Button revokeButton;
    
    @FXML private Label loadingLabel;
    @FXML private Label errorLabel;
    @FXML private TableView<ApplicationListEntry> applicationList;
    @FXML private TableColumn<ApplicationListEntry, String> typeCol;
    @FXML private TableColumn<ApplicationListEntry, String> dateCol;
    @FXML private TableColumn<ApplicationListEntry, String> reasonCol;
    @FXML private TableColumn<ApplicationListEntry, String> statusCol;
    
    private static final Gson gson = new Gson();
    private DataInputStream dataIn;
    private DataOutputStream dataOut;
    private ObservableList<ApplicationListEntry> applications = FXCollections.observableArrayList();

	@Override
	public void initialize(URL location, ResourceBundle resources) {
		initApplicationList();
		initNetworkConnections();
		loadStudentApplications();
		setUpDoubleClick();
	}
	
	private void initApplicationList() {
		typeCol.setCellValueFactory(new PropertyValueFactory<>("type"));
		dateCol.setCellValueFactory(new PropertyValueFactory<>("date"));
		reasonCol.setCellValueFactory(new PropertyValueFactory<>("reason"));
		statusCol.setCellValueFactory(new PropertyValueFactory<>("status"));
		
		applicationList.setItems(FXCollections.observableArrayList());
	}
	
	
	private void initNetworkConnections() { // TODO 其它controller中也有这些 希望能统一
		try {
			dataIn = SocketManager.getInstance().getIn();
			dataOut = SocketManager.getInstance().getOut();
		} catch (Exception e) {
			showError("网络连接失败，无法连接至服务器，请检查网络设置");
			e.printStackTrace();
		}
	}
	
	private void loadStudentApplications() {
		showLoading(true);
		
		Task<ObservableList<StatusChangeApplication>> loadTask = new Task<>() {
			@Override
			protected ObservableList<StatusChangeApplication> call() throws Exception {
				dataOut.writeUTF("GET_STATUS_APPLICATIONS");
				ObservableList<StatusChangeApplication> theList = FXCollections.observableArrayList();
				int count = dataIn.readInt();
				for (int i = 0; i < count; ++i) {
					String json = dataIn.readUTF();
					StatusChangeApplication temp = gson.fromJson(json, StatusChangeApplication.class);
					theList.add(temp);
				}
				return theList;
			}
			
			@Override
			protected void succeeded() {
				super.succeeded();
				ObservableList<StatusChangeApplication> theList = getValue();
				for (StatusChangeApplication i : theList) {
					applications.add(new ApplicationListEntry(i.getId(), i.getType(), i.getDate(), i.getReason(), i.getStatus()));
				}
				
				applicationList.setItems(applications);
				showLoading(false);
			}
			
			@Override
			protected void failed() {
				showError("发生错误");
				showLoading(false);
			}
		};
		
		
		new Thread(loadTask).start();
 	}
	
    private void setUpDoubleClick() {
        applicationList.setRowFactory(tv -> {
            TableRow<ApplicationListEntry> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty()) {
                    handleDoubleClick(row.getItem());
                }
            });
            return row;
        });
    }
    
    private void handleDoubleClick(ApplicationListEntry entry) {
    	switch(entry.getType()) {
    		case "休学":
    			loadAbsenceForm(entry);
    			break;
    		case "转专业":
    			loadMajorChangeForm(entry);
    			break;
    		case "退学":
    			loadDropoutForm(entry);
    			break;
    		default:
    			showError("学籍变更申请类型“" + entry.getType() + "”未知！" );
    			break;
    			
    	}
    }
    private void loadAbsenceForm(ApplicationListEntry entry) {
        // 清空现有表单并释放资源
        applicationForm.getChildren().clear();
        if (currentForm == absenceForm) {
            absenceForm = null;
        } else if (currentForm == majorChangeForm) {
            majorChangeForm = null;
        } else if (currentForm == dropoutForm) {
            dropoutForm = null;
        }
        currentForm = null;
        
        showLoading(true);
        final AbsenceFormController controller; // 保存控制器引用 必须是final
       
        try {
            // 先加载表单UI，获取控制器实例
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/seu/vcampus/client/view/absence_form.fxml"));
            currentForm = loader.load();
            controller = loader.getController(); // 获取控制器
            applicationForm.getChildren().add(currentForm);
            
            // 填充已知的基础数据
            controller.setName(UserSession.getInstance().getCurrentUserName());
            controller.setStudentId(UserSession.getInstance().getCurrentUserId());
            controller.setReason(entry.getReason());
            controller.setEditable(false);
            
        } catch (Exception e) {
            e.printStackTrace();
            showError("加载表单失败");
            showLoading(false);
            return; // 加载失败直接返回
        }
        
        // 启动后台任务获取详细信息
        Task<AbsenceInfo> loadTask = new Task<>() {
            @Override
            protected AbsenceInfo call() throws Exception {
                dataOut.writeUTF("GET_ABSENCE_INFO");
                dataOut.writeInt(entry.getId()); // 发送申请ID
                dataOut.flush();
                
                String json = dataIn.readUTF();
                if (json.startsWith("ERROR|")) {
                    throw new Exception(json.substring(6)); // 抛出服务器返回的错误
                }
                return gson.fromJson(json, AbsenceInfo.class);
            }
            
            @Override
            protected void succeeded() {
                super.succeeded();
                showLoading(false);
                AbsenceInfo absenceInfo = getValue();
                
                // 将服务器返回的详细数据填充到表单
                if (absenceInfo != null && controller != null) {
                    controller.setAbsenceStartDate(absenceInfo.getStartDate());
                    controller.setExpectedResumptionDate(absenceInfo.getEndDate());
                }
            }
            
            @Override
            protected void failed() {
                super.failed();
                showLoading(false);
                showError("加载休学详情失败：" + getException().getMessage());
            }
        };
        
        new Thread(loadTask).start();
    }
    
    private void loadMajorChangeForm(ApplicationListEntry application) {
        applicationForm.getChildren().clear();
        final MajorChangeFormController controller; // 声明为final供内部类访问
        try {
            // 加载转专业表单FXML
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/seu/vcampus/client/view/major_change_form.fxml"));
            Parent majorChangeForm = loader.load();
            controller = loader.getController(); // 获取控制器实例
            
            // 设置基本信息
            controller.setName(UserSession.getInstance().getCurrentUserName());
            controller.setStudentId(UserSession.getInstance().getCurrentUserId());
            controller.setReason(application.getReason());
            controller.setEditable(false);
            
            // 添加表单到容器
            applicationForm.getChildren().add(majorChangeForm);
            
            // 启动后台任务获取目标专业信息
            showLoading(true);
            Task<String> loadTask = new Task<>() {
                @Override
                protected String call() throws Exception {
                    // 发送查询转专业信息的请求
                    dataOut.writeUTF("GET_MAJOR_TRANSFER_INFO");
                    dataOut.writeInt(application.getId()); // 发送申请ID
                    dataOut.flush();
                    
                    // 接收服务器响应
                    String response = dataIn.readUTF();
                    if (response.startsWith("ERROR|")) {
                        throw new Exception(response.substring(6)); // 抛出错误信息
                    }
                    return response; // 直接返回目标专业字符串
                }
                
                @Override
                protected void succeeded() {
                    super.succeeded();
                    showLoading(false);
                    String targetMajor = getValue();
                    // 设置目标专业到表单
                    if (targetMajor != null && !targetMajor.isEmpty()) {
                        controller.setTargetMajor(targetMajor);
                    } else {
                        showError("未获取到目标专业信息");
                    }
                }
                
                @Override
                protected void failed() {
                    super.failed();
                    showLoading(false);
                    showError("加载转专业信息失败：" + getException().getMessage());
                }
            };
            
            new Thread(loadTask).start();
            
        } catch (IOException e) {
            e.printStackTrace();
            showError("加载转专业表单失败");
        }
    }
    
    // TODO 设置成不可修改 然后设置成从服务端读取就ok了
    
    private void loadDropoutForm(ApplicationListEntry entry) {
    	applicationForm.getChildren().clear();
    	
    	if (currentForm == absenceForm) { // 把它们看成指针 指向同一块内存区域自然相同
    		absenceForm = null; // 由JVM回收
    	} else if (currentForm == majorChangeForm) {
    		majorChangeForm = null;
    	} else if (currentForm == dropoutForm) {
    		dropoutForm = null;
    	}
    	
    	currentForm = null; // 自己也放弃指向 才会真正回收
    	try {
    		FXMLLoader loader = new FXMLLoader(getClass().getResource("/seu/vcampus/client/view/dropout_form.fxml"));
    		currentForm = loader.load();
    		DropoutFormController controller = loader.getController();
    		applicationForm.getChildren().add(currentForm);
    		controller.setName(UserSession.getInstance().getCurrentUserName());
    		controller.setStudentId(UserSession.getInstance().getCurrentUserId());
    		controller.setReason(entry.getReason());
    	} catch (Exception e) {
    		e.printStackTrace();
    	}
    }
	
    @FXML void displayAbsenceForm(ActionEvent event) {
    	try {
        	applicationForm.getChildren().clear();
        	if(absenceForm == null) {
        		FXMLLoader loader = new FXMLLoader(getClass().getResource("/seu/vcampus/client/view/absence_form.fxml"));

            	absenceForm = loader.load();
            	
        		AbsenceFormController controller = loader.getController();
        		controller.setName(UserSession.getInstance().getCurrentUserName());
        		controller.setStudentId(UserSession.getInstance().getCurrentUserId());
        	}
        	applicationForm.getChildren().add(absenceForm);
        	currentForm = absenceForm;
    	} catch (Exception e) {
    		System.err.println("加载页面失败！检查路径：" + "/seu/vcampus/client/view/absence_form.fxml");
    		e.printStackTrace();
    	}
    }

    @FXML
    void displayMajorChangeForm(ActionEvent event) {
    	try {
        	applicationForm.getChildren().clear();
        	if(majorChangeForm == null) {
        		FXMLLoader loader = new FXMLLoader(getClass().getResource("/seu/vcampus/client/view/major_change_form.fxml"));

        		majorChangeForm = loader.load();
        		
        		MajorChangeFormController controller = loader.getController();
        		controller.setName(UserSession.getInstance().getCurrentUserName());
        		controller.setStudentId(UserSession.getInstance().getCurrentUserId());
        		controller.setCurrentMajor("暂未处理");
        	}
        	applicationForm.getChildren().add(majorChangeForm);
        	currentForm = majorChangeForm;
    	} catch (Exception e) {
    		System.err.println("加载页面失败！检查路径：" + "/seu/vcampus/client/view/major_change_form.fxml");
    		e.printStackTrace();
    	}
    }

    @FXML
    void displayDropoutForm(ActionEvent event) {
    	try {
        	applicationForm.getChildren().clear();
        	if(dropoutForm == null) {
        		FXMLLoader loader = new FXMLLoader(getClass().getResource("/seu/vcampus/client/view/dropout_form.fxml"));

        		dropoutForm = loader.load();
        		
        		DropoutFormController controller = loader.getController();
        		controller.setName(UserSession.getInstance().getCurrentUserName());
        		controller.setStudentId(UserSession.getInstance().getCurrentUserId());
        	}
        	applicationForm.getChildren().add(dropoutForm);
        	currentForm = dropoutForm;
    	} catch (Exception e) {
    		System.err.println("加载页面失败！检查路径：" + "/seu/vcampus/client/view/dropout_form.fxml");
    		e.printStackTrace();
    	}
    }

    @FXML
    void handleSubmission(ActionEvent event) {
    	String type;
    	
    	if (currentForm == absenceForm) {
    		type = "休学";
    	} else if (currentForm == majorChangeForm) {
    		type = "转专业";
    	} else if (currentForm == dropoutForm) {
    		type = "退学";
    	} else {
    		type = "invalid";
    	}
    	
    	switch(type) {
    		case "休学":
    			break;
    		case "转专业":
    			break;
    		case "退学":
    			break;
    		default:
    			showError("未知类型表单");
    			break;
    	}
    	
    	System.out.println(type);
    }

    @FXML
    void handleFormClose(ActionEvent event) {
    	applicationForm.getChildren().clear();
    	
    	if (currentForm == absenceForm) { // 把它们看成指针 指向同一块内存区域自然相同
    		absenceForm = null; // 由JVM回收
    	} else if (currentForm == majorChangeForm) {
    		majorChangeForm = null;
    	} else if (currentForm == dropoutForm) {
    		dropoutForm = null;
    	}
    	
    	currentForm = null; // 自己也放弃指向 才会真正回收
    }

    @FXML
    void handleRevocation(ActionEvent event) {

    	
    }
    
    
    
    
    private void showLoading(boolean show) {
        Platform.runLater(() -> loadingLabel.setVisible(show));
    }

    private void showError(String msg) {
        showError(msg, true);
    }

    private void showError(String msg, boolean isError) {
        Platform.runLater(() -> {
            errorLabel.setText(msg);
            // 设置文字颜色（错误=红色，成功=绿色，与 Teacher 控制器一致）
            errorLabel.setStyle(isError ? 
                "-fx-text-fill: #d32f2f; -fx-font-size: 12px;" : 
                "-fx-text-fill: #388e3c; -fx-font-size: 12px;"
            );
            errorLabel.setVisible(true);

            // 3秒后自动隐藏提示（新线程延迟执行）
            new Thread(() -> {
                try {
                    Thread.sleep(3000); // 延迟3秒
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                // 回到 FX 线程隐藏标签
                Platform.runLater(() -> errorLabel.setVisible(false));
            }).start();
        });
    }
    
    public static class ApplicationListEntry {
    	private final IntegerProperty id;
        private final StringProperty type;
        private final StringProperty date;
        private final StringProperty reason;
        private final StringProperty status;

        public ApplicationListEntry(Integer id, String type, String date, String reason, String status) {
        	this.id = new SimpleIntegerProperty(id);
            this.type = new SimpleStringProperty(type);
            this.date = new SimpleStringProperty(date);
            this.reason = new SimpleStringProperty(reason);
            this.status = new SimpleStringProperty(status);
        }

        // Getter and Setter for type
        public Integer getId() { return id.get(); }
        public void setId(Integer id) { this.id.set(id); }
        
        // Getter and Setter for type
        public String getType() { return type.get(); }
        public void setType(String type) { this.type.set(type); }

        // Getter and Setter for date
        public String getDate() { return date.get(); }
        public void setDate(String date) { this.date.set(date); }

        // Getter and Setter for reason
        public String getReason() { return reason.get(); }
        public void setReason(String reason) { this.reason.set(reason); }

        // Getter and Setter for status
        public String getStatus() { return status.get(); }
        public void setStatus(String status) { this.status.set(status); }
    }
}
