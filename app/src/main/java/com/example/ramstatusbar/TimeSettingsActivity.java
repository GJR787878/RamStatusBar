package com.example.ramstatusbar;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

public class TimeSettingsActivity extends Activity {

    private static final String PREFS_NAME = "time_prefs";
    private static final String KEY_AUTO_SYNC = "auto_sync";
    private static final String KEY_TIME_ZONE = "time_zone";
    private static final String KEY_CUSTOM_TIME = "custom_time";
    // 时间配置文件，MainHook 读取此文件来决定状态栏显示的时间
    private static final String TIME_CONFIG_FILE = "/data/local/tmp/ramstatusbar_time";

    private static final String UI_PREFS_NAME = "ui_prefs";
    private static final String KEY_LANGUAGE = "language";
    private static final String LANG_ZH = "zh";
    private static final String LANG_EN = "en";
    private static final String LANG_RU = "ru";

    private static final int COLOR_WHITE = 0xFFFFFFFF;
    private static final int COLOR_ACCENT = 0xFF0A84FF;
    private static final int COLOR_NAV_BG = 0xB31C1C1E;
    private static final int COLOR_NAV_BORDER = 0x40FFFFFF;

    private String mLanguage = LANG_ZH;
    private boolean mAutoSync = true;
    private String mTimeZoneId = TimeZone.getDefault().getID();
    private long mCustomTime = 0;
    // NTP同步时间基准
    private long mSyncTimeBase = 0;
    private long mSyncElapsedRealtime = 0;

    private TextView mAutoSyncStatus;
    private TextView mTimeZoneStatus;
    private TextView mCustomTimeStatus;

    // 主要城市时区列表
    private static final String[][] CITY_TIMEZONES = {
            {"北京 / 上海", "Asia/Shanghai"},
            {"香港", "Asia/Hong_Kong"},
            {"台北", "Asia/Taipei"},
            {"东京", "Asia/Tokyo"},
            {"首尔", "Asia/Seoul"},
            {"新加坡", "Asia/Singapore"},
            {"曼谷", "Asia/Bangkok"},
            {"雅加达", "Asia/Jakarta"},
            {"马尼拉", "Asia/Manila"},
            {"吉隆坡", "Asia/Kuala_Lumpur"},
            {"新德里", "Asia/Kolkata"},
            {"迪拜", "Asia/Dubai"},
            {"德黑兰", "Asia/Tehran"},
            {"伊斯坦布尔", "Europe/Istanbul"},
            {"莫斯科", "Europe/Moscow"},
            {"伦敦", "Europe/London"},
            {"巴黎", "Europe/Paris"},
            {"柏林", "Europe/Berlin"},
            {"罗马", "Europe/Rome"},
            {"马德里", "Europe/Madrid"},
            {"阿姆斯特丹", "Europe/Amsterdam"},
            {"斯德哥尔摩", "Europe/Stockholm"},
            {"苏黎世", "Europe/Zurich"},
            {"维也纳", "Europe/Vienna"},
            {"华沙", "Europe/Warsaw"},
            {"雅典", "Europe/Athens"},
            {"开罗", "Africa/Cairo"},
            {"约翰内斯堡", "Africa/Johannesburg"},
            {"拉各斯", "Africa/Lagos"},
            {"内罗毕", "Africa/Nairobi"},
            {"纽约", "America/New_York"},
            {"洛杉矶", "America/Los_Angeles"},
            {"芝加哥", "America/Chicago"},
            {"休斯顿", "America/Chicago"},
            {"凤凰城", "America/Phoenix"},
            {"丹佛", "America/Denver"},
            {"西雅图", "America/Los_Angeles"},
            {"波士顿", "America/New_York"},
            {"迈阿密", "America/New_York"},
            {"亚特兰大", "America/New_York"},
            {"达拉斯", "America/Chicago"},
            {"旧金山", "America/Los_Angeles"},
            {"华盛顿", "America/New_York"},
            {"多伦多", "America/Toronto"},
            {"温哥华", "America/Vancouver"},
            {"蒙特利尔", "America/Toronto"},
            {"墨西哥城", "America/Mexico_City"},
            {"圣保罗", "America/Sao_Paulo"},
            {"里约热内卢", "America/Sao_Paulo"},
            {"布宜诺斯艾利斯", "America/Argentina/Buenos_Aires"},
            {"圣地亚哥", "America/Santiago"},
            {"利马", "America/Lima"},
            {"波哥大", "America/Bogota"},
            {"悉尼", "Australia/Sydney"},
            {"墨尔本", "Australia/Melbourne"},
            {"布里斯班", "Australia/Brisbane"},
            {"珀斯", "Australia/Perth"},
            {"奥克兰", "Pacific/Auckland"},
            {"惠灵顿", "Pacific/Auckland"},
            {"斐济", "Pacific/Fiji"},
            {"夏威夷", "Pacific/Honolulu"},
            {"阿拉斯加", "America/Anchorage"},
    };

