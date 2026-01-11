package com.genshin.gm.service;

import com.genshin.gm.config.ConfigLoader;
import com.genshin.gm.model.OpenCommandResponse;
import com.genshin.gm.model.VerificationCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 验证码服务
 * 管理UID验证码的生成、验证和过期清理
 */
@Service
public class VerificationService {
    private static final Logger logger = LoggerFactory.getLogger(VerificationService.class);

    // 验证码有效期（分钟）
    private static final int EXPIRY_MINUTES = 5;

    // 存储验证码的Map: UID -> VerificationCode
    private final Map<String, VerificationCode> verificationCodes = new ConcurrentHashMap<>();

    @Autowired
    private GrasscutterService grasscutterService;

    private final Random random = new Random();

    /**
     * 生成6位数字验证码
     */
    private String generateCode() {
        int code = 100000 + random.nextInt(900000);
        return String.valueOf(code);
    }

    /**
     * 发送验证码到玩家游戏内邮箱
     * @param uid 玩家UID
     * @return 操作结果
     */
    public Map<String, Object> sendVerificationCode(String uid) {
        Map<String, Object> result = new ConcurrentHashMap<>();

        try {
            // 生成验证码
            String code = generateCode();
            LocalDateTime expiryTime = LocalDateTime.now().plusMinutes(EXPIRY_MINUTES);

            // 存储验证码
            VerificationCode verificationCode = new VerificationCode(uid, code, expiryTime);
            verificationCodes.put(uid, verificationCode);

            logger.info("为UID {} 生成验证码: {}, 过期时间: {}", uid, code, expiryTime);

            // 通过OpenCommand发送邮件到游戏内
            String serverUrl = ConfigLoader.getConfig().getGrasscutter().getFullUrl();
            String consoleToken = ConfigLoader.getConfig().getGrasscutter().getConsoleToken();

            // 构建发送邮件的指令
            String mailCommand = String.format(
                "sendmail %s \"身份验证\" \"您的验证码是：%s，有效期5分钟。如非本人操作，请忽略此邮件。\" 201 1",
                uid, code
            );

            logger.info("发送验证码邮件指令: {}", mailCommand);

            OpenCommandResponse response = grasscutterService.executeConsoleCommand(
                serverUrl,
                consoleToken,
                mailCommand
            );

            if (response != null && response.getRetcode() == 200) {
                result.put("success", true);
                result.put("message", "验证码已发送到游戏内邮箱，请查收");
                result.put("expiryMinutes", EXPIRY_MINUTES);
            } else {
                // 发送失败，清除验证码
                verificationCodes.remove(uid);
                result.put("success", false);
                result.put("message", "发送验证码失败: " + (response != null ? response.getMessage() : "未知错误"));
            }

        } catch (Exception e) {
            logger.error("发送验证码异常", e);
            verificationCodes.remove(uid);
            result.put("success", false);
            result.put("message", "发送验证码失败: " + e.getMessage());
        }

        return result;
    }

    /**
     * 验证验证码
     * @param uid 玩家UID
     * @param code 输入的验证码
     * @return 验证结果
     */
    public Map<String, Object> verifyCode(String uid, String code) {
        Map<String, Object> result = new ConcurrentHashMap<>();

        VerificationCode storedCode = verificationCodes.get(uid);

        if (storedCode == null) {
            result.put("success", false);
            result.put("message", "验证码不存在或已过期，请重新获取");
            return result;
        }

        if (storedCode.isExpired()) {
            verificationCodes.remove(uid);
            result.put("success", false);
            result.put("message", "验证码已过期，请重新获取");
            return result;
        }

        if (storedCode.getCode().equals(code)) {
            // 验证成功，标记为已验证
            storedCode.setVerified(true);
            result.put("success", true);
            result.put("message", "验证成功");
            result.put("expiryTime", storedCode.getExpiryTime().toString());
            logger.info("UID {} 验证成功", uid);
        } else {
            result.put("success", false);
            result.put("message", "验证码错误");
            logger.warn("UID {} 验证码错误: 输入={}, 实际={}", uid, code, storedCode.getCode());
        }

        return result;
    }

    /**
     * 检查UID是否已验证且验证未过期
     * @param uid 玩家UID
     * @return 是否有效
     */
    public boolean isVerified(String uid) {
        VerificationCode code = verificationCodes.get(uid);
        if (code == null) {
            return false;
        }

        if (code.isExpired()) {
            verificationCodes.remove(uid);
            return false;
        }

        return code.isVerified();
    }

    /**
     * 获取验证状态
     * @param uid 玩家UID
     * @return 验证状态信息
     */
    public Map<String, Object> getVerificationStatus(String uid) {
        Map<String, Object> result = new ConcurrentHashMap<>();

        VerificationCode code = verificationCodes.get(uid);

        if (code == null) {
            result.put("verified", false);
            result.put("message", "未验证");
            return result;
        }

        if (code.isExpired()) {
            verificationCodes.remove(uid);
            result.put("verified", false);
            result.put("message", "验证已过期");
            return result;
        }

        result.put("verified", code.isVerified());
        result.put("expiryTime", code.getExpiryTime().toString());
        result.put("message", code.isVerified() ? "已验证" : "待验证");

        return result;
    }

    /**
     * 定时清理过期的验证码（每分钟执行一次）
     */
    @Scheduled(fixedRate = 60000)
    public void cleanupExpiredCodes() {
        int removed = 0;
        for (Map.Entry<String, VerificationCode> entry : verificationCodes.entrySet()) {
            if (entry.getValue().isExpired()) {
                verificationCodes.remove(entry.getKey());
                removed++;
            }
        }
        if (removed > 0) {
            logger.info("清理了 {} 个过期验证码", removed);
        }
    }
}
