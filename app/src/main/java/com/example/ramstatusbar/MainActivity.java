package com.example.ramstatusbar;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import java.io.DataOutputStream;
import java.util.Locale;

public class MainActivity extends Activity {

    private static final String CONFIG_FILE = "/data/local/tmp/ramstatusbar_mode";

    private static final int MODE_TIME_ONLY = 0;
    private static final int MODE_TIME_RAM = 1;
    private static final int MODE_RAM_ONLY = 2;

    private static final String UI_PREFS_NAME = "ui_prefs";
    private static final String KEY_LANGUAGE = "language";
    private static final String LANG_ZH = "zh";
    private static final String LANG_EN = "en";

    private boolean mEnglish;
    private TextView mDeepSleepText;
    private final Handler mUiHandler = new Handler(Looper.getMainLooper());
    private Runnable mDeepSleepUpdater;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SharedPreferences uiPrefs = getSharedPreferences(UI_PREFS_NAME, MODE_PRIVATE);
        mEnglish = LANG_EN.equals(uiPrefs.getString(KEY_LANGUAGE, LANG_ZH));

        int currentMode = readCurrentModeOrDefault();

        FrameLayout frame = new FrameLayout(this);

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(48, 96, 48, 48);

        TextView title = new TextView(this);
        title.setTextSize(17);
        title.setText(mEnglish
                ? "RAM Status Bar Display\n\n"
                        + "Pick a display mode below, it takes effect within 1 second, "
                        + "no reboot needed. A root permission prompt will appear the "
                        + "first time you switch \u2014 please allow it.\n"
                : "RAM 状态栏显示\n\n"
                        + "选择下面的显示模式，最多等 1 秒即可生效，不需要重启手机。"
                        + "首次切换会弹出 root 授权请求，请点击允许。\n");
        content.addView(title);

        final RadioGroup radioGroup = new RadioGroup(this);
        radioGroup.setOrientation(RadioGroup.VERTICAL);

        final RadioButton rbTimeOnly = new RadioButton(this);
        rbTimeOnly.setId(1001);
        rbTimeOnly.setText(mEnglish ? "Time only" : "仅显示时间");

        final RadioButton rbTimeRam = new RadioButton(this);
        rbTimeRam.setId(1002);
        rbTimeRam.setText(mEnglish
                ? "Time + RAM (e.g. 21:11 2.5G/8G)"
                : "时间 + 内存 (如 21:11 2.5G/8G)");

        final RadioButton rbRamOnly = new RadioButton(this);
        rbRamOnly.setId(1003);
        rbRamOnly.setText(mEnglish
                ? "RAM only (e.g. 2.5G/8G)"
                : "仅显示内存 (如 2.5G/8G)");

        radioGroup.addView(rbTimeOnly);
        radioGroup.addView(rbTimeRam);
        radioGroup.addView(rbRamOnly);
        content.addView(radioGroup);

        if (currentMode == MODE_TIME_ONLY) {
            radioGroup.check(rbTimeOnly.getId());
        } else if (currentMode == MODE_RAM_ONLY) {
            radioGroup.check(rbRamOnly.getId());
        } else {
            radioGroup.check(rbTimeRam.getId());
        }

        radioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
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
                            mEnglish
                                    ? "Write failed, please check root permission"
                                    : "写入失败，请检查是否已授予 root 权限",
                            Toast.LENGTH_LONG).show();
                }
            }
        });

        TextView note = new TextView(this);
        note.setTextSize(13);
        note.setPadding(0, 80, 0, 0);
        note.setText(mEnglish
                ? "Tip: In LSPosed / Vector Manager, enable this module and check "
                        + "the scope com.android.systemui. A reboot is required once "
                        + "right after the first install.\n\n"
                        + "Total RAM is auto-detected and rounded up to the nearest "
                        + "common spec (8/12/16/24G, etc)."
                : "提示：请到 LSPosed / Vector Manager 里，对本模块勾选作用域 "
                        + "com.android.systemui 并启用模块，首次安装完成后需要重启一次手机才会生效。\n\n"
                        + "总内存会自动检测并取整到最接近的常见规格(8/12/16/24G等)。");
        content.addView(note);

        mDeepSleepText = new TextView(this);
        mDeepSleepText.setTextSize(13);
        mDeepSleepText.setPadding(0, 48, 0, 0);
        content.addView(mDeepSleepText);

        frame.addView(content, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        Button langButton = new Button(this);
        langButton.setText(mEnglish ? "\u4e2d\u6587" : "EN");
        langButton.setOnClickListener(v -> {
            SharedPreferences prefs = getSharedPreferences(UI_PREFS_NAME, MODE_PRIVATE);
            String newLang = mEnglish ? LANG_ZH : LANG_EN;
            prefs.edit().putString(KEY_LANGUAGE, newLang).apply();
            recreate();
        });
        FrameLayout.LayoutParams langParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        langParams.gravity = Gravity.BOTTOM | Gravity.END;
        langParams.setMargins(0, 0, 48, 48);
        frame.addView(langButton, langParams);

        setContentView(frame);
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
        mDeepSleepUpdater = new Runnable() {
            @Override
            public void run() {
                mDeepSleepText.setText(formatDeepSleepLine());
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
        int percent = elapsed > 0 ? (int) Math.round(deepSleepMs * 100.0 / elapsed) : 0;

        long totalSeconds = deepSleepMs / 1000;
        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;

        String time = String.format(Locale.getDefault(), "%d:%02d:%02d", hours, minutes, seconds);
        String label = mEnglish ? "Deep sleep" : "深度休眠";
        return label + "  " + time + " (" + percent + "%)";
    }

    private int readCurrentModeOrDefault() {
        try {
            java.io.File f = new java.io.File(CONFIG_FILE);
            if (!f.exists()) {
                return MODE_TIME_RAM;
            }
            java.io.BufferedReader br = new java.io.BufferedReader(new java.io.FileReader(f));
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
