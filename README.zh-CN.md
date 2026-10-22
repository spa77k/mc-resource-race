# ResourceRace

[English](README.md) | [日本語](README.ja.md) | 简体中文 | [한국어](README.ko.md)

一款 Paper 插件，在资源世界重置后立即开启一场比赛：按收集指定物品的先后顺序决定名次，并发放 Spa 币。

- 同一时间只进行一场比赛。开始新比赛时，会先结束正在进行的比赛。
- 目标物品从该世界配置的候选中随机抽取一个。
- 奖励通过 Vault 按名次只发放一次。已发放的状态会被保存，重启服务器也不会重复发放给同一个人。
- 名次会在聊天栏向全体公布，获奖者本人还会看到标题提示。
- 比赛期间，只有在目标世界中获得的物品才会被打上标记并计数。
- 只能通过控制台和 RCON 开始或结束比赛。玩家可以用 `/race` 查看比赛情况。

## 运行环境

- Paper 1.21.4 或更高版本（已在 Paper 26.2 上测试）
- Java 21 或更高版本（以服务器版本的要求为准）
- [Vault](https://www.spigotmc.org/resources/vault.34315/) 和一个经济插件（默认使用 EssentialsX）

## 安装

1. 从 [Releases](https://github.com/spa77k/ResourceRace/releases) 下载 `resourcerace-1.0.0.jar`。
2. 将其放入与 Vault 相同的 `plugins/` 文件夹。
3. 重启服务器，会生成 `plugins/ResourceRace/config.yml`。
4. 在 `worlds` 中填写资源世界，然后重启服务器。
5. 重置之后，在控制台或 RCON 中执行 `race start <世界>`。

## 命令

| 命令 | 可用者 | 说明 |
| --- | --- | --- |
| `/race` | 所有人 | 显示目标物品、目标世界、截止时间、自己的进度和排名 |
| `/race start <世界>` | `resourcerace.admin` | 开始比赛。如有正在进行的比赛，会先将其结束 |
| `/race stop` | `resourcerace.admin` | 结束正在进行的比赛 |
| `/race reload` | `resourcerace.admin` | 重新加载 `config.yml` |

`resourcerace.admin` 默认授予 OP。命令可在控制台和 RCON 中使用，目标世界无需事先加载。

### 返回结果

| 结果 | 含义 |
| --- | --- |
| `RESOURCE_RACE_OK started world=<世界> item=<MATERIAL> amount=<数量> race=<race-id>` | 比赛已开始 |
| `RESOURCE_RACE_ERROR world-not-configured` | 该世界不在 `worlds` 中 |
| `RESOURCE_RACE_ERROR no-targets` | 该世界的候选全部无效 |
| `RESOURCE_RACE_OK stopped` | 比赛已结束 |
| `RESOURCE_RACE_ERROR no-race` | 没有可结束的比赛 |
| `RESOURCE_RACE_OK reloaded` | 已重新加载 `config.yml` |

## 配置

`plugins/ResourceRace/config.yml`

| 键 | 默认值 | 说明 |
| --- | --- | --- |
| `worlds` | 三个资源世界 | 可以进行比赛的世界。每个世界包含 `display-name` 和 `targets`。 |
| `worlds.<世界>.targets[].item` | | 候选物品的 Material 名称。 |
| `worlds.<世界>.targets[].amount` | | 需要收集的数量。 |
| `rewards` | `[1000, 500, 300]` | 各名次的 Spa 币奖励。元素个数即为名次数量。 |
| `duration-hours` | `167` | 比赛时长（小时）。为避免与下一次重置重叠，请设置为略短于 7 天。 |

开始比赛时，会从该世界的 `targets` 中抽取一个。Material 名称无法识别或 `amount` 小于 1 的候选，会在加载时发出警告并被排除。

比赛和排名保存在 `plugins/ResourceRace/state.yml` 中。

## 标记规则

比赛期间，在目标世界中获得物品时会为其打上标记。只有目标 Material、属于本场比赛、且带有自己标记的物品才会计入进度。计算背包（存储栏）和副手，不计算末影箱。

计入的物品：

- 自己破坏的方块的掉落物（容器和自己放置的方块除外）。
- 生物等非玩家实体的掉落物。
- 箱子等容器中生成的战利品。
- 钓鱼钓到的物品。
- 与猪灵以物易物得到的物品。
- 从自然生成的物品展示框中取出的物品，或破坏它时掉落的物品。
- 用带标记的材料在熔炉中烧制的产物。
- 所有非空材料都带有同一玩家标记时的合成结果。

熔炉和工作台只是继承材料的标记，因此即使在目标世界以外使用，产物也会带有标记。排名判定在任何世界中都会进行。

不计入的物品：

- 在目标世界以外获得的物品。
- 自己放置的方块的掉落物。
- 被破坏的容器的掉落物。
- 捡起过物品的生物的掉落物。
- 自己放置或放入物品的展示框中的物品。
- 没有标记、带有他人标记或旧比赛标记的物品。
- 末影箱中的物品。

旧比赛的标记会在登录时、打开物品栏时以及比赛结束时清除，因此比赛结束后物品可以重新与普通物品堆叠。

## 许可证

[MIT](LICENSE)
