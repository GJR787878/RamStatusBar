package com.example.ramstatusbar;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.DataOutputStream;
import java.util.Locale;

public class MainActivity extends Activity {

    private static final String CONFIG_FILE =
            "/data/local/tmp/ramstatusbar_mode";
    private static final int MODE_TIME_ONLY = 0;
    private static final int MODE_TIME_RAM = 1;
    private static final int MODE_RAM_ONLY = 2;

    private static final String UI_PREFS_NAME = "ui_prefs";
    private static final String KEY_LANGUAGE = "language";
    private static final String LANG_ZH = "zh";
    private static final String LANG_EN = "en";

    private static final int TAB_HOME = 0;
    private static final int TAB_CONFIG = 1;
    private static final int TAB_SETTINGS = 2;

    private static final int COLOR_ACCENT = 0xFF0A84FF;
    private static final int COLOR_WHITE = 0xFFFFFFFF;
    private static final int COLOR_NAV_BG = 0xB31C1C1E;
    private static final int COLOR_NAV_BORDER = 0x40FFFFFF;
    private static final int COLOR_TAB_SELECTED_BG = 0x2EFFFFFF;

    private boolean mEnglish;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SharedPreferences uiPrefs = getSharedPreferences(UI_PREFS_NAME, MODE_PRIVATE);
        mEnglish = LANG_EN.equals(uiPrefs.getString(KEY_LANGUAGE, LANG_ZH));

        float density = getResources().getDisplayMetrics().density;
        int screenWidthPx = getResources().getDisplayMetrics().widthPixels;
        final int contentWidthPx = screenWidthPx
                - Math.round(48 * density) - Math.round(48 * density);

        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(0xFF000000);

