package com.genshin.gm.util;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * OpenCommand指令处理工具类
 * 用于智能处理需要UID的GM指令
 */
public class CommandProcessor {

    // 可以指定目标UID的指令列表（在远程执行时需要添加@UID）
    // 注意：Grasscutter允许@UID放在命令的任意位置，但为了远程执行，我们统一放在指令名后面
    private static final Set<String> UID_TARGETABLE_COMMANDS = new HashSet<>(Arrays.asList(
            // 物品给予类
            "give", "giveall", "giveart", "givechar",

            // 角色管理类
            "avatar", "char",
            "setfetterlevel", "setfetter",
            "setconstellation", "setconst",
            "settalent", "talent",

            // 武器类
            "weapon",

            // 传送类
            "tp", "teleport",
            "tpall",

            // 任务类
            "quest",

            // 玩家状态类
            "heal",
            "setstats", "stats",
            "setworldlevel", "setwl",
            "energy",

            // 战斗相关
            "killall", "killcharacter",

            // 队伍管理
            "team",

            // 邮件
            "mail",

            // 圣遗物构建
            "build",

            // 其他
            "kick",
            "ban",
            "unban"
    ));

    // 不需要UID的指令列表（这些指令直接对当前玩家生效，或是服务器管理指令）
    private static final Set<String> NO_UID_COMMANDS = new HashSet<>(Arrays.asList(
            // 信息查询类
            "help", "h",
            "list",
            "position", "pos",
            "status",

            // 服务器管理类
            "reload",
            "stop",
            "account",
            "announce", "announcement",
            "broadcast",
            "sendmessage", "say",

            // 自身效果类（作用于执行者自己）
            "clear",        // 清理自己的物品
            "spawn",        // 在自己附近生成
            "weather",      // 改变当前天气
            "coop",         // 加入联机
            "enterdungeon", "dungeon",  // 自己进入副本
            "resetshop",    // 重置自己的商店
            "resetconst",   // 重置自己的命座
            "unlockall",    // 解锁自己的所有内容

            // prop系列（作用于自己）
            "prop", "setprop",
            "godmode", "god",
            "nostamina", "ns",
            "unlimitedenergy", "ue",

            // 其他自身指令
            "permission"    // 给自己添加权限
    ));

