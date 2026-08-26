package mizukichou.rpgdrop.command.sub;

import mizukichou.rpgdrop.command.SubCommand;
import mizukichou.rpgdrop.command.TabUtil;
import mizukichou.rpgdrop.drop.DropManager;
import mizukichou.rpgdrop.drop.DropRule;
import mizukichou.rpgdrop.drop.NekoNYumeDropItem;
import mizukichou.rpgdrop.drop.RPGItemDropItem;
import mizukichou.rpgdrop.drop.VanillaDropItem;
import mizukichou.rpgdrop.util.Msg;
import org.bukkit.command.CommandSender;

import java.util.List;

public final class InfoCommand implements SubCommand {

    private final DropManager dropManager;

    public InfoCommand(DropManager dropManager) {
        this.dropManager = dropManager;
    }

    @Override
    public String name() {
        return "info";
    }

    @Override
    public String usage() {
        return "/rdrop info <id>";
    }

    @Override
    public String descriptionKey() {
        return "command.desc.info";
    }

    @Override
    public int minArgs() {
        return 1;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        DropRule rule = dropManager.getRule(args[0]);
        if (rule == null) {
            Msg.send(sender, "command.rule_not_found", args[0]);
            return;
        }
        Msg.sendRaw(sender, "command.help_header");
        Msg.sendRaw(sender, "command.info_enabled", rule.isEnabled() ? Msg.tr(sender, "yes") : Msg.tr(sender, "no"));
        Msg.sendRaw(sender, "command.info_entities",
                String.join(", ", rule.entities().stream().map(Enum::name).sorted().toList()));
        Msg.sendRaw(sender, "command.info_worlds", rule.worldMode().name(),
                String.join(", ", rule.worlds().stream().sorted().toList()));
        if (rule.item() instanceof RPGItemDropItem rpgItem) {
            Msg.sendRaw(sender, "command.info_item_rpgitem", rpgItem.rpgItemId());
        } else if (rule.item() instanceof VanillaDropItem vanilla) {
            Msg.sendRaw(sender, "command.info_item_vanilla", vanilla.material());
        } else if (rule.item() instanceof NekoNYumeDropItem nyn) {
            Msg.sendRaw(sender, "command.info_item_nyn", nyn.kind() + ":" + nyn.value());
        } else {
            Msg.sendRaw(sender, "command.info_item_none");
        }
        Msg.sendRaw(sender, "command.info_chance", rule.chance());
        Msg.sendRaw(sender, "command.info_amount", rule.minAmount(), rule.maxAmount());
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            return TabUtil.filter(TabUtil.ruleIds(dropManager), args[0]);
        }
        return List.of();
    }
}
