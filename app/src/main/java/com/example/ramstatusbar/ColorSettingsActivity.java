package com.example.ramstatusbar;

import android.app.Activity;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class ColorSettingsActivity extends Activity {

    private static final String PREFS_NAME = "ui_prefs";

    private static final String KEY_BG_COLOR =
            "background_color";

    private static final String KEY_LANGUAGE =
            "language";

    private static final String LANG_EN =
            "en";

    private boolean mEnglish;

    private int mSelectedColor;

    private TextView mPreview;

    private static final int[] COLORS = {
            0xFF000000,
            0xFFFFFFFF,
            0xFF202124,
            0xFF3F51B5,
            0xFF2196F3,
            0xFF03A9F4,
            0xFF00BCD4,
            0xFF009688,
            0xFF4CAF50,
            0xFF8BC34A,
            0xFFCDDC39,
            0xFFFFEB3B,
            0xFFFFC107,
            0xFFFF9800,
            0xFFFF5722,
            0xFFF44336,
            0xFFE91E63,
            0xFF9C27B0,
            0xFF673AB7
    };

    @Override
    protected void onCreate(
            Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        android.content.SharedPreferences prefs =
                getSharedPreferences(
                        PREFS_NAME,
                        MODE_PRIVATE
                );

        mEnglish =
                LANG_EN.equals(
                        prefs.getString(
                                KEY_LANGUAGE,
                                "zh"
                        )
                );

        mSelectedColor =
                prefs.getInt(
                        KEY_BG_COLOR,
                        0xCC000000
                );

        float density =
                getResources()
                        .getDisplayMetrics()
                        .density;

        LinearLayout root =
                new LinearLayout(this);

        root.setOrientation(
                LinearLayout.VERTICAL
        );

        root.setPadding(
                Math.round(24 * density),
                Math.round(48 * density),
                Math.round(24 * density),
                Math.round(32 * density)
        );

        TextView title =
                new TextView(this);

        title.setTextSize(20);

        title.setText(
                mEnglish
                        ? "Background Color"
                        : "背景颜色"
        );

        root.addView(
                title,
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                )
        );

        TextView description =
                new TextView(this);

        description.setTextSize(14);

        description.setPadding(
                0,
                Math.round(16 * density),
                0,
                Math.round(24 * density)
        );

        description.setText(
                mEnglish
                        ? "Choose a background color for the "
                        + "status bar capsule."
                        : "选择状态栏胶囊背景颜色。"
        );

        root.addView(
                description
        );

        TextView previewTitle =
                new TextView(this);

        previewTitle.setTextSize(14);

        previewTitle.setText(
                mEnglish
                        ? "Preview"
                        : "预览"
        );

        root.addView(
                previewTitle
        );

        mPreview =
                new TextView(this);

        mPreview.setTextSize(16);

        mPreview.setTextColor(
                getPreviewTextColor()
        );

        mPreview.setGravity(
                Gravity.CENTER
        );

        mPreview.setText(
                mEnglish
                        ? "21:11 2.5G/8G"
                        : "21:11 2.5G/8G"
        );

        LinearLayout.LayoutParams previewParams =
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        Math.round(56 * density)
                );

        previewParams.topMargin =
                Math.round(12 * density);

        previewParams.bottomMargin =
                Math.round(32 * density);

        root.addView(
                mPreview,
                previewParams
        );

        updatePreview();

        TextView colorTitle =
                new TextView(this);

        colorTitle.setTextSize(14);

        colorTitle.setText(
                mEnglish
                        ? "Colors"
                        : "颜色"
        );

        root.addView(
                colorTitle
        );

        LinearLayout colorGrid =
                new LinearLayout(this);

        colorGrid.setOrientation(
                LinearLayout.VERTICAL
        );

        int columnCount = 4;

        LinearLayout currentRow = null;

        for (int i = 0; i < COLORS.length; i++) {

            if (i % columnCount == 0) {

                currentRow =
                        new LinearLayout(this);

                currentRow.setOrientation(
                        LinearLayout.HORIZONTAL
                );

                colorGrid.addView(
                        currentRow,
                        new LinearLayout.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                Math.round(64 * density)
                        )
                );
            }

            final int color =
                    COLORS[i];

            Button colorButton =
                    new Button(this);

            colorButton.setText("");

            colorButton.setPadding(
                    0,
                    0,
                    0,
                    0
            );

            GradientDrawable colorDrawable =
                    new GradientDrawable();

            colorDrawable.setShape(
                    GradientDrawable.OVAL
            );

            colorDrawable.setColor(
                    color
            );

            colorDrawable.setStroke(
                    Math.round(1 * density),
                    0x55000000
            );

            colorButton.setBackground(
                    colorDrawable
            );

            colorButton.setOnClickListener(
                    new View.OnClickListener() {

                        @Override
                        public void onClick(
                                View v) {

                            mSelectedColor =
                                    color;

                            saveColor();

                            updatePreview();

                            Toast.makeText(
                                    ColorSettingsActivity.this,
                                    mEnglish
                                            ? "Color saved"
                                            : "颜色已保存",
                                    Toast.LENGTH_SHORT
                            ).show();
                        }
                    }
            );

            LinearLayout.LayoutParams buttonParams =
                    new LinearLayout.LayoutParams(
                            0,
                            Math.round(48 * density),
                            1
                    );

            buttonParams.setMargins(
                    Math.round(6 * density),
                    Math.round(6 * density),
                    Math.round(6 * density),
                    Math.round(6 * density)
            );

            currentRow.addView(
                    colorButton,
                    buttonParams
            );
        }

        root.addView(
                colorGrid,
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                )
        );

        Button transparentButton =
                new Button(this);

        transparentButton.setText(
                mEnglish
                        ? "Transparent"
                        : "透明"
        );

        transparentButton.setAllCaps(false);

        transparentButton.setOnClickListener(
                new View.OnClickListener() {

                    @Override
                    public void onClick(View v) {

                        mSelectedColor =
                                Color.TRANSPARENT;

                        saveColor();

                        updatePreview();

                        Toast.makeText(
                                ColorSettingsActivity.this,
                                mEnglish
                                        ? "Transparent background saved"
                                        : "已设置为透明背景",
                                Toast.LENGTH_SHORT
                        ).show();
                    }
                }
        );

        LinearLayout.LayoutParams transparentParams =
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                );

        transparentParams.topMargin =
                Math.round(24 * density);

        root.addView(
                transparentButton,
                transparentParams
        );

        Button backButton =
                new Button(this);

        backButton.setText(
                mEnglish
                        ? "Back"
                        : "返回"
        );

        backButton.setAllCaps(false);

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
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                );

        backParams.topMargin =
                Math.round(12 * density);

        root.addView(
                backButton,
                backParams
        );

        setContentView(root);
    }

    private void saveColor() {

        getSharedPreferences(
                PREFS_NAME,
                MODE_PRIVATE
        )
                .edit()
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

        drawable.setShape(
                GradientDrawable.RECTANGLE
        );

        drawable.setColor(
                mSelectedColor
        );

        drawable.setCornerRadius(
                1000
        );

        mPreview.setBackground(
                drawable
        );

        mPreview.setTextColor(
                getPreviewTextColor()
        );
    }

    private int getPreviewTextColor() {

        if (mSelectedColor == Color.TRANSPARENT) {
            return Color.BLACK;
        }

        int red =
                Color.red(mSelectedColor);

        int green =
                Color.green(mSelectedColor);

        int blue =
                Color.blue(mSelectedColor);

        int brightness =
                (red * 299
                        + green * 587
                        + blue * 114)
                        / 1000;

        return brightness < 150
                ? Color.WHITE
                : Color.BLACK;
    }
}
