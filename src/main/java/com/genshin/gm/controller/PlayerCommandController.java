package com.genshin.gm.controller;

import com.genshin.gm.config.AppConfig;
import com.genshin.gm.config.ConfigLoader;
import com.genshin.gm.model.OpenCommandResponse;
import com.genshin.gm.model.PlayerCommand;
import com.genshin.gm.service.GrasscutterService;
import com.genshin.gm.service.PlayerCommandService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 玩家指令控制器
 */
@RestController
@RequestMapping("/api/commands")
public class PlayerCommandController {

    private static final Logger logger = LoggerFactory.getLogger(PlayerCommandController.class);

    @Autowired
    private PlayerCommandService service;

    @Autowired
    private GrasscutterService grasscutterService;

    /**
     * 提交新指令
     */
    @PostMapping("/submit")
    public ResponseEntity<Map<String, Object>> submitCommand(@RequestBody PlayerCommand command) {
        Map<String, Object> response = new HashMap<>();
        try {
            PlayerCommand saved = service.submitCommand(command);
            response.put("success", true);
            response.put("message", "指令提交成功，等待审核");
            response.put("data", saved);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("提交指令失败", e);
            response.put("success", false);
            response.put("message", "提交失败: " + e.getMessage());
            return ResponseEntity.ok(response);
        }
    }

    /**
     * 获取所有已审核通过的指令
     */
    @GetMapping("/approved")
    public ResponseEntity<List<PlayerCommand>> getApprovedCommands(
            @RequestParam(required = false) String category,
            @RequestParam(required = false, defaultValue = "time") String sort) {

        try {
            List<PlayerCommand> commands;

            if (category != null && !category.isEmpty()) {
                commands = service.getApprovedCommandsByCategory(category);
            } else if ("popular".equals(sort)) {
                commands = service.getPopularCommands();
            } else {
                commands = service.getApprovedCommands();
            }

            return ResponseEntity.ok(commands);
        } catch (Exception e) {
            logger.error("获取指令列表失败", e);
            return ResponseEntity.ok(List.of());
        }
    }

    /**
     * 增加浏览数
     */
    @PostMapping("/{id}/view")
    public ResponseEntity<Map<String, Object>> incrementViews(@PathVariable String id) {
        Map<String, Object> response = new HashMap<>();
        try {
            service.incrementViews(id);
            response.put("success", true);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("增加浏览数失败", e);
            response.put("success", false);
            return ResponseEntity.ok(response);
        }
    }

    /**
     * 点赞（需要UID）
     */
    @PostMapping("/{id}/like")
    public ResponseEntity<Map<String, Object>> likeCommand(
            @PathVariable String id,
            @RequestBody Map<String, String> body) {

        Map<String, Object> response = new HashMap<>();
        try {
            String uid = body.get("uid");
            if (uid == null || uid.trim().isEmpty()) {
                response.put("success", false);
                response.put("message", "请提供UID");
                return ResponseEntity.ok(response);
            }

            boolean success = service.likeCommand(id, uid);
            if (success) {
                response.put("success", true);
                response.put("message", "点赞成功");
            } else {
                response.put("success", false);
                response.put("message", "您已经点过赞了");
            }
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("点赞失败", e);
            response.put("success", false);
            response.put("message", "点赞失败");
            return ResponseEntity.ok(response);
        }
    }

