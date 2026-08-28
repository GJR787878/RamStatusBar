package com.example.ramstatusbar;

import android.app.Activity;
import android.content.SharedPreferences;
import android.graphics.Color;
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

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileReader;
import java.util.Locale;

public class MainActivity extends Activity {

    private static final String CONFIG_FILE =
            "/data/local/tmp/ramstatusbar_mode";

    private static final String COLOR_FILE =
            "/data/local/tmp/ramstatusbar_color";

    private static final int MODE_TIME_ONLY = 0;
    private static final int MODE_TIME_RAM = 1;
    private static final int MODE_RAM_ONLY = 2;

    private static final String UI_PREFS_NAME =
            "ui_prefs";

    private static final String KEY_LANGUAGE =
            "language";

    private static final String LANG_ZH =
            "zh";

    private static final String LANG_EN =
            "en";

    private static final int DEFAULT_BACKGROUND_COLOR =
            0x66000000;

    private static final int LANG_BUTTON_WIDTH_DP =
            72;

    private boolean mEnglish;

    private TextView mDeepSleepText;

    private final Handler mUiHandler =
            new Handler(Looper.getMainLooper());

    private Runnable mDeepSleepUpdater;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
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

    /*
     * ============================
     * 主页面
     * ============================
     */
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
                180
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
                "Pick a display mode below. Changes take "
                        + "effect within 1 second, no reboot needed.";

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

        radioGroup.addView(
                rbTimeOnly
        );

        radioGroup.addView(
                rbTimeRam
        );

        radioGroup.addView(
                rbRamOnly
        );

        content.addView(
                radioGroup
        );

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
                                writeModeToFile(
                                        mode
                                );

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

        dividerParams.topMargin =
                64;

        dividerParams.bottomMargin =
                24;

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
                "点击状态栏的时钟：第 1 次点击显示 CPU "
                        + "占用率，第 2 次点击显示 GPU 占用率，"
                        + "第 3 次点击回到正常显示；10 秒内不再点击"
                        + "也会自动回到正常显示。\n\n"
                        + "GPU 占用率依赖具体芯片的私有接口，部分设备上"
                        + "可能会显示 \"GPU N/A\"。";

        String tapEn =
                "Tap the status bar clock: 1st tap shows CPU "
                        + "usage, 2nd tap shows GPU usage, 3rd tap "
                        + "returns to normal. If left untouched for "
                        + "10 seconds it automatically returns to normal.\n\n"
                        + "GPU usage relies on chip-specific sysfs "
                        + "paths and may show \"GPU N/A\" on some devices.";

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
                "下面显示的是开机以来设备处于深度休眠状态的"
                        + "时长和占比，跟\"关于本机\"里的数值一致。";

        String deepSleepEn =
                "Shown below is how much of the time since boot "
                        + "the device has spent in deep sleep.";

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

        mDeepSleepText.setTextSize(
                14
        );

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

        setupTitle.setTextSize(
                15
        );

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

        setupBody.setTextSize(
                13
        );

        setupBody.setText(
                mEnglish
                        ? "1. In LSPosed / Vector Manager, enable this "
                        + "module and check the scope com.android.systemui.\n"
                        + "2. Reboot once after the first install.\n"
                        + "3. Switching display mode and background "
                        + "color requires root permission.\n"
                        + "4. Total RAM is auto-detected."
                        : "1. 到 LSPosed / Vector Manager 里，对本模块"
                        + "勾选作用域 com.android.systemui 并启用模块。\n"
                        + "2. 首次安装完成后需要重启一次手机。\n"
                        + "3. 切换显示模式和背景颜色需要 root 权限。\n"
                        + "4. 总内存会自动检测。"
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
         * [ 背景颜色 ] [ 中英文 ]
         *
         * 两个按钮放在同一行。
         */
        LinearLayout bottomBar =
                new LinearLayout(this);

        bottomBar.setOrientation(
                LinearLayout.HORIZONTAL
        );

        bottomBar.setGravity(
                Gravity.CENTER
        );

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

        float density =
                getResources()
                        .getDisplayMetrics()
                        .density;

        int buttonWidth =
                Math.round(
                        LANG_BUTTON_WIDTH_DP *
                                density
                );

        LinearLayout.LayoutParams colorParams =
                new LinearLayout.LayoutParams(
                        0,
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        1f
                );

        colorParams.setMargins(
                24,
                0,
                12,
                24
        );

        LinearLayout.LayoutParams langParams =
                new LinearLayout.LayoutParams(
                        buttonWidth,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                );

        langParams.setMargins(
                12,
                0,
                24,
                24
        );

        bottomBar.addView(
                colorButton,
                colorParams
        );

        bottomBar.addView(
                langButton,
                langParams
        );

        FrameLayout.LayoutParams bottomParams =
                new FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                );

