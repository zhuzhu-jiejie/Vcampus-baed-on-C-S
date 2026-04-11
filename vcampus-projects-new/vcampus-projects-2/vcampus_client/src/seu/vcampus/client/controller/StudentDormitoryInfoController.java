package seu.vcampus.client.controller;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.Gson;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.util.Callback;
import seu.vcampus.client.network.SocketManager;
import seu.vcampus.model.DormitoryInfoRequest;
import seu.vcampus.model.DormitoryInfoResponse;
import seu.vcampus.model.HygieneRecord;
import seu.vcampus.model.StudentInfo;

public class StudentDormitoryInfoController {

    // 学生本人信息标签
    @FXML private Label studentId;
    @FXML private Label studentName;
    @FXML private Label studentCollege;
    @FXML private Label studentMajor;
    @FXML private Label studentBuilding;
    @FXML private Label studentRoom;
    @FXML private Label studentBed;
    @FXML private Label studentPhone;

    // 舍友信息表格
    @FXML private TableView<Map<String, String>> roommateTable;

    // 舍友信息表格列
    @FXML private TableColumn<Map<String, String>, String> roommateIdColumn;
    @FXML private TableColumn<Map<String, String>, String> roommateNameColumn;
    @FXML private TableColumn<Map<String, String>, String> roommateCollegeColumn;
    @FXML private TableColumn<Map<String, String>, String> roommateMajorColumn;
    @FXML private TableColumn<Map<String, String>, String> roommateBedColumn;
    @FXML private TableColumn<Map<String, String>, String> roommatePhoneColumn;

    // 宿舍卫生评分
    @FXML private Label currentScore;
    @FXML private LineChart<String, Number> scoreChart;
    @FXML private TableView<Map<String, String>> scoreHistoryTable;

    // 刷新按钮
    @FXML private Button refreshButton;

    private ObservableList<Map<String, String>> roommateData = FXCollections.observableArrayList();
    private ObservableList<Map<String, String>> scoreHistoryData = FXCollections.observableArrayList();
    private Gson gson = new Gson();
    private String studentIdValue;

    @FXML
    private void initialize() {
        // 初始化表格列
        setupTableColumns();

        // 初始化时不加载数据，等待setStudentId调用
        System.out.println("宿舍信息控制器初始化完成");
    }

