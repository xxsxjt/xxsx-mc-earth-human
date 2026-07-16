# 地球：人类兼容接口

目标版本：Minecraft 26.2 / NeoForge 26.2.0.7-beta
当前实现：v0.1.14

## 设计边界

- 原版生命、最大生命、护甲、伤害修正、治疗和复活继续作为 Minecraft 生态兼容入口。
- 身体部位是附加状态层。其他 mod 增加 `MAX_HEALTH` 时，各部位容量按比例扩展。
- 快捷护理按钮只发送动作请求；物品查找、动作计时、效果应用和物品消耗全部由服务端完成。
- 护理界面显示服务端同步进度并支持取消；动作完成时会验证最初选中的物品 ID 和组件，不能用另一个同标签物品静默替换。
- 没有加入兼容标签的第三方物品不会被自动使用，避免误吃、误耗或把普通物品当药品。

## 生命桥接模式

在 `config/earth_human-common.toml` 的 `gameplay.health_bridge_mode` 中选择：

| 模式 | 行为 | 适用场景 |
|---|---|---|
| `vanilla_compatible` | 默认。保留原版最终扣血、吸收心、治疗、统计和其他 mod 事件，再把吸收后真实伤害映射到身体部位 | 普通游玩、整合包、未知 mod 环境 |
| `head_bar` | 原版红心作为头部/立即致死风险，非头部伤害主要进入部位系统 | 更沉浸的单机或明确测试过的模组组合 |
| `overlay_only` | 保留原版生命流程并记录身体状态，但不施加部位功能惩罚 | 只需要状态显示或兼容排查 |

`head_bar` 会主动接管部分伤害，无法保证所有依赖原版最终生命扣除的第三方机制都按原样工作，因此不作为默认值。

## 伤害类型标签

数据包和其他 mod 可以扩展以下 damage type tags：

| 标签 | 用途 |
|---|---|
| `earth_human:bypasses_body` | 完全绕过部位系统，例如脚本强制死亡 |
| `earth_human:head_priority` | 优先解释为头部/意识风险，例如窒息、毒性或特殊魔法 |
| `earth_human:full_body` | 按全身影响处理，而不是随机选择单一部位 |

文件示例：

```text
data/earth_human/tags/damage_type/head_priority.json
```

```json
{
  "replace": false,
  "values": ["example_mod:psychic_damage"]
}
```

## 服务端调节

`earth_human-common.toml` 提供：

- `gameplay.preset`：`casual`、`standard`、`realistic`、`custom`。
- `systems.*`：身体、水分、体温、疲劳、训练和部位功能后果的独立开关。
- `multipliers.*`：消耗、恢复、身体伤害、扩散、治疗和训练倍率。
- `respawn.*`：死亡后头部和其他部位的最低稳定比例，不会重置高于该比例的长期状态。

## 活动与移动兼容

- 活动负荷不拦截移动事件，也不重新实现移动物理；服务端每秒读取 Minecraft 原版逐 tick 累积的统计增量。
- 当前识别 `WALK_ONE_CM`、`CROUCH_ONE_CM`、`SPRINT_ONE_CM`、水中移动、`CLIMB_ONE_CM`、各原版载具距离、`AVIATE_ONE_CM` 和 `FLY_ONE_CM`。
- 步行、疾跑、游泳、攀爬、载具、鞘翅和普通飞行分别使用不同疲劳、水分、训练和连续负荷权重。
- 正常维护原版移动统计的其他 mod 会自动接入；完全自定义且不更新原版统计的移动方式当前不会被凭空猜测，后续可通过公开活动 API 声明。
- 载具移动默认只产生极低躯干/腿部负荷，静止乘坐时会缓慢恢复连续负荷，避免把高速载具误判成人体疾跑。

## 护理物品标签

数据包或其他 mod 可向以下 item tags 加入自己的物品：

| 标签 | 用途 | 未提供专用 Java 行为时的默认效果 |
|---|---|---|
| `earth_human:care/hydration` | 补充水分 | 水分 +25 |
| `earth_human:care/bandages` | 包扎伤口 | 治疗最严重的非头部部位 1.5 |
| `earth_human:care/first_aid` | 综合急救 | 治疗最严重部位 4.0，可包含头部 |
| `earth_human:care/warming` | 保暖处理 | 体温 +0.45°C，疲劳 -1 |
| `earth_human:care/cooling` | 降温处理 | 体温 -0.45°C，疲劳 -1 |

本 mod 自己的 `StatusConsumableItem` 会优先执行物品定义中的精确效果，表中的默认效果主要供第三方标签物品使用。

## 数据包示例

```json
{
  "replace": false,
  "values": [
    "example_mod:advanced_bandage"
  ]
}
```

把文件放在：

```text
data/earth_human/tags/item/care/bandages.json
```

## 当前限制

- 标签只能选择标准护理分类；第三方物品需要完全自定义动作、动画、目标选择或副作用时，仍需要未来的公开 Java 注册 API。
- 快捷护理目前从玩家主背包选择第一个匹配物品，不自动判断第三方物品的价格或稀有度。
- 当前界面不替代其他 mod 的装备栏、药水系统或治疗事件，只提供明确的可选护理入口。
- `head_bar` 是明确的沉浸式兼容取舍；无法识别的第三方伤害应通过上述 damage type tags 声明。
