# HyperOS 2 底角输入兼容回退设计

## 背景

HyperOS 2 `OS2.0.215.0.VNCCNXM` 的兼容报告表明，模块已经注入
`com.android.systemui`，原生小窗控制器、动画和贴边处理接口也存在，但 ROM 中没有
当前依赖的 `MulWinSwitchEventController`。现有运行时只在该输入控制器创建事件接收器后
初始化，因此输入类缺失会连带禁用底角捕获窗、小窗桥和接口状态上报。

另一台 HyperOS 3 / Android 15 设备使用 build 136，SystemUI 诊断在运行时创建前无法
写入报告，不能排除同一个输入入口缺失问题。本设计只增加兼容回退，不改变已适配
HyperOS 3 的原生输入路径。

## 目标

- 当 ROM 不提供受支持的全局输入控制器时，恢复底角扇形、底角二段蜂窝和松手启动小窗。
- 原生输入控制器存在时继续使用原路径，交互、手势仲裁和 `pilferPointers()` 行为不变。
- 同一触摸流只能由原生输入或底角回退中的一条路径处理，禁止重复触发。
- 兼容报告明确区分运行时、小窗控制器、原生全局输入和底角回退输入状态。
- 所有兼容失败均隔离在模块内部，不允许异常逃逸到 SystemUI 主线程。

## 非目标

- 本版不通过底角回退恢复侧边长滑、压感手势或全屏范围的窗外点击。
- 本版不猜测 HyperOS 2 中不存在于报告里的替代私有类。
- 本版不修改原生小窗控制、动画、任务追踪和 HyperOS 3 手势状态机。

## 方案

### 运行时生命周期解耦

SystemUI `Application.attach()` 后立即、且只初始化一次 `FanRuntime`，不再等待
`MulWinSwitchEventController.createEventReceiver()`。已有小窗控制器实例在运行时创建后
继续注入；外部启动接收器也在运行时创建后安装。

原生输入控制器随后可继续按现有方式注册事件代理。注册期间运行时进入
`SWITCHING_TO_NATIVE` 模式并暂停回退分发；注册成功后进入 `NATIVE_GLOBAL`，注册失败则
恢复 `CORNER_FALLBACK`。控制器始终具有更高优先级。

### 底角窗口事件回退

`FanTriggerCapture` 增加触摸回调，将窗口收到的 `MotionEvent` 以屏幕坐标交给
`FanRuntime.onMotion(event, null)`。该回调只在运行时处于 `CORNER_FALLBACK` 模式时启用。

底角捕获窗本身已经是本次触摸的目标窗口，因此回退路径不需要
`InputMonitor.pilferPointers()`。现有状态机在 `triggerCapture.isCapturing()` 为真时已经支持
无 InputMonitor 的底角方向判断、扇形/蜂窝跟手、松手选择和小窗启动。

### HyperOS 3 不变条件

- 原生事件代理注册前，先原子地把输入模式切换为 `SWITCHING_TO_NATIVE` 并暂停回退分发。
- 注册成功后切换为 `NATIVE_GLOBAL`；注册抛错时恢复 `CORNER_FALLBACK`。
- `NATIVE_GLOBAL` 模式下底角窗口继续负责输入优先级，但不再向运行时转发事件。
- 原生事件代理保持唯一的状态机输入来源，继续传入 ROM 的 `InputMonitor`。
- 重复调用运行时初始化或原生输入就绪通知必须幂等。
- 现有输入控制器候选、反射方法发现和 ROM 扫描逻辑不删除、不降级。

## 状态与诊断

运行时记录以下能力状态并写入现有兼容报告日志：

- `runtime=ready`：SystemUI 上下文已取得，配置和窗口控制器可初始化。
- `freeform=ready|waiting`：原生小窗控制器是否已经连接。
- `native_input=ready|missing|waiting`：ROM 全局输入控制器状态。
- `corner_fallback=ready|unavailable|inactive`：底角捕获窗是否承担事件输入。

用户界面的接口摘要保持简短：原生输入和小窗都存在时显示现有“HyperOS 原生接口已连接”；
仅回退输入可用时显示“底角兼容输入已连接”；窗口挂载失败时显示“底角兼容输入不可用”。

## 错误处理

- 运行时构造、捕获窗挂载和回调分发分别捕获异常并写入报告。
- 捕获窗挂载失败时不尝试伪造全局输入，也不拦截系统手势。
- 原生输入在回退运行后变为可用时，先关闭回退分发，再注册原生事件代理。
- 回退触摸回调复制事件生命周期内需要的数据，不保存或异步使用已回收的
  `MotionEvent`。

## 验证

### 自动测试

- 输入模式状态机：初始回退、切换中暂停、注册成功接管、注册失败恢复、重复通知和禁止双路分发。
- 底角捕获回调：回退模式转发，原生模式不转发。
- 运行时重复初始化和小窗控制器先后到达的幂等行为。
- 现有手势、配置和小窗桥测试全部通过。

### 构建检查

- 单元测试、lint、debug APK 和 unsigned release APK 全部通过。
- 检查 APK 的版本号、签名状态、包名和 Xposed 作用域资源。

### 日志验收

HyperOS 2 新报告至少应出现：

1. `runtime=ready`。
2. `freeform=ready`。
3. `native_input=missing`。
4. `corner_fallback=ready`，并在触摸后记录底角 `DOWN`、armed、claimed 和 `UP`。

HyperOS 3 新报告应出现 `native_input=ready`、`corner_fallback=inactive`，且原有输入处理、
底角、侧滑、压感和窗外交互无行为变化。

## 交付边界

本地自动测试和构建成功只能证明代码与 APK 完整；HyperOS 2 的真实窗口权限、事件连续性和
小窗启动结果仍需用户安装后通过操作结果及下一份兼容报告确认。
