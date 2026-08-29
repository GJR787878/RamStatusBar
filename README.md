<div align="center">

# RamStatusBar

**LSPosed 模块，在 Android 状态栏时钟位置实时显示剩余内存。**
*An LSPosed module that shows remaining RAM on the Android status bar clock.*

**点击可循环查看 CPU / GPU 占用与温度。**
*Tap to cycle through CPU / GPU usage & temperature.*

**支持三种显示模式：仅时间、时间 + 内存、仅内存。**
*Three display modes: Time only, Time + RAM, RAM only.*

**内置 HSV 取色盘，可自定义胶囊背景颜色，并附带深度休眠统计。**
*Built-in HSV color picker for the capsule background, plus deep sleep stats.*

**三页底部导航：主页 / 配置 / 设置，玻璃质感 UI。**
*Three-tab bottom navigation: Home / Config / Settings, with glassmorphism UI.*

**支持中文 / English / Русский 三种语言。**
*Supports three languages: 中文 / English / Русский.*

**纯 Hook 方案，无需修改系统，切换模式约 1 秒生效。**
*Pure Hook approach — no system modification, mode switch takes effect in ~1 second.*

</div>

---

## 📖 项目介绍 / Introduction

`RamStatusBar` 是一个基于 **LSPosed / Xposed** 框架的 Android 系统 UI 定制模块。它接管状态栏的时钟区域，在原本只显示时间的地方，额外显示**可用 / 总内存**（例如 `2.5G/8G`），并支持通过点击时钟快速查看 **CPU / GPU 占用率与温度**。

`RamStatusBar` is an Android SystemUI customization module based on the **LSPosed / Xposed** framework. It takes over the status bar clock area, showing the **available / total RAM** (e.g. `2.5G/8G`) in addition to the time, and lets you quickly check **CPU / GPU usage & temperature** by tapping the clock.

- **无需修改系统**：纯 Hook 方案，通过 Xposed 框架注入 SystemUI
  *No system modification: pure Hook approach, injected into SystemUI via the Xposed framework.*
- **即时生效**：切换显示模式无需重启手机，最多等 1 秒
  *Instant effect: switching display modes takes effect within ~1 second, no reboot needed.*
- **所见即所得**：内置 HSV 取色盘，可自由定制状态栏胶囊的背景颜色、亮度与透明度
  *WYSIWYG: built-in HSV color picker to freely customize the capsule background color, brightness and transparency.*
- **三页底部导航**：主页（介绍 + 深度休眠）、配置（显示模式）、设置（背景颜色 + 语言），玻璃质感 UI
  *Three-tab bottom nav: Home (intro + deep sleep), Config (display modes), Settings (background + language), with glassmorphism UI.*
- **多语言支持**：内置中文 / English / Русский 三种语言，设置页一键切换
  *Multilingual: built-in 中文 / English / Русский, switchable from the Settings page.*

> ⚠️ 需要 **Root** 权限 + **LSPosed / Vector Manager** 环境。
> ⚠️ Requires **Root** access + **LSPosed / Vector Manager** environment.

---

## 📸 截图展示 / Screenshots

<div align="center">

