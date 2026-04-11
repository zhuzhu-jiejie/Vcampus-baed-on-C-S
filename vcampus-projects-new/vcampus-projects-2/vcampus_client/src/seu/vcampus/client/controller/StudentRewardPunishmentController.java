package seu.vcampus.client.controller;

import java.net.URL;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.util.ResourceBundle;
import java.util.List;
import java.lang.reflect.Type;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.fxml.Initializable;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.beans.property.StringProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.concurrent.Task;
import javafx.application.Platform;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import seu.vcampus.client.session.UserSession;
import seu.vcampus.client.network.SocketManager;
import seu.vcampus.model.RewardPunishment;


public class StudentRewardPunishmentController implements Initializable {

    // -------------------------- UI 组件（原有，保持不变） --------------------------
    @FXML private TableView<RewardPunishmentListEntry> rewardPunishmentList;
    @FXML private TableColumn<RewardPunishmentListEntry, String> typeCol;
    @FXML private TableColumn<RewardPunishmentListEntry, String> titleCol;
    @FXML private TableColumn<RewardPunishmentListEntry, String> reasonCol;
    @FXML private TableColumn<RewardPunishmentListEntry, String> awardingOrganizationCol;
    @FXML private TableColumn<RewardPunishmentListEntry, String> effectiveDateCol;
    @FXML private TableColumn<RewardPunishmentListEntry, String> statusCol;
    
    @FXML private Label loadingLabel;  // 加载状态提示
    @FXML private Label errorLabel;    // 错误/成功提示