        // 内容容器
        FrameLayout contentContainer = new FrameLayout(this);
        FrameLayout.LayoutParams containerParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT);
        root.addView(contentContainer, containerParams);

        // 创建三个页面
        mHomePage = createHomePage(density, contentWidthPx);
        mConfigPage = createConfigPage(density, contentWidthPx);
        mSettingsPage = createSettingsPage(density, contentWidthPx);

        contentContainer.addView(mHomePage);
        contentContainer.addView(mConfigPage);
        contentContainer.addView(mSettingsPage);

        // 底部胶囊导航栏
        View bottomNav = createBottomNav(density);
        FrameLayout.LayoutParams navParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        navParams.gravity = Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL;
        navParams.leftMargin = Math.round(24 * density);
        navParams.rightMargin = Math.round(24 * density);
        navParams.bottomMargin = Math.round(24 * density);
        root.addView(bottomNav, navParams);

        setContentView(root);

        // 默认显示主页
        switchTab(TAB_HOME);
    }

    // ==================== 主页：软件介绍 + 深度休眠 ====================
    private View createHomePage(float density, int contentWidthPx) {
        ScrollView scrollView = new ScrollView(this);
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(
                Math.round(48 * density),
                Math.round(96 * density),
                Math.round(48 * density),
                Math.round(140 * density)
        );

        TextView title = new TextView(this);
        title.setTextSize(22);
        title.setTextColor(COLOR_WHITE);
        title.setText(mEnglish ? "RAM Status Bar" : "RAM 状态栏显示");
        content.addView(title);

        TextView intro = new TextView(this);
        intro.setTextSize(14);
        intro.setTextColor(0xFFCCCCCC);
        intro.setPadding(0, Math.round(24 * density), 0, Math.round(32 * density));
        String introZh = "在状态栏时间旁实时显示可用内存，点击时钟可查看 CPU / GPU 占用与温度。"
                + "选择显示模式最多等 1 秒即可生效，不需要重启手机。";
        String introEn = "Shows available RAM beside the status bar clock. Tap the clock "
                + "to check CPU / GPU usage & temperature. Mode changes take effect "
                + "within 1 second, no reboot needed.";
        setBilingualText(intro, introZh, introEn, contentWidthPx);
        content.addView(intro);

        // 点击时钟说明
        TextView tapTitle = new TextView(this);
        tapTitle.setTextSize(15);
        tapTitle.setTextColor(COLOR_WHITE);
        tapTitle.setPadding(0, Math.round(32 * density), 0, Math.round(8 * density));
        tapTitle.setText(mEnglish ? "Tap the clock to check CPU / GPU" : "点击时钟查看 CPU / GPU");
        content.addView(tapTitle);

        TextView tapBody = new TextView(this);
        tapBody.setTextSize(13);
        tapBody.setTextColor(0xFFAAAAAA);
        String tapZh = "点击状态栏的时钟：第 1 次点击显示 CPU 占用率，第 2 次点击显示 GPU 占用率，"
                + "第 3 次点击回到正常显示；10 秒内不再点击也会自动回到正常显示。\n\n"
                + "GPU 占用率依赖具体芯片的私有接口，部分设备上可能会显示 \"GPU N/A\"。";
        String tapEn = "Tap the status bar clock: 1st tap shows CPU usage, 2nd tap shows "
                + "GPU usage, 3rd tap returns to normal. Auto-restores after 10s.\n\n"
                + "GPU usage relies on chip-specific sysfs paths and may show \"GPU N/A\" "
                + "on some devices.";
        setBilingualText(tapBody, tapZh, tapEn, contentWidthPx);
        content.addView(tapBody);

        // 深度休眠
        TextView deepSleepTitle = new TextView(this);
        deepSleepTitle.setTextSize(15);
        deepSleepTitle.setTextColor(COLOR_WHITE);
        deepSleepTitle.setPadding(0, Math.round(48 * density), 0, Math.round(8 * density));
        deepSleepTitle.setText(mEnglish ? "Deep sleep" : "深度休眠");
        content.addView(deepSleepTitle);

        TextView deepSleepDesc = new TextView(this);
        deepSleepDesc.setTextSize(13);
        deepSleepDesc.setTextColor(0xFFAAAAAA);
        String dsZh = "下面显示的是开机以来设备处于深度休眠状态的时长和占比，跟 \"关于本机\" 里的数值一致。";
        String dsEn = "How much time since boot the device has spent in deep sleep, "
                + "same figure as in About Phone.";
        setBilingualText(deepSleepDesc, dsZh, dsEn, contentWidthPx);
        content.addView(deepSleepDesc);

        mDeepSleepText = new TextView(this);
        mDeepSleepText.setTextSize(16);
        mDeepSleepText.setTextColor(COLOR_ACCENT);
        mDeepSleepText.setPadding(0, Math.round(16 * density), 0, 0);
        content.addView(mDeepSleepText);

        scrollView.addView(content, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        return scrollView;
    }

    // ==================== 配置页：显示模式选择 ====================
    private View createConfigPage(float density, int contentWidthPx) {
        ScrollView scrollView = new ScrollView(this);
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(
                Math.round(48 * density),
                Math.round(96 * density),
                Math.round(48 * density),
                Math.round(140 * density)
        );

        TextView title = new TextView(this);
        title.setTextSize(22);
        title.setTextColor(COLOR_WHITE);
        title.setText(mEnglish ? "Display Mode" : "显示模式");
        content.addView(title);

        TextView desc = new TextView(this);
        desc.setTextSize(14);
        desc.setTextColor(0xFFCCCCCC);
        desc.setPadding(0, Math.round(24 * density), 0, Math.round(32 * density));
        String descZh = "选择状态栏的显示内容，最多等 1 秒即可生效。";
        String descEn = "Choose what the status bar shows. Changes take effect within 1 second.";
        setBilingualText(desc, descZh, descEn, contentWidthPx);
        content.addView(desc);

        int currentMode = readCurrentModeOrDefault();

        final RadioGroup radioGroup = new RadioGroup(this);
        radioGroup.setOrientation(RadioGroup.VERTICAL);

        final RadioButton rbTimeOnly = createModeRadioButton(
                1001, mEnglish ? "Time only" : "仅显示时间", density);
        final RadioButton rbTimeRam = createModeRadioButton(
                1002, mEnglish ? "Time + RAM (e.g. 21:11 2.5G/8G)" : "时间 + 内存 (如 21:11 2.5G/8G)", density);
        final RadioButton rbRamOnly = createModeRadioButton(
                1003, mEnglish ? "RAM only (e.g. 2.5G/8G)" : "仅显示内存 (如 2.5G/8G)", density);

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
                            mEnglish ? "Write failed, please check root permission"
                                    : "写入失败，请检查是否已授予 root 权限",
                            Toast.LENGTH_LONG).show();
                }
            }
        });

        content.addView(radioGroup);

        // 安装说明
        TextView setupTitle = new TextView(this);
        setupTitle.setTextSize(15);
        setupTitle.setTextColor(COLOR_WHITE);
        setupTitle.setPadding(0, Math.round(64 * density), 0, Math.round(8 * density));
        setupTitle.setText(mEnglish ? "Setup" : "安装说明");
        content.addView(setupTitle);

        TextView setupBody = new TextView(this);
        setupBody.setTextSize(13);
        setupBody.setTextColor(0xFFAAAAAA);
        setupBody.setText(mEnglish
                ? "1. In LSPosed / Vector Manager, enable this module and check the scope "
                + "com.android.systemui.\n"
                + "2. Reboot once after the first install.\n"
                + "3. Switching display mode requires root; allow the prompt on first use.\n"
                + "4. Total RAM is auto-detected and rounded to common specs (8/12/16/24G)."
                : "1. 到 LSPosed / Vector Manager 里，对本模块勾选作用域 com.android.systemui 并启用。\n"
                + "2. 首次安装完成后需要重启一次手机。\n"
                + "3. 切换显示模式需要 root 权限，首次切换会弹出授权请求，请点击允许。\n"
                + "4. 总内存会自动检测并取整到常见规格 (8/12/16/24G等)。");
        content.addView(setupBody);

        scrollView.addView(content, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        return scrollView;
    }

    // ==================== 设置页：背景颜色 + 语言切换 ====================
    private View createSettingsPage(float density, int contentWidthPx) {
        ScrollView scrollView = new ScrollView(this);
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(
                Math.round(48 * density),
                Math.round(96 * density),
                Math.round(48 * density),
                Math.round(140 * density)
        );

        TextView title = new TextView(this);
        title.setTextSize(22);
        title.setTextColor(COLOR_WHITE);
        title.setText(mEnglish ? "Settings" : "设置");
        content.addView(title);

        // 背景颜色按钮
        TextView colorLabel = new TextView(this);
        colorLabel.setTextSize(15);
        colorLabel.setTextColor(COLOR_WHITE);
        colorLabel.setPadding(0, Math.round(40 * density), 0, Math.round(12 * density));
        colorLabel.setText(mEnglish ? "Background color" : "背景颜色");
        content.addView(colorLabel);

        Button colorButton = new Button(this);
        colorButton.setText(mEnglish ? "Pick background color" : "选择背景颜色");
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
        LinearLayout.LayoutParams colorBtnParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        content.addView(colorButton, colorBtnParams);

        // 语言切换
        TextView langLabel = new TextView(this);
        langLabel.setTextSize(15);
        langLabel.setTextColor(COLOR_WHITE);
        langLabel.setPadding(0, Math.round(48 * density), 0, Math.round(12 * density));
        langLabel.setText(mEnglish ? "Language" : "语言");
        content.addView(langLabel);

        Button langButton = new Button(this);
        langButton.setText(mEnglish ? "Switch to 中文" : "Switch to English");
        langButton.setAllCaps(false);
        langButton.setTextColor(COLOR_WHITE);
        langButton.setBackground(createGlassButtonBg(density));
        langButton.setPadding(
                Math.round(24 * density), Math.round(14 * density),
                Math.round(24 * density), Math.round(14 * density));
        langButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SharedPreferences prefs = getSharedPreferences(UI_PREFS_NAME, MODE_PRIVATE);
                String newLang = mEnglish ? LANG_ZH : LANG_EN;
                prefs.edit().putString(KEY_LANGUAGE, newLang).apply();
                recreate();
            }
        });
        LinearLayout.LayoutParams langBtnParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        content.addView(langButton, langBtnParams);

        // 关于
        TextView aboutLabel = new TextView(this);
        aboutLabel.setTextSize(15);
        aboutLabel.setTextColor(COLOR_WHITE);
        aboutLabel.setPadding(0, Math.round(48 * density), 0, Math.round(12 * density));
        aboutLabel.setText(mEnglish ? "About" : "关于");
        content.addView(aboutLabel);

        TextView aboutBody = new TextView(this);
        aboutBody.setTextSize(13);
        aboutBody.setTextColor(0xFFAAAAAA);
        aboutBody.setText(mEnglish
                ? "RamStatusBar\nAn LSPosed module for status bar RAM display.\nRequires Root + LSPosed."
                : "RamStatusBar\n状态栏内存显示 LSPosed 模块。\n需要 Root + LSPosed 环境。");
        content.addView(aboutBody);

        scrollView.addView(content, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        return scrollView;
    }

    // ==================== 底部胶囊导航栏（半透明玻璃质感） ====================
    private View createBottomNav(float density) {
        // 外层容器，用于居中
        FrameLayout navWrapper = new FrameLayout(this);

        // 胶囊背景
        LinearLayout navBar = new LinearLayout(this);
        navBar.setOrientation(LinearLayout.HORIZONTAL);
        navBar.setGravity(Gravity.CENTER);
        navBar.setPadding(
                Math.round(8 * density),
                Math.round(8 * density),
                Math.round(8 * density),
                Math.round(8 * density)
        );

        GradientDrawable navBg = new GradientDrawable();
        navBg.setColor(COLOR_NAV_BG);
        navBg.setCornerRadius(Math.round(28 * density));
        navBg.setStroke(Math.round(1 * density), COLOR_NAV_BORDER);
        navBar.setBackground(navBg);

        FrameLayout.LayoutParams navBarParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                Math.round(76 * density));
        navWrapper.addView(navBar, navBarParams);

        // 三个导航项
        mNavHome = createNavItem(R.drawable.ic_home,
                mEnglish ? "Home" : "主页", density);
        mNavConfig = createNavItem(R.drawable.ic_config,
                mEnglish ? "Config" : "配置", density);
        mNavSettings = createNavItem(R.drawable.ic_settings,
                mEnglish ? "Settings" : "设置", density);

        mIconHome = (ImageView) ((LinearLayout) mNavHome).getChildAt(0);
        mLabelHome = (TextView) ((LinearLayout) mNavHome).getChildAt(1);
        mIconConfig = (ImageView) ((LinearLayout) mNavConfig).getChildAt(0);
        mLabelConfig = (TextView) ((LinearLayout) mNavConfig).getChildAt(1);
        mIconSettings = (ImageView) ((LinearLayout) mNavSettings).getChildAt(0);
        mLabelSettings = (TextView) ((LinearLayout) mNavSettings).getChildAt(1);

        mNavHome.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switchTab(TAB_HOME);
            }
        });
        mNavConfig.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switchTab(TAB_CONFIG);
            }
        });
        mNavSettings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switchTab(TAB_SETTINGS);
            }
        });

        LinearLayout.LayoutParams itemParams = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.MATCH_PARENT, 1f);
        itemParams.gravity = Gravity.CENTER;

        navBar.addView(mNavHome, itemParams);
        navBar.addView(mNavConfig, itemParams);
        navBar.addView(mNavSettings, itemParams);

        return navWrapper;
    }

    private GradientDrawable createGlassButtonBg(float density) {
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(COLOR_NAV_BG);
        bg.setCornerRadius(Math.round(28 * density));
        bg.setStroke(Math.round(1 * density), COLOR_NAV_BORDER);
        return bg;
    }

    private RadioButton createModeRadioButton(int id, String text, float density) {
        RadioButton rb = new RadioButton(this);
        rb.setId(id);
        rb.setText(text);
        rb.setTextColor(COLOR_WHITE);
        rb.setTextSize(14);
        rb.setGravity(Gravity.CENTER_VERTICAL);
        rb.setButtonDrawable(null);
        rb.setPadding(
                Math.round(24 * density), Math.round(16 * density),
                Math.round(24 * density), Math.round(16 * density));
        rb.setBackground(createGlassButtonBg(density));
        RadioGroup.LayoutParams params = new RadioGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        params.topMargin = Math.round(12 * density);
        rb.setLayoutParams(params);
        return rb;
    }

    private void updateModeButtonStyles(float density, RadioGroup group,
                                        RadioButton... buttons) {
        for (RadioButton rb : buttons) {
            boolean selected = (group.getCheckedRadioButtonId() == rb.getId());
            GradientDrawable bg = new GradientDrawable();
            bg.setColor(COLOR_NAV_BG);
            bg.setCornerRadius(Math.round(28 * density));
            if (selected) {
                bg.setStroke(Math.round(2 * density), COLOR_WHITE);
                rb.setTextColor(COLOR_ACCENT);
            } else {
                bg.setStroke(Math.round(1 * density), COLOR_NAV_BORDER);
                rb.setTextColor(COLOR_WHITE);
            }
            rb.setBackground(bg);
        }
    }

    private View createNavItem(int iconRes, String label, float density) {
        LinearLayout item = new LinearLayout(this);
        item.setOrientation(LinearLayout.VERTICAL);
        item.setGravity(Gravity.CENTER);
        item.setPadding(
                Math.round(4 * density),
                Math.round(4 * density),
                Math.round(4 * density),
                Math.round(4 * density)
        );

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

    // ==================== 标签切换 ====================
    private void switchTab(int tab) {
        mCurrentTab = tab;

        mHomePage.setVisibility(tab == TAB_HOME ? View.VISIBLE : View.GONE);
        mConfigPage.setVisibility(tab == TAB_CONFIG ? View.VISIBLE : View.GONE);
        mSettingsPage.setVisibility(tab == TAB_SETTINGS ? View.VISIBLE : View.GONE);

        updateNavSelection();

        // 深度休眠只在主页时更新
        if (tab == TAB_HOME) {
            startDeepSleepUpdates();
        } else {
            stopDeepSleepUpdates();
        }
    }

    private void updateNavSelection() {
        // 重置所有项
        setNavItemSelected(mNavHome, mIconHome, mLabelHome, false);
        setNavItemSelected(mNavConfig, mIconConfig, mLabelConfig, false);
        setNavItemSelected(mNavSettings, mIconSettings, mLabelSettings, false);

        // 高亮当前项
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
    private void setBilingualText(TextView view, String zhText, String enText, int widthPx) {
        view.setText(mEnglish ? enText : zhText);
        if (widthPx > 0) {
            int linesZh = countLines(view, zhText, widthPx);
            int linesEn = countLines(view, enText, widthPx);
            view.setMinLines(Math.max(linesZh, linesEn));
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
        try {
            Process su = Runtime.getRuntime().exec("su");
            DataOutputStream os = new DataOutputStream(su.getOutputStream());
            os.writeBytes("echo " + mode + " > " + CONFIG_FILE + "\n");
            os.writeBytes("chmod 666 " + CONFIG_FILE + "\n");
            os.writeBytes("exit\n");
            os.flush();
            int result = su.waitFor();
            return result == 0;
        } catch (Throwable t) {
            return false;
        }
    }
}
