package mizukichou.rpgdrop.drop;

import mizukichou.rpgdrop.util.Chance;

/**
 * 抽奖奖品：物品 + 权重（百分比）。
 * 一条抽奖规则内所有奖品的权重合计必须正好为 100%，每次抽奖必中其一。
 */
public record Prize(DropItem item, double weight) {

    public Prize {
        if (item == null) {
            throw new IllegalArgumentException("prize item must not be null");
        }
        if (!Chance.isValid(weight)) {
            throw new IllegalArgumentException("prize weight invalid: " + weight);
        }
    }
}
