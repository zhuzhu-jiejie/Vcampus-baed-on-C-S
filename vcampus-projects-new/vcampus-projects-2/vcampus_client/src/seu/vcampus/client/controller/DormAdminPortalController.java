package seu.vcampus.client.controller;

import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

import javafx.animation.FadeTransition;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
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

public class DormAdminPortalController implements Initializable {
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

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        initAppSubFuncMap();
        initAppSidebarButtons(); // 仅修改此方法
        loadDefaultSubFunc();
        setupClip();
        setupDrag();
        setupVcampusClick();
    }

    // 其他方法保持不变...
    private void initAppSubFuncMap() {
        // 原有代码不变
        appSubFuncMap = new HashMap<>();
        appSubFuncMap.put("个人信息", Arrays.asList("基本资料", "联系方式", "修改密码", "个人头像"));
        appSubFuncMap.put("宿舍管理", Arrays.asList("宿舍信息", "水电费催缴", "退换舍申请处理", "报修申请处理"));
    }

    // 修改此方法：使用自定义图片作为图标
    private void initAppSidebarButtons() {
        // 应用名称与对应图片路径的映射（请替换为你的图片路径）
        Map<String, String> appImageMap = new HashMap<>();
        appImageMap.put("个人信息", "/seu/vcampus/client/images/profileicon.png");      // 个人信息图标
        appImageMap.put("宿舍管理", "/seu/vcampus/client/images/dormitoryicon.png");        // 宿舍管理图标


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
        // 原有代码不变
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
    private void handleSidebarClick(String funcName) {
        updateSidebarSelection(funcName);
        String fxmlPath = getFXMLPath(funcName);
        loadFXMLToContent(fxmlPath);
    }

    private String getFXMLPath(String funcName) {
        String basePath = "/seu/vcampus/client/view/";
        switch (funcName) {
            case "宿舍信息": return basePath + "admin_dormitoryinfo.fxml";
            case "水电费催缴": return basePath + "admin_reminder.fxml";
            case "退换舍申请处理": return basePath + "admin_exchange.fxml";
            case "报修申请处理": return basePath + "admin_repair.fxml";
            default: return basePath + "admin_dormitoryinfo.fxml";
        }
    }

    private void loadFXMLToContent(String fxmlPath) {
        try {
            contentArea.getChildren().clear();
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            BorderPane content = loader.load();
            content.prefWidthProperty().bind(contentArea.widthProperty());
            content.prefHeightProperty().bind(contentArea.heightProperty());
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

    // 设置学生姓名的方法，可以从外部调用
    public void setStudentName(String name) {
        if (welcomeLabel != null) {
            welcomeLabel.setText("欢迎 " + name + " 同学！");
        }
    }
}


