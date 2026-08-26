package mizukichou.rpgdrop.gui.page;

import mizukichou.rpgdrop.RPGDropPlugin;
import mizukichou.rpgdrop.drop.DropItem;
import mizukichou.rpgdrop.drop.DropManager;
import mizukichou.rpgdrop.drop.LotteryManager;
import mizukichou.rpgdrop.drop.RPGItemDropItem;
import mizukichou.rpgdrop.gui.Gui;
import mizukichou.rpgdrop.gui.GuiManager;
import mizukichou.rpgdrop.util.Items;
import mizukichou.rpgdrop.util.Msg;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.function.Consumer;

/**
 * RPGItem 点选页：列出 RPGItems 中已创建的全部物品（带实时预览），点击即选。
 * 另提供「手动输入 ID」按钮兜底（兼容旧方式 / RPGItems 未安装时）。
 */
public final class RpgItemPickerGui extends Gui {

    private static final int PAGE_SIZE = 45;

    private final DropManager dropManager;
    private final LotteryManager lotteryManager;
    /** 父页面（物品选择页）的标题键与参数：返回父页时用于重建 ItemPickerGui。 */
    private final String parentTitleKey;
    private final String parentTitleArg;
    private final Consumer<DropItem> onPick;
    private final Runnable onBack;
    private final int page;

    public RpgItemPickerGui(RPGDropPlugin plugin, GuiManager manager, Player viewer,
                            DropManager dropManager, LotteryManager lotteryManager,
                            String titleKey, String titleArg, Consumer<DropItem> onPick, Runnable onBack) {
        this(plugin, manager, viewer, dropManager, lotteryManager, titleKey, titleArg, onPick, onBack, 0);
    }

    private RpgItemPickerGui(RPGDropPlugin plugin, GuiManager manager, Player viewer,
                             DropManager dropManager, LotteryManager lotteryManager,
                             String titleKey, String titleArg, Consumer<DropItem> onPick, Runnable onBack, int page) {
        super(plugin, manager, viewer);
        this.dropManager = dropManager;
        this.lotteryManager = lotteryManager;
        this.parentTitleKey = titleKey;
        this.parentTitleArg = titleArg;
        this.onPick = onPick;
        this.onBack = onBack;
        this.page = page;
    }

    @Override
    protected void fill() {
        // 先过滤非法长度 ID 再分页：否则跳过会造成页面空洞、分页不饱满
        List<String> ids = plugin.getRpgItemIds().stream()
                .filter(id -> id.length() <= RPGItemDropItem.MAX_ID_LENGTH)
                .toList();

        int totalPages = Math.max(1, (ids.size() + PAGE_SIZE - 1) / PAGE_SIZE);
        int current = Math.min(page, totalPages - 1);
        int from = current * PAGE_SIZE;
        int to = Math.min(ids.size(), from + PAGE_SIZE);

        int slot = 0;
        for (int i = from; i < to; i++) {
            String id = ids.get(i);
            ItemStack icon;
            if (plugin.isRpgItemsAvailable()) {
                icon = plugin.previewRpgItem(id).orElseGet(() ->
                        Items.icon(Material.PAPER, "&f" + id, t("gui.rpgitem_picker.preview_fail")));
            } else {
                icon = Items.icon(Material.PAPER, "&f" + id, t("gui.rpgitem_picker.unavailable"));
            }
            int pickSlot = slot++;
            button(pickSlot, icon, () -> {
                RPGItemDropItem rpgItem = new RPGItemDropItem(id);
                if (!plugin.isRpgItemsAvailable()) {
                    Msg.send(viewer, "command.item_rpgitems_warning");
                } else if (!plugin.isRpgItemExist(id)) {
                    Msg.send(viewer, "command.item_rpgitem_missing", id);
                }
                onPick.accept(rpgItem);
            });
        }

        if (to == from && from == 0) {
            icon(22, Items.icon(Material.PAPER, t("gui.rpgitem_picker.empty"), t("gui.rpgitem_picker.empty_lore")));
        }

        if (current > 0) {
            button(46, Items.icon(Material.ARROW, t("gui.main.prev")), () -> navigate(new RpgItemPickerGui(
                    plugin, manager, viewer, dropManager, lotteryManager, parentTitleKey, parentTitleArg, onPick, onBack, current - 1)));
        }
        if (current < totalPages - 1) {
            button(53, Items.icon(Material.ARROW, t("gui.main.next")), () -> navigate(new RpgItemPickerGui(
                    plugin, manager, viewer, dropManager, lotteryManager, parentTitleKey, parentTitleArg, onPick, onBack, current + 1)));
        }

        button(49, Items.icon(Material.OAK_SIGN, t("gui.rpgitem_picker.manual")),
                () -> manager.requestTextInput(viewer, "gui.item.rpgitem_prompt", this::onManualInput, this::backToItemPicker));
        button(48, Items.icon(Material.BARRIER, t("close")), viewer::closeInventory);
        button(45, Items.icon(Material.ARROW, t("back")), onBack);
    }

    private void onManualInput(String raw) {
        String id = raw.trim();
        if (id.isEmpty()) {
            Msg.send(viewer, "gui.item.id_empty");
            backToItemPicker();
            return;
        }
        if (id.length() > RPGItemDropItem.MAX_ID_LENGTH) {
            Msg.send(viewer, "gui.item.id_too_long", RPGItemDropItem.MAX_ID_LENGTH);
            backToItemPicker();
            return;
        }
        RPGItemDropItem rpgItem = new RPGItemDropItem(id);
        if (!plugin.isRpgItemsAvailable()) {
            Msg.send(viewer, "command.item_rpgitems_warning");
        } else if (!plugin.isRpgItemExist(id)) {
            Msg.send(viewer, "command.item_rpgitem_missing", id);
        }
        onPick.accept(rpgItem);
    }

    private void backToItemPicker() {
        navigate(new ItemPickerGui(plugin, manager, viewer, dropManager, lotteryManager,
                parentTitleKey, parentTitleArg, onPick, onBack));
    }

    @Override
    protected String titleKey() {
        return "gui.rpgitem_picker.title";
    }

    @Override
    protected String title() {
        return t(titleKey(), parentTitleArg);
    }

    @Override
    protected int size() {
        return 54;
    }
}
