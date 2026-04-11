package seu.vcampus.model;

public class ChangePasswordRequest {
    private String userid;
    private String oldPassword;
    private String newPassword;
    
    public ChangePasswordRequest() {}
    
    public ChangePasswordRequest(String userid, String oldPassword, String newPassword) {
        this.userid = userid;
        this.oldPassword = oldPassword;
        this.newPassword = newPassword;
    }
    
    // Getters and Setters
    public String getUserId() { return userid; }
    public void setUserid(String userid) { this.userid = userid; }
    
    public String getOldPassword() { return oldPassword; }
    public void setOldPassword(String oldPassword) { this.oldPassword = oldPassword; }
    
    public String getNewPassword() { return newPassword; }
    public void setNewPassword(String newPassword) { this.newPassword = newPassword; }
}