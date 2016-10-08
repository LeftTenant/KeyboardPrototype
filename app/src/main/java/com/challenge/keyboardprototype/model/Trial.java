package com.challenge.keyboardprototype.model;

import com.challenge.keyboardprototype.KeyboardApp;

/**
 * A single attempt to type a word with a keyboard.
 *
 * Created by lpayne on 2016-10-05.
 */
public class Trial {
    public static final long UNDEFINED_TIMESTAMP = 0;

    private final long m_trialId;
    private final long m_sessionId;
    private final KeyboardType m_keyboardType;
    private final String m_targetWord;
    private long m_startTimestamp;
    private long m_endTimestamp;
    private EntryMethod m_entryMethod;
    private String m_enteredWord;

    private Trial(long trialId, long sessionId, String targetWord, KeyboardType keyboardType) {
        this(trialId, sessionId, keyboardType, targetWord, UNDEFINED_TIMESTAMP, UNDEFINED_TIMESTAMP, null, null);
    }

    public Trial(long trialId,
                 long sessionId,
                 KeyboardType keyboardType,
                 String targetWord,
                 long startTimestamp,
                 long endTimestamp,
                 EntryMethod entryMethod,
                 String enteredWord)
    {
        m_trialId = trialId;
        m_sessionId = sessionId;
        m_keyboardType = keyboardType;
        m_targetWord = targetWord;
        m_startTimestamp = startTimestamp;
        m_endTimestamp = endTimestamp;
        m_entryMethod = entryMethod;
        m_enteredWord = enteredWord;
    }

    public long getTrialId() {
        return m_trialId;
    }

    public long getSessionId() {
        return m_sessionId;
    }

    public long getStartTimestamp() {
        return m_startTimestamp;
    }

    public long getEndTimestamp() {
        return m_endTimestamp;
    }

    public EntryMethod getEntryMethod() {
        return m_entryMethod;
    }

    public String getEnteredWord() {
        return m_enteredWord;
    }

    public KeyboardType getKeyboardType() {
        return m_keyboardType;
    }

    public String getTargetWord() {
        return m_targetWord;
    }

    public static Trial createTrial(long sessionId, String targetWord, KeyboardType keyboardType) {
        long trialId = KeyboardApp.getDatabase().insertTrial(
                sessionId, targetWord, keyboardType.getDbChar());

        return new Trial(trialId, sessionId, targetWord, keyboardType);
    }

    public void start() {
        m_startTimestamp = System.currentTimeMillis();
    }

    public void end(EntryMethod method, String enteredWord) {
        m_entryMethod = method;
        m_enteredWord = enteredWord;
        m_endTimestamp = System.currentTimeMillis();

        save();
    }

    public void save() {
        KeyboardApp.getDatabase().updateTrial(this);
    }

    public boolean hasEnded() {
        return m_endTimestamp != UNDEFINED_TIMESTAMP;
    }

    public int getDurationMs() {
        return hasEnded() ? (int) (m_endTimestamp - m_startTimestamp) : -1;
    }
}
