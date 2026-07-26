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
- 窗口退出：`MiuiFreeformModeController.exitFreeformTask`
- 可见边界：`MiuiFreeformTaskInfo.getScaledBounds`

## 实机结果

- SystemUI 重启后成功加载模块，手势输入和原生小窗控制器均已连接。
- 底角滑动可显示 `FanFreeformOverlay`，移动选择后以 HyperOS 原生 `windowingMode=5` 启动目标应用。
- 启动后正确关联唯一的扇形小窗任务。
- 首次点击该小窗外部时，输入被抢占，日志确认执行 `Dismissed fan-launched task`；目标小窗关闭。
- 配置应用在真机正常显示，能读取 SystemUI 回报的“HyperOS 3 原生接口已连接”状态。

## 自动校验

- `:app:testDebugUnitTest` 通过。
- `:app:lintDebug` 通过，无 Lint issue。
- `:app:assembleDebug` 通过，APK 已安装到上述设备。
