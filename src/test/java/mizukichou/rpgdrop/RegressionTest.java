package mizukichou.rpgdrop;

import mizukichou.rpgdrop.config.ConfigException;
import mizukichou.rpgdrop.config.LotteryRuleSerializer;
import mizukichou.rpgdrop.drop.DropRule;
import mizukichou.rpgdrop.drop.LotteryManager;
import mizukichou.rpgdrop.drop.LotteryRule;
import mizukichou.rpgdrop.drop.NekoNYumeDropItem;
import mizukichou.rpgdrop.drop.Prize;
import mizukichou.rpgdrop.drop.RPGItemDropItem;
import mizukichou.rpgdrop.i18n.I18n;
import mizukichou.rpgdrop.util.Amounts;
import mizukichou.rpgdrop.util.Chance;
import mizukichou.rpgdrop.util.RuleIds;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * 回归/随机化测试（纯 JVM 运行，无需服务器）。运行：gradlew regressionTest
 * 固定 seed 可复现；随机生成输入并断言不变量（property-based testing）。
 * 说明：VANILLA 分支依赖 Paper 运行时 Registry，测试只覆盖 RPGITEM 分支与纯逻辑。
 */
public final class RegressionTest {

    private static int failures = 0;

    private static void check(boolean cond, String msg) {
        if (!cond) {
            failures++;
            System.out.println("FAIL: " + msg);
        }
    }

    public static void main(String[] args) throws Exception {
        long seed = args.length > 0 ? Long.parseLong(args[0]) : System.currentTimeMillis();
        Random rnd = new Random(seed);
        System.out.println("RegressionTest seed=" + seed);

        testChance(rnd);
        testAmounts(rnd);
        testRuleIds();
        testNormalize(rnd);
        testFormat(rnd);
        testPrizeValidation(rnd);
        testSerializerRoundTrip(rnd);
        testSerializerRejectsInvalid(rnd);

        testPickPrizeIndex(rnd);
        testModelInvariants(rnd);
        System.out.println(failures == 0 ? "ALL PASS" : failures + " FAILURES");
        System.exit(failures == 0 ? 0 : 1);
    }

    private static boolean refChance(double v) {
        return !(v != v) && !Double.isInfinite(v) && v >= 0 && v <= 100;
    }

    private static void testChance(Random rnd) {
        for (int i = 0; i < 20000; i++) {
            double v;
            switch (rnd.nextInt(6)) {
                case 0 -> v = Double.NaN;
                case 1 -> v = Double.POSITIVE_INFINITY;
                case 2 -> v = Double.NEGATIVE_INFINITY;
                case 3 -> v = -rnd.nextDouble() * 50;
                case 4 -> v = rnd.nextDouble() * 200;
                default -> v = rnd.nextDouble() * 100;
            }
            check(Chance.isValid(v) == refChance(v), "Chance.isValid(" + v + ") != reference");
        }
        check(!Chance.isValid(Double.NaN) && !Chance.isValid(101) && !Chance.isValid(-1)
                && Chance.isValid(0) && Chance.isValid(100), "Chance boundary values");
    }

    private static void testAmounts(Random rnd) {
        for (int i = 0; i < 20000; i++) {
            int min = rnd.nextInt(2000) - 500;
            int max = rnd.nextInt(2000) - 500;
            boolean ref = min >= 1 && max >= min && max <= Amounts.MAX_AMOUNT;
            check(Amounts.isValid(min, max) == ref, "Amounts.isValid(" + min + "," + max + ") != reference");
        }
    }

    private static void testRuleIds() {
        check(RuleIds.isValid("abc_1-2"), "valid id rejected");
        check(!RuleIds.isValid(""), "empty id accepted");
        check(!RuleIds.isValid("a b"), "id with space accepted");
        check(!RuleIds.isValid("中文"), "non-ascii id accepted");
        check(!RuleIds.isValid("a".repeat(33)), "33-char id accepted");
        check(RuleIds.isValid("a".repeat(32)), "32-char id rejected");
        check(!RuleIds.isValid(null), "null id accepted");
    }

    private static void testNormalize(Random rnd) throws Exception {
        Method m = I18n.class.getDeclaredMethod("normalize", String.class);
        m.setAccessible(true);
        check("zh_cn".equals(m.invoke(null, "zh_CN")), "zh_CN not normalized to zh_cn");
        check("zh_cn".equals(m.invoke(null, "zh-CN")), "zh-CN not normalized");
        check("zh_tw".equals(m.invoke(null, "zh_HK")), "zh_HK should map to zh_tw");
        check("ja_jp".equals(m.invoke(null, "ja")), "ja should map to ja_jp");
        check("en_us".equals(m.invoke(null, "en")), "en should map to en_us");
        for (int i = 0; i < 5000; i++) {
            String junk = randomJunk(rnd);
            check("en_us".equals(m.invoke(null, junk)), "junk locale '" + junk + "' should fall back to en_us");
        }
    }

