package com.challenge.keyboardprototype;

import android.app.Application;
import android.util.Log;
import com.challenge.keyboardprototype.db.Database;
import com.challenge.keyboardprototype.model.KeyboardType;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * Custom keyboard app definition for global state management
 *
 * Created by lpayne on 2016-10-04.
 */
public class KeyboardApp extends Application {
    private static final String TAG = "KB-App";
    private static final int DEFAULT_TRIALS_PER_KEYBOARD = 2;

    private static KeyboardApp m_instance;

    @Override
    public void onCreate() {
        super.onCreate();

        Log.i(TAG, "Initializing...");

        m_instance = this;

        getDatabase().loadDictionary();

        Log.i(TAG, "Done initializing!");
    }

    public static Database getDatabase() { return Database.getInstance(m_instance); }

    public static int getTrialsPerKeyboard() {
        // TODO: make trials per session configurable
        return DEFAULT_TRIALS_PER_KEYBOARD;
    }
}