    // 修改 setupTableColumns 方法，使用自定义的Callback实现
    private void setupTableColumns() {
        // 舍友表格列映射 - 使用英文键名与数据填充保持一致
        roommateIdColumn.setCellValueFactory(new Callback<TableColumn.CellDataFeatures<Map<String, String>, String>, ObservableValue<String>>() {
            @Override
            public ObservableValue<String> call(TableColumn.CellDataFeatures<Map<String, String>, String> param) {
                return new SimpleStringProperty(param.getValue().get("userid"));
            }
        });

        roommateNameColumn.setCellValueFactory(new Callback<TableColumn.CellDataFeatures<Map<String, String>, String>, ObservableValue<String>>() {
            @Override
            public ObservableValue<String> call(TableColumn.CellDataFeatures<Map<String, String>, String> param) {
                return new SimpleStringProperty(param.getValue().get("name"));
            }
        });

        roommateCollegeColumn.setCellValueFactory(new Callback<TableColumn.CellDataFeatures<Map<String, String>, String>, ObservableValue<String>>() {
            @Override
            public ObservableValue<String> call(TableColumn.CellDataFeatures<Map<String, String>, String> param) {
                return new SimpleStringProperty(param.getValue().get("college"));
            }
        });

        roommateMajorColumn.setCellValueFactory(new Callback<TableColumn.CellDataFeatures<Map<String, String>, String>, ObservableValue<String>>() {
            @Override
            public ObservableValue<String> call(TableColumn.CellDataFeatures<Map<String, String>, String> param) {
                return new SimpleStringProperty(param.getValue().get("major"));
            }
        });

        roommateBedColumn.setCellValueFactory(new Callback<TableColumn.CellDataFeatures<Map<String, String>, String>, ObservableValue<String>>() {
            @Override
            public ObservableValue<String> call(TableColumn.CellDataFeatures<Map<String, String>, String> param) {
                return new SimpleStringProperty(param.getValue().get("bed"));
            }
        });

        roommatePhoneColumn.setCellValueFactory(new Callback<TableColumn.CellDataFeatures<Map<String, String>, String>, ObservableValue<String>>() {
            @Override
            public ObservableValue<String> call(TableColumn.CellDataFeatures<Map<String, String>, String> param) {
                return new SimpleStringProperty(param.getValue().get("phone"));
            }
        });

        // 历史评分表格列映射
        if (scoreHistoryTable.getColumns().size() >= 4) {
            TableColumn<Map<String, String>, String> dateColumn = (TableColumn<Map<String, String>, String>) scoreHistoryTable.getColumns().get(0);
            TableColumn<Map<String, String>, String> scoreColumn = (TableColumn<Map<String, String>, String>) scoreHistoryTable.getColumns().get(1);
            TableColumn<Map<String, String>, String> inspectorColumn = (TableColumn<Map<String, String>, String>) scoreHistoryTable.getColumns().get(2);
            TableColumn<Map<String, String>, String> remarkColumn = (TableColumn<Map<String, String>, String>) scoreHistoryTable.getColumns().get(3);

            dateColumn.setCellValueFactory(new Callback<TableColumn.CellDataFeatures<Map<String, String>, String>, ObservableValue<String>>() {
                @Override
                public ObservableValue<String> call(TableColumn.CellDataFeatures<Map<String, String>, String> param) {
                    return new SimpleStringProperty(param.getValue().get("date"));
                }
            });

            scoreColumn.setCellValueFactory(new Callback<TableColumn.CellDataFeatures<Map<String, String>, String>, ObservableValue<String>>() {
                @Override
                public ObservableValue<String> call(TableColumn.CellDataFeatures<Map<String, String>, String> param) {
                    return new SimpleStringProperty(param.getValue().get("score"));
                }
            });

            inspectorColumn.setCellValueFactory(new Callback<TableColumn.CellDataFeatures<Map<String, String>, String>, ObservableValue<String>>() {
                @Override
                public ObservableValue<String> call(TableColumn.CellDataFeatures<Map<String, String>, String> param) {
                    return new SimpleStringProperty(param.getValue().get("inspector"));
                }
            });

            remarkColumn.setCellValueFactory(new Callback<TableColumn.CellDataFeatures<Map<String, String>, String>, ObservableValue<String>>() {
                @Override
                public ObservableValue<String> call(TableColumn.CellDataFeatures<Map<String, String>, String> param) {
                    return new SimpleStringProperty(param.getValue().get("remark"));
                }
            });
        }
    }

    // 创建舍友信息Map
    private Map<String, String> createRoommate(String id, String name, String college,
                                              String major, String bed, String phone) {
        Map<String, String> roommate = new HashMap<>();
        roommate.put("userid", id);
        roommate.put("name", name);
        roommate.put("college", college);
        roommate.put("major", major);
        roommate.put("bed", bed);
        roommate.put("phone", phone);
        return roommate;
    }

    public void setStudentId(String studentId) {
        this.studentIdValue = studentId;
        loadDormitoryInfo();
    }

