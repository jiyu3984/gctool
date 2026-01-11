package com.genshin.gm.util;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * OpenCommand指令处理工具类
 * 用于智能处理需要UID的GM指令
 */
public class CommandProcessor {

    // 需要UID作为第一个参数的指令列表
    private static final Set<String> UID_FIRST_COMMANDS = new HashSet<>(Arrays.asList(
            "give", "giveall", "giveart", "givechar",
            "avatar", "char",
            "weapon",
            "tp", "teleport",
            "quest",
            "heal",
            "godmode", "god",
            "setstats", "stats",
            "setfetterlevel", "setfetter",
            "setconstellation", "setconst",
            "settalent",
            "setworldlevel", "setwl",
            "energy",
            "resetconst",
            "killall", "killcharacter",
            "team",
            "mail"
    ));

    // 不需要UID的指令列表
    private static final Set<String> NO_UID_COMMANDS = new HashSet<>(Arrays.asList(
            "help", "h",
            "reload",
            "status",
            "list",
            "clear",
            "account",
            "announce", "announcement",
            "broadcast",
            "coop",
            "enter_dungeon", "dungeon",
            "prop",
            "resetshop",
            "sendmail",
            "sendmessage",
            "spawn",
            "stop",
            "weather",
            "position", "pos"
    ));

    /**
     * 处理指令，智能添加UID
     * @param command 原始指令
     * @param uid 玩家UID
     * @return 处理后的指令
     */
    public static String processCommand(String command, String uid) {
        if (command == null || command.trim().isEmpty()) {
            return command;
        }

        if (uid == null || uid.trim().isEmpty()) {
            throw new IllegalArgumentException("UID不能为空");
        }

        command = command.trim();
        uid = uid.trim();

        // 如果已经包含@UID占位符，直接替换
        if (command.contains("@UID")) {
            return command.replace("@UID", "@" + uid);
        }

        // 如果包含 "@ " (@ + 空格)，替换为 @UID + 空格
        if (command.contains("@ ")) {
            return command.replace("@ ", "@" + uid + " ");
        }

        // 如果已经包含@但后面跟着数字（说明已经有UID了），不处理
        Pattern uidPattern = Pattern.compile("@\\d+");
        Matcher matcher = uidPattern.matcher(command);
        if (matcher.find()) {
            return command; // 已经有UID了，不处理
        }

        // 分割指令获取指令名
        String[] parts = command.split("\\s+");
        if (parts.length == 0) {
            return command;
        }

        String cmdName = parts[0].toLowerCase();

        // 移除指令名开头的斜杠（支持 /tp 和 tp 两种格式）
        if (cmdName.startsWith("/")) {
            cmdName = cmdName.substring(1);
        }

        // 检查是否是不需要UID的指令
        if (NO_UID_COMMANDS.contains(cmdName)) {
            return command;
        }

        // 检查是否是需要UID的指令
        if (UID_FIRST_COMMANDS.contains(cmdName)) {
            // 检查第一个参数是否已经是@开头的UID
            if (parts.length > 1 && parts[1].startsWith("@")) {
                // 已经有@，可能是占位符，替换它
                parts[1] = "@" + uid;
                return String.join(" ", parts);
            } else {
                // 没有@，在指令名后插入@UID
                StringBuilder result = new StringBuilder(parts[0]);
                result.append(" @").append(uid);
                for (int i = 1; i < parts.length; i++) {
                    result.append(" ").append(parts[i]);
                }
                return result.toString();
            }
        }

        // 其他情况：尝试在指令名后添加@UID（保守策略）
        // 这是为了处理一些未列出但可能需要UID的指令
        if (parts.length > 1) {
            // 如果第二个参数看起来不像UID（不是纯数字），则在前面添加UID
            if (!parts[1].matches("\\d+")) {
                StringBuilder result = new StringBuilder(parts[0]);
                result.append(" @").append(uid);
                for (int i = 1; i < parts.length; i++) {
                    result.append(" ").append(parts[i]);
                }
                return result.toString();
            }
        }

        // 默认返回原指令
        return command;
    }

    /**
     * 检查指令是否需要UID
     * @param command 指令
     * @return true如果需要UID
     */
    public static boolean needsUid(String command) {
        if (command == null || command.trim().isEmpty()) {
            return false;
        }

        String[] parts = command.trim().split("\\s+");
        if (parts.length == 0) {
            return false;
        }

        String cmdName = parts[0].toLowerCase();
        return UID_FIRST_COMMANDS.contains(cmdName);
    }

    /**
     * 验证指令格式
     * @param command 指令
     * @return 错误信息，如果格式正确则返回null
     */
    public static String validateCommand(String command) {
        if (command == null || command.trim().isEmpty()) {
            return "指令不能为空";
        }

        command = command.trim();

        // 基本格式检查
        if (command.length() > 500) {
            return "指令过长（最多500字符）";
        }

        // 检查是否包含危险字符
        if (command.contains(";") || command.contains("&&") || command.contains("|")) {
            return "指令包含不允许的字符";
        }

        return null; // 验证通过
    }
}
