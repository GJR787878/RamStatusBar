package com.example.ramstatusbar;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.PorterDuff;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.provider.Settings;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileReader;
import java.util.Locale;

public class MainActivity extends Activity {

    private static final String CONFIG_FILE =
            "/data/local/tmp/ramstatusbar_mode";
    /*
     * 手动胶囊宽度（像素）。0 = 自动。
     * 与 MainHook 的 WIDTH_FILE 一致，滑块写入后
     * SystemUI 每秒轮询，无需重启即可实时生效。
     */
    private static final String WIDTH_FILE =
            "/data/local/tmp/ramstatusbar_width";
    private static final int MODE_TIME_ONLY = 0;
    private static final int MODE_TIME_RAM = 1;
    private static final int MODE_RAM_ONLY = 2;

    private static final String UI_PREFS_NAME = "ui_prefs";
    private static final String KEY_LANGUAGE = "language";
    private static final String LANG_ZH = "zh";
    private static final String LANG_EN = "en";
    private static final String LANG_RU = "ru";

    private static final int TAB_HOME = 0;
    private static final int TAB_CONFIG = 1;
    private static final int TAB_SETTINGS = 2;

    private static final int COLOR_ACCENT = 0xFF0A84FF;
    private static final int COLOR_WHITE = 0xFFFFFFFF;
    private static final int COLOR_NAV_BG = 0xB31C1C1E;
    private static final int COLOR_NAV_BORDER = 0x40FFFFFF;
    private static final int COLOR_TAB_SELECTED_BG = 0x2EFFFFFF;

    // 更新检测：GitHub 仓库与发布页
    private static final String UPDATE_REPO = "GJR787878/RamStatusBar";
    private static final String RELEASES_URL = "https://github.com/GJR787878/RamStatusBar/releases/latest";

    private String mLanguage = LANG_ZH;
    private TextView mDeepSleepText;
    private final Handler mUiHandler = new Handler(Looper.getMainLooper());
    private Runnable mDeepSleepUpdater;

    private View mHomePage;
    private View mConfigPage;
    private View mSettingsPage;

    private View mNavHome;
    private View mNavConfig;
    private View mNavSettings;
    private ImageView mIconHome;
    private ImageView mIconConfig;
    private ImageView mIconSettings;
    private TextView mLabelHome;
    private TextView mLabelConfig;
    private TextView mLabelSettings;

    private int mCurrentTab = TAB_HOME;

    private String lang(String zh, String en, String ru) {
        if (LANG_RU.equals(mLanguage)) return ru;
        if (LANG_EN.equals(mLanguage)) return en;
        return zh;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SharedPreferences uiPrefs = getSharedPreferences(UI_PREFS_NAME, MODE_PRIVATE);
        mLanguage = uiPrefs.getString(KEY_LANGUAGE, LANG_ZH);

        float density = getResources().getDisplayMetrics().density;
        int screenWidthPx = getResources().getDisplayMetrics().widthPixels;
        int screenHeightPx = getResources().getDisplayMetrics().heightPixels;
        // 平板（sw600dp+）：导航栏改左侧悬浮胶囊，内容区让出导航宽度
        final boolean tablet = getResources().getConfiguration().smallestScreenWidthDp >= 600;
        final int sideNavAreaPx = tablet ? Math.round(140 * density) : 0;
        int contentWidthPx = screenWidthPx - sideNavAreaPx
                - Math.round(48 * density) - Math.round(48 * density);
        // 平板内容宽度上限，避免长文本整行拉伸
        if (contentWidthPx > Math.round(760 * density)) {
            contentWidthPx = Math.round(760 * density);
        }

        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(0xFF000000);

        FrameLayout contentContainer = new FrameLayout(this);
        FrameLayout.LayoutParams containerParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT);
        containerParams.leftMargin = sideNavAreaPx;
        root.addView(contentContainer, containerParams);

        mHomePage = createHomePage(density, contentWidthPx, tablet);
        mConfigPage = createConfigPage(density, contentWidthPx, tablet);
        mSettingsPage = createSettingsPage(density, contentWidthPx, tablet);

        contentContainer.addView(mHomePage);
        contentContainer.addView(mConfigPage);
        contentContainer.addView(mSettingsPage);

