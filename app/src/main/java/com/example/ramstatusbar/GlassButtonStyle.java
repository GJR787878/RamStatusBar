package com.example.ramstatusbar;

import android.graphics.drawable.GradientDrawable;

/**
 * GlassButtons 玻璃拟态样式：常量与背景工厂。
 *
 * 提取自 RamStatusBar 项目（MainActivity / ColorSettingsActivity / TimeSettingsActivity），
 * 三个界面使用的按钮风格完全一致：
 *   - 底色 0xB31C1C1E（约 70% 不透明度的深灰，玻璃拟态质感）
 *   - 圆角 28dp（胶囊形）
 *   - 默认：1dp 淡白描边（0x40FFFFFF），白色文字
 *   - 选中：2dp 纯白描边（0xFFFFFFFF），蓝色文字（0xFF0A84FF）
 */
public final class GlassButtonStyle {

    /** 选中态强调色（iOS 蓝） */
    public static final int COLOR_ACCENT = 0xFF0A84FF;

    /** 普通文字白色 */
    public static final int COLOR_WHITE = 0xFFFFFFFF;

    /** 玻璃底：半透明深灰（约 70% 不透明度） */
    public static final int COLOR_NAV_BG = 0xB31C1C1E;

    /** 默认细描边：25% 白 */
    public static final int COLOR_NAV_BORDER = 0x40FFFFFF;

    /** 选中态淡白底：18% 白（用于导航项等轻量高亮） */
    public static final int COLOR_TAB_SELECTED_BG = 0x2EFFFFFF;

    private GlassButtonStyle() {
    }

    /**
     * 创建玻璃胶囊按钮背景。
     *
     * @param density        屏幕密度（{@code context.getResources().getDisplayMetrics().density}）
     * @param cornerRadiusDp 圆角半径，单位 dp（原项目为 28dp）
     * @param selected       是否选中态：选中 = 2dp 纯白描边，否则 1dp 淡白描边
     */
    public static GradientDrawable createGlassBackground(float density, float cornerRadiusDp, boolean selected) {
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(COLOR_NAV_BG);
        bg.setCornerRadius(Math.round(cornerRadiusDp * density));
        if (selected) {
            bg.setStroke(Math.round(2 * density), COLOR_WHITE);
        } else {
            bg.setStroke(Math.round(1 * density), COLOR_NAV_BORDER);
        }
        return bg;
    }

    /**
     * 创建选中态淡白高亮背景（导航项等），全圆角。
     */
    public static GradientDrawable createSelectedHighlight() {
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(COLOR_TAB_SELECTED_BG);
        bg.setCornerRadius(1000f);
        return bg;
    }
}
