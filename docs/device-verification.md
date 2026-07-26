# HyperOS 3 真机验收记录

验收日期：2026-07-26

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
- 动画退出：`MiuiFreeformModeAnimation.startExitFreeformShellTransition`
- 全屏任务转小窗：`MulWinSwitchAnimStarter.switchFullscreenToFreeform`
- 可见边界：`MiuiFreeformTaskInfo.getScaledBounds`

## 实机结果

- SystemUI 重启后成功加载模块，手势输入和原生小窗控制器均已连接。
- 底角滑动可显示 `FanFreeformOverlay`，移动选择后以 HyperOS 原生 `windowingMode=5` 启动目标应用。
- 启动后正确关联唯一的扇形小窗任务。
- 外部单击关闭连续命中 `Animated dismiss started`，并由 HyperOS Shell Transition 完成退出，没有触发无动画回退。
- 抖音已有全屏任务 `16533` 时，扇形启动连续命中 `Reusing fullscreen task=16533 ... as freeform`；同一任务原地切换到 `windowingMode=5` 并重新建立跟踪，没有再创建随后消失的临时小窗任务。
- 初始向上轨迹在 32–35px 完成 `claimed` 并提前抢占输入；横向轨迹命中 `yielded to system gesture` 后不显示扇形。
- 配置应用在真机正常显示，能读取 SystemUI 回报的“HyperOS 3 原生接口已连接”状态。

## 自动校验

- `:app:testDebugUnitTest` 通过。
- `:app:lintDebug` 通过，无 Lint issue。
- `:app:assembleDebug` 通过，APK 已安装到上述设备。
- 安装版本：`0.3.1-debug`（versionCode 4）。
- APK SHA-256：`bfd0b67b273f8e1a5d2475b13bd270362ce3f495ab9eaffd67a13db9543a1d3a`。
