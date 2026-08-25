package com.example.ramstatusbar;

import android.app.Activity;
import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import java.io.DataOutputStream;

public class MainActivity extends Activity {

    private static final String CONFIG_FILE = "/data/local/tmp/ramstatusbar_mode";

    private static final int MODE_TIME_ONLY = 0;
    private static final int MODE_TIME_RAM = 1;
    private static final int MODE_RAM_ONLY = 2;

    private int mCurrentMode = MODE_TIME_RAM;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mCurrentMode = readCurrentModeOrDefault();

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(48, 96, 48, 48);

        TextView title = new TextView(this);
        title.setTextSize(17);
        title.setText("RAM 状态栏显示\n\n"
                + "选择下面的显示模式，最多等 1 秒即可生效，不需要重启手机。"
                + "首次切换会弹出 root 授权请求，请点击允许。\n");
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

        if (mCurrentMode == MODE_TIME_ONLY) {
            radioGroup.check(rbTimeOnly.getId());
        } else if (mCurrentMode == MODE_RAM_ONLY) {
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
                            "写入失败，请检查是否已授予 root 权限",
                            Toast.LENGTH_LONG).show();
                }
            }
        });

        TextView note = new TextView(this);
        note.setTextSize(13);
        note.setPadding(0, 80, 0, 0);
        note.setText("提示：请到 LSPosed / Vector Manager 里，对本模块勾选作用域 "
                + "com.android.systemui 并启用模块，首次安装完成后需要重启一次手机才会生效。\n\n"
                + "总内存会自动检测并取整到最接近的常见规格(8/12/16/24G等)。");
        root.addView(note);

        setContentView(root);
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
