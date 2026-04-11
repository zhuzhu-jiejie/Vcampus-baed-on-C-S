package seu.vcampus.client.session;

/**
 * 全局用户会话管理类（单例模式）
 * 存储当前登录用户的核心信息，提供全局访问、设置、清空方法
 */
public class UserSession {
    // 1. 单例实例（静态私有，确保唯一）
    private static volatile UserSession instance;

    // 2. 存储当前登录用户的核心信息（根据系统需求扩展）
    private String userId;         // 用户ID（教师ID/学生ID/管理员ID，对应数据库中的teacherid/studentid）
    private String userName;       // 用户名（显示用，如“李教授”“张三”）
    private int userType;          // 用户类型（1-学生，2-教师，3-管理员，与Server端usertype对齐）
    private boolean isLoggedIn;    // 登录状态（true=已登录，false=未登录）

    // 3. 私有构造方法（禁止外部new实例，确保单例）
    private UserSession() {
        // 初始化：默认未登录，信息为空
        this.userId = null;
        this.userName = null;
        this.userType = 0;
        this.isLoggedIn = false;
    }

    // 4. 静态方法：获取单例实例（线程安全）
    public static UserSession getInstance() {
        if (instance == null) {
            // 双重检查锁，确保多线程环境下只创建一个实例
            synchronized (UserSession.class) {
                if (instance == null) {
                    instance = new UserSession();
                }
            }
        }
        return instance;
    }

    // 5. 登录时调用：设置用户信息（登录成功后调用）
    public void setUserInfo(String userId, String userName, int userType) {
        this.userId = userId;
        this.userName = userName;
        this.userType = userType;
        this.isLoggedIn = true; // 标记为已登录
    }

    // 6. 登出时调用：清空用户信息（退出登录时调用）
    public void clearUserInfo() {
        this.userId = null;
        this.userName = null;
        this.userType = 0;
        this.isLoggedIn = false; // 标记为未登录
    }

    // 7. Getter方法（只提供查询，不提供setter，避免外部随意修改）
    public String getCurrentUserId() {
        return this.userId;
    }

    public String getCurrentUserName() {
        return this.userName;
    }

    public int getCurrentUserType() {
        return this.userType;
    }

    public boolean isLoggedIn() {
        return this.isLoggedIn;
    }

    // 8. 辅助方法：判断是否为教师（简化控制器中的判断逻辑）
    public boolean isTeacher() {
        return isLoggedIn && userType == 2;
    }

    // 辅助方法：判断是否为学生
    public boolean isStudent() {
        return isLoggedIn && userType == 1;
    }

    // 辅助方法：判断是否为管理员
    public boolean isAdmin() {
        return isLoggedIn && userType == 3;
    }
}