    private static void testFormat(Random rnd) throws Exception {
        Method m = I18n.class.getDeclaredMethod("format", String.class, Object[].class);
        m.setAccessible(true);
        for (int i = 0; i < 5000; i++) {
            int n = rnd.nextInt(4);
            StringBuilder tpl = new StringBuilder("x");
            for (int k = 0; k < n; k++) {
                tpl.append(" {").append(k).append("} y");
            }
            Object[] args = new Object[n];
            for (int k = 0; k < n; k++) {
                args[k] = "V" + rnd.nextInt(1000);
            }
            String out = (String) m.invoke(null, tpl.toString(), args);
            for (int k = 0; k < n; k++) {
                check(out.contains(String.valueOf(args[k])), "format lost arg " + k + " in [" + out + "]");
                check(!out.contains("{" + k + "}"), "format left placeholder {" + k + "} in [" + out + "]");
            }
        }
    }

    private static void testPrizeValidation(Random rnd) {
        for (int i = 0; i < 20000; i++) {
            double w;
            switch (rnd.nextInt(6)) {
                case 0 -> w = Double.NaN;
                case 1 -> w = Double.POSITIVE_INFINITY;
                case 2 -> w = Double.NEGATIVE_INFINITY;
                case 3 -> w = -rnd.nextDouble() * 50;
                case 4 -> w = 100 + rnd.nextDouble() * 50;
                default -> w = rnd.nextDouble() * 100;
            }
            try {
                new Prize(new RPGItemDropItem("test_item"), w);
                check(Chance.isValid(w), "Prize accepted invalid weight " + w);
            } catch (IllegalArgumentException expected) {
                check(!Chance.isValid(w), "Prize rejected valid weight " + w);
            }
        }
    }

    private static void testSerializerRoundTrip(Random rnd) throws Exception {
        for (int i = 0; i < 300; i++) {
            String id = "r" + i;
            LotteryRule rule = new LotteryRule(id);
            rule.setEnabled(rnd.nextBoolean());
            rule.setTrigger(new RPGItemDropItem("trigger_" + rnd.nextInt(5)));
            int count = 1 + rnd.nextInt(4);
            double used = 0;
            for (int k = 0; k < count; k++) {
                double w;
                if (k == count - 1) {
                    w = Math.max(0.0, 100.0 - used); // 最后一个吃掉剩余，保证累计 <= 100
                } else {
                    w = (100.0 - used) * 0.5 * rnd.nextDouble(); // 保证不会超过 100
                }
                used += w;
                rule.addPrize(new Prize(new RPGItemDropItem("prize_" + rnd.nextInt(5)), w));
            }
            YamlConfiguration cfg = new YamlConfiguration();
            ConfigurationSection section = cfg.createSection("lotteries");
            LotteryRuleSerializer.write(rule, section);

            YamlConfiguration back = YamlConfiguration.loadConfiguration(new java.io.StringReader(cfg.saveToString()));
            LotteryRule parsed = LotteryRuleSerializer.parse(id, back.getConfigurationSection("lotteries." + id));
            check(parsed.isEnabled() == rule.isEnabled(), "enabled mismatch");
            check(parsed.prizes().size() == rule.prizes().size(), "prize count mismatch");
            for (int k = 0; k < rule.prizes().size(); k++) {
                check(rule.prizes().get(k).weight() == parsed.prizes().get(k).weight(), "weight round-trip mismatch");
            }
            check(parsed.trigger() instanceof RPGItemDropItem, "trigger type mismatch");
        }
    }

    private static void testSerializerRejectsInvalid(Random rnd) throws Exception {
        for (int i = 0; i < 3000; i++) {
            YamlConfiguration cfg = new YamlConfiguration();
            ConfigurationSection s = cfg.createSection("lotteries.test");
            s.set("enabled", true);
            s.set("trigger.type", "RPGITEM");
            s.set("trigger.id", "t");
            List<java.util.Map<String, Object>> prizes = new ArrayList<>();
            boolean badWeight = false;
            for (int k = 0; k < 1 + rnd.nextInt(3); k++) {
                java.util.Map<String, Object> p = new java.util.LinkedHashMap<>();
                p.put("type", "RPGITEM");
                p.put("id", "p" + k);
                double w = rnd.nextBoolean() ? rnd.nextDouble() * 200 - 50 : rnd.nextDouble() * 100;
                if (w < 0 || w > 100) badWeight = true;
                p.put("weight", w);
                prizes.add(p);
            }
            s.set("prizes", prizes);
            boolean shouldFail = badWeight || hasTotalOver100(prizes);
            try {
                LotteryRuleSerializer.parse("test", s);
                check(!shouldFail, "invalid config accepted (iter " + i + ")");
            } catch (ConfigException expected) {
                check(shouldFail, "valid config rejected (iter " + i + "): " + expected.getMessage());
            }
        }
    }

