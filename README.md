# RamStatusBar

> 在状态栏时钟处显示实时内存占用的 LSPosed 模块
> An LSPosed module that displays real-time RAM usage in the status bar clock area

[![Android](https://img.shields.io/badge/Android-8.0%2B-green.svg)](https://www.android.com/)
[![LSPosed](https://img.shields.io/badge/LSPosed-Required-blue.svg)](https://github.com/LSPosed/LSPosed)
[![Root](https://img.shields.io/badge/Root-Required-orange.svg)]()
[![License](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

[中文](#-中文) | [English](#-english) | [Русский](#-русский)

---

## 📱 界面预览 / Screenshots

| 主页 / Home | 配置 / Config |
|:---:|:---:|
| ![主页](images/screenshot_home.png) | ![配置](images/screenshot_config.png) |

| 设置 / Settings | 背景颜色 / Background Color |
|:---:|:---:|
| ![设置](images/screenshot_settings.png) | ![背景颜色](images/screenshot_color_page.png) |

| HSV 取色盘 / Color Picker |
|:---:|
| ![取色盘](images/screenshot_color_picker.png) |

---

## 🇨🇳 中文

### 目录
- [简介](#简介)
- [功能特性](#-功能特性)
- [系统要求](#-系统要求)
- [安装方法](#-安装方法)
- [使用方法](#-使用方法)
- [工作原理](#-工作原理)
- [常见问题](#-常见问题)
- [注意事项](#️-注意事项)
- [许可证](#-许可证)

### 简介

一个 LSPosed 模块：在系统状态栏的时钟位置**实时显示内存占用**（如 `21:11 2.5G/8G`），支持点击时钟切换查看 CPU / GPU 占用率与温度，并可自定义状态栏胶囊背景颜色。应用采用三页底部导航（主页 / 配置 / 设置），界面简洁直观。

### ✨ 功能特性

- **三种显示模式**：仅时间 / 时间+内存 / 仅内存，切换后 1 秒内生效，无需重启
- **点击时钟查 CPU/GPU**：第 1 次点击显示 CPU 占用率，第 2 次显示 GPU 占用率，第 3 次恢复正常；10 秒无操作自动恢复
- **温度显示**：支持读取 CPU / GPU 温度（取决于芯片是否暴露温控节点）
- **自定义背景颜色**：HSV 取色盘 + 亮度/透明度调节 + 预设色板，支持完全透明
- **深度休眠统计**：主页实时显示开机以来深度休眠时长和占比
- **总内存自动识别**：自动检测总内存并取整到常见规格（3/4/6/8/12/16/18/24/32GB）
- **三语界面**：支持中文 / English / Русский 一键切换
- **三页底部导航**：主页（概览）/ 配置（显示模式）/ 设置（颜色与语言）
- **胶囊样式**：状态栏文字自带圆角胶囊背景，宽度自适应不抖动

### 📋 系统要求

| 项目 | 要求 |
|------|------|
| **Android 版本** | Android 8.0 及以上（API 26+） |
| **Root 权限** | 必须（用于写入配置到 /data/local/tmp） |
| **Xposed 框架** | LSPosed / LSPosed_mod（推荐） |
| **作用域** | com.android.systemui |
| **存储空间** | 约 2MB |

> **注意**：本模块仅在 LSPosed 框架下测试通过，其他框架可能存在兼容性问题。

### 📦 安装方法

1. 前往 [Releases](https://github.com/GJR787878/RamStatusBar/releases) 页面下载最新版 APK
2. 安装 APK
3. 打开 **LSPosed 管理器** → **模块** → 找到 **RamStatusBar** → 启用模块
4. 点击模块进入 **作用域** 设置，勾选 **com.android.systemui**（系统界面）
5. **重启手机**（首次安装必须重启一次，LSPosed 注入需要重启）
6. 打开 **RamStatusBar** 应用，进入「配置」页选择显示模式，首次切换会弹出 Root 授权请求，点击允许

### 🚀 使用方法

应用底部有三个标签页：**主页 / 配置 / 设置**

1. **配置页**：选择显示模式
   - **仅显示时间**：状态栏只显示时间（恢复原生效果）
   - **时间 + 内存**：显示 `21:11 2.5G/8G`（默认）
   - **仅显示内存**：显示 `2.5G/8G`
   - 切换后 1 秒内生效，无需重启
2. **主页**：查看模块介绍、点击时钟查 CPU/GPU 的说明、实时深度休眠统计
3. **设置页**：
   - 点击 **「Pick background color」** 自定义状态栏胶囊背景颜色（HSV 取色盘，支持亮度、透明度、预设色）
   - 点击 **「Transparent」** 一键设置完全透明背景
   - 切换界面语言：中文 / English / Русский
4. 在状态栏上**点击时钟**可循环查看 CPU 占用率 → GPU 占用率 → 正常显示

> **提示**：GPU 占用率依赖芯片的私有接口，部分设备上可能显示 "GPU N/A"，能否读取取决于你的芯片型号。

### 🔧 工作原理

模块通过 LSPosed Hook 系统界面（com.android.systemui）中的时钟控件，每秒读取系统内存信息并替换时钟文字。显示模式和背景颜色通过 `/data/local/tmp/` 下的配置文件传递，切换时即时生效。点击时钟时在 CPU / GPU 占用率和正常显示之间循环，10 秒无操作自动恢复。

### ❓ 常见问题

**Q：安装后状态栏没有变化？**
A：检查以下几点：
1. LSPosed 作用域是否勾选了 **com.android.systemui**
2. 模块是否已启用
3. 首次安装后是否重启了手机
4. 打开 RamStatusBar 应用，进入「配置」页并选择一个显示模式（非"仅时间"）
5. 首次切换是否授予了 Root 权限

**Q：切换显示模式后不生效？**
A：确保已授予 Root 权限。模块需要 Root 才能将配置写入 `/data/local/tmp/ramstatusbar_mode`。可以在 Root 管理应用中检查 RamStatusBar 是否被允许获取 Root 权限。

**Q：CPU / GPU 显示 N/A？**
A：CPU 和 GPU 占用率及温度依赖芯片的 sysfs 接口。部分芯片（尤其是 GPU）不暴露使用率节点，会显示 N/A，这是正常现象，不影响内存显示功能。

**Q：每次切换都要重启吗？**
A：不需要。只有**首次安装后需要重启一次**让 LSPosed 注入生效。之后切换显示模式、修改颜色都是 1 秒内即时生效，无需重启。

**Q：自定义颜色后怎么恢复默认？**
A：在设置页点击「背景颜色」进入颜色设置页，点击 **「Transparent」** 按钮即可设置为完全透明（默认效果）。

**Q：状态栏文字宽度会抖动吗？**
A：不会。模块采用固定宽度策略，宽度只扩大不缩小，避免 CPU/GPU 数字变化导致状态栏抖动。

### ⚠️ 注意事项

- 本模块**仅用于个人使用和学习交流**
- 需要 **Root 权限**和 **LSPosed 框架**，无 Root 无法使用
- 部分高度定制的 ROM 可能修改了 SystemUI 时钟控件，存在兼容性问题
- 建议先在当前系统上测试确认无误后长期使用
- 排查问题：LSPosed → 日志 → 搜索「RamStatusBar」

### 📄 许可证

[MIT License](LICENSE)

---

## 🇬🇧 English

### Table of Contents
- [Introduction](#introduction)
- [Features](#-features)
- [Requirements](#-requirements)
- [Installation](#-installation)
- [Usage](#-usage)
- [How It Works](#-how-it-works)
- [FAQ](#-faq)
- [Warnings](#️-warnings)
- [License](#-license)

### Introduction

An LSPosed module that **displays real-time RAM usage in the status bar clock area** (e.g., `21:11 2.5G/8G`). Tap the clock to cycle through CPU / GPU usage & temperature, and customize the pill background color. The app features a three-tab bottom navigation (Home / Config / Settings) for a clean, intuitive interface.

### ✨ Features

- **Three display modes**: Time only / Time + RAM / RAM only — changes apply within 1 second, no reboot needed
- **Tap clock for CPU/GPU**: 1st tap shows CPU usage, 2nd tap shows GPU usage, 3rd tap returns to normal; auto-reverts after 10 seconds
- **Temperature display**: Reads CPU / GPU temperature when available (depends on chipset thermal zones)
- **Custom background color**: HSV color picker + brightness/opacity sliders + preset swatches, including fully transparent
- **Deep sleep stats**: Real-time deep sleep duration and percentage since boot on the Home tab
- **Auto RAM detection**: Automatically detects total RAM and rounds to common tiers (3/4/6/8/12/16/18/24/32GB)
- **Trilingual UI**: Chinese / English / Русский — switch with one tap
- **Three-tab navigation**: Home (overview) / Config (display mode) / Settings (color & language)
- **Pill style**: Rounded capsule background with adaptive fixed width, no jitter

### 📋 Requirements

| Item | Requirement |
|------|-------------|
| **Android Version** | Android 8.0+ (API 26+) |
| **Root Access** | Required (for writing config to /data/local/tmp) |
| **Xposed Framework** | LSPosed / LSPosed_mod (recommended) |
| **Scope** | com.android.systemui |
| **Storage** | ~2MB |

> **Note**: This module is tested on LSPosed only. Other frameworks may have compatibility issues.

### 📦 Installation

1. Download the latest APK from the [Releases](https://github.com/GJR787878/RamStatusBar/releases) page
2. Install the APK
3. Open **LSPosed Manager** → **Modules** → Find **RamStatusBar** → Enable the module
4. Tap the module → **Scope** → Check **com.android.systemui**
5. **Reboot your phone** (required once after initial install for LSPosed injection)
6. Open the **RamStatusBar** app, go to the **Config** tab, select a display mode, and grant Root permission when prompted

### 🚀 Usage

The app has three tabs at the bottom: **Home / Config / Settings**

1. **Config tab**: Select a display mode
   - **Time only**: Status bar shows only the time (stock behavior)
   - **Time + RAM**: Shows `21:11 2.5G/8G` (default)
   - **RAM only**: Shows `2.5G/8G`
   - Changes apply within 1 second — no reboot needed
2. **Home tab**: Module overview, CPU/GPU tap instructions, real-time deep sleep stats
3. **Settings tab**:
   - Tap **「Pick background color」** to customize the pill background (HSV picker with brightness, opacity, presets)
   - Tap **「Transparent」** for a fully transparent background
   - Switch UI language: Chinese / English / Русский
4. **Tap the clock** in the status bar to cycle through CPU usage → GPU usage → normal display

> **Tip**: GPU usage relies on chip-specific sysfs paths and may show "GPU N/A" on some devices, depending on your chipset.

### 🔧 How It Works

The module hooks the clock widget in SystemUI (com.android.systemui) via LSPosed, reads system memory info every second, and replaces the clock text. Display mode and background color are passed via config files in `/data/local/tmp/`, so changes take effect instantly. Tapping the clock cycles between CPU / GPU usage and normal display, auto-reverting after 10 seconds of inactivity.

### ❓ FAQ

**Q: No change in the status bar after installation?**
A: Check the following:
1. Is **com.android.systemui** checked in LSPosed scope?
2. Is the module enabled?
3. Did you reboot after the initial install?
4. Did you open the RamStatusBar app, go to the Config tab, and select a mode (not "Time only")?
5. Did you grant Root permission on the first mode switch?

**Q: Display mode changes don't take effect?**
A: Make sure Root permission is granted. The module needs Root to write the config to `/data/local/tmp/ramstatusbar_mode`. Check your Root manager app to confirm RamStatusBar is allowed.

**Q: CPU / GPU shows N/A?**
A: CPU and GPU usage/temperature depend on chipset sysfs interfaces. Some chips (especially GPUs) don't expose usage nodes and will show N/A — this is normal and doesn't affect the RAM display.

**Q: Do I need to reboot every time I change the mode?**
A: No. Only the **initial install requires one reboot** for LSPosed injection. After that, switching display modes and colors takes effect within 1 second — no reboot needed.

**Q: How do I restore the default color?**
A: In the Settings tab, tap "Background color" to enter the color settings page, then tap the **「Transparent」** button for a fully transparent background (the default).

**Q: Does the status bar text width jitter?**
A: No. The module uses a fixed-width strategy — width only expands, never shrinks — preventing jitter from changing CPU/GPU numbers.

### ⚠️ Warnings

- For **personal use and educational purposes only**
- Requires **Root access** and **LSPosed framework** — won't work without Root
- Some heavily customized ROMs may modify the SystemUI clock widget, causing compatibility issues
- Test on your device before long-term use
- Troubleshooting: LSPosed → Logs → Search "RamStatusBar"

### 📄 License

[MIT License](LICENSE)

---

## 🇷🇺 Русский

### Содержание
- [Введение](#введение)
- [Возможности](#-возможности)
- [Требования](#-требования)
- [Установка](#-установка)
- [Использование](#-использование)
- [Как это работает](#-как-это-работает)
- [Часто задаваемые вопросы](#-часто-задаваемые-вопросы)
- [Предупреждения](#️-предупреждения)
- [Лицензия](#-лицензия)

### Введение

Модуль LSPosed, который **отображает использование оперативной памяти в реальном времени в области часов строки состояния** (например, `21:11 2.5G/8G`). Нажмите на часы, чтобы переключиться на использование CPU / GPU и температуру, а также настройте цвет фона капсулы. Приложение имеет трёхвкладочную нижнюю навигацию (Главная / Конфиг / Настройки) для чистого и интуитивного интерфейса.

### ✨ Возможности

- **Три режима отображения**: только время / время + ОЗУ / только ОЗУ — изменения применяются за 1 секунду, перезагрузка не требуется
- **Нажатие на часы для CPU/GPU**: 1-е нажатие показывает загрузку CPU, 2-е — загрузку GPU, 3-е — возврат к норме; автоматический возврат через 10 секунд
- **Отображение температуры**: считывает температуру CPU / GPU при наличии (зависит от термальных зон чипсета)
- **Настраиваемый цвет фона**: палитра HSV + ползунки яркости/прозрачности + предустановленные образцы, включая полную прозрачность
- **Статистика глубокого сна**: длительность и процент глубокого сна с момента загрузки на вкладке «Главная»
- **Автоопределение ОЗУ**: автоматически определяет общий объём ОЗУ и округляется до распространённых значений (3/4/6/8/12/16/18/24/32 ГБ)
- **Трёхъязычный интерфейс**: китайский / English / Русский — переключение одним нажатием
- **Трёхвкладочная навигация**: Главная (обзор) / Конфиг (режим отображения) / Настройки (цвет и язык)
- **Стиль капсулы**: скруглённый фон капсулы с адаптивной фиксированной шириной, без дрожания

### 📋 Требования

| Пункт | Требование |
|------|-------------|
| **Версия Android** | Android 8.0+ (API 26+) |
| **Права Root** | Обязательны (для записи конфигурации в /data/local/tmp) |
| **Фреймворк Xposed** | LSPosed / LSPosed_mod (рекомендуется) |
| **Область действия** | com.android.systemui |
| **Хранилище** | ~2 МБ |

> **Примечание**: Этот модуль протестирован только на LSPosed. Другие фреймворки могут иметь проблемы совместимости.

### 📦 Установка

1. Скачайте последний APK со страницы [Releases](https://github.com/GJR787878/RamStatusBar/releases)
2. Установите APK
3. Откройте **LSPosed Manager** → **Modules** → Найдите **RamStatusBar** → Включите модуль
4. Нажмите на модуль → **Scope** → Отметьте **com.android.systemui**
5. **Перезагрузите телефон** (требуется один раз после первоначальной установки для инъекции LSPosed)
6. Откройте приложение **RamStatusBar**, перейдите на вкладку **Конфиг**, выберите режим отображения и предоставьте права Root при запросе

### 🚀 Использование

В приложении три вкладки внизу: **Главная / Конфиг / Настройки**

1. **Вкладка «Конфиг»**: выберите режим отображения
   - **Только время**: в строке состояния отображается только время (стоковое поведение)
   - **Время + ОЗУ**: отображается `21:11 2.5G/8G` (по умолчанию)
   - **Только ОЗУ**: отображается `2.5G/8G`
   - Изменения применяются за 1 секунду — перезагрузка не требуется
2. **Вкладка «Главная»**: обзор модуля, инструкции по нажатию на часы для CPU/GPU, статистика глубокого сна в реальном времени
3. **Вкладка «Настройки»**:
   - Нажмите **«Pick background color»**, чтобы настроить фон капсулы (палитра HSV с яркостью, прозрачностью, образцами)
   - Нажмите **«Transparent»** для полностью прозрачного фона
   - Переключите язык интерфейса: китайский / English / Русский
4. **Нажмите на часы** в строке состояния, чтобы переключаться между загрузкой CPU → загрузкой GPU → обычным отображением

> **Совет**: Загрузка GPU зависит от специфичных для чипа путей sysfs и может отображать «GPU N/A» на некоторых устройствах в зависимости от вашего чипсета.

### 🔧 Как это работает

Модуль перехватывает виджет часов в SystemUI (com.android.systemui) через LSPosed, считывает информацию о памяти системы каждую секунду и заменяет текст часов. Режим отображения и цвет фона передаются через файлы конфигурации в `/data/local/tmp/`, поэтому изменения применяются мгновенно. Нажатие на часы переключается между загрузкой CPU / GPU и обычным отображением, с автоматическим возвратом через 10 секунд бездействия.

### ❓ Часто задаваемые вопросы

**Q: После установки в строке состояния ничего не изменилось?**
A: Проверьте следующее:
1. Отмечен ли **com.android.systemui** в области действия LSPosed?
2. Включён ли модуль?
3. Перезагружали ли вы телефон после первоначальной установки?
4. Открыли ли вы приложение RamStatusBar, перешли на вкладку «Конфиг» и выбрали режим (не «Только время»)?
5. Предоставили ли вы права Root при первом переключении режима?

**Q: Изменения режима отображения не применяются?**
A: Убедитесь, что права Root предоставлены. Модулю нужны права Root для записи конфигурации в `/data/local/tmp/ramstatusbar_mode`. Проверьте в приложении управления Root, разрешён ли RamStatusBar.

**Q: CPU / GPU отображает N/A?**
A: Загрузка и температура CPU/GPU зависят от интерфейсов sysfs чипсета. Некоторые чипы (особенно GPU) не предоставляют узлы загрузки и будут показывать N/A — это нормально и не влияет на отображение ОЗУ.

**Q: Нужно ли перезагружаться при каждом изменении режима?**
A: Нет. Только **первоначальная установка требует одной перезагрузки** для инъекции LSPosed. После этого переключение режимов и цветов применяется за 1 секунду — перезагрузка не требуется.

**Q: Как восстановить цвет по умолчанию?**
A: На вкладке «Настройки» нажмите «Background color», чтобы перейти на страницу настроек цвета, затем нажмите кнопку **«Transparent»** для полностью прозрачного фона (по умолчанию).

**Q: Дрожит ли ширина текста в строке состояния?**
A: Нет. Модуль использует стратегию фиксированной ширины — ширина только увеличивается, никогда не уменьшается — что предотвращает дрожание от меняющихся чисел CPU/GPU.

### ⚠️ Предупреждения

- Только для **личного использования и образовательных целей**
- Требуются **права Root** и **фреймворк LSPosed** — без Root не работает
- Некоторые сильно кастомизированные прошивки могут изменять виджет часов SystemUI, вызывая проблемы совместимости
- Протестируйте на своём устройстве перед долгосрочным использованием
- Устранение неполадок: LSPosed → Logs → Поиск «RamStatusBar»

### 📄 Лицензия

[MIT License](LICENSE)

---

## ⭐ Support

If this project helps you, please give it a Star ⭐

For issues or suggestions, please submit an [Issue](https://github.com/GJR787878/RamStatusBar/issues).
