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

    private static final String UI_PREFS_NAME = "ui_prefs";
    private static final String KEY_BG_COLOR = "bg_color";

    private LinearLayout preview;
    private TextView previewText;

    private boolean english;

    private final int[] COLORS = {
            Color.TRANSPARENT,
            Color.WHITE,
            Color.BLACK,
            Color.rgb(144, 238, 144),
            Color.rgb(76, 175, 80),
            Color.rgb(173, 216, 230),
            Color.rgb(255, 235, 59),
            Color.rgb(255, 152, 0),
            Color.rgb(244, 67, 54),
            Color.rgb(156, 39, 176)
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SharedPreferences prefs =
                getSharedPreferences(UI_PREFS_NAME, MODE_PRIVATE);

        english = "en".equals(
                prefs.getString("language", "zh")
        );

        buildPage();
    }

    private void buildPage() {

        float density =
                getResources().getDisplayMetrics().density;

        int padding =
                Math.round(24 * density);

        LinearLayout root =
                new LinearLayout(this);

        root.setOrientation(
                LinearLayout.VERTICAL
        );

        root.setPadding(
                padding,
                padding,
                padding,
                padding
        );

        TextView title =
                new TextView(this);

        title.setText(
                english
                        ? "Background Color"
                        : "背景颜色"
        );

        title.setTextSize(20);

        root.addView(
                title,
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                )
        );

        TextView description =
                new TextView(this);

        description.setText(
                english
                        ? "Choose the capsule background color."
                        : "选择状态栏胶囊背景颜色。"
        );

        description.setTextSize(14);

        LinearLayout.LayoutParams descParams =
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                );

        descParams.topMargin =
                Math.round(16 * density);

        root.addView(
                description,
                descParams
        );

        preview =
                new LinearLayout(this);

        preview.setGravity(
                Gravity.CENTER
        );

        LinearLayout.LayoutParams previewParams =
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        Math.round(90 * density)
                );

        previewParams.topMargin =
                Math.round(28 * density);

        root.addView(
                preview,
                previewParams
        );

        previewText =
                new TextView(this);

        previewText.setText(
                english
                        ? "21:11 2.5G/8G"
                        : "21:11 2.5G/8G"
        );

        previewText.setTextSize(15);

        previewText.setGravity(
                Gravity.CENTER
        );

        previewText.setPadding(
                Math.round(16 * density),
                Math.round(7 * density),
                Math.round(16 * density),
                Math.round(7 * density)
        );

        updatePreview();

        preview.addView(
                previewText,
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                )
        );

        TextView colorTitle =
                new TextView(this);

        colorTitle.setText(
                english
                        ? "Preset Colors"
                        : "预设颜色"
        );

        colorTitle.setTextSize(16);

        LinearLayout.LayoutParams colorTitleParams =
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                );

        colorTitleParams.topMargin =
                Math.round(28 * density);

        root.addView(
                colorTitle,
                colorTitleParams
        );

        for (final int color : COLORS) {

            Button button =
                    new Button(this);

            button.setText(
                    getColorName(color)
            );

            button.setOnClickListener(
                    new View.OnClickListener() {

                        @Override
                        public void onClick(View v) {

                            saveColor(color);

                            updatePreview();

                            Toast.makeText(
                                    ColorSettingsActivity.this,
                                    english
                                            ? "Color saved"
                                            : "颜色已保存",
                                    Toast.LENGTH_SHORT
                            ).show();
                        }
                    }
            );

            LinearLayout.LayoutParams params =
                    new LinearLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT
                    );

            params.topMargin =
                    Math.round(6 * density);

            root.addView(
                    button,
                    params
            );
        }

        Button backButton =
                new Button(this);

        backButton.setText(
                english
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
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                );

        backParams.topMargin =
                Math.round(20 * density);

        root.addView(
                backButton,
                backParams
        );

        setContentView(root);
    }

    private String getColorName(int color) {

        if (color == Color.TRANSPARENT) {
            return english
                    ? "Transparent"
                    : "透明";
        }

        if (color == Color.WHITE) {
            return english
                    ? "White"
                    : "白色";
        }

        if (color == Color.BLACK) {
            return english
                    ? "Black"
                    : "黑色";
        }

        if (color == Color.rgb(144, 238, 144)) {
            return english
                    ? "Light Green"
                    : "浅绿色";
        }

        if (color == Color.rgb(76, 175, 80)) {
            return english
                    ? "Green"
                    : "绿色";
        }

        if (color == Color.rgb(173, 216, 230)) {
            return english
                    ? "Light Blue"
                    : "浅蓝色";
        }

        if (color == Color.rgb(255, 235, 59)) {
            return english
                    ? "Yellow"
                    : "黄色";
        }

        if (color == Color.rgb(255, 152, 0)) {
            return english
                    ? "Orange"
                    : "橙色";
        }

        if (color == Color.rgb(244, 67, 54)) {
            return english
                    ? "Red"
                    : "红色";
        }

        if (color == Color.rgb(156, 39, 176)) {
            return english
                    ? "Purple"
                    : "紫色";
        }

        return english
                ? "Custom"
                : "自定义";
    }

    private void saveColor(int color) {

        getSharedPreferences(
                UI_PREFS_NAME,
                MODE_PRIVATE
        )
                .edit()
                .putInt(
                        KEY_BG_COLOR,
                        color
                )
                .apply();
    }

    private void updatePreview() {

        int color =
                getSharedPreferences(
                        UI_PREFS_NAME,
                        MODE_PRIVATE
                )
                        .getInt(
                                KEY_BG_COLOR,
                                Color.TRANSPARENT
                        );

        GradientDrawable drawable =
                new GradientDrawable();

        drawable.setColor(color);

        float density =
                getResources()
                        .getDisplayMetrics()
                        .density;

        drawable.setCornerRadius(
                100 * density
        );

        previewText.setBackground(
                drawable
        );

        if (color == Color.WHITE
                || color == Color.rgb(144, 238, 144)
                || color == Color.rgb(173, 216, 230)
                || color == Color.rgb(255, 235, 59)) {

            previewText.setTextColor(
                    Color.BLACK
            );

        } else {

            previewText.setTextColor(
                    Color.WHITE
            );
        }
    }
}
