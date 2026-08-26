package mizukichou.rpgdrop.gui.page;

import mizukichou.rpgdrop.RPGDropPlugin;
import mizukichou.rpgdrop.drop.DropManager;
import mizukichou.rpgdrop.drop.LotteryManager;
import mizukichou.rpgdrop.drop.LotteryRule;
import mizukichou.rpgdrop.gui.Gui;
import mizukichou.rpgdrop.gui.GuiManager;
import mizukichou.rpgdrop.util.Items;
import mizukichou.rpgdrop.util.Msg;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import java.util.Optional;

/**
 * 抽奖规则编辑器：触发物 / 奖品 / 启用开关 / 删除。
 */
public final class LotteryEditorGui extends Gui {

    private final DropManager dropManager;
    private final LotteryManager lotteryManager;
    private final LotteryRule rule;

    public LotteryEditorGui(RPGDropPlugin plugin, GuiManager manager, Player viewer,
                            DropManager dropManager, LotteryManager lotteryManager, LotteryRule rule) {
        super(plugin, manager, viewer);
        this.dropManager = dropManager;
        this.lotteryManager = lotteryManager;
        this.rule = rule;
    }

    @Override
    protected void fill() {
        if (lotteryManager.getRule(rule.id()) == null) {
            Msg.send(viewer, "gui.lottery_editor.rule_deleted", rule.id());
            close();
            return;
        }

        icon(0, rule.isEnabled()
                ? Items.glow(Items.icon(Material.END_CRYSTAL, "&d" + rule.id(), t("gui.lottery_editor.info_enabled", t("yes")), t("gui.lottery_editor.info_hint")))
                : Items.icon(Material.ECHO_SHARD, "&7" + rule.id(), t("gui.lottery_editor.info_enabled", t("no")), t("gui.lottery_editor.info_hint")));

        button(10, Items.icon(Material.TARGET, t("gui.lottery_editor.trigger"),
                        rule.trigger() == null ? t("gui.lottery_editor.trigger_unset")
                                : t("gui.lottery_editor.trigger_current", "&f" + lotteryManager.describe(rule.trigger())),
                        t("gui.lottery_editor.trigger_hint")),
                () -> navigate(new ItemPickerGui(plugin, manager, viewer, dropManager, lotteryManager,
                        "gui.lottery_trigger_page.title", rule.id(),
                        item -> {
                            Optional<LotteryRule> conflictOpt = item == null
                                    ? Optional.empty()
                                    : lotteryManager.findTriggerConflict(rule, item);
                            if (conflictOpt.isPresent()) {
                                Msg.send(viewer, "command.lottery_trigger_conflict", "", conflictOpt.get().id());
                            } else {
                                rule.setTrigger(item);
                                lotteryManager.ruleUpdated(rule);
                                if (item != null) {
                                    Msg.send(viewer, "command.lottery_trigger_set", rule.id(), lotteryManager.describe(item));
                                    if (item instanceof mizukichou.rpgdrop.drop.RPGItemDropItem rpgItem) {
                                        plugin.notifyRpgItemMissing(viewer, rpgItem.rpgItemId());
                                    }
                                }
                            }
                            navigate(new LotteryEditorGui(plugin, manager, viewer, dropManager, lotteryManager, rule));
                        },
                        () -> navigate(new LotteryEditorGui(plugin, manager, viewer, dropManager, lotteryManager, rule)))));

        button(12, Items.icon(Material.CHEST, t("gui.lottery_editor.prizes"),
                        t("gui.lottery_editor.prizes_current", rule.prizes().size()),
                        lotteryManager.isReady(rule)
                                ? t("gui.lottery_editor.prizes_total_ok", lotteryManager.totalWeight(rule))
                                : t("gui.lottery_editor.prizes_total_bad", lotteryManager.totalWeight(rule), 100.0 - lotteryManager.totalWeight(rule)),
                        t("gui.lottery_editor.prizes_hint")),
                () -> navigate(new LotteryPrizesGui(plugin, manager, viewer, dropManager, lotteryManager, rule)));

        button(14, Items.icon(rule.isEnabled() ? Material.LIME_DYE : Material.RED_DYE,
                        t("gui.lottery_editor.toggle", ""),
                        t("gui.lottery_editor.toggle_current", rule.isEnabled() ? t("yes") : t("no")),
                        t("gui.lottery_editor.toggle_click")),
                () -> {
                    rule.setEnabled(!rule.isEnabled());
                    lotteryManager.ruleUpdated(rule);
                    render();
                });

        button(15, Items.icon(Material.BARRIER, t("gui.editor.delete"), t("gui.lottery_editor.delete_confirm", rule.id())),
                () -> navigate(new ConfirmGui(plugin, manager, viewer, "gui.lottery_editor.delete_confirm", rule.id(),
                        () -> {
                            lotteryManager.deleteRule(rule.id());
                            Msg.send(viewer, "command.lottery_deleted", rule.id());
                            navigate(new LotteryListGui(plugin, manager, viewer, dropManager, lotteryManager));
                        },
                        () -> navigate(new LotteryEditorGui(plugin, manager, viewer, dropManager, lotteryManager, rule)))));

        button(16, Items.icon(Material.ARROW, t("back")),
                () -> navigate(new LotteryListGui(plugin, manager, viewer, dropManager, lotteryManager)));
    }

    @Override
    protected String titleKey() {
        return "gui.lottery_editor.title";
    }

    @Override
    protected String title() {
        return t(titleKey(), rule.id());
    }

    @Override
    protected int size() {
        return 27;
    }
}
