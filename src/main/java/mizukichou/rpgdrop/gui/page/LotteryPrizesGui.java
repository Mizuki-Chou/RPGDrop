package mizukichou.rpgdrop.gui.page;

import mizukichou.rpgdrop.RPGDropPlugin;
import mizukichou.rpgdrop.drop.DropManager;
import mizukichou.rpgdrop.drop.LotteryManager;
import mizukichou.rpgdrop.drop.LotteryRule;
import mizukichou.rpgdrop.drop.Prize;
import mizukichou.rpgdrop.drop.NekoNYumeDropItem;
import mizukichou.rpgdrop.drop.RPGItemDropItem;
import mizukichou.rpgdrop.drop.VanillaDropItem;
import mizukichou.rpgdrop.gui.Gui;
import mizukichou.rpgdrop.gui.GuiManager;
import mizukichou.rpgdrop.util.Items;
import mizukichou.rpgdrop.util.Msg;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Optional;

/**
 * 奖品列表：查看 / 移除 / 添加入口。
 */
public final class LotteryPrizesGui extends Gui {

    private static final int ITEMS_PER_PAGE = 45;

    private final DropManager dropManager;
    private final LotteryManager lotteryManager;
    private final LotteryRule rule;
    private int page;

    public LotteryPrizesGui(RPGDropPlugin plugin, GuiManager manager, Player viewer,
                            DropManager dropManager, LotteryManager lotteryManager, LotteryRule rule) {
        this(plugin, manager, viewer, dropManager, lotteryManager, rule, 0);
    }

    public LotteryPrizesGui(RPGDropPlugin plugin, GuiManager manager, Player viewer,
                            DropManager dropManager, LotteryManager lotteryManager, LotteryRule rule,
                            int page) {
        super(plugin, manager, viewer);
        this.dropManager = dropManager;
        this.lotteryManager = lotteryManager;
        this.rule = rule;
        this.page = page;
    }

    @Override
    protected void fill() {
        if (lotteryManager.getRule(rule.id()) == null) {
            Msg.send(viewer, "gui.lottery_editor.rule_deleted", rule.id());
            close();
            return;
        }

        List<Prize> prizes = List.copyOf(rule.prizes());
        int totalPages = Math.max(1, (int) Math.ceil(prizes.size() / (double) ITEMS_PER_PAGE));
        if (page >= totalPages) {
            page = totalPages - 1;
        }
        int from = page * ITEMS_PER_PAGE;
        int to = Math.min(prizes.size(), from + ITEMS_PER_PAGE);

        for (int i = from; i < to; i++) {
            final int index = i;
            Prize prize = prizes.get(i);
            int slot = i - from;
            ItemStack icon = prizeIcon(prize);
            button(slot, icon,
                    () -> navigate(new ConfirmGui(plugin, manager, viewer, "gui.lottery_prizes_page.remove_confirm", prizeLabel(prize),
                            () -> {
                                // TOCTOU 防护：确认期间奖品列表若被其他入口修改（命令/另一管理员），
                                // 按下标删除会误删"现在停在该位置"的另一个奖品——先按对象身份校验。
                                List<Prize> current = rule.prizes();
                                if (index < 0 || index >= current.size() || current.get(index) != prize) {
                                    Msg.send(viewer, "command.lottery_prize_invalid", index + 1);
                                    navigate(new LotteryPrizesGui(plugin, manager, viewer, dropManager, lotteryManager, rule, page));
                                    return;
                                }
                                rule.removePrize(index);
                                lotteryManager.ruleUpdated(rule);
                                Msg.send(viewer, "command.lottery_prize_removed", index + 1);
                                navigate(new LotteryPrizesGui(plugin, manager, viewer, dropManager, lotteryManager, rule, page));
                            },
                            () -> navigate(new LotteryPrizesGui(plugin, manager, viewer, dropManager, lotteryManager, rule)))));
        }

        if (prizes.isEmpty()) {
            icon(22, Items.icon(Material.OAK_SIGN, t("gui.lottery_prizes_page.empty"), ""));
        }

        button(45, Items.icon(Material.ARROW, t("back")),
                () -> navigate(new LotteryEditorGui(plugin, manager, viewer, dropManager, lotteryManager, rule)));
        if (page > 0) {
            button(46, Items.icon(Material.ARROW, t("gui.main.prev")), () -> {
                page--;
                render();
            });
        }
        if (page + 1 < totalPages) {
            button(53, Items.icon(Material.ARROW, t("gui.main.next")), () -> {
                page++;
                render();
            });
        }
        button(49, Items.glow(Items.icon(Material.EMERALD, t("gui.lottery_prizes_page.add"))),
                () -> navigate(new LotteryAddGui(plugin, manager, viewer, dropManager, lotteryManager, rule, null, null, page)));
        button(48, Items.icon(Material.BARRIER, t("close")), this::close);

        double total = lotteryManager.totalWeight(rule);
        icon(50, Items.icon(Material.PAPER, "&7" + t("gui.lottery_editor.prizes_total_label"),
                Math.abs(total - 100.0) < 1e-9
                        ? t("gui.lottery_editor.prizes_total_ok", total)
                        : t("gui.lottery_editor.prizes_total_bad", total, 100.0 - total)));
    }

    /** 奖品图标：真实物品预览（RPGItem 实时生成），附概率与移除提示。 */
    private ItemStack prizeIcon(Prize prize) {
        ItemStack icon;
        if (prize.item() instanceof VanillaDropItem vanilla) {
            icon = new ItemStack(vanilla.material());
        } else if (prize.item() instanceof RPGItemDropItem rpgItem) {
            Optional<ItemStack> preview = plugin.previewRpgItem(rpgItem.rpgItemId());
            icon = preview.orElseGet(() -> Items.icon(Material.PAPER, "&fRPGITEM(" + rpgItem.rpgItemId() + ")"));
        } else if (prize.item() instanceof NekoNYumeDropItem nyn) {
            icon = plugin.nekoNYumeHook().createItemStack(nyn)
                    .orElseGet(() -> Items.icon(Material.GOLD_NUGGET, "&f" + nyn.kind() + ":" + nyn.value(),
                            t("gui.nyn_picker.preview_fail")));
        } else {
            icon = Items.icon(Material.PAPER, "&f?");
        }
        return Items.withLore(icon,
                "&f" + lotteryManager.describe(prize.item()),
                t("gui.lottery_prizes_page.line_lore", prize.weight()),
                "",
                t("gui.lottery_prizes_page.remove_hint"));
    }

    private String prizeLabel(Prize prize) {
        return lotteryManager.describe(prize.item());
    }

    @Override
    protected String titleKey() {
        return "gui.lottery_prizes_page.title";
    }

    @Override
    protected String title() {
        return t(titleKey(), rule.id());
    }

    @Override
    protected int size() {
        return 54;
    }
}
