package seu.vcampus.client.controller;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.google.gson.Gson;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.effect.BoxBlur;
import javafx.scene.image.Image;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundImage;
import javafx.scene.layout.BackgroundPosition;
import javafx.scene.layout.BackgroundRepeat;
import javafx.scene.layout.BackgroundSize;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.util.Duration;
import seu.vcampus.client.network.SocketManager;
import seu.vcampus.client.session.UserSession;
import seu.vcampus.model.LoginRequest;
import seu.vcampus.model.LoginResponse;

import seu.vcampus.client.util.PreferencesUtil;
import seu.vcampus.client.util.AesEncryptUtil;
import seu.vcampus.client.util.DeviceUuidUtil;

public class LoginController {
    private static final Gson gson = new Gson();

    @FXML
    private TextField userIDField;
    @FXML
    private PasswordField passwordField;
    @FXML
    private Button loginBtn;
    @FXML
    private Hyperlink changePasswordHyplink;
    @FXML
    private CheckBox rememberPasswordChkBox;
    @FXML
    private AnchorPane rootPane;
    @FXML
    private StackPane backgroundContainer;
    @FXML
    private StackPane leftImagePane;  // 确保类型为StackPane，与FXML一致
    @FXML
    private HBox mainContainer;
    @FXML
    private Button prevBtn;  // 上一张按钮
    @FXML
    private Button nextBtn;  // 下一张按钮
    @FXML
    private Button closeBtn;

    private List<String> backgroundImages;
    private int currentBackgroundIndex = 0;
    private Timeline backgroundTimeline;

    @FXML
    public void initialize() {
    	// -------------------------- 新增：解密填充密码逻辑 --------------------------
        try {
            // 1. 读取本地存储的账号和加密密码
            String rememberedUserId = PreferencesUtil.getString(PreferencesUtil.KEY_REMEMBERED_USER_ID);
            String encryptedPassword = PreferencesUtil.getString(PreferencesUtil.KEY_REMEMBERED_PASSWORD);
            
            // 2. 若两者都存在，解密密码并填充
            if (!rememberedUserId.isEmpty() && !encryptedPassword.isEmpty()) {
                String aesKey = DeviceUuidUtil.getAesKeyFromDeviceUuid();
                String plainPassword = AesEncryptUtil.decrypt(encryptedPassword, aesKey);
                
                // 填充到输入框
                userIDField.setText(rememberedUserId);
                passwordField.setText(plainPassword);
                rememberPasswordChkBox.setSelected(true); // 勾选“记住密码”
            }
        } catch (Exception e) {
            // 解密失败（如密钥变化、密文损坏），清除无效存储
            PreferencesUtil.clearLoginData();
            e.printStackTrace();
            // 不弹窗提示，避免影响用户体验（仅默默清除错误数据）
        }
        // --------------------------------------------------------------------------
    	
        // 初始化背景轮换图片列表
        backgroundImages = new ArrayList<>();
        backgroundImages.add(getClass().getResource("/seu/vcampus/client/images/background1.jpg").toExternalForm());
        backgroundImages.add(getClass().getResource("/seu/vcampus/client/images/background2.jpg").toExternalForm());
        backgroundImages.add(getClass().getResource("/seu/vcampus/client/images/background3.jpg").toExternalForm());
        backgroundImages.add(getClass().getResource("/seu/vcampus/client/images/background4.jpg").toExternalForm());
        backgroundImages.add(getClass().getResource("/seu/vcampus/client/images/background5.jpg").toExternalForm());
        backgroundImages.add(getClass().getResource("/seu/vcampus/client/images/background6.jpg").toExternalForm());
        backgroundImages.add(getClass().getResource("/seu/vcampus/client/images/background7.jpg").toExternalForm());

        // 背景容器添加模糊效果
        BoxBlur blur = new BoxBlur(5, 5, 3);
        backgroundContainer.setEffect(blur);

        // 设置初始背景


        // 启动背景轮换
        setupBackgroundRotation();

        // 确保左侧图片区域与右侧表单区域高度一致
        Platform.runLater(() -> {
            // 绑定左侧图片区域高度到右侧表单区域高度
            leftImagePane.prefHeightProperty().bind(mainContainer.heightProperty());

            // 登录卡片居中显示
            double rootWidth = rootPane.getWidth();
            double rootHeight = rootPane.getHeight();
            double containerWidth = mainContainer.getWidth();
            double containerHeight = mainContainer.getHeight();

            double centerX = (rootWidth - containerWidth) / 2;
            double centerY = (rootHeight - containerHeight) / 2;

            AnchorPane.setLeftAnchor(mainContainer, centerX);
            AnchorPane.setTopAnchor(mainContainer, centerY);
            if (!backgroundImages.isEmpty()) {
                setBackgroundImage(backgroundImages.get(0));
            }
        });
    }

