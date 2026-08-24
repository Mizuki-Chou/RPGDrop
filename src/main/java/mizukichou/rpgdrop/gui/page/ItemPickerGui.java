package mizukichou.rpgdrop.gui.page;

import mizukichou.rpgdrop.RPGDropPlugin;
import mizukichou.rpgdrop.drop.DropManager;
import mizukichou.rpgdrop.drop.DropRule;
import mizukichou.rpgdrop.drop.RPGItemDropItem;
import mizukichou.rpgdrop.drop.VanillaDropItem;
import mizukichou.rpgdrop.gui.Gui;
import mizukichou.rpgdrop.gui.GuiManager;
import mizukichou.rpgdrop.util.Items;
import mizukichou.rpgdrop.util.Msg;
import org.bukkit.Material;
import org.bukkit.entity.Player;

/**
 * 掉落物类型选择页：原版物品 / RPGItem / 清除。
 *
 * 注意（硬性原则）：只保存 Material / RPGItem ID，绝不保存 ItemStack 本体。
 * V0.3 起将支持把 RPGItem 拖入本页面自动识别 ID。
 */
public final class ItemPickerGui extends Gui {

    private final DropManager dropManager;
    private final DropRule rule;

    public ItemPickerGui(RPGDropPlugin plugin, GuiManager manager, Player viewer, DropManager dropManager, DropRule rule) {
        super(plugin, manager, viewer);
        this.dropManager = dropManager;
        this.rule = rule;
    }

    @Override
    protected void fill() {
        icon(4, Items.icon(Material.CHEST, t("gui.item.current"), "&7" + describeCurrent()));

        button(11, Items.icon(Material.STONE, t("gui.item.vanilla"),
                        t("gui.item.vanilla_lore_1"),
                        t("gui.item.vanilla_lore_2"),
                        "",
                        t("gui.item.click_choose")),
                () -> navigate(new MaterialPickerGui(plugin, manager, viewer, dropManager, rule)));

        button(13, Items.icon(Material.ENDER_EYE, t("gui.item.rpgitem"),
                        t("gui.item.rpgitem_lore_1"),
                        t("gui.item.rpgitem_lore_2"),
                        plugin.isRpgItemsAvailable() ? t("gui.item.rpg_available") : t("gui.item.rpg_unavailable"),
                        "",
                        t("gui.item.click_set")),
                () -> manager.requestTextInput(viewer, "gui.item.rpgitem_prompt",
                        this::onRpgItem,
                        () -> navigate(new ItemPickerGui(plugin, manager, viewer, dropManager, rule))));

        button(15, Items.icon(Material.BARRIER, t("gui.item.clear"), t("gui.item.clear_lore")), () -> {
            rule.setItem(null);
            dropManager.ruleUpdated(rule);
            Msg.send(viewer, "gui.item.cleared");
            navigate(new RuleEditorGui(plugin, manager, viewer, dropManager, rule));
        });

        button(17, Items.icon(Material.ARROW, t("back")),
                () -> navigate(new RuleEditorGui(plugin, manager, viewer, dropManager, rule)));
    }

    private void onRpgItem(String raw) {
        String id = raw.trim();
        if (id.isEmpty()) {
            Msg.send(viewer, "gui.item.id_empty");
            navigate(new ItemPickerGui(plugin, manager, viewer, dropManager, rule));
            return;
        }
        if (id.length() > RPGItemDropItem.MAX_ID_LENGTH) {
            Msg.send(viewer, "gui.item.id_too_long", RPGItemDropItem.MAX_ID_LENGTH);
            navigate(new ItemPickerGui(plugin, manager, viewer, dropManager, rule));
            return;
        }
        rule.setItem(new RPGItemDropItem(id));
        dropManager.ruleUpdated(rule);
        Msg.send(viewer, "gui.item.set_rpgitem", rule.id(), id);
        if (!plugin.isRpgItemsAvailable()) {
            Msg.send(viewer, "command.item_rpgitems_warning");
        } else if (!plugin.isRpgItemExist(id)) {
            Msg.send(viewer, "command.item_rpgitem_missing", id);
        }
        navigate(new RuleEditorGui(plugin, manager, viewer, dropManager, rule));
    }

    private String describeCurrent() {
        if (rule.item() instanceof RPGItemDropItem rpgItem) {
            return "&fRPGITEM(" + rpgItem.rpgItemId() + ")";
        }
        if (rule.item() instanceof VanillaDropItem vanilla) {
            return "&fVANILLA(" + vanilla.material() + ")";
        }
        return "&c" + t("not_set");
    }

    @Override
    protected String titleKey() {
        return "gui.item.title";
    }

    @Override
    protected int size() {
        return 27;
    }
}
