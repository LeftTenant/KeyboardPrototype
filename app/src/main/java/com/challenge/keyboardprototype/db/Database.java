package com.challenge.keyboardprototype.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;
import com.challenge.keyboardprototype.model.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Persistent storage of dictionary and trial data.
 *
 * TODO: transactional consistency
 *
 * Created by lpayne on 2016-10-04.
 */
public class Database {
    private static final String TAG = "KB-DB";

    private static Database m_instance;

    private final SQLiteDatabase  m_db;
    private final Context         m_context;

    private Database(Context context) {
        m_db = new DatabaseOpenHelper(context).getWritableDatabase();
        m_context = context;
    }

    public static Database getInstance(Context context) {
        if (m_instance == null) {
            m_instance = new Database(context);
        }

        return m_instance;
    }

    public void doInTransaction(Runnable runnable) {
        m_db.beginTransaction();
        try {
            runnable.run();
            m_db.setTransactionSuccessful();
        } finally {
            m_db.endTransaction();
        }
    }

    // Sessions

    private static final String[] SESSION_FIELDS = new String[] {
            "session_id", "participant_id", "keyboard_type_order", "status", "start_ts" };
    private Session fetchSession(Cursor cursor) {
        return new Session(
                cursor.getLong(0),
                cursor.getString(1),
                KeyboardType.orderFromDbString(cursor.getString(2)),
                SessionStatus.valueOf(cursor.getString(3)),
                cursor.getLong(4));
    }

    public long insertSession(String participantId, String keyboardOrder, String status) {
        ContentValues values = new ContentValues(3);
        values.put("participant_id", participantId);
        values.put("keyboard_type_order", keyboardOrder);
        values.put("status", status);
        return m_db.insert("session", null, values);
    }

    public void updateSession(Session session) {
        ContentValues values = new ContentValues(2);
        values.put("status", session.getStatus().toString());
        if (session.hasStarted()) {
            values.put("start_ts", session.getStartTimestamp());
        }

        int numRows = m_db.update("session",
                values,
                "session_id = ?",
                new String[] { Long.toString(session.getSessionId())});
        if (numRows == 0) {
            throw new IllegalArgumentException("Session " + session.getSessionId() + " did not exist.");
        }
    }

    public List<Session> getAllSessions() {
        Cursor cursor = m_db.query("session", SESSION_FIELDS, null, null, null, null, "session_id ASC");
        List<Session> sessions = new ArrayList<>(cursor.getCount());
        while (cursor.moveToNext()) {
            sessions.add(fetchSession(cursor));
        }
        cursor.close();
        return sessions;
    }

    public int deleteAllSessions() {
        return m_db.delete("session", null, null);
    }

    public Session getLastCompletedSession() {
        Cursor cursor = m_db.query("session",
                SESSION_FIELDS,
                "status = ?",
                new String[] { SessionStatus.COMPLETED.toString() },
                null,
                null,
                "session_id DESC",
                "1");

        Session session = null;
        if (cursor.moveToFirst()) {
            session = fetchSession(cursor);
        }
        cursor.close();
        return session;
    }

    // Trials

    private static final String[] TRIAL_FIELDS = new String[] {
            "trial_id",
            "session_id",
            "keyboard_type",
            "target_word",
            "entered_word",
            "entry_method",
            "start_ts",
            "end_ts",

    };

    private Trial fetchTrial(Cursor cursor) {
        return new Trial(cursor.getLong(0),
                cursor.getLong(1),
                KeyboardType.fromDbChar(cursor.getString(2).charAt(0)),
                cursor.getString(3),
                cursor.isNull(6) ? Trial.UNDEFINED_TIMESTAMP : cursor.getLong(6),
                cursor.isNull(7) ? Trial.UNDEFINED_TIMESTAMP : cursor.getLong(7),
                cursor.isNull(5) ? null : EntryMethod.valueOf(cursor.getString(5)),
                cursor.isNull(4) ? null : cursor.getString(4));
    }

    public long insertTrial(long sessionId, String targetWord, char keyboardType) {
        ContentValues values = new ContentValues(3);
        values.put("session_id", sessionId);
        values.put("target_word", targetWord);
        values.put("keyboard_type", String.valueOf(keyboardType));
        return m_db.insert("trial", null, values);
    }

    public void updateTrial(Trial trial) {
        ContentValues values = new ContentValues(4);
        values.put("start_ts", trial.getStartTimestamp());

        if (trial.hasEnded()) {
            values.put("end_ts", trial.getEndTimestamp());
            values.put("entry_method", trial.getEntryMethod().toString());
            values.put("entered_word", trial.getEnteredWord());
        }

        int numRows = m_db.update("trial", values, "trial_id = ?", new String[] { Long.toString(trial.getTrialId()) });
        if (numRows == 0) {
            throw new IllegalArgumentException("Trial " + trial.getTrialId() + " did not exist.");
        }
    }

    public int deleteAllTrials() {
        return m_db.delete("trial", null, null);
    }

    public List<Trial> getTrialsForSession(long sessionId) {
        Cursor cursor = m_db.query("trial",
                TRIAL_FIELDS,
                "session_id = ?",
                new String[] { Long.toString(sessionId) },
                null,
                null,
                "trial_id");
        List<Trial> trials = new ArrayList<>(cursor.getCount());
        while (cursor.moveToNext()) {
            trials.add(fetchTrial(cursor));
        }
        cursor.close();
        return trials;
    }

    // Dictionary

    public void insertWord(String word, int frequency) {
        ContentValues values = new ContentValues(2);
        values.put("word", word);
        values.put("frequency", frequency);
        m_db.insertWithOnConflict("dictionary", null, values, SQLiteDatabase.CONFLICT_REPLACE);
    }

    public String getRandomWord() {
        Cursor cursor = m_db.query("dictionary", new String[] { "word" },
                null, null, null, null, "RANDOM()", "1");

        String word = cursor.moveToFirst() ? cursor.getString(0) : null;
        cursor.close();
        return word;
    }

    public int getWordCount() {
        Cursor cursor = m_db.rawQuery("SELECT COUNT(*) FROM dictionary", null);
        int count = cursor.moveToFirst() ? cursor.getInt(0) : 0;
        cursor.close();
        return count;
    }

    public List<String> getSuggestedWords(List<CharSequence> keysPressed, int numWords) {
        if (keysPressed.isEmpty()) {
            return Collections.emptyList();
        }

        StringBuilder sb = new StringBuilder();

        sb.append("word GLOB '");
        for (CharSequence keyChars : keysPressed) {
            // Not ideal to embed characters here, but they should always be legit ascii alpha for now
            sb.append("[").append(keyChars.toString().toLowerCase()).append("]");
        }
        sb.append("*'");

        Cursor cursor = m_db.query("dictionary",
                new String[] { "word" },
                sb.toString(),
                null,
                null,
                null,
                "frequency",
                Integer.toString(numWords));

        List<String> words = new ArrayList<>(cursor.getCount());
        while (cursor.moveToNext()) {
            words.add(cursor.getString(0));
        }
        cursor.close();
        return words;
    }
}
