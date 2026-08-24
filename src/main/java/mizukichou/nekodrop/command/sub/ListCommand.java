package mizukichou.nekodrop.command.sub;

import mizukichou.nekodrop.command.SubCommand;
import mizukichou.nekodrop.drop.DropManager;
import mizukichou.nekodrop.drop.DropRule;
import mizukichou.nekodrop.drop.RPGItemDropItem;
import mizukichou.nekodrop.drop.VanillaDropItem;
import mizukichou.nekodrop.util.Msg;
import org.bukkit.command.CommandSender;

import java.util.Collection;

public final class ListCommand implements SubCommand {

    private final DropManager dropManager;

    public ListCommand(DropManager dropManager) {
        this.dropManager = dropManager;
    }

    @Override
    public String name() {
        return "list";
    }

    @Override
    public String usage() {
        return "/ndrop list";
    }

    @Override
    public String descriptionKey() {
        return "command.desc.list";
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        Collection<DropRule> rules = dropManager.getAllRules();
        if (rules.isEmpty()) {
            Msg.send(sender, "command.list_empty");
            return;
        }
        Msg.send(sender, "command.list_header", rules.size());
        for (DropRule rule : rules) {
            Msg.sendRaw(sender, "command.list_entry",
                    rule.id(),
                    rule.entities().size(),
                    rule.worlds().size(),
                    rule.chance(),
                    rule.isEnabled() ? Msg.tr(sender, "enabled") : Msg.tr(sender, "disabled"),
                    describeItem(sender, rule));
        }
    }

    private String describeItem(CommandSender sender, DropRule rule) {
        if (rule.item() instanceof RPGItemDropItem rpgItem) {
            return "&fRPGITEM(" + rpgItem.rpgItemId() + ")";
        }
        if (rule.item() instanceof VanillaDropItem vanilla) {
            return "&fVANILLA(" + vanilla.material() + ")";
        }
        return "&c" + Msg.tr(sender, "not_set");
    }
}
