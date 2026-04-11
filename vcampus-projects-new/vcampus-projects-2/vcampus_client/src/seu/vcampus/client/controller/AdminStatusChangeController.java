package seu.vcampus.client.controller;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TableView;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TextField;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.VBox;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.Initializable;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.TableRow;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;


// TODO 使用socket 得到休学、转专业两个表单的额外信息
// TODO 完善一些没实现的函数

public class AdminStatusChangeController implements Initializable {
	@FXML private ScrollPane scrollPane;
	
    @FXML private TextField byStudentId;
    @FXML private TextField byName;
    @FXML private ComboBox<String> byType;
    @FXML private ComboBox<String> byStatus;
    @FXML private Button retrieve;
    @FXML private Button conditionClearButton;    
    
    @FXML private Label loadingLabel; 
    @FXML private Label errorLabel; 
    @FXML private TableView<ApplicationListEntry> applicationList;
    @FXML private TableColumn<ApplicationListEntry, String> studentIdCol;
    @FXML private TableColumn<ApplicationListEntry, String> nameCol;
    @FXML private TableColumn<ApplicationListEntry, String> typeCol;
    @FXML private TableColumn<ApplicationListEntry, String> dateCol;
    @FXML private TableColumn<ApplicationListEntry, String> reasonCol;
    @FXML private TableColumn<ApplicationListEntry, String> statusCol;
    
    @FXML private VBox applicationForm;
    @FXML private Button deselect;
    @FXML private Button pass;
    @FXML private Button refuse;
    
    @Override
    public void initialize(URL location, ResourceBundle resources) {
    	initUIState();
    	initComboBox();
    	initApplicationList();
    }
    
    private void initUIState() {
    	deselect.setDisable(true);
        pass.setDisable(true);
        refuse.setDisable(true);
    }
    
    private void initComboBox() {
    	ObservableList<String> types = FXCollections.observableArrayList(
    		"休学",
    		"转专业",
    		"退学"
    	); 
    	byType.setItems(types);
    	
    	ObservableList<String> status = FXCollections.observableArrayList(
    		"审核中",
    		"已通过",	
    		"已拒绝" // 如果这个时候撤销了
    	);
    	byStatus.setItems(status);
    }
    
