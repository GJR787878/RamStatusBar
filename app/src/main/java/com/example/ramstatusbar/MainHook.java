package com.example.ramstatusbar;

import android.widget.TextView;

import java.util.regex.Pattern;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class MainHook implements IXposedHookLoadPackage {

    private static final String TAG = "RamStatusBar";
    private static final Pattern TIME_PATTERN = Pattern.compile("^\\d{1,2}:\\d{2}(:\\d{2})?\\s*(AM|PM|上午|下午)?$");

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) {
        if (!"com.android.systemui".equals(lpparam.packageName)) {
            return;
        }

        try {
            XposedHelpers.findAndHookMethod(
                    TextView.class,
                    "onAttachedToWindow",
                    new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) {
                            try {
                                TextView tv = (TextView) param.thisObject;
                                CharSequence text = tv.getText();
                                if (text != null && TIME_PATTERN.matcher(text.toString().trim()).matches()) {
                                    XposedBridge.log(TAG + ": 疑似时钟类 -> "
                                            + tv.getClass().getName()
                                            + " | 当前文字=\"" + text + "\""
                                            + " | id=" + safeIdName(tv));
                                }
                            } catch (Throwable t) {
                            }
                        }
                    });

            XposedBridge.log(TAG + ": 诊断 hook 安装成功，等待发现时钟类...");
        } catch (Throwable t) {
            XposedBridge.log(TAG + ": 诊断 hook 安装失败: " + t);
        }
    }

    private String safeIdName(TextView tv) {
        try {
            int id = tv.getId();
            if (id == -1) return "no-id";
            return tv.getResources().getResourceName(id);
        } catch (Throwable t) {
            return "unknown";
        }
    }
}
