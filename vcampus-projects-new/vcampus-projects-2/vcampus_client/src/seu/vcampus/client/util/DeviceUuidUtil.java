package seu.vcampus.client.util;

import java.net.NetworkInterface;
import java.util.Enumeration;
import java.util.UUID;

/**
 * 设备唯一标识工具类（生成设备UUID，用于生成AES密钥）
 */
public class DeviceUuidUtil {
    private static UUID deviceUUID;

    // 获取设备唯一UUID（不同设备生成不同值，同一设备固定）
    public static UUID getDeviceUuid() {
        if (deviceUUID == null) {
            try {
                // 优先从网卡MAC地址生成UUID（跨平台）
                Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
                while (interfaces.hasMoreElements()) {
                    NetworkInterface ni = interfaces.nextElement();
                    byte[] mac = ni.getHardwareAddress();
                    if (mac != null) {
                        deviceUUID = UUID.nameUUIDFromBytes(mac);
                        return deviceUUID;
                    }
                }
                // 若无法获取MAC，从系统属性生成（降级方案）
                String fallback = System.getProperty("os.name") + System.getProperty("user.name") + System.getProperty("java.vm.specification.version");
                deviceUUID = UUID.nameUUIDFromBytes(fallback.getBytes());
            } catch (Exception e) {
                // 极端情况：生成随机UUID（不推荐，会导致密钥变化）
                deviceUUID = UUID.randomUUID();
            }
        }
        return deviceUUID;
    }

    // 将UUID转为16位AES密钥（AES密钥长度需16/24/32位）
    public static String getAesKeyFromDeviceUuid() {
        String uuidStr = getDeviceUuid().toString().replace("-", "");
        return uuidStr.substring(0, 16); // 截取前16位，满足AES-128密钥长度
    }
}