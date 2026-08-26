package mizukichou.rpgdrop.command.sub;

import mizukichou.rpgdrop.RPGDropPlugin;
import mizukichou.rpgdrop.command.SubCommand;
import mizukichou.rpgdrop.config.ConfigManager;
import mizukichou.rpgdrop.command.TabUtil;
import mizukichou.rpgdrop.drop.DropItem;
import mizukichou.rpgdrop.drop.LotteryManager;
import mizukichou.rpgdrop.drop.LotteryRule;
import mizukichou.rpgdrop.drop.NekoNYumeDropItem;
import mizukichou.rpgdrop.drop.Prize;
import mizukichou.rpgdrop.drop.RPGItemDropItem;
import mizukichou.rpgdrop.drop.VanillaDropItem;
import mizukichou.rpgdrop.util.Chance;
import mizukichou.rpgdrop.util.Materials;
import mizukichou.rpgdrop.util.Msg;
import mizukichou.rpgdrop.util.RuleIds;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Arrays;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * 抽奖规则命令（Release 2 新增）：
 * /rdrop lottery create|delete|list|info <id>
 * /rdrop lottery trigger <id> vanilla <材料> | rpgitem <ID>
 * /rdrop lottery prize <id> add <vanilla <材料>|rpgitem <ID>> <概率> | remove <序号> | list
 */
public final class LotteryCommand implements SubCommand {

    /** NekoNYume 触发物/奖品补全前缀（kind:value 格式）。 */
    private static final List<String> NYN_KINDS = List.of("meowdan:", "xppill:", "equipment:", "equipbag");

    private static final List<String> MATERIALS =
            Arrays.stream(Material.values()).map(m -> m.getKey().getKey()).sorted().toList();

    private final RPGDropPlugin plugin;
    private final LotteryManager lotteryManager;

    public LotteryCommand(RPGDropPlugin plugin, LotteryManager lotteryManager) {
        this.plugin = plugin;
        this.lotteryManager = lotteryManager;
    }

    @Override
    public String name() {
        return "lottery";
    }

    @Override
    public String usage() {
        return "/rdrop lottery <create|delete|list|info|trigger|prize> ...";
    }

    @Override
    public String descriptionKey() {
        return "command.desc.lottery";
    }

    @Override
    public int minArgs() {
        return 1;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        String op = args[0].toLowerCase(Locale.ROOT);
        switch (op) {
            case "create" -> doCreate(sender, args);
            case "delete" -> doDelete(sender, args);
            case "list" -> doList(sender);
            case "info" -> doInfo(sender, args);
            case "trigger" -> doTrigger(sender, args);
            case "prize" -> doPrize(sender, args);
            default -> Msg.send(sender, "command.entity_unknown_op", args[0]);
        }
    }

    private void doCreate(CommandSender sender, String[] args) {
        if (args.length < 2) {
            Msg.send(sender, "command.usage_hint", usage());
            return;
        }
        String id = args[1];
        if (!RuleIds.isValid(id)) {
            Msg.send(sender, "command.invalid_id");
            return;
        }
        if (lotteryManager.isLimitReached()) {
            Msg.send(sender, "command.lottery_limit", ConfigManager.MAX_LOTTERY_RULES);
            return;
        }
        if (!lotteryManager.createRule(id)) {
            Msg.send(sender, "command.rule_already_exists", id);
            return;
        }
        Msg.send(sender, "command.lottery_created", id);
    }

    private void doDelete(CommandSender sender, String[] args) {
        LotteryRule rule = require(sender, args);
        if (rule == null) {
            return;
        }
        lotteryManager.deleteRule(rule.id());
        Msg.send(sender, "command.lottery_deleted", rule.id());
    }

    private void doList(CommandSender sender) {
        var rules = lotteryManager.getAllRules();
        Msg.send(sender, "command.lottery_list_header", rules.size());
        for (LotteryRule rule : rules) {
            String trigger = rule.trigger() == null
                    ? Msg.tr(sender, "not_set")
                    : "&f" + lotteryManager.describe(rule.trigger());
            Msg.sendRaw(sender, "command.lottery_list_entry", rule.id(),
                    trigger,
                    rule.prizes().size(),
                    (rule.isEnabled() ? "&a" : "&c") + Msg.tr(sender, rule.isEnabled() ? "enabled" : "disabled"));
        }
    }