    private void loadDormitoryInfo() {
        new Thread(() -> {
            try {
                DataOutputStream out = SocketManager.getInstance().getOut();
                DataInputStream in = SocketManager.getInstance().getIn();

                // 发送请求类型
                out.writeUTF("DormitoryInfoRequest");

                // 发送获取宿舍信息的请求
                DormitoryInfoRequest req = new DormitoryInfoRequest();
                req.setUserid(studentIdValue);
                out.writeUTF(gson.toJson(req));
                out.flush();

                // 接收服务器响应
                String response = in.readUTF();
                System.out.println("收到服务器响应: " + response);

                if (response.startsWith("ERROR|")) {
                    // 处理错误信息
                    String errorMsg = response.substring(6);
                    javafx.application.Platform.runLater(() ->
                        showErrorAlert("获取失败", errorMsg));
                } else if (response.startsWith("SUCCESS|")) {
                    // 处理成功响应
                    String jsonStr = response.substring(8);
                    DormitoryInfoResponse dormInfo = gson.fromJson(jsonStr, DormitoryInfoResponse.class);

                    // 在UI线程中更新界面
                    javafx.application.Platform.runLater(() -> {
                        StudentInfo studentInfo = dormInfo.getStudentInfo();
                        List<StudentInfo> roommates = dormInfo.getRoommates();
                        List<HygieneRecord> hygieneRecords = dormInfo.getHygieneRecords();

                        // 设置学生本人信息
                        if (studentInfo != null) {
                            studentId.setText(studentInfo.getUserid());
                            studentName.setText(studentInfo.getName());
                            studentCollege.setText(studentInfo.getCollege());
                            studentMajor.setText(studentInfo.getMajor());
                            studentBuilding.setText(studentInfo.getDormitoryBuilding());
                            studentRoom.setText(studentInfo.getDormitoryRoom());
                            studentBed.setText(studentInfo.getDormitoryBed());
                            studentPhone.setText(studentInfo.getPhone());
                        }

                        // 设置舍友信息
                        if (roommates != null) {
                            loadRoommateInfo(roommates);
                        }

                        // 加载宿舍卫生评分
                        if (hygieneRecords != null) {
                            loadDormitoryScores(hygieneRecords);
                        } else {
                            loadDormitoryScores(new ArrayList<>()); // 空列表
                        }
                    });
                } else {
                    javafx.application.Platform.runLater(() ->
                        showErrorAlert("响应格式错误", "服务器返回了未知格式的响应: " + response));
                }
            } catch (Exception e) {
                javafx.application.Platform.runLater(() ->
                    showErrorAlert("连接错误", "获取宿舍信息失败：" + e.getMessage()));
                e.printStackTrace();
            }
        }).start();
    }

    private void loadRoommateInfo(List<StudentInfo> roommates) {
        // 清空现有数据
        roommateData.clear();

        System.out.println("开始加载舍友信息，共 " + roommates.size() +  " 个舍友");

        // 添加舍友信息
        for (StudentInfo roommate : roommates) {
            // 跳过当前登录学生本人
            if (roommate.getUserid().equals(studentIdValue)) {
                continue;
            }

            Map<String, String> roommateMap = createRoommate(
                roommate.getUserid(),
                roommate.getName(),
                roommate.getCollege(),
                roommate.getMajor(),
                roommate.getDormitoryBed(),
                roommate.getPhone()
            );
            roommateData.add(roommateMap);
            System.out.println("添加舍友: " + roommateMap);
        }

        // 设置表格数据
        roommateTable.setItems(roommateData);
        System.out.println("已加载 " + roommates.size() + " 个舍友信息到表格");
    }

    private void loadDormitoryScores(List<HygieneRecord> hygieneRecords) {
        // 清空图表数据
        scoreChart.getData().clear();

        // 设置当前评分（如果有记录，取最新的评分）
        if (!hygieneRecords.isEmpty()) {
            currentScore.setText(String.valueOf(hygieneRecords.get(0).getScore()));
        } else {
            currentScore.setText("暂无数据");
        }

        // 设置历史评分图表
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("宿舍卫生评分");

        // 添加数据到图表
        for (HygieneRecord record : hygieneRecords) {
            series.getData().add(new XYChart.Data<>(record.getDate(), record.getScore()));
        }

        scoreChart.getData().add(series);

        // 设置历史评分表格
        scoreHistoryData.clear();

        for (HygieneRecord record : hygieneRecords) {
            scoreHistoryData.add(createScoreRecord(
                record.getDate(),
                String.valueOf(record.getScore()),
                record.getInspector(),
                record.getRemark()
            ));
        }

        scoreHistoryTable.setItems(scoreHistoryData);
    }

    private Map<String, String> createScoreRecord(String date, String score, String inspector, String remark) {
        Map<String, String> record = new HashMap<>();
        record.put("date", date);
        record.put("score", score);
        record.put("inspector", inspector);
        record.put("remark", remark);
        return record;
    }

    @FXML
    private void handleRefresh() {
        // 刷新按钮处理
        loadDormitoryInfo();
        System.out.println("宿舍数据已刷新");

        // 显示成功提示
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("操作成功");
        alert.setHeaderText(null);
        alert.setContentText("宿舍数据已成功刷新！");
        alert.showAndWait();
    }

    private void showErrorAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("错误");
        alert.setHeaderText(title);
        alert.setContentText(message);
        alert.showAndWait();
    }
}