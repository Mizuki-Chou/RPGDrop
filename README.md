# RPGDrop

给 Paper 服务器增加自定义生物掉落和抽奖系统的插件。

**Release 2**

## 特性

### 📦 自定义掉落

按以下条件配置生物掉落：

* 生物类型
* 世界
* 概率
* 数量
* 掉落物

支持原版物品和 RPGItems。

RPGItems 物品通过 API 实时生成，不保存物品快照，因此 RPGItems 修改属性后，RPGDrop 掉落的也是最新物品。

### 🎰 抽奖系统

执行 `/rdrop gui` 后，可以选择：

* 📦 掉落规则
* 🎰 抽奖规则

抽奖由**触发物 + 奖品权重**组成。

* 触发物支持原版 / RPGItems / NekoNYume
* 奖品支持原版 / RPGItems / NekoNYume
* 所有奖品权重必须合计 100%
* 每次抽奖必定获得一个奖品
* 权重未达到 100% 时视为未就绪，不消耗触发物

抽奖规则支持：

```text
/rdrop lottery create
/rdrop lottery delete
/rdrop lottery list
/rdrop lottery info
/rdrop lottery trigger
/rdrop lottery prize add
/rdrop lottery prize remove
```

### 🐱 NekoNYume 支持

NekoNYume 为可选依赖，通过运行时反射集成。

支持：

* 喵丹 5 品质
* 经验丸 2 档
* 装备 25 件
* 装备袋

NekoNYume 物品可用于：

**掉落物 / 抽奖奖品 / 抽奖触发物**

未安装 NekoNYume 时，RPGDrop 仍可正常构建和运行。

### 🖱️ GUI 点选物品

RPGItems 和 NekoNYume 都支持 GUI 点选：

* 列出可用物品
* 显示真实物品预览
* 点击即可选择
* 保留手动输入作为兜底

NekoNYume 当前提供 33 种物品的直接选择。

### 🌐 多语言

内置：

* 简体中文
* 繁体中文
* 日本語
* English

根据玩家客户端语言自动切换。

语言文件位于：

```text
plugins/RPGDrop/lang/
```

---

## 环境

* Paper 26.2
* Java 25

RPGItems / NekoNYume 均为可选依赖。

RPGItems 如需使用，还需要 RPGItems 自身的依赖。

---

## 安装

将：

```text
RPGDrop-Release-2.jar
```

放入服务器：

```text
plugins/
```

然后**重启服务器**。

配置文件位于：

```text
plugins/RPGDrop/
```

主要包括：

```text
config.yml
drops.yml
lang/
```

---

## 构建

无需安装 RPGItems 或 NekoNYume 即可编译。

```bash
./gradlew build
```

Windows：

```bat
gradlew.bat build
```

产物：

```text
build/libs/RPGDrop-Release-2.jar
```

---

## 掉落配置示例

```yaml
imagedrops:
  nekomoon_broke:
    enabled: true
    entities: [ZOMBIE, SKELETON]
    worlds:
      mode: WHITELIST
      list: [resource]
    item:
      type: RPGITEM
      id: nekomoon_broke
    chance: 0.01
    amount:
      min: 1
      max: 1
```

`chance` 使用百分比：

```text
0.01 = 0.01%
```

原版物品：

```yaml
item:
  type: VANILLA
  material: DIAMOND
```

---

## 命令

主命令：

```text
/rpgdrop
/rdrop
```

常用命令：

```text
/rdrop gui
/rdrop list
/rdrop info
/rdrop create
/rdrop delete
/rdrop reload
```

抽奖：

```text
/rdrop lottery ...
```

完整帮助：

```text
/rdrop help
```

---

## 注意

* 每次修改规则会自动保存
* `drops.yml.bak` 可用于恢复备份
* 单次死亡单条规则最多生成 256 个掉落物实体
* `settings.debug: true` 仅建议短时间排查问题
* 抽奖权重必须达到 100%，否则不会消耗触发物
* 未安装 RPGItems / NekoNYume 时，对应物品类型无法生成

---

## Release 2

Release 2 在原有自定义掉落基础上新增：

**🎰 抽奖系统 · 🐱 NekoNYume 支持 · 🖱️ GUI 点选物品 · 🎁 权重奖品**