    private void doInfo(CommandSender sender, String[] args) {
        LotteryRule rule = require(sender, args);
        if (rule == null) {
            return;
        }
        Msg.sendRaw(sender, "command.info_enabled", rule.isEnabled() ? Msg.tr(sender, "yes") : Msg.tr(sender, "no"));
        Msg.sendRaw(sender, "gui.lottery_list.rule_lore_trigger", rule.trigger() == null
                ? Msg.tr(sender, "not_set") : "&f" + lotteryManager.describe(rule.trigger()));
        Msg.sendRaw(sender, "gui.lottery_list.rule_lore_prizes", rule.prizes().size());
        for (int i = 0; i < rule.prizes().size(); i++) {
            Prize prize = rule.prizes().get(i);
            Msg.sendRaw(sender, "command.lottery_prize_line", i + 1,
                    "&f" + lotteryManager.describe(prize.item()), prize.weight());
        }
    }

    private void doTrigger(CommandSender sender, String[] args) {
        LotteryRule rule = require(sender, args);
        if (rule == null) {
            return;
        }
        if (args.length < 3) {
            Msg.send(sender, "command.lottery_trigger_usage", rule.id());
            return;
        }
        Optional<DropItem> parsed = parseItem(args[2], args.length > 3 ? args[3] : null);
        if (parsed.isEmpty()) {
            Msg.send(sender, "command.lottery_trigger_usage", rule.id());
            return;
        }
        DropItem trigger = parsed.get();
        Optional<LotteryRule> conflict = lotteryManager.findTriggerConflict(rule, trigger);
        if (conflict.isPresent()) {
            Msg.send(sender, "command.lottery_trigger_conflict", "", conflict.get().id());
            return;
        }
        rule.setTrigger(trigger);
        lotteryManager.ruleUpdated(rule);
        Msg.send(sender, "command.lottery_trigger_set", rule.id(), lotteryManager.describe(trigger));
        if (trigger instanceof RPGItemDropItem rpgItem) {
            plugin.notifyRpgItemMissing(sender, rpgItem.rpgItemId());
        } else if (trigger instanceof NekoNYumeDropItem && !plugin.isNekoNYumeAvailable()) {
            Msg.send(sender, "command.item_nyn_unavailable");
        }
    }

    private void doPrize(CommandSender sender, String[] args) {
        LotteryRule rule = require(sender, args);
        if (rule == null) {
            return;
        }
        if (args.length < 3) {
            Msg.send(sender, "command.lottery_prize_usage", rule.id());
            return;
        }
        switch (args[2].toLowerCase(Locale.ROOT)) {
            case "list" -> {
                if (rule.prizes().isEmpty()) {
                    Msg.send(sender, "command.lottery_prize_empty");
                    return;
                }
                Msg.send(sender, "command.lottery_prize_list_header", rule.id(), rule.prizes().size());
                for (int i = 0; i < rule.prizes().size(); i++) {
                    Prize prize = rule.prizes().get(i);
                    Msg.sendRaw(sender, "command.lottery_prize_line", i + 1,
                            "&f" + lotteryManager.describe(prize.item()), prize.weight());
                }
                Msg.sendRaw(sender, "command.lottery_prize_total", lotteryManager.totalWeight(rule));
            }
            case "add" -> {
                if (args.length < 6) {
                    Msg.send(sender, "command.lottery_prize_usage", rule.id());
                    return;
                }
                Optional<DropItem> parsed = parseItem(args[3], args[4]);
                if (parsed.isEmpty()) {
                    Msg.send(sender, "command.lottery_prize_usage", rule.id());
                    return;
                }
                double weight;
                try {
                    weight = Double.parseDouble(args[5]);
                } catch (NumberFormatException e) {
                    Msg.send(sender, "command.chance_not_number", args[5]);
                    return;
                }
                if (!Chance.isValid(weight)) {
                    Msg.send(sender, "command.chance_range");
                    return;
                }
                DropItem item = parsed.get();
                double currentTotal = lotteryManager.totalWeight(rule);
                if (currentTotal + weight > 100.0 + 1e-9) {
                    Msg.send(sender, "command.lottery_weight_exceeded", currentTotal, 100.0 - currentTotal);
                    return;
                }
                if (!rule.addPrize(new Prize(item, weight))) {
                    Msg.send(sender, "command.lottery_prize_limit", LotteryRule.MAX_PRIZES_PER_RULE);
                    return;
                }
                lotteryManager.ruleUpdated(rule);
                Msg.send(sender, "command.lottery_prize_added", lotteryManager.describe(item), weight);
                if (item instanceof RPGItemDropItem rpgItem) {
                    plugin.notifyRpgItemMissing(sender, rpgItem.rpgItemId());
                }
            }
            case "remove" -> {
                if (args.length < 4) {
                    Msg.send(sender, "command.lottery_prize_usage", rule.id());
                    return;
                }
                int index;
                try {
                    index = Integer.parseInt(args[3]);
                } catch (NumberFormatException e) {
                    Msg.send(sender, "command.lottery_prize_invalid", args[3]);
                    return;
                }
                if (index < 1 || index > rule.prizes().size()) {
                    Msg.send(sender, "command.lottery_prize_invalid", index);
                    return;
                }
                rule.removePrize(index - 1);
                lotteryManager.ruleUpdated(rule);
                Msg.send(sender, "command.lottery_prize_removed", index);
            }
            default -> Msg.send(sender, "command.lottery_prize_usage", rule.id());
        }
    }

