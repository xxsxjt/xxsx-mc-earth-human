# 26.1.2 移植记录（release/neoforge-26.1.2）

日期：2026-10-29（2026-11-02 更新至 `7e1966a`）
目标：Minecraft 26.1.2 / NeoForge 26.1.2.124，mod 版本仍为 0.1.20
当前代码 HEAD：`7e1966a`（`d527c9a`–`7e1966a`）
产物：`earth-human-neoforge-26.1.2-0.1.20.jar`
实测大小：`304,682 B`
SHA-256：`ED9F5BD528738F7E0124221160564566838C72C8FF44D0C92DE082BE5395184E`

（初版移植 `f6a3318` 产物为 `277,937 B`，SHA-256 `BCA36ACB1A2B7F756AB818D4BE231EE0F3E8FE7131A36D85E032B3036910CA7B`。）

## 与 main（26.2）的差异

- `neoForge.version` `26.2.0.93` → `26.1.2.124`；`base.archivesName` 与
  `rootProject.name` 改为 `earth-human-neoforge-26.1.2`，避免与 26.2 产物混淆。
- 26.1.2 没有 `net.minecraft.client.gui.Hud`：`Gui.setScreen` → `Minecraft.setScreen`，
  `Gui.hud.rightHeight` → `Gui.rightHeight`，`Hud.isHidden()` → `Options.hideGui`。
- `neoforge.mods.toml` 要求 NeoForge `[26.1.2.124,)`、Minecraft `[26.1.2]`。
- `pack.mcmeta` 用 `min_format [84,0]` / `max_format [101,1]`，即 26.1.2 `version.json`
  的资源/数据格式。

## 本分支后续功能更新（`d527c9a`–`7e1966a`）

- `d527c9a`（水壶打水与可见物品动作）：
  - 空水壶（`canteen_empty`）对水源方块右键可直接打水变为满水壶（`canteen_filled`），播放装水音效且不消耗水源。
  - 体温计（`BLOCK` 姿态 20 tick）、野外健康记录（`BLOCK` 姿态 10 tick）与防护口罩（`BRUSH` 姿态 12 tick 后装备，支持副手）由瞬时无动作改为带原版可见使用姿态。
  - 服务端快捷护理进行期间客户端每 10 tick 挥动主手；`/eh exercise` 命令触发时主手挥动一次。
- `5ecc9b8`（放置型健身训练站与第三人称训练会话）：
  - 阻力带、训练垫、简易哑铃改为可放置的训练站方块（`ExerciseStationBlock` + `BlockItem`，保留原物品 ID 与背包图标）。
  - 放置后右键开启服务端权威计时训练会话（40–62 tick，通过 `HumanExerciseSession` 与 `HumanExerciseSessionPayload` 同步），期间客户端切换至第三人称视角（会话结束后恢复原视角）并每 8 tick 挥动主手；距离超过 6 格或方块被破坏时自动中止并提示原因。
- `14b44ce`、`6893085`、`7e1966a`（3D 训练站方块模型与区块图集纹理）：
  - 为三个训练站方块提供独立的 `assets/earth_human/textures/block/*.png`，满足 26.1.2 `SimpleModelWrapper` 对块图集（block atlas）的检查。
  - 新增生成脚本 `tools/build_station_block_models.py`，将三个训练站升级为 64×64 完全不透明（`alpha=255`）渐变材质与多元素旋转 3D 模型（`training_mat` 12 元素、`simple_dumbbell` 18 元素、`resistance_band` 16 元素），方块形状调整为 `Block.column(14.0D, 0.0D, 4.0D)`。

## 验证证据

- `gradlew build`：BUILD SUCCESSFUL；12 个测试类共 36 项，0 失败 0 错误 0 跳过。
- `tools/validate_release_resources.py`：98 个 JSON、25 条完整物品资源链、双语 key 对齐通过。
- 最新构建产物 `build/libs/earth-human-neoforge-26.1.2-0.1.20.jar`（对应代码 HEAD `7e1966a`）：实测大小 `304,682 B`，SHA-256 `ED9F5BD528738F7E0124221160564566838C72C8FF44D0C92DE082BE5395184E`。
- 专用服务器（仅 earth_human）：`Done (1.990s)`，`Earth Human 0.1.20 (earth_human)` 加载，
  日志含 `[Earth Human] NeoForge 26.1.2 module loaded`；`Loaded 1542 recipes`
  （1515 原版 + 27 本 mod）、1617 进度；14 条 WARN 全部来自原版（命令歧义、离线模式），
  无 ERROR/FATAL。（`d527c9a` 与 `5ecc9b8` 亦分别通过 26.1.2.124 专用服务端启动验证。）
- 初版部署记录（2026-10-29）：`E:\_dx\_Games\MC\xxxxxx\.minecraft\versions\26.1.2-NeoForge_26.1.2.124\mods`
  当时放入初版 jar（`BCA36ACB...`）。

## 未验证

- 未进入游戏客户端：HUD 叠放、物品栏内人体摘要的坐标、状态界面、快捷护理交互均只有
  源码级与服务器级证据。
- 26.1.2 实例里没有 `earth_on_minecraft`，饮水/环境联动为可选依赖，本次未实际联动。
- 物品提示、事件注册点与 26.2 的差异已逐一对比源码（`LivingDamageEvent` 仅少了
  `shouldCauseSideEffects`，`ItemTooltipEvent` 少了 `TooltipDisplay`，本 mod 都未使用），
  但没有运行时行为对照。

## 相关仓库

`earth_human_body` 的 `build.gradle:34` 仍写复合坐标
`com.xxsx.earthhuman:earth-human-neoforge-26.2:0.1.20`，与本分支的新 archive 名不再匹配；
该问题已记入 `mc-agent/HANDOFF.md`，未在本仓库修改。
