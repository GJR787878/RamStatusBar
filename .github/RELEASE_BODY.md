<div align="center">

# RamStatusBar

**LSPosed 模块，在 Android 状态栏时钟位置实时显示剩余内存，点击可查看 CPU / GPU 占用与温度。**
*An LSPosed module that shows remaining RAM on the status bar clock, tap to check CPU / GPU usage & temperature.*

</div>

---

## 📖 项目介绍 / Introduction

**在状态栏时间旁实时显示可用内存（如 `2.5G/8G`），支持三种显示模式、点击查看 CPU / GPU、深度休眠统计与自定义胶囊背景颜色。**
*Shows available RAM (e.g. `2.5G/8G`) beside the status bar clock, with three display modes, tap-to-view CPU / GPU, deep sleep stats, and customizable capsule background.*

---

## ✨ 功能亮点 / Features

- **三种显示模式**：仅时间 / 时间 + 内存 / 仅内存
  *Three display modes: Time only / Time + RAM / RAM only*
- **点击状态栏时钟**：第 1 次看 CPU，第 2 次看 GPU，第 3 次还原，10 秒无操作自动还原
  *Tap the clock: 1st tap CPU, 2nd tap GPU, 3rd tap restore, auto-restores after 10s*
- **实时刷新**：可用内存约每秒更新一次
  *Live refresh: available RAM updates ~every second*
- **背景颜色自定义**：内置 HSV 取色盘，可调亮度与透明度
  *Customizable background: built-in HSV color picker with brightness & opacity sliders*
- **深度休眠统计**：直接显示开机以来的休眠时长与占比
  *Deep sleep stats: shows deep sleep duration & ratio since boot*

---

## 📱 使用要求 / Requirements

**需要 Root 权限，配合 LSPosed 与 Magisk 使用；首次安装后需重启手机生效。**
*Requires root access, intended for use with LSPosed and Magisk. A reboot is required after the first install.*

## 🔧 安装步骤 / Installation

1. 安装 APK 后，在 LSPosed 中启用模块并勾选 `com.android.systemui` 作用域
   *Install the APK, enable the module in LSPosed and check the `com.android.systemui` scope.*
2. 重启手机生效
   *Reboot the device to apply.*
3. 打开应用选择显示模式，点击"选择颜色"自定义背景
   *Open the app to pick a display mode; tap "Pick Color" to customize the background.*

---

## 📸 截图展示 / Screenshots

<div align="center">

**状态栏显示效果 / Status bar display**
![Status bar](https://raw.githubusercontent.com/GJR787878/RamStatusBar/main/docs/images/screenshot_statusbar.png)

**三种显示模式 / Three display modes (CPU · Time+RAM · GPU)**
![Display modes](https://raw.githubusercontent.com/GJR787878/RamStatusBar/main/docs/images/screenshot_modes.png)

**颜色设置界面 / Color picker**
![Color picker](https://raw.githubusercontent.com/GJR787878/RamStatusBar/main/docs/images/screenshot_color_picker.png)

</div>

---

## 📄 完整文档 / Full Docs

详见项目主页 README：https://github.com/GJR787878/RamStatusBar
*See the README on the project home page for full documentation.*
