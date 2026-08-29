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

    private static final String PREFS_NAME = "ui_prefs";
    private static final String KEY_LANGUAGE = "language";
    private static final String COLOR_FILE = "/data/local/tmp/ramstatusbar_color";
    private static final String LANG_ZH = "zh";
    private static final String LANG_EN = "en";
    private static final String LANG_RU = "ru";

    private static final int[] PRESET_COLORS = {
            0xFF000000, 0xFFFFFFFF, 0xFF9E9E9E,
            0xFF2196F3, 0xFF4CAF50, 0xFFF44336
    };

    private static final int COLOR_WHITE = 0xFFFFFFFF;
    private static final int COLOR_NAV_BG = 0xB31C1C1E;
    private static final int COLOR_NAV_BORDER = 0x40FFFFFF;

    private String mLanguage = LANG_ZH;
    private int mSelectedColor;
    private TextView mPreview;

    private String lang(String zh, String en, String ru) {
        if (LANG_RU.equals(mLanguage)) return ru;
        if (LANG_EN.equals(mLanguage)) return en;
        return zh;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        mLanguage = prefs.getString(KEY_LANGUAGE, LANG_ZH);

        mSelectedColor = readColorFromFile();
        float density = getResources().getDisplayMetrics().density;

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(
                Math.round(24 * density), Math.round(48 * density),
                Math.round(24 * density), Math.round(32 * density));
        root.setBackgroundColor(0xFF000000);

        TextView title = new TextView(this);
        title.setTextSize(20);
        title.setTextColor(COLOR_WHITE);
        title.setText(lang("背景颜色", "Background Color", "Цвет фона"));
        root.addView(title, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));

        TextView description = new TextView(this);
        description.setTextSize(14);
        description.setTextColor(0xFFCCCCCC);
        description.setPadding(0, Math.round(16 * density), 0, Math.round(24 * density));
        description.setText(lang(
                "选择状态栏胶囊背景颜色。",
                "Choose a background color for the status bar capsule.",
                "Выберите цвет фона капсулы в строке состояния."));
        root.addView(description);

        TextView previewTitle = new TextView(this);
        previewTitle.setTextSize(14);
        previewTitle.setTextColor(COLOR_WHITE);
        previewTitle.setText(lang("预览", "Preview", "Предпросмотр"));
        root.addView(previewTitle);

        mPreview = new TextView(this);
        mPreview.setTextSize(16);
        mPreview.setGravity(Gravity.CENTER);
        mPreview.setText("21:11 2.5G/8G");
        LinearLayout.LayoutParams previewParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, Math.round(56 * density));
        previewParams.topMargin = Math.round(12 * density);
        previewParams.bottomMargin = Math.round(32 * density);
        root.addView(mPreview, previewParams);
        updatePreview();

        Button pickButton = new Button(this);
        pickButton.setText(lang("选择颜色", "Pick Color", "Выбрать цвет"));
        pickButton.setAllCaps(false);
        pickButton.setTextColor(COLOR_WHITE);
        pickButton.setBackground(createGlassButtonBg(density));
        pickButton.setPadding(
                Math.round(24 * density), Math.round(14 * density),
                Math.round(24 * density), Math.round(14 * density));
        pickButton.setOnClickListener(v -> showColorPicker());
        root.addView(pickButton, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));

        Button transparentButton = new Button(this);
        transparentButton.setText(lang("透明", "Transparent", "Прозрачный"));
        transparentButton.setAllCaps(false);
        transparentButton.setTextColor(COLOR_WHITE);
        transparentButton.setBackground(createGlassButtonBg(density));
        transparentButton.setPadding(
                Math.round(24 * density), Math.round(14 * density),
                Math.round(24 * density), Math.round(14 * density));
        transparentButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (saveColorToFile(Color.TRANSPARENT)) {
                    mSelectedColor = Color.TRANSPARENT;
                    updatePreview();
                    Toast.makeText(ColorSettingsActivity.this,
                            lang("已设置为透明背景",
                                    "Transparent background saved",
                                    "Прозрачный фон установлен"),
                            Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(ColorSettingsActivity.this,
                            lang("保存失败，请检查 root 权限。",
                                    "Save failed. Please grant root permission.",
                                    "Ошибка сохранения. Предоставьте права root."),
                            Toast.LENGTH_LONG).show();
                }
            }
        });
        LinearLayout.LayoutParams transparentParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        transparentParams.topMargin = Math.round(24 * density);
        root.addView(transparentButton, transparentParams);

        Button backButton = new Button(this);
        backButton.setText(lang("返回", "Back", "Назад"));
        backButton.setAllCaps(false);
        backButton.setTextColor(COLOR_WHITE);
        backButton.setBackground(createGlassButtonBg(density));
        backButton.setPadding(
                Math.round(24 * density), Math.round(14 * density),
                Math.round(24 * density), Math.round(14 * density));
        backButton.setOnClickListener(v -> finish());
        LinearLayout.LayoutParams backParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        backParams.topMargin = Math.round(12 * density);
        root.addView(backButton, backParams);

        setContentView(root);
    }

    private GradientDrawable createGlassButtonBg(float density) {
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(COLOR_NAV_BG);
        bg.setCornerRadius(Math.round(28 * density));
        bg.setStroke(Math.round(1 * density), COLOR_NAV_BORDER);
        return bg;
    }

    private void showColorPicker() {
        final float density = getResources().getDisplayMetrics().density;

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(
                Math.round(20 * density), Math.round(16 * density),
                Math.round(20 * density), Math.round(8 * density));

        LinearLayout compareRow = new LinearLayout(this);
        compareRow.setOrientation(LinearLayout.HORIZONTAL);
        compareRow.setGravity(Gravity.CENTER_VERTICAL);

        final View currentPanel = buildColorPanel(
                lang("当前", "Current", "Текущий"));
        final View newPanel = buildColorPanel(
                lang("新颜色", "New", "Новый"));

        TextView arrow = new TextView(this);
        arrow.setText("  →  ");
        arrow.setTextSize(18);
        arrow.setGravity(Gravity.CENTER);

        compareRow.addView(currentPanel, new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        compareRow.addView(arrow, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        compareRow.addView(newPanel, new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        content.addView(compareRow);

        final HsvWheelView wheel = new HsvWheelView(this);
        LinearLayout.LayoutParams wheelParams = new LinearLayout.LayoutParams(
                Math.round(240 * density), Math.round(240 * density));
        wheelParams.gravity = Gravity.CENTER_HORIZONTAL;
        wheelParams.topMargin = Math.round(8 * density);
        content.addView(wheel, wheelParams);

        final TextView hexText = new TextView(this);
        hexText.setGravity(Gravity.CENTER);
        hexText.setTextSize(13);
        hexText.setPadding(0, Math.round(4 * density), 0, Math.round(4 * density));
        content.addView(hexText);

        LinearLayout valueRow = new LinearLayout(this);
        valueRow.setOrientation(LinearLayout.HORIZONTAL);
        valueRow.setGravity(Gravity.CENTER_VERTICAL);
        TextView valueLabel = new TextView(this);
        valueLabel.setText(lang("亮度", "Brightness", "Яркость"));
        valueLabel.setTextSize(13);
        valueLabel.setPadding(0, 0, Math.round(12 * density), 0);
        final SeekBar valueSeek = new SeekBar(this);
        valueSeek.setMax(255);
        valueRow.addView(valueLabel, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        valueRow.addView(valueSeek, new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        content.addView(valueRow);

        LinearLayout alphaRow = new LinearLayout(this);
        alphaRow.setOrientation(LinearLayout.HORIZONTAL);
        alphaRow.setGravity(Gravity.CENTER_VERTICAL);
        TextView alphaLabel = new TextView(this);
        alphaLabel.setText(lang("透明度", "Opacity", "Прозрачность"));
        alphaLabel.setTextSize(13);
        alphaLabel.setPadding(0, 0, Math.round(12 * density), 0);
        final SeekBar alphaSeek = new SeekBar(this);
        alphaSeek.setMax(255);
        final TextView alphaValue = new TextView(this);
        alphaValue.setTextSize(13);
        alphaRow.addView(alphaLabel, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        alphaRow.addView(alphaSeek, new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        alphaRow.addView(alphaValue, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        content.addView(alphaRow);

        LinearLayout presetsRow = new LinearLayout(this);
        presetsRow.setOrientation(LinearLayout.HORIZONTAL);
        presetsRow.setGravity(Gravity.CENTER);
        presetsRow.setPadding(0, Math.round(12 * density), 0, Math.round(4 * density));
        content.addView(presetsRow);

        final int[] dialogColor = new int[]{mSelectedColor};

        final Runnable updateNewColor = new Runnable() {
            @Override
            public void run() {
                setPanelColor(newPanel, dialogColor[0]);
                hexText.setText(String.format(Locale.US, "#%02X%02X%02X%02X",
                        Color.alpha(dialogColor[0]),
                        Color.red(dialogColor[0]),
                        Color.green(dialogColor[0]),
                        Color.blue(dialogColor[0])));
            }
        };

        wheel.setOnColorChangedListener(color -> {
            dialogColor[0] = (color & 0x00FFFFFF) | (Color.alpha(dialogColor[0]) << 24);
            updateNewColor.run();
        });

        valueSeek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (!fromUser) return;
                wheel.setValue(progress / 255f);
                int color = wheel.getColor();
                dialogColor[0] = (color & 0x00FFFFFF) | (Color.alpha(dialogColor[0]) << 24);
                updateNewColor.run();
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        alphaSeek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (!fromUser) return;
                dialogColor[0] = (dialogColor[0] & 0x00FFFFFF) | (progress << 24);
                alphaValue.setText(String.valueOf(Math.round(progress * 100f / 255f)) + "%");
                updateNewColor.run();
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        for (final int preset : PRESET_COLORS) {
            Button presetButton = new Button(this);
            presetButton.setText("");
            GradientDrawable presetDrawable = new GradientDrawable();
            presetDrawable.setShape(GradientDrawable.OVAL);
            presetDrawable.setColor(preset);
            presetDrawable.setStroke(Math.round(1 * density), 0x55000000);
            presetButton.setBackground(presetDrawable);
            presetButton.setOnClickListener(v -> {
                dialogColor[0] = preset;
                float[] hsv = new float[3];
                Color.colorToHSV(preset, hsv);
                wheel.setColor(preset);
                valueSeek.setProgress(Math.round(hsv[2] * 255));
                int alpha = Color.alpha(preset);
                alphaSeek.setProgress(alpha);
                alphaValue.setText(String.valueOf(Math.round(alpha * 100f / 255f)) + "%");
                updateNewColor.run();
            });
            LinearLayout.LayoutParams presetParams = new LinearLayout.LayoutParams(
                    Math.round(40 * density), Math.round(40 * density));
            presetParams.setMargins(Math.round(4 * density), 0, Math.round(4 * density), 0);
            presetsRow.addView(presetButton, presetParams);
        }

        int startColor = mSelectedColor;
        if (Color.alpha(startColor) == 0) {
            startColor = 0xCC000000;
        }
        float[] startHsv = new float[3];
        Color.colorToHSV(startColor, startHsv);
        if (startHsv[2] < 0.2f) {
            startHsv[2] = 1f;
            startColor = Color.HSVToColor(Color.alpha(startColor), startHsv);
        }
        dialogColor[0] = startColor;
        setPanelColor(currentPanel, mSelectedColor);
        wheel.setColor(startColor);
        valueSeek.setProgress(Math.round(startHsv[2] * 255));
        int startAlpha = Color.alpha(startColor);
        alphaSeek.setProgress(startAlpha);
        alphaValue.setText(String.valueOf(Math.round(startAlpha * 100f / 255f)) + "%");
        updateNewColor.run();

        new AlertDialog.Builder(this)
                .setView(content)
                .setPositiveButton(
                        lang("确定", "Done", "Готово"),
                        (dialog, which) -> {
                            if (saveColorToFile(dialogColor[0])) {
                                mSelectedColor = dialogColor[0];
                                updatePreview();
                                Toast.makeText(ColorSettingsActivity.this,
                                        lang("颜色已保存", "Color saved", "Цвет сохранён"),
                                        Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(ColorSettingsActivity.this,
                                        lang("保存失败，请检查 root 权限。",
                                                "Save failed. Please grant root permission.",
                                                "Ошибка сохранения. Предоставьте права root."),
                                        Toast.LENGTH_LONG).show();
                            }
                        })
                .setNegativeButton(
                        lang("取消", "Cancel", "Отмена"), null)
                .show();
    }

    private View buildColorPanel(String label) {
        float density = getResources().getDisplayMetrics().density;
        LinearLayout column = new LinearLayout(this);
        column.setOrientation(LinearLayout.VERTICAL);
        column.setGravity(Gravity.CENTER_HORIZONTAL);
        TextView labelView = new TextView(this);
        labelView.setText(label);
        labelView.setTextSize(13);
        labelView.setGravity(Gravity.CENTER);
        column.addView(labelView, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        View square = new View(this);
        LinearLayout.LayoutParams squareParams = new LinearLayout.LayoutParams(
                Math.round(64 * density), Math.round(40 * density));
        squareParams.topMargin = Math.round(4 * density);
        column.addView(square, squareParams);
        column.setTag(square);
        return column;
    }

    private void setPanelColor(View panel, int color) {
        View square = (View) panel.getTag();
        GradientDrawable drawable = new GradientDrawable();
        drawable.setShape(GradientDrawable.RECTANGLE);
        float density = getResources().getDisplayMetrics().density;
        drawable.setCornerRadius(6 * density);
        drawable.setColor(color);
        drawable.setStroke(Math.round(1 * density), 0x55000000);
        square.setBackground(drawable);
    }

    private int readColorFromFile() {
        try {
            java.io.File file = new java.io.File(COLOR_FILE);
            if (!file.exists()) return 0xCC000000;
            java.io.BufferedReader br = new java.io.BufferedReader(
                    new java.io.FileReader(file));
            String line = br.readLine();
            br.close();
            if (line == null) return 0xCC000000;
            line = line.trim().replace("#", "");
            long value = Long.parseLong(line, 16);
            return (int) value;
        } catch (Throwable t) {
            return 0xCC000000;
        }
    }

    private boolean saveColorToFile(int color) {
        try {
            String hex = String.format(Locale.US, "%08X", color);
            Process su = Runtime.getRuntime().exec("su");
            DataOutputStream os = new DataOutputStream(su.getOutputStream());
            os.writeBytes("echo " + hex + " > " + COLOR_FILE + "\n");
            os.writeBytes("chmod 666 " + COLOR_FILE + "\n");
            os.writeBytes("exit\n");
            os.flush();
            int result = su.waitFor();
            return result == 0;
        } catch (Throwable t) {
            return false;
        }
    }

    private void updatePreview() {
        if (mPreview == null) return;
        if (mSelectedColor == Color.TRANSPARENT) {
            mPreview.setBackground(null);
        } else {
            GradientDrawable drawable = new GradientDrawable();
            drawable.setShape(GradientDrawable.RECTANGLE);
            drawable.setColor(mSelectedColor);
            drawable.setCornerRadius(1000f);
            mPreview.setBackground(drawable);
        }
        mPreview.setTextColor(getPreviewTextColor());
    }

    private int getPreviewTextColor() {
        if (mSelectedColor == Color.TRANSPARENT) return Color.BLACK;
        int red = Color.red(mSelectedColor);
        int green = Color.green(mSelectedColor);
        int blue = Color.blue(mSelectedColor);
        int brightness = (red * 299 + green * 587 + blue * 114) / 1000;
        return brightness < 150 ? Color.WHITE : Color.BLACK;
    }

    // ================== HSV 色轮控件 ==================
    private static class HsvWheelView extends View {
        interface OnColorChangedListener {
            void onColorChanged(int color);
        }

        private static final int[] HUES = {
                0xFFFF0000, 0xFFFFFF00, 0xFF00FF00,
                0xFF00FFFF, 0xFF0000FF, 0xFFFF00FF, 0xFFFF0000
        };

        private final Paint mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint mSelectorPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint mDimPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
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
            mSelectorPaint.setStyle(Paint.Style.STROKE);
            mSelectorPaint.setStrokeWidth(3 * context.getResources().getDisplayMetrics().density);
            mSelectorPaint.setColor(Color.WHITE);
        }

        void setOnColorChangedListener(OnColorChangedListener listener) {
            mListener = listener;
        }

        void setValue(float value) {
            mValue = Math.max(0f, Math.min(1f, value));
            invalidate();
        }

        int getColor() {
            float effValue;
            if (mValue >= 1f) {
                effValue = 1f;
            } else {
                effValue = (127f + 128f * mValue) / 255f;
            }
            return Color.HSVToColor(new float[]{mHue, mSat, effValue});
        }

        void setColor(int color) {
            float[] hsv = new float[3];
            Color.colorToHSV(color, hsv);
            mHue = hsv[0];
            mSat = hsv[1];
            mValue = hsv[2];
            invalidate();
        }

        @Override
        protected void onSizeChanged(int w, int h, int oldw, int oldh) {
            super.onSizeChanged(w, h, oldw, oldh);
            mRadius = Math.min(w, h) / 2f - 2;
            mCenterX = w / 2f;
            mCenterY = h / 2f;
            if (mRadius <= 0) {
                mWheelBitmap = null;
                return;
            }
            int size = Math.round(mRadius * 2);
            mWheelBitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(mWheelBitmap);
            float cx = size / 2f;
            float cy = size / 2f;
            Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
            paint.setShader(new SweepGradient(cx, cy, HUES, null));
            canvas.drawCircle(cx, cy, mRadius, paint);
            paint.setShader(new RadialGradient(cx, cy, mRadius,
                    new int[]{0xFFFFFFFF, 0x00FFFFFF},
                    new float[]{0f, 1f}, Shader.TileMode.CLAMP));
            canvas.drawCircle(cx, cy, mRadius, paint);
            paint.setShader(null);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            mPaint.setColor(Color.WHITE);
            mPaint.setAlpha(255);
            mPaint.setStyle(Paint.Style.FILL);
            mPaint.setShader(null);
            if (mWheelBitmap != null) {
                canvas.drawBitmap(mWheelBitmap, mCenterX - mRadius, mCenterY - mRadius, mPaint);
            }
            if (mValue < 1f) {
                int dim = Math.round((1f - mValue) * 128f);
                if (dim > 0) {
                    mDimPaint.setColor(Color.argb(dim, 0, 0, 0));
                    mDimPaint.setStyle(Paint.Style.FILL);
                    canvas.drawCircle(mCenterX, mCenterY, mRadius, mDimPaint);
                }
            }
            double angle = Math.toRadians(mHue);
            float selectorRadius = mSat * mRadius;
            float sx = mCenterX + (float) (Math.cos(angle) * selectorRadius);
            float sy = mCenterY + (float) (Math.sin(angle) * selectorRadius);
            float selectorDensity = getResources().getDisplayMetrics().density;
            mSelectorPaint.setStyle(Paint.Style.STROKE);
            mSelectorPaint.setStrokeWidth(2 * selectorDensity);
            mSelectorPaint.setColor(0xCC000000);
            canvas.drawCircle(sx, sy, 13 * selectorDensity, mSelectorPaint);
            mSelectorPaint.setStrokeWidth(3 * selectorDensity);
            mSelectorPaint.setColor(Color.WHITE);
            canvas.drawCircle(sx, sy, 10 * selectorDensity, mSelectorPaint);
        }

        @Override
        public boolean onTouchEvent(MotionEvent event) {
            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                case MotionEvent.ACTION_MOVE: {
                    float dx = event.getX() - mCenterX;
                    float dy = event.getY() - mCenterY;
                    float dist = (float) Math.sqrt(dx * dx + dy * dy);
                    if (dist > mRadius) {
                        dx = dx / dist * mRadius;
                        dy = dy / dist * mRadius;
                        mSat = 1f;
                    } else {
                        mSat = dist / mRadius;
                    }
                    mHue = (float) ((Math.atan2(dy, dx) * 180.0 / Math.PI) + 360.0) % 360f;
                    if (mListener != null) {
                        mListener.onColorChanged(getColor());
                    }
                    invalidate();
                    return true;
                }
                default:
                    return super.onTouchEvent(event);
            }
        }
    }
}
