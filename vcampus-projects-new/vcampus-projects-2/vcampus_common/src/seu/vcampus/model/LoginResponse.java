package seu.vcampus.model;

import java.io.Serializable;

public class LoginResponse implements Serializable {

	private static final long serialVersionUID = -3155624109696361803L;

	private int response;

    private String username;

    private int userType;

    public LoginResponse() {}

    public LoginResponse(int response, String username, int userType) {
        this.response = response;
        this.username = username;
        this.userType = userType;
    }

    // Getter 和 Setter（必须提供，Gson 需要通过 getter 序列化）
    public int getResponse() {
    	return response;
    }
    
    public void setResponse(int response) {
        this.response = response;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public int getUserType() {
        return userType;
    }

    public void setUserType(int userType) {
        this.userType = userType;
    }

	
}
