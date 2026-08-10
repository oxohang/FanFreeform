# 随用随走（FanFreeform）

面向 HyperOS 3 的 LSPosed 模块：通过左右底角斜滑或整侧长滑呼出扇形应用菜单，松手以系统原生小窗打开应用。仅由该手势打开的小窗会响应可配置的窗外单击和双击操作。

目标测试环境：HyperOS `OS3.0.317.0.WPBCNXM`、Android 16、`com.android.systemui` `16.03.251211.r`。

## 已实现

- 左右底角扇形手势，滑动选择、松手启动。
- 可选的左右整侧距离手势：短滑保留 HyperOS 原生返回，长滑取消返回并展开与侧滑方向匹配的扇形；与底角斜滑同时可用。
- 3–24 个用户自定义应用，支持搜索、添加、移除和拖动排序。
- 应用选择器包含可启动的系统应用；双开应用使用双圆徽标，快捷方式使用闪电徽标。
- 独立震动开关，可调触发距离、底角触发区宽高、选择半径和图标大小。
- 全局小窗宽度、高度、水平位置和垂直位置，含可视化预览。
- 圆形满铺应用图标，松手必须命中图标才启动，空白处松手直接取消。
- 窗外单击和双击可分别配置为关闭、全屏或无操作；左右系统返回手势优先。
- 统一启动并复用已有应用任务，保留触发手势前的应用作为小窗背景，不再先回桌面。
- 窗外关闭接入 HyperOS 小窗标题栏同款 Shell Transition，并在 Shell 线程按实际位置连续播放，关闭后直接回到原应用。
- 底部手势采用早期方向仲裁：明确上滑提前接管，横滑永久交还系统。
- 切换全屏、迷你窗或贴边后自动停止追踪，不影响其他小窗。

## 构建

```bash
export JAVA_HOME=/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home
export ANDROID_HOME=/opt/homebrew/share/android-commandlinetools
./gradlew :app:assembleDebug
```

如需将 APK 归档到 `release-archive/`，显式执行：

```bash
./gradlew :app:assembleDebug -ParchiveApk=true
```

## 安装

安装 APK 后，在 LSPosed 中启用模块并勾选“系统界面（com.android.systemui）”，随后重启 SystemUI 或手机。打开“随用随走”配置应用，选择 3–24 个应用并调整参数。

详细行为与验收标准见 [基础设计](docs/specs/2026-07-26-hyperos-fan-freeform-design.md)、[交互优化](docs/specs/2026-07-26-interaction-optimization-design.md)、[扇形与窗外交互](docs/specs/2026-07-26-gesture-area-icon-unified-outside-design.md)、[任务复用与手势仲裁](docs/specs/2026-07-26-task-reuse-exit-animation-gesture-arbitration-design.md)、[后台保留与原生关闭](docs/specs/2026-07-27-background-preserving-launch-native-close-design.md) 和 [侧滑距离扇形手势](docs/specs/2026-07-27-side-distance-fan-gesture-design.md)。
已验证的设备、接口和操作链路见 [真机验收记录](docs/device-verification.md)。

## 问题反馈

请通过 [GitHub Issues](https://github.com/oxohang/FanFreeform/issues) 提交问题。应用内可进入“杂项设置 → 提交 Bug 反馈”，自动生成设备、系统、模块版本、Hook 状态和兼容性诊断摘要；提交时只需补充复现步骤、预期结果和实际结果。请不要提交联系人、账号或快捷方式名称等隐私信息。
