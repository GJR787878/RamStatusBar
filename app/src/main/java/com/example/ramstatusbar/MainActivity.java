package com.example.ramstatusbar;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import java.io.DataOutputStream;

public class MainActivity extends Activity {

    private static final String PREFS_NAME = "config";
    private static final String KEY_DISPLAY_MODE = "display_mode";

    private static final int MODE_TIME_ONLY = 0;
    private static final int MODE_TIME_RAM = 1;
    private static final int MODE_RAM_ONLY = 2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        if (!prefs.contains(KEY_DISPLAY_MODE)) {
            prefs.edit().putInt(KEY_DISPLAY_MODE, MODE_TIME_RAM).commit();
        }
        makeConfigWorldReadable();

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(48, 96, 48, 48);

        TextView title = new TextView(this);
        title.setTextSize(17);
        title.setText("RAM 状态栏显示\n\n"
                + "选择下面的显示模式，最多等 5 秒(下次自动刷新)即可生效，"
                + "不需要重启手机。三种模式占用的字符宽度是一致的，"
                + "切换时状态栏不会跳动。\n");
        root.addView(title);

        final RadioGroup radioGroup = new RadioGroup(this);
        radioGroup.setOrientation(RadioGroup.VERTICAL);

        final RadioButton rbTimeOnly = new RadioButton(this);
        rbTimeOnly.setId(1001);
        rbTimeOnly.setText("仅显示时间");

        final RadioButton rbTimeRam = new RadioButton(this);
        rbTimeRam.setId(1002);
        rbTimeRam.setText("时间 + 内存 (如 21:11 2.5G/8G)");

        final RadioButton rbRamOnly = new RadioButton(this);
        rbRamOnly.setId(1003);
        rbRamOnly.setText("仅显示内存 (如 2.5G/8G)");

        radioGroup.addView(rbTimeOnly);
        radioGroup.addView(rbTimeRam);
        radioGroup.addView(rbRamOnly);
        root.addView(radioGroup);

        int currentMode = prefs.getInt(KEY_DISPLAY_MODE, MODE_TIME_RAM);
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
                prefs.edit().putInt(KEY_DISPLAY_MODE, mode).commit();
                makeConfigWorldReadable();
            }
        });

        TextView note = new TextView(this);
        note.setTextSize(13);
        note.setPadding(0, 80, 0, 0);
        note.setText("提示：请到 LSPosed / Vector Manager 里，对本模块勾选作用域 "
                + "com.android.systemui 并启用模块，首次安装完成后需要重启一次手机才会生效。\n\n"
                + "总内存会自动检测并取整到最接近的常见规格(8/12/16/24G等)。\n\n"
                + "本功能需要 root 权限才能让切换即时同步(设备已检测到 Magisk，"
                + "正常情况下应该可以正常工作)。");
        root.addView(note);

        setContentView(root);
    }

    private void makeConfigWorldReadable() {
        try {
            String pkgDir = "/data/data/" + getPackageName();
            String prefsFile = pkgDir + "/shared_prefs/" + PREFS_NAME + ".xml";

            Process su = Runtime.getRuntime().exec("su");
            DataOutputStream os = new DataOutputStream(su.getOutputStream());
            os.writeBytes("chmod 711 " + pkgDir + "\n");
            os.writeBytes("chmod 711 " + pkgDir + "/shared_prefs\n");
            os.writeBytes("chmod 644 " + prefsFile + "\n");
            os.writeBytes("exit\n");
            os.flush();
            su.waitFor();
        } catch (Throwable t) {
        }
    }
}
