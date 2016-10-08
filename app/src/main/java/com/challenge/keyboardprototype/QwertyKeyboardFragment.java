package com.challenge.keyboardprototype;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import com.challenge.keyboardprototype.model.KeyboardType;

/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link OnKeyboardInteractionListener} interface
 * to handle interaction events.
 *
 * Use the {@link QwertyKeyboardFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class QwertyKeyboardFragment extends Fragment {

    private static final String QWERTY_KEY_TAG = "qwerty_key";
    private static final String KEYBOARD_TYPE_PARAM = "keyboardType";

    private OnKeyboardInteractionListener m_listener;

    private final Button.OnClickListener m_keyPressListener = new Button.OnClickListener() {
        @Override
        public void onClick(View view) {
            Button button = (Button) view;
            keyPressed(button.getText().toString().replace("\n",""));
        }
    };


    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnKeyboardInteractionListener {
        /**
         * Called when a key is pressed on the keyboard
         *
         * @param keyChars string of letters on the key that was pressed
         */
        void onKeyPressed(CharSequence keyChars);
    }

    public QwertyKeyboardFragment() {
        // Required empty public constructor
    }

    public KeyboardType getKeyboardType() {
        return KeyboardType.valueOf(getArguments().getString(KEYBOARD_TYPE_PARAM));
    }

    /**
     * @return A new instance of fragment QwertyKeyboardFragment.
     */
    public static QwertyKeyboardFragment newInstance(KeyboardType keyboardType) {
        // This factory method could eventually return radically different keyboard
        // fragments, but for now just use the same class for all of them
        QwertyKeyboardFragment fragment = new QwertyKeyboardFragment();

        Bundle args = new Bundle();
        args.putString(KEYBOARD_TYPE_PARAM, keyboardType.toString());
        fragment.setArguments(args);

        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(getKeyboardType().getLayoutResource(), container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        activateAllQwertyKeys((ViewGroup) view);
    }

    private void activateAllQwertyKeys(ViewGroup root) {
        final int childCount = root.getChildCount();
        for (int i = 0; i < childCount; i++) {
            View child = root.getChildAt(i);
            if (child instanceof ViewGroup) {
                activateAllQwertyKeys((ViewGroup)child);
            }

            Object tagObj = child.getTag();
            if (tagObj != null && tagObj.equals(QWERTY_KEY_TAG)) {
                if (child instanceof Button) {
                    child.setOnClickListener(m_keyPressListener);
                } else {
                    throw new RuntimeException(
                            "All views with '" + QWERTY_KEY_TAG + "' tag must be buttons.");
                }
            }

        }
    }

    private void keyPressed(CharSequence keyChars) {
        if (m_listener != null) {
            m_listener.onKeyPressed(keyChars);
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnKeyboardInteractionListener) {
            m_listener = (OnKeyboardInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnKeyboardInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        m_listener = null;
    }
}
