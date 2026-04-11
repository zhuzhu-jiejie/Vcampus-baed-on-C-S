package seu.vcampus.model;

import java.io.Serializable;

public class LoginRequest implements Serializable {
	
	// 客户端和服务端会比较这个ID 保证序列化与反序列化使用的是同一个类
	private static final long serialVersionUID = 2292037296486915448L;
	
	private String userID;
	private String password;
	
	public LoginRequest() {}

	public LoginRequest(String userID, String password) {
		this.userID = userID;
		this.password = password;
	}
	
	public void setUserID(String userID) {
		this.userID = userID;
	}
	
	public String getUserID() {
		return userID;
	}
	
	public void setPassword(String password) {
		this.password = password;
	}
	
	public String getPassword() {
		return password;
	}
}
