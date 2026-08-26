// 可能是antigravity写的
// 小红书app浏览记录这里无障碍确实是获取不到
package com.screen.textgrabber;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.provider.Settings;
import android.text.TextUtils;
import android.widget.Switch;
import android.widget.Toast;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;

/**
 * 应用主界面
 * 负责提供用户界面来开启/关闭抓取功能，并引导用户开启无障碍服务权限。
 */
public class MainActivity extends AppCompatActivity {

    private Switch toggleSwitch; // 控制开关
    private SharedPreferences sp; // 用于保存开关状态
    private android.widget.TextView tvCounter; // 显示保存计数
    private android.widget.TextView tvRecentTexts; // 显示最近的文本
    private android.widget.TextView tvDbSize; // 显示数据库大小
    private android.widget.EditText etSearch; // 搜索输入框
    private android.widget.Button btnSearch; // 搜索按钮
    private android.widget.TextView tvSearchResults; // 显示搜索结果
    private android.os.Handler handler; // 用于定时更新计数
    private Runnable updateCounterRunnable; // 更新计数的任务
    private Runnable updateRecentTextsRunnable; // 更新最近文本的任务
    private Runnable updateDbSizeRunnable; // 更新数据库大小的任务
    private DBManager dbManager; // 数据库管理器

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 初始化 SharedPreferences
        sp = getSharedPreferences("app_config", MODE_PRIVATE);
        toggleSwitch = findViewById(R.id.btn_toggle);
        tvCounter = findViewById(R.id.tv_counter);
        tvRecentTexts = findViewById(R.id.tv_recent_texts);
        tvDbSize = findViewById(R.id.tv_db_size);
        etSearch = findViewById(R.id.et_search);
        btnSearch = findViewById(R.id.btn_search);
        tvSearchResults = findViewById(R.id.tv_search_results);
        
        // 初始化数据库管理器
        dbManager = new DBManager(this);

        // 初始化 Handler
        handler = new android.os.Handler();
        updateCounterRunnable = new Runnable() {
            @Override
            public void run() {
                updateCounter();
                handler.postDelayed(this, 1000); // 每 1000ms 更新一次
            }
        };
        
        updateRecentTextsRunnable = new Runnable() {
            @Override
            public void run() {
                updateRecentTexts();
                handler.postDelayed(this, 5000); // 每 5000ms 更新一次
            }
        };
        
        updateDbSizeRunnable = new Runnable() {
            @Override
            public void run() {
                updateDbSize();
                handler.postDelayed(this, 10000); // 每 10000ms 更新一次
            }
        };
        Toast.makeText(this, "TextGrabber 服务已启动", Toast.LENGTH_SHORT).show();

        // 初始化 SharedPreferences
        sp = getSharedPreferences("app_config", MODE_PRIVATE);
        toggleSwitch = findViewById(R.id.btn_toggle);

        // 恢复上次保存的开关状态
        toggleSwitch.setChecked(sp.getBoolean("is_active", false));

        // 监听开关变化
        toggleSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            Log.d("MainActivity", "Switch state changed: " + isChecked);
            System.out.println("TextGrabber: Switch state changed: " + isChecked);
            
            // 显式创建并显示 Toast
            Toast toast = Toast.makeText(MainActivity.this, "开关状态: " + isChecked, Toast.LENGTH_LONG);
            toast.show();
            
            // 保存当前状态
            sp.edit().putBoolean("is_active", isChecked).apply();

