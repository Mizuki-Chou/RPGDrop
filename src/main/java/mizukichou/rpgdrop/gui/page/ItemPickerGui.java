package mizukichou.rpgdrop.gui.page;

import mizukichou.rpgdrop.RPGDropPlugin;
import mizukichou.rpgdrop.drop.DropItem;
import mizukichou.rpgdrop.drop.DropManager;
import mizukichou.rpgdrop.drop.LotteryManager;
import mizukichou.rpgdrop.drop.VanillaDropItem;
import mizukichou.rpgdrop.gui.Gui;
import mizukichou.rpgdrop.gui.GuiManager;
import mizukichou.rpgdrop.util.Items;
import mizukichou.rpgdrop.util.Msg;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.function.Consumer;

/**
 * 物品类型选择页（通用版，Release 2 起同时服务掉落规则与抽奖规则）：
 * 原版物品 / RPGItem / 清除。选择结果通过回调返回。
 *
 * 硬性原则：只保存 Material / RPGItem ID，绝不保存 ItemStack 本体。
 */
public final class ItemPickerGui extends Gui {

    private final DropManager dropManager;
    private final LotteryManager lotteryManager;
    private final String titleKey;
    private final String titleArg;
    private final Consumer<DropItem> onPick;
    private final Runnable onBack;

    public ItemPickerGui(RPGDropPlugin plugin, GuiManager manager, Player viewer,
                         DropManager dropManager, LotteryManager lotteryManager,
                         String titleKey, String titleArg, Consumer<DropItem> onPick, Runnable onBack) {
        super(plugin, manager, viewer);
        this.dropManager = dropManager;
        this.lotteryManager = lotteryManager;
        this.titleKey = titleKey;
        this.titleArg = titleArg;
        this.onPick = onPick;
        this.onBack = onBack;
    }

    @Override
    protected void fill() {
        button(11, Items.icon(Material.STONE, t("gui.item.vanilla"),
                        t("gui.item.vanilla_lore_1"),
                        t("gui.item.vanilla_lore_2"),
                        "",
                        t("gui.item.click_choose")),
                () -> navigate(new MaterialPickerGui(plugin, manager, viewer, titleArg,
                        material -> onPick.accept(new VanillaDropItem(material)),
                        () -> navigate(new ItemPickerGui(plugin, manager, viewer, dropManager, lotteryManager,
                                titleKey, titleArg, onPick, onBack)))));

        button(13, Items.icon(Material.ENDER_EYE, t("gui.item.rpgitem"),
                        t("gui.item.rpgitem_lore_1"),
                        t("gui.item.rpgitem_lore_2"),
                        plugin.isRpgItemsAvailable() ? t("gui.item.rpg_available") : t("gui.item.rpg_unavailable"),
                        "",
                        t("gui.item.click_set")),
                () -> navigate(new RpgItemPickerGui(plugin, manager, viewer, dropManager, lotteryManager,
                        titleKey, titleArg, onPick, onBack)));

        button(14, Items.icon(Material.GOLD_NUGGET, t("gui.item.nyn"),
                        t("gui.item.nyn_lore_1"),
                        t("gui.item.nyn_lore_2"),
                        plugin.isNekoNYumeAvailable() ? "" : t("gui.item.nyn_unavailable"),
                        "",
                        t("gui.item.click_set")),
                () -> navigate(new NynPickerGui(plugin, manager, viewer, dropManager, lotteryManager,
                        titleKey, titleArg, onPick, onBack)));

        button(15, Items.icon(Material.BARRIER, t("gui.item.clear"), t("gui.item.clear_lore")),
                () -> onPick.accept(null));

        button(17, Items.icon(Material.ARROW, t("back")), onBack::run);
    }



    @Override
    protected String titleKey() {
        return titleKey;
    }

    @Override
    protected String title() {
        return t(titleKey, titleArg);
    }

    @Override
    protected int size() {
        return 27;
    }
}