    private String lang(String zh, String en, String ru) {
        if (LANG_RU.equals(mLanguage)) return ru;
        if (LANG_EN.equals(mLanguage)) return en;
        return zh;
    }

    // 把时间设置写入配置文件，MainHook 会读取此文件
    private void saveTimeConfig() {
        try {
            String json = "{\"autoSync\":" + mAutoSync
                    + ",\"timeZone\":\"" + mTimeZoneId + "\""
                    + ",\"customTime\":" + mCustomTime
                    + ",\"syncTimeBase\":" + mSyncTimeBase
                    + ",\"syncElapsedRealtime\":" + mSyncElapsedRealtime + "}";
            java.io.FileWriter writer = new java.io.FileWriter(TIME_CONFIG_FILE);
            writer.write(json);
            writer.close();
        } catch (Throwable t) {
            // 写入失败时静默处理，SharedPreferences 仍会保存设置
        }
    }

    // 通过NTP协议获取网络准确时间（UTC毫秒）
    private long getNtpTime() {
        String[] ntpServers = {"pool.ntp.org", "time.google.com", "time.windows.com", "ntp.aliyun.com"};
        for (String server : ntpServers) {
            try {
                java.net.DatagramSocket socket = new java.net.DatagramSocket();
                socket.setSoTimeout(3000);
                byte[] buffer = new byte[48];
                buffer[0] = 0x1B; // LI=0, VN=3, Mode=3 (client)
                java.net.InetAddress address = java.net.InetAddress.getByName(server);
                java.net.DatagramPacket request = new java.net.DatagramPacket(buffer, buffer.length, address, 123);
                socket.send(request);
                java.net.DatagramPacket response = new java.net.DatagramPacket(buffer, buffer.length);
                socket.receive(response);
                socket.close();

                // NTP时间戳从第40字节开始，64位固定点数（高32位秒，低32位分数）
                long seconds = 0;
                long fraction = 0;
                for (int i = 40; i < 44; i++) {
                    seconds = (seconds << 8) | (buffer[i] & 0xFF);
                }
                for (int i = 44; i < 48; i++) {
                    fraction = (fraction << 8) | (buffer[i] & 0xFF);
                }
                // NTP时间从1900年开始，Unix时间从1970年开始，相差2208988800秒
                long unixSeconds = seconds - 2208988800L;
                long unixMillis = unixSeconds * 1000 + (fraction * 1000) / 0x100000000L;
                if (unixMillis > 0) {
                    return unixMillis;
                }
            } catch (Exception e) {
                // 尝试下一个NTP服务器
            }
        }
        return -1;
    }

    // 在后台线程执行NTP同步
    private void syncNtpTime() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                final long ntpTime = getNtpTime();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (ntpTime > 0) {
                            mSyncTimeBase = ntpTime;
                            mSyncElapsedRealtime = android.os.SystemClock.elapsedRealtime();
                            getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit()
                                    .putLong("sync_time_base", mSyncTimeBase)
                                    .putLong("sync_elapsed_realtime", mSyncElapsedRealtime).apply();
                            saveTimeConfig();
                            Toast.makeText(TimeSettingsActivity.this,
                                    lang("时间同步成功", "Time synced", "Время синхронизировано"),
                                    Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(TimeSettingsActivity.this,
                                    lang("同步失败，使用系统时间", "Sync failed, using system time", "Синхр. не удалась"),
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }
        }).start();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SharedPreferences uiPrefs = getSharedPreferences(UI_PREFS_NAME, MODE_PRIVATE);
        mLanguage = uiPrefs.getString(KEY_LANGUAGE, LANG_ZH);

        SharedPreferences timePrefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        mAutoSync = timePrefs.getBoolean(KEY_AUTO_SYNC, true);
        mTimeZoneId = timePrefs.getString(KEY_TIME_ZONE, TimeZone.getDefault().getID());
        mCustomTime = timePrefs.getLong(KEY_CUSTOM_TIME, 0);

        float density = getResources().getDisplayMetrics().density;

