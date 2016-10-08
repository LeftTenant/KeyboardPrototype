package com.challenge.keyboardprototype.db;

import android.content.Context;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

/**
 * Builds the schema for the KeyboardPrototype app.
 *
 * Created by lpayne on 2016-10-04.
 */
class DatabaseOpenHelper extends SQLiteOpenHelper {
    private static final String TAG = "KB-DBOpen";

    private static final int VERSION = 1;
    private static final String DATABASE_NAME = "keyboard_prototype";

    private static final String[] SCHEMA_V1 = {
            "CREATE TABLE dictionary (" +
                    "word TEXT PRIMARY KEY, " +
                    "frequency INTEGER NOT NULL)",
            "CREATE TABLE session (" +
                    "session_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "participant_id TEXT NOT NULL, " +
                    "start_ts INTEGER, " +
                    "keyboard_type_order TEXT NOT NULL, " +
                    "status TEXT NOT NULL)",
            "CREATE TABLE trial (" +
                    "trial_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "session_id INTEGER NOT NULL, " +
                    "keyboard_type TEXT NOT NULL, " +
                    "target_word TEXT NOT NULL, " +
                    "entered_word TEXT, " +
                    "entry_method TEXT, " +
                    "start_ts INTEGER, " +
                    "end_ts INTEGER)",
            "CREATE INDEX ix_session_trial ON trial (session_id ASC)",
            "CREATE INDEX ix_dictionary ON dictionary (word ASC, frequency DESC)"
    };

    DatabaseOpenHelper(Context context) {
        super(context, DATABASE_NAME, null, VERSION);
    }

    private void execStatements(SQLiteDatabase db, String[] statements) {
        for (String statement : statements) {
            db.execSQL(statement);
        }
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        try {
            execStatements(db, SCHEMA_V1);
        } catch (SQLException e) {
            Log.e(TAG, "Error creating database schema", e);
            throw e;
        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }
}
