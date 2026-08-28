package com.example.ramstatusbar;

import android.app.Activity;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.DataOutputStream;
import java.io.File;
import java.io.BufferedReader;
import java.io.FileReader;
import java.util.Locale;

public class MainActivity extends Activity {

    private static final String CONFIG_FILE =
            "/data/local/tmp/ramstatusbar_mode";

    private static final int MODE_TIME_ONLY = 0;
    private static final int MODE_TIME_RAM = 1;
    private static final int MODE_RAM_ONLY = 2;

    private static final String UI_PREFS_NAME = "ui_prefs";
    private static final String KEY_LANGUAGE = "language";
    private static final String KEY_BG_COLOR = "bg_color";

    private static final String LANG_ZH = "zh";
    private static final String LANG_EN = "en";

    private static final int DEFAULT_BG_COLOR =
            0x99000000;

    private static final int LANG_BUTTON_WIDTH_DP = 72;
    private static final int COLOR_BUTTON_WIDTH_DP = 100;

    private boolean mEnglish;

    private TextView mDeepSleepText;

    private final Handler mUiHandler =
            new Handler(Looper.getMainLooper());

    private Runnable mDeepSleepUpdater;

    @Override
    protected void onCreate(
            Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        SharedPreferences uiPrefs =
                getSharedPreferences(
                        UI_PREFS_NAME,
                        MODE_PRIVATE
                );

        mEnglish =
                LANG_EN.equals(
                        uiPrefs.getString(
                                KEY_LANGUAGE,
                                LANG_ZH
                        )
                );

        showMainPage();
    }

    private void showMainPage() {

        int currentMode =
                readCurrentModeOrDefault();

        int screenWidthPx =
                getResources()
                        .getDisplayMetrics()
                        .widthPixels;

        final int contentWidthPx =
                screenWidthPx - 96;

        FrameLayout frame =
                new FrameLayout(this);

        ScrollView scrollView =
                new ScrollView(this);

        LinearLayout content =
                new LinearLayout(this);

        content.setOrientation(
                LinearLayout.VERTICAL
        );

        content.setPadding(
                48,
                96,
                48,
                220
        );

        TextView title =
                new TextView(this);

        title.setTextSize(18);

        title.setText(
                mEnglish
                        ? "RAM Status Bar Display"
                        : "RAM 状态栏显示"
        );

        content.addView(title);

        TextView intro =
                new TextView(this);

        intro.setTextSize(14);

        intro.setPadding(
                0,
                24,
                0,
                32
        );

        String introZh =
                "选择下面的显示模式，最多等 1 秒即可生效，"
                + "不需要重启手机。";

        String introEn =
                "Pick a display mode below. Changes take effect "
                + "within 1 second, no reboot needed.";

        setBilingualText(
                intro,
                introZh,
                introEn,
                contentWidthPx
        );

        content.addView(intro);

        final RadioGroup radioGroup =
                new RadioGroup(this);

        radioGroup.setOrientation(
                RadioGroup.VERTICAL
        );

        final RadioButton rbTimeOnly =
                new RadioButton(this);

        rbTimeOnly.setId(1001);

        rbTimeOnly.setText(
                mEnglish
                        ? "Time only"
                        : "仅显示时间"
        );

        final RadioButton rbTimeRam =
                new RadioButton(this);

        rbTimeRam.setId(1002);

        rbTimeRam.setText(
                mEnglish
                        ? "Time + RAM (e.g. 21:11 2.5G/8G)"
                        : "时间 + 内存 (如 21:11 2.5G/8G)"
        );

        final RadioButton rbRamOnly =
                new RadioButton(this);

        rbRamOnly.setId(1003);

        rbRamOnly.setText(
                mEnglish
                        ? "RAM only (e.g. 2.5G/8G)"
                        : "仅显示内存 (如 2.5G/8G)"
        );

        radioGroup.addView(rbTimeOnly);
        radioGroup.addView(rbTimeRam);
        radioGroup.addView(rbRamOnly);

        content.addView(radioGroup);

        if (currentMode == MODE_TIME_ONLY) {

            radioGroup.check(
                    rbTimeOnly.getId()
            );

        } else if (currentMode == MODE_RAM_ONLY) {

            radioGroup.check(
                    rbRamOnly.getId()
            );

        } else {

            radioGroup.check(
                    rbTimeRam.getId()
            );
        }

        radioGroup.setOnCheckedChangeListener(
                new RadioGroup.OnCheckedChangeListener() {

                    @Override
                    public void onCheckedChanged(
                            RadioGroup group,
                            int checkedId) {

                        int mode;

                        if (checkedId ==
                                rbTimeOnly.getId()) {

                            mode =
                                    MODE_TIME_ONLY;

                        } else if (checkedId ==
                                rbRamOnly.getId()) {

                            mode =
                                    MODE_RAM_ONLY;

                        } else {

                            mode =
                                    MODE_TIME_RAM;
                        }

                        boolean ok =
                                writeModeToFile(mode);

                        if (!ok) {

                            Toast.makeText(
                                    MainActivity.this,
                                    mEnglish
                                            ? "Write failed, please check root permission"
                                            : "写入失败，请检查是否已授予 root 权限",
                                    Toast.LENGTH_LONG
                            ).show();
                        }
                    }
                }
        );

        View divider =
                new View(this);

        divider.setBackgroundColor(
                0x33FFFFFF
        );

        LinearLayout.LayoutParams dividerParams =
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        2
                );