    private void initApplicationList() {
    	studentIdCol.setCellValueFactory(new PropertyValueFactory<>("studentId"));
    	nameCol.setCellValueFactory(new PropertyValueFactory<>("name"));
    	typeCol.setCellValueFactory(new PropertyValueFactory<>("type"));
    	dateCol.setCellValueFactory(new PropertyValueFactory<>("date"));
    	reasonCol.setCellValueFactory(new PropertyValueFactory<>("reason"));
    	statusCol.setCellValueFactory(new PropertyValueFactory<>("status"));
    	
    	applicationList.setItems(FXCollections.observableArrayList());

    	setUpDoubleClick();    	
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

    private void handleDoubleClick(ApplicationListEntry application) {
    	switch (application.getType()) {
    		case "休学":
    			loadAbsenceForm(application);
    			break;
    		case "转专业":
    			loadMajorChangeForm(application);
    			break;
    		case "退学":
    			loadDropoutForm(application);
    			break;
    		default:
    			showError("学籍变更申请类型“" + application.getType() + "”未知！" );
    			break;
    	}
    	
    	deselect.setDisable(false);
    	if (application.getStatus().equals("待审核")) {
            pass.setDisable(false);
            refuse.setDisable(false);
    	} else {
            pass.setDisable(true);
            refuse.setDisable(true);
    	}
    	

        scrollPane.layout(); // 好像还是没用 有可能滚不到底部！
        scrollPane.setVvalue(1.0);
    }

    @FXML
    void handleRetrieve(ActionEvent event) {
    	showLoading(true);
    	// 在这里连接网络 加载 同时 应该按条件检索 这边直接检索不管他的条件
    	ObservableList<ApplicationListEntry> list = FXCollections.observableArrayList(
    			new ApplicationListEntry("20230001", "张三", "转专业", "2024-01-01", "感兴趣", "已通过"),
    			new ApplicationListEntry("20230002", "王五", "转专业", "2024-01-01", "感兴趣", "待审核"),
    			new ApplicationListEntry("20230003", "李四", "转专业", "2024-01-01", "感兴趣", "已拒绝")
    	);
    	
    	applicationList.setItems(list);
    	showLoading(false);
    }

    @FXML
    void handleClearCondition(ActionEvent event) {
    	byStudentId.clear();
    	byName.clear();
    	byType.setValue(null);
    	byStatus.setValue(null);
    }

    @FXML
    void handleDeselect(ActionEvent event) {
    	scrollPane.setVvalue(0.0);
    	applicationForm.getChildren().clear();
    	deselect.setDisable(true);
        pass.setDisable(true);
        refuse.setDisable(true);
    }

    @FXML
    void handlePass(ActionEvent event) {
    	
    }

    @FXML
    void handleRefuse(ActionEvent event) {

    }
    
    private void loadAbsenceForm(ApplicationListEntry application) {
    	applicationForm.getChildren().clear();
    	FXMLLoader loader = new FXMLLoader(getClass().getResource("/seu/vcampus/client/view/absence_form.fxml"));
    	try {
			Parent absenceForm = loader.load();
			applicationForm.getChildren().add(absenceForm);
			AbsenceFormController controller = loader.getController();
			
			controller.setName(application.getName());
			controller.setStudentId(application.getStudentId());
			controller.setReason(application.getReason());
			controller.setEditable(false);
		} catch (IOException e) {
			e.printStackTrace();
		}
    }
    

    private void loadMajorChangeForm(ApplicationListEntry application) {
    	applicationForm.getChildren().clear();
    	FXMLLoader loader = new FXMLLoader(getClass().getResource("/seu/vcampus/client/view/major_change_form.fxml"));
    	try {
			Parent majorChangeForm = loader.load();
			applicationForm.getChildren().add(majorChangeForm);
			MajorChangeFormController controller = loader.getController();
			
			controller.setName(application.getName());
			controller.setStudentId(application.getStudentId());
			controller.setReason(application.getReason());
			controller.setEditable(false);
		} catch (IOException e) {
			e.printStackTrace();
		}
    }
    
    private void loadDropoutForm(ApplicationListEntry application) {
    	applicationForm.getChildren().clear();
    	FXMLLoader loader = new FXMLLoader(getClass().getResource("/seu/vcampus/client/view/dropout_form.fxml"));
    	try {
			Parent dropoutForm = loader.load();
			applicationForm.getChildren().add(dropoutForm);
			DropoutFormController controller = loader.getController();
			
			controller.setName(application.getName());
			controller.setStudentId(application.getStudentId());
			controller.setReason(application.getReason());
			controller.setEditable(false);
		} catch (IOException e) {
			e.printStackTrace();
		}
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
            errorLabel.setStyle(isError ? "-fx-text-fill: #d32f2f;" : "-fx-text-fill: #388e3c;");
            errorLabel.setVisible(true);
            new Thread(() -> {
                try { Thread.sleep(3000); } catch (InterruptedException e) {}
                Platform.runLater(() -> errorLabel.setVisible(false));
            }).start();
        });
    }
    
    public static class ApplicationListEntry {
    	private final StringProperty studentId;
    	private final StringProperty name;
    	private final StringProperty type;
    	private final StringProperty date;
    	private final StringProperty reason;
    	private final StringProperty status;
    	
    	public ApplicationListEntry(String studentId, String name, String type, String date, String reason, String status) {
    		this.studentId = new SimpleStringProperty(studentId);
    		this.name = new SimpleStringProperty(name);
    		this.type = new SimpleStringProperty(type);
    		this.date = new SimpleStringProperty(date);
    		this.reason = new SimpleStringProperty(reason);
    		this.status = new SimpleStringProperty(status);
    	}
    	
    	public String getStudentId() { return studentId.get(); }
    	public String getName() { return name.get(); }
    	public String getType() { return type.get(); }
    	public String getDate() { return date.get(); }
    	public String getReason() { return reason.get(); }
    	public String getStatus() { return status.get(); }
    	
    	public void setStudentId(String studentId) { this.studentId.setValue(studentId); }
    	public void setName(String name) { this.name.setValue(name); }
    	public void setType(String type) { this.type.setValue(type); }
    	public void setDate(String date) { this.date.setValue(date); }
    	public void setReason(String reason) { this.reason.setValue(reason); }
    	public void setStatus(String status) { this.status.setValue(status); }
    	
    	public StringProperty studentIdProperty() { return studentId; };
    	public StringProperty nameProperty() { return name; };
    	public StringProperty typeProperty() { return type; };
    	public StringProperty dateProperty() { return date; };
    	public StringProperty reasonProperty() { return reason; };
    	public StringProperty statusProperty() { return status; };
    }
}