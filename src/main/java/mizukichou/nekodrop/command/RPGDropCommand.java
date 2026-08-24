package mizukichou.nekodrop.command;

import mizukichou.nekodrop.RPGDropPlugin;
import mizukichou.nekodrop.command.sub.AmountCommand;
import mizukichou.nekodrop.command.sub.ChanceCommand;
import mizukichou.nekodrop.command.sub.CreateCommand;
import mizukichou.nekodrop.command.sub.DeleteCommand;
import mizukichou.nekodrop.command.sub.EntityCommand;
import mizukichou.nekodrop.command.sub.GuiCommand;
import mizukichou.nekodrop.command.sub.HelpCommand;
import mizukichou.nekodrop.command.sub.InfoCommand;
import mizukichou.nekodrop.command.sub.ItemCommand;
import mizukichou.nekodrop.command.sub.ListCommand;
import mizukichou.nekodrop.command.sub.ReloadCommand;
import mizukichou.nekodrop.command.sub.WorldCommand;
import mizukichou.nekodrop.drop.DropManager;
import mizukichou.nekodrop.util.Log;
import mizukichou.nekodrop.util.Msg;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * /nekodrop（别名 /ndrop）根命令：只做分发，不写业务逻辑。
 */
public final class RPGDropCommand implements CommandExecutor, TabCompleter {

    private final RPGDropPlugin plugin;
    private final DropManager dropManager;
    private final Log log;
    private final Map<String, SubCommand> subCommands = new LinkedHashMap<>();

    public RPGDropCommand(RPGDropPlugin plugin, DropManager dropManager, Log log) {
        this.plugin = plugin;
        this.dropManager = dropManager;
        this.log = log;

        register(new HelpCommand(this));
        register(new ListCommand(dropManager));
        register(new InfoCommand(dropManager));
        register(new CreateCommand(dropManager));
        register(new DeleteCommand(dropManager));
        register(new EntityCommand(dropManager));
        register(new WorldCommand(dropManager));
        register(new ItemCommand(plugin, dropManager));
        register(new ChanceCommand(dropManager));
        register(new AmountCommand(dropManager));
        register(new ReloadCommand(plugin));
        register(new GuiCommand(plugin, dropManager));
    }

    private void register(SubCommand sub) {
        subCommands.put(sub.name().toLowerCase(Locale.ROOT), sub);
    }

    public Map<String, SubCommand> subCommands() {
        return subCommands;
    }

    public RPGDropPlugin plugin() {
        return plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            subCommands.get("help").execute(sender, new String[0]);
            return true;
        }

        SubCommand sub = subCommands.get(args[0].toLowerCase(Locale.ROOT));
        if (sub == null) {
            Msg.send(sender, "command.unknown_subcommand", args[0], label);
            return true;
        }
        if (!sub.permission().isEmpty() && !sender.hasPermission(sub.permission())) {
            Msg.send(sender, "command.no_permission");
            return true;
        }

        String[] rest = Arrays.copyOfRange(args, 1, args.length);
        if (rest.length < sub.minArgs()) {
            Msg.send(sender, "command.usage_hint", usageOf(sender, sub));
            return true;
        }

        try {
            sub.execute(sender, rest);
        } catch (Exception e) {
            log.severe("Error while executing command /" + label + " " + String.join(" ", args), e);
            Msg.send(sender, "command.execution_error");
        }
        return true;
    }

    /** 用法文本：优先语言文件，否则使用纯英文语法。 */
    public String usageOf(CommandSender sender, SubCommand sub) {
        return sub.usageKey() != null ? Msg.tr(sender, sub.usageKey()) : sub.usage();
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return TabUtil.filter(subCommands.keySet(), args[0]);
        }
        SubCommand sub = subCommands.get(args[0].toLowerCase(Locale.ROOT));
        if (sub == null) {
            return List.of();
        }
        return sub.tabComplete(sender, Arrays.copyOfRange(args, 1, args.length));
    }
}
