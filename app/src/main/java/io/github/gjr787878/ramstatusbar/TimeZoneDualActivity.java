package io.github.gjr787878.ramstatusbar;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

/**
 * 时区Ⅰ｜时区Ⅱ 二级界面（§3.7 深色主题、自绘返回）
 * 分别选择两个时区，列表包含主要国家的首都和主要城市，按钮右侧实时显示当前时间。
 */
public class TimeZoneDualActivity extends Activity {

    private static final String UI_PREFS_NAME = "ui_prefs";
    private static final String KEY_LANGUAGE = "language";
    private static final String KEY_TZ1_ID = "tz1_id";
    private static final String KEY_TZ2_ID = "tz2_id";
    private static final String LANG_ZH = "zh";
    private static final String LANG_EN = "en";
    private static final String LANG_RU = "ru";

    private static final int COLOR_ACCENT = 0xFF0A84FF;
    private static final int COLOR_WHITE = 0xFFFFFFFF;

    // 主要城市时区列表 [中文, 英文, 俄文, 时区ID]（与 TimeSettingsActivity 一致）
    private static final String[][] CITY_TIMEZONES = {
            {"北京 / 上海", "Beijing / Shanghai", "Пекин / Шанхай", "Asia/Shanghai"},
            {"香港", "Hong Kong", "Гонконг", "Asia/Hong_Kong"},
            {"台北", "Taipei", "Тайбэй", "Asia/Taipei"},
            {"东京", "Tokyo", "Токио", "Asia/Tokyo"},
            {"首尔", "Seoul", "Сеул", "Asia/Seoul"},
            {"新加坡", "Singapore", "Сингапур", "Asia/Singapore"},
            {"曼谷", "Bangkok", "Бангкок", "Asia/Bangkok"},
            {"雅加达", "Jakarta", "Джакарта", "Asia/Jakarta"},
            {"马尼拉", "Manila", "Манила", "Asia/Manila"},
            {"吉隆坡", "Kuala Lumpur", "Куала-Лумпур", "Asia/Kuala_Lumpur"},
            {"新德里", "New Delhi", "Нью-Дели", "Asia/Kolkata"},
            {"迪拜", "Dubai", "Дубай", "Asia/Dubai"},
            {"德黑兰", "Tehran", "Тегеран", "Asia/Tehran"},
            {"伊斯坦布尔", "Istanbul", "Стамбул", "Europe/Istanbul"},
            {"莫斯科", "Moscow", "Москва", "Europe/Moscow"},
            {"伦敦", "London", "Лондон", "Europe/London"},
            {"巴黎", "Paris", "Париж", "Europe/Paris"},
            {"柏林", "Berlin", "Берлин", "Europe/Berlin"},
            {"罗马", "Rome", "Рим", "Europe/Rome"},
            {"马德里", "Madrid", "Мадрид", "Europe/Madrid"},
            {"阿姆斯特丹", "Amsterdam", "Амстердам", "Europe/Amsterdam"},
            {"斯德哥尔摩", "Stockholm", "Стокгольм", "Europe/Stockholm"},
            {"苏黎世", "Zurich", "Цюрих", "Europe/Zurich"},
            {"维也纳", "Vienna", "Вена", "Europe/Vienna"},
            {"华沙", "Warsaw", "Варшава", "Europe/Warsaw"},
            {"雅典", "Athens", "Афины", "Europe/Athens"},
            {"开罗", "Cairo", "Каир", "Africa/Cairo"},
            {"约翰内斯堡", "Johannesburg", "Йоханнесбург", "Africa/Johannesburg"},
            {"拉各斯", "Lagos", "Лагос", "Africa/Lagos"},
            {"内罗毕", "Nairobi", "Найроби", "Africa/Nairobi"},
            {"纽约", "New York", "Нью-Йорк", "America/New_York"},
            {"洛杉矶", "Los Angeles", "Лос-Анджелес", "America/Los_Angeles"},
            {"芝加哥", "Chicago", "Чикаго", "America/Chicago"},
            {"休斯顿", "Houston", "Хьюстон", "America/Chicago"},
            {"凤凰城", "Phoenix", "Феникс", "America/Phoenix"},
            {"丹佛", "Denver", "Денвер", "America/Denver"},
            {"西雅图", "Seattle", "Сиэтл", "America/Los_Angeles"},
            {"波士顿", "Boston", "Бостон", "America/New_York"},
            {"迈阿密", "Miami", "Майами", "America/New_York"},
            {"亚特兰大", "Atlanta", "Атланта", "America/New_York"},
            {"达拉斯", "Dallas", "Даллас", "America/Chicago"},
            {"旧金山", "San Francisco", "Сан-Франциско", "America/Los_Angeles"},
            {"华盛顿", "Washington", "Вашингтон", "America/New_York"},
            {"多伦多", "Toronto", "Торонто", "America/Toronto"},
            {"温哥华", "Vancouver", "Ванкувер", "America/Vancouver"},
            {"蒙特利尔", "Montreal", "Монреаль", "America/Toronto"},
            {"墨西哥城", "Mexico City", "Мехико", "America/Mexico_City"},
            {"圣保罗", "Sao Paulo", "Сан-Паулу", "America/Sao_Paulo"},
            {"里约热内卢", "Rio de Janeiro", "Рио-де-Жанейро", "America/Sao_Paulo"},
            {"布宜诺斯艾利斯", "Buenos Aires", "Буэнос-Айрес", "America/Argentina/Buenos_Aires"},
            {"圣地亚哥", "Santiago", "Сантьяго", "America/Santiago"},
            {"利马", "Lima", "Лима", "America/Lima"},
            {"波哥大", "Bogota", "Богота", "America/Bogota"},
            {"悉尼", "Sydney", "Сидней", "Australia/Sydney"},
            {"墨尔本", "Melbourne", "Мельбурн", "Australia/Melbourne"},
            {"布里斯班", "Brisbane", "Брисбен", "Australia/Brisbane"},
            {"珀斯", "Perth", "Перт", "Australia/Perth"},
            {"奥克兰", "Auckland", "Окленд", "Pacific/Auckland"},
            {"惠灵顿", "Wellington", "Веллингтон", "Pacific/Auckland"},
            {"斐济", "Fiji", "Фиджи", "Pacific/Fiji"},
            {"夏威夷", "Hawaii", "Гавайи", "Pacific/Honolulu"},
            {"阿拉斯加", "Alaska", "Аляска", "America/Anchorage"},
    };

