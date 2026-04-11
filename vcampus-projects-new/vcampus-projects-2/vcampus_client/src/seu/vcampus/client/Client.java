package seu.vcampus.client;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

public class Client extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        Parent root = FXMLLoader.load(getClass().getResource("/seu/vcampus/client/view/login.fxml"));

        // 初始场景尺寸：与FXML根容器的minWidth/minHeight一致（800×600）
        Scene scene = new Scene(root, 800, 600);
        scene.getStylesheets().add(getClass().getResource("/seu/vcampus/client/css/login.css").toExternalForm());

        // 关键：删除 setResizable(false)，允许窗口缩放（背景和卡片同步适配）
        // primaryStage.setResizable(false); // 注释或删除这行
        primaryStage.initStyle(StageStyle.TRANSPARENT);
        primaryStage.setMinWidth(800.0);  // 与FXML根容器minWidth一致
        primaryStage.setMinHeight(600.0); // 与FXML根容器minHeight一致

        primaryStage.setTitle("身份认证系统");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}