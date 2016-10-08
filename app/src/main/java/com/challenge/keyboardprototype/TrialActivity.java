package com.challenge.keyboardprototype;

import android.content.Intent;
import android.content.res.Resources;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Html;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import com.challenge.keyboardprototype.model.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class TrialActivity extends AppCompatActivity
        implements QwertyKeyboardFragment.OnKeyboardInteractionListener
{
    private static final String TAG = "KB-Trial";

    static final String PARTICIPANT_ID_PARAM =
            AppCompatActivity.class.getCanonicalName() + ".participantId";

    private static final List<KeyboardType> DEFAULT_KEYBOARD_ORDER =
            Arrays.asList(KeyboardType.values());
    private static final String KEYBOARD_FRAGMENT_TAG = "keyboardFragment";
    private static final int MAX_SUGGESTED_WORDS = 12;
    private static final int MAX_SUGGESTED_WORDS_VISIBLE = 3;

    private TextView    m_targetWordText;
    private TextView    m_autocompleteWordText;
    private Button      m_acceptButton;
    private ListView    m_suggestedList;

    private Session m_currentSession;
    private int m_currentKeyboardTrialsRemaining;
    private Trial m_currentTrial;
    private String m_autocompleteWord;
    private final List<CharSequence> m_keysPressed = new ArrayList<>();
    private ArrayAdapter<String> m_suggestedAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_trial);

        m_targetWordText = (TextView) findViewById(R.id.activity_trial_target_word);
        m_autocompleteWordText = (TextView) findViewById(R.id.activity_trial_autocomplete_word);
        m_acceptButton = (Button) findViewById(R.id.activity_trial_accept);
        m_suggestedList = (ListView) findViewById(R.id.activity_trial_suggestions);

        m_acceptButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                acceptWord(EntryMethod.PRIMARY_SUGGESTION_ACCEPTED, m_autocompleteWord);
            }
        });
        m_acceptButton.setEnabled(false);

        // Set up the adapter to handle the list of suggested words
        m_suggestedAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1);
        m_suggestedList.setAdapter(m_suggestedAdapter);
        m_suggestedList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                acceptWord(EntryMethod.ALTERNATE_SUGGESTION_SELECTED, m_suggestedAdapter.getItem(position));
            }
        });

        // Initialize the session

        // TODO: Handle re-creation of this activity by un-bundling the session id (if available)
        Intent intent = getIntent();
        String participantId = intent.getStringExtra(PARTICIPANT_ID_PARAM);

        // Fetch the last session to determine the keyboard order and
        m_currentSession = Session.createSession(participantId, getNextKeyboardOrder());
        m_currentKeyboardTrialsRemaining = KeyboardApp.getTrialsPerKeyboard();
        setUpNextTrial();
    }

    @Override
    public void onBackPressed() {
        m_currentSession.cancel();
        super.onBackPressed();
    }

    private List<KeyboardType> getNextKeyboardOrder() {
        Session lastSession = KeyboardApp.getDatabase().getLastCompletedSession();
        if (lastSession == null) {
            return DEFAULT_KEYBOARD_ORDER;
        } else {
            return KeyboardType.nextKeyboardOrder(lastSession.getKeyboardOrder());
        }
    }

    private void setUpNextTrial() {
        KeyboardType nextKeyboard;

        if (m_currentTrial == null) {
            // This is the first trial, so just use the first keyboard type from the session's order
            nextKeyboard = m_currentSession.getFirstKeyboardType();
        } else {
            // This is not the first trial, so use the same keyboard type as last trial
            nextKeyboard = m_currentTrial.getKeyboardType();

            if (m_currentKeyboardTrialsRemaining == 0) {
                // Unless we are out of trials for this keyboard type, then go on to the next one
                nextKeyboard = m_currentSession.getKeyboardTypeAfter(nextKeyboard);
                m_currentKeyboardTrialsRemaining = KeyboardApp.getTrialsPerKeyboard();

                // TODO: Show Likert scale
            }
        }
        --m_currentKeyboardTrialsRemaining;

        if (nextKeyboard == null) {
            m_currentSession.complete();
            finish();
            return;
        }

        // Create the new trial based on a randomly chosen word
        m_currentTrial = Trial.createTrial(m_currentSession.getSessionId(),
                KeyboardApp.getDatabase().getRandomWord(), nextKeyboard);

        // Clear previous state of activity for new word entry
        m_targetWordText.setText(m_currentTrial.getTargetWord());
        m_autocompleteWord = null;
        m_autocompleteWordText.setText("");
        m_keysPressed.clear();
        refreshSuggestions(Collections.<String>emptyList());
        updateKeyboardFragment(m_currentTrial.getKeyboardType());

        // Start the current trial and the current session (if not yet started)
        if (m_currentSession.getStatus() == SessionStatus.CREATED) {
            m_currentSession.start();
        }
        m_currentTrial.start();
    }

    private void updateKeyboardFragment(KeyboardType keyboardType) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        QwertyKeyboardFragment fragment = (QwertyKeyboardFragment) fragmentManager.findFragmentByTag(
                KEYBOARD_FRAGMENT_TAG);
        if (fragment != null && fragment.getKeyboardType() == keyboardType) {
            return;
        }

        fragment = QwertyKeyboardFragment.newInstance(keyboardType);
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        transaction.replace(R.id.activity_trial_keyboard_container, fragment, KEYBOARD_FRAGMENT_TAG);
        transaction.commit();
        Log.d(TAG, "Keyboard type changed to " + keyboardType.toString());
    }

    private void acceptWord(EntryMethod method, String word) {
        Log.i(TAG, "Autocomplete word accepted: " + word);

        m_currentTrial.end(method, word);
        setUpNextTrial();
    }

    @Override
    public void onKeyPressed(CharSequence keyChars) {
        Log.i(TAG, "Key pressed: " + keyChars);
        m_acceptButton.setEnabled(true);
        m_keysPressed.add(keyChars);
        List<String> suggestedWords = KeyboardApp.getDatabase().getSuggestedWords(
                m_keysPressed, MAX_SUGGESTED_WORDS);

        if (suggestedWords.isEmpty()) {
            m_autocompleteWord = null;
            m_autocompleteWordText.setText(getResources().getString(R.string.trial_no_word_found));
            m_autocompleteWordText.setTextColor(getResources().getColor(R.color.colorError, null));
            refreshSuggestions(suggestedWords);
        } else {
            int autocompleteIndex = suggestedWords.size() - 1;
            m_autocompleteWord = suggestedWords.get(autocompleteIndex);
            String html =
                    "<font color=\"#" + getHtmlColor(R.color.colorTyped) + "\">" +
                    m_autocompleteWord.substring(0, m_keysPressed.size()) +
                    "</font>" +
                    "<font color=\"#" + getHtmlColor(R.color.colorAutocomplete) + "\">" +
                    m_autocompleteWord.substring(m_keysPressed.size(), m_autocompleteWord.length()) +
                    "</font>";
            m_autocompleteWordText.setText(Html.fromHtml(html));

            refreshSuggestions(suggestedWords.subList(0, autocompleteIndex));
            m_suggestedList.smoothScrollToPosition(m_suggestedAdapter.getCount() - 1);
        }
    }

    private void refreshSuggestions(List<String> suggestedWords) {
        m_suggestedAdapter.clear();
        m_suggestedAdapter.addAll(suggestedWords);
        m_suggestedAdapter.notifyDataSetChanged();

        // Adjust the height to fit items
        int numItemsVisible = Math.min(MAX_SUGGESTED_WORDS_VISIBLE, suggestedWords.size());

        if (numItemsVisible == 0) {
            m_suggestedList.setVisibility(View.GONE);
        } else {
            m_suggestedList.setVisibility(View.VISIBLE);
        }

        // Get total height of visible items.
        int totalItemsHeight = 0;
        for (int i = 0; i < numItemsVisible; ++i) {
            View item = m_suggestedAdapter.getView(i, null, m_suggestedList);
            item.measure(0, 0);
            totalItemsHeight += item.getMeasuredHeight();
        }

        // Get total height of all item dividers.
        int totalDividersHeight = m_suggestedList.getDividerHeight() * (numItemsVisible - 1);

        // Set list height.
        ViewGroup.LayoutParams params = m_suggestedList.getLayoutParams();
        params.height = totalItemsHeight + totalDividersHeight;
        m_suggestedList.setLayoutParams(params);
        m_suggestedList.requestLayout();
    }

    private String getHtmlColor(int colorRes) {
        Resources res = getResources();
        return Integer.toHexString(res.getColor(colorRes, null) & 0x00FFFFFF);
    }
}
