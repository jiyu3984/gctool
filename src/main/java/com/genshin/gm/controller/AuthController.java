package com.genshin.gm.controller;

import com.genshin.gm.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 认证控制器
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    @Autowired
    private UserService userService;

    /**
     * 用户注册
     */
    @PostMapping("/register")
    public ResponseEntity<Map<String, Object>> register(@RequestBody Map<String, String> body) {
        String username = body.get("username");
        String password = body.get("password");

        Map<String, Object> result = userService.register(username, password);
        return ResponseEntity.ok(result);
    }

    /**
     * 用户登录
     */
    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@RequestBody Map<String, String> body) {
        String username = body.get("username");
        String password = body.get("password");

        Map<String, Object> result = userService.login(username, password);
        return ResponseEntity.ok(result);
    }

    /**
     * 登出
     */
    @PostMapping("/logout")
    public ResponseEntity<Map<String, Object>> logout(@RequestBody Map<String, String> body) {
        Map<String, Object> result = new HashMap<>();

        try {
            String sessionToken = body.get("sessionToken");
            userService.logout(sessionToken);

            result.put("success", true);
            result.put("message", "登出成功");

        } catch (Exception e) {
            logger.error("登出失败", e);
            result.put("success", false);
            result.put("message", "登出失败");
        }

        return ResponseEntity.ok(result);
    }

    /**
     * 获取用户信息
     */
    @GetMapping("/user-info")
    public ResponseEntity<Map<String, Object>> getUserInfo(@RequestParam String sessionToken) {
        Map<String, Object> result = userService.getUserInfo(sessionToken);
        return ResponseEntity.ok(result);
    }

    /**
     * 添加已验证的UID
     */
    @PostMapping("/add-uid")
    public ResponseEntity<Map<String, Object>> addVerifiedUid(@RequestBody Map<String, String> body) {
        Map<String, Object> result = new HashMap<>();

        try {
            String sessionToken = body.get("sessionToken");
            String uid = body.get("uid");

            String username = userService.validateSession(sessionToken);
            if (username == null) {
                result.put("success", false);
                result.put("message", "未登录或session已过期");
                return ResponseEntity.ok(result);
            }

            boolean success = userService.addVerifiedUid(username, uid);
            if (success) {
                result.put("success", true);
                result.put("message", "UID已添加到您的账户");
            } else {
                result.put("success", false);
                result.put("message", "添加UID失败");
            }

        } catch (Exception e) {
            logger.error("添加UID失败", e);
            result.put("success", false);
            result.put("message", "添加UID失败");
        }

        return ResponseEntity.ok(result);
    }

    /**
     * 移除已验证的UID
     */
    @PostMapping("/remove-uid")
    public ResponseEntity<Map<String, Object>> removeVerifiedUid(@RequestBody Map<String, String> body) {
        Map<String, Object> result = new HashMap<>();

        try {
            String sessionToken = body.get("sessionToken");
            String uid = body.get("uid");

            String username = userService.validateSession(sessionToken);
            if (username == null) {
                result.put("success", false);
                result.put("message", "未登录或session已过期");
                return ResponseEntity.ok(result);
            }

            boolean success = userService.removeVerifiedUid(username, uid);
            if (success) {
                result.put("success", true);
                result.put("message", "UID已移除");
            } else {
                result.put("success", false);
                result.put("message", "移除UID失败");
            }

        } catch (Exception e) {
            logger.error("移除UID失败", e);
            result.put("success", false);
            result.put("message", "移除UID失败");
        }

        return ResponseEntity.ok(result);
    }

    /**
     * 检查UID是否已验证
     */
    @GetMapping("/check-uid")
    public ResponseEntity<Map<String, Object>> checkUidVerified(
            @RequestParam String sessionToken,
            @RequestParam String uid) {

        Map<String, Object> result = new HashMap<>();

        try {
            String username = userService.validateSession(sessionToken);
            if (username == null) {
                result.put("success", false);
                result.put("verified", false);
                result.put("message", "未登录或session已过期");
                return ResponseEntity.ok(result);
            }

            boolean verified = userService.isUidVerified(username, uid);
            result.put("success", true);
            result.put("verified", verified);

        } catch (Exception e) {
            logger.error("检查UID验证状态失败", e);
            result.put("success", false);
            result.put("verified", false);
            result.put("message", "检查失败");
        }

        return ResponseEntity.ok(result);
    }
}