    // 设置背景图片
    private void setBackgroundImage(String imageUrl) {
        Image image = new Image(imageUrl);
        BackgroundImage backgroundImage = new BackgroundImage(
            image,
            BackgroundRepeat.NO_REPEAT,
            BackgroundRepeat.NO_REPEAT,
            BackgroundPosition.CENTER,
            new BackgroundSize(100, 100, true, true, false, true)
        );
        backgroundContainer.setBackground(new Background(backgroundImage));
        leftImagePane.setBackground(new Background(backgroundImage));
    }

    // 启动背景轮换
    private void setupBackgroundRotation() {
        backgroundTimeline = new Timeline(
            new KeyFrame(Duration.seconds(3), e -> rotateBackground())
        );
        backgroundTimeline.setCycleCount(Animation.INDEFINITE);
        backgroundTimeline.play();
    }

    // 自动轮换背景（直接切换）
    private void rotateBackground() {
        if (backgroundImages.isEmpty()) {
			return;
		}
        currentBackgroundIndex = (currentBackgroundIndex + 1) % backgroundImages.size();
        setBackgroundImage(backgroundImages.get(currentBackgroundIndex));
    }

    // 上一张按钮点击事件
    @FXML
    private void onPrevClick() {
        if (backgroundImages.isEmpty()) {
			return;
		}

        // 暂停自动切换，切换完成后重新开始计时
        backgroundTimeline.stop();

        // 计算上一张索引（处理边界情况）
        currentBackgroundIndex = (currentBackgroundIndex - 1 + backgroundImages.size()) % backgroundImages.size();
        setBackgroundImage(backgroundImages.get(currentBackgroundIndex));

        // 重新开始自动切换计时
        backgroundTimeline.playFromStart();
    }

    // 下一张按钮点击事件
    @FXML
    private void onNextClick() {
        if (backgroundImages.isEmpty()) {
			return;
		}

        // 暂停自动切换，切换完成后重新开始计时
        backgroundTimeline.stop();

        // 计算下一张索引
        currentBackgroundIndex = (currentBackgroundIndex + 1) % backgroundImages.size();
        setBackgroundImage(backgroundImages.get(currentBackgroundIndex));

        // 重新开始自动切换计时
        backgroundTimeline.playFromStart();
    }

    // 登录按钮点击
    @FXML
    private void onLoginClick() {
        String userID = userIDField.getText().trim();
        String password = passwordField.getText().trim();

        if (userID.isEmpty() || password.isEmpty()) {
            showAlert(AlertType.WARNING, "输入错误", "账号或密码不能为空", "请检查并重新输入");
            return;
        }

        new Thread(() -> {
            try {
                // 登录时创建一次Socket

                // 初始化流并保存到SocketManager
                SocketManager.getInstance();

                // 使用SocketManager的out流发送登录请求
                DataOutputStream out = SocketManager.getInstance().getOut();

                out.writeUTF("LoginRequest");

                LoginRequest loginReq = new LoginRequest(userID, password);

                String jsonReq = gson.toJson(loginReq);
                out.writeUTF(jsonReq);
                out.flush();

                // 使用SocketManager的in流接收响应
                DataInputStream in = SocketManager.getInstance().getIn();

                String jsonRsp = in.readUTF();
                LoginResponse loginRsp = gson.fromJson(jsonRsp, LoginResponse.class);

                int response = loginRsp.getResponse();
                int type = loginRsp.getUserType();
                String username = loginRsp.getUsername();

                Platform.runLater(() -> handleLoginResponse(response, userID, type, username));

            } catch (Exception e) {
                Platform.runLater(() ->
                    showAlert(AlertType.ERROR, "连接失败", "无法连接到服务器",
                             "请检查服务器是否启动或网络是否正常：" + e.getMessage())
                );
                e.printStackTrace();
            }
        }).start();
    }

