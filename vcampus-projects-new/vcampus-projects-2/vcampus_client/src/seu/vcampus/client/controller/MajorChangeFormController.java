package seu.vcampus.client.controller;

import java.net.URL;
import java.util.ResourceBundle;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

public class MajorChangeFormController implements Initializable {

    @FXML private Label name;
    @FXML private Label studentId;
    @FXML private Label currentMajor;
    @FXML private ComboBox<String> targetMajor;
    @FXML private TextField applicationReason;

	@Override
	public void initialize(URL location, ResourceBundle resources) {
		initTargetMajor();
	}

	private void initTargetMajor() {
		// 这个应该使用网络的方式 问题是 由于这种操作很多 是不是需要session缓存一下？
		ObservableList<String> majors = FXCollections.observableArrayList(
			"计算机科学与技术",
			"电子科学与技术"
		);
		targetMajor.setItems(majors);
	}

    public String getName() { return name.getText(); }
    public String getStudentId() { return studentId.getText(); }
    public String getReason() { return applicationReason.getText(); }
    public String getCurrentMajor() { return currentMajor.getText(); }
    public String getTargetMajor() { return targetMajor.getValue(); }

    public void setName(String name) { this.name.setText(name); }
    public void setStudentId(String studentId) { this.studentId.setText(studentId); }
    public void setReason(String reason) { this.applicationReason.setText(reason); }
    public void setCurrentMajor(String currentMajor) { this.currentMajor.setText(currentMajor); }
    public void setTargetMajor(String targetMajor) { this.targetMajor.setValue(targetMajor); }

    public void setEditable(boolean isEditable) {
    	targetMajor.setEditable(isEditable);
    	applicationReason.setEditable(isEditable);
    }
}
