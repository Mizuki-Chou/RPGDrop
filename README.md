# RPGDrop

给 Paper 服务器加自定义生物掉落的插件。Release 1。

按"什么生物、在哪些世界、多少概率、掉几个"来配置掉落。掉落物可以是原版物品，也可以直接掉 RPGItems 的物品——通过 RPGItems 的 API 实时生成，不是存物品快照，所以 RPGItems 那边改了属性，这边掉的也是最新的。

## 特性

- 掉落规则：生物类型 + 世界 + 概率 + 数量，全部可配置
- 图形编辑器：`/rpgdrop gui`，规则在游戏里点出来，不用手改文件
- RPGItems 集成（可选）：不装也能用，只是掉不了 RPGItems 物品
- 语言：简中 / 繁中 / 日语 / 英语，按客户端语言自动切换，翻译文件在 plugins/RPGDrop/lang/ 里可以自己改

## 环境

- Paper 26.2、Java 25。服务器版本不同的话需要改构建配置，见下文
- RPGItems 可选。要用的话它还需要 NyaaCore 和 Vault

## 构建

把服务器 plugins 目录里的 RPGItems.jar 复制到项目的 libs 文件夹，然后：

```
gradlew build
```

产物在 `build/libs/RPGDrop-Release-1.jar`。

服务器不是 26.2 的话，改 build.gradle.kts：

- `paperVersion` 改成对应版本（1.21.x 用 `1.21.11-R0.1-SNAPSHOT`）
- `release` 改成 21
- plugin.yml 的 `api-version` 改成 `1.21`

## 安装与配置

把 jar 丢进 plugins 文件夹重启。配置文件在 plugins/RPGDrop/：

- `config.yml`：插件设置（debug、是否清原版掉落等）
- `drops.yml`：掉落规则
- `lang/`：语言文件，改完 `/rpgdrop reload` 生效

规则长这样：

```yaml
drops:
  nekomoon_broke:
    enabled: true
    entities: [ZOMBIE, SKELETON]
    worlds:
      mode: WHITELIST
      list: [resource]
    item:
      type: RPGITEM
      id: nekomoon_broke
    chance: 0.01      # 百分比单位，0.01 就是 0.01%
    amount:
      min: 1
      max: 1
```

item.type 填 `VANILLA` 就是原版物品，配 `material: DIAMOND` 这种。

## 命令

`/rpgdrop` 或 `/rdrop` 都行：

- `gui`：打开编辑器
- `list` / `info`：查看规则
- `create` / `delete`：创建、删除规则
- `entity` / `world` / `item` / `chance` / `amount`：修改规则的各个部分
- `reload`：重载配置和语言文件

具体用法 `/rdrop help` 看。

## 注意

- chance 的单位是百分比，`0.01 = 0.01%`，不是小数概率
- 每次修改规则都会自动保存，文件损坏了有 drops.yml.bak 可以恢复
- 没装 RPGItems 的时候，RPGITEM 类型的规则不会掉东西，启动时会警告
