# 旧任务复用、退出动画与底部手势仲裁设计

## 背景与问题结论

本轮处理三个相互独立但共同影响“小窗随用随走”体验的问题：

1. 抖音第一次能够以小窗打开，执行全屏操作后再次从扇形启动时，小窗任务短暂出现后消失。
2. 点击小窗外关闭时使用了 HyperOS 的无动画退出接口，窗口瞬间消失。
3. 扇形候选手势达到完整触发距离后才调用 `pilferPointers()`，此时系统底部横滑切换可能已经开始，导致扇形和应用切换同时发生。

设备日志和 `dumpsys activity` 已确认抖音问题不是权限失效：抖音的旧任务 `16533` 被模块全屏化后继续保留；后续新建的小窗启动任务 `16543` 等被抖音的任务复用策略合并回旧的全屏任务，因此新小窗立即消失。

## 目标

- 从扇形再次启动已有全屏任务的应用时，优先把旧任务原生转换回小窗。
- 点击外部关闭小窗时使用 HyperOS 原生退出过渡动画。
- 触发区域继续紧贴屏幕底边，不改变用户现有的宽度、高度和触发距离配置。
- 系统横向切换手势优先；一旦早期轨迹被判定为横滑，本次触摸不得再转成扇形。

## 非目标

- 不上移触发区域，不增加底部避让带。
- 不增加新的设置项。
- 不强制结束应用进程，也不清除应用任务或数据。
- 不为窗口制作截图遮罩或自绘伪动画。

## 方案一：已有任务优先转换

### 启动决策

扇形选中应用并松手后，按以下顺序处理：

1. 通过 SystemUI 已持有的 `ShellTaskOrganizer.getRunningTasks(displayId)` 查询当前屏幕任务，寻找包名相同、处于全屏模式且仍有有效顶层或根 Activity 的任务。
2. 找到时不再启动新的 Launcher Activity；通过 HyperOS WMShell 的 `MulWinSwitchAnimStarter.switchFullscreenToFreeform(...)` 将该任务原生转换为小窗。
3. 复用任务转换完成后，沿用 HyperOS 的小窗 memory bounds；本模块首次启动该任务时写入的自定义尺寸和位置会由系统记忆并在转换时恢复。若系统没有记忆值，则使用 HyperOS 的默认小窗 bounds，避免在转换动画中途强行修改 bounds。
4. 未找到可复用全屏任务时，继续使用当前 `MiuiFreeFormManager.getActivityOptions(...)` 加自定义 bounds 的新任务启动路径。

### 跟踪规则

- 复用转换前写入待匹配包名和开始时间。
- `onTaskModeChanged` 或延迟扫描确认该任务进入普通小窗状态后，将原任务 ID 设置为本模块的 tracked task。
- 原生转换调用失败时，记录明确日志并回退到现有的新任务启动路径。
- 查询任务失败时同样安全回退，不阻断其他应用启动。

### 选择该接口的原因

ROM 反编译代码表明 HyperOS 自身的全屏转小窗路径最终调用 `switchFullscreenToFreeform(...)`，它负责系统级窗口模式切换和配套动画。直接复用该路径比强制结束旧任务或反复创建新任务更符合抖音等 singleTask/singleTop 应用的任务模型。

## 方案二：原生退出过渡动画

当前关闭路径调用 `MiuiFreeformModeController.exitFreeformTask(taskId, true)`；该方法在本 ROM 内部最终走 `exitFreeformWithoutAnimation(...)`，因此表现生硬。

新的关闭路径：

1. 从 `mMultiTaskingTaskRepository` 取得 tracked task 的 `MiuiFreeformModeTaskInfo`。
2. 从控制器取得 `mMiuiFreeformModeAnimation`。
3. 调用 ROM 已用于批量关闭小窗的 `startExitFreeformShellTransition(taskInfo, false)`。
4. 该接口由 WMShell 操作真实窗口 Surface，使用 HyperOS 自带的缩小、淡出和圆角退出过渡。
5. 若动画对象、任务信息或方法在运行时不可用，则回退到原有的无动画关闭，确保“关闭”动作一定完成。