    /**
     * 执行指令到玩家账户（通过OpenCommand）
     */
    @PostMapping("/{id}/execute")
    public ResponseEntity<Map<String, Object>> executeCommand(
            @PathVariable String id,
            @RequestBody Map<String, String> body) {

        Map<String, Object> response = new HashMap<>();
        try {
            String uid = body.get("uid");
            if (uid == null || uid.trim().isEmpty()) {
                response.put("success", false);
                response.put("message", "请提供UID");
                return ResponseEntity.ok(response);
            }

            // 获取指令详情
            Optional<PlayerCommand> optional = service.getCommandById(id);
            if (!optional.isPresent()) {
                response.put("success", false);
                response.put("message", "指令不存在");
                return ResponseEntity.ok(response);
            }

            PlayerCommand command = optional.get();

            // 增加浏览数
            service.incrementViews(id);

            // 获取Grasscutter配置
            AppConfig config = ConfigLoader.getConfig();
            if (config == null || config.getGrasscutter() == null) {
                response.put("success", false);
                response.put("message", "Grasscutter配置未找到");
                return ResponseEntity.ok(response);
            }

            AppConfig.GrasscutterConfig gcConfig = config.getGrasscutter();

            // 替换指令中的@为实际UID
            String finalCommand = command.getCommand().replace("@", "@" + uid);

            // 执行指令
            OpenCommandResponse result = grasscutterService.executeConsoleCommand(
                    gcConfig.getFullUrl(),
                    gcConfig.getConsoleToken(),
                    finalCommand
            );

            if (result.getRetcode() == 200) {
                response.put("success", true);
                response.put("message", "指令执行成功");
                response.put("data", result.getData());
            } else {
                response.put("success", false);
                response.put("message", "指令执行失败: " + result.getMessage());
            }

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("执行指令失败", e);
            response.put("success", false);
            response.put("message", "执行失败: " + e.getMessage());
            return ResponseEntity.ok(response);
        }
    }

    /**
     * 获取所有指令（管理后台）
     */
    @GetMapping("/admin/all")
    public ResponseEntity<List<PlayerCommand>> getAllCommands() {
        try {
            return ResponseEntity.ok(service.getAllCommands());
        } catch (Exception e) {
            logger.error("获取所有指令失败", e);
            return ResponseEntity.ok(List.of());
        }
    }

    /**
     * 获取待审核的指令（管理后台）
     */
    @GetMapping("/admin/pending")
    public ResponseEntity<List<PlayerCommand>> getPendingCommands() {
        try {
            return ResponseEntity.ok(service.getPendingCommands());
        } catch (Exception e) {
            logger.error("获取待审核指令失败", e);
            return ResponseEntity.ok(List.of());
        }
    }

    /**
     * 审核通过（管理后台）
     */
    @PostMapping("/admin/{id}/approve")
    public ResponseEntity<Map<String, Object>> approveCommand(
            @PathVariable String id,
            @RequestBody(required = false) Map<String, String> body) {

        Map<String, Object> response = new HashMap<>();
        try {
            String reviewNote = body != null ? body.get("reviewNote") : "";
            PlayerCommand updated = service.approveCommand(id, reviewNote);

            if (updated != null) {
                response.put("success", true);
                response.put("message", "审核通过");
                response.put("data", updated);
            } else {
                response.put("success", false);
                response.put("message", "指令不存在");
            }
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("审核通过失败", e);
            response.put("success", false);
            response.put("message", "操作失败: " + e.getMessage());
            return ResponseEntity.ok(response);
        }
    }

    /**
     * 审核拒绝（管理后台）
     */
    @PostMapping("/admin/{id}/reject")
    public ResponseEntity<Map<String, Object>> rejectCommand(
            @PathVariable String id,
            @RequestBody(required = false) Map<String, String> body) {

        Map<String, Object> response = new HashMap<>();
        try {
            String reviewNote = body != null ? body.get("reviewNote") : "";
            PlayerCommand updated = service.rejectCommand(id, reviewNote);

            if (updated != null) {
                response.put("success", true);
                response.put("message", "已拒绝");
                response.put("data", updated);
            } else {
                response.put("success", false);
                response.put("message", "指令不存在");
            }
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("审核拒绝失败", e);
            response.put("success", false);
            response.put("message", "操作失败: " + e.getMessage());
            return ResponseEntity.ok(response);
        }
    }

    /**
     * 删除指令（管理后台）
     */
    @DeleteMapping("/admin/{id}")
    public ResponseEntity<Map<String, Object>> deleteCommand(@PathVariable String id) {
        Map<String, Object> response = new HashMap<>();
        try {
            service.deleteCommand(id);
            response.put("success", true);
            response.put("message", "删除成功");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("删除指令失败", e);
            response.put("success", false);
            response.put("message", "删除失败: " + e.getMessage());
            return ResponseEntity.ok(response);
        }
    }
}
