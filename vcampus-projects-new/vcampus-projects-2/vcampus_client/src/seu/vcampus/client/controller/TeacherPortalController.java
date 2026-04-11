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

public class TeacherPortalController implements Initializable {

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
    private String teacherId;  // 新增教师ID属性

    private Map<String, Parent> pageMap = new HashMap<>();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        initAppSubFuncMap();
        initAppSidebarButtons();
        //loadDefaultSubFunc();移除这个就好使了
        setupClip();
        setupDrag();
        setupVcampusClick();
    }

    private void initAppSubFuncMap() {
        appSubFuncMap = new HashMap<>();
        appSubFuncMap.put("个人信息", Arrays.asList("基本资料",  "修改密码","登出"));
        appSubFuncMap.put("学籍管理", Arrays.asList("成绩查询"));
        appSubFuncMap.put("教务系统", Arrays.asList("课表", "课程申请"));
        appSubFuncMap.put("图书馆", Arrays.asList("馆藏查询", "图书借阅", "续借管理", "研讨间预约"));
        appSubFuncMap.put("商店", Arrays.asList("商品浏览",  "购物车","订单管理", "消费记录"));
    }

    private void initAppSidebarButtons() {
        Map<String, String> appImageMap = new HashMap<>();
        appImageMap.put("个人信息", "/seu/vcampus/client/images/profileicon.png");
        appImageMap.put("教务系统", "/seu/vcampus/client/images/courseicon.png");
        appImageMap.put("图书馆", "/seu/vcampus/client/images/libraryicon.png");
        appImageMap.put("商店", "/seu/vcampus/client/images/storeicon.png");
        appImageMap.put("学籍管理", "/seu/vcampus/client/images/xuejiicon.png");

        for (String app : appImageMap.keySet()) {
            Image iconImage = new Image(getClass().getResourceAsStream(appImageMap.get(app)));
            ImageView icon = new ImageView(iconImage);

            icon.setFitWidth(16);
            icon.setFitHeight(16);
            icon.setPreserveRatio(true);

            HBox buttonContent = new HBox();
            buttonContent.setAlignment(Pos.CENTER_LEFT);
            buttonContent.getChildren().addAll(icon, new Label(app));
            HBox.setMargin(icon, new Insets(0, 10, 0, 0));

            Button appBtn = new Button();
            appBtn.setGraphic(buttonContent);

            appBtn.setOnAction(e -> {
                loadSubFuncByApp(app);
                hideAppSidebar();
            });

            appSidebar.getChildren().add(appBtn);
        }
    }

    private void loadDefaultSubFunc() {
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

    private void handleSidebarClick(String funcName) {
        updateSidebarSelection(funcName);
        String fxmlPath = getFXMLPath(funcName);
        loadFXMLToContent(funcName , fxmlPath);
    }

    private String getFXMLPath(String funcName) {
        String basePath = "/seu/vcampus/client/view/";
        switch (funcName) {
        	case "成绩查询":
        		return basePath + "teacher_grades.fxml";
            case "基本资料":
                return basePath + "teacher_info.fxml";
            case "修改密码":
                return basePath + "change_password.fxml";
            case "课表":
                return basePath + "course_desk.fxml";
            case "课程申请":
                return basePath + "course_application.fxml";
            case "馆藏查询":
                return basePath + "library_check.fxml";
            case "图书借阅":
                return basePath + "library_borrow.fxml";
            case "续借管理":
                return basePath + "borrow_manage.fxml";
            case "研讨间预约":
                return basePath + "room_manage.fxml";
            case "商品浏览":
                return basePath + "product_browse.fxml";
            case "订单管理":
                return basePath + "order_history.fxml";
            case "购物车":
                return basePath + "cart.fxml";
            case "消费记录":
                return basePath + "transaction_history.fxml";
            case "登出":
                return basePath + "logout.fxml";
            default:
                return basePath + "teacher_info.fxml";
        }
    }

    private void loadFXMLToContent(String funcName, String fxmlPath) {
        try {
            contentArea.getChildren().clear();
            // 检查是否需要刷新页面
            boolean needRefresh = fxmlPath.contains("borrow_manage.fxml") ||
                                 fxmlPath.contains("library_borrow.fxml"); // 添加其他需要刷新的页面

            Parent content = pageMap.get(funcName);
            FXMLLoader loader = null;

            if (content == null || needRefresh) {
                loader = new FXMLLoader(getClass().getResource(fxmlPath));
                content = loader.load();
                pageMap.put(funcName, content);

             // 传递教师ID给对应控制器
                if (fxmlPath.contains("teacher_info.fxml")) {
                    TeacherInfoController infoController = loader.getController();
                    infoController.setTeacherId(teacherId);
                } else if (fxmlPath.contains("change_password.fxml")) {
                    ChangePasswordController passwordController = loader.getController();
                    passwordController.setUserId(teacherId);
                }else if (fxmlPath.contains("library_borrow.fxml")) {
                    LibraryBorrowController libraryborrowcontroller = loader.getController();
                    libraryborrowcontroller.setUserId(teacherId);
                } else if (fxmlPath.contains("borrow_manage.fxml")) {
                    BorrowManageController borrowmanagecontroller = loader.getController();
                    borrowmanagecontroller.setUserId(teacherId);
                }else if (fxmlPath.contains("room_manage.fxml")) {
                    RoomManageController roommanageController = loader.getController();
                    roommanageController.setUserId(teacherId);
                }else if (fxmlPath.contains("cart.fxml")) {
                    CartController cartController = loader.getController();
                    cartController.setUserId(teacherId);
                }else if (fxmlPath.contains("product_browse.fxml")) {
                    ProductBrowseController productbrowseController = loader.getController();
                    productbrowseController.setUserId(teacherId);
                }else if (fxmlPath.contains("order_history.fxml")) {
                    OrderHistoryController orderController = loader.getController();
                    orderController.setUserId(teacherId);
                }else if (fxmlPath.contains("transaction_history.fxml")) { // 消费记录界面
                    TransactionHistoryController controller = loader.getController();
                    controller.setUserId(teacherId); // 传递当前用户ID
                }else if(fxmlPath.contains("course_application.fxml")) {
                	CourseApplicationController courseApplicationController = loader.getController();
                	courseApplicationController.setUserId(teacherId);
                }else if(fxmlPath.contains("course_desk.fxml")) {
                	CourseDeskController coursedesk = loader.getController();
                	coursedesk.setUserId(teacherId);
                }
            }

            FadeTransition fadeIn = new FadeTransition(Duration.millis(300), content);
            fadeIn.setFromValue(0);
            fadeIn.setToValue(1);
            contentArea.getChildren().add(content);
            fadeIn.play();
        } catch (IOException e) {
            System.err.println("加载页面失败！检查路径：" + fxmlPath);
            e.printStackTrace();
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
    	SocketManager.getInstance().close();
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

    public void setTeacherName(String name) {
        if (welcomeLabel != null) {
            welcomeLabel.setText("欢迎 " + name + " 老师！");
        }
    }

    // 新增设置教师ID的方法，用于从登录页面传递教师工号
    public void setTeacherId(String teacherId) {
        this.teacherId = teacherId;
    	Platform.runLater(() -> {
            loadDefaultSubFunc();
        });
    }
}