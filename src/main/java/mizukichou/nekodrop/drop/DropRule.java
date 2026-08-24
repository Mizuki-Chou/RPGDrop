package mizukichou.nekodrop.drop;

import org.bukkit.World;
import org.bukkit.entity.EntityType;

import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;

/**
 * 一条掉落规则：什么生物、在哪些世界、以什么概率、掉什么物品、掉多少个。
 *
 * 规则 ID 全局唯一（大小写不敏感）。
 * 概率单位：chance = 0.01 表示 0.01%（全项目统一，勿混淆为 1%）。
 */
public final class DropRule {

    private final String id;

    private final Set<EntityType> entities = EnumSet.noneOf(EntityType.class);
    private final Set<String> worlds = new HashSet<>();
    private WorldMode worldMode = WorldMode.WHITELIST;

    private DropItem item;
    /** 概率（百分比单位）：0.01 = 0.01%。新建规则默认为 100，避免"忘了设概率就永远不掉"的陷阱（新规则生物/世界为空，本身不会触发）。 */
    private double chance = 100.0;
    private int minAmount = 1;
    private int maxAmount = 1;
    private boolean enabled = true;

    public DropRule(String id) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Rule ID must not be empty");
        }
        this.id = id;
    }

    public String id() {
        return id;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Set<EntityType> entities() {
        return Collections.unmodifiableSet(entities);
    }

    public void addEntity(EntityType type) {
        entities.add(type);
    }

    public void removeEntity(EntityType type) {
        entities.remove(type);
    }

    public Set<String> worlds() {
        return Collections.unmodifiableSet(worlds);
    }

    public void addWorld(String world) {
        worlds.add(world);
    }

    public void removeWorld(String world) {
        worlds.remove(world);
    }

    public WorldMode worldMode() {
        return worldMode;
    }

    public void setWorldMode(WorldMode worldMode) {
        this.worldMode = worldMode;
    }

    /** 掉落物描述；未配置时为 null（掉落时会被跳过并告警）。 */
    public DropItem item() {
        return item;
    }

    public void setItem(DropItem item) {
        this.item = item;
    }

    /** 概率（百分比单位）：0.01 = 0.01%。 */
    public double chance() {
        return chance;
    }

    public void setChance(double chance) {
        this.chance = chance;
    }

    public int minAmount() {
        return minAmount;
    }

    public int maxAmount() {
        return maxAmount;
    }

    public void setAmount(int min, int max) {
        this.minAmount = min;
        this.maxAmount = max;
    }

    public boolean matchesEntity(EntityType type) {
        return entities.contains(type);
    }

    public boolean matchesWorld(World world) {
        return switch (worldMode) {
            case WHITELIST -> worlds.contains(world.getName());
        };
    }
}