    private String mLanguage = LANG_ZH;
    private String mTz1Id = "Asia/Shanghai";
    private String mTz2Id = "Europe/London";

    private TextView mTz1Status;
    private TextView mTz2Status;
    private LinearLayout mTz1Button;
    private LinearLayout mTz2Button;

    private final Handler mHandler = new Handler(Looper.getMainLooper());
    private Runnable mClockUpdater;

    private String lang(String zh, String en, String ru) {
        if (LANG_RU.equals(mLanguage)) return ru;
        if (LANG_EN.equals(mLanguage)) return en;
        return zh;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SharedPreferences uiPrefs = getSharedPreferences(UI_PREFS_NAME, MODE_PRIVATE);
        mLanguage = uiPrefs.getString(KEY_LANGUAGE, LANG_ZH);
        mTz1Id = uiPrefs.getString(KEY_TZ1_ID, "Asia/Shanghai");
        mTz2Id = uiPrefs.getString(KEY_TZ2_ID, "Europe/London");

        float density = getResources().getDisplayMetrics().density;
        boolean tablet = getResources().getConfiguration().smallestScreenWidthDp >= 600;
        int screenWidthPx = getResources().getDisplayMetrics().widthPixels;
        int sidePad = Math.round(24 * density);
        if (tablet) {
            int cap = Math.round(760 * density);
            sidePad = Math.max(sidePad, (screenWidthPx - cap) / 2);
        }

        getWindow().setBackgroundDrawableResource(android.R.color.black);

        ScrollView scrollView = new ScrollView(this);
        scrollView.setBackgroundColor(Color.BLACK);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(sidePad, Math.round(48 * density), sidePad, Math.round(32 * density));
        root.setBackgroundColor(Color.BLACK);

        // ===== 顶部栏：返回箭头 + 标题 =====
        LinearLayout topBar = new LinearLayout(this);
        topBar.setOrientation(LinearLayout.HORIZONTAL);
        topBar.setGravity(Gravity.CENTER_VERTICAL);

        TextView backArrow = new TextView(this);
        backArrow.setText("←");
        backArrow.setTextSize(24);
        backArrow.setTextColor(COLOR_WHITE);
        backArrow.setPadding(0, 0, Math.round(16 * density), 0);
        backArrow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        topBar.addView(backArrow, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));