        dividerParams.topMargin = 64;
        dividerParams.bottomMargin = 24;

        content.addView(
                divider,
                dividerParams
        );

        TextView tapFeatureTitle =
                new TextView(this);

        tapFeatureTitle.setTextSize(15);

        tapFeatureTitle.setPadding(
                0,
                32,
                0,
                8
        );

        tapFeatureTitle.setText(
                mEnglish
                        ? "Tap the clock to check CPU / GPU"
                        : "点击时钟查看 CPU / GPU"
        );

        content.addView(
                tapFeatureTitle
        );

        TextView tapFeatureBody =
                new TextView(this);

        tapFeatureBody.setTextSize(13);

        String tapZh =
                "点击状态栏的时钟：第 1 次点击显示 CPU 占用率，"
                + "第 2 次点击显示 GPU 占用率，第 3 次点击回到正常显示；"
                + "10 秒内不再点击也会自动回到正常显示。\n\n"
                + "GPU 占用率依赖具体芯片的私有接口，部分设备上可能会显示"
                + "\"GPU N/A\"，能否读取取决于你的芯片型号。";

        String tapEn =
                "Tap the status bar clock: 1st tap shows CPU usage, "
                + "2nd tap shows GPU usage, 3rd tap returns to normal. "
                + "If left untouched for 10 seconds it automatically "
                + "returns to normal as well.\n\n"
                + "GPU usage relies on chip-specific sysfs paths and "
                + "may show \"GPU N/A\" on some devices, depending "
                + "on your chipset.";

        setBilingualText(
                tapFeatureBody,
                tapZh,
                tapEn,
                contentWidthPx
        );

        content.addView(
                tapFeatureBody
        );

        TextView deepSleepTitle =
                new TextView(this);

        deepSleepTitle.setTextSize(15);

        deepSleepTitle.setPadding(
                0,
                96,
                0,
                8
        );

        deepSleepTitle.setText(
                mEnglish
                        ? "Deep sleep"
                        : "深度休眠"
        );

        content.addView(
                deepSleepTitle
        );

        TextView deepSleepDesc =
                new TextView(this);

        deepSleepDesc.setTextSize(13);

        String deepSleepZh =
                "下面显示的是开机以来设备处于深度休眠状态的时长和占比，"
                + "跟\"关于本机\"里的数值一致，不用再去系统设置里翻找。";

        String deepSleepEn =
                "Shown below is how much of the time since boot the device "
                + "has spent in deep sleep, same figure as in About Phone.";

        setBilingualText(
                deepSleepDesc,
                deepSleepZh,
                deepSleepEn,
                contentWidthPx
        );

        content.addView(
                deepSleepDesc
        );

