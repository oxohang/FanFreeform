# Hyper手势（HyperGesture）

把 HyperOS 原生小窗、应用快捷方式和最近任务装进屏幕边缘手势。通过底角斜滑或侧滑即可选择目标，松手后以小窗或全屏方式启动；也可以进入蜂窝应用总览、Hyper 任务中心或系统任务中心。

当前版本：`1.0`（build 131）

> Hyper手势是依赖 HyperOS 私有接口的 LSPosed 实验性模块，不是小米、魅族或 Apple 的官方项目。不同机型和系统版本的内部接口可能不同，请先阅读兼容性说明并保留可用的模块禁用或救砖方案。



## 主要功能

### 底角扇形

- 一段扇形和二段蜂窝可分别启用，竖屏与横屏独立配置。
- 支持智能排布、固定排布和最多三排的自定义内、中、外排容量。
- 可调整触发区域、分段距离、二段回退距离、选择范围、图标大小和目标数量。
- 呼出、图标旋转、选中放大、取消淡出、速度和扇形阴影均可独立调整。
- 可统一显示选中应用名称、启用震动反馈，或关闭强制圆形图标以保留应用原始轮廓。
<img width="320" height="696" alt="演示" src="https://github.com/user-attachments/assets/c2ecf35b-6e28-4912-a19c-1e4bf94813f2" />



## 使用要求

- 已解锁并取得 root 权限的小米设备。
- 已安装并能正常工作的 LSPosed。
- HyperOS 3；其他版本需要根据兼容性报告单独适配。
- LSPosed 作用域同时勾选：
  - 系统界面：`com.android.systemui`
  - 小米桌面：`com.miui.home`

主要验证环境：小米 `2509FPN0BC`（`popsicle`）、HyperOS `OS3.0.317.0.WPBCNXM`、Android 16。该记录只代表已验证环境，不代表所有 HyperOS 3 版本均兼容。

## 安装与首次配置

1. 正式发布后从 GitHub Releases 获取签名 APK；当前也可以自行构建 Debug APK。
2. 安装后在 LSPosed 中启用 Hyper手势，并勾选系统界面和小米桌面。
3. 打开 Hyper手势，在“杂项设置”点击“一键重载 Hook 进程”。该操作只重载 SystemUI 和小米桌面，不会重启手机。
4. 确认首页 Hook 状态已连接。
5. 分别进入底角、侧滑或蜂窝页面，选择应用与快捷方式。
6. 先使用默认参数测试，再根据手感调整触发距离、安全区域、选择范围和动画。

若隐藏了桌面图标，仍可从 LSPosed 的模块设置入口打开，或使用 `hypergesture://settings` 深层链接。

仓库不跟踪 APK。公开安装包应通过 GitHub Releases 提供，并使用维护者长期保存的正式证书签名。如果设备曾安装其他证书签名的开发版，需要先卸载旧版或继续使用相同证书。

## 兼容性与排查

Hyper手势会注入 SystemUI 和小米桌面，并调用 HyperOS 自由窗口、手势与任务管理私有接口。下列情况可能需要单独适配：

- SystemUI、系统桌面或自由窗口服务类名和方法签名发生变化；
- 系统或目标应用禁止自由窗口；
- 第三方主题、导航模式或其他手势模块改变触摸层级；
- 应用双开、桌面快捷方式目录在当前桌面版本不可读；
- 厂商更新改变状态栏、控制中心、输入法或最近任务行为。

出现问题时：

1. 确认 LSPosed 中两个作用域都已勾选。
2. 在“杂项设置”执行 Hook 热重载。
3. 点击“一键导出兼容性报告”。报告包含系统、桌面、Hook 状态、缺失接口和配置摘要，不包含联系人或快捷方式内容。
4. 使用 [Bug 反馈模板](https://github.com/oxohang/FanFreeform/issues/new?template=bug_report.yml) 提交设备版本、复现步骤、兼容性报告和相关日志。

上传前仍请自行检查报告和日志，移除不希望公开的设备信息。

## 构建

需要 JDK 17 和 Android SDK：

```bash
export JAVA_HOME=/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home
export ANDROID_HOME=/opt/homebrew/share/android-commandlinetools
./gradlew :app:testDebugUnitTest :app:lintDebug :app:assembleDebug
```

Debug APK 位于 `app/build/outputs/apk/debug/`。构建生成的本地归档位于 `release-archive/`，该目录不会提交到 Git。

## 正式签名

Release 构建不会使用 Android 调试证书。通过环境变量或本机未跟踪的 Gradle 属性提供以下值：

```bash
export HYPERGESTURE_KEYSTORE=/absolute/path/to/release.jks
export HYPERGESTURE_STORE_PASSWORD=your_store_password
export HYPERGESTURE_KEY_ALIAS=your_key_alias
export HYPERGESTURE_KEY_PASSWORD=your_key_password
./gradlew :app:assembleRelease
```

未提供完整签名配置时，Gradle 会生成 unsigned release，便于 CI 或本地验证，但不能直接安装发布。不要把 keystore、密码、`local.properties` 或本机 Gradle 配置提交到仓库。

## 项目资料

- [真机接口与历史验收记录](docs/device-verification.md)
- [手势设置结构](docs/specs/2026-07-31-gesture-settings-information-architecture-design.md)
- [扇形多排与快捷方式设计](docs/specs/2026-07-30-shortcut-fan-layout-stationary-honeycomb-design.md)
- [蜂窝全局凸面镜动效](docs/specs/2026-07-29-honeycomb-global-fisheye-design.md)
- [窗外交互设计](docs/specs/2026-07-26-gesture-area-icon-unified-outside-design.md)
- [任务中心动效](docs/specs/2026-07-30-task-center-motion-design.md)

## 许可证

Hyper手势以 [GNU General Public License v3.0](LICENSE) 发布。修改和分发时必须遵守 GPL-3.0 的源代码公开及同许可证要求。
