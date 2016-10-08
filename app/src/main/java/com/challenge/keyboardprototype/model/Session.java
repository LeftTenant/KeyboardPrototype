package com.challenge.keyboardprototype.model;

import com.challenge.keyboardprototype.KeyboardApp;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Stores the overall state for a series of word typing trials.
 *
 * Created by lpayne on 2016-10-05.
 */
public class Session {
    public static final long UNDEFINED_TIMESTAMP = 0;

    private final long m_sessionId;
    private final String m_participantId;
    private final List<KeyboardType> m_keyboardOrder;

    private long m_startTimestamp;
    private SessionStatus m_status;

    public static Session createSession(String participantId, List<KeyboardType> keyboardOrder) {
        if (keyboardOrder.isEmpty()) {
            throw new IllegalArgumentException("At least one keyboard type is required.");
        }
        long sessionId = KeyboardApp.getDatabase().insertSession(
                participantId,
                KeyboardType.orderAsDbString(keyboardOrder),
                SessionStatus.CREATED.toString());

        return new Session(sessionId, participantId, keyboardOrder, SessionStatus.CREATED, UNDEFINED_TIMESTAMP);
    }

    public Session(long sessionId,
                   String participantId,
                   List<KeyboardType> keyboardOrder,
                   SessionStatus status,
                   long startTimestamp)
    {
        m_sessionId = sessionId;
        m_participantId = participantId;
        m_startTimestamp = startTimestamp;
        m_keyboardOrder = Collections.unmodifiableList(new ArrayList<>(keyboardOrder));
        m_status = status;
    }

    public long getSessionId() {
        return m_sessionId;
    }

    public List<KeyboardType> getKeyboardOrder() {
        return m_keyboardOrder;
    }

    public SessionStatus getStatus() {
        return m_status;
    }

    public long getStartTimestamp() {
        return m_startTimestamp;
    }

    public KeyboardType getFirstKeyboardType() {
        return m_keyboardOrder.get(0);
    }

    public KeyboardType getKeyboardTypeAfter(KeyboardType keyboardType) {
        int foundIndex = m_keyboardOrder.size();

        for (int i = 0; i < m_keyboardOrder.size(); ++i) {
            if (m_keyboardOrder.get(i).equals(keyboardType)) {
                foundIndex = i;
                break;
            }
        }

        if (foundIndex == m_keyboardOrder.size()) {
            throw new IllegalArgumentException(
                    "Unknown keyboard type: " + keyboardType.toString());
        }

        int nextIndex = foundIndex + 1;
        if (nextIndex == m_keyboardOrder.size()) {
            return null;
        }
        return m_keyboardOrder.get(nextIndex);
    }

    public void start() {
        m_startTimestamp = System.currentTimeMillis();
        m_status = SessionStatus.STARTED;
        save();
    }

    public void complete() {
        m_status = SessionStatus.COMPLETED;
        save();
    }

    public void cancel() {
        m_status = SessionStatus.CANCELED;
        save();
    }

    private void save() {
        KeyboardApp.getDatabase().updateSession(this);
    }

    public boolean hasStarted() {
        return m_status != SessionStatus.CREATED;
    }

    public String getParticipantId() {
        return m_participantId;
    }
}
