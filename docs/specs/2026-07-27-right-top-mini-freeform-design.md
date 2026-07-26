# 右上角迷你小窗挂起设计

日期：2026-07-27

## 问题

原“挂起”动作调用 `MiuiFreeformModeController.startPinAnimation(...)`。该接口进入 HyperOS pin 模式，会把小窗缩成贴边图标，不符合“保留当前页面并挂到右上角”的目标。

## 设计

- 保留现有动作值 `ACTION_PIN=2`，兼容用户已有的单击、双击配置，不迁移或重置设置。
- 设置页文案改为“挂起到右上角”，明确实际行为。
- 从 tracked task 取得最新 `ActivityManager.RunningTaskInfo`。
- 调用 `MiuiFreeformModeController.lunchSmallFreeformFromRecent(runningTaskInfo, 2)`；HyperOS 源码中 corner position `2` 表示右上角。
- 由系统计算迷你小窗尺寸、安全边距和侧边栏避让，保持应用页面内容并执行 `startEnterMiniShellTransition`。
- 主路径不可用时，仅回退到原生 `fromFreeformToMini(taskId)`；不得回退到 `startPinAnimation`。
- 发出挂起请求后停止模块对该扇形小窗的窗外动作跟踪，避免迷你状态被误关闭。

## 验收

- 代码不再调用 `startPinAnimation`。
- 窗外动作触发后，任务状态为 `windowState=1`。
- `inPinMode=false` 且 `isForegroundPin=false`。
- `smallWindowBounds` 位于屏幕右上角，页面内容仍可见。
- 单击和双击的动作选择、手势返回优先级保持不变。

