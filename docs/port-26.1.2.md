# 26.1.2 移植记录（release/neoforge-26.1.2）

日期：2026-10-29
目标：Minecraft 26.1.2 / NeoForge 26.1.2.124，mod 版本仍为 0.1.20
产物：`earth-human-neoforge-26.1.2-0.1.20.jar`
SHA-256：`BCA36ACB1A2B7F756AB818D4BE231EE0F3E8FE7131A36D85E032B3036910CA7B`（277,937 B）

## 与 main（26.2）的差异

- `neoForge.version` `26.2.0.93` → `26.1.2.124`；`base.archivesName` 与
  `rootProject.name` 改为 `earth-human-neoforge-26.1.2`，避免与 26.2 产物混淆。
- 26.1.2 没有 `net.minecraft.client.gui.Hud`：`Gui.setScreen` → `Minecraft.setScreen`，
  `Gui.hud.rightHeight` → `Gui.rightHeight`，`Hud.isHidden()` → `Options.hideGui`。
- `neoforge.mods.toml` 要求 NeoForge `[26.1.2.124,)`、Minecraft `[26.1.2]`。
- `pack.mcmeta` 用 `min_format [84,0]` / `max_format [101,1]`，即 26.1.2 `version.json`
  的资源/数据格式。

## 验证证据

- `gradlew build`：BUILD SUCCESSFUL；12 个测试类共 36 项，0 失败 0 错误 0 跳过。
- `tools/validate_release_resources.py`：92 个 JSON、25 条完整物品资源链、双语 key 对齐通过。
- jar 校验：233 条目、89 个 class 均为 major 69、无异常条目名、无用户路径或凭据字样。
- 专用服务器（仅 earth_human）：`Done (1.990s)`，`Earth Human 0.1.20 (earth_human)` 加载，
  日志含 `[Earth Human] NeoForge 26.1.2 module loaded`；`Loaded 1542 recipes`
  （1515 原版 + 27 本 mod）、1617 进度；14 条 WARN 全部来自原版（命令歧义、离线模式），
  无 ERROR/FATAL。
- 部署：`E:\_dx\_Games\MC\xxxxxx\.minecraft\versions\26.1.2-NeoForge_26.1.2.124\mods`
  只新增上述 jar 一个文件（69 → 70 条目），复制后哈希与源一致。

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