启动动画后立即停止外部手势跟踪，最终由 `onTaskVanished` 完成状态清理，避免动画期间重复触发关闭。

## 方案三：两阶段手势仲裁

### 状态机

扇形手势状态从原来的 `IDLE -> ARMED -> ACTIVE` 改为：

```text
IDLE -> ARMED -> CLAIMED -> ACTIVE
          |          |
          +-> YIELDED+-> cancelled
```

- `ARMED`：按下点位于用户配置的底部左右热区，但尚未抢占触摸。
- `YIELDED`：早期轨迹不是明确的向上手势，本次触摸永久交给系统，直到抬手。
- `CLAIMED`：早期轨迹明确向上，模块立即调用 `pilferPointers()`，但尚未显示扇形。
- `ACTIVE`：总移动距离达到用户设置的“触发距离”，显示扇形并允许选择图标。

### 方向判定

- 判定距离使用 `max(ViewConfiguration.scaledTouchSlop, 10dp)`，比完整扇形触发距离更早。
- 使用从按下点到当前点的累计位移，而不是单个 MOVE 的瞬时方向。
- 当向上位移为正，且 `upward >= abs(horizontal) * 1.15` 时，判为扇形意图并进入 `CLAIMED`。
- 达到判定距离但不满足上述条件时，进入 `YIELDED`。横向、向下和模糊对角轨迹均优先交给系统。
- `YIELDED` 不允许因为后续手指转向而重新进入扇形，解决“先横滑、后斜上滑仍呼出扇形”的问题。
- `CLAIMED` 后才允许继续向图标方向自由移动；后半段不再受初始方向角限制。

### 触发距离与取消

- 用户现有“手势触发距离”只负责决定何时从 `CLAIMED` 进入 `ACTIVE`，不再承担方向仲裁。
- 在 `CLAIMED` 状态未达到触发距离就抬手：不显示扇形、不打开应用。
- 多指、系统取消、息屏或锁屏仍按现有规则安全取消。
- 因为系统在约 10dp 的早期阶段就收到取消事件，而不是等到完整触发距离，横条应用切换不会与扇形同时完成。

## 日志与故障回退

新增或调整关键日志：

- `Reusing fullscreen task=... package=... as freeform`
- `Fullscreen task reuse failed; falling back to new launch`
- `Animated dismiss started task=...`
- `Animated dismiss unavailable; falling back to immediate exit`
- `Fan candidate claimed after upward arbitration`
- `Fan candidate yielded to system gesture`

所有 ROM 私有接口均通过反射调用并捕获异常；单个接口失效不得导致 SystemUI 崩溃。

## 验收标准

### 自动测试

- 方向判定：明确上滑进入 CLAIMED；横滑、下滑和模糊对角线进入 YIELDED。
- 不可反悔：YIELDED 后即使轨迹转为向上也不能激活扇形。
- 分离距离：早期判定距离与用户配置的完整触发距离互不替代。
- 原有图标命中、热区、外部单击/双击逻辑测试继续通过。
- `testDebugUnitTest`、`lintDebug` 和 `assembleDebug` 全部通过。

### 真机验证

1. 抖音从无任务状态由扇形打开为小窗。
2. 将抖音全屏后再次由扇形选择抖音，原任务转换回小窗，不出现瞬时空任务。
3. 点击小窗外关闭，能看到连续的 HyperOS 原生退出动画，任务最终消失。
4. 从左右底角进行横向应用切换，只发生系统切换，不显示扇形。
5. 从左右底角先明确向上，再滑向扇形图标，系统横向切换不启动，命中图标后正常打开小窗。
6. 向上认领后未达到配置触发距离就松手，不显示扇形且不启动应用。
