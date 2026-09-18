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
- **三页底部导航**：主页 / 配置 / 设置，玻璃质感 UI
  *Three-tab bottom nav: Home / Config / Settings, glassmorphism UI*
- **多语言**：中文 / English / Русский 一键切换
  *Multilingual: 中文 / English / Русский, switch in one tap*

---

## 📱 使用要求 / Requirements

**需要 Root 权限，配合 LSPosed 与 Magisk 使用；首次安装后需重启手机生效。**
*Requires root access, intended for use with LSPosed and Magisk. A reboot is required after the first install.*

## 🆕 更新日志 / Changelog

### v1.4.15（2026-09-18）

- 修复安装权限（REQUEST_INSTALL_PACKAGES）
- 下载代理优先，直连兜底
- 更新检测防 CDN 缓存

### v1.4.14（2026-09-18）

- 优化下载弹窗：圆角与按钮风格一致，加大按钮底部间距

### v1.4.13（2026-09-18）

- 测试内置下载进度弹窗：进度条 + 取消 / 仓库主页按钮

### v1.4.12（2026-09-18）

- **内置下载更新**：检测到新版本后直接在应用内下载 APK，不再跳转网页
- **下载进度弹窗**：显示实时进度条与百分比；进度条下方提供「取消」与「仓库主页」两个按钮，取消即停止下载，仓库主页可跳转 GitHub 下载
- **弹窗风格统一**：下载进度弹窗与软件其它界面保持一致（暗色主题 + 玻璃按钮）

### v1.4.11（2026-09-18）

- **平板适配**：导航栏从底部改为屏幕左侧悬浮胶囊（垂直居中、约半屏高、不铺满），手机端保持原底部导航不变
- **平板适配**：主页/配置/设置/时间/颜色各页按钮改为横排网格布局，内容宽度自适应不再整行拉伸
- **界面统一**：日期、时间、时区、颜色选择等所有系统弹窗统一为暗色主题

---

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

## 📄 完整文档 / Full Docs

详见项目主页 README：https://github.com/GJR787878/RamStatusBar
*See the README on the project home page for full documentation.*
