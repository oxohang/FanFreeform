# Hyper手势（HyperGesture）

Hyper手势是面向小米 HyperOS 3 的 LSPosed 模块。它通过底角斜滑、侧滑和蜂窝应用总览提供快捷启动，并可调用 HyperOS 原生自由窗口能力，让应用以小窗、全屏或最近任务形式打开。

> 本项目依赖 HyperOS 私有接口，兼容性与系统版本、SystemUI 和系统桌面实现有关。目前主要在 Android 16 / HyperOS 3 上开发和验证，不保证适用于其他 Android 系统，也不是小米、魅族或 Apple 的官方项目。

## 主要功能

- 底角斜滑：一段扇形应用选择、二段蜂窝应用总览，横竖屏可分别配置。
- 侧滑手势：纵向列表、环形、扇形、蜂窝、Hyper 任务中心和系统任务中心等布局。
- 应用与快捷方式：各手势使用独立目标清单，支持批量选择、排序和应用快捷方式。
- 蜂窝动效：壁纸模糊背景、跟手或固定位置、聚焦放大、边缘压缩和惯性调节。
- 小窗控制：复用已有任务，支持竖屏与横屏独立位置、系统原生比例缩放和全屏启动。
- 窗外交互：单击、双击动作可配置；输入法、状态栏、控制中心和窗口移动期间进行触摸仲裁。
- 任务切换：最近任务卡片或图标模式，并可跳转 HyperOS 系统任务中心。
- 兼容性报告：导出系统版本、Hook 状态和缺失接口信息，便于排查不同 HyperOS 版本。

## 使用要求

- 已解锁并取得 root 权限的小米设备。
- 已安装并可正常工作的 LSPosed。
- HyperOS 3；其他版本需要根据兼容性报告适配。
- LSPosed 作用域勾选 `com.android.systemui` 和 `com.miui.home`。

这是会注入系统界面与桌面进程的实验性模块。修改或安装前请确保设备具备可用的救砖、ADB 或模块禁用方案。

## 安装与配置

1. 构建并安装 APK。
2. 在 LSPosed 中启用 Hyper手势，并勾选“系统界面”和“小米桌面”。
3. 使用应用内“杂项设置”重启 Hook 进程，或手动重启 SystemUI 与桌面。
4. 在设置页选择应用或快捷方式，再按设备手感调整触发区、距离、动画和小窗位置。

仓库不跟踪 APK。公开安装包应通过 GitHub Releases 提供，并使用项目维护者自己的正式证书签名。若设备曾安装使用其他证书签名的开发版，需要先卸载旧版或继续使用相同证书。

## 构建

需要 JDK 17 和 Android SDK：

```bash
export JAVA_HOME=/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home
export ANDROID_HOME=/opt/homebrew/share/android-commandlinetools
./gradlew :app:testDebugUnitTest :app:lintDebug :app:assembleDebug
```

Debug APK 位于 `app/build/outputs/apk/debug/`。构建过程生成的本地归档位于 `release-archive/`，该目录不会提交到 Git。

## 正式签名

正式构建不会使用 Android 调试证书。通过环境变量或本机未跟踪的 Gradle 属性提供以下值：

```bash
export HYPERGESTURE_KEYSTORE=/absolute/path/to/release.jks
export HYPERGESTURE_STORE_PASSWORD=your_store_password
export HYPERGESTURE_KEY_ALIAS=your_key_alias
export HYPERGESTURE_KEY_PASSWORD=your_key_password
./gradlew :app:assembleRelease
```

未提供完整签名配置时，Gradle 仍可生成 unsigned release，便于 CI 验证，但不能直接安装发布。不要把 keystore、密码或本机 `gradle.properties` 提交到仓库。

## 兼容性反馈

不同 HyperOS 版本出现无法触发或无法小窗时，请在“杂项设置”中导出兼容性报告，并在 GitHub Issue 中附上：

- 设备型号、HyperOS 完整版本和 Android 版本；
- LSPosed 版本及模块作用域；
- 可复现步骤、预期行为和实际行为；
- 兼容性报告，以及问题发生时的相关日志。

报告设计为不包含联系人和快捷方式内容。提交前仍请自行检查并移除不希望公开的设备信息。

## 开发资料

- [真机验收记录](docs/device-verification.md)
- [手势设置结构](docs/specs/2026-07-31-gesture-settings-information-architecture-design.md)
- [蜂窝全局动效](docs/specs/2026-07-29-honeycomb-global-fisheye-design.md)
- [窗外交互设计](docs/specs/2026-07-26-gesture-area-icon-unified-outside-design.md)
- [任务中心动效](docs/specs/2026-07-30-task-center-motion-design.md)

## 许可证

本项目以 [GNU General Public License v3.0](LICENSE) 发布。修改和分发时请遵守 GPL-3.0 的源代码公开及同许可证要求。
