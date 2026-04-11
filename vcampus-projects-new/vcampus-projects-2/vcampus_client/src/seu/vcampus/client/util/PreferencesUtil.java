package seu.vcampus.client.util;

import java.util.prefs.Preferences;

/**
 * 本地存储工具类（扩展密码存储）
 */
public class PreferencesUtil {
    // 存储节点（与之前一致，确保数据在同一目录）
    private static final Preferences PREFS = Preferences.userRoot().node("seu.vcampus.client.login");
    
    // 存储键（新增密码键）
    public static final String KEY_REMEMBERED_USER_ID = "remembered_user_id";   // 账号
    public static final String KEY_REMEMBERED_PASSWORD = "remembered_password"; // 加密后的密码

    // 存储字符串
    public static void putString(String key, String value) {
        PREFS.put(key, value);
    }

    // 读取字符串（默认空）
    public static String getString(String key) {
        return PREFS.get(key, "");
    }

    // 删除指定键
    public static void remove(String key) {
        PREFS.remove(key);
    }

    // 清除所有登录相关存储
    public static void clearLoginData() {
        remove(KEY_REMEMBERED_USER_ID);
        remove(KEY_REMEMBERED_PASSWORD);
    }
}