        mDeepSleepText =
                new TextView(this);

        mDeepSleepText.setTextSize(14);

        mDeepSleepText.setPadding(
                0,
                16,
                0,
                0
        );

        content.addView(
                mDeepSleepText
        );

        TextView setupTitle =
                new TextView(this);

        setupTitle.setTextSize(15);

        setupTitle.setPadding(
                0,
                96,
                0,
                8
        );

        setupTitle.setText(
                mEnglish
                        ? "Setup"
                        : "安装说明"
        );

        content.addView(
                setupTitle
        );

        TextView setupBody =
                new TextView(this);

        setupBody.setTextSize(13);

        setupBody.setText(
                mEnglish
                        ? "1. In LSPosed / Vector Manager, enable this module "
                        + "and check the scope com.android.systemui.\n"
                        + "2. Reboot once after the first install for it to take effect.\n"
                        + "3. Switching the display mode above requires root; "
                        + "a permission prompt will appear the first time.\n"
                        + "4. Total RAM is auto-detected and rounded to a common spec."
                        : "1. 到 LSPosed / Vector Manager 里，对本模块勾选作用域 "
                        + "com.android.systemui 并启用模块。\n"
                        + "2. 首次安装完成后需要重启一次手机才会生效。\n"
                        + "3. 切换上面的显示模式需要 root 权限，首次切换会弹出授权请求。\n"
                        + "4. 总内存会自动检测并取整到常见规格。"
        );

        content.addView(
                setupBody
        );

