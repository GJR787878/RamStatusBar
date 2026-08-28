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

    private boolean mEnglish;

    private int mSelectedColor;

    private TextView mPreview;

    /*
     * 预设颜色。
     *
     * 格式：
     * AARRGGBB
     */
    private static final int[] COLORS = {

            0xCC000000,
            0xCCFFFFFF,

            0xCC202124,
            0xCC3F51B5,
            0xCC2196F3,
            0xCC03A9F4,
            0xCC00BCD4,

            0xCC009688,
            0xCC4CAF50,
            0xCC8BC34A,

            0xCCCDDC39,
            0xCCFFEB3B,
            0xCCFFC107,
            0xCCFF9800,

            0xCCFF5722,
            0xCCF44336,
            0xCCE91E63,
            0xCC9C27B0,
            0xCC673AB7
    };

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

        for (int i = 0;
             i < COLORS.length;
             i++) {

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

                            if (saveColorToFile(
                                    color)) {

                                mSelectedColor =
                                        color;

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

        /*
         * 透明背景按钮。
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
        if (mSelectedColor ==
                Color.TRANSPARENT) {

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

        if (mSelectedColor ==
                Color.TRANSPARENT) {

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
}
