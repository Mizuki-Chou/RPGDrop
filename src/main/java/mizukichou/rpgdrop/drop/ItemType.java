package mizukichou.rpgdrop.drop;

import java.util.Locale;
import java.util.Optional;

/**
 * 掉落物来源类型。
 *
 * 未来扩展：ITEMSADDER、MMOITEMS、ORAXEN ...（配合新的 ItemProvider 实现，勿硬编码）。
 */
public enum ItemType {
    VANILLA,
    RPGITEM;

    public static Optional<ItemType> parse(String raw) {
        if (raw == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(ItemType.valueOf(raw.trim().toUpperCase(Locale.ROOT)));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