        View nav = tablet ? createSideNav(density, screenHeightPx) : createBottomNav(density);
        FrameLayout.LayoutParams navParams;
        if (tablet) {
            // 左侧悬浮胶囊：垂直居中，高度约为屏幕一半，不铺满
            navParams = new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT);
            navParams.gravity = Gravity.LEFT | Gravity.CENTER_VERTICAL;
            navParams.leftMargin = Math.round(20 * density);
        } else {
            navParams = new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT);
            navParams.gravity = Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL;
            navParams.leftMargin = Math.round(24 * density);
            navParams.rightMargin = Math.round(24 * density);
            navParams.bottomMargin = Math.round(24 * density);
        }
        root.addView(nav, navParams);

        setContentView(root);
        setTitle(lang("RAM 状态栏显示", "RAM Status Bar", "RAM Статус-бар"));
        switchTab(TAB_HOME);

        // 启动时自动检测更新（后台，有新版本才提示）
        checkUpdate(false);
    }

    // ==================== 主页 ====================
    private View createHomePage(float density, int contentWidthPx, boolean tablet) {
        ScrollView scrollView = new ScrollView(this);
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(
                Math.round(48 * density), Math.round(40 * density),
                Math.round(48 * density), Math.round(140 * density));

        TextView title = new TextView(this);
        title.setTextSize(22);
        title.setTextColor(COLOR_WHITE);
        title.setText(lang("RAM 状态栏显示", "RAM Status Bar", "RAM Статус-бар"));
        content.addView(title);

        TextView intro = new TextView(this);
        intro.setTextSize(14);
        intro.setTextColor(0xFFCCCCCC);
        intro.setPadding(0, Math.round(24 * density), 0, Math.round(32 * density));
        setBilingualText(intro,
                "在状态栏时间旁实时显示可用内存，点击时钟可查看 CPU / GPU 占用与温度。选择显示模式最多等 1 秒即可生效，不需要重启手机。",
                "Shows available RAM beside the status bar clock. Tap the clock to check CPU / GPU usage & temperature. Mode changes take effect within 1 second, no reboot needed.",
                "Отображает доступную RAM рядом с часами в строке состояния. Нажмите на часы, чтобы проверить загрузку CPU/GPU и температуру. Изменения режима вступают в силу за 1 секунду, перезагрузка не требуется.",
                contentWidthPx);
        content.addView(intro);

        TextView tapTitle = new TextView(this);
        tapTitle.setTextSize(15);
        tapTitle.setTextColor(COLOR_WHITE);
        tapTitle.setPadding(0, Math.round(32 * density), 0, Math.round(8 * density));
        tapTitle.setText(lang("点击时钟查看 CPU / GPU", "Tap the clock to check CPU / GPU", "Нажмите на часы для проверки CPU / GPU"));
        content.addView(tapTitle);

        TextView tapBody = new TextView(this);
        tapBody.setTextSize(13);
        tapBody.setTextColor(0xFFAAAAAA);
        setBilingualText(tapBody,
                "点击状态栏的时钟：第 1 次点击显示 CPU 占用率，第 2 次点击显示 GPU 占用率，第 3 次点击回到正常显示；10 秒内不再点击也会自动回到正常显示。\n\nGPU 占用率依赖具体芯片的私有接口，部分设备上可能会显示 \"GPU N/A\"。",
                "Tap the status bar clock: 1st tap shows CPU usage, 2nd tap shows GPU usage, 3rd tap returns to normal. Auto-restores after 10s.\n\nGPU usage relies on chip-specific sysfs paths and may show \"GPU N/A\" on some devices.",
                "Нажмите на часы в строке состояния: 1-е нажатие — загрузка CPU, 2-е — загрузка GPU, 3-е — возврат к обычному виду. Автоматический возврат через 10 секунд.\n\nЗагрузка GPU зависит от чипсета и может отображаться как \"GPU N/A\" на некоторых устройствах.",
                contentWidthPx);
        content.addView(tapBody);

        TextView deepSleepTitle = new TextView(this);
        deepSleepTitle.setTextSize(15);
        deepSleepTitle.setTextColor(COLOR_WHITE);
        deepSleepTitle.setPadding(0, Math.round(48 * density), 0, Math.round(8 * density));
        deepSleepTitle.setText(lang("深度休眠", "Deep sleep", "Глубокий сон"));
        content.addView(deepSleepTitle);

        TextView deepSleepDesc = new TextView(this);
        deepSleepDesc.setTextSize(13);
        deepSleepDesc.setTextColor(0xFFAAAAAA);
        setBilingualText(deepSleepDesc,
                "下面显示的是开机以来设备处于深度休眠状态的时长和占比，跟 \"关于本机\" 里的数值一致。",
                "How much time since boot the device has spent in deep sleep, same figure as in About Phone.",
                "Ниже отображается время и процент глубокого сна с момента включения устройства, те же данные, что и в разделе \"О телефоне\".",
                contentWidthPx);
        content.addView(deepSleepDesc);

        mDeepSleepText = new TextView(this);
        mDeepSleepText.setTextSize(16);
        mDeepSleepText.setTextColor(COLOR_ACCENT);
        mDeepSleepText.setPadding(0, Math.round(16 * density), 0, 0);
        content.addView(mDeepSleepText);

        // 检查更新按钮（主页最下方）
        Button btnCheckUpdate = new Button(this);
        btnCheckUpdate.setTextSize(14);
        btnCheckUpdate.setTextColor(COLOR_WHITE);
        btnCheckUpdate.setAllCaps(false);
        btnCheckUpdate.setText(lang("🔄 检查更新", "🔄 Check Update", "🔄 Проверить обновления"));
        btnCheckUpdate.setBackground(createGlassButtonBg(density));
        LinearLayout.LayoutParams checkParams;
        if (tablet) {
            // 平板：按钮不整行拉伸，居中固定宽度
            checkParams = new LinearLayout.LayoutParams(
                    Math.round(360 * density),
                    ViewGroup.LayoutParams.WRAP_CONTENT);
            checkParams.gravity = Gravity.CENTER_HORIZONTAL;
            checkParams.topMargin = Math.round(32 * density);
        } else {
            checkParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT);
            checkParams.topMargin = Math.round(32 * density);
        }
        btnCheckUpdate.setLayoutParams(checkParams);
        btnCheckUpdate.setOnClickListener(v -> checkUpdate(true));
        content.addView(btnCheckUpdate);

        scrollView.addView(content, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        return scrollView;
    }

    // ==================== 检查更新 ====================
    private void checkUpdate(boolean manual) {
        String versionName;
        try {
            versionName = getPackageManager()
                    .getPackageInfo(getPackageName(), 0).versionName;
        } catch (Exception e) {
            versionName = "0";
        }
        UpdateChecker.check(UPDATE_REPO, versionName,
                (latest, tag, hasUpdate, error) -> {
                    if (hasUpdate) {
                        new AlertDialog.Builder(this)
                                .setTitle(lang("🔄 发现新版本 v" + latest,
                                        "🔄 New version v" + latest + " available",
                                        "🔄 Доступна новая версия v" + latest))
                                .setMessage(lang("检测到新版本 v" + latest + "，是否前往下载？",
                                        "New version v" + latest + " detected. Download now?",
                                        "Обнаружена новая версия v" + latest + ". Скачать?"))
                                .setPositiveButton(lang("下载", "Download", "Скачать"), (d, w) -> {
                                    try {
                                        startActivity(new Intent(
                                                Intent.ACTION_VIEW,
                                                Uri.parse(RELEASES_URL)));
                                    } catch (Exception ignored) {
                                    }
                                })
                                .setNegativeButton(lang("取消", "Cancel", "Отмена"), null)
                                .show();
                    } else if (manual) {
                        if (error != null) {
                            Toast.makeText(this,
                                    lang("检查更新失败：", "Update check failed: ",
                                            "Не удалось проверить обновления: ") + error,
                                    Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(this,
                                    lang("已是最新版本", "You are up to date",
                                            "У вас последняя версия"),
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    // ==================== 配置页 ====================
    private View createConfigPage(float density, int contentWidthPx, boolean tablet) {
        ScrollView scrollView = new ScrollView(this);
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(
                Math.round(48 * density), Math.round(40 * density),
                Math.round(48 * density), Math.round(140 * density));

        TextView title = new TextView(this);
        title.setTextSize(22);
        title.setTextColor(COLOR_WHITE);
        title.setText(lang("显示模式", "Display Mode", "Режим отображения"));
        content.addView(title);

        TextView desc = new TextView(this);
        desc.setTextSize(14);
        desc.setTextColor(0xFFCCCCCC);
        desc.setPadding(0, Math.round(24 * density), 0, Math.round(32 * density));
        setBilingualText(desc,
                "选择状态栏的显示内容，最多等 1 秒即可生效。",
                "Choose what the status bar shows. Changes take effect within 1 second.",
                "Выберите, что отображать в строке состояния. Изменения вступают в силу за 1 секунду.",
                contentWidthPx);
        content.addView(desc);

        int currentMode = readCurrentModeOrDefault();

        final RadioGroup radioGroup = new RadioGroup(this);
        // 平板：3 个模式按钮横排一行；手机：保持竖排
        radioGroup.setOrientation(tablet ? RadioGroup.HORIZONTAL : RadioGroup.VERTICAL);

        final RadioButton rbTimeOnly = createModeRadioButton(1001,
                lang("仅显示时间", "Time only", "Только время"), density, tablet);
        final RadioButton rbTimeRam = createModeRadioButton(1002,
                lang("时间 + 内存 (如 21:11 2.5G/8G)", "Time + RAM (e.g. 21:11 2.5G/8G)", "Время + RAM (напр. 21:11 2.5G/8G)"), density, tablet);
        final RadioButton rbRamOnly = createModeRadioButton(1003,
                lang("仅显示内存 (如 2.5G/8G)", "RAM only (e.g. 2.5G/8G)", "Только RAM (напр. 2.5G/8G)"), density, tablet);

        radioGroup.addView(rbTimeOnly);
        radioGroup.addView(rbTimeRam);
        radioGroup.addView(rbRamOnly);

        if (currentMode == MODE_TIME_ONLY) {
            radioGroup.check(rbTimeOnly.getId());
        } else if (currentMode == MODE_RAM_ONLY) {
            radioGroup.check(rbRamOnly.getId());
        } else {
            radioGroup.check(rbTimeRam.getId());
        }
        updateModeButtonStyles(density, radioGroup, rbTimeOnly, rbTimeRam, rbRamOnly);

        radioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                updateModeButtonStyles(density, radioGroup, rbTimeOnly, rbTimeRam, rbRamOnly);
                int mode;
                if (checkedId == rbTimeOnly.getId()) {
                    mode = MODE_TIME_ONLY;
                } else if (checkedId == rbRamOnly.getId()) {
                    mode = MODE_RAM_ONLY;
                } else {
                    mode = MODE_TIME_RAM;
                }
                boolean ok = writeModeToFile(mode);
                if (!ok) {
                    Toast.makeText(MainActivity.this,
                            lang("写入失败，请检查是否已授予 root 权限",
                                    "Write failed, please check root permission",
                                    "Ошибка записи, проверьте права root"),
                            Toast.LENGTH_LONG).show();
                }
            }
        });

        content.addView(radioGroup);

        TextView setupTitle = new TextView(this);
        setupTitle.setTextSize(15);
        setupTitle.setTextColor(COLOR_WHITE);
        setupTitle.setPadding(0, Math.round(64 * density), 0, Math.round(8 * density));
        setupTitle.setText(lang("安装说明", "Setup", "Установка"));
        content.addView(setupTitle);

        TextView setupBody = new TextView(this);
        setupBody.setTextSize(13);
        setupBody.setTextColor(0xFFAAAAAA);
        setupBody.setText(lang(
                "1. 到 LSPosed / Vector Manager 里，对本模块勾选作用域 com.android.systemui 并启用。\n"
                        + "2. 首次安装完成后需要重启一次手机。\n"
                        + "3. 切换显示模式需要 root 权限，首次切换会弹出授权请求，请点击允许。\n"
                        + "4. 总内存会自动检测并取整到常见规格 (8/12/16/24G等)。",
                "1. In LSPosed / Vector Manager, enable this module and check the scope com.android.systemui.\n"
                        + "2. Reboot once after the first install.\n"
                        + "3. Switching display mode requires root; allow the prompt on first use.\n"
                        + "4. Total RAM is auto-detected and rounded to common specs (8/12/16/24G).",
                "1. В LSPosed / Vector Manager включите модуль и отметьте область com.android.systemui.\n"
                        + "2. Перезагрузите устройство после первой установки.\n"
                        + "3. Для смены режима нужны права root, разрешите запрос при первом использовании.\n"
                        + "4. Общий объём RAM определяется автоматически и округляется (8/12/16/24 ГБ и т.д.)."));
        content.addView(setupBody);

        scrollView.addView(content, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        return scrollView;
    }

    // ==================== 设置页 ====================
    private View createSettingsPage(float density, int contentWidthPx, boolean tablet) {
        ScrollView scrollView = new ScrollView(this);
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(
                Math.round(48 * density), Math.round(40 * density),
                Math.round(48 * density), Math.round(140 * density));

        TextView title = new TextView(this);
        title.setTextSize(22);
        title.setTextColor(COLOR_WHITE);
        title.setText(lang("设置", "Settings", "Настройки"));
        content.addView(title);

        TextView colorLabel = new TextView(this);
        colorLabel.setTextSize(15);
        colorLabel.setTextColor(COLOR_WHITE);
        colorLabel.setPadding(0, Math.round(40 * density), 0, Math.round(12 * density));
        colorLabel.setText(lang("背景颜色", "Background color", "Цвет фона"));
        if (!tablet) content.addView(colorLabel);

        Button colorButton = new Button(this);
        colorButton.setText(lang("选择背景颜色", "Pick background color", "Выбрать цвет фона"));
        colorButton.setAllCaps(false);
        colorButton.setTextColor(COLOR_WHITE);
        colorButton.setBackground(createGlassButtonBg(density));
        colorButton.setPadding(
                Math.round(24 * density), Math.round(14 * density),
                Math.round(24 * density), Math.round(14 * density));
        colorButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, ColorSettingsActivity.class);
                startActivity(intent);
            }
        });
        if (!tablet) content.addView(colorButton, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));

        // ===== 胶囊长度滑块 =====
        TextView widthLabel = new TextView(this);
        widthLabel.setTextSize(15);
        widthLabel.setTextColor(COLOR_WHITE);
        widthLabel.setPadding(0, Math.round(48 * density), 0, Math.round(12 * density));
        widthLabel.setText(lang("胶囊长度", "Capsule width", "Ширина капсулы"));
        content.addView(widthLabel);

        final TextView widthValue = new TextView(this);
        widthValue.setTextSize(14);
        widthValue.setTextColor(COLOR_ACCENT);
        widthValue.setSingleLine(true);
        widthValue.setClickable(true);
        widthValue.setFocusable(true);
        /*
         * 用蓝色边框 + 半透明蓝底把数值框起来，提示可点击手动输入。
         */
        GradientDrawable widthBox = new GradientDrawable();
        widthBox.setShape(GradientDrawable.RECTANGLE);
        widthBox.setCornerRadius(Math.round(10 * density));
        widthBox.setStroke(Math.round(2 * density), COLOR_ACCENT);
        widthBox.setColor(0x1A0A84FF);
        widthValue.setBackground(widthBox);
        widthValue.setPadding(
                Math.round(20 * density), Math.round(10 * density),
                Math.round(20 * density), Math.round(10 * density));
        content.addView(widthValue, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));

        final SeekBar widthSeek = new SeekBar(this);
        widthSeek.setMax(300);
        int curWidth = readCurrentWidth();
        widthSeek.setProgress(Math.min(curWidth, 300));
        updateWidthLabel(widthValue, curWidth);

        /*
         * 点击数值框 → 弹出数字输入对话框，手动输入 0~300。
         */
        widthValue.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showWidthInputDialog(widthSeek, widthValue);
            }
        });

        widthSeek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            private long lastWrite = 0;

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (!fromUser) {
                    return;
                }
                updateWidthLabel(widthValue, progress);
                // 拖动过程节流写入（约每 200ms 一次），SystemUI 每秒轮询应用
                long now = System.currentTimeMillis();
                if (now - lastWrite > 200) {
                    lastWrite = now;
                    writeWidthToFile(progress);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                // 松手时确保最终值已写入
                writeWidthToFile(seekBar.getProgress());
            }
        });
        content.addView(widthSeek);

        TextView widthHint = new TextView(this);
        widthHint.setTextSize(12);
        widthHint.setTextColor(0xFF888888);
        widthHint.setText(lang(
                "0 = 自动（按内容精确适配）\n拖动滑块实时生效，约 1 秒内更新",
                "0 = Auto (fits content precisely)\nDrag to adjust, takes effect within ~1 second",
                "0 = Авто (по содержимому)\nПеретащите — применится в течение ~1 секунды"));
        content.addView(widthHint);

        TextView timeLabel = new TextView(this);
        timeLabel.setTextSize(15);
        timeLabel.setTextColor(COLOR_WHITE);
        timeLabel.setPadding(0, Math.round(48 * density), 0, Math.round(12 * density));
        timeLabel.setText(lang("时间设置", "Time Settings", "Настройки времени"));
        if (!tablet) content.addView(timeLabel);

        Button timeButton = new Button(this);
        timeButton.setText(lang("时间设置", "Time Settings", "Настройки времени"));
        timeButton.setAllCaps(false);
        timeButton.setTextColor(COLOR_WHITE);
        timeButton.setBackground(createGlassButtonBg(density));
        timeButton.setPadding(
                Math.round(24 * density), Math.round(14 * density),
                Math.round(24 * density), Math.round(14 * density));
        timeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, TimeSettingsActivity.class);
                startActivity(intent);
            }
        });
        if (!tablet) content.addView(timeButton, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));

        TextView langLabel = new TextView(this);
        langLabel.setTextSize(15);
        langLabel.setTextColor(COLOR_WHITE);
        langLabel.setPadding(0, Math.round(48 * density), 0, Math.round(12 * density));
        langLabel.setText(lang("语言", "Language", "Язык"));
        content.addView(langLabel);

        final RadioGroup langGroup = new RadioGroup(this);
        // 平板：3 个语言按钮横排一行；手机：保持竖排
        langGroup.setOrientation(tablet ? RadioGroup.HORIZONTAL : RadioGroup.VERTICAL);

        final RadioButton rbZh = createModeRadioButton(2001, "中文", density, tablet);
        final RadioButton rbEn = createModeRadioButton(2002, "English", density, tablet);
        final RadioButton rbRu = createModeRadioButton(2003, "Русский", density, tablet);

        langGroup.addView(rbZh);
        langGroup.addView(rbEn);
        langGroup.addView(rbRu);

        if (LANG_ZH.equals(mLanguage)) {
            langGroup.check(rbZh.getId());
        } else if (LANG_RU.equals(mLanguage)) {
            langGroup.check(rbRu.getId());
        } else {
            langGroup.check(rbEn.getId());
        }
        updateModeButtonStyles(density, langGroup, rbZh, rbEn, rbRu);

        langGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                updateModeButtonStyles(density, langGroup, rbZh, rbEn, rbRu);
                String newLang;
                if (checkedId == rbZh.getId()) {
                    newLang = LANG_ZH;
                } else if (checkedId == rbRu.getId()) {
                    newLang = LANG_RU;
                } else {
                    newLang = LANG_EN;
                }
                if (!newLang.equals(mLanguage)) {
                    getSharedPreferences(UI_PREFS_NAME, MODE_PRIVATE)
                            .edit().putString(KEY_LANGUAGE, newLang).apply();
                    recreate();
                }
            }
        });

        content.addView(langGroup);

        TextView aboutLabel = new TextView(this);
        aboutLabel.setTextSize(15);
        aboutLabel.setTextColor(COLOR_WHITE);
        aboutLabel.setPadding(0, Math.round(48 * density), 0, Math.round(12 * density));
        aboutLabel.setText(lang("关于", "About", "О приложении"));
        content.addView(aboutLabel);

        TextView aboutBody = new TextView(this);
        aboutBody.setTextSize(13);
        aboutBody.setTextColor(0xFFAAAAAA);
        aboutBody.setText(lang(
                "RamStatusBar\n状态栏内存显示 LSPosed 模块。\n需要 Root + LSPosed 环境。",
                "RamStatusBar\nAn LSPosed module for status bar RAM display.\nRequires Root + LSPosed.",
                "RamStatusBar\nМодуль LSPosed для отображения RAM в строке состояния.\nТребуются Root + LSPosed."));
        content.addView(aboutBody);

        // 显示当前设备信息按钮
        TextView deviceInfoLabel = new TextView(this);
        deviceInfoLabel.setTextSize(15);
        deviceInfoLabel.setTextColor(COLOR_WHITE);
        deviceInfoLabel.setPadding(0, Math.round(48 * density), 0, Math.round(12 * density));
        deviceInfoLabel.setText(lang("设备信息", "Device Info", "Информация об устройстве"));
        if (!tablet) content.addView(deviceInfoLabel);

        Button deviceInfoButton = new Button(this);
        deviceInfoButton.setText(lang("显示当前设备信息", "Show Device Info", "Показать информацию об устройстве"));
        deviceInfoButton.setAllCaps(false);
        deviceInfoButton.setTextColor(COLOR_WHITE);
        deviceInfoButton.setBackground(createGlassButtonBg(density));
        deviceInfoButton.setPadding(
                Math.round(24 * density), Math.round(14 * density),
                Math.round(24 * density), Math.round(14 * density));
        deviceInfoButton.setOnClickListener(v -> showDeviceInfoDialog());
        if (!tablet) content.addView(deviceInfoButton, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));

        if (tablet) {
            // 平板：三个操作按钮（背景颜色/时间设置/设备信息）横排一行，插入在滑块块之后
            content.removeView(colorLabel);
            content.removeView(colorButton);
            content.removeView(timeLabel);
            content.removeView(timeButton);
            content.removeView(deviceInfoLabel);
            content.removeView(deviceInfoButton);

            LinearLayout actionRow = new LinearLayout(this);
            actionRow.setOrientation(LinearLayout.HORIZONTAL);
            LinearLayout.LayoutParams colParams = new LinearLayout.LayoutParams(
                    0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            colParams.setMargins(Math.round(4 * density), 0, Math.round(4 * density), 0);
            actionRow.addView(buildLabelButtonColumn(colorLabel, colorButton, density), colParams);
            actionRow.addView(buildLabelButtonColumn(timeLabel, timeButton, density), colParams);
            actionRow.addView(buildLabelButtonColumn(deviceInfoLabel, deviceInfoButton, density), colParams);

            int insertIndex = content.indexOfChild(widthHint) + 1;
            content.addView(actionRow, insertIndex, new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT));
        }

        scrollView.addView(content, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        return scrollView;
    }

    // ==================== 左侧导航栏（平板专用，参考 Play 商店侧栏样式） ====================
    private View createSideNav(float density, int screenHeightPx) {
        FrameLayout navWrapper = new FrameLayout(this);

        LinearLayout navBar = new LinearLayout(this);
        navBar.setOrientation(LinearLayout.VERTICAL);
        navBar.setGravity(Gravity.CENTER);
        navBar.setPadding(
                Math.round(8 * density), Math.round(8 * density),
                Math.round(8 * density), Math.round(8 * density));

        GlassButtonDrawable navBg = new GlassButtonDrawable(
                Math.round(28 * density), Math.round(1 * density), false);
        navBar.setBackground(navBg);

        // 悬浮胶囊：垂直居中，高度约为屏幕一半，不铺满
        int navWidth = Math.round(96 * density);
        int navHeight = screenHeightPx / 2;
        FrameLayout.LayoutParams navBarParams = new FrameLayout.LayoutParams(navWidth, navHeight);
        navWrapper.addView(navBar, navBarParams);

        mNavHome = createNavItem(R.drawable.ic_home,
                lang("主页", "Home", "Главная"), density);
        mNavConfig = createNavItem(R.drawable.ic_config,
                lang("配置", "Config", "Конфиг"), density);
        mNavSettings = createNavItem(R.drawable.ic_settings,
                lang("设置", "Settings", "Настройки"), density);

        mIconHome = (ImageView) ((LinearLayout) mNavHome).getChildAt(0);
        mLabelHome = (TextView) ((LinearLayout) mNavHome).getChildAt(1);
        mIconConfig = (ImageView) ((LinearLayout) mNavConfig).getChildAt(0);
        mLabelConfig = (TextView) ((LinearLayout) mNavConfig).getChildAt(1);
        mIconSettings = (ImageView) ((LinearLayout) mNavSettings).getChildAt(0);
        mLabelSettings = (TextView) ((LinearLayout) mNavSettings).getChildAt(1);

        mNavHome.setOnClickListener(v -> switchTab(TAB_HOME));
        mNavConfig.setOnClickListener(v -> switchTab(TAB_CONFIG));
        mNavSettings.setOnClickListener(v -> switchTab(TAB_SETTINGS));

        // 三个导航项均分胶囊高度
        LinearLayout.LayoutParams itemParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f);
        itemParams.gravity = Gravity.CENTER;

        navBar.addView(mNavHome, itemParams);
        navBar.addView(mNavConfig, itemParams);
        navBar.addView(mNavSettings, itemParams);

        return navWrapper;
    }

    // ==================== 底部导航栏（手机） ====================
    private View createBottomNav(float density) {
        FrameLayout navWrapper = new FrameLayout(this);

        LinearLayout navBar = new LinearLayout(this);
        navBar.setOrientation(LinearLayout.HORIZONTAL);
        navBar.setGravity(Gravity.CENTER);
        navBar.setPadding(
                Math.round(8 * density), Math.round(8 * density),
                Math.round(8 * density), Math.round(8 * density));

        GlassButtonDrawable navBg = new GlassButtonDrawable(
                Math.round(28 * density), Math.round(1 * density), false);
        navBar.setBackground(navBg);

        FrameLayout.LayoutParams navBarParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                Math.round(76 * density));
        navWrapper.addView(navBar, navBarParams);

        mNavHome = createNavItem(R.drawable.ic_home,
                lang("主页", "Home", "Главная"), density);
        mNavConfig = createNavItem(R.drawable.ic_config,
                lang("配置", "Config", "Конфиг"), density);
        mNavSettings = createNavItem(R.drawable.ic_settings,
                lang("设置", "Settings", "Настройки"), density);

        mIconHome = (ImageView) ((LinearLayout) mNavHome).getChildAt(0);
        mLabelHome = (TextView) ((LinearLayout) mNavHome).getChildAt(1);
        mIconConfig = (ImageView) ((LinearLayout) mNavConfig).getChildAt(0);
        mLabelConfig = (TextView) ((LinearLayout) mNavConfig).getChildAt(1);
        mIconSettings = (ImageView) ((LinearLayout) mNavSettings).getChildAt(0);
        mLabelSettings = (TextView) ((LinearLayout) mNavSettings).getChildAt(1);

        mNavHome.setOnClickListener(v -> switchTab(TAB_HOME));
        mNavConfig.setOnClickListener(v -> switchTab(TAB_CONFIG));
        mNavSettings.setOnClickListener(v -> switchTab(TAB_SETTINGS));

        LinearLayout.LayoutParams itemParams = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.MATCH_PARENT, 1f);
        itemParams.gravity = Gravity.CENTER;

        navBar.addView(mNavHome, itemParams);
        navBar.addView(mNavConfig, itemParams);
        navBar.addView(mNavSettings, itemParams);

        return navWrapper;
    }

    private View createNavItem(int iconRes, String label, float density) {
        LinearLayout item = new LinearLayout(this);
        item.setOrientation(LinearLayout.VERTICAL);
        item.setGravity(Gravity.CENTER);
        item.setPadding(
                Math.round(4 * density), Math.round(4 * density),
                Math.round(4 * density), Math.round(4 * density));

        ImageView icon = new ImageView(this);
        icon.setImageResource(iconRes);
        icon.setScaleType(ImageView.ScaleType.FIT_CENTER);
        LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(
                Math.round(24 * density), Math.round(24 * density));
        iconParams.gravity = Gravity.CENTER;
        item.addView(icon, iconParams);

        TextView text = new TextView(this);
        text.setText(label);
        text.setTextSize(12);
        text.setGravity(Gravity.CENTER);
        text.setPadding(0, Math.round(2 * density), 0, 0);
        LinearLayout.LayoutParams textParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        textParams.gravity = Gravity.CENTER;
        item.addView(text, textParams);

        return item;
    }

    private GlassButtonDrawable createGlassButtonBg(float density) {
        return new GlassButtonDrawable(Math.round(28 * density), Math.round(1 * density), false);
    }

    private RadioButton createModeRadioButton(int id, String text, float density) {
        return createModeRadioButton(id, text, density, false);
    }

    private RadioButton createModeRadioButton(int id, String text, float density, boolean horizontal) {
        RadioButton rb = new RadioButton(this);
        rb.setId(id);
        rb.setText(text);
        rb.setTextColor(COLOR_WHITE);
        rb.setTextSize(14);
        rb.setGravity(horizontal ? Gravity.CENTER : Gravity.CENTER_VERTICAL);
        rb.setButtonDrawable(null);
        rb.setPadding(
                Math.round(24 * density), Math.round(16 * density),
                Math.round(24 * density), Math.round(16 * density));
        rb.setBackground(createGlassButtonBg(density));
        RadioGroup.LayoutParams params;
        if (horizontal) {
            // 平板横排：等宽按钮 + 左右留隙
            params = new RadioGroup.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            params.setMargins(Math.round(6 * density), 0, Math.round(6 * density), 0);
        } else {
            params = new RadioGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT);
            params.topMargin = Math.round(12 * density);
        }
        rb.setLayoutParams(params);
        return rb;
    }

    // 平板：标签 + 按钮组成一列，用于横排组合
    private LinearLayout buildLabelButtonColumn(TextView label, View button, float density) {
        LinearLayout column = new LinearLayout(this);
        column.setOrientation(LinearLayout.VERTICAL);
        label.setPadding(0, Math.round(24 * density), 0, Math.round(12 * density));
        column.addView(label, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        column.addView(button, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        return column;
    }

    private void updateModeButtonStyles(float density, RadioGroup group,
                                        RadioButton... buttons) {
        for (RadioButton rb : buttons) {
            boolean selected = (group.getCheckedRadioButtonId() == rb.getId());
            if (rb.getBackground() instanceof GlassButtonDrawable) {
                ((GlassButtonDrawable) rb.getBackground()).setGlassSelected(selected);
            } else {
                rb.setBackground(new GlassButtonDrawable(
                        Math.round(28 * density),
                        Math.round((selected ? 2 : 1) * density),
                        selected));
            }
            rb.setTextColor(selected ? COLOR_ACCENT : COLOR_WHITE);
        }
    }

    // ==================== 标签切换 ====================
    private void switchTab(int tab) {
        mCurrentTab = tab;
        mHomePage.setVisibility(tab == TAB_HOME ? View.VISIBLE : View.GONE);
        mConfigPage.setVisibility(tab == TAB_CONFIG ? View.VISIBLE : View.GONE);
        mSettingsPage.setVisibility(tab == TAB_SETTINGS ? View.VISIBLE : View.GONE);
        updateNavSelection();
        if (tab == TAB_HOME) {
            startDeepSleepUpdates();
        } else {
            stopDeepSleepUpdates();
        }
    }

    private void updateNavSelection() {
        setNavItemSelected(mNavHome, mIconHome, mLabelHome, false);
        setNavItemSelected(mNavConfig, mIconConfig, mLabelConfig, false);
        setNavItemSelected(mNavSettings, mIconSettings, mLabelSettings, false);
        if (mCurrentTab == TAB_HOME) {
            setNavItemSelected(mNavHome, mIconHome, mLabelHome, true);
        } else if (mCurrentTab == TAB_CONFIG) {
            setNavItemSelected(mNavConfig, mIconConfig, mLabelConfig, true);
        } else {
            setNavItemSelected(mNavSettings, mIconSettings, mLabelSettings, true);
        }
    }

    private void setNavItemSelected(View item, ImageView icon, TextView label, boolean selected) {
        if (selected) {
            GradientDrawable bg = new GradientDrawable();
            bg.setColor(COLOR_TAB_SELECTED_BG);
            bg.setCornerRadius(1000);
            item.setBackground(bg);
            icon.setColorFilter(COLOR_ACCENT, PorterDuff.Mode.SRC_IN);
            label.setTextColor(COLOR_ACCENT);
        } else {
            item.setBackground(null);
            icon.setColorFilter(COLOR_WHITE, PorterDuff.Mode.SRC_IN);
            label.setTextColor(COLOR_WHITE);
        }
    }

    // ==================== 生命周期 ====================
    @Override
    protected void onResume() {
        super.onResume();
        if (mCurrentTab == TAB_HOME) {
            startDeepSleepUpdates();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopDeepSleepUpdates();
    }

    // ==================== 深度休眠 ====================
    private void startDeepSleepUpdates() {
        stopDeepSleepUpdates();
        mDeepSleepUpdater = new Runnable() {
            @Override
            public void run() {
                if (mDeepSleepText != null) {
                    mDeepSleepText.setText(formatDeepSleepLine());
                }
                mUiHandler.postDelayed(this, 1000);
            }
        };
        mUiHandler.post(mDeepSleepUpdater);
    }

    private void stopDeepSleepUpdates() {
        if (mDeepSleepUpdater != null) {
            mUiHandler.removeCallbacks(mDeepSleepUpdater);
            mDeepSleepUpdater = null;
        }
    }

    private String formatDeepSleepLine() {
        long elapsed = SystemClock.elapsedRealtime();
        long awake = SystemClock.uptimeMillis();
        long deepSleepMs = elapsed - awake;
        int percent = elapsed > 0
                ? (int) Math.round(deepSleepMs * 100.0 / elapsed)
                : 0;
        long totalSeconds = deepSleepMs / 1000;
        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;
        return String.format(Locale.getDefault(),
                "%d:%02d:%02d (%d%%)", hours, minutes, seconds, percent);
    }

    // ==================== 双语文本工具 ====================
    private void setBilingualText(TextView view, String zhText, String enText, String ruText, int widthPx) {
        view.setText(lang(zhText, enText, ruText));
        if (widthPx > 0) {
            int linesZh = countLines(view, zhText, widthPx);
            int linesEn = countLines(view, enText, widthPx);
            int linesRu = countLines(view, ruText, widthPx);
            view.setMinLines(Math.max(Math.max(linesZh, linesEn), linesRu));
        }
    }

    private int countLines(TextView view, String text, int widthPx) {
        try {
            TextPaint paint = view.getPaint();
            StaticLayout layout = StaticLayout.Builder
                    .obtain(text, 0, text.length(), paint, widthPx)
                    .build();
            return layout.getLineCount();
        } catch (Throwable t) {
            return 1;
        }
    }

    // ==================== 设备信息对话框 ====================
    private void showDeviceInfoDialog() {
        StringBuilder sb = new StringBuilder();
        try {
            android.app.ActivityManager am = (android.app.ActivityManager) getSystemService(ACTIVITY_SERVICE);
            android.app.ActivityManager.MemoryInfo memInfo = new android.app.ActivityManager.MemoryInfo();
            am.getMemoryInfo(memInfo);
            long totalMb = memInfo.totalMem / (1024 * 1024);
            long availMb = memInfo.availMem / (1024 * 1024);
            sb.append(lang("总内存: ", "Total RAM: ", "ОЗУ: ")).append(totalMb).append(" MB\n");
            sb.append(lang("可用内存: ", "Available: ", "Доступно: ")).append(availMb).append(" MB\n\n");
        } catch (Throwable ignored) {}

        try {
            String androidId = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
            sb.append(lang("Android ID: ", "Android ID: ", "Android ID: ")).append(androidId).append("\n");
        } catch (Throwable ignored) {}

        sb.append(lang("型号: ", "Model: ", "Модель: ")).append(Build.MODEL).append("\n");
        sb.append(lang("厂商: ", "Manufacturer: ", "Производитель: ")).append(Build.MANUFACTURER).append("\n");
        sb.append(lang("品牌: ", "Brand: ", "Бренд: ")).append(Build.BRAND).append("\n");
        sb.append(lang("Android 版本: ", "Android version: ", "Версия Android: ")).append(Build.VERSION.RELEASE).append("\n");
        sb.append(lang("SDK: ", "SDK: ", "SDK: ")).append(Build.VERSION.SDK_INT).append("\n");
        sb.append(lang("Build: ", "Build: ", "Build: ")).append(Build.DISPLAY).append("\n");
        sb.append(lang("指纹: ", "Fingerprint: ", "Отпечаток: ")).append(Build.FINGERPRINT);

        new AlertDialog.Builder(this)
                .setTitle(lang("当前设备信息", "Current Device Info", "Текущая информация об устройстве"))
                .setMessage(sb.toString())
                .setPositiveButton(lang("确定", "OK", "ОК"), null)
                .show();
    }

    // ==================== 模式读写 ====================
    private int readCurrentModeOrDefault() {
        try {
            java.io.File f = new java.io.File(CONFIG_FILE);
            if (!f.exists()) {
                return MODE_TIME_RAM;
            }
            java.io.BufferedReader br = new java.io.BufferedReader(
                    new java.io.FileReader(f));
            String line = br.readLine();
            br.close();
            if (line == null) {
                return MODE_TIME_RAM;
            }
            return Integer.parseInt(line.trim());
        } catch (Throwable t) {
            return MODE_TIME_RAM;
        }
    }

    private boolean writeModeToFile(int mode) {
        return RootUtils.exec(
                "echo " + mode + " > " + CONFIG_FILE
                        + " && chmod 666 " + CONFIG_FILE
        );
    }

    // ==================== 胶囊宽度读写 ====================
    private int readCurrentWidth() {
        try {
            File f = new File(WIDTH_FILE);
            if (!f.exists()) {
                return 0;
            }
            BufferedReader br = new BufferedReader(new FileReader(f));
            String line = br.readLine();
            br.close();
            if (line == null) {
                return 0;
            }
            int value = Integer.parseInt(line.trim());
            return Math.max(0, Math.min(value, 300));
        } catch (Throwable t) {
            return 0;
        }
    }

    private boolean writeWidthToFile(int px) {
        return RootUtils.exec(
                "echo " + px + " > " + WIDTH_FILE
                        + " && chmod 666 " + WIDTH_FILE
        );
    }

    private void updateWidthLabel(TextView tv, int px) {
        tv.setText(px <= 0
                ? lang("自动", "Auto", "Авто")
                : px + " px");
    }

    /*
     * 点击胶囊宽度数值框 → 弹出数字输入对话框。
     * 输入 0~300（像素），0 = 自动；确认后写入并同步滑块。
     */
    private void showWidthInputDialog(
            final SeekBar seek,
            final TextView label) {

        final EditText input =
                new EditText(
                        MainActivity.this
                );

        input.setInputType(
                android.text.InputType.TYPE_CLASS_NUMBER
        );

        input.setText(
                String.valueOf(
                        readCurrentWidth()
                )
        );

        input.setSelection(
                input.getText().length()
        );

        new AlertDialog.Builder(
                MainActivity.this
        )
                .setTitle(
                        lang(
                                "手动输入胶囊宽度",
                                "Enter capsule width",
                                "Введите ширину капсулы"
                        )
                )
                .setMessage(
                        lang(
                                "输入 0~300（像素），0 = 自动",
                                "Enter 0~300 (px), 0 = Auto",
                                "Введите 0~300 (px), 0 = Авто"
                        )
                )
                .setView(input)
                .setPositiveButton(
                        lang(
                                "确定",
                                "OK",
                                "ОК"
                        ),
                        (d, w) -> {

                            try {

                                int value =
                                        Integer.parseInt(
                                                input.getText()
                                                        .toString()
                                                        .trim()
                                        );

                                if (value < 0
                                        || value > 300) {

                                    Toast.makeText(
                                            MainActivity.this,
                                            lang(
                                                    "请输入 0~300 之间的数值",
                                                    "Enter a value between 0 and 300",
                                                    "Введите значение от 0 до 300"
                                            ),
                                            Toast.LENGTH_SHORT
                                    ).show();

                                    return;
                                }

                                writeWidthToFile(
                                        value
                                );

                                updateWidthLabel(
                                        label,
                                        value
                                );

                                seek.setProgress(
                                        value
                                );

                            } catch (Throwable t) {

                                Toast.makeText(
                                        MainActivity.this,
                                        lang(
                                                "输入无效",
                                                "Invalid input",
                                                "Неверный ввод"
                                        ),
                                        Toast.LENGTH_SHORT
                                ).show();
                            }
                        }
                )
                .setNegativeButton(
                        lang(
                                "取消",
                                "Cancel",
                                "Отмена"
                        ),
                        null
                )
                .show();
    }
}