        TextView title = new TextView(this);
        title.setTextSize(20);
        title.setTextColor(COLOR_WHITE);
        title.setText(lang("时区设置", "Time Zones", "Часовые пояса"));
        topBar.addView(title, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        root.addView(topBar, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));

        TextView description = new TextView(this);
        description.setTextSize(14);
        description.setTextColor(0xFFCCCCCC);
        description.setPadding(0, Math.round(16 * density), 0, Math.round(32 * density));
        description.setText(lang(
                "分别选择两个时区，覆盖主要国家的首都和主要城市。",
                "Pick two time zones, covering capitals and major cities.",
                "Выберите два часовых пояса: столицы и крупные города."));
        root.addView(description);

        // ===== 时区Ⅰ =====
        mTz1Button = buildTimeZoneRow(root, density,
                lang("时区Ⅰ", "Time Zone I", "Пояс I"),
                "tz1");
        // ===== 时区Ⅱ =====
        mTz2Button = buildTimeZoneRow(root, density,
                lang("时区Ⅱ", "Time Zone II", "Пояс II"),
                "tz2");

        // ===== 说明文字 =====
        TextView note = new TextView(this);
        note.setTextSize(13);
        note.setTextColor(0xFF888888);
        note.setPadding(0, Math.round(32 * density), 0, 0);
        note.setText(lang(
                "按钮右侧显示两个时区当前的本地时间，每分钟自动刷新。",
                "The right side shows the current local time of both zones, refreshed every minute.",
                "Справа отображается текущее местное время обоих поясов, обновление раз в минуту."));
        root.addView(note);

        // ===== 返回按钮 =====
        LinearLayout backButton = new LinearLayout(this);
        backButton.setBackground(createGlassButtonBg(density));
        backButton.setPadding(
                Math.round(24 * density), Math.round(14 * density),
                Math.round(24 * density), Math.round(14 * density));
        backButton.setGravity(Gravity.CENTER);
        TextView backText = new TextView(this);
        backText.setText(lang("返回", "Back", "Назад"));
        backText.setTextSize(14);
        backText.setTextColor(COLOR_WHITE);
        backButton.addView(backText);
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        LinearLayout.LayoutParams backParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        backParams.topMargin = Math.round(48 * density);
        root.addView(backButton, backParams);

        scrollView.addView(root, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        setContentView(scrollView);
    }

    /** 构建一行时区胶囊按钮：标题 + 右侧状态（城市 + 偏移 + 当前时间） */
    private LinearLayout buildTimeZoneRow(LinearLayout root, float density,
                                          String labelText, final String slot) {
        TextView label = new TextView(this);
        label.setTextSize(15);
        label.setTextColor(COLOR_WHITE);
        label.setPadding(0, Math.round(24 * density), 0, Math.round(12 * density));
        label.setText(labelText);
        root.addView(label, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));

