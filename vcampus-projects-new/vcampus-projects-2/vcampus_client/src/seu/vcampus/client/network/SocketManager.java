package seu.vcampus.client.network;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

/**
 * 连接管理工具类（单例模式）
 * 用于保存登录后的Socket连接及输入输出流，供全局复用
 */
public class SocketManager {
    // 单例实例（全局唯一）
    private static SocketManager instance;

    // 保存Socket连接及流对象
    private Socket socket;
    private DataInputStream in;
    private DataOutputStream out;

    // 服务器配置
    private String host = "127.0.0.1";
    private int port = 8083;

    // 私有构造器：禁止外部直接创建实例
    private SocketManager() {}

    // 全局获取单例的方法（线程安全）
    public static synchronized SocketManager getInstance() {
        if (instance == null) {
            instance = new SocketManager();
        }
        return instance;
    }

    /**
     * 初始化Socket连接（替代原来的init方法）
     */
    public void initialize() {
        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }

            // 创建新的Socket连接
            socket = new Socket(host, port);
            this.in = new DataInputStream(socket.getInputStream());
            this.out = new DataOutputStream(socket.getOutputStream());

            System.out.println("Socket连接已建立: " + host + ":" + port);
        } catch (IOException e) {
            System.err.println("Socket连接初始化失败: " + e.getMessage());
            e.printStackTrace();

            // 尝试重新连接
            try {
                Thread.sleep(5000); // 等待5秒后重试
                System.out.println("尝试重新连接...");
                initialize();
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            }
        }
    }

    /**
     * 初始化Socket连接（保持与原有代码兼容）
     * @param socket 已建立的Socket连接
     */
    public void init(Socket socket) throws Exception {
        this.socket = socket;
        this.in = new DataInputStream(socket.getInputStream());
        this.out = new DataOutputStream(socket.getOutputStream());
    }

    // 获取输入流（供读取服务器响应）
    public DataInputStream getIn() {
        // 检查连接是否有效，如果无效则尝试重新连接
        if (!isConnected()) {
            System.out.println("连接已断开，尝试重新连接...");
            initialize();
        }
        return in;
    }

    // 获取输出流（供发送请求到服务器）
    public DataOutputStream getOut() {
        // 检查连接是否有效，如果无效则尝试重新连接
        if (!isConnected()) {
            System.out.println("连接已断开，尝试重新连接...");
            initialize();
        }
        return out;
    }

    // 添加一个方法来检查连接状态
    public boolean isConnected() {
        return socket != null && socket.isConnected() && !socket.isClosed();
    }

    // 添加一个方法来重新连接
    public void reconnect() throws IOException {
        close();
        initialize();
    }

    /**
     * 设置服务器地址
     */
    public void setHost(String host) {
        this.host = host;
    }

    /**
     * 设置服务器端口
     */
    public void setPort(int port) {
        this.port = port;
    }

    /**
     * 获取服务器地址
     */
    public String getHost() {
        return host;
    }

    /**
     * 获取服务器端口
     */
    public int getPort() {
        return port;
    }

    /**
     * 关闭连接及流（程序退出时调用）
     */
    public void close() {
        try {
            if (in != null) {
				in.close();
			}
            if (out != null) {
				out.close();
			}
            if (socket != null && !socket.isClosed()) {
				socket.close();
			}
            System.out.println("Socket连接已关闭");
        } catch (Exception e) {
            System.err.println("关闭Socket连接时发生错误: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 检查连接是否有效
     */
    // 已通过isConnected()方法实现
}