    // -------------------------- 新增：核心数据与通信变量（对齐 Teacher 控制器） --------------------------
    private DataInputStream dataIn;       // Socket 输入流（从 SocketManager 获取）
    private DataOutputStream dataOut;     // Socket 输出流（从 SocketManager 获取）
    private String currentStudentId;      // 当前登录学生 ID（从 UserSession 获取）
    private static final Gson gson = new Gson();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        initNetworkConnections();
        initListColumns();
        loadCurrentStudentId();
        loadRewardPunishmentDataFromSocket();
    }

    // -------------------------- （对齐 Teacher 控制器） --------------------------
    /**
     * 初始化 UI 状态：隐藏加载/错误提示、获取 Socket 流、处理流异常
     */
    private void initNetworkConnections() {
        try {
            dataIn = SocketManager.getInstance().getIn();
            dataOut = SocketManager.getInstance().getOut();
        } catch (Exception e) {
            showError("获取服务器连接失败：" + e.getMessage());
        }
    }

    /**
     * 初始化表格列：绑定实体类的 getter 方法
     */
    private void initListColumns() {
        typeCol.setCellValueFactory(new PropertyValueFactory<>("type"));
        titleCol.setCellValueFactory(new PropertyValueFactory<>("title"));
        reasonCol.setCellValueFactory(new PropertyValueFactory<>("reason"));
        awardingOrganizationCol.setCellValueFactory(new PropertyValueFactory<>("awardingOrganization"));
        effectiveDateCol.setCellValueFactory(new PropertyValueFactory<>("effectiveDate"));
        statusCol.setCellValueFactory(new PropertyValueFactory<>("status"));

        rewardPunishmentList.setItems(FXCollections.observableArrayList());
    }

    /**
     * 从 UserSession 获取当前登录学生 ID（学生身份验证的核心）
     */
    private void loadCurrentStudentId() {
        currentStudentId = UserSession.getInstance().getCurrentUserId();
        if (currentStudentId == null || currentStudentId.trim().isEmpty()) {
            showError("学生身份未验证，请重新登录");
        }
    }

    // -------------------------- 从 Socket 加载奖惩数据 --------------------------
    /**
     * 异步加载当前学生的奖惩记录（避免阻塞 UI，对齐 Teacher 控制器的 Task 设计）
     */
    private void loadRewardPunishmentDataFromSocket() {
        if (currentStudentId == null || dataIn == null || dataOut == null) {
            return;
        }

        showLoading(true);
        
        Task<List<RewardPunishment>> loadTask = new Task<>() {
            @Override
            protected List<RewardPunishment> call() throws Exception {
                dataOut.writeUTF("GetStudentRewardPunishmentById");
                dataOut.flush();
                dataOut.writeUTF(currentStudentId);
                dataOut.flush();
                
                String json = dataIn.readUTF();
                
                if (json.contains("\"error\"")) {
                	throw new Exception("服务端查询失败：" + json);
                }
                
                Type rewardPunishmentListType = new TypeToken<List<RewardPunishment>>() {}.getType();
                List<RewardPunishment> result = gson.fromJson(json, rewardPunishmentListType);

                if (result == null) {
                	throw new Exception("未获取到学生奖惩信息");
                }
                
                return result;
            }

            /**
             * 任务成功执行后（获取数据成功）：更新表格数据，隐藏加载状态
             */
            @Override
            protected void succeeded() {
            	super.succeeded();
                showLoading(false);
                
                List<RewardPunishment> result = getValue();
                ObservableList<RewardPunishmentListEntry> tableEntries= FXCollections.observableArrayList();
                
                for (RewardPunishment rp : result) {
                	tableEntries.add(new RewardPunishmentListEntry(
                		rp.getId(),
                		rp.getType(),
                		rp.getTitle(),
                		rp.getReason(),
                		rp.getAwardingOrganization(),
                		rp.getEffectiveDate(),
                		rp.getStatus()
                	));
                }
                
                rewardPunishmentList.setItems(tableEntries);
                
                if (result.isEmpty()) {
                    showError("暂无奖惩记录", false);
                }
            }

            /**
             * 任务执行失败（如 Socket 异常、Server 错误）：显示错误信息
             */
            @Override
            protected void failed() {
                super.failed();
                showLoading(false);
                Throwable error = getException();
                String errorMsg = error.getMessage() != null ? error.getMessage() : "加载奖惩记录失败";
                showError("错误：" + errorMsg);
                
                rewardPunishmentList.setItems(FXCollections.observableArrayList());
            }
        };

        new Thread(loadTask).start();
    }

    // -------------------------- UI 辅助方法 --------------------------
    /**
     * 显示/隐藏加载提示（确保在 FX 线程更新 UI）
     */
    private void showLoading(boolean show) {
        Platform.runLater(() -> loadingLabel.setVisible(show));
    }

    /**
     * 显示错误提示（默认红色文字，3秒后自动隐藏）
     */
    private void showError(String msg) {
        showError(msg, true);
    }

    /**
     * 显示提示（支持错误/成功两种样式）
     * @param msg 提示内容
     * @param isError true=错误（红色），false=成功（绿色）
     */
    private void showError(String msg, boolean isError) {
        Platform.runLater(() -> {
            errorLabel.setText(msg);
            errorLabel.setStyle(isError ? 
                "-fx-text-fill: #d32f2f; -fx-font-size: 12px;" : 
                "-fx-text-fill: #388e3c; -fx-font-size: 12px;"
            );
            errorLabel.setVisible(true);

            new Thread(() -> {
                try {
                    Thread.sleep(3000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                Platform.runLater(() -> errorLabel.setVisible(false));
            }).start();
        });
    }

    // -------------------------- 奖惩记录实体类 --------------------------
    /**
     * 奖惩记录实体类（使用 StringProperty 支持 UI 数据绑定，适合表格动态更新）
     */
    public static class RewardPunishmentListEntry {
        private final StringProperty id;
        private final StringProperty type;
        private final StringProperty title;
        private final StringProperty reason;
        private final StringProperty awardingOrganization;
        private final StringProperty effectiveDate;
        private final StringProperty status;

        public RewardPunishmentListEntry(String id, String type, String title, String reason, 
                                        String awardingOrganization, String effectiveDate, 
                                        String status) {
        	this.id = new SimpleStringProperty(id);
            this.type = new SimpleStringProperty(type);
            this.title = new SimpleStringProperty(title);
            this.reason = new SimpleStringProperty(reason);
            this.awardingOrganization = new SimpleStringProperty(awardingOrganization);
            this.effectiveDate = new SimpleStringProperty(effectiveDate);
            this.status = new SimpleStringProperty(status);
        }

        // Getter 方法
        public String getId() { return id.get(); }
        public String getType() { return type.get(); }
        public String getTitle() { return title.get(); }
        public String getReason() { return reason.get(); }
        public String getAwardingOrganization() { return awardingOrganization.get(); }
        public String getEffectiveDate() { return effectiveDate.get(); }
        public String getStatus() { return status.get(); }
        
        // Setter 方法
        public void setId(String id) { this.id.setValue(id); }
        public void setType(String type) { this.type.set(type); }
        public void setTitle(String title) { this.title.set(title); }
        public void setReason(String reason) { this.reason.set(reason); }
        public void setAwardingOrganization(String awardingOrganization) { this.awardingOrganization.set(awardingOrganization); }
        public void setEffectiveDate(String effectiveDate) { this.effectiveDate.set(effectiveDate); }
        public void setStatus(String status) { this.status.set(status); }
        
        // Property 访问器
        public StringProperty idProperty() { return id; }
        public StringProperty typeProperty() { return type; }
        public StringProperty titleProperty() { return title; }
        public StringProperty reasonProperty() { return reason; }
        public StringProperty awardingOrganizationProperty() { return awardingOrganization; }
        public StringProperty effectiveDateProperty() { return effectiveDate; }
        public StringProperty statusProperty() { return status; }
    }

}