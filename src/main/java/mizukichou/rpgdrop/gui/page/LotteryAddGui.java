package mizukichou.rpgdrop.gui.page;

import mizukichou.rpgdrop.RPGDropPlugin;
import mizukichou.rpgdrop.drop.DropItem;
import mizukichou.rpgdrop.drop.DropManager;
import mizukichou.rpgdrop.drop.LotteryManager;
import mizukichou.rpgdrop.drop.LotteryRule;
import mizukichou.rpgdrop.drop.Prize;
import mizukichou.rpgdrop.drop.RPGItemDropItem;
import mizukichou.rpgdrop.gui.Gui;
import mizukichou.rpgdrop.gui.GuiManager;
import mizukichou.rpgdrop.util.Items;
import mizukichou.rpgdrop.util.Msg;
import org.bukkit.Material;
import org.bukkit.entity.Player;

/**
 * 添加奖品向导：选物品 -> 设概率 -> 完成。状态通过构造参数在页面间传递。
 */
public final class LotteryAddGui extends Gui {

    private final DropManager dropManager;
    private final LotteryManager lotteryManager;
    private final LotteryRule rule;
    private final DropItem pickedItem;
    private final Double weight;
    /** 完成/取消后返回奖品列表时应停留在第几页（防止"第 2/3 页添加后弹回第 1 页"）。 */
    private final int returnPage;

    public LotteryAddGui(RPGDropPlugin plugin, GuiManager manager, Player viewer,
                         DropManager dropManager, LotteryManager lotteryManager, LotteryRule rule,
                         DropItem pickedItem, Double weight, int returnPage) {
        super(plugin, manager, viewer);
        this.dropManager = dropManager;
        this.lotteryManager = lotteryManager;
        this.rule = rule;
        this.pickedItem = pickedItem;
        this.weight = weight;
        this.returnPage = returnPage;
    }

    @Override
    protected void fill() {
        if (lotteryManager.getRule(rule.id()) == null) {
            Msg.send(viewer, "gui.lottery_editor.rule_deleted", rule.id());
            close();
            return;
        }

        button(11, Items.icon(Material.CHEST, t("gui.lottery_add_page.pick"),
                        pickedItem == null ? t("gui.lottery_add_page.picked_unset")
                                : t("gui.lottery_add_page.picked", "&f" + lotteryManager.describe(pickedItem)),
                        t("gui.lottery_add_page.pick_hint")),
                () -> navigate(new ItemPickerGui(plugin, manager, viewer, dropManager, lotteryManager,
                        "gui.item.title", rule.id(),
                        item -> navigate(new LotteryAddGui(plugin, manager, viewer, dropManager, lotteryManager, rule, item, weight, returnPage)),
                        () -> navigate(new LotteryAddGui(plugin, manager, viewer, dropManager, lotteryManager, rule, pickedItem, weight, returnPage)))));

        button(13, Items.icon(Material.GOLD_NUGGET, t("gui.lottery_add_page.weight"),
                        weight == null ? t("gui.lottery_add_page.weight_unset")
                                : t("gui.lottery_add_page.weight_current", weight),
                        t("gui.lottery_add_page.weight_hint")),
                () -> navigate(new LotteryChanceGui(plugin, manager, viewer,
                        value -> navigate(new LotteryAddGui(plugin, manager, viewer, dropManager, lotteryManager, rule, pickedItem, value, returnPage)),
                        () -> navigate(new LotteryAddGui(plugin, manager, viewer, dropManager, lotteryManager, rule, pickedItem, weight, returnPage)))));

        button(15, Items.glow(Items.icon(Material.LIME_WOOL, t("gui.lottery_add_page.confirm"),
                        t("gui.lottery_add_page.confirm_hint"))),
                () -> {
                    if (pickedItem == null) {
                        Msg.send(viewer, "gui.lottery_add_page.picked_unset");
                        render(); // 刷新重新挂载按钮，防止"确认"永久失效
                        return;
                    }
                    if (weight == null) {
                        Msg.send(viewer, "gui.lottery_add_page.weight_unset");
                        render();
                        return;
                    }
                    double currentTotal = lotteryManager.totalWeight(rule);
                    if (currentTotal + weight > 100.0 + 1e-9) {
                        Msg.send(viewer, "command.lottery_weight_exceeded", currentTotal, 100.0 - currentTotal);
                        render();
                        return;
                    }
                    if (!rule.addPrize(new Prize(pickedItem, weight))) {
                        Msg.send(viewer, "command.lottery_prize_limit", LotteryRule.MAX_PRIZES_PER_RULE);
                        navigate(new LotteryPrizesGui(plugin, manager, viewer, dropManager, lotteryManager, rule, returnPage));
                        return;
                    }
                    lotteryManager.ruleUpdated(rule);
                    Msg.send(viewer, "command.lottery_prize_added", lotteryManager.describe(pickedItem), weight);
                    navigate(new LotteryPrizesGui(plugin, manager, viewer, dropManager, lotteryManager, rule, returnPage));
                });

        button(16, Items.icon(Material.RED_WOOL, t("gui.lottery_add_page.cancel")),
                () -> {
                    Msg.send(viewer, "gui.lottery_add_page.cancelled");
                    navigate(new LotteryPrizesGui(plugin, manager, viewer, dropManager, lotteryManager, rule, returnPage));
                });
    }

    @Override
    protected String titleKey() {
        return "gui.lottery_add_page.title";
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
