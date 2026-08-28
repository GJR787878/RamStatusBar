package com.example.ramstatusbar;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Shader;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileReader;
import java.util.Locale;

public class MainActivity extends Activity {

    private static final String CONFIG_FILE =
            "/data/local/tmp/ramstatusbar_mode";

    private static final String COLOR_FILE =
            "/data/local/tmp/ramstatusbar_color";

    private static final int MODE_TIME_ONLY = 0;
    private static final int MODE_TIME_RAM = 1;
    private static final int MODE_RAM_ONLY = 2;

    private static final String UI_PREFS_NAME = "ui_prefs";
    private static final String KEY_LANGUAGE = "language";
    private static final String LANG_ZH = "zh";
    private static final String LANG_EN = "en";

    private static final int DEFAULT_COLOR = 0x80000000;

    private static final int LANG_BUTTON_WIDTH_DP = 72;

    private boolean mEnglish;

    private TextView mDeepSleepText;

    private final Handler mUiHandler =
            new Handler(Looper.getMainLooper());

    private Runnable mDeepSleepUpdater;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SharedPreferences uiPrefs =
                getSharedPreferences(
                        UI_PREFS_NAME,
                        MODE_PRIVATE
                );

        mEnglish =
                LANG_EN.equals(
                        uiPrefs.getString(
                                KEY_LANGUAGE,
                                LANG_ZH
                        )
                );

