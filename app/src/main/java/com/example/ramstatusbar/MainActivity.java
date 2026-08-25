package com.example.ramstatusbar;

import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;

public class MainActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        TextView tv = new TextView(this);
        tv.setPadding(48, 96, 48, 48);
        tv.setTextSize(16);
        tv.setText("RAM 状态栏显示\n\n"
                + "安装完成后，请到 LSPosed / Vector Manager 里，\n"
                + "对本模块勾选作用域 com.android.systemui，\n"
                + "然后重启手机即可在状态栏时钟旁看到可用内存。");
        setContentView(tv);
    }
}
