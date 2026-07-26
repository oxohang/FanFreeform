# HyperOS 3 真机验收记录

验收日期：2026-07-27

## 设备

- 型号：小米 2509FPN0BC（`popsicle`）
- 系统：HyperOS `OS3.0.317.0.WPBCNXM`
- Android：16（API 36）
- SystemUI：`16.03.251211.r`
- LSPosed 作用域：`com.android.systemui`

## 已确认的 ROM 接口

- 全局输入：`MulWinSwitchEventController.registerEventHandler`
- 原生小窗参数：`MiuiFreeFormManager.getActivityOptions`
- 小窗任务管理：`MiuiFreeformModeController`
- 标题栏同款关闭：`MulWinSwitchAnimStarter.closeFullOrFreeform`
- 原生关闭转场：`ANIMATION_WIN_CTL_CLOSE_FREEFORM`（11117）
- 可见边界：`MiuiFreeformTaskInfo.getScaledBounds`

## 实机结果

- SystemUI 重启后成功加载模块，手势输入和原生小窗控制器均已连接。
- 底角滑动可显示 `FanFreeformOverlay`，移动选择后以 HyperOS 原生 `windowingMode=5` 启动目标应用。
- 启动后正确关联唯一的扇形小窗任务。
- 统一使用带小窗 `ActivityOptions` 的 `NEW_TASK` 启动请求；哔哩哔哩旧任务 `16541` 返回 `result code=2` 并直接进入 `windowingMode=5`，没有调用会重排桌面的全屏转小窗接口。
- 微信任务 `16466` 处于前台时，扇形打开计算器和今日头条均保留微信作为小窗背景；窗外关闭后系统直接将微信提回前台，没有经过桌面。
- 外部单击关闭命中 `Native caption-close transition started`，创建 11117 转场并执行 `startCloseFullOrFreeformAnim`，与系统标题栏关闭按钮使用相同链路，没有触发无动画回退。
- `0.3.3` 将窗外关闭从 SystemUI 主线程 `26683` 投递到 `mMainExecutor` 的 Shell 线程 `26767`；实机关闭时两次任务更新均命中 `skip updateBaseAnimParam`，直到 11117 动画完成后才写入全屏 bounds，避免自定义位置先被覆盖而产生 A 点到 B 点的首帧跳动。
- `0.3.4` 将“挂起”改为 `lunchSmallFreeformFromRecent(task, 2)`。今日头条和哔哩哔哩均进入右上角迷你小窗：`windowState=1`、`freeFormScale=0.25`、`smallWindowBounds=[860,166][1160,646]`，且 `inPinMode=false`、`isForegroundPin=false`。
- 抖音、哔哩哔哩和今日头条均可重复复用旧任务，不再创建随后消失的临时小窗任务。
- 初始向上轨迹在 32–35px 完成 `claimed` 并提前抢占输入；横向轨迹命中 `yielded to system gesture` 后不显示扇形。
- 配置应用在真机正常显示，能读取 SystemUI 回报的“HyperOS 3 原生接口已连接”状态。

## 自动校验

- `:app:testDebugUnitTest` 通过。
- `:app:lintDebug` 通过，无 Lint issue。
- `:app:assembleDebug` 通过，APK 已安装到上述设备。
- 安装版本：`0.3.4-debug`（versionCode 7）。
- APK SHA-256：`7eec92bbd3bd549299bce70afc1022ff4617f30e59e388a30f9a3049999ee006`。