        // 设置窗口背景为不透明黑色，防止透看到上一个页面
        getWindow().setBackgroundDrawableResource(android.R.color.black);

        ScrollView scrollView = new ScrollView(this);
        scrollView.setBackgroundColor(0xFF000000);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(
                Math.round(24 * density), Math.round(48 * density),
                Math.round(24 * density), Math.round(32 * density));
        root.setBackgroundColor(0xFF000000);

        // 顶部栏：返回箭头 + 标题
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
        title.setText(lang("时间设置", "Time Settings", "Настройки времени"));
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
                "配置时间同步、时区和自定义时间。",
                "Configure time sync, time zone and custom time.",
                "Настройте синхронизацию времени, часовой пояс и пользовательское время."));
        root.addView(description);

        // ===== 自动同步 =====
        TextView autoSyncLabel = new TextView(this);
        autoSyncLabel.setTextSize(15);
        autoSyncLabel.setTextColor(COLOR_WHITE);
        autoSyncLabel.setPadding(0, Math.round(16 * density), 0, Math.round(12 * density));
        autoSyncLabel.setText(lang("自动同步", "Auto Sync", "Автосинхронизация"));
        root.addView(autoSyncLabel);

        LinearLayout autoSyncButton = new LinearLayout(this);
        autoSyncButton.setBackground(createGlassButtonBg(density));
        autoSyncButton.setPadding(
                Math.round(24 * density), Math.round(14 * density),
                Math.round(24 * density), Math.round(14 * density));
        autoSyncButton.setGravity(Gravity.CENTER_VERTICAL);

        LinearLayout autoSyncLayout = new LinearLayout(this);
        autoSyncLayout.setOrientation(LinearLayout.HORIZONTAL);
        autoSyncLayout.setGravity(Gravity.CENTER_VERTICAL);

        TextView autoSyncTitle = new TextView(this);
        autoSyncTitle.setText(lang("自动同步网络时间", "Auto sync network time", "Автосинхр. сетевого времени"));
        autoSyncTitle.setTextSize(14);
        autoSyncTitle.setTextColor(COLOR_WHITE);
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        autoSyncLayout.addView(autoSyncTitle, titleParams);

        mAutoSyncStatus = new TextView(this);
        mAutoSyncStatus.setTextSize(14);
        mAutoSyncStatus.setGravity(Gravity.CENTER);
        updateAutoSyncStatus();
        autoSyncLayout.addView(mAutoSyncStatus, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));

        autoSyncButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mAutoSync = !mAutoSync;
                getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                        .edit().putBoolean(KEY_AUTO_SYNC, mAutoSync).apply();
                updateAutoSyncStatus();
                updateButtonStyle(autoSyncButton, mAutoSync, density);
                if (mAutoSync) {
                    // 开启自动同步时，通过NTP获取当前选择时区的准确时间
                    Toast.makeText(TimeSettingsActivity.this,
                            lang("正在同步时间...", "Syncing time...", "Синхронизация времени..."),
                            Toast.LENGTH_SHORT).show();
                    syncNtpTime();
                } else {
                    saveTimeConfig();
                    Toast.makeText(TimeSettingsActivity.this,
                            lang("已关闭自动同步", "Auto sync disabled", "Автосинхр. выключена"),
                            Toast.LENGTH_SHORT).show();
                }
            }
        });
        updateButtonStyle(autoSyncButton, mAutoSync, density);

        // 把布局放进按钮
        autoSyncButton.addView(autoSyncLayout, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        root.addView(autoSyncButton, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));

        // ===== 选择时区 =====
        TextView timeZoneLabel = new TextView(this);
        timeZoneLabel.setTextSize(15);
        timeZoneLabel.setTextColor(COLOR_WHITE);
        timeZoneLabel.setPadding(0, Math.round(48 * density), 0, Math.round(12 * density));
        timeZoneLabel.setText(lang("选择时区", "Select Time Zone", "Выбор часового пояса"));
        root.addView(timeZoneLabel);

        LinearLayout timeZoneButton = new LinearLayout(this);
        timeZoneButton.setBackground(createGlassButtonBg(density));
        timeZoneButton.setPadding(
                Math.round(24 * density), Math.round(14 * density),
                Math.round(24 * density), Math.round(14 * density));
        timeZoneButton.setGravity(Gravity.CENTER_VERTICAL);

        LinearLayout timeZoneLayout = new LinearLayout(this);
        timeZoneLayout.setOrientation(LinearLayout.HORIZONTAL);
        timeZoneLayout.setGravity(Gravity.CENTER_VERTICAL);

        TextView timeZoneTitle = new TextView(this);
        timeZoneTitle.setText(lang("选择时区", "Select time zone", "Выбрать часовой пояс"));
        timeZoneTitle.setTextSize(14);
        timeZoneTitle.setTextColor(COLOR_WHITE);
        timeZoneLayout.addView(timeZoneTitle, titleParams);

        mTimeZoneStatus = new TextView(this);
        mTimeZoneStatus.setTextSize(13);
        mTimeZoneStatus.setTextColor(0xFFAAAAAA);
        mTimeZoneStatus.setGravity(Gravity.CENTER);
        updateTimeZoneStatus();
        timeZoneLayout.addView(mTimeZoneStatus, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));

        timeZoneButton.addView(timeZoneLayout, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));

        timeZoneButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showTimeZonePicker();
            }
        });
        root.addView(timeZoneButton, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));

        // ===== 自定义时间 =====
        TextView customTimeLabel = new TextView(this);
        customTimeLabel.setTextSize(15);
        customTimeLabel.setTextColor(COLOR_WHITE);
        customTimeLabel.setPadding(0, Math.round(48 * density), 0, Math.round(12 * density));
        customTimeLabel.setText(lang("自定义时间", "Custom Time", "Пользовательское время"));
        root.addView(customTimeLabel);

        LinearLayout customTimeButton = new LinearLayout(this);
        customTimeButton.setBackground(createGlassButtonBg(density));
        customTimeButton.setPadding(
                Math.round(24 * density), Math.round(14 * density),
                Math.round(24 * density), Math.round(14 * density));
        customTimeButton.setGravity(Gravity.CENTER_VERTICAL);

        LinearLayout customTimeLayout = new LinearLayout(this);
        customTimeLayout.setOrientation(LinearLayout.HORIZONTAL);
        customTimeLayout.setGravity(Gravity.CENTER_VERTICAL);

        TextView customTimeTitle = new TextView(this);
        customTimeTitle.setText(lang("设置自定义时间", "Set custom time", "Задать время"));
        customTimeTitle.setTextSize(14);
        customTimeTitle.setTextColor(COLOR_WHITE);
        customTimeLayout.addView(customTimeTitle, titleParams);

        mCustomTimeStatus = new TextView(this);
        mCustomTimeStatus.setTextSize(13);
        mCustomTimeStatus.setTextColor(0xFFAAAAAA);
        mCustomTimeStatus.setGravity(Gravity.CENTER);
        updateCustomTimeStatus();
        customTimeLayout.addView(mCustomTimeStatus, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));

        customTimeButton.addView(customTimeLayout, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));

        customTimeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showCustomTimePicker();
            }
        });
        root.addView(customTimeButton, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));

        // 说明文字
        TextView noteLabel = new TextView(this);
        noteLabel.setTextSize(13);
        noteLabel.setTextColor(0xFF888888);
        noteLabel.setPadding(0, Math.round(32 * density), 0, 0);
        noteLabel.setText(lang(
                "注意：自定义时间仅在关闭自动同步后生效。修改系统时间需要 root 权限。",
                "Note: Custom time only takes effect when auto sync is off. Changing system time requires root.",
                "Примечание: пользовательское время действует только при выкл. автосинхр. Изменение системного времени требует root."));
        root.addView(noteLabel);

        // 返回按钮
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

    private GradientDrawable createGlassButtonBg(float density) {
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(COLOR_NAV_BG);
        bg.setCornerRadius(Math.round(28 * density));
        bg.setStroke(Math.round(1 * density), COLOR_NAV_BORDER);
        return bg;
    }

    private void updateButtonStyle(LinearLayout button, boolean selected, float density) {
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(COLOR_NAV_BG);
        bg.setCornerRadius(Math.round(28 * density));
        if (selected) {
            bg.setStroke(Math.round(2 * density), COLOR_WHITE);
        } else {
            bg.setStroke(Math.round(1 * density), COLOR_NAV_BORDER);
        }
        button.setBackground(bg);
    }

    private void updateAutoSyncStatus() {
        if (mAutoSyncStatus != null) {
            mAutoSyncStatus.setText(mAutoSync
                    ? lang("已开启", "ON", "ВКЛ")
                    : lang("已关闭", "OFF", "ВЫКЛ"));
            mAutoSyncStatus.setTextColor(mAutoSync ? COLOR_ACCENT : 0xFFAAAAAA);
        }
    }

    private void updateTimeZoneStatus() {
        if (mTimeZoneStatus != null) {
            TimeZone tz = TimeZone.getTimeZone(mTimeZoneId);
            String cityName = getCityNameByTimeZone(mTimeZoneId);
            String offset = formatTimeZoneOffset(tz);
            mTimeZoneStatus.setText(cityName + " (" + offset + ")");
        }
    }

    private void updateCustomTimeStatus() {
        if (mCustomTimeStatus != null) {
            if (mCustomTime > 0) {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
                sdf.setTimeZone(TimeZone.getTimeZone(mTimeZoneId));
                mCustomTimeStatus.setText(sdf.format(mCustomTime));
            } else {
                mCustomTimeStatus.setText(lang("未设置", "Not set", "Не задано"));
            }
        }
    }

    private String getCityNameByTimeZone(String zoneId) {
        for (String[] entry : CITY_TIMEZONES) {
            if (entry[1].equals(zoneId)) {
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

    private void showTimeZonePicker() {
        final float density = getResources().getDisplayMetrics().density;

        // 构建城市列表
        final List<String> cityNames = new ArrayList<>();
        final List<String> zoneIds = new ArrayList<>();
        for (String[] entry : CITY_TIMEZONES) {
            TimeZone tz = TimeZone.getTimeZone(entry[1]);
            cityNames.add(entry[0] + "  (" + formatTimeZoneOffset(tz) + ")");
            zoneIds.add(entry[1]);
        }

        // 找到当前选中的位置
        int selectedIndex = 0;
        for (int i = 0; i < zoneIds.size(); i++) {
            if (zoneIds.get(i).equals(mTimeZoneId)) {
                selectedIndex = i;
                break;
            }
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(lang("选择时区", "Select Time Zone", "Выбор часового пояса"));
        final int finalSelectedIndex = selectedIndex;
        builder.setSingleChoiceItems(
                cityNames.toArray(new String[0]),
                finalSelectedIndex,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        mTimeZoneId = zoneIds.get(which);
                        getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                                .edit().putString(KEY_TIME_ZONE, mTimeZoneId).apply();
                        saveTimeConfig();
                        updateTimeZoneStatus();
                        updateCustomTimeStatus();
                        Toast.makeText(TimeSettingsActivity.this,
                                lang("时区已更新", "Time zone updated", "Часовой пояс обновлён"),
                                Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                    }
                });
        builder.setNegativeButton(lang("取消", "Cancel", "Отмена"), null);
        builder.show();
    }

    private void showCustomTimePicker() {
        final Calendar calendar = Calendar.getInstance();
        if (mCustomTime > 0) {
            calendar.setTimeInMillis(mCustomTime);
        }
        calendar.setTimeZone(TimeZone.getTimeZone(mTimeZoneId));

        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        int minute = calendar.get(Calendar.MINUTE);

        // 先选日期
        DatePickerDialog datePicker = new DatePickerDialog(
                this,
                new DatePickerDialog.OnDateSetListener() {
                    @Override
                    public void onDateSet(android.widget.DatePicker view, int year, int month, int dayOfMonth) {
                        // 再选时间
                        TimePickerDialog timePicker = new TimePickerDialog(
                                TimeSettingsActivity.this,
                                new TimePickerDialog.OnTimeSetListener() {
                                    @Override
                                    public void onTimeSet(android.widget.TimePicker view, int hourOfDay, int minute) {
                                        Calendar cal = Calendar.getInstance();
                                        cal.setTimeZone(TimeZone.getTimeZone(mTimeZoneId));
                                        cal.set(year, month, dayOfMonth, hourOfDay, minute, 0);
                                        mCustomTime = cal.getTimeInMillis();
                                        getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                                                .edit().putLong(KEY_CUSTOM_TIME, mCustomTime).apply();
                                        saveTimeConfig();
                                        updateCustomTimeStatus();
                                        Toast.makeText(TimeSettingsActivity.this,
                                                lang("自定义时间已保存",
                                                        "Custom time saved",
                                                        "Пользовательское время сохранено"),
                                                Toast.LENGTH_SHORT).show();
                                    }
                                },
                                hour, minute, true);
                        timePicker.setTitle(lang("选择时间", "Select Time", "Выбор времени"));
                        timePicker.show();
                    }
                },
                year, month, day);
        datePicker.setTitle(lang("选择日期", "Select Date", "Выбор даты"));
        datePicker.show();
    }


    }
}
