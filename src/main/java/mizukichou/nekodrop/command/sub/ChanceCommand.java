package mizukichou.nekodrop.command.sub;

import mizukichou.nekodrop.command.SubCommand;
import mizukichou.nekodrop.command.TabUtil;
import mizukichou.nekodrop.util.Chance;
import mizukichou.nekodrop.drop.DropManager;
import mizukichou.nekodrop.drop.DropRule;
import mizukichou.nekodrop.util.Msg;
import org.bukkit.command.CommandSender;

import java.util.List;

/**
 * /ndrop chance <id> <percent>  —— 0.01 = 0.01%
 */
public final class ChanceCommand implements SubCommand {

    private final DropManager dropManager;

    public ChanceCommand(DropManager dropManager) {
        this.dropManager = dropManager;
    }

    @Override
    public String name() {
        return "chance";
    }

    @Override
    public String usageKey() {
        return "command.usage.chance";
    }

    @Override
    public String usage() {
        return "/ndrop chance <id> <percent>";
    }

    @Override
    public String descriptionKey() {
        return "command.desc.chance";
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

        double value;
        try {
            value = Double.parseDouble(args[1]);
        } catch (NumberFormatException e) {
            Msg.send(sender, "command.chance_not_number", args[1]);
            return;
        }
        if (!Chance.isValid(value)) {
            Msg.send(sender, "command.chance_range");
            return;
        }

        rule.setChance(value);
        dropManager.ruleUpdated(rule);
        Msg.send(sender, "command.chance_set", rule.id(), value);
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            return TabUtil.filter(TabUtil.ruleIds(dropManager), args[0]);
        }
        if (args.length == 2) {
            return TabUtil.filter(List.of("100", "10", "1", "0.1", "0.01", "0"), args[1]);
        }
        return List.of();
    }
}
