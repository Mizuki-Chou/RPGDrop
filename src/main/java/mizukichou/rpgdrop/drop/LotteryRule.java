package mizukichou.rpgdrop.drop;

import java.util.ArrayList;
import java.util.List;

/**
 * 一条抽奖规则：触发物（原版物品或 RPGItem）+ 奖品列表。
 * 规则 ID 全局唯一（大小写不敏感）。
 */
public final class LotteryRule {

    /** 单条规则的最大奖品数（模型层硬限制，命令 / GUI / YAML 加载共用）。 */
    public static final int MAX_PRIZES_PER_RULE = 100;

    private final String id;
    private DropItem trigger;
    private final List<Prize> prizes = new ArrayList<>();
    private boolean enabled = true;

    public LotteryRule(String id) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("lottery rule id must not be empty");
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

    /** 触发物；未设置时返回 null。 */
    public DropItem trigger() {
        return trigger;
    }

    public void setTrigger(DropItem trigger) {
        this.trigger = trigger;
    }

    public List<Prize> prizes() {
        return List.copyOf(prizes);
    }

    /** 添加奖品；达到 {@link #MAX_PRIZES_PER_RULE} 上限时拒绝并返回 false（模型层最后一道防线）。 */
    public boolean addPrize(Prize prize) {
        if (prize == null) {
            return false; // 模型层防御：不接收 null
        }
        if (prizes.size() >= MAX_PRIZES_PER_RULE) {
            return false;
        }
        prizes.add(prize);
        return true;
    }

    public void removePrize(int index) {
        if (index < 0 || index >= prizes.size()) {
            return; // 模型层防御：越界删除直接忽略
        }
        prizes.remove(index);
    }
}