        scrollView.addView(
                content,
                new ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                )
        );

        frame.addView(
                scrollView,
                new FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                )
        );

        /*
         * 底部按钮区域：
         *
         * [ 背景颜色 ] [ EN / 中文 ]
         *
         * 两个按钮在同一行。
         */
        LinearLayout bottomButtons =
                new LinearLayout(this);

        bottomButtons.setOrientation(
                LinearLayout.HORIZONTAL
        );

        bottomButtons.setGravity(
                Gravity.CENTER
        );

        /*
         * 背景颜色按钮。
         */
        Button colorButton =
                new Button(this);

        colorButton.setText(
                mEnglish
                        ? "Background"
                        : "背景颜色"
        );

        colorButton.setOnClickListener(
                new View.OnClickListener() {

                    @Override
                    public void onClick(View v) {

                        showColorPage();
                    }
                }
        );

        float density =
                getResources()
                        .getDisplayMetrics()
                        .density;

        int colorButtonWidthPx =
                Math.round(
                        COLOR_BUTTON_WIDTH_DP *
                                density
                );

        LinearLayout.LayoutParams colorParams =
                new LinearLayout.LayoutParams(
                        colorButtonWidthPx,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                );

        colorParams.rightMargin =
                Math.round(8 * density);

        bottomButtons.addView(
                colorButton,
                colorParams
        );

        /*
         * 中英文切换。
         */
        Button langButton =
                new Button(this);

        langButton.setText(
                mEnglish
                        ? "中文"
                        : "EN"
        );

        langButton.setOnClickListener(
                new View.OnClickListener() {

                    @Override
                    public void onClick(View v) {

                        SharedPreferences prefs =
                                getSharedPreferences(
                                        UI_PREFS_NAME,
                                        MODE_PRIVATE
                                );

                        String newLang =
                                mEnglish
                                        ? LANG_ZH
                                        : LANG_EN;

                        prefs.edit()
                                .putString(
                                        KEY_LANGUAGE,
                                        newLang
                                )
                                .apply();

                        recreate();
                    }
                }
        );

        int langButtonWidthPx =
                Math.round(
                        LANG_BUTTON_WIDTH_DP *
                                density
                );

        LinearLayout.LayoutParams langParams =
                new LinearLayout.LayoutParams(
                        langButtonWidthPx,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                );

        bottomButtons.addView(
                langButton,
                langParams
        );

        FrameLayout.LayoutParams bottomParams =
                new FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                );

        bottomParams.gravity =
                Gravity.BOTTOM | Gravity.END;

        bottomParams.setMargins(
                0,
                0,
                Math.round(24 * density),
                Math.round(24 * density)
        );

        frame.addView(
                bottomButtons,
                bottomParams
        );

        setContentView(frame);
    }

    /*
     * ==============================
     * 二级：背景颜色页面
     * ==============================
     */
    private void showColorPage() {

        final SharedPreferences prefs =
                getSharedPreferences(
                        UI_PREFS_NAME,
                        MODE_PRIVATE
                );

        int currentColor =
                prefs.getInt(
                        KEY_BG_COLOR,
                        DEFAULT_BG_COLOR
                );

        FrameLayout frame =
                new FrameLayout(this);

        LinearLayout content =
                new LinearLayout(this);

        content.setOrientation(
                LinearLayout.VERTICAL
        );

        content.setPadding(
                48,
                80,
                48,
                48
        );

        /*
         * 返回按钮。
         */
        Button backButton =
                new Button(this);

        backButton.setText(
                mEnglish
                        ? "← Back"
                        : "← 返回"
        );

        backButton.setOnClickListener(
                new View.OnClickListener() {

                    @Override
                    public void onClick(View v) {

                        showMainPage();
                    }
                }
        );

        content.addView(
                backButton,
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                )
        );

        TextView title =
                new TextView(this);

        title.setTextSize(20);

        title.setPadding(
                0,
                32,
                0,
                16
        );

        title.setText(
                mEnglish
                        ? "Background Color"
                        : "背景颜色"
        );

        content.addView(title);

        TextView description =
                new TextView(this);

        description.setTextSize(14);

        description.setText(
                mEnglish
                        ? "Choose a capsule background color."
                        : "选择状态栏胶囊背景颜色。"
        );

        content.addView(
                description
        );

        /*
         * 当前颜色预览。
         */
        final View preview =
                new View(this);

        GradientDrawable previewDrawable =
                new GradientDrawable();

        previewDrawable.setColor(
                currentColor
        );

        previewDrawable.setCornerRadius(
                100000.0f
        );

        preview.setBackground(
                previewDrawable
        );

        LinearLayout.LayoutParams previewParams =
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        100
                );

        previewParams.topMargin = 48;
        previewParams.bottomMargin = 48;

        content.addView(
                preview,
                previewParams
        );

        TextView presetTitle =
                new TextView(this);

        presetTitle.setTextSize(15);

        presetTitle.setText(
                mEnglish
                        ? "Preset colors"
                        : "预设颜色"
        );

        content.addView(
                presetTitle
        );

        /*
         * 预设颜色。
         */
        int[] colors = {
                0x99000000,
                0x99757575,
                0x99606060,
                0x993F51B5,
                0x994CAF50,
                0x99009688,
                0x99FF9800,
                0x99E91E63,
                0x996733AB,
                0x992196F3
        };

        String[] namesZh = {
                "黑色",
                "灰色",
                "深灰",
                "靛蓝",
                "绿色",
                "青色",
                "橙色",
                "粉色",
                "紫色",
                "蓝色"
        };

        String[] namesEn = {
                "Black",
                "Gray",
                "Dark Gray",
                "Indigo",
                "Green",
                "Teal",
                "Orange",
                "Pink",
                "Purple",
                "Blue"
        };

        LinearLayout colorGrid =
                new LinearLayout(this);

        colorGrid.setOrientation(
                LinearLayout.VERTICAL
        );

        for (int row = 0; row < 5; row++) {

            LinearLayout rowLayout =
                    new LinearLayout(this);

            rowLayout.setOrientation(
                    LinearLayout.HORIZONTAL
            );

            rowLayout.setGravity(
                    Gravity.CENTER
            );

            for (int col = 0; col < 2; col++) {

                final int index =
                        row * 2 + col;

                Button colorButton =
                        new Button(this);

                colorButton.setText(
                        mEnglish
                                ? namesEn[index]
                                : namesZh[index]
                );

                colorButton.setTextColor(
                        Color.WHITE
                );

                GradientDrawable buttonBg =
                        new GradientDrawable();

                buttonBg.setColor(
                        colors[index]
                );

                buttonBg.setCornerRadius(
                        100000.0f
                );

                colorButton.setBackground(
                        buttonBg
                );

                final int selectedColor =
                        colors[index];

                colorButton.setOnClickListener(
                        new View.OnClickListener() {

                            @Override
                            public void onClick(
                                    View v) {

                                prefs.edit()
                                        .putInt(
                                                KEY_BG_COLOR,
                                                selectedColor
                                        )
                                        .apply();

                                GradientDrawable d =
                                        new GradientDrawable();

                                d.setColor(
                                        selectedColor
                                );

                                d.setCornerRadius(
                                        100000.0f
                                );

                                preview.setBackground(d);

                                Toast.makeText(
                                        MainActivity.this,
                                        mEnglish
                                                ? "Color applied"
                                                : "颜色已应用",
                                        Toast.LENGTH_SHORT
                                ).show();
                            }
                        }
                );

                LinearLayout.LayoutParams params =
                        new LinearLayout.LayoutParams(
                                0,
                                56
                        );

                params.weight = 1;
                params.setMargins(
                        8,
                        8,
                        8,
                        8
                );

                rowLayout.addView(
                        colorButton,
                        params
                );
            }

            colorGrid.addView(
                    rowLayout,
                    new LinearLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT
                    )
            );
        }

        content.addView(
                colorGrid
        );

        /*
         * 自定义颜色。
         *
         * Android 原生系统会提供颜色选择器。
         */
        Button customButton =
                new Button(this);

        customButton.setText(
                mEnglish
                        ? "Custom color"
                        : "自定义颜色"
        );

        customButton.setOnClickListener(
                new View.OnClickListener() {

                    @Override
                    public void onClick(View v) {

                        showCustomColorDialog();
                    }
                }
        );

        LinearLayout.LayoutParams customParams =
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                );

        customParams.topMargin = 32;

        content.addView(
                customButton,
                customParams
        );

        ScrollView scroll =
                new ScrollView(this);

        scroll.addView(
                content,
                new ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                )
        );

        frame.addView(
                scroll,
                new FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                )
        );

        setContentView(frame);
    }

    /*
     * 自定义颜色输入。
     *
     * 这里先使用 Android 原生的颜色值输入方式，
     * 输入例如：
     *
     * #9900FF00
     *
     * 前两位是透明度。
     */
    private void showCustomColorDialog() {

        final android.widget.EditText input =
                new android.widget.EditText(this);

        input.setSingleLine(true);

        input.setHint(
                "#9900FF00"
        );

        input.setText(
                String.format(
                        Locale.US,
                        "#%08X",
                        getSharedPreferences(
                                UI_PREFS_NAME,
                                MODE_PRIVATE
                        ).getInt(
                                KEY_BG_COLOR,
                                DEFAULT_BG_COLOR
                        )
                )
        );

        android.app.AlertDialog dialog =
                new android.app.AlertDialog.Builder(this)
                        .setTitle(
                                mEnglish
                                        ? "Custom color"
                                        : "自定义颜色"
                        )
                        .setMessage(
                                mEnglish
                                        ? "Enter ARGB color, for example #9900FF00."
                                        : "输入 ARGB 颜色，例如 #9900FF00。"
                        )
                        .setView(input)
                        .setNegativeButton(
                                mEnglish
                                        ? "Cancel"
                                        : "取消",
                                null
                        )
                        .setPositiveButton(
                                mEnglish
                                        ? "Apply"
                                        : "应用",
                                null
                        )
                        .create();

        dialog.setOnShowListener(
                d -> {

                    dialog.getButton(
                            android.app.AlertDialog.BUTTON_POSITIVE
                    ).setOnClickListener(
                            v -> {

                                try {

                                    String value =
                                            input.getText()
                                                    .toString()
                                                    .trim();

                                    if (!value.startsWith("#")) {
                                        value = "#" + value;
                                    }

                                    long parsed =
                                            Long.parseLong(
                                                    value.substring(1),
                                                    16
                                            );

                                    int color;

                                    if (value.length() == 7) {

                                        color =
                                                0xFF000000 |
                                                        (int) parsed;

                                    } else if (value.length() == 9) {

                                        color =
                                                (int) parsed;

                                    } else {

                                        throw new IllegalArgumentException();
                                    }

                                    getSharedPreferences(
                                            UI_PREFS_NAME,
                                            MODE_PRIVATE
                                    )
                                            .edit()
                                            .putInt(
                                                    KEY_BG_COLOR,
                                                    color
                                            )
                                            .apply();

                                    Toast.makeText(
                                            MainActivity.this,
                                            mEnglish
                                                    ? "Color applied"
                                                    : "颜色已应用",
                                            Toast.LENGTH_SHORT
                                    ).show();

                                    dialog.dismiss();

                                } catch (Throwable t) {

                                    Toast.makeText(
                                            MainActivity.this,
                                            mEnglish
                                                    ? "Invalid color"
                                                    : "颜色格式错误",
                                            Toast.LENGTH_SHORT
                                    ).show();
                                }
                            }
                    );
                }
        );

        dialog.show();
    }

    @Override
    protected void onResume() {

        super.onResume();

        startDeepSleepUpdates();
    }

    @Override
    protected void onPause() {

        super.onPause();

        stopDeepSleepUpdates();
    }

    private void startDeepSleepUpdates() {

        stopDeepSleepUpdates();

        mDeepSleepUpdater =
                new Runnable() {

                    @Override
                    public void run() {

                        if (mDeepSleepText != null) {

                            mDeepSleepText.setText(
                                    formatDeepSleepLine()
                            );
                        }

                        mUiHandler.postDelayed(
                                this,
                                1000
                        );
                    }
                };

        mUiHandler.post(
                mDeepSleepUpdater
        );
    }

    private void stopDeepSleepUpdates() {

        if (mDeepSleepUpdater != null) {

            mUiHandler.removeCallbacks(
                    mDeepSleepUpdater
            );

            mDeepSleepUpdater = null;
        }
    }

    private String formatDeepSleepLine() {

        long elapsed =
                SystemClock.elapsedRealtime();

        long awake =
                SystemClock.uptimeMillis();

        long deepSleepMs =
                elapsed - awake;

        int percent =
                elapsed > 0
                        ? (int) Math.round(
                                deepSleepMs *
                                        100.0 /
                                        elapsed
                        )
                        : 0;

        long totalSeconds =
                deepSleepMs / 1000;

        long hours =
                totalSeconds / 3600;

        long minutes =
                (totalSeconds % 3600) / 60;

        long seconds =
                totalSeconds % 60;

        return String.format(
                Locale.getDefault(),
                "%d:%02d:%02d (%d%%)",
                hours,
                minutes,
                seconds,
                percent
        );
    }

    private void setBilingualText(
            TextView view,
            String zhText,
            String enText,
            int widthPx) {

        view.setText(
                mEnglish
                        ? enText
                        : zhText
        );
    }

    private int readCurrentModeOrDefault() {

        try {

            File f =
                    new File(CONFIG_FILE);

            if (!f.exists()) {
                return MODE_TIME_RAM;
            }

            BufferedReader br =
                    new BufferedReader(
                            new FileReader(f)
                    );

            String line =
                    br.readLine();

            br.close();

            if (line == null) {
                return MODE_TIME_RAM;
            }

            return Integer.parseInt(
                    line.trim()
            );

        } catch (Throwable t) {

            return MODE_TIME_RAM;
        }
    }

    private boolean writeModeToFile(
            int mode) {

        try {

            Process su =
                    Runtime.getRuntime()
                            .exec("su");

            DataOutputStream os =
                    new DataOutputStream(
                            su.getOutputStream()
                    );

            os.writeBytes(
                    "echo " +
                    mode +
                    " > " +
                    CONFIG_FILE +
                    "\n"
            );

            os.writeBytes(
                    "chmod 666 " +
                    CONFIG_FILE +
                    "\n"
            );

            os.writeBytes(
                    "exit\n"
            );

            os.flush();

            int result =
                    su.waitFor();

            return result == 0;

        } catch (Throwable t) {

            return false;
        }
    }
}