    private LotteryRule require(CommandSender sender, String[] args) {
        if (args.length < 2) {
            Msg.send(sender, "command.usage_hint", usage());
            return null;
        }
        LotteryRule rule = lotteryManager.getRule(args[1]);
        if (rule == null) {
            Msg.send(sender, "command.lottery_not_found", args[1]);
            return null;
        }
        return rule;
    }

    /** 解析 vanilla <材料> 或 rpgitem <ID>。 */
    private Optional<DropItem> parseItem(String kind, String value) {
        if (value == null) {
            return Optional.empty();
        }
        switch (kind.toLowerCase(Locale.ROOT)) {
            case "vanilla" -> {
                var material = Materials.parse(value);
                return material.map(VanillaDropItem::new);
            }
            case "rpgitem" -> {
                if (value.length() > RPGItemDropItem.MAX_ID_LENGTH) {
                    return Optional.empty();
                }
                return Optional.of(new RPGItemDropItem(value));
            }
            case "nyn" -> {
                Optional<NekoNYumeDropItem> nyn = NekoNYumeDropItem.fromInput(value);
                return nyn.map(i -> (DropItem) i);
            }
            default -> {
                return Optional.empty();
            }
        }
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            return TabUtil.filter(List.of("create", "delete", "list", "info", "trigger", "prize"), args[0]);
        }
        if (args.length == 2) {
            return TabUtil.filter(lotteryManager.getAllRules().stream().map(LotteryRule::id).toList(), args[1]);
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("trigger")) {
            return TabUtil.filter(TabUtil.itemKinds(plugin), args[2]);
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("prize")) {
            return TabUtil.filter(List.of("add", "remove", "list"), args[2]);
        }
        if (args.length >= 4 && args[0].equalsIgnoreCase("prize") && args[2].equalsIgnoreCase("add")) {
            if (args.length == 4) {
                return TabUtil.filter(TabUtil.itemKinds(plugin), args[3]);
            }
            if (args.length == 5) {
                if (args[3].equalsIgnoreCase("rpgitem")) {
                    return TabUtil.filter(plugin.getRpgItemIds(), args[4]);
                }
                if (args[3].equalsIgnoreCase("nyn")) {
                    return TabUtil.filter(NYN_KINDS, args[4]);
                }
                return TabUtil.filter(MATERIALS, args[4]);
            }
        }
        if (args.length == 4 && args[0].equalsIgnoreCase("trigger")) {
            if (args[2].equalsIgnoreCase("rpgitem")) {
                return TabUtil.filter(plugin.getRpgItemIds(), args[3]);
            }
            if (args[2].equalsIgnoreCase("nyn")) {
                return TabUtil.filter(NYN_KINDS, args[3]);
            }
            return TabUtil.filter(MATERIALS, args[3]);
        }
        return List.of();
    }
}
