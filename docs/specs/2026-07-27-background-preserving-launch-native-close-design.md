# 保留后台应用的小窗启动与系统同款关闭动画设计

日期：2026-07-27

## 问题结论

真机日志已确认上一版使用了两条不符合目标体验的 HyperOS 路径：

1. 已有全屏任务通过 `MulWinSwitchAnimStarter.switchFullscreenToFreeform(...)` 转换时，内部 `MulWinSwitchTransition.startToFreeform(...)` 会在类型 `11100` 下主动执行 `reorder(homeTask.token, true)`。因此从微信呼出哔哩哔哩小窗时，微信被隐藏，桌面成为小窗后层。
2. 窗外单击关闭使用的 `MiuiFreeformModeAnimation.startExitFreeformShellTransition(...)` 是“退出小窗模式”路径，并非小窗标题栏关闭按钮的动画。系统标题栏关闭按钮实际调用 `MulWinSwitchAnimStarter.closeFullOrFreeform(...)`，对应 `ANIMATION_WIN_CTL_CLOSE_FREEFORM`（11117）。

## 目标

- 从任意前台应用呼出目标小窗时，原前台应用保持在小窗后层，不经过桌面。
- 关闭小窗后直接回到原前台应用的原界面。
- 抖音、哔哩哔哩等已有全屏任务继续原地复用，避免创建后立即消失的临时任务。
- 窗外关闭使用与 HyperOS 小窗标题栏“关闭”按钮完全相同的系统动画路径。
- 继续应用用户配置的全局小窗尺寸和位置。

## 非目标

- 不修改扇形手势、图标命中或方向仲裁。
- 不修改窗外单击和双击的动作配置。
- 不增加设置项。
- 不强制结束目标应用进程，也不清理应用数据。

## 启动设计：统一任务启动路径

### 启动参数

所有扇形启动统一使用 `MiuiFreeFormManager.getActivityOptions(...)` 返回的 HyperOS 小窗参数，并写入当前全局尺寸与位置生成的 launch bounds。

Launcher Intent 仅保留 `FLAG_ACTIVITY_NEW_TASK`，删除 `FLAG_ACTIVITY_MULTIPLE_TASK`：

- 没有旧任务时，系统创建新的 `windowingMode=5` 小窗任务。
- 已有同应用任务时，Android 按任务 affinity/launchMode 复用原任务，并在同一次启动中应用小窗 ActivityOptions。
- 不再调用会把 Home 提到前台的 `switchFullscreenToFreeform(...)`。

### 任务匹配与失败处理

- 启动前仍记录待匹配包名和时间。
- `onTaskAppeared`、`onTaskModeChanged` 和延迟扫描确认包名相同且进入普通小窗状态后，建立 tracked task。
- 若目标应用拒绝复用或没有进入小窗，记录明确日志并停止本次跟踪；不得回退到会重排 Home 的旧 Shell 转换路径。
- 私有接口异常不得影响 SystemUI，当前前台应用保持不变。

### 层级要求

验收时以任务层级为准：小窗出现后，目标任务为 `windowingMode=5`；小窗后方仍是触发手势前的前台任务。不得出现 Home/Launcher 在两者之间，也不得先切到桌面再显示小窗。

## 关闭设计：接入标题栏同款路径

窗外动作配置为“关闭”时：

1. 通过 tracked task ID 从 `mMultiTaskingTaskRepository` 取得 `MiuiFreeformModeTaskInfo`。
2. 从其中取得真实的 `ActivityManager.RunningTaskInfo`。
3. 将关闭任务投递到 `MiuiFreeformModeController.mMainExecutor`，与系统标题栏点击使用相同的 Shell 线程和事件顺序。
4. 在 Shell 线程从 `MiuiFreeformModeController.mMulWinSwitchAnimStarter` 取得系统动画启动器并调用 `closeFullOrFreeform(runningTaskInfo)`。
5. HyperOS 创建类型 11117 的 Shell Transition，执行与标题栏关闭按钮相同的缩小、淡出、阴影和圆角动画，并把小窗任务移到后台。
6. 投递系统转场后停止模块侧的窗外手势跟踪；任务消失回调负责完成系统状态清理。

关闭调用不能直接在 SystemUI 主线程执行。否则 WindowContainerTransaction 将小窗恢复为全屏 bounds/scale 的任务更新，可能抢先覆盖 `MultiTaskingFolmeControl` 保存的当前小窗起点，导致动画首帧从自定义位置 A 跳到系统计算位置 B。串行投递到 Shell 主执行器后，转场先登记为运行中，后续任务更新不会覆盖动画起点。

若 RunningTaskInfo、动画启动器或方法不可用，则记录回退日志并调用原有 `exitFreeformTask(taskId, true)`，确保关闭动作仍能完成。

## 删除的错误路径

- 删除已有全屏任务扫描后调用 `switchFullscreenToFreeform(...)` 的分支。
- 删除窗外关闭调用 `startExitFreeformShellTransition(...)` 的分支。
- 保留相关日志字段但更名，避免把“退出小窗模式”误报为“系统关闭动画”。

## 日志

关键成功日志：

- `Launching or reusing ... in freeform without home reorder`
- `Tracking fan-launched task=... package=...`
- `Native caption-close scheduled on Shell thread task=...`
- `Native caption-close transition started on Shell thread task=...`

关键失败日志：

- `No matching freeform task appeared for ...`
- `Native caption-close unavailable; falling back to immediate exit`

## 验收标准

### 自动检查

- Launcher Intent 不包含 `FLAG_ACTIVITY_MULTIPLE_TASK`。
- 代码中不再调用 `switchFullscreenToFreeform(...)`。
- 关闭优先调用 `closeFullOrFreeform(...)`，异常时才调用无动画退出。
- 现有手势、图标命中、窗外动作和配置迁移测试全部通过。
- `testDebugUnitTest`、`lintDebug`、`assembleDebug` 全部通过。

### 真机检查

1. 微信处于前台，从扇形打开没有旧任务的应用：微信保持在小窗后面。
2. 微信处于前台，从扇形打开已有全屏任务的哔哩哔哩：不显示桌面，哔哩哔哩直接成为小窗。
3. 对抖音重复执行“打开小窗 → 关闭 → 再次打开”：复用同一任务，不出现临时任务消失。
4. 点击窗外关闭：日志出现 `ANIMATION_WIN_CTL_CLOSE_FREEFORM` 或 transition type 11117，视觉效果与点击系统小窗标题栏关闭按钮一致。
5. 关闭后立即显示触发前的微信原界面，不经过桌面。
6. SystemUI 无崩溃，现有手势方向仲裁行为不变。
