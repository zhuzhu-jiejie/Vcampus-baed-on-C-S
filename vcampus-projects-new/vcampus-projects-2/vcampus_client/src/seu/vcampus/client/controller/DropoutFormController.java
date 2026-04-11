package seu.vcampus.client.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

public class DropoutFormController {
    @FXML private Label name;
    @FXML private Label studentId;
    @FXML private TextField applicationReason;
    
    public String getName() { return name.getText(); }
    public String getStudentId() { return studentId.getText(); }
    public String getReason() { return applicationReason.getText(); }
    
    public void setName(String name) { this.name.setText(name); }
    public void setStudentId(String studentId) { this.studentId.setText(studentId); }
    public void setReason(String reason) { this.applicationReason.setText(reason); } 
    
    public void setEditable(boolean isEditable) {
    	applicationReason.setEditable(isEditable);
    }
}
