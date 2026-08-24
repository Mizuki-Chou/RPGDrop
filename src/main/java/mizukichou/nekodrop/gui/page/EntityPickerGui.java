package mizukichou.nekodrop.gui.page;

import mizukichou.nekodrop.RPGDropPlugin;
import mizukichou.nekodrop.drop.DropManager;
import mizukichou.nekodrop.drop.DropRule;
import mizukichou.nekodrop.gui.Gui;
import mizukichou.nekodrop.gui.GuiManager;
import mizukichou.nekodrop.util.Items;
import mizukichou.nekodrop.util.Msg;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Locale;

/**
 * 生物选择页：点击切换选中状态（多选），支持搜索任意生物类型。
 * 生物名通过语言文件键 entity.&lt;EntityType&gt; 翻译。
 */
public final class EntityPickerGui extends Gui {

    /** 常用生物清单（规格书指定的 6 种亡灵 + 未来扩展清单）。 */
    private static final List<String> MOBS = List.of(
            "ZOMBIE", "HUSK", "DROWNED", "SKELETON", "STRAY", "WITHER_SKELETON",
            "CREEPER", "SPIDER", "CAVE_SPIDER", "ENDERMAN", "BLAZE", "WITCH",
            "PIGLIN", "ZOMBIFIED_PIGLIN", "PIGLIN_BRUTE", "HOGLIN", "ZOGLIN",
            "GHAST", "MAGMA_CUBE", "SLIME", "PHANTOM", "SILVERFISH", "ENDERMITE",
            "VEX", "EVOKER", "VINDICATOR", "PILLAGER", "RAVAGER", "WARDEN",
            "GUARDIAN", "ELDER_GUARDIAN", "SHULKER", "ENDER_DRAGON", "WITHER",
            "WOLF", "IRON_GOLEM", "SNOW_GOLEM", "ZOMBIE_VILLAGER"
    );

    private final DropManager dropManager;
    private final DropRule rule;

    public EntityPickerGui(RPGDropPlugin plugin, GuiManager manager, Player viewer, DropManager dropManager, DropRule rule) {
        super(plugin, manager, viewer);
        this.dropManager = dropManager;
        this.rule = rule;
    }

    @Override
    protected void fill() {
        int slot = 0;
        for (String mob : MOBS) {
            boolean selected = rule.entities().stream().anyMatch(t -> t.name().equals(mob));
            ItemStack icon = Items.mobIcon(mob);
            icon = Items.icon(icon.getType(), (selected ? "&a✔ " : "&7") + t("entity." + mob),
                    selected ? t("gui.entity.selected") : t("gui.entity.unselected"));
            if (selected) {
                icon = Items.glow(icon);
            }
            button(slot++, icon, () -> toggle(mob));
        }

        // 选中数量显示
        icon(49, Items.icon(Material.NAME_TAG, t("gui.entity.count"),
                t("gui.entity.count_value", rule.entities().size()),
                rule.entities().isEmpty() ? t("gui.entity.count_empty")
                        : "&f" + String.join(", ", rule.entities().stream().map(Enum::name).sorted().toList())));

        // 清空全部：清掉规则里所有实体（含通过搜索添加的、不在常用清单内的实体）
        button(46, Items.icon(Material.REDSTONE_BLOCK, t("gui.entity.clear")), () -> {
            for (EntityType type : List.copyOf(rule.entities())) {
                rule.removeEntity(type);
            }
            dropManager.ruleUpdated(rule);
            render();
        });
        button(47, Items.icon(Material.OAK_SIGN, t("gui.entity.search"), t("gui.entity.search_lore")), () ->
                manager.requestTextInput(viewer, "gui.entity.search_prompt",
                        this::onSearch,
                        () -> navigate(new EntityPickerGui(plugin, manager, viewer, dropManager, rule))));
        button(48, Items.icon(Material.ARROW, t("back")),
                () -> navigate(new RuleEditorGui(plugin, manager, viewer, dropManager, rule)));
    }

    private void toggle(String mob) {
        EntityType type = EntityType.valueOf(mob);
        if (rule.entities().contains(type)) {
            rule.removeEntity(type);
        } else {
            rule.addEntity(type);
        }
        dropManager.ruleUpdated(rule);
        render();
    }

    private void onSearch(String raw) {
        try {
            EntityType type = EntityType.valueOf(raw.trim().toUpperCase(Locale.ROOT));
            if (rule.entities().contains(type)) {
                rule.removeEntity(type);
                Msg.send(viewer, "gui.entity.removed", type.name());
            } else {
                rule.addEntity(type);
                Msg.send(viewer, "gui.entity.added", type.name());
            }
            dropManager.ruleUpdated(rule);
        } catch (IllegalArgumentException e) {
            Msg.send(viewer, "gui.entity.unknown", raw);
        }
        navigate(new EntityPickerGui(plugin, manager, viewer, dropManager, rule));
    }

    @Override
    protected String titleKey() {
        return "gui.entity.title";
    }

    @Override
    protected int size() {
        return 54;
    }
}