**主页 / Home（软件介绍 + 深度休眠）**
![Home](https://raw.githubusercontent.com/GJR787878/RamStatusBar/main/docs/images/screenshot_home.png)

**配置页 / Config（显示模式选择）**
![Config](https://raw.githubusercontent.com/GJR787878/RamStatusBar/main/docs/images/screenshot_config.png)

**设置页 / Settings（背景颜色 + 语言切换）**
![Settings](https://raw.githubusercontent.com/GJR787878/RamStatusBar/main/docs/images/screenshot_settings.png)

**颜色设置页 / Background Color**
![Color page](https://raw.githubusercontent.com/GJR787878/RamStatusBar/main/docs/images/screenshot_color_page.png)

**HSV 取色器 / HSV Color Picker**
![Color picker](https://raw.githubusercontent.com/GJR787878/RamStatusBar/main/docs/images/screenshot_color_picker_dialog.png)

</div>

---

## ✨ 功能特性 / Features

### 1. 三种显示模式 / Three Display Modes

在应用内一键切换，约 1 秒生效，无需重启。
Switch between modes in-app with one tap — takes effect in ~1 second, no reboot.

| 模式 / Mode | 效果 / Effect | 示例 / Example |
|------|------|------|
| **仅时间** / Time only | 只显示当前时间（还原原生时钟）<br>Shows the time only (native clock) | `21:11` |
| **时间 + 内存** / Time + RAM | 时间 + 可用/总内存（默认）<br>Time + available/total RAM (default) | `21:11 2.5G/8G` |
| **仅内存** / RAM only | 只显示内存占用<br>Shows RAM usage only | `2.5G/8G` |

- 可用内存实时刷新（约每秒一次）
  Available RAM refreshes in real time (~every second).
- 总内存自动检测，并向上取整到最接近的常见规格（8 / 12 / 16 / 24G 等）
  Total RAM is auto-detected and rounded up to the nearest common spec (8 / 12 / 16 / 24G, etc.).

### 2. 点击时钟查看 CPU / GPU / Tap the Clock for CPU / GPU

点击状态栏的时钟文字：
Tap the status bar clock text:

- **第 1 次点击** → 显示 **CPU 占用率 + 温度**（如 `CPU14% 43°C`）
  *1st tap* → shows **CPU usage + temperature** (e.g. `CPU14% 43°C`)
- **第 2 次点击** → 显示 **GPU 占用率 + 温度**（如 `GPU12% 40°C`）
  *2nd tap* → shows **GPU usage + temperature** (e.g. `GPU12% 40°C`)
- **第 3 次点击** → 回到正常显示
  *3rd tap* → returns to normal display
- **10 秒内无操作** → 自动回到正常显示
  *No touch for 10s* → automatically returns to normal

> GPU 占用依赖芯片私有接口（KGSL / Mali sysfs 路径），部分设备可能显示 `GPU N/A`，能否读取取决于芯片型号。
> GPU usage relies on chip-specific interfaces (KGSL / Mali sysfs paths). Some devices may show `GPU N/A` — readability depends on the chipset.

### 3. 深度休眠统计 / Deep Sleep Stats

应用内直接显示**开机以来深度休眠时长与占比**（如 `4:56:35 (48%)`），与系统"关于本机"中的数值一致，无需再进系统设置翻找，方便判断待机省电是否正常。

Shows the **deep sleep duration and ratio since boot** (e.g. `4:56:35 (48%)`) right in the app, matching the figure in "About Phone" — no need to dig through system settings, handy for judging whether standby power saving is healthy.

### 4. 背景颜色自定义 / Background Color Customization

内置完整的 **HSV 取色器** / Built-in full **HSV color picker**:

- 圆形色相环，拖动即可选色
  *Circular hue wheel, drag to pick a color.*
- **亮度**、**透明度**两个滑块独立调节
  *Independent **Brightness** and **Opacity** sliders.*
- 6 个预设色（黑 / 白 / 灰 / 蓝 / 绿 / 红）快速选择
  *6 preset colors (black / white / gray / blue / green / red) for quick selection.*
- 当前色 ↔ 新颜色实时对比，十六进制（`#AARRGGBB`）显示
  *Live Current ↔ New comparison with HEX (`#AARRGGBB`) display.*
- 状态栏胶囊背景为高圆角矩形，实时预览
  *The capsule background is a highly rounded rectangle with live preview.*

---

## 📱 启动方法（安装与激活）/ Getting Started (Install & Activation)

### 前置条件 / Prerequisites

- Android 8.0（API 26）及以上 / Android 8.0 (API 26) or above
- 已获取 **Root** 权限 / **Root** access
- 已安装 **LSPosed**（或 Vector Manager）框架 / **LSPosed** (or Vector Manager) framework installed

### 安装步骤 / Installation Steps

1. **安装 APK**：将 `app-debug.apk`（或从 [Releases / Actions 产物](https://github.com/GJR787878/RamStatusBar/actions) 下载）安装到手机
   **Install the APK**: install `app-debug.apk` (or download from [Releases / Actions artifacts](https://github.com/GJR787878/RamStatusBar/actions)) onto your phone.
2. **启用模块**：打开 **LSPosed / Vector Manager**，在模块列表中找到 `RamStatusBar` 并启用
   **Enable the module**: open **LSPosed / Vector Manager**, find `RamStatusBar` in the module list and enable it.
3. **勾选作用域**：勾选 **`com.android.systemui`**（系统界面）
   **Check the scope**: check **`com.android.systemui`** (System UI).
4. **重启手机**：首次安装后必须重启一次，模块才会注入 SystemUI 生效
   **Reboot**: a reboot is required after the first install for the module to be injected into SystemUI.

### 使用说明 / Usage

- 打开 **RamStatusBar** 应用，选择你想要的显示模式（最多等 1 秒生效，无需重启）
  Open the **RamStatusBar** app and pick your display mode (takes effect within ~1 second, no reboot).
- 首次切换模式会弹出 **Root 授权请求**，请点击允许
  A **Root permission prompt** appears on the first mode switch — please allow it.
- 点击应用内 **"选择颜色"** 按钮，自定义状态栏胶囊背景颜色
  Tap **"Pick Color"** in the app to customize the capsule background.
- 回到桌面，点击状态栏时钟即可循环切换 CPU / GPU 显示
  Back on the home screen, tap the status bar clock to cycle through CPU / GPU displays.

---

## 🔧 从源码构建 / Build from Source

### 环境要求 / Requirements

- JDK 17
- Android SDK（compileSdk 34）
- Gradle（推荐 8.4+，仓库 CI 会自动生成 wrapper）
  Gradle (8.4+ recommended; CI generates the wrapper automatically)

### 构建命令 / Build Commands

```bash
# 构建 Debug APK / Build the debug APK
./gradlew assembleDebug

# 产物路径 / Output path
# app/build/outputs/apk/debug/app-debug.apk
```

仓库已配置 **GitHub Actions**，每次推送到 `main` 分支会自动构建并上传 APK 产物。
The repo is configured with **GitHub Actions** — every push to `main` automatically builds and uploads the APK artifact.

---

## 🛠️ 技术实现 / Technical Implementation

| 模块 / Module | 说明 / Description |
|------|------|
| **MainHook** | Hook `com.android.systemui.statusbar.policy.Clock`，接管时钟显示并注入内存/CPU/GPU逻辑<br>Hooks the Clock to take over the display and inject RAM/CPU/GPU logic |
| **内存读取** / RAM | `ActivityManager.getMemoryInfo()` 获取可用/总内存<br>Gets available/total RAM via `ActivityManager.getMemoryInfo()` |
| **CPU 占用** / CPU | 读取 `/data/local/tmp/ramstatusbar_cpu`（由外部脚本写入）<br>Reads `/data/local/tmp/ramstatusbar_cpu` (written by an external script) |
| **GPU 占用** / GPU | 读取 KGSL / Mali 的 sysfs 接口（`gpu_busy_percentage` / `gpubusy` / `utilization` 等）<br>Reads KGSL / Mali sysfs interfaces (`gpu_busy_percentage` / `gpubusy` / `utilization`, etc.) |
| **温度** / Temp | 扫描 `/sys/class/thermal` 下的 `cpuss-*` / `gpuss-*` 热区<br>Scans `cpuss-*` / `gpuss-*` thermal zones under `/sys/class/thermal` |
| **配置同步** / Config | 通过 `/data/local/tmp/` 下的文件在应用与 SystemUI 之间共享模式与颜色<br>Shares mode & color between the app and SystemUI via files under `/data/local/tmp/` |
| **界面** / UI | 纯代码构建 UI，内置中 / 英文双语<br>Pure-code UI with built-in Chinese / English bilingual support |

---

## ❓ 常见问题 / FAQ

**Q：启用了模块但状态栏没有变化？**
**Q: The module is enabled but the status bar doesn't change?**

**A：** 请确认：① 已在 LSPosed 中勾选 `com.android.systemui` 作用域；② 已重启手机；③ 未与其他状态栏时钟/内存模块冲突。
**A:** Check that: ① the `com.android.systemui` scope is checked in LSPosed; ② the phone was rebooted; ③ there is no conflict with other status bar clock / RAM modules.

**Q：切换模式提示需要 Root？**
**Q: Switching modes asks for Root?**

**A：** 模式切换需要写入 `/data/local/tmp`，请在弹出的授权窗口中点击允许。若未弹出，请确认 Root 管理器（Magisk / KernelSU）已正常授权。
**A:** Mode switching writes to `/data/local/tmp` — allow the prompt. If it doesn't appear, make sure your root manager (Magisk / KernelSU) has granted permission.

**Q：GPU 显示 N/A？**
**Q: GPU shows N/A?**

**A：** GPU 读取依赖芯片私有接口，不同芯片路径不同。骁龙等部分芯片可读取，其余设备可能显示 `GPU N/A`，属正常现象，不影响其他功能。
**A:** GPU reading relies on chip-specific interfaces with different paths. Some chipsets like Snapdragon work; others may show `GPU N/A`, which is normal and doesn't affect other features.

**Q：手机提示存储空间不足？**
**Q: The phone reports insufficient storage?**

**A：** 本模块自身不会占用多少空间。若安装了多个 LSPosed 模块，个别模块冲突可能导致第三方 ROM 下应用误报存储已满，可尝试临时关闭其他模块排查。
**A:** This module barely uses storage. If multiple LSPosed modules are installed, module conflicts may cause apps to falsely report full storage on custom ROMs — try temporarily disabling other modules to troubleshoot.

**Q：背景颜色怎么恢复默认？**
**Q: How do I restore the default background color?**

**A：** 在取色器中透明度调到 **0（完全透明）** 即可去掉胶囊背景，或选择预设黑色并调整透明度。
**A:** Set **Opacity to 0 (fully transparent)** in the picker to remove the capsule background, or choose the black preset and adjust its opacity.

---

## 📄 License / 许可证

本项目仅供学习交流使用，请遵守当地法律法规与 Android 设备相关使用条款。
This project is for learning and communication only. Please comply with local laws and the terms of use for Android devices.
