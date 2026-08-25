package com.example.ramstatusbar;

import android.view.View;
import android.view.ViewParent;
import android.widget.TextView;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class MainHook implements IXposedHookLoadPackage {

    private static final String TAG = "RamStatusBar";

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) {
        if (!"com.android.systemui".equals(lpparam.packageName)) {
            return;
        }

        try {
            XposedBridge.hookAllConstructors(TextView.class, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    try {
                        TextView tv = (TextView) param.thisObject;
                        String className = tv.getClass().getName();
                        String idName = safeIdName(tv);

                        boolean looksLikeClock =
                                className.toLowerCase().contains("clock")
                                        || idName.toLowerCase().contains("clock");

                        if (looksLikeClock) {
                            XposedBridge.log(TAG + ": [构造时刻] 发现候选 -> "
                                    + className
                                    + " | id=" + idName);

                            final TextView finalTv = tv;
                            tv.post(new Runnable() {
                                @Override
                                public void run() {
                                    try {
                                        XposedBridge.log(TAG + ": [延迟检查] "
                                                + finalTv.getClass().getName()
                                                + " | 当前文字=\"" + finalTv.getText() + "\""
                                                + " | 父级链=" + parentChain(finalTv, 6));
                                    } catch (Throwable ignored) {
                                    }
                                }
                            });
                        }
                    } catch (Throwable t) {
                    }
                }
            });

            XposedBridge.log(TAG + ": 诊断v3 构造函数hook 安装成功");
        } catch (Throwable t) {
            XposedBridge.log(TAG + ": 诊断v3 hook 安装失败: " + t);
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

    private String parentChain(View view, int depth) {
        StringBuilder sb = new StringBuilder();
        ViewParent p = view.getParent();
        int i = 0;
        while (p instanceof View && i < depth) {
            View pv = (View) p;
            sb.append(pv.getClass().getSimpleName());
            String idName = "";
            try {
                int id = pv.getId();
                if (id != -1) {
                    idName = "(" + pv.getResources().getResourceEntryName(id) + ")";
                }
            } catch (Throwable ignored) {
            }
            sb.append(idName).append(" < ");
            p = pv.getParent();
            i++;
        }
        return sb.toString();
    }
}
