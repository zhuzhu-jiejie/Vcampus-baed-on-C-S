package seu.vcampus.client.controller;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.lang.reflect.Type;
import java.net.URL;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import javafx.animation.FadeTransition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;
import javafx.util.Duration;
import seu.vcampus.client.network.SocketManager;
import seu.vcampus.client.session.CampusSession;
import seu.vcampus.model.ClassInfo;
import seu.vcampus.model.CollegeInfo;
import seu.vcampus.model.MajorInfo;
import seu.vcampus.util.LocalDateAdapter;


public class AdministratorPortalController implements Initializable {
    // 所有变量定义保持不变
    @FXML private BorderPane rootPane;
    @FXML private HBox titleBar;
    @FXML private Button vcampusButton;
    @FXML private StackPane leftStack;
    @FXML private VBox appSidebar;
    @FXML private VBox sidebarContainer;
    @FXML private VBox contentArea;
    @FXML private Button minimizeButton;
    @FXML private Button closeButton;

    private double xOffset = 0;
    private double yOffset = 0;
    private Map<String, List<String>> appSubFuncMap;
    private Button currentSelectedSidebarButton;
    private Label welcomeLabel;
    private String adminid;
    private int admintype;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        initAppSubFuncMap();
        setupClip();
        setupDrag();
        setupVcampusClick();
    }

    // 其他方法保持不变...
    private void initAppSubFuncMap() {
        // 原有代码不变
        appSubFuncMap = new HashMap<>();
        appSubFuncMap.put("个人信息", Arrays.asList("基本资料",  "修改密码", "登出"));
        appSubFuncMap.put("学籍管理", Arrays.asList("学籍信息", "成绩查询", "奖惩记录"));
        appSubFuncMap.put("选课系统", Arrays.asList("课程发布", "排课"));
        appSubFuncMap.put("宿舍管理", Arrays.asList("宿舍信息", "水电费催缴", "退换舍申请处理", "报修申请处理"));
        appSubFuncMap.put("图书馆", Arrays.asList("图书管理","研讨间管理"));
        appSubFuncMap.put("商店", Arrays.asList("商品管理","订单管理","财务报表"));
    }

    // 修改此方法：使用自定义图片作为图标
    private void initAppSidebarButtons() {
        // 应用名称与对应图片路径的映射（请替换为你的图片路径）
        Map<String, String> appImageMap = new HashMap<>();
        switch(admintype) {
        case 6:appImageMap.put("个人信息", "/seu/vcampus/client/images/profileicon.png");      // 个人信息图标
               appImageMap.put("学籍管理", "/seu/vcampus/client/images/xuejiicon.png");     // 学籍管理图标
               break;
        case 7:appImageMap.put("个人信息", "/seu/vcampus/client/images/profileicon.png");      // 个人信息图标
               appImageMap.put("选课系统", "/seu/vcampus/client/images/courseicon.png");      // 选课系统图标
               break;
        case 3:appImageMap.put("个人信息", "/seu/vcampus/client/images/profileicon.png");      // 个人信息图标
               appImageMap.put("宿舍管理", "/seu/vcampus/client/images/dormitoryicon.png");        // 宿舍管理图标
               break;
        case 4:appImageMap.put("个人信息", "/seu/vcampus/client/images/profileicon.png");      // 个人信息图标
               appImageMap.put("图书馆", "/seu/vcampus/client/images/libraryicon.png");       // 图书馆图标
               break;
        case 5:appImageMap.put("个人信息", "/seu/vcampus/client/images/profileicon.png");      // 个人信息图标
               appImageMap.put("商店", "/seu/vcampus/client/images/storeicon.png");            // 商店图标
               break;
        }
        

        for (String app : appImageMap.keySet()) {
            // 加载图片（确保图片文件存在于指定路径）
            Image iconImage = new Image(getClass().getResourceAsStream(appImageMap.get(app)));
            ImageView icon = new ImageView(iconImage);

            // 设置图片大小（与按钮比例适配）
            icon.setFitWidth(16);   // 图片宽度
            icon.setFitHeight(16);  // 图片高度
            icon.setPreserveRatio(true); // 保持比例

            // 组合图标和文字
            HBox buttonContent = new HBox();
            buttonContent.setAlignment(Pos.CENTER_LEFT);
            buttonContent.getChildren().addAll(icon, new Label(app));
            HBox.setMargin(icon, new Insets(0, 10, 0, 0)); // 图标与文字间距

            // 创建按钮并设置内容
            Button appBtn = new Button();
            appBtn.setGraphic(buttonContent);

            // 保留原有事件逻辑
            appBtn.setOnAction(e -> {
                loadSubFuncByApp(app);
                hideAppSidebar();
            });

            appSidebar.getChildren().add(appBtn);
        }
    }

    // 以下所有方法保持不变...
    private void loadDefaultSubFunc() {
        // 原有代码不变
        loadSubFuncByApp("个人信息");
        List<String> defaultSubFuncs = appSubFuncMap.get("个人信息");
        if (defaultSubFuncs != null && !defaultSubFuncs.isEmpty()) {
            handleSidebarClick(defaultSubFuncs.get(0));
        }
    }

    private void loadSubFuncByApp(String selectedApp) {
        if ("学籍管理".equals(selectedApp)) { // TODO 在这里添加 使得所有需要的都被加载
            CampusSession campusSession = CampusSession.getInstance();
            if (!campusSession.isDataLoaded()) {
                new Thread(() -> {
                    try {
                        SocketManager socketManager = SocketManager.getInstance();
                        DataOutputStream out = socketManager.getOut();
                        DataInputStream in = socketManager.getIn();

                        out.writeUTF("GetCampusData");
                        out.flush();
                        
                        Gson gson = new GsonBuilder()
                        	    .registerTypeAdapter(LocalDate.class, new LocalDateAdapter())
                        	    .create();
                        
                		String jsonColleges = in.readUTF();
                		String jsonCollegeMajorsMap = in.readUTF();;
                		String jsonMajorClassesMap = in.readUTF();;
                		String jsonClassRooms = in.readUTF();;
                		String jsonSeverDate = in.readUTF();;
                		String jsonFoundingDate = in.readUTF();;
                        
                		Type collegeInfoListType = new TypeToken<List<CollegeInfo>>() {}.getType();
                		Type collegeMajorsMapType = new TypeToken<Map<String, List<MajorInfo>>>() {}.getType();
                		Type majorClassesMapType = new TypeToken<Map<String, List<ClassInfo>>>() {}.getType();
                		Type stringList = new TypeToken<List<String>>() {}.getType();
                		
                		List<CollegeInfo> colleges = gson.fromJson(jsonColleges, collegeInfoListType);
                        Map<String, List<MajorInfo>> collegeMajorsMap = gson.fromJson(jsonCollegeMajorsMap, collegeMajorsMapType);
                        Map<String, List<ClassInfo>> majorClassesMap = gson.fromJson(jsonMajorClassesMap, majorClassesMapType);
                        List<String> classRooms = gson.fromJson(jsonClassRooms, stringList);
                        LocalDate serverDate = gson.fromJson(jsonSeverDate, LocalDate.class);
                        LocalDate foundingDate = gson.fromJson(jsonFoundingDate, LocalDate.class);
                		
                        campusSession.initData(colleges, collegeMajorsMap, majorClassesMap, classRooms, serverDate, foundingDate);
                		
                        Platform.runLater(() -> {
                            showSubFunctions(selectedApp);
                        });
                    } catch (Exception e) {
                        Platform.runLater(() -> {
                            showErrorAlert("数据加载失败", "无法获取校园数据，请检查网络后重试：" + e.getMessage());
                        });
                        e.printStackTrace();
                    }
                }).start();
                return;
            }
        }
        showSubFunctions(selectedApp);
    }

    // 抽取显示子功能的逻辑为单独方法
    private void showSubFunctions(String selectedApp) {
        sidebarContainer.getChildren().clear();
        List<String> subFuncs = appSubFuncMap.get(selectedApp);
        if (subFuncs == null) {
            return;
        }
        for (String func : subFuncs) {
            Button funcBtn = new Button(func);
            funcBtn.setPrefWidth(150);
            funcBtn.setPrefHeight(36);
            funcBtn.setOnAction(e -> handleSidebarClick(func));
            sidebarContainer.getChildren().add(funcBtn);
        }
    }

    // 其他方法（handleSidebarClick、getFXMLPath、loadFXMLToContent等）均保持不变

    // 修改handleSidebarClick方法，添加对报修申请处理的特殊处理
    private void handleSidebarClick(String funcName) {
        updateSidebarSelection(funcName);
        String fxmlPath = getFXMLPath(funcName);
        Object controller = loadFXMLToContent(fxmlPath); // 加载页面并获取控制器实例
        
        // 根据不同页面调用对应的刷新方法
        if (controller != null) {
            if (controller instanceof AdminDormitoryInfoController) {
                // 宿舍信息页面刷新
                ((AdminDormitoryInfoController) controller).refreshData();
            } else if (controller instanceof AdminReminderController) {
                // 水电费催缴页面刷新
                ((AdminReminderController) controller).refreshData();
            } else if (controller instanceof AdminRepairController) {
                // 报修申请处理页面刷新
                ((AdminRepairController) controller).refreshData();
            } else if (controller instanceof AdminExchangeController) {
                // 退换舍申请处理页面刷新
                ((AdminExchangeController) controller).refreshData();
            } 
            // 可根据需要添加其他页面的刷新处理
        }
    }
    private String getFXMLPath(String funcName) {
        String basePath = "/seu/vcampus/client/view/";
        switch (funcName) {
            case "基本资料": return basePath + "admin_info.fxml";
            case "修改密码": return basePath + "change_password.fxml";
            case "登出": return basePath + "logout.fxml";
            case "学籍信息": return basePath + "admin_student_profile.fxml";
            case "成绩查询": return basePath + "admin_grades.fxml";
            case "奖惩记录": return basePath + "admin_reward_punishment.fxml";
            // case "学籍变动": return basePath + "admin_status_change.fxml";
            case "课程发布": return basePath + "course_public.fxml";
            case "排课": return basePath + "course_schedule.fxml";           
            case "宿舍信息": return basePath + "admin_dormitory_info.fxml";
            case "水电费催缴": return basePath + "admin_reminder.fxml";
            case "退换舍申请处理": return basePath + "admin_exchange.fxml";
            case "报修申请处理": return basePath + "admin_repair.fxml";
            case "图书管理": return basePath + "library_manage1.fxml";
            case "研讨间管理": return basePath + "library_manage2.fxml";
            case "商品管理": return basePath + "store_manage.fxml";
            case "订单管理": return basePath + "admin_order_manage.fxml";
            case "财务报表": return basePath + "store_manage3.fxml";
            default: return basePath + "admin_info.fxml";
        }
    }

    private Object loadFXMLToContent(String fxmlPath) {
        try {
            contentArea.getChildren().clear();
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent content = loader.load();
            
            // 将ID传给相应的控制器
            if (fxmlPath.contains("admin_info.fxml")) {
                AdminInfoController infoController = loader.getController();
                infoController.setAdminId(adminid);
            } else if (fxmlPath.contains("change_password.fxml")) {
                ChangePasswordController passwordController = loader.getController();
                passwordController.setUserId(adminid);
            }
            
            FadeTransition fadeIn = new FadeTransition(Duration.millis(300), content);
            fadeIn.setFromValue(0);
            fadeIn.setToValue(1);
            contentArea.getChildren().add(content);
            fadeIn.play();
            
            // 返回控制器实例
            return loader.getController();
        } catch (IOException e) {
            System.err.println("加载页面失败！检查路径：" + fxmlPath);
            e.printStackTrace();
            return null;
        }
    }
    private void setupClip() {
        Rectangle clip = new Rectangle();
        rootPane.layoutBoundsProperty().addListener((obs, oldBounds, newBounds) -> {
            clip.setWidth(newBounds.getWidth());
            clip.setHeight(newBounds.getHeight());
            clip.setArcWidth(30);
            clip.setArcHeight(30);
        });
        rootPane.setClip(clip);
    }

    private void setupDrag() {
        titleBar.setOnMousePressed((MouseEvent e) -> {
            xOffset = e.getSceneX();
            yOffset = e.getSceneY();
        });
        titleBar.setOnMouseDragged((MouseEvent e) -> {
            Stage stage = (Stage) rootPane.getScene().getWindow();
            stage.setX(e.getScreenX() - xOffset);
            stage.setY(e.getScreenY() - yOffset);
        });
    }

    private void setupVcampusClick() {
        vcampusButton.setOnAction(e -> {
            if (appSidebar.isVisible()) {
                hideAppSidebar();
            } else {
                showAppSidebar();
            }
        });
    }

    private void showAppSidebar() {
        appSidebar.setVisible(true);
        FadeTransition fadeIn = new FadeTransition(Duration.millis(200), appSidebar);
        fadeIn.setFromValue(0);
        fadeIn.setToValue(1);
        fadeIn.play();
    }

    private void hideAppSidebar() {
        FadeTransition fadeOut = new FadeTransition(Duration.millis(200), appSidebar);
        fadeOut.setFromValue(1);
        fadeOut.setToValue(0);
        fadeOut.setOnFinished(e -> appSidebar.setVisible(false));
        fadeOut.play();
    }

    private void updateSidebarSelection(String selectedFunc) {
        if (currentSelectedSidebarButton != null) {
            currentSelectedSidebarButton.getStyleClass().remove("selected");
        }
        for (Node node : sidebarContainer.getChildren()) {
            if (node instanceof Button) {
                Button btn = (Button) node;
                if (btn.getText().equals(selectedFunc)) {
                    btn.getStyleClass().add("selected");
                    currentSelectedSidebarButton = btn;
                    break;
                }
            }
        }
    }

    @FXML
    private void handleClose() {
        Stage stage = (Stage) rootPane.getScene().getWindow();
        stage.close();
    }

    @FXML
    private void handleMinimize() {
        Stage stage = (Stage) rootPane.getScene().getWindow();
        stage.setIconified(true);
    }

    private void showErrorAlert(String header, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("错误");
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.showAndWait();
    }

    // 设置管理员姓名的方法，可以从外部调用
    public void setAdminName(String name) {
        if (welcomeLabel != null) {
            welcomeLabel.setText("欢迎 " + name + " 管理员！");
        }
    }

	public void setAdminId(String userid) {
		this.adminid = userid;
		Platform.runLater(() -> {
            loadDefaultSubFunc();
        });
	}
	
	public void setAdminType(int usertype) {
		this.admintype = usertype;
		Platform.runLater(() -> {
			initAppSidebarButtons();
            loadDefaultSubFunc();
        });
	}
}