        final LinearLayout button = new LinearLayout(this);
        button.setBackground(createGlassButtonBg(density));
        button.setPadding(
                Math.round(24 * density), Math.round(14 * density),
                Math.round(24 * density), Math.round(14 * density));
        button.setGravity(Gravity.CENTER_VERTICAL);

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);

        TextView buttonTitle = new TextView(this);
        buttonTitle.setTextSize(14);
        buttonTitle.setTextColor(COLOR_WHITE);
        buttonTitle.setText(labelText);
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        row.addView(buttonTitle, titleParams);

        final TextView status = new TextView(this);
        status.setTextSize(13);
        status.setTextColor(COLOR_ACCENT);
        status.setGravity(Gravity.CENTER);
        row.addView(status, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));

        button.addView(row, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));

        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showZonePicker(slot);
            }
        });
        root.addView(button, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));

        if ("tz1".equals(slot)) {
            mTz1Status = status;
            mTz1Button = button;
        } else {
            mTz2Status = status;
            mTz2Button = button;
        }
        return button;
    }

    private void showZonePicker(final String slot) {
        final float density = getResources().getDisplayMetrics().density;
        final List<String> cityNames = new ArrayList<>();
        final List<String> zoneIds = new ArrayList<>();
        for (String[] entry : CITY_TIMEZONES) {
            TimeZone tz = TimeZone.getTimeZone(entry[3]);
            String cityName;
            if (LANG_EN.equals(mLanguage)) cityName = entry[1];
            else if (LANG_RU.equals(mLanguage)) cityName = entry[2];
            else cityName = entry[0];
            cityNames.add(cityName + "  (" + formatTimeZoneOffset(tz) + ")");
            zoneIds.add(entry[3]);
        }

        String current = "tz1".equals(slot) ? mTz1Id : mTz2Id;
        int selectedIndex = 0;
        for (int i = 0; i < zoneIds.size(); i++) {
            if (zoneIds.get(i).equals(current)) {
                selectedIndex = i;
                break;
            }
        }

        final int finalSelectedIndex = selectedIndex;
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("tz1".equals(slot)
                ? lang("选择时区Ⅰ", "Pick Zone I", "Выбор пояса I")
                : lang("选择时区Ⅱ", "Pick Zone II", "Выбор пояса II"));
        builder.setSingleChoiceItems(cityNames.toArray(new String[0]),
                finalSelectedIndex,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String zoneId = zoneIds.get(which);
                        getSharedPreferences(UI_PREFS_NAME, MODE_PRIVATE)
                                .edit().putString("tz1".equals(slot) ? KEY_TZ1_ID : KEY_TZ2_ID, zoneId).apply();
                        if ("tz1".equals(slot)) {
                            mTz1Id = zoneId;
                        } else {
                            mTz2Id = zoneId;
                        }
                        updateStatus();
                        Toast.makeText(TimeZoneDualActivity.this,
                                lang("时区已更新", "Time zone updated", "Пояс обновлён"),
                                Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                    }
                });
        builder.setNegativeButton(lang("取消", "Cancel", "Отмена"), null);
        builder.show();
    }

    private void updateStatus() {
        if (mTz1Status != null) {
            mTz1Status.setText(cityTimeLine(mTz1Id));
        }
        if (mTz2Status != null) {
            mTz2Status.setText(cityTimeLine(mTz2Id));
        }
    }

    /** 状态文本：城市 (UTC偏移) + 当前时间 HH:mm */
    private String cityTimeLine(String zoneId) {
        TimeZone tz = TimeZone.getTimeZone(zoneId);
        String city = getCityNameByTimeZone(zoneId);
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
        sdf.setTimeZone(tz);
        return city + " (" + formatTimeZoneOffset(tz) + ")  " + sdf.format(new Date());
    }

    private String getCityNameByTimeZone(String zoneId) {
        for (String[] entry : CITY_TIMEZONES) {
            if (entry[3].equals(zoneId)) {
                if (LANG_EN.equals(mLanguage)) return entry[1];
                if (LANG_RU.equals(mLanguage)) return entry[2];
                return entry[0];
            }
        }
        return zoneId;
    }

    private String formatTimeZoneOffset(TimeZone tz) {
        int offsetMillis = tz.getRawOffset();
        int hours = Math.abs(offsetMillis) / 3600000;
        int minutes = (Math.abs(offsetMillis) % 3600000) / 60000;
        String sign = offsetMillis >= 0 ? "+" : "-";
        if (minutes > 0) {
            return String.format(Locale.US, "UTC%s%02d:%02d", sign, hours, minutes);
        }
        return String.format(Locale.US, "UTC%s%02d", sign, hours);
    }

    private GlassButtonDrawable createGlassButtonBg(float density) {
        return new GlassButtonDrawable(Math.round(28 * density), Math.round(1 * density), false);
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateStatus();
        stopClock();
        mClockUpdater = new Runnable() {
            @Override
            public void run() {
                updateStatus();
                mHandler.postDelayed(this, 60000);
            }
        };
        mHandler.postDelayed(mClockUpdater, 60000);
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopClock();
    }

    private void stopClock() {
        if (mClockUpdater != null) {
            mHandler.removeCallbacks(mClockUpdater);
            mClockUpdater = null;
        }
    }
}
