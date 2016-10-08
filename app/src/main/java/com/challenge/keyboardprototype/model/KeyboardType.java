package com.challenge.keyboardprototype.model;

import com.challenge.keyboardprototype.R;

import java.util.ArrayList;
import java.util.List;

/**
 * The set of keyboards that can be tried.
 *
 * Created by lpayne on 2016-10-05.
 */
public enum KeyboardType {
    QWERTY_STANDARD('S', R.layout.fragment_qwerty_standard_keyboard),
    QWERTY_NINE_KEY('9', R.layout.fragment_qwerty_nine_keyboard),
    QWERTY_THREE_KEY('3', R.layout.fragment_qwerty_three_keyboard),
    ;

    private final char m_dbChar;
    private final int m_layoutResource;

    KeyboardType(char dbChar, int layoutResource) {
        m_dbChar = dbChar;
        m_layoutResource = layoutResource;
    }

    public char getDbChar() {
        return m_dbChar;
    }

    public int getLayoutResource() {
        return m_layoutResource;
    }

    public static KeyboardType fromDbChar(char dbChar) {
        for (KeyboardType type : values()) {
            if (type.getDbChar() == dbChar) {
                return type;
            }
        }

        throw new IllegalArgumentException("No keyboard type found for '" + dbChar + "'");
    }

    public static String orderAsDbString(List<KeyboardType> keyboardOrder) {
        StringBuilder sb = new StringBuilder(keyboardOrder.size());
        for (KeyboardType type : keyboardOrder) {
            sb.append(type.getDbChar());
        }
        return sb.toString();
    }


    public static List<KeyboardType> orderFromDbString(String string) {
        List<KeyboardType> list = new ArrayList<>(string.length());

        for (int i = 0; i < string.length(); ++i) {
            KeyboardType keyboardType = fromDbChar(string.charAt(i));
            list.add(keyboardType);
        }

        return list;
    }

    public static List<KeyboardType> nextKeyboardOrder(List<KeyboardType> keyboardOrder) {
        List<KeyboardType> newOrder = new ArrayList<>(keyboardOrder.size());

        for (int i = 1; i <= keyboardOrder.size(); ++i) {
            newOrder.add(keyboardOrder.get(i % keyboardOrder.size()));
        }

        return newOrder;
    }
}
