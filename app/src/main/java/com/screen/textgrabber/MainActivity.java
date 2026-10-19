// 可能是antigravity写的
// 小红书app浏览记录这里无障碍确实是获取不到
package com.screen.textgrabber;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.provider.Settings;
import android.text.TextUtils;
import android.text.InputType;
import android.net.Uri;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.Toast;
import android.util.Log;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
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
    private Spinner spProjects;
    private android.widget.Button btnNewProject;
    private android.widget.Button btnExport;
    private android.os.Handler handler; // 用于定时更新计数
    private Runnable updateCounterRunnable; // 更新计数的任务
    private Runnable updateRecentTextsRunnable; // 更新最近文本的任务
    private Runnable updateDbSizeRunnable; // 更新数据库大小的任务
    private DBManager dbManager; // 数据库管理器
    private ArrayAdapter<DBManager.Project> projectAdapter;
    private long selectedProjectId = -1L;
    private ActivityResultLauncher<String> exportLauncher;

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
        spProjects = findViewById(R.id.sp_projects);
        btnNewProject = findViewById(R.id.btn_new_project);
        btnExport = findViewById(R.id.btn_export);
        
        // 初始化数据库管理器
        dbManager = new DBManager(this);
        long defaultProjectId = dbManager.ensureDefaultProject(getString(R.string.default_project));
        selectedProjectId = sp.getLong("active_project_id", defaultProjectId);
        if (dbManager.getProjects().stream().noneMatch(project -> project.id == selectedProjectId)) {
            selectedProjectId = defaultProjectId;
        }
        sp.edit().putLong("active_project_id", selectedProjectId).apply();
        projectAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item,
                dbManager.getProjects());
        projectAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spProjects.setAdapter(projectAdapter);
        selectProjectInSpinner(selectedProjectId);
        spProjects.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent,
                                       android.view.View view, int position, long id) {
                DBManager.Project project = projectAdapter.getItem(position);
                if (project != null && project.id != selectedProjectId) {
                    selectedProjectId = project.id;
                    sp.edit().putLong("active_project_id", selectedProjectId).apply();
                    updateRecentTexts();
                    updateCounter();
                }
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {
            }
        });
        btnNewProject.setOnClickListener(v -> showCreateProjectDialog());
        exportLauncher = registerForActivityResult(
                new ActivityResultContracts.CreateDocument("text/plain"), this::exportProject);
        btnExport.setOnClickListener(v -> exportLauncher.launch(getExportFileName()));

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
        long count = dbManager.getTextCount(selectedProjectId);
        tvCounter.setText(getString(R.string.saved_count, count));
    }

    /**
     * 更新最近文本显示
     */
    private void updateRecentTexts() {
        new Thread(() -> {
            java.util.List<String> recentTexts = dbManager.getTextsForProject(selectedProjectId);
            
            StringBuilder sb = new StringBuilder(getString(R.string.recent_texts_title, recentTexts.size()));
            if (recentTexts.isEmpty()) {
                sb.append(getString(R.string.no_data));
            } else {
                for (int i = 0; i < recentTexts.size(); i++) {
                    String text = recentTexts.get(i);
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
            java.util.List<String> results = dbManager.searchByContent(keyword, selectedProjectId);

            StringBuilder sb = new StringBuilder(getString(R.string.search_results_title, results.size()));
            if (results.isEmpty()) {
                sb.append(getString(R.string.no_data));
            } else {
                for (int i = 0; i < results.size(); i++) {
                    String text = results.get(i);
                    sb.append((i + 1)).append(". ").append(text).append("\n");
                }

                private void showCreateProjectDialog() {
                    EditText input = new EditText(this);
                    input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
                    new androidx.appcompat.app.AlertDialog.Builder(this)
                            .setTitle(R.string.new_project)
                            .setView(input)
                            .setNegativeButton(android.R.string.cancel, null)
                            .setPositiveButton(R.string.create, (dialog, which) -> {
                                String name = input.getText().toString().trim();
                                if (name.isEmpty()) {
                                    Toast.makeText(this, R.string.project_name_empty, Toast.LENGTH_SHORT).show();
                                    return;
                                }
                                long projectId = dbManager.createProject(name);
                                if (projectId < 0) {
                                    Toast.makeText(this, R.string.project_create_failed, Toast.LENGTH_SHORT).show();
                                    return;
                                }
                                selectedProjectId = projectId;
                                sp.edit().putLong("active_project_id", projectId).apply();
                                projectAdapter.clear();
                                projectAdapter.addAll(dbManager.getProjects());
                                projectAdapter.notifyDataSetChanged();
                                selectProjectInSpinner(projectId);
                                updateRecentTexts();
                                updateCounter();
                            })
                            .show();
                }

                private void selectProjectInSpinner(long projectId) {
                    for (int i = 0; i < projectAdapter.getCount(); i++) {
                        DBManager.Project project = projectAdapter.getItem(i);
                        if (project != null && project.id == projectId) {
                            spProjects.setSelection(i);
                            return;
                        }
                    }
                }

                private String getExportFileName() {
                    DBManager.Project project = getSelectedProject();
                    String name = project == null ? getString(R.string.default_project) : project.name;
                    return name.replaceAll("[\\\\/:*?\"<>|]", "_") + ".txt";
                }

                private DBManager.Project getSelectedProject() {
                    for (int i = 0; i < projectAdapter.getCount(); i++) {
                        DBManager.Project project = projectAdapter.getItem(i);
                        if (project != null && project.id == selectedProjectId) {
                            return project;
                        }
                    }
                    return null;
                }

                private void exportProject(Uri uri) {
                    if (uri == null) {
                        return;
                    }
                    long projectId = selectedProjectId;
                    new Thread(() -> {
                        try (java.io.OutputStream outputStream = getContentResolver().openOutputStream(uri);
                             java.io.OutputStreamWriter writer = new java.io.OutputStreamWriter(
                                     outputStream, java.nio.charset.StandardCharsets.UTF_8)) {
                            for (String text : dbManager.getTextsForProject(projectId)) {
                                writer.write(text == null ? "" : text);
                                writer.write(System.lineSeparator());
                            }
                            runOnUiThread(() ->
                                    Toast.makeText(this, R.string.export_success, Toast.LENGTH_SHORT).show());
                        } catch (Exception e) {
                            Log.e("MainActivity", "Failed to export project", e);
                            runOnUiThread(() ->
                                    Toast.makeText(this, R.string.export_failed, Toast.LENGTH_SHORT).show());
                        }
                    }).start();
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
