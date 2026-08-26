package mizukichou.rpgdrop.gui.page;

import mizukichou.rpgdrop.RPGDropPlugin;
import mizukichou.rpgdrop.drop.DropItem;
import mizukichou.rpgdrop.drop.DropManager;
import mizukichou.rpgdrop.drop.LotteryManager;
import mizukichou.rpgdrop.drop.NekoNYumeDropItem;
import mizukichou.rpgdrop.gui.Gui;
import mizukichou.rpgdrop.gui.GuiManager;
import mizukichou.rpgdrop.util.Items;
import mizukichou.rpgdrop.util.Msg;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * NekoNYume 物品点选页：列出全部 NekoNYume 可掉落物品（33 种），点击即选。
 * 物品清单与 NekoNYume 源码的枚举保持一致：
 *   meowdan  5 品质（COMMON/UNCOMMON/RARE/EPIC/LEGENDARY）
 *   xppill   2 档（NORMAL/ELITE）
 *   equipment 25 件（COLLAR/BELL/SCARF/NAME_TAG/YARN_BALL × 5 品质）
 *   equipbag 1 种（无参数）
 * 另提供「手动输入」按钮兜底（与命令语法 kind:value 一致）。
 */
public final class NynPickerGui extends Gui {

    private static final List<String[]> ITEMS = List.of(
            // 喵丹（5 品质）
            new String[]{"meowdan", "COMMON"},
            new String[]{"meowdan", "UNCOMMON"},
            new String[]{"meowdan", "RARE"},
            new String[]{"meowdan", "EPIC"},
            new String[]{"meowdan", "LEGENDARY"},
            // 经验丸（2 档）
            new String[]{"xppill", "NORMAL"},
            new String[]{"xppill", "ELITE"},
            // 装备（25 件）
            new String[]{"equipment", "COLLAR_COMMON"},
            new String[]{"equipment", "COLLAR_UNCOMMON"},
            new String[]{"equipment", "COLLAR_RARE"},
            new String[]{"equipment", "COLLAR_EPIC"},
            new String[]{"equipment", "COLLAR_LEGENDARY"},
            new String[]{"equipment", "BELL_COMMON"},
            new String[]{"equipment", "BELL_UNCOMMON"},
            new String[]{"equipment", "BELL_RARE"},
            new String[]{"equipment", "BELL_EPIC"},
            new String[]{"equipment", "BELL_LEGENDARY"},
            new String[]{"equipment", "SCARF_COMMON"},
            new String[]{"equipment", "SCARF_UNCOMMON"},
            new String[]{"equipment", "SCARF_RARE"},
            new String[]{"equipment", "SCARF_EPIC"},
            new String[]{"equipment", "SCARF_LEGENDARY"},
            new String[]{"equipment", "NAME_TAG_COMMON"},
            new String[]{"equipment", "NAME_TAG_UNCOMMON"},
            new String[]{"equipment", "NAME_TAG_RARE"},
            new String[]{"equipment", "NAME_TAG_EPIC"},
            new String[]{"equipment", "NAME_TAG_LEGENDARY"},
            new String[]{"equipment", "YARN_BALL_COMMON"},
            new String[]{"equipment", "YARN_BALL_UNCOMMON"},
            new String[]{"equipment", "YARN_BALL_RARE"},
            new String[]{"equipment", "YARN_BALL_EPIC"},
            new String[]{"equipment", "YARN_BALL_LEGENDARY"},
            // 装备袋
            new String[]{"equipbag", ""}
    );

    private final DropManager dropManager;
    private final LotteryManager lotteryManager;
    /** 父页面（物品选择页）的标题键与参数：返回父页时用于重建 ItemPickerGui。 */
    private final String parentTitleKey;
    private final String parentTitleArg;
    private final Consumer<DropItem> onPick;
    private final Runnable onBack;

    public NynPickerGui(RPGDropPlugin plugin, GuiManager manager, Player viewer,
                        DropManager dropManager, LotteryManager lotteryManager,
                        String titleKey, String titleArg, Consumer<DropItem> onPick, Runnable onBack) {
        super(plugin, manager, viewer);
        this.dropManager = dropManager;
        this.lotteryManager = lotteryManager;
        this.parentTitleKey = titleKey;
        this.parentTitleArg = titleArg;
        this.onPick = onPick;
        this.onBack = onBack;
    }

    @Override
    protected void fill() {
        boolean available = plugin.isNekoNYumeAvailable();

        int slot = 0;
        for (String[] pair : ITEMS) {
            NekoNYumeDropItem item = new NekoNYumeDropItem(pair[0], pair[1]);
            ItemStack icon;
            if (available) {
                icon = plugin.nekoNYumeHook().createItemStack(item).orElseGet(() ->
                        Items.icon(Material.GOLD_NUGGET, "&f" + label(item), t("gui.nyn_picker.preview_fail")));
            } else {
                icon = Items.icon(Material.GOLD_NUGGET, "&f" + label(item), t("gui.nyn_picker.unavailable"));
            }
            int pickSlot = slot++;
            button(pickSlot, icon, () -> {
                if (!plugin.isNekoNYumeAvailable()) {
                    Msg.send(viewer, "gui.item.nyn_unavailable");
                }
                onPick.accept(item);
            });
        }

        button(49, Items.icon(Material.OAK_SIGN, t("gui.nyn_picker.manual"), t("gui.nyn_picker.manual_lore")),
                () -> manager.requestTextInput(viewer, "gui.item.nyn_prompt", this::onManualInput, this::backToItemPicker));
        button(48, Items.icon(Material.BARRIER, t("close")), viewer::closeInventory);
        button(45, Items.icon(Material.ARROW, t("back")), onBack);
    }

    private static String label(NekoNYumeDropItem item) {
        return item.value().isEmpty() ? item.kind() : item.kind() + ":" + item.value();
    }

    private void onManualInput(String raw) {
        Optional<NekoNYumeDropItem> parsed = NekoNYumeDropItem.fromInput(raw);
        if (parsed.isEmpty()) {
            Msg.send(viewer, "gui.item.nyn_invalid", raw);
            backToItemPicker();
            return;
        }
        NekoNYumeDropItem nyn = parsed.get();
        if (!plugin.isNekoNYumeAvailable()) {
            Msg.send(viewer, "gui.item.nyn_unavailable");
        }
        onPick.accept(nyn);
    }

    private void backToItemPicker() {
        navigate(new ItemPickerGui(plugin, manager, viewer, dropManager, lotteryManager,
                parentTitleKey, parentTitleArg, onPick, onBack));
    }

    @Override
    protected String titleKey() {
        return "gui.nyn_picker.title";
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
