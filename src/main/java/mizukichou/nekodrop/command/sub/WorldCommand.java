package mizukichou.nekodrop.command.sub;

import mizukichou.nekodrop.command.SubCommand;
import mizukichou.nekodrop.command.TabUtil;
import mizukichou.nekodrop.drop.DropManager;
import mizukichou.nekodrop.drop.DropRule;
import mizukichou.nekodrop.util.Msg;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.CommandSender;

import java.util.List;
import java.util.Locale;

/**
 * /ndrop world <id> add|remove|list [world...]
 */
public final class WorldCommand implements SubCommand {

    private static final List<String> OPS = List.of("add", "remove", "list");

    private final DropManager dropManager;

    public WorldCommand(DropManager dropManager) {
        this.dropManager = dropManager;
    }

    @Override
    public String name() {
        return "world";
    }

    @Override
    public String usageKey() {
        return "command.usage.world";
    }

    @Override
    public String usage() {
        return "/ndrop world <id> add|remove|list [world name...]";
    }

    @Override
    public String descriptionKey() {
        return "command.desc.world";
    }

    @Override
    public int minArgs() {
        return 2;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        DropRule rule = dropManager.getRule(args[0]);
        if (rule == null) {
            Msg.send(sender, "command.rule_not_found", args[0]);
            return;
        }

        String op = args[1].toLowerCase(Locale.ROOT);
        switch (op) {
            case "list" -> {
                if (rule.worlds().isEmpty()) {
                    Msg.send(sender, "command.world_list_empty", rule.id());
                    return;
                }
                Msg.send(sender, "command.world_list", rule.id(), rule.worldMode().name(),
                        String.join(", ", rule.worlds().stream().sorted().toList()));
            }
            case "add", "remove" -> {
                if (args.length < 3) {
                    Msg.send(sender, "command.world_usage", rule.id(), op);
                    return;
                }
                boolean add = op.equals("add");
                for (int i = 2; i < args.length; i++) {
                    String input = args[i];
                    // 已加载的世界按真实名（大小写）归一，避免大小写写错导致永远匹配不上
                    World loaded = Bukkit.getWorld(input);
                    String world = loaded != null ? loaded.getName() : input;
                    if (add) {
                        rule.addWorld(world);
                        if (loaded == null) {
                            Msg.send(sender, "command.world_not_loaded", world);
                        }
                    } else {
                        rule.removeWorld(world);
                    }
                    Msg.send(sender, add ? "command.world_added" : "command.world_removed", world);
                }
                dropManager.ruleUpdated(rule);
            }
            default -> Msg.send(sender, "command.world_unknown_op", op);
        }
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            return TabUtil.filter(TabUtil.ruleIds(dropManager), args[0]);
        }
        if (args.length == 2) {
            return TabUtil.filter(OPS, args[1]);
        }
        if (args.length >= 3 && args[1].equalsIgnoreCase("add")) {
            List<String> worldNames = Bukkit.getWorlds().stream().map(World::getName).toList();
            return TabUtil.filter(worldNames, args[args.length - 1]);
        }
        return List.of();
    }
}
