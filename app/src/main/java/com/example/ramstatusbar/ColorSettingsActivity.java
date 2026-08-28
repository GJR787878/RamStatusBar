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
                LinearLayout.VERTICAL
        );

        root.setPadding(
                Math.round(32 * density),
                Math.round(48 * density),
                Math.round(32 * density),
                Math.round(48 * density)
        );

        TextView title =
                new TextView(this);

        title.setTextSize(20);

        title.setText(
                mEnglish
                        ? "Background color"
                        : "背景颜色"
        );

        root.addView(
                title,
                createParams(
                        0,
                        0,
                        0,
                        24
                )
        );

        TextView description =
                new TextView(this);

        description.setTextSize(14);

        description.setText(
                mEnglish
                        ? "Choose a preset color for the capsule "
                        + "behind the status bar text."
                        : "选择状态栏文字后面的胶囊背景颜色。"
        );

        root.addView(
                description,
                createParams(
                        0,
                        0,
                        0,
                        32
                )
        );

        TextView previewTitle =
                new TextView(this);

        previewTitle.setTextSize(15);

        previewTitle.setText(
                mEnglish
                        ? "Preview"
                        : "预览"
        );

        root.addView(
                previewTitle,
                createParams(
                        0,
                        0,
                        0,
                        12
                )
        );

        mPreview = new View(this);

        updatePreview();

        LinearLayout.LayoutParams previewParams =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        Math.round(56 * density)
                );

        previewParams.bottomMargin =
                Math.round(12 * density);

        root.addView(
                mPreview,
                previewParams
        );

        mColorValue =
                new TextView(this);

        mColorValue.setTextSize(13);

        mColorValue.setGravity(
                Gravity.CENTER
        );

        updateColorText();

        root.addView(
                mColorValue,
                createParams(
                        0,
                        0,
                        0,
                        32
                )
        );

        LinearLayout colorGrid =
                new LinearLayout(this);

        colorGrid.setOrientation(
                LinearLayout.VERTICAL
        );

        for (int row = 0; row < 3; row++) {

            LinearLayout rowLayout =
                    new LinearLayout(this);

            rowLayout.setOrientation(
                    LinearLayout.HORIZONTAL
            );

            rowLayout.setGravity(
                    Gravity.CENTER
            );

            for (int col = 0; col < 3; col++) {

                final int index =
                        row * 3 + col;

                Button colorButton =
                        createColorButton(
                                index,
                                density
                        );

                rowLayout.addView(
                        colorButton
                );
            }

            LinearLayout.LayoutParams rowParams =
                    new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            Math.round(58 * density)
                    );

            if (row > 0) {
                rowParams.topMargin =
                        Math.round(8 * density);
            }

            colorGrid.addView(
                    rowLayout,
                    rowParams
            );
        }

        root.addView(colorGrid);

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
                        finish();
                    }
                }
        );

        LinearLayout.LayoutParams backParams =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );

        backParams.topMargin =
                Math.round(40 * density);

        root.addView(
                backButton,
                backParams
        );

        scrollView.addView(
                root,
                new ScrollView.LayoutParams(
                        ScrollView.LayoutParams.MATCH_PARENT,
                        ScrollView.LayoutParams.WRAP_CONTENT
                )
        );

        setContentView(scrollView);
    }

    private Button createColorButton(
            final int index,
            float density) {

        Button button =
                new Button(this);

        button.setText(
                mEnglish
                        ? COLOR_NAMES_EN[index]
                        : COLOR_NAMES_ZH[index]
        );

        button.setTextSize(11);

        button.setAllCaps(false);

        GradientDrawable background =
                new GradientDrawable();

        background.setColor(
                COLORS[index]
        );

        background.setCornerRadius(
                1000.0f
        );

        background.setStroke(
                Math.round(1 * density),
                0x55000000
        );

        button.setBackground(background);

        LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(
                        0,
                        Math.round(52 * density),
                        1.0f
                );

        if (index % 3 != 0) {
            params.leftMargin =
                    Math.round(4 * density);
        }

        if (index % 3 != 2) {
            params.rightMargin =
                    Math.round(4 * density);
        }

        button.setLayoutParams(params);

        button.setOnClickListener(
                new View.OnClickListener() {

                    @Override
                    public void onClick(View v) {

                        mSelectedColor =
                                COLORS[index];

                        saveColor();

                        updatePreview();

                        updateColorText();

                        Toast.makeText(
                                ColorSettingsActivity.this,
                                mEnglish
                                        ? "Color applied"
                                        : "颜色已应用",
                                Toast.LENGTH_SHORT
                        ).show();
                    }
                }
        );

        return button;
    }

    private void saveColor() {

        mPrefs.edit()
                .putInt(
                        KEY_BG_COLOR,
                        mSelectedColor
                )
                .apply();
    }

    private void updatePreview() {

        if (mPreview == null) {
            return;
        }

        GradientDrawable drawable =
                new GradientDrawable();

        drawable.setColor(
                mSelectedColor
        );

        drawable.setCornerRadius(
                1000.0f
        );

        mPreview.setBackground(
                drawable
        );
    }

    private void updateColorText() {

        if (mColorValue == null) {
            return;
        }

        String hex =
                String.format(
                        "#%08X",
                        mSelectedColor
                );

        mColorValue.setText(
                mEnglish
                        ? "Current color: " + hex
                        : "当前颜色：" + hex
        );
    }

    private LinearLayout.LayoutParams createParams(
            int width,
            int height,
            int left,
            int bottom) {

        LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(
                        width == 0
                                ? LinearLayout.LayoutParams.MATCH_PARENT
                                : width,
                        height == 0
                                ? LinearLayout.LayoutParams.WRAP_CONTENT
                                : height
                );

        float density =
                getResources()
                        .getDisplayMetrics()
                        .density;

        params.leftMargin =
                Math.round(left * density);

        params.bottomMargin =
                Math.round(bottom * density);

        return params;
    }
}
