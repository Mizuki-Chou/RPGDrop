package mizukichou.rpgdrop.drop;

import java.util.Locale;
import java.util.Optional;
import java.util.Set;

/**
 * NekoNYume 插件物品（可选依赖）。只保存"种类 + 值"，掉落时通过 {@code NekoNYumeHook}
 * 调用其官方 API 实时生成，绝不保存 ItemStack 本体。
 *
 * <p>支持 {@code nyadmin give} 面板可发放的物品种类：</p>
 * <ul>
 *   <li>{@code meowdan:品质}（COMMON/UNCOMMON/RARE/EPIC/LEGENDARY）</li>
 *   <li>{@code xppill:等级}（NORMAL/ELITE）</li>
 *   <li>{@code equipment:装备ID}（如 COLLAR_RARE、YARN_BALL_LEGENDARY）</li>
 *   <li>{@code equipbag}（装备袋，无参数）</li>
 * </ul>
 */
public record NekoNYumeDropItem(String kind, String value) implements DropItem {

    public static final int MAX_VALUE_LENGTH = 64;

    public static final Set<String> KINDS = Set.of("meowdan", "xppill", "equipment", "equipbag");

    public NekoNYumeDropItem {
        if (kind == null || !KINDS.contains(kind.toLowerCase(Locale.ROOT))) {
            throw new IllegalArgumentException("unknown nyn kind: " + kind);
        }
        kind = kind.toLowerCase(Locale.ROOT);
        if ("equipbag".equals(kind)) {
            value = "";
        } else {
            value = value == null ? "" : value.trim();
            if (value.isBlank()) {
                throw new IllegalArgumentException("nyn value must not be empty for kind: " + kind);
            }
        }
        if (value.length() > MAX_VALUE_LENGTH) {
            throw new IllegalArgumentException("nyn value too long (max " + MAX_VALUE_LENGTH + " chars)");
        }
    }

    /** 解析玩家输入（{@code kind:value}），非法格式返回 empty。 */
    public static Optional<NekoNYumeDropItem> fromInput(String raw) {
        if (raw == null) {
            return Optional.empty();
        }
        String[] parts = raw.trim().split(":", 2);
        if (parts.length == 0 || !KINDS.contains(parts[0].trim().toLowerCase(Locale.ROOT))) {
            return Optional.empty();
        }
        String kind = parts[0].trim().toLowerCase(Locale.ROOT);
        String value = parts.length > 1 ? parts[1].trim() : "";
        if ("equipbag".equals(kind)) {
            value = "";
        } else if (value.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new NekoNYumeDropItem(kind, value));
    }

    @Override
    public ItemType type() {
        return ItemType.NEKONYUME;
    }
}