            // 如果开启了功能，但系统无障碍服务未开启，则提示并跳转设置
            if (isChecked && !isAccessibilitySettingsOn(this)) {
                Toast.makeText(this, R.string.permission_hint, Toast.LENGTH_LONG).show();
                startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));
            }
        });

        btnSearch.setOnClickListener(v -> performSearch());
        etSearch.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH) {
                performSearch();
                return true;
            }
            return false;
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        // 开始定时更新计数
        handler.post(updateCounterRunnable);
        // 开始定时更新最近文本
        handler.post(updateRecentTextsRunnable);
        // 开始定时更新数据库大小
        handler.post(updateDbSizeRunnable);
    }

    @Override
    protected void onPause() {
        super.onPause();
        // 停止定时更新
        handler.removeCallbacks(updateCounterRunnable);
        handler.removeCallbacks(updateRecentTextsRunnable);
        handler.removeCallbacks(updateDbSizeRunnable);
    }

    /**
     * 更新计数显示
     */
    private void updateCounter() {
        int count = sp.getInt("saved_count", 0);
        tvCounter.setText(getString(R.string.saved_count, count));
    }

    /**
     * 更新最近文本显示
     */
    private void updateRecentTexts() {
        new Thread(() -> {
            java.util.List<String> recentTexts = dbManager.getRecentTexts(50);
            
            StringBuilder sb = new StringBuilder(getString(R.string.recent_texts_title, 50));
            if (recentTexts.isEmpty()) {
                sb.append(getString(R.string.no_data));
            } else {
                for (int i = 0; i < recentTexts.size(); i++) {
                    String text = recentTexts.get(i);
                    // 截断过长的文本
                    if (text.length() > 50) {
                        text = text.substring(0, 50) + "...";
                    }
                    sb.append((i + 1)).append(". ").append(text).append("\n");
                }
            }
            
            final String displayText = sb.toString();
            runOnUiThread(() -> tvRecentTexts.setText(displayText));
        }).start();
    }

    /**
     * 按关键词搜索 content 列并展示结果
     */
    private void performSearch() {
        final String keyword = etSearch.getText() != null ? etSearch.getText().toString().trim() : "";
        if (keyword.isEmpty()) {
            Toast.makeText(this, R.string.search_empty_keyword, Toast.LENGTH_SHORT).show();
            return;
        }

        new Thread(() -> {
            java.util.List<String> results = dbManager.searchByContent(keyword, 50);

            StringBuilder sb = new StringBuilder(getString(R.string.search_results_title, results.size()));
            if (results.isEmpty()) {
                sb.append(getString(R.string.no_data));
            } else {
                for (int i = 0; i < results.size(); i++) {
                    String text = results.get(i);
                    if (text != null && text.length() > 50) {
                        text = text.substring(0, 50) + "...";
                    }
                    sb.append((i + 1)).append(". ").append(text).append("\n");
                }
            }

            final String displayText = sb.toString();
            runOnUiThread(() -> tvSearchResults.setText(displayText));
        }).start();
    }

    /**
     * 更新数据库大小显示（基于数据库文件大小）
     */
    private void updateDbSize() {
        try {
            java.io.File dbFile = getDatabasePath(dbManager.getDatabaseName());
            long sizeBytes = (dbFile != null && dbFile.exists()) ? dbFile.length() : 0L;
            tvDbSize.setText(getString(R.string.db_size, formatBytes(sizeBytes)));
        } catch (Exception e) {
            Log.e("MainActivity", "Failed to update db size", e);
            tvDbSize.setText(getString(R.string.db_size, "N/A"));
        }
    }

    private String formatBytes(long bytes) {
        if (bytes <= 0) return "0 B";

        final String[] units = {"B", "KB", "MB", "GB", "TB"};
        int digitGroups = (int) (Math.log10((double) bytes) / Math.log10(1024));
        if (digitGroups < 0) digitGroups = 0;
        if (digitGroups >= units.length) digitGroups = units.length - 1;

        double value = bytes / Math.pow(1024, digitGroups);
        return String.format(java.util.Locale.US, "%.1f %s", value, units[digitGroups]);
    }

    /**
     * 检查当前应用的无障碍服务是否已开启
     *
     * @param mContext 上下文
     * @return true 表示已开启，false 表示未开启
     */
    private boolean isAccessibilitySettingsOn(Context mContext) {
        int accessibilityEnabled = 0;
        final String service = getPackageName() + "/" + MyAccessibilityService.class.getCanonicalName();
        try {
            accessibilityEnabled = Settings.Secure.getInt(
                    mContext.getApplicationContext().getContentResolver(),
                    android.provider.Settings.Secure.ACCESSIBILITY_ENABLED);
        } catch (Settings.SettingNotFoundException e) {
            return false;
        }
        TextUtils.SimpleStringSplitter mStringColonSplitter = new TextUtils.SimpleStringSplitter(':');

        if (accessibilityEnabled == 1) {
            String settingValue = Settings.Secure.getString(
                    mContext.getApplicationContext().getContentResolver(),
                    Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
            if (settingValue != null) {
                mStringColonSplitter.setString(settingValue);
                while (mStringColonSplitter.hasNext()) {
                    String accessibilityService = mStringColonSplitter.next();
                    if (accessibilityService.equalsIgnoreCase(service)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
}
