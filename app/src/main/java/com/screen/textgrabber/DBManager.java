package com.screen.textgrabber;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.List;

public class DBManager extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "text_grabber.db";
    private static final int DATABASE_VERSION = 3;
    private static final String PROJECTS_TABLE = "capture_projects";
    private static final String TABLE_NAME = "captured_text";

    private static final String COLUMN_ID = "_id";
    private static final String COLUMN_CONTENT = "content";
    private static final String COLUMN_PKG_NAME = "pkg_name";
    private static final String COLUMN_VIEW_ID = "view_id";
    private static final String COLUMN_CAPTURE_TIME = "capture_time";
    private static final String COLUMN_PROJECT_ID = "project_id";

    public static class Project {
        public final long id;
        public final String name;

        Project(long id, String name) {
            this.id = id;
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    public DBManager(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + PROJECTS_TABLE + " (" +
                COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "name TEXT NOT NULL, created_at INTEGER NOT NULL)");
        createCaptureTable(db);
    }

    private void createCaptureTable(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + TABLE_NAME + " (" +
                COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COLUMN_CONTENT + " TEXT, " +
                COLUMN_PKG_NAME + " TEXT, " +
                COLUMN_VIEW_ID + " TEXT, " +
                COLUMN_CAPTURE_TIME + " INTEGER, " +
                COLUMN_PROJECT_ID + " INTEGER NOT NULL)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 3) {
            db.beginTransaction();
            try {
                db.execSQL("CREATE TABLE " + PROJECTS_TABLE + " (" +
                        COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        "name TEXT NOT NULL, created_at INTEGER NOT NULL)");
                ContentValues project = new ContentValues();
                project.put("name", "Default project");
                project.put("created_at", System.currentTimeMillis());
                long projectId = db.insert(PROJECTS_TABLE, null, project);

                db.execSQL("ALTER TABLE " + TABLE_NAME + " RENAME TO captured_text_legacy");
                createCaptureTable(db);
                db.execSQL("INSERT INTO " + TABLE_NAME + " (" + COLUMN_ID + ", " +
                        COLUMN_CONTENT + ", " + COLUMN_PKG_NAME + ", " + COLUMN_VIEW_ID + ", " +
                        COLUMN_CAPTURE_TIME + ", " + COLUMN_PROJECT_ID + ") SELECT " +
                        COLUMN_ID + ", " + COLUMN_CONTENT + ", " + COLUMN_PKG_NAME + ", " +
                        COLUMN_VIEW_ID + ", " + COLUMN_CAPTURE_TIME + ", " + projectId +
                        " FROM captured_text_legacy");
                db.execSQL("DROP TABLE captured_text_legacy");
                db.setTransactionSuccessful();
            } finally {
                db.endTransaction();
            }
        }
    }

    public long createProject(String name) {
        ContentValues values = new ContentValues();
        values.put("name", name);
        values.put("created_at", System.currentTimeMillis());
        return getWritableDatabase().insert(PROJECTS_TABLE, null, values);
    }

    public List<Project> getProjects() {
        List<Project> projects = new ArrayList<>();
        try (Cursor cursor = getReadableDatabase().query(
                PROJECTS_TABLE,
                new String[]{COLUMN_ID, "name"},
                null, null, null, null, COLUMN_ID + " ASC")) {
            while (cursor.moveToNext()) {
                projects.add(new Project(cursor.getLong(0), cursor.getString(1)));
            }
        }
        return projects;
    }

    public long ensureDefaultProject(String name) {
        SQLiteDatabase db = getWritableDatabase();
        try (Cursor cursor = db.query(PROJECTS_TABLE, new String[]{COLUMN_ID},
                null, null, null, null, COLUMN_ID + " ASC", "1")) {
            if (cursor.moveToFirst()) {
                return cursor.getLong(0);
            }
        }
        ContentValues values = new ContentValues();
        values.put("name", name);
        values.put("created_at", System.currentTimeMillis());
        return db.insert(PROJECTS_TABLE, null, values);
    }

    public void insertText(String content, String pkgName, String viewId, long captureTime,
                           long projectId) {
        ContentValues values = new ContentValues();
        values.put(COLUMN_CONTENT, content);
        values.put(COLUMN_PKG_NAME, pkgName);
        values.put(COLUMN_VIEW_ID, viewId);
        values.put(COLUMN_CAPTURE_TIME, captureTime);
        values.put(COLUMN_PROJECT_ID, projectId);
        getWritableDatabase().insert(TABLE_NAME, null, values);
    }

    public List<String> getTextsForProject(long projectId) {
        List<String> texts = new ArrayList<>();
        try (Cursor cursor = getReadableDatabase().query(
                TABLE_NAME, new String[]{COLUMN_CONTENT},
                COLUMN_PROJECT_ID + " = ?", new String[]{String.valueOf(projectId)},
                null, null, COLUMN_CAPTURE_TIME + " ASC, " + COLUMN_ID + " ASC")) {
            while (cursor.moveToNext()) {
                texts.add(cursor.getString(0));
            }
        }
        return texts;
    }

    public List<String> searchByContent(String keyword, long projectId) {
        List<String> texts = new ArrayList<>();
        if (keyword == null || keyword.trim().isEmpty()) {
            return texts;
        }
        try (Cursor cursor = getReadableDatabase().query(
                TABLE_NAME, new String[]{COLUMN_CONTENT},
                COLUMN_PROJECT_ID + " = ? AND " + COLUMN_CONTENT + " LIKE ?",
                new String[]{String.valueOf(projectId), "%" + keyword.trim() + "%"},
                null, null, COLUMN_CAPTURE_TIME + " ASC, " + COLUMN_ID + " ASC")) {
            while (cursor.moveToNext()) {
                texts.add(cursor.getString(0));
            }
        }
        return texts;
    }

    public long getTextCount(long projectId) {
        try (Cursor cursor = getReadableDatabase().rawQuery(
                "SELECT COUNT(*) FROM " + TABLE_NAME + " WHERE " + COLUMN_PROJECT_ID + " = ?",
                new String[]{String.valueOf(projectId)})) {
            return cursor.moveToFirst() ? cursor.getLong(0) : 0;
        }
    }
}
