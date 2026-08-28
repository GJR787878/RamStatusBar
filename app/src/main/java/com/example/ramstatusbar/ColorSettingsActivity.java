package com.example.ramstatusbar;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.graphics.SweepGradient;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.DataOutputStream;
import java.util.Locale;

public class ColorSettingsActivity extends Activity {

    private static final String PREFS_NAME =
            "ui_prefs";
    private static final String KEY_LANGUAGE =
            "language";
    /*
     * MainActivity 和 SystemUI 的 MainHook
     * 都通过这个文件共享背景颜色。
     */
    private static final String COLOR_FILE =
            "/data/local/tmp/ramstatusbar_color";
    private static final String LANG_EN =
            "en";

    /*
     * 取色器里的 6 个预设色（AARRGGBB）。
     */
    private static final int[] PRESET_COLORS = {
            0xFF000000,
            0xFFFFFFFF,
            0xFF9E9E9E,
            0xFF2196F3,
            0xFF4CAF50,
            0xFFF44336
    };

    private boolean mEnglish;
    private int mSelectedColor;
    private TextView mPreview;

    @Override
    protected void onCreate(
            Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SharedPreferences prefs =
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
        /*
         * 默认使用半透明黑色。
         */
        mSelectedColor =
                readColorFromFile();
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
                        ? "Choose a background color for the status bar capsule."
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
        mPreview.setGravity(
                Gravity.CENTER
        );
        mPreview.setText(
                "21:11 2.5G/8G"
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
        /*
         * 打开 HSV 取色器按钮。
         */
        Button pickButton =
                new Button(this);
        pickButton.setText(
                mEnglish
                        ? "Pick Color"
                        : "选择颜色"
        );
        pickButton.setAllCaps(false);
        pickButton.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(
                            View v) {
                        showColorPicker();
                    }
                }
        );
        root.addView(
                pickButton,
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                )
        );
        /*
         * 透明背景按钮（保留）。
         */
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
                    public void onClick(
                            View v) {
                        if (saveColorToFile(
                                Color.TRANSPARENT)) {
                            mSelectedColor =
                                    Color.TRANSPARENT;
                            updatePreview();
                            Toast.makeText(
                                    ColorSettingsActivity.this,
                                    mEnglish
                                            ? "Transparent background saved"
                                            : "已设置为透明背景",
                                    Toast.LENGTH_SHORT
                            ).show();
                        } else {
                            Toast.makeText(
                                    ColorSettingsActivity.this,
                                    mEnglish
                                            ? "Save failed. Please grant root permission."
                                            : "保存失败，请检查 root 权限。",
                                    Toast.LENGTH_LONG
                            ).show();
                        }
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
        /*
         * 返回按钮。
         */
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
                    public void onClick(
                            View v) {
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

    /*
     * ================== HSV 取色对话框 ==================
     * 复刻“Current/New 对比 + 圆形 HSV 色轮 + 亮度滑块 +
     * 透明度滑块 + 6 个预设色 + Cancel/Done”的样式。
     */
    private void showColorPicker() {
        final float density =
                getResources()
                        .getDisplayMetrics()
                        .density;
        /*
         * 对话框内容根布局。
         */
        LinearLayout content =
                new LinearLayout(this);
        content.setOrientation(
                LinearLayout.VERTICAL
        );
        content.setPadding(
                Math.round(20 * density),
                Math.round(16 * density),
                Math.round(20 * density),
                Math.round(8 * density)
        );
        /*
         * Current / New 颜色对比行。
         */
        LinearLayout compareRow =
                new LinearLayout(this);
        compareRow.setOrientation(
                LinearLayout.HORIZONTAL
        );
        compareRow.setGravity(
                Gravity.CENTER_VERTICAL
        );
        final View currentPanel =
                buildColorPanel(
                        mEnglish
                                ? "Current"
                                : "当前"
                );
        final View newPanel =
                buildColorPanel(
                        mEnglish
                                ? "New"
                                : "新颜色"
                );
        TextView arrow =
                new TextView(this);
        arrow.setText("  →  ");
        arrow.setTextSize(18);
        arrow.setGravity(
                Gravity.CENTER
        );
        compareRow.addView(
                currentPanel,
                new LinearLayout.LayoutParams(
                        0,
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        1f
                )
        );
        compareRow.addView(
                arrow,
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                )
        );
        compareRow.addView(
                newPanel,
                new LinearLayout.LayoutParams(
                        0,
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        1f
                )
        );
        content.addView(
                compareRow
        );
        /*
         * HSV 色轮。
         */
        final HsvWheelView wheel =
                new HsvWheelView(this);
        LinearLayout.LayoutParams wheelParams =
                new LinearLayout.LayoutParams(
                        Math.round(240 * density),
                        Math.round(240 * density)
                );
        wheelParams.gravity =
                Gravity.CENTER_HORIZONTAL;
        wheelParams.topMargin =
                Math.round(8 * density);
        content.addView(
                wheel,
                wheelParams
        );
        /*
         * HEX 显示。
         */
        final TextView hexText =
                new TextView(this);
        hexText.setGravity(
                Gravity.CENTER
        );
        hexText.setTextSize(13);
        hexText.setPadding(
                0,
                Math.round(4 * density),
                0,
                Math.round(4 * density)
        );
        content.addView(
                hexText
        );
        /*
         * 亮度滑块。
         */
        LinearLayout valueRow =
                new LinearLayout(this);
        valueRow.setOrientation(
                LinearLayout.HORIZONTAL
        );
        valueRow.setGravity(
                Gravity.CENTER_VERTICAL
        );
        TextView valueLabel =
                new TextView(this);
        valueLabel.setText(
                mEnglish
                        ? "Brightness"
                        : "亮度"
        );
        valueLabel.setTextSize(13);
        valueLabel.setPadding(
                0,
                0,
                Math.round(12 * density),
                0
        );
        final SeekBar valueSeek =
                new SeekBar(this);
        valueSeek.setMax(255);
        valueRow.addView(
                valueLabel,
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                )
        );
        valueRow.addView(
                valueSeek,
                new LinearLayout.LayoutParams(
                        0,
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        1f
                )
        );
        content.addView(
                valueRow
        );
        /*
         * 透明度滑块。
         */
        LinearLayout alphaRow =
                new LinearLayout(this);
        alphaRow.setOrientation(
                LinearLayout.HORIZONTAL
        );
        alphaRow.setGravity(
                Gravity.CENTER_VERTICAL
        );
        TextView alphaLabel =
                new TextView(this);
        alphaLabel.setText(
                mEnglish
                        ? "Opacity"
                        : "透明度"
        );
        alphaLabel.setTextSize(13);
        alphaLabel.setPadding(
                0,
                0,
                Math.round(12 * density),
                0
        );
        final SeekBar alphaSeek =
                new SeekBar(this);
        alphaSeek.setMax(255);
        final TextView alphaValue =
                new TextView(this);
        alphaValue.setTextSize(13);
        alphaRow.addView(
                alphaLabel,
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                )
        );
        alphaRow.addView(
                alphaSeek,
                new LinearLayout.LayoutParams(
                        0,
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        1f
                )
        );
        alphaRow.addView(
                alphaValue,
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                )
        );
        content.addView(
                alphaRow
        );
        /*
         * 6 个预设色位。
         */
        LinearLayout presetsRow =
                new LinearLayout(this);
        presetsRow.setOrientation(
                LinearLayout.HORIZONTAL
        );
        presetsRow.setGravity(
                Gravity.CENTER
        );
        presetsRow.setPadding(
                0,
                Math.round(12 * density),
                0,
                Math.round(4 * density)
        );
        content.addView(
                presetsRow
        );
        /*
         * 当前在对话框里选中的颜色（AARRGGBB）。
         */
        final int[] dialogColor =
                new int[]{
                        mSelectedColor
                };
        /*
         * 更新“新颜色”面板和 HEX 文本。
         */
        final Runnable updateNewColor =
                new Runnable() {
                    @Override
                    public void run() {
                        setPanelColor(
                                newPanel,
                                dialogColor[0]
                        );
                        hexText.setText(
                                String.format(
                                        Locale.US,
                                        "#%02X%02X%02X%02X",
                                        Color.alpha(
                                                dialogColor[0]
                                        ),
                                        Color.red(
                                                dialogColor[0]
                                        ),
                                        Color.green(
                                                dialogColor[0]
                                        ),
                                        Color.blue(
                                                dialogColor[0]
                                        )
                                )
                        );
                    }
                };
        /*
         * 色轮拖动 → 更新颜色（保留当前 alpha）。
         */
        wheel.setOnColorChangedListener(
                new HsvWheelView.OnColorChangedListener() {
                    @Override
                    public void onColorChanged(
                            int color) {
                        dialogColor[0] =
                                (color & 0x00FFFFFF)
                                        | (Color.alpha(
                                                dialogColor[0])
                                        << 24);
                        updateNewColor.run();
                    }
                }
        );
        /*
         * 亮度滑块 → 更新色轮明暗。
         */
        valueSeek.setOnSeekBarChangeListener(
                new SeekBar.OnSeekBarChangeListener() {
                    @Override
                    public void onProgressChanged(
                            SeekBar seekBar,
                            int progress,
                            boolean fromUser) {
                        if (!fromUser) {
                            return;
                        }
                        wheel.setValue(
                                progress / 255f
                        );
                        int color =
                                wheel.getColor();
                        dialogColor[0] =
                                (color & 0x00FFFFFF)
                                        | (Color.alpha(
                                                dialogColor[0])
                                        << 24);
                        updateNewColor.run();
                    }

                    @Override
                    public void onStartTrackingTouch(
                            SeekBar seekBar) {
                    }

                    @Override
                    public void onStopTrackingTouch(
                            SeekBar seekBar) {
                    }
                }
        );
        /*
         * 透明度滑块。
         */
        alphaSeek.setOnSeekBarChangeListener(
                new SeekBar.OnSeekBarChangeListener() {
                    @Override
                    public void onProgressChanged(
                            SeekBar seekBar,
                            int progress,
                            boolean fromUser) {
                        if (!fromUser) {
                            return;
                        }
                        dialogColor[0] =
                                (dialogColor[0]
                                        & 0x00FFFFFF)
                                        | (progress << 24);
                        alphaValue.setText(
                                String.valueOf(
                                        Math.round(
                                                progress
                                                        * 100f
                                                        / 255f
                                        )
                                ) + "%"
                        );
                        updateNewColor.run();
                    }

                    @Override
                    public void onStartTrackingTouch(
                            SeekBar seekBar) {
                    }

                    @Override
                    public void onStopTrackingTouch(
                            SeekBar seekBar) {
                    }
                }
        );
        /*
         * 预设色点击。
         */
        for (final int preset :
                PRESET_COLORS) {
            Button presetButton =
                    new Button(this);
            presetButton.setText("");
            GradientDrawable presetDrawable =
                    new GradientDrawable();
            presetDrawable.setShape(
                    GradientDrawable.OVAL
            );
            presetDrawable.setColor(
                    preset
            );
            presetDrawable.setStroke(
                    Math.round(1 * density),
                    0x55000000
            );
            presetButton.setBackground(
                    presetDrawable
            );
            presetButton.setOnClickListener(
                    new View.OnClickListener() {
                        @Override
                        public void onClick(
                                View v) {
                            dialogColor[0] =
                                    preset;
                            float[] hsv =
                                    new float[3];
                            Color.colorToHSV(
                                    preset,
                                    hsv
                            );
                            wheel.setColor(
                                    preset
                            );
                            valueSeek.setProgress(
                                    Math.round(
                                            hsv[2]
                                                    * 255
                                    )
                            );
                            int alpha =
                                    Color.alpha(
                                            preset
                                    );
                            alphaSeek.setProgress(
                                    alpha
                            );
                            alphaValue.setText(
                                    String.valueOf(
                                            Math.round(
                                                    alpha
                                                            * 100f
                                                            / 255f
                                            )
                                    ) + "%"
                            );
                            updateNewColor.run();
                        }
                    }
            );
            LinearLayout.LayoutParams presetParams =
                    new LinearLayout.LayoutParams(
                            Math.round(40 * density),
                            Math.round(40 * density)
                    );
            presetParams.setMargins(
                    Math.round(4 * density),
                    0,
                    Math.round(4 * density),
                    0
            );
            presetsRow.addView(
                    presetButton,
                    presetParams
            );
        }
        /*
         * 初始化对话框状态。
         *
         * 如果当前是透明背景，就从半透明黑开始取色，
         * 避免起点全透明看不见。
         */
        int startColor =
                mSelectedColor;
        if (Color.alpha(startColor)
                == 0) {
            startColor = 0xCC000000;
        }
        float[] startHsv =
                new float[3];
        Color.colorToHSV(
                startColor,
                startHsv
        );
        /*
         * 如果起始颜色太暗（几乎纯黑），
         * 把亮度提到最亮，让取色盘一打开就是鲜艳的。
         */
        if (startHsv[2] < 0.2f) {
            startHsv[2] = 1f;
            startColor =
                    Color.HSVToColor(
                            Color.alpha(
                                    startColor
                            ),
                            startHsv
                    );
        }
        dialogColor[0] =
                startColor;
        setPanelColor(
                currentPanel,
                mSelectedColor
        );
        wheel.setColor(
                startColor
        );
        valueSeek.setProgress(
                Math.round(
                        startHsv[2]
                                * 255
                )
        );
        int startAlpha =
                Color.alpha(
                        startColor
                );
        alphaSeek.setProgress(
                startAlpha
        );
        alphaValue.setText(
                String.valueOf(
                        Math.round(
                                startAlpha
                                        * 100f
                                        / 255f
                        )
                ) + "%"
        );
        updateNewColor.run();
        /*
         * 弹出对话框。
         */
        new AlertDialog.Builder(
                this
        )
                .setView(content)
                .setPositiveButton(
                        mEnglish
                                ? "Done"
                                : "确定",
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(
                                    DialogInterface dialog,
                                    int which) {
                                if (saveColorToFile(
                                        dialogColor[0])) {
                                    mSelectedColor =
                                            dialogColor[0];
                                    updatePreview();
                                    Toast.makeText(
                                            ColorSettingsActivity.this,
                                            mEnglish
                                                    ? "Color saved"
                                                    : "颜色已保存",
                                            Toast.LENGTH_SHORT
                                    ).show();
                                } else {
                                    Toast.makeText(
                                            ColorSettingsActivity.this,
                                            mEnglish
                                                    ? "Save failed. Please grant root permission."
                                                    : "保存失败，请检查 root 权限。",
                                            Toast.LENGTH_LONG
                                    ).show();
                                }
                            }
                        }
                )
                .setNegativeButton(
                        mEnglish
                                ? "Cancel"
                                : "取消",
                        null
                )
                .show();
    }

    /*
     * 构建一个带文字标签的颜色面板。
     *
     * 返回的 View 用 setTag 保存内部的色块 View，
     * 之后通过 setPanelColor 更新颜色。
     */
    private View buildColorPanel(
            String label) {
        float density =
                getResources()
                        .getDisplayMetrics()
                        .density;
        LinearLayout column =
                new LinearLayout(this);
        column.setOrientation(
                LinearLayout.VERTICAL
        );
        column.setGravity(
                Gravity.CENTER_HORIZONTAL
        );
        TextView labelView =
                new TextView(this);
        labelView.setText(
                label
        );
        labelView.setTextSize(13);
        labelView.setGravity(
                Gravity.CENTER
        );
        column.addView(
                labelView,
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                )
        );
        View square =
                new View(this);
        LinearLayout.LayoutParams squareParams =
                new LinearLayout.LayoutParams(
                        Math.round(64 * density),
                        Math.round(40 * density)
                );
        squareParams.topMargin =
                Math.round(4 * density);
        column.addView(
                square,
                squareParams
        );
        column.setTag(square);
        return column;
    }

    /*
     * 更新颜色面板里色块的颜色。
     */
    private void setPanelColor(
            View panel,
            int color) {
        View square =
                (View) panel.getTag();
        GradientDrawable drawable =
                new GradientDrawable();
        drawable.setShape(
                GradientDrawable.RECTANGLE
        );
        drawable.setCornerRadius(
                6
                        * getResources()
                        .getDisplayMetrics()
                        .density
        );
        drawable.setColor(
                color
        );
        drawable.setStroke(
                Math.round(
                        1
                                * getResources()
                                .getDisplayMetrics()
                                .density
                ),
                0x55000000
        );
        square.setBackground(
                drawable
        );
    }

    /*
     * 从共享文件读取颜色。
     *
     * 文件内容格式：
     *
     * CC000000
     * FFFFFFFF
     * 00000000
     *
     * 即 AARRGGBB。
     */
    private int readColorFromFile() {
        try {
            java.io.File file =
                    new java.io.File(
                            COLOR_FILE
                    );
            if (!file.exists()) {
                return 0xCC000000;
            }
            java.io.BufferedReader br =
                    new java.io.BufferedReader(
                            new java.io.FileReader(
                                    file
                            )
                    );
            String line =
                    br.readLine();
            br.close();
            if (line == null) {
                return 0xCC000000;
            }
            line =
                    line.trim()
                            .replace(
                                    "#",
                                    ""
                            );
            long value =
                    Long.parseLong(
                            line,
                            16
                    );
            return (int) value;
        } catch (Throwable t) {
            return 0xCC000000;
        }
    }

    /*
     * 使用 root 写入共享颜色文件。
     */
    private boolean saveColorToFile(
            int color) {
        try {
            /*
             * 转成 8 位 AARRGGBB。
             */
            String hex =
                    String.format(
                            Locale.US,
                            "%08X",
                            color
                    );
            Process su =
                    Runtime.getRuntime()
                            .exec("su");
            DataOutputStream os =
                    new DataOutputStream(
                            su.getOutputStream()
                    );
            os.writeBytes(
                    "echo "
                            + hex
                            + " > "
                            + COLOR_FILE
                            + "\n"
            );
            os.writeBytes(
                    "chmod 666 "
                            + COLOR_FILE
                            + "\n"
            );
            os.writeBytes(
                    "exit\n"
            );
            os.flush();
            int result =
                    su.waitFor();
            return result == 0;
        } catch (Throwable t) {
            return false;
        }
    }

    private void updatePreview() {
        if (mPreview == null) {
            return;
        }
        /*
         * 完全透明时不设置 drawable。
         */
        if (mSelectedColor
                == Color.TRANSPARENT) {
            mPreview.setBackground(
                    null
            );
        } else {
            GradientDrawable drawable =
                    new GradientDrawable();
            drawable.setShape(
                    GradientDrawable.RECTANGLE
            );
            drawable.setColor(
                    mSelectedColor
            );
            /*
             * 超大圆角 = 胶囊形。
             */
            drawable.setCornerRadius(
                    1000f
            );
            mPreview.setBackground(
                    drawable
            );
        }
        mPreview.setTextColor(
                getPreviewTextColor()
        );
    }

    private int getPreviewTextColor() {
        if (mSelectedColor
                == Color.TRANSPARENT) {
            return Color.BLACK;
        }
        int red =
                Color.red(
                        mSelectedColor
                );
        int green =
                Color.green(
                        mSelectedColor
                );
        int blue =
                Color.blue(
                        mSelectedColor
                );
        int brightness =
                (red * 299
                        + green * 587
                        + blue * 114)
                        / 1000;
        return brightness < 150
                ? Color.WHITE
                : Color.BLACK;
    }

    /*
     * ================== HSV 色轮控件 ==================
     *
     * 用 SweepGradient（色相）+ RadialGradient（饱和度）
     * 渲染一次色轮位图，亮度用黑色半透明遮罩实现，
     * 拖动时无需重新渲染，性能更好。
     */
    private static class HsvWheelView
            extends View {

        interface OnColorChangedListener {
            void onColorChanged(int color);
        }

        /*
         * 色相环颜色（红→黄→绿→青→蓝→品红→红）。
         */
        private static final int[] HUES = {
                0xFFFF0000,
                0xFFFFFF00,
                0xFF00FF00,
                0xFF00FFFF,
                0xFF0000FF,
                0xFFFF00FF,
                0xFFFF0000
        };

        private final Paint mPaint =
                new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint mSelectorPaint =
                new Paint(Paint.ANTI_ALIAS_FLAG);
        private Bitmap mWheelBitmap;
        private float mRadius;
        private float mCenterX;
        private float mCenterY;
        private float mHue;
        private float mSat;
        private float mValue = 1f;
        private OnColorChangedListener mListener;

        HsvWheelView(Context context) {
            super(context);
            mSelectorPaint.setStyle(
                    Paint.Style.STROKE
            );
            mSelectorPaint.setStrokeWidth(
                    3
                            * context
                            .getResources()
                            .getDisplayMetrics()
                            .density
            );
            mSelectorPaint.setColor(
                    Color.WHITE
            );
        }

        void setOnColorChangedListener(
                OnColorChangedListener listener) {
            mListener = listener;
        }

        void setValue(float value) {
            mValue = Math.max(
                    0f,
                    Math.min(
                            1f,
                            value
                    )
            );
            invalidate();
        }

        int getColor() {
            return Color.HSVToColor(
                    new float[]{
                            mHue,
                            mSat,
                            mValue
                    }
            );
        }

        void setColor(int color) {
            float[] hsv =
                    new float[3];
            Color.colorToHSV(
                    color,
                    hsv
            );
            mHue = hsv[0];
            mSat = hsv[1];
            mValue = hsv[2];
            invalidate();
        }

        @Override
        protected void onSizeChanged(
                int w,
                int h,
                int oldw,
                int oldh) {
            super.onSizeChanged(
                    w,
                    h,
                    oldw,
                    oldh
            );
            mRadius =
                    Math.min(w, h)
                            / 2f
                            - 2;
            mCenterX =
                    w / 2f;
            mCenterY =
                    h / 2f;
            if (mRadius <= 0) {
                mWheelBitmap = null;
                return;
            }
            int size =
                    Math.round(
                            mRadius
                                    * 2
                    );
            mWheelBitmap =
                    Bitmap.createBitmap(
                            size,
                            size,
                            Bitmap.Config.ARGB_8888
                    );
            Canvas canvas =
                    new Canvas(
                            mWheelBitmap
                    );
            float cx =
                    size / 2f;
            float cy =
                    size / 2f;
            Paint paint =
                    new Paint(
                            Paint.ANTI_ALIAS_FLAG
                    );
            /*
             * 1) 色相：扫描渐变。
             */
            paint.setShader(
                    new SweepGradient(
                            cx,
                            cy,
                            HUES,
                            null
                    )
            );
            canvas.drawCircle(
                    cx,
                    cy,
                    mRadius,
                    paint
            );
            /*
             * 2) 饱和度：中心白色 → 边缘透明，
             *    越靠中心越接近白色（饱和度越低）。
             */
            paint.setShader(
                    new RadialGradient(
                            cx,
                            cy,
                            mRadius,
                            new int[]{
                                    0xFFFFFFFF,
                                    0x00FFFFFF
                            },
                            new float[]{
                                    0f,
                                    1f
                            },
                            Shader.TileMode.CLAMP
                    )
            );
            canvas.drawCircle(
                    cx,
                    cy,
                    mRadius,
                    paint
            );
            paint.setShader(null);
        }

        @Override
        protected void onDraw(
                Canvas canvas) {
            super.onDraw(canvas);
            if (mWheelBitmap != null) {
                canvas.drawBitmap(
                        mWheelBitmap,
                        mCenterX
                                - mRadius,
                        mCenterY
                                - mRadius,
                        mPaint
                );
            }
            /*
             * 亮度遮罩。
             *
             * 最多压暗 50%，保证取色盘在任何亮度下
             * 都能看到颜色，不会整个变黑。
             */
            if (mValue < 1f) {
                int dim =
                        Math.round(
                                (1f
                                        - mValue)
                                        * 128f
                        );
                if (dim > 0) {
                    mPaint.setColor(
                            Color.argb(
                                    dim,
                                    0,
                                    0,
                                    0
                            )
                    );
                    mPaint.setStyle(
                            Paint.Style.FILL
                    );
                    canvas.drawCircle(
                            mCenterX,
                            mCenterY,
                            mRadius,
                            mPaint
                    );
                }
            }
            /*
             * 选择点：深色外圈 + 白色内圈，
             * 保证在亮色区域也看得见。
             */
            double angle =
                    Math.toRadians(
                            mHue
                    );
            float selectorRadius =
                    mSat
                            * mRadius;
            float sx =
                    mCenterX
                            + (float) (Math.cos(angle)
                            * selectorRadius);
            float sy =
                    mCenterY
                            + (float) (Math.sin(angle)
                            * selectorRadius);
            float selectorDensity =
                    getResources()
                            .getDisplayMetrics()
                            .density;
            mSelectorPaint.setStyle(
                    Paint.Style.STROKE
            );
            mSelectorPaint.setStrokeWidth(
                    2 * selectorDensity
            );
            mSelectorPaint.setColor(
                    0xCC000000
            );
            canvas.drawCircle(
                    sx,
                    sy,
                    13 * selectorDensity,
                    mSelectorPaint
            );
            mSelectorPaint.setStrokeWidth(
                    3 * selectorDensity
            );
            mSelectorPaint.setColor(
                    Color.WHITE
            );
            canvas.drawCircle(
                    sx,
                    sy,
                    10 * selectorDensity,
                    mSelectorPaint
            );
        }

        @Override
        public boolean onTouchEvent(
                MotionEvent event) {
            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                case MotionEvent.ACTION_MOVE: {
                    float dx =
                            event.getX()
                                    - mCenterX;
                    float dy =
                            event.getY()
                                    - mCenterY;
                    float dist =
                            (float) Math.sqrt(
                                    dx * dx
                                            + dy * dy
                            );
                    if (dist > mRadius) {
                        /*
                         * 拖到圈外时夹到边缘。
                         */
                        dx =
                                dx
                                        / dist
                                        * mRadius;
                        dy =
                                dy
                                        / dist
                                        * mRadius;
                        mSat = 1f;
                    } else {
                        mSat =
                                dist
                                        / mRadius;
                    }
                    mHue =
                            (float) ((Math.atan2(
                                    dy,
                                    dx
                            )
                                    * 180.0
                                    / Math.PI)
                                    + 360.0)
                                    % 360f;
                    if (mListener != null) {
                        mListener.onColorChanged(
                                getColor()
                        );
                    }
                    invalidate();
                    return true;
                }
                default:
                    return super.onTouchEvent(
                            event
                    );
            }
        }
    }
}
