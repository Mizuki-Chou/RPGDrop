package mizukichou.nekodrop.gui.page;

import mizukichou.nekodrop.RPGDropPlugin;
import mizukichou.nekodrop.drop.DropManager;
import mizukichou.nekodrop.drop.DropRule;
import mizukichou.nekodrop.drop.RPGItemDropItem;
import mizukichou.nekodrop.drop.VanillaDropItem;
import mizukichou.nekodrop.gui.Gui;
import mizukichou.nekodrop.gui.GuiManager;
import mizukichou.nekodrop.util.Items;
import mizukichou.nekodrop.util.Msg;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Optional;

/**
 * 规则编辑页：生物 / 世界 / 掉落物 / 概率 / 数量 / 启用 / 删除 / 返回。
 *
 * 掉落物按钮会通过 RPGItems API 实时预览真实物品（改动自动生效）。
 */
public final class RuleEditorGui extends Gui {

    private final DropManager dropManager;
    private final DropRule rule;

    public RuleEditorGui(RPGDropPlugin plugin, GuiManager manager, Player viewer, DropManager dropManager, DropRule rule) {
        super(plugin, manager, viewer);
        this.dropManager = dropManager;
        this.rule = rule;
    }

    @Override
    protected void fill() {
        // 规则可能已被删除（如被其它管理员删除）
        if (dropManager.getRule(rule.id()) == null) {
            Msg.send(viewer, "gui.editor.rule_deleted", rule.id());
            close();
            return;
        }

        // 标题区
        icon(0, Items.icon(Material.NAME_TAG, "&d" + rule.id(),
                t("gui.editor.info_enabled", rule.isEnabled() ? t("yes") : t("no")),
                t("gui.editor.info_hint")));

        // 功能按钮
        button(10, Items.icon(Material.ZOMBIE_SPAWN_EGG, t("gui.editor.entity"),
                        t("gui.editor.entity_current", rule.entities().isEmpty()
                                ? t("gui.editor.entity_empty")
                                : "&f" + String.join(", ", rule.entities().stream().map(Enum::name).sorted().toList())),
                        "",
                        t("gui.editor.click_edit")),
                () -> navigate(new EntityPickerGui(plugin, manager, viewer, dropManager, rule)));

        button(11, Items.icon(Material.GRASS_BLOCK, t("gui.editor.world"),
                        t("gui.editor.world_current", rule.worlds().isEmpty()
                                ? t("gui.editor.world_empty")
                                : "&f" + String.join(", ", rule.worlds().stream().sorted().toList())),
                        "",
                        t("gui.editor.click_edit")),
                () -> navigate(new WorldPickerGui(plugin, manager, viewer, dropManager, rule)));

        button(12, dropItemIcon(), () -> navigate(new ItemPickerGui(plugin, manager, viewer, dropManager, rule)));

        button(13, Items.icon(Material.GOLD_NUGGET, t("gui.editor.chance"),
                        t("gui.editor.chance_current", rule.chance()),
                        t("gui.editor.chance_note"),
                        "",
                        t("gui.editor.click_edit")),
                () -> navigate(new ChancePickerGui(plugin, manager, viewer, dropManager, rule)));

        button(14, Items.icon(Material.PAPER, t("gui.editor.amount"),
                        t("gui.editor.amount_current", rule.minAmount(), rule.maxAmount()),
                        "",
                        t("gui.editor.click_edit")),
                () -> navigate(new AmountPickerGui(plugin, manager, viewer, dropManager, rule)));

        button(15, Items.icon(rule.isEnabled() ? Material.LIME_DYE : Material.RED_DYE,
                        (rule.isEnabled() ? "&a" : "&c") + t("gui.editor.toggle", ""),
                        t("gui.editor.toggle_current", rule.isEnabled() ? t("enabled") : t("disabled")),
                        "",
                        t("gui.editor.toggle_click")), () -> {
            rule.setEnabled(!rule.isEnabled());
            dropManager.ruleUpdated(rule);
            render();
        });

        button(16, Items.icon(Material.BARRIER, t("gui.editor.delete"), t("gui.editor.delete_lore")), () ->
                navigate(new ConfirmGui(plugin, manager, viewer, "gui.editor.delete_confirm", rule.id(),
                        () -> {
                            dropManager.deleteRule(rule.id());
                            Msg.send(viewer, "command.rule_deleted", rule.id());
                            navigate(new MainMenuGui(plugin, manager, viewer, dropManager));
                        },
                        () -> navigate(new RuleEditorGui(plugin, manager, viewer, dropManager, rule)))));

        button(17, Items.icon(Material.ARROW, t("back"), t("gui.editor.back_lore")),
                () -> navigate(new MainMenuGui(plugin, manager, viewer, dropManager)));
    }

    /**
     * 掉落物按钮图标：
     *   - RPGItem：通过 RPGItems API 实时生成真实物品作为预览（无 RPGItems 时降级为说明图标）；
     *   - 原版物品：直接展示；
     *   - 未设置：提示图标。
     */
    private ItemStack dropItemIcon() {
        if (rule.item() instanceof RPGItemDropItem rpgItem) {
            Optional<ItemStack> preview = plugin.previewRpgItem(rpgItem.rpgItemId());
            if (preview.isPresent()) {
                return preview.get();
            }
            return Items.icon(Material.ENDER_EYE, t("gui.editor.drop"),
                    t("gui.editor.drop_rpgitem", rpgItem.rpgItemId()),
                    t("gui.editor.drop_preview_fail"),
                    "",
                    t("gui.editor.drop_click_edit"));
        }
        if (rule.item() instanceof VanillaDropItem vanilla) {
            return new ItemStack(vanilla.material());
        }
        return Items.icon(Material.CHEST, t("gui.editor.drop"), t("gui.editor.entity_current", t("not_set")), "", t("gui.editor.drop_click_set"));
    }

    @Override
    protected String titleKey() {
        return "gui.editor.title";
    }

    @Override
    protected int size() {
        return 27;
    }
}