    /**
     * 处理指令，智能添加UID
     * 规则：
     * 1. give 和 quest 命令：UID 在命令名后面（例如：give @UID 202 x999）
     * 2. 其他需要UID的命令：UID 在命令最后（例如：tp 1000 500 -1000 @UID）
     * 3. 处理前先删除命令中已有的 @ 符号，然后统一添加 @UID
     *
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

        // 第一步：移除命令中所有的 @ 符号和相关内容
        // 匹配模式：@UID, @数字, @ （包括后面的空格）
        String cleanCommand = command.replaceAll("@UID", "")
                                    .replaceAll("@\\d+", "")
                                    .replaceAll("@\\s+", "")
                                    .replaceAll("@", "")
                                    .replaceAll("\\s+", " ")
                                    .trim();

        // 重新分割清理后的命令
        parts = cleanCommand.split("\\s+");
        String cleanCmdName = parts[0].toLowerCase();
        if (cleanCmdName.startsWith("/")) {
            cleanCmdName = cleanCmdName.substring(1);
        }

        // 第二步：根据命令类型添加 @UID
        // give 和 quest 命令：UID 在命令名后面
        if (cleanCmdName.equals("give") || cleanCmdName.equals("giveall") || cleanCmdName.equals("quest")) {
            String[] cmdParts = cleanCommand.split("\\s+", 2); // 分割成命令名和剩余部分
            if (cmdParts.length == 1) {
                // 只有命令名，没有参数
                return cmdParts[0] + " @" + uid;
            } else {
                // 命令名 + @UID + 剩余参数
                return cmdParts[0] + " @" + uid + " " + cmdParts[1];
            }
        }

        // 其他所有需要UID的命令：UID 在最后
        if (UID_TARGETABLE_COMMANDS.contains(cleanCmdName)) {
            return cleanCommand + " @" + uid;
        }

        // 未知命令，保守策略：在最后添加 @UID
        return cleanCommand + " @" + uid;
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
        return UID_TARGETABLE_COMMANDS.contains(cmdName);
    }

    /**
     * 验证指令格式（基于Grasscutter标准命令格式）
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

        // 移除开头的斜杠进行检查
        String cmdForValidation = command.startsWith("/") ? command.substring(1) : command;
        String[] parts = cmdForValidation.split("\\s+");

        if (parts.length == 0) {
            return "指令格式错误";
        }

        String cmdName = parts[0].toLowerCase();

        // 验证具体指令格式
        String validationError = validateSpecificCommand(cmdName, parts);
        if (validationError != null) {
            return validationError;
        }

        return null; // 验证通过
    }

    /**
     * 验证特定指令的参数格式
     */
    private static String validateSpecificCommand(String cmdName, String[] parts) {
        switch (cmdName) {
            case "clear":
                // clear 需要参数: all, wp, art, mat
                if (parts.length < 2) {
                    return "clear指令缺少参数，用法: clear <all|wp|art|mat> [lv<level>] [r<refinement>] [<quality>*]";
                }
                String clearType = parts[1].toLowerCase();
                if (!clearType.equals("all") && !clearType.equals("wp") &&
                    !clearType.equals("art") && !clearType.equals("mat")) {
                    return "clear指令参数错误，必须是 all, wp, art 或 mat 之一";
                }
                break;

            case "give":
            case "giveall":
                // give 需要物品ID或角色ID
                if (parts.length < 2) {
                    return "give指令缺少物品/角色ID，用法: give <itemId|avatarId> [x<amount>] [lv<level>] [r<refinement>] [c<constellation>]";
                }
                // 检查第二个参数是否是数字或@UID
                if (!parts[1].matches("@?\\d+")) {
                    return "give指令的物品/角色ID格式错误，应为数字";
                }
                // 验证修饰符格式（x, lv, r, c）
                for (int i = 2; i < parts.length; i++) {
                    String param = parts[i].toLowerCase();
                    if (!param.startsWith("@") && !validateGiveModifier(param)) {
                        return "give指令参数错误: '" + parts[i] + "'，支持的修饰符: x<amount>, lv<level>, r<refinement>, c<constellation>";
                    }
                }
                break;

            case "spawn":
                // spawn 需要实体ID
                if (parts.length < 2) {
                    return "spawn指令缺少实体ID，用法: spawn <entityId> [amount] [level]";
                }
                if (!parts[1].matches("\\d+")) {
                    return "spawn指令的实体ID必须是数字";
                }
                // 验证可选的数量和等级参数
                if (parts.length > 2 && !parts[2].matches("\\d+") && !parts[2].startsWith("x")) {
                    return "spawn指令的数量参数格式错误";
                }
                break;

            case "prop":
            case "setprop":
                // prop 需要属性名和值
                if (parts.length < 2) {
                    return "prop指令缺少参数，用法: prop <property> <value> 或 prop <godmode|nostamina|unlimitedenergy> <on|off>";
                }
                break;

            case "account":
                // account 需要 create/delete 动作
                if (parts.length < 3) {
                    return "account指令缺少参数，用法: account <create|delete> <username> [UID]";
                }
                String action = parts[1].toLowerCase();
                if (!action.equals("create") && !action.equals("delete")) {
                    return "account指令的动作必须是 create 或 delete";
                }
                break;

            case "permission":
                // permission 需要 add/remove 动作
                if (parts.length < 3) {
                    return "permission指令缺少参数，用法: permission <add|remove> <permission>";
                }
                String permAction = parts[1].toLowerCase();
                if (!permAction.equals("add") && !permAction.equals("remove")) {
                    return "permission指令的动作必须是 add 或 remove";
                }
                break;

            case "teleport":
            case "tp":
                // teleport 需要坐标或场景
                if (parts.length < 2) {
                    return "teleport指令缺少参数，用法: tp <x> <y> <z> [sceneId] 或 tp @UID";
                }
                // 如果不是@UID格式，则需要至少3个坐标参数
                if (!parts[1].startsWith("@") && parts.length < 4) {
                    return "teleport指令需要3个坐标参数 (x, y, z)";
                }
                break;

            case "team":
                // team 需要动作参数
                if (parts.length < 2) {
                    return "team指令缺少参数，用法: team <add|remove|set> [avatarId,...]";
                }
                String teamAction = parts[1].toLowerCase();
                if (!teamAction.equals("add") && !teamAction.equals("remove") && !teamAction.equals("set")) {
                    return "team指令的动作必须是 add, remove 或 set";
                }
                break;

            case "enterdungeon":
            case "dungeon":
                // enterdungeon 需要副本ID
                if (parts.length < 2) {
                    return "enterdungeon指令缺少副本ID，用法: enterdungeon <dungeonId>";
                }
                if (!parts[1].matches("\\d+")) {
                    return "enterdungeon指令的副本ID必须是数字";
                }
                break;

            case "setstats":
            case "stats":
                // setstats 需要属性和值
                if (parts.length < 3) {
                    return "setstats指令缺少参数，用法: setstats <stat> <value>";
                }
                break;

            case "setfetterlevel":
            case "setfetter":
                // setfetterlevel 需要等级
                if (parts.length < 2) {
                    return "setfetterlevel指令缺少等级参数，用法: setfetterlevel <level>";
                }
                if (!parts[1].matches("\\d+")) {
                    return "setfetterlevel指令的等级必须是数字";
                }
                break;

            case "weather":
                // weather 可以有参数也可以没有
                if (parts.length > 1 && !parts[1].matches("\\d+")) {
                    return "weather指令的天气ID必须是数字，用法: weather [weatherId] [climateId]";
                }
                break;

            case "build":
                // build 需要构建名称
                if (parts.length < 2) {
                    return "build指令缺少构建名称，用法: build <buildName>";
                }
                // 验证构建名称格式（只允许字母、数字）
                if (!parts[1].matches("[a-zA-Z0-9]+")) {
                    return "build指令的构建名称格式错误，只允许字母和数字";
                }
                break;

            // 其他指令暂不做严格验证，允许通过
            default:
                break;
        }

        return null; // 验证通过
    }

    /**
     * 验证give指令的修饰符格式
     */
    private static boolean validateGiveModifier(String modifier) {
        String lower = modifier.toLowerCase();

        // x<amount> or <amount>x
        if (lower.matches("\\d+x|x\\d+")) {
            return true;
        }

        // lv<level>, l<level>, lvl<level>
        if (lower.matches("(lv|l|lvl)\\d+")) {
            return true;
        }

        // r<refinement>
        if (lower.matches("r\\d+")) {
            return true;
        }

        // c<constellation>
        if (lower.matches("c\\d+")) {
            return true;
        }

        // 组合形式，如 lv90r5, x10c6 等
        if (lower.matches("((lv|l|lvl)?\\d+)?(r\\d+)?(c\\d+)?(x\\d+)?")) {
            return true;
        }

        // 星级，如 5* 或 4*
        if (lower.matches("\\d+\\*")) {
            return true;
        }

        return false;
    }
}
