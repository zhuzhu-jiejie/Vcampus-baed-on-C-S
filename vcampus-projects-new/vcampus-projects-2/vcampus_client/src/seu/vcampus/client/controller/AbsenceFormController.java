package seu.vcampus.client.controller;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import javafx.fxml.FXML;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

public class AbsenceFormController {
    @FXML private Label name;
    @FXML private Label studentId;
    @FXML private DatePicker absenceStartDate;
    @FXML private DatePicker expectedResumptionDate;
    @FXML private TextField applicationReason;
    
    public String getName() { return name.getText(); }
    public String getStudentId() { return studentId.getText(); }
    public String getReason() { return applicationReason.getText(); }
    public String getAbsenceStartDate() { return absenceStartDate.getValue().toString(); }
    public String getExpectedResumptionDate() { return expectedResumptionDate.getValue().toString(); }
    public String getApplicationReason() { return expectedResumptionDate.getValue().toString(); }
    
    public void setName(String name) { this.name.setText(name); }
    public void setStudentId(String studentId) { this.studentId.setText(studentId); }
    public void setReason(String reason) { this.applicationReason.setText(reason); } 
    public void setAbsenceStartDate(String dateStr) {
    	DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        LocalDate date = LocalDate.parse(dateStr, formatter);
    	this.absenceStartDate.setValue(date); 
    }
    public void setExpectedResumptionDate(String dateStr) {
    	DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    	LocalDate date = LocalDate.parse(dateStr, formatter);
    	this.expectedResumptionDate.setValue(date);
    }
    
    public void setEditable(boolean isEditable) {
    	absenceStartDate.setEditable(isEditable);
    	expectedResumptionDate.setEditable(isEditable);
    	applicationReason.setEditable(isEditable);
    }
}