    private static boolean hasTotalOver100(List<java.util.Map<String, Object>> prizes) {
        double total = 0;
        for (java.util.Map<String, Object> p : prizes) {
            Object w = p.get("weight");
            if (w instanceof Number n && Double.isFinite(n.doubleValue()) && n.doubleValue() >= 0 && n.doubleValue() <= 100) {
                total += n.doubleValue();
            }
        }
        return total > 100 + 1e-9;
    }


    // ============================================================
    // 以下为系统级回归：抽奖概率边界 / 模型层 invariant（次级社区 issue #28/#29 要求）
    // ============================================================
    private static void testPickPrizeIndex(Random rnd) {
        // sum=100 时必中：随机 20000 次 roll 必须落在合法下标
        List<Prize> prizes = List.of(
                new Prize(new RPGItemDropItem("a"), 1.0),
                new Prize(new RPGItemDropItem("b"), 99.0));
        for (int i = 0; i < 20000; i++) {
            double roll = rnd.nextDouble() * 100.0;
            int idx = LotteryManager.pickPrizeIndex(prizes, roll);
            check(idx >= 0 && idx < prizes.size(), "pickPrizeIndex returned out-of-range index " + idx);
        }
        // 权重边界
        check(LotteryManager.pickPrizeIndex(prizes, 0.0) == 0, "roll=0 should hit first prize");
        check(LotteryManager.pickPrizeIndex(prizes, 0.999) == 0, "roll=0.999 should hit first prize");
        check(LotteryManager.pickPrizeIndex(prizes, 1.0) == 1, "roll=1.0 should hit second prize");
        check(LotteryManager.pickPrizeIndex(prizes, 99.999) == 1, "roll=99.999 should hit second prize");
        // 浮点兜底：总权重略小于 100（容差内），roll 接近 100 时兜底返回最后一个
        List<Prize> edge = List.of(
                new Prize(new RPGItemDropItem("x"), 99.99999999),
                new Prize(new RPGItemDropItem("y"), 0.000000005));
        check(LotteryManager.pickPrizeIndex(edge, 99.999999999) == 1, "fallback to last prize on epsilon boundary");
    }

    private static void testModelInvariants(Random rnd) {
        // LotteryRule：奖品上限 / null / 越界删除
        LotteryRule rule = new LotteryRule("test-lottery");
        int accepted = 0;
        for (int i = 0; i < 150; i++) {
            if (rule.addPrize(new Prize(new RPGItemDropItem("p" + i), 1.0))) {
                accepted++;
            }
        }
        check(accepted == LotteryRule.MAX_PRIZES_PER_RULE, "addPrize should stop at 100 (accepted=" + accepted + ")");
        check(!rule.addPrize(new Prize(new RPGItemDropItem("overflow"), 1.0)), "101st addPrize must be rejected");
        check(!rule.addPrize(null), "null prize must be rejected");
        rule.removePrize(-1);
        rule.removePrize(999);
        check(rule.prizes().size() == LotteryRule.MAX_PRIZES_PER_RULE, "out-of-range removePrize must be a no-op");

        // DropRule：NaN/Infinity 概率忽略、世界上限/超长拒绝、null 实体防御
        DropRule drop = new DropRule("test-drop");
        drop.setChance(50.0);
        check(drop.chance() == 50.0, "valid chance should be accepted");
        drop.setChance(Double.NaN);
        check(drop.chance() == 50.0, "NaN chance must be rejected (model invariant)");
        drop.setChance(-1.0);
        check(drop.chance() == 50.0, "negative chance must be rejected");
        drop.setAmount(1, 5);
        check(drop.minAmount() == 1 && drop.maxAmount() == 5, "valid amount should be accepted");
        drop.setAmount(-100, -1);
        check(drop.minAmount() == 1 && drop.maxAmount() == 5, "invalid amount must be rejected");
        drop.setAmount(10, 2);
        check(drop.minAmount() == 1 && drop.maxAmount() == 5, "max<min must be rejected");
        drop.addEntity(null);
        check(drop.entities().isEmpty(), "null entity must be rejected");
        int worlds = 0;
        for (int i = 0; i < 100; i++) {
            if (drop.addWorld("world_" + i)) {
                worlds++;
            }
        }
        check(worlds == DropRule.MAX_WORLDS_PER_RULE, "world count must be capped at 64 (got " + worlds + ")");
        check(!drop.addWorld(String.valueOf('a').repeat(100)), "over-long world name must be rejected");
    }

    private static String randomJunk(Random rnd) {
        int len = 3 + rnd.nextInt(10); // 纯字母 3+ 字符不会命中合法语言码
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < len; i++) {
            sb.append((char) ('a' + rnd.nextInt(26)));
        }
        return sb.toString();
    }
}
