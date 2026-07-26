# 随用随走（FanFreeform）

面向 HyperOS 3 的 LSPosed 模块：从屏幕左下角或右下角向中心滑动，呼出扇形应用菜单，松手以系统原生小窗打开应用。仅由该手势打开的小窗会在第一次点击窗外时关闭，并消费该次点击。

目标测试环境：HyperOS `OS3.0.317.0.WPBCNXM`、Android 16、`com.android.systemui` `16.03.251211.r`。

## 已实现

- 左右底角扇形手势，滑动选择、松手启动。
- 3–8 个用户自定义应用，支持搜索、添加、移除和拖动排序。
- 独立震动开关和可调触发距离。
- 全局小窗宽度、高度、水平位置和垂直位置，含可视化预览。
- 只追踪扇形手势启动的原生小窗；第一次窗外按下被消费并关闭小窗。
- 切换全屏、迷你窗或贴边后自动停止追踪，不影响其他小窗。

## 构建

```bash
export JAVA_HOME=/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home
export ANDROID_HOME=/opt/homebrew/share/android-commandlinetools
./gradlew :app:assembleDebug
```

## 安装

安装 APK 后，在 LSPosed 中启用模块并勾选“系统界面（com.android.systemui）”，随后重启 SystemUI 或手机。打开“随用随走”配置应用，选择 3–8 个应用并调整参数。

详细行为与验收标准见 [设计规格](docs/specs/2026-07-26-hyperos-fan-freeform-design.md)。
已验证的设备、接口和操作链路见 [真机验收记录](docs/device-verification.md)。
