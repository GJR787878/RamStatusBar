package com.example.ramstatusbar;

import android.app.Activity;
import android.content.SharedPreferences;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

public class ColorSettingsActivity extends Activity {

    private static final String UI_PREFS_NAME = "ui_prefs";
    private static final String KEY_BG_COLOR = "background_color";

    private static final int DEFAULT_BG_COLOR = 0xFFB8E6C1;

    private boolean mEnglish;
    private SharedPreferences mPrefs;

    private View mPreview;
    private TextView mColorValue;

    private int mSelectedColor;

    private static final int[] COLORS = {
            0xFFB8E6C1,
            0xFFC8E6C9,
            0xFFB2DFDB,
            0xFFBBDEFB,
            0xFFD1C4E9,
            0xFFFFCCBC,
            0xFFFFF59D,
            0xFFF5F5F5,
            0xFFCFD8DC
    };

    private static final String[] COLOR_NAMES_ZH = {
            "浅绿色",
            "淡绿色",
            "浅青色",
            "浅蓝色",
            "浅紫色",
            "浅橙色",
            "浅黄色",
            "浅灰色",
            "蓝灰色"
    };

    private static final String[] COLOR_NAMES_EN = {
            "Light green",
            "Pale green",
            "Light cyan",
            "Light blue",
            "Light purple",
            "Light orange",
            "Light yellow",
            "Light gray",
            "Blue gray"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mPrefs = getSharedPreferences(
                UI_PREFS_NAME,
                MODE_PRIVATE
        );

        String language = mPrefs.getString(
                "language",
                "zh"
        );

        mEnglish = "en".equals(language);

        mSelectedColor = mPrefs.getInt(
                KEY_BG_COLOR,
                DEFAULT_BG_COLOR
        );

        buildInterface();
    }

    private void buildInterface() {

        float density =
                getResources()
                        .getDisplayMetrics()
                        .density;

        ScrollView scrollView =
                new ScrollView(this);

        LinearLayout root =
                new LinearLayout(this);

        root.setOrientation(
               
