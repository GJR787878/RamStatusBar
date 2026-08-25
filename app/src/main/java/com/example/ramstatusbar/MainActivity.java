package com.example.ramstatusbar;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;

public class MainActivity extends Activity {

    private static final String PREFS_NAME = "config";
    private static final String KEY_SHOW_TIME = "show_time";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(48, 96, 48, 48);

        TextView title = new TextView(this);
        title.setTextSize(17);
        title.setText("RAM 状态栏显示\n\n"
                + "切换下面的开关，最多等 5 秒(下次自动刷新)即可生效，"
                + "不需要重启手机。\n");
        root.addView(title);

        final Switch switchView = new Switch(this);
        switchView.setText("显示时间 + 内存 (关闭则只显示内存)");
        switchView.setTextSize(15);
        switchView.setChecked(prefs.getBoolean(KEY_SHOW_TIME, true));
        switchView.setPadding(0, 64, 0, 0);
        switchView.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                prefs.edit().putBoolean(KEY_SHOW_TIME, isChecked).apply();
            }
        });
        root.addView(switchView);

        TextView note = new TextView(this);
        note.setTextSize(13);
        note.setPadding(0, 80, 0, 0);
        note.setText("提示：请到 LSPosed / Vector Manager 里，对本模块勾选作用域 "
                + "com.android.systemui 并启用模块，首次安装完成后需要重启一次手机才会生效。\n\n"
                + "开：时间 + 可用/总内存(如 13:31 2.5G/8G)\n"
                + "关：只显示 可用/总内存(如 2.5G/8G)\n"
                + "总内存会自动检测并取整到最接近的常见规格(8/12/16/24G等)。");
        root.addView(note);

        setContentView(root);
    }
}