    // 处理登录响应
    private void handleLoginResponse(int response, String userId, int type, String username) {
    	if (response == 1) {
    		// -------------------------- 新增：密码加密存储逻辑 --------------------------
            try {
                // 1. 获取AES密钥（从设备UUID生成，同一设备固定）
                String aesKey = DeviceUuidUtil.getAesKeyFromDeviceUuid();
                
                // 2. 根据“记住密码”状态，存储/清除账号和密码
                if (rememberPasswordChkBox.isSelected()) {
                    // 勾选：存储账号 + 加密后的密码
                    PreferencesUtil.putString(PreferencesUtil.KEY_REMEMBERED_USER_ID, userId);
                    // 获取明文密码，加密后存储
                    String plainPassword = passwordField.getText().trim();
                    String encryptedPassword = AesEncryptUtil.encrypt(plainPassword, aesKey);
                    PreferencesUtil.putString(PreferencesUtil.KEY_REMEMBERED_PASSWORD, encryptedPassword);
                } else {
                    // 未勾选：清除本地存储的账号和密码
                    PreferencesUtil.clearLoginData();
                }
            } catch (Exception e) {
                // 加密失败时，仅清除存储（避免存储无效数据）
                PreferencesUtil.clearLoginData();
                e.printStackTrace();
                showAlert(AlertType.WARNING, "存储失败", "记住密码功能异常", "无法安全存储密码，请重试");
            }
            // --------------------------------------------------------------------------

        	switch(type) {
        		case 1: // 学生
        			try {
        				FXMLLoader loader = new FXMLLoader(getClass().getResource("/seu/vcampus/client/view/student_portal.fxml"));
                        Parent root = loader.load();
                        StudentPortalController studentController = loader.getController();
                        studentController.setStudentName(username);
                        studentController.setStudentId(userId);

                        Stage stage = (Stage) loginBtn.getScene().getWindow();
                        Scene scene = new Scene(root);
                        scene.setFill(Color.TRANSPARENT); // 设置圆角的必备条件
                        stage.setScene(scene);
                        stage.setTitle("学生应用中心");
                        stage.centerOnScreen();
                        stopBackgroundRotation();
        			} catch(IOException e) {
        				e.printStackTrace();
                    	showAlert(AlertType.ERROR, "界面加载失败", "无法加载学生应用中心",
                    			"请检查文件路径是否正确: " + e.getMessage());
                    }
        			break;
        		case 2: // 教师
                    try {
                        FXMLLoader loader = new FXMLLoader(getClass().getResource("/seu/vcampus/client/view/teacher_portal.fxml"));
                        Parent root = loader.load();
                        TeacherPortalController teacherController = loader.getController();
                        teacherController.setTeacherName(username);
                        teacherController.setTeacherId(userId);

                        Stage stage = (Stage) loginBtn.getScene().getWindow();
                        Scene scene = new Scene(root);
                        scene.setFill(Color.TRANSPARENT);
                        stage.setScene(scene);
                        stage.setTitle("教师应用中心");
                        stage.centerOnScreen();
                        stopBackgroundRotation();
                    } catch(IOException e) {
                        e.printStackTrace();
                        showAlert(AlertType.ERROR, "界面加载失败", "无法加载教师应用中心",
                                "请检查文件路径是否正确: " + e.getMessage());
                    }
                    break;
                case 3: // 宿舍管理员
                case 4: // 图书管理员
                case 5: // 商店管理员
                case 6: // 学籍管理员
                case 7: // 选课管理员
                    try {
                        FXMLLoader loader = new FXMLLoader(getClass().getResource("/seu/vcampus/client/view/admin_portal.fxml"));
                        Parent root = loader.load();
                        AdministratorPortalController adminController = loader.getController();
                        adminController.setAdminName(username);
                        adminController.setAdminId(userId);
                        adminController.setAdminType(type);

                        Stage stage = (Stage) loginBtn.getScene().getWindow();
                        Scene scene = new Scene(root);
                        scene.setFill(Color.TRANSPARENT);
                        stage.setScene(scene);
                        stage.setTitle("管理员应用中心");
                        stage.centerOnScreen();
                        stopBackgroundRotation();
                    } catch(IOException e) {
                        e.printStackTrace();
                        showAlert(AlertType.ERROR, "界面加载失败", "无法加载教师应用中心",
                                "请检查文件路径是否正确: " + e.getMessage());
                    }
                    break;
                default:
                    showAlert(AlertType.ERROR, "未知用户类型", "无法识别用户类型: " + type,
                            "请联系系统管理员");
                }
        	UserSession.getInstance().setUserInfo(userId, username, type);
        } else if (response == 0) {
            showAlert(AlertType.WARNING, "登录失败", "账号或密码错误", "请检查你的账号和密码是否正确");
        } else if (response == -1) {
            showAlert(AlertType.ERROR, "服务器错误", "服务器处理失败", "请稍后再试");
        }else if (response==-2) {
        	showAlert(AlertType.ERROR, "登录失败", "账号已经其他地方登录", "请退出后再试");
        }
    }

    // 显示提示框
    private void showAlert(AlertType type, String title, String header, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.showAndWait();
    }

    // 密码修改链接
    @FXML
    private void onChangePasswordClick() {
        showAlert(AlertType.INFORMATION, "功能提示", "密码修改", "请联系管理员修改密码");
    }
    
    @FXML
    private void onCloseClick() {
    	 // 停止背景轮换
        stopBackgroundRotation();
        
        // 关闭Socket连接
        SocketManager.getInstance().close();
        
        // （可选）公共设备场景：关闭时清除本地存储的密码（账号可保留）
        // PreferencesUtil.remove(PreferencesUtil.KEY_REMEMBERED_PASSWORD);
        
        
        // 获取当前窗口并关闭
        Stage stage = (Stage) closeBtn.getScene().getWindow();
        stage.close();
    }

    // 停止背景轮换
    /*@Override
    public void stop() {
        // 清理资源
        RequestManager.getInstance().shutdown();
        SocketManager.getInstance().close();
    }*/
    public void stopBackgroundRotation() {
        if (backgroundTimeline != null) {
            backgroundTimeline.stop();
        }
    }
}
