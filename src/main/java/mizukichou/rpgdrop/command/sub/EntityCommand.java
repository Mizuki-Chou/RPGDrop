package mizukichou.rpgdrop.command.sub;

import mizukichou.rpgdrop.command.SubCommand;
import mizukichou.rpgdrop.command.TabUtil;
import mizukichou.rpgdrop.drop.DropManager;
import mizukichou.rpgdrop.drop.DropRule;
import mizukichou.rpgdrop.util.Msg;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.EntityType;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * /rdrop entity <id> add|remove|list [entity type...]
 */
public final class EntityCommand implements SubCommand {

    private static final List<String> OPS = List.of("add", "remove", "list");
    private static final List<String> ENTITY_TYPES =
            Arrays.stream(EntityType.values()).map(Enum::name).sorted().toList();

    private final DropManager dropManager;

    public EntityCommand(DropManager dropManager) {
        this.dropManager = dropManager;
    }

    @Override
    public String name() {
        return "entity";
    }

    @Override
    public String usageKey() {
        return "command.usage.entity";
    }

    @Override
    public String usage() {
        return "/rdrop entity <id> add|remove|list [entity type...]";
    }

    @Override
    public String descriptionKey() {
        return "command.desc.entity";
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
                if (rule.entities().isEmpty()) {
                    Msg.send(sender, "command.entity_list_empty", rule.id());
                    return;
                }
                Msg.send(sender, "command.entity_list", rule.id(),
                        String.join(", ", rule.entities().stream().map(Enum::name).sorted().toList()));
            }
            case "add", "remove" -> {
                if (args.length < 3) {
                    Msg.send(sender, "command.entity_usage", rule.id(), op);
                    return;
                }
                boolean add = op.equals("add");
                for (int i = 2; i < args.length; i++) {
                    try {
                        EntityType type = EntityType.valueOf(args[i].trim().toUpperCase(Locale.ROOT));
                        if (add) {
                            rule.addEntity(type);
                        } else {
                            rule.removeEntity(type);
                        }
                        Msg.send(sender, add ? "command.entity_added" : "command.entity_removed", type.name());
                    } catch (IllegalArgumentException e) {
                        Msg.send(sender, "command.entity_unknown", args[i]);
                    }
                }
                dropManager.ruleUpdated(rule);
            }
            default -> Msg.send(sender, "command.entity_unknown_op", op);
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
            return TabUtil.filter(ENTITY_TYPES, args[args.length - 1]);
        }
        return List.of();
    }
}