        showMainPage();
    }

    private void showMainPage() {

        int currentMode =
                readCurrentModeOrDefault();

        int screenWidthPx =
                getResources()
                        .getDisplayMetrics()
                        .widthPixels;

        final int contentWidthPx =
                screenWidthPx - 96;

        FrameLayout frame =
                new FrameLayout(this);

        ScrollView scrollView =
                new ScrollView(this);

        LinearLayout content =
                new LinearLayout(this);

        content.setOrientation(
                LinearLayout.VERTICAL
        );

        content.setPadding(
                48,
                96,
                48,
                220
        );

        TextView title =
                new TextView(this);

        title.setTextSize(18);

        title.setText(
                mEnglish
                        ? "RAM Status Bar Display"
                        : "RAM 状态栏显示"
        );

        content.addView(title);

        TextView intro =
                new TextView(this);

        intro.setTextSize(14);

        intro.setPadding(
                0,
                24,
                0,
                32
        );

        setBilingualText(
                intro,
                "选择下面的显示模式，最多等 1 秒即可生效，不需要重启手机。",
                "Pick a display mode below. Changes take effect within 1 second, "
                        + "no reboot needed.",
                contentWidthPx
        );

        content.addView(intro);

        final RadioGroup radioGroup =
                new RadioGroup(this);

        radioGroup.setOrientation(
                RadioGroup.VERTICAL
        );

        final RadioButton rbTimeOnly =
                new RadioButton(this);

        rbTimeOnly.setId(1001);

        rbTimeOnly.setText(
                mEnglish
                        ? "Time only"
                        : "仅显示时间"
        );

        final RadioButton rbTimeRam =
                new RadioButton(this);

        rbTimeRam.setId(1002);

        rbTimeRam.setText(
                mEnglish
                        ? "Time + RAM (e.g. 21:11 2.5G/8G)"
                        : "时间 + 内存 (如 21:11 2.5G/8G)"
        );

        final RadioButton rbRamOnly =
                new RadioButton(this);

        rbRamOnly.setId(1003);

        rbRamOnly.setText(
                mEnglish
                        ? "RAM only (e.g. 2.5G/8G)"
                        : "仅显示内存 (如 2.5G/8G)"
        );

        radioGroup.addView(rbTimeOnly);
        radioGroup.addView(rbTimeRam);
        radioGroup.addView(rbRamOnly);

        content.addView(radioGroup);

        if (currentMode == MODE_TIME_ONLY) {

            radioGroup.check(
                    rbTimeOnly.getId()
            );

        } else if (currentMode == MODE_RAM_ONLY) {

            radioGroup.check(
                    rbRamOnly.getId()
            );

        } else {

            radioGroup.check(
                    rbTimeRam.getId()
            );
        }

        radioGroup.setOnCheckedChangeListener(
                new RadioGroup.OnCheckedChangeListener() {

                    @Override
                    public void onCheckedChanged(
                            RadioGroup group,
                            int checkedId) {

                        int mode;

                        if (checkedId ==
                                rbTimeOnly.getId()) {

                            mode =
                                    MODE_TIME_ONLY;

                        } else if (checkedId ==
                                rbRamOnly.getId()) {

                            mode =
                                    MODE_RAM_ONLY;

                        } else {

                            mode =
                                    MODE_TIME_RAM;
                        }

                        boolean ok =
                                writeModeToFile(mode);

                        if (!ok) {

                            Toast.makeText(
                                    MainActivity.this,
                                    mEnglish
                                            ? "Write failed, please check root permission"
                                            : "写入失败，请检查是否已授予 root 权限",
                                    Toast.LENGTH_LONG
                            ).show();
                        }
                    }
                }
        );

        View divider =
                new View(this);

        divider.setBackgroundColor(
                0x33FFFFFF
        );

        LinearLayout.LayoutParams dividerParams =
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        2
                );

        dividerParams.topMargin = 64;
        dividerParams.bottomMargin = 24;

        content.addView(
                divider,
                dividerParams
        );

        TextView tapFeatureTitle =
                new TextView(this);

        tapFeatureTitle.setTextSize(15);

        tapFeatureTitle.setPadding(
                0,
                32,
                0,
                8
        );

        tapFeatureTitle.setText(
                mEnglish
                        ? "Tap the clock to check CPU / GPU"
                        : "点击时钟查看 CPU / GPU"
        );

        content.addView(
                tapFeatureTitle
        );

        TextView tapFeatureBody =
                new TextView(this);

        tapFeatureBody.setTextSize(13);

        setBilingualText(
                tapFeatureBody,
                "点击状态栏的时钟：第 1 次点击显示 CPU 占用率，第 2 次点击显示 "
                        + "GPU 占用率，第 3 次点击回到正常显示；10 秒内不再点击也会"
                        + "自动回到正常显示。\n\n"
                        + "GPU 占用率依赖具体芯片的私有接口，部分设备上可能会显示"
                        + "\"GPU N/A\"，能否读取取决于你的芯片型号。",
                "Tap the status bar clock: 1st tap shows CPU usage, 2nd tap shows "
                        + "GPU usage, 3rd tap returns to normal. If left untouched for "
                        + "10 seconds it automatically returns to normal as well.\n\n"
                        + "GPU usage relies on chip-specific sysfs paths and may show "
                        + "\"GPU N/A\" on some devices, depending on your chipset.",
                contentWidthPx
        );

        content.addView(
                tapFeatureBody
        );

        TextView deepSleepTitle =
                new TextView(this);

        deepSleepTitle.setTextSize(15);

        deepSleepTitle.setPadding(
                0,
                96,
                0,
                8
        );

        deepSleepTitle.setText(
                mEnglish
                        ? "Deep sleep"
                        : "深度休眠"
        );

        content.addView(
                deepSleepTitle
        );

        TextView deepSleepDesc =
                new TextView(this);

        deepSleepDesc.setTextSize(13);

        setBilingualText(
                deepSleepDesc,
                "下面显示的是开机以来设备处于深度休眠状态的时长和占比，跟"
                        + "\"关于本机\"里的数值一致，不用再去系统设置里翻找。",
                "Shown below is how much of the time since boot the device has "
                        + "spent in deep sleep, same figure as in About Phone.",
                contentWidthPx
        );

        content.addView(
                deepSleepDesc
        );

        mDeepSleepText =
                new TextView(this);

        mDeepSleepText.setTextSize(14);

        mDeepSleepText.setPadding(
                0,
                16,
                0,
                0
        );

        content.addView(
                mDeepSleepText
        );

        TextView setupTitle =
                new TextView(this);

        setupTitle.setTextSize(15);

        setupTitle.setPadding(
                0,
                96,
                0,
                8
        );

        setupTitle.setText(
                mEnglish
                        ? "Setup"
                        : "安装说明"
        );

        content.addView(
                setupTitle
        );

        TextView setupBody =
                new TextView(this);

        setupBody.setTextSize(13);

        setupBody.setText(
                mEnglish
                        ? "1. In LSPosed / Vector Manager, enable this module and "
                                + "check the scope com.android.systemui.\n"
                                + "2. Reboot once after the first install for it to take effect.\n"
                                + "3. Switching the display mode above requires root; a "
                                + "permission prompt will appear the first time.\n"
                                + "4. Total RAM is auto-detected and rounded to a common spec."
                        : "1. 到 LSPosed / Vector Manager 里，对本模块勾选作用域 "
                                + "com.android.systemui 并启用模块。\n"
                                + "2. 首次安装完成后需要重启一次手机才会生效。\n"
                                + "3. 切换上面的显示模式需要 root 权限，首次切换会弹出授权请求。\n"
                                + "4. 总内存会自动检测并取整到最接近的常见规格。"
        );

        content.addView(
                setupBody
        );

        scrollView.addView(
                content,
                new ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                )
        );

        frame.addView(
                scrollView,
                new FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                )
        );

        LinearLayout bottomButtons =
                new LinearLayout(this);

        bottomButtons.setOrientation(
                LinearLayout.HORIZONTAL
        );

        bottomButtons.setGravity(
                Gravity.CENTER_VERTICAL
        );

        Button colorButton =
                new Button(this);

        colorButton.setText(
                mEnglish
                        ? "Background color"
                        : "自定义背景颜色"
        );

        colorButton.setOnClickListener(
                new View.OnClickListener() {

                    @Override
                    public void onClick(View v) {
                        showColorPage();
                    }
                }
        );

        Button langButton =
                new Button(this);

        langButton.setText(
                mEnglish
                        ? "中文"
                        : "EN"
        );

        langButton.setOnClickListener(
                new View.OnClickListener() {

                    @Override
                    public void onClick(View v) {

                        SharedPreferences prefs =
                                getSharedPreferences(
                                        UI_PREFS_NAME,
                                        MODE_PRIVATE
                                );

                        String newLang =
                                mEnglish
                                        ? LANG_ZH
                                        : LANG_EN;

                        prefs.edit()
                                .putString(
                                        KEY_LANGUAGE,
                                        newLang
                                )
                                .apply();

                        recreate();
                    }
                }
        );

        float density =
                getResources()
                        .getDisplayMetrics()
                        .density;

        int langWidth =
                Math.round(
                        LANG_BUTTON_WIDTH_DP
                                * density
                );

        bottomButtons.addView(
                colorButton,
                new LinearLayout.LayoutParams(
                        0,
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        1
                )
        );

        bottomButtons.addView(
                langButton,
                new LinearLayout.LayoutParams(
                        langWidth,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                )
        );

        FrameLayout.LayoutParams bottomParams =
                new FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                );

        bottomParams.gravity =
                Gravity.BOTTOM;

        bottomParams.setMargins(
                32,
                0,
                32,
                32
        );

        frame.addView(
                bottomButtons,
                bottomParams
        );

        setContentView(frame);
    }

    private void showColorPage() {

        final int savedColor =
                readColorFromFile();

        final float[] hsv =
                new float[3];

        Color.colorToHSV(
                savedColor,
                hsv
        );

        final int[] alpha =
                new int[]{
                        Color.alpha(savedColor)
                };

        LinearLayout root =
                new LinearLayout(this);

        root.setOrientation(
                LinearLayout.VERTICAL
        );

        root.setPadding(
                40,
                48,
                40,
                40
        );

        TextView title =
                new TextView(this);

        title.setTextSize(20);

        title.setText(
                mEnglish
                        ? "Background Color"
                        : "背景颜色"
        );

        root.addView(title);

        Button back =
                new Button(this);

        back.setText(
                mEnglish
                        ? "← Back"
                        : "← 返回"
        );

        back.setOnClickListener(
                new View.OnClickListener() {

                    @Override
                    public void onClick(View v) {
                        showMainPage();
                    }
                }
        );

        root.addView(back);

        final ColorPickerView picker =
                new ColorPickerView(
                        this,
                        hsv
                );

        LinearLayout.LayoutParams pickerParams =
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        520
                );

        pickerParams.topMargin = 32;

        root.addView(
                picker,
                pickerParams
        );

        TextView hueTitle =
                new TextView(this);

        hueTitle.setText(
                mEnglish
                        ? "Hue"
                        : "色相"
        );

        hueTitle.setTextSize(14);

        hueTitle.setPadding(
                0,
                24,
                0,
                8
        );

        root.addView(hueTitle);

        SeekBar hueBar =
                new SeekBar(this);

        hueBar.setMax(360);

        hueBar.setProgress(
                Math.round(hsv[0])
        );

        setHueBarGradient(hueBar);

        root.addView(hueBar);

        TextView alphaTitle =
                new TextView(this);

        alphaTitle.setText(
                mEnglish
                        ? "Alpha / Transparency"
                        : "透明度 Alpha"
        );

        alphaTitle.setTextSize(14);

        alphaTitle.setPadding(
                0,
                24,
                0,
                8
        );

        root.addView(alphaTitle);

        SeekBar alphaBar =
                new SeekBar(this);

        alphaBar.setMax(255);

        alphaBar.setProgress(
                alpha[0]
        );

        root.addView(alphaBar);

        final TextView preview =
                new TextView(this);

        preview.setTextSize(15);

        preview.setGravity(
                Gravity.CENTER
        );

        LinearLayout.LayoutParams previewParams =
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        120
                );

        previewParams.topMargin = 28;

        root.addView(
                preview,
                previewParams
        );

        final TextView hexText =
                new TextView(this);

        hexText.setTextSize(15);

        hexText.setGravity(
                Gravity.CENTER
        );

        root.addView(hexText);

        View.OnClickListener unused =
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                    }
                };

        Runnable update =
                new Runnable() {

                    @Override
                    public void run() {

                        int color =
                                Color.HSVToColor(
                                        alpha[0],
                                        hsv
                                );

                        updateColorPreview(
                                preview,
                                color
                        );

                        hexText.setText(
                                String.format(
                                        Locale.US,
                                        "#%08X",
                                        color
                                )
                        );
                    }
                };

        hueBar.setOnSeekBarChangeListener(
                new SeekBar.OnSeekBarChangeListener() {

                    @Override
                    public void onProgressChanged(
                            SeekBar seekBar,
                            int progress,
                            boolean fromUser) {

                        hsv[0] =
                                progress;

                        picker.setHue(
                                hsv[0]
                        );

                        int color =
                                Color.HSVToColor(
                                        alpha[0],
                                        hsv
                                );

                        updateColorPreview(
                                preview,
                                color
                        );

                        hexText.setText(
                                String.format(
                                        Locale.US,
                                        "#%08X",
                                        color
                                )
                        );
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

        alphaBar.setOnSeekBarChangeListener(
                new SeekBar.OnSeekBarChangeListener() {

                    @Override
                    public void onProgressChanged(
                            SeekBar seekBar,
                            int progress,
                            boolean fromUser) {

                        alpha[0] =
                                progress;

                        int color =
                                Color.HSVToColor(
                                        alpha[0],
                                        hsv
                                );

                        updateColorPreview(
                                preview,
                                color
                        );

                        hexText.setText(
                                String.format(
                                        Locale.US,
                                        "#%08X",
                                        color
                                )
                        );
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

        Button apply =
                new Button(this);

        apply.setText(
                mEnglish
                        ? "Apply"
                        : "应用"
        );

        apply.setOnClickListener(
                new View.OnClickListener() {

                    @Override
                    public void onClick(View v) {

                        int color =
                                Color.HSVToColor(
                                        alpha[0],
                                        hsv
                                );

                        boolean ok =
                                writeColorToFile(
                                        color
                                );

                        if (ok) {

                            Toast.makeText(
                                    MainActivity.this,
                                    mEnglish
                                            ? "Background color saved"
                                            : "背景颜色已保存",
                                    Toast.LENGTH_SHORT
                            ).show();

                        } else {

                            Toast.makeText(
                                    MainActivity.this,
                                    mEnglish
                                            ? "Write failed, please check root permission"
                                            : "写入失败，请检查 root 权限",
                                    Toast.LENGTH_LONG
                            ).show();
                        }
                    }
                }
        );

        root.addView(apply);

        setContentView(root);

        update.run();
    }

    private void updateColorPreview(
            TextView view,
            int color) {

        GradientDrawable drawable =
                new GradientDrawable();

        drawable.setColor(color);

        drawable.setCornerRadius(
                1000
        );

        view.setBackground(
                drawable
        );

        view.setText(
                mEnglish
                        ? "Preview"
                        : "预览"
        );
    }

    private void setHueBarGradient(
            SeekBar bar) {

        bar.post(
                new Runnable() {

                    @Override
                    public void run() {

                        int width =
                                bar.getWidth();

                        if (width <= 0) {
                            return;
                        }

                        int[] colors =
                                new int[]{
                                        Color.RED,
                                        Color.YELLOW,
                                        Color.GREEN,
                                        Color.CYAN,
                                        Color.BLUE,
                                        Color.MAGENTA,
                                        Color.RED
                                };

                        LinearGradient gradient =
                                new LinearGradient(
                                        0,
                                        0,
                                        width,
                                        0,
                                        colors,
                                        null,
                                        Shader.TileMode.CLAMP
                                );

                        Paint paint =
                                new Paint();

                        paint.setShader(
                                gradient
                        );

                        android.graphics.Bitmap bitmap =
                                android.graphics.Bitmap.createBitmap(
                                        width,
                                        40,
                                        android.graphics.Bitmap.Config.ARGB_8888
                                );

                        android.graphics.Canvas canvas =
                                new android.graphics.Canvas(bitmap);

                        canvas.drawRect(
                                0,
                                0,
                                width,
                                40,
                                paint
                        );

                        android.graphics.drawable.BitmapDrawable drawable =
                                new android.graphics.drawable.BitmapDrawable(
                                        getResources(),
                                        bitmap
                                );

                        bar.setProgressDrawable(
                                drawable
                        );
                    }
                }
        );
    }

    private static class ColorPickerView
            extends View {

        private final Paint paint =
                new Paint(
                        Paint.ANTI_ALIAS_FLAG
                );

        private final float[] hsv;

        ColorPickerView(
                android.content.Context context,
                float[] initialHsv) {

            super(context);

            hsv =
                    initialHsv;

            setLayerType(
                    View.LAYER_TYPE_SOFTWARE,
                    null
            );
        }

        void setHue(float hue) {

            hsv[0] =
                    hue;

            invalidate();
        }

        @Override
        protected void onDraw(
                android.graphics.Canvas canvas) {

            super.onDraw(canvas);

            int width =
                    getWidth();

            int height =
                    getHeight();

            if (width <= 0
                    || height <= 0) {
                return;
            }

            int hueColor =
                    Color.HSVToColor(
                            new float[]{
                                    hsv[0],
                                    1f,
                                    1f
                            }
                    );

            LinearGradient saturation =
                    new LinearGradient(
                            0,
                            0,
                            width,
                            0,
                            Color.WHITE,
                            hueColor,
                            Shader.TileMode.CLAMP
                    );

            paint.setShader(
                    saturation
            );

            canvas.drawRect(
                    0,
                    0,
                    width,
                    height,
                    paint
            );

            LinearGradient brightness =
                    new LinearGradient(
                            0,
                            0,
                            0,
                            height,
                            0x00000000,
                            0xFF000000,
                            Shader.TileMode.CLAMP
                    );

            paint.setShader(
                    brightness
            );

            canvas.drawRect(
                    0,
                    0,
                    width,
                    height,
                    paint
            );

            paint.setShader(null);

            float x =
                    hsv[1] * width;

            float y =
                    (1f - hsv[2]) * height;

            paint.setStyle(
                    Paint.Style.STROKE
            );

            paint.setStrokeWidth(
                    5
            );

            paint.setColor(
                    Color.WHITE
            );

            canvas.drawCircle(
                    x,
                    y,
                    14,
                    paint
            );

            paint.setStyle(
                    Paint.Style.FILL
            );
        }

        @Override
        public boolean onTouchEvent(
                MotionEvent event) {

            if (event.getAction() ==
                    MotionEvent.ACTION_DOWN
                    || event.getAction() ==
                    MotionEvent.ACTION_MOVE
                    || event.getAction() ==
                    MotionEvent.ACTION_UP) {

                float saturation =
                        event.getX()
                                / getWidth();

                float value =
                        1f -
                                event.getY()
                                        / getHeight();

                hsv[1] =
                        Math.max(
                                0f,
                                Math.min(
                                        1f,
                                        saturation
                                )
                        );

                hsv[2] =
                        Math.max(
                                0f,
                                Math.min(
                                        1f,
                                        value
                                )
                        );

                invalidate();

                return true;
            }

            return true;
        }
    }

    @Override
    protected void onResume() {

        super.onResume();

        startDeepSleepUpdates();
    }

    @Override
    protected void onPause() {

        super.onPause();

        stopDeepSleepUpdates();
    }

    private void startDeepSleepUpdates() {

        stopDeepSleepUpdates();

        mDeepSleepUpdater =
                new Runnable() {

                    @Override
                    public void run() {

                        if (mDeepSleepText != null) {

                            mDeepSleepText.setText(
                                    formatDeepSleepLine()
                            );
                        }

                        mUiHandler.postDelayed(
                                this,
                                1000
                        );
                    }
                };

        mUiHandler.post(
                mDeepSleepUpdater
        );
    }

    private void stopDeepSleepUpdates() {

        if (mDeepSleepUpdater != null) {

            mUiHandler.removeCallbacks(
                    mDeepSleepUpdater
            );

            mDeepSleepUpdater = null;
        }
    }

    private String formatDeepSleepLine() {

        long elapsed =
                SystemClock.elapsedRealtime();

        long awake =
                SystemClock.uptimeMillis();

        long deepSleepMs =
                elapsed - awake;

        int percent =
                elapsed > 0
                        ? (int) Math.round(
                                deepSleepMs
                                        * 100.0
                                        / elapsed
                        )
                        : 0;

        long totalSeconds =
                deepSleepMs / 1000;

        long hours =
                totalSeconds / 3600;

        long minutes =
                (totalSeconds % 3600) / 60;

        long seconds =
                totalSeconds % 60;

        return String.format(
                Locale.getDefault(),
                "%d:%02d:%02d (%d%%)",
                hours,
                minutes,
                seconds,
                percent
        );
    }

    private void setBilingualText(
            TextView view,
            String zhText,
            String enText,
            int widthPx) {

        view.setText(
                mEnglish
                        ? enText
                        : zhText
        );
    }

    private int readCurrentModeOrDefault() {

        try {

            File f =
                    new File(CONFIG_FILE);

            if (!f.exists()) {
                return MODE_TIME_RAM;
            }

            BufferedReader br =
                    new BufferedReader(
                            new FileReader(f)
                    );

            String line =
                    br.readLine();

            br.close();

            if (line == null) {
                return MODE_TIME_RAM;
            }

            return Integer.parseInt(
                    line.trim()
            );

        } catch (Throwable t) {

            return MODE_TIME_RAM;
        }
    }

    private boolean writeModeToFile(
            int mode) {

        try {

            Process su =
                    Runtime.getRuntime()
                            .exec("su");

            DataOutputStream os =
                    new DataOutputStream(
                            su.getOutputStream()
                    );

            os.writeBytes(
                    "echo " +
                            mode +
                            " > " +
                            CONFIG_FILE +
                            "\n"
            );

            os.writeBytes(
                    "chmod 666 " +
                            CONFIG_FILE +
                            "\n"
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

    private int readColorFromFile() {

        try {

            File f =
                    new File(COLOR_FILE);

            if (!f.exists()) {
                return DEFAULT_COLOR;
            }

            BufferedReader br =
                    new BufferedReader(
                            new FileReader(f)
                    );

            String line =
                    br.readLine();

            br.close();

            if (line == null) {
                return DEFAULT_COLOR;
            }

            return (int)
                    Long.parseLong(
                            line.trim(),
                            16
                    );

        } catch (Throwable t) {

            return DEFAULT_COLOR;
        }
    }

    private boolean writeColorToFile(
            int color) {

        try {

            Process su =
                    Runtime.getRuntime()
                            .exec("su");

            DataOutputStream os =
                    new DataOutputStream(
                            su.getOutputStream()
                    );

            os.writeBytes(
                    "echo " +
                            String.format(
                                    Locale.US,
                                    "%08X",
                                    color
                            ) +
                            " > " +
                            COLOR_FILE +
                            "\n"
            );

            os.writeBytes(
                    "chmod 666 " +
                            COLOR_FILE +
                            "\n"
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
}
