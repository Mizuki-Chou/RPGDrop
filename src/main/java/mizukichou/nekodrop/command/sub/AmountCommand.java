package mizukichou.nekodrop.command.sub;

import mizukichou.nekodrop.command.SubCommand;
import mizukichou.nekodrop.command.TabUtil;
import mizukichou.nekodrop.drop.DropManager;
import mizukichou.nekodrop.drop.DropRule;
import mizukichou.nekodrop.util.Amounts;
import mizukichou.nekodrop.util.Msg;
import org.bukkit.command.CommandSender;

import java.util.List;

/**
 * /ndrop amount <id> <min> <max>
 */
public final class AmountCommand implements SubCommand {

    private final DropManager dropManager;

    public AmountCommand(DropManager dropManager) {
        this.dropManager = dropManager;
    }

    @Override
    public String name() {
        return "amount";
    }

    @Override
    public String usage() {
        return "/ndrop amount <id> <min> <max>";
    }

    @Override
    public String descriptionKey() {
        return "command.desc.amount";
    }

    @Override
    public int minArgs() {
        return 3;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        DropRule rule = dropManager.getRule(args[0]);
        if (rule == null) {
            Msg.send(sender, "command.rule_not_found", args[0]);
            return;
        }

        int min;
        int max;
        try {
            min = Integer.parseInt(args[1]);
        } catch (NumberFormatException e) {
            Msg.send(sender, "command.amount_not_int", args[1]);
            return;
        }
        try {
            max = Integer.parseInt(args[2]);
        } catch (NumberFormatException e) {
            Msg.send(sender, "command.amount_not_int", args[2]);
            return;
        }
        if (!Amounts.isValid(min, max)) {
            Msg.send(sender, "command.amount_range");
            return;
        }

        rule.setAmount(min, max);
        dropManager.ruleUpdated(rule);
        Msg.send(sender, "command.amount_set", rule.id(), min, max);
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            return TabUtil.filter(TabUtil.ruleIds(dropManager), args[0]);
        }
        return List.of();
    }
}