        bottomParams.gravity =
                Gravity.BOTTOM;

        frame.addView(
                bottomBar,
                bottomParams
        );

        setContentView(frame);
    }

    /*
     * ============================
     * 二级颜色选择页面
     * ============================
     */
    private void showColorPage() {

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

        ScrollView scrollView =
                new ScrollView(this);

        TextView title =
                new TextView(this);

        title.setTextSize(
                20
        );

        title.setText(
                mEnglish
                        ? "Background Color"
                        : "背景颜色"
        );

        content.addView(
                title
        );

        TextView description =
                new TextView(this);

        description.setTextSize(
                14
        );

        description.setPadding(
                0,
                24,
                0,
                32
        );

        description.setText(
                mEnglish
                        ? "Choose a preset color for the status bar capsule."
                        : "选择状态栏胶囊背景颜色。"
        );

        content.addView(
                description
        );

        /*
         * 预设颜色。
         */
        addColorButton(
                content,
                mEnglish ? "Transparent" : "透明",
                Color.TRANSPARENT
        );

        addColorButton(
                content,
                mEnglish ? "Black" : "黑色",
                0x99000000
        );

        addColorButton(
                content,
                mEnglish ? "Dark Gray" : "深灰色",
                0x99606060
        );

        addColorButton(
                content,
                mEnglish ? "White" : "白色",
                0x99FFFFFF
        );

        addColorButton(
                content,
                mEnglish ? "Red" : "红色",
                0x99F44336
        );

        addColorButton(
                content,
                mEnglish ? "Orange" : "橙色",
                0x99FF9800
        );

        addColorButton(
                content,
                mEnglish ? "Yellow" : "黄色",
                0x99FFEB3B
        );

        addColorButton(
                content,
                mEnglish ? "Green" : "绿色",
                0x994CAF50
        );

        addColorButton(
                content,
                mEnglish ? "Light Green" : "浅绿色",
                0x997CB342
        );

        addColorButton(
                content,
                mEnglish ? "Blue" : "蓝色",
                0x992196F3
        );

        addColorButton(
                content,
                mEnglish ? "Purple" : "紫色",
                0x999C27B0
        );

        addColorButton(
                content,
                mEnglish ? "Pink" : "粉色",
                0x99E91E63
        );

        TextView note =
                new TextView(this);

        note.setTextSize(
                13
        );

        note.setPadding(
                0,
                32,
                0,
                32
        );

        note.setText(
                mEnglish
                        ? "The background uses a capsule shape. "
                        + "The text area automatically expands when "
                        + "CPU / GPU information is longer."
                        : "背景使用胶囊形状。CPU / GPU 信息较长时，"
                        + "胶囊会自动扩大，避免文字溢出。"
        );

        content.addView(
                note
        );

        Button backButton =
                new Button(this);

        backButton.setText(
                mEnglish
                        ? "Back"
                        : "返回"
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
                backButton
        );

        scrollView.addView(
                content
        );

        frame.addView(
                scrollView,
                new FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                )
        );

        setContentView(frame);
    }

    private void addColorButton(
            LinearLayout parent,
            String title,
            final int color) {

        Button button =
                new Button(this);

        button.setText(
                title
        );

        button.setOnClickListener(
                new View.OnClickListener() {

                    @Override
                    public void onClick(View v) {

                        boolean ok =
                                writeColorToFile(
                                        color
                                );

                        if (ok) {

                            Toast.makeText(
                                    MainActivity.this,
                                    mEnglish
                                            ? "Background color changed"
                                            : "背景颜色已修改",
                                    Toast.LENGTH_SHORT
                            ).show();

                        } else {

                            Toast.makeText(
                                    MainActivity.this,
                                    mEnglish
                                            ? "Write failed, please check root permission"
                                            : "写入失败，请检查 root 权限",
                                    Toast.LENGTH_LONG
                            ).show();
                        }
                    }
                }
        );

        LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                );

        params.setMargins(
                0,
                4,
                0,
                4
        );

        parent.addView(
                button,
                params
        );
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (mDeepSleepText != null) {
            startDeepSleepUpdates();
        }
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
                    new File(
                            CONFIG_FILE
                    );

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

    private boolean writeColorToFile(
            int color) {

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
                            color +
                            " > " +
                            COLOR_FILE +
                            "\n"
            );

            os.writeBytes(
                    "chmod 666 " +
                            COLOR_FILE +
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
