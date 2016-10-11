package com.challenge.keyboardprototype;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import com.challenge.keyboardprototype.db.Database;
import com.challenge.keyboardprototype.model.Session;
import com.challenge.keyboardprototype.model.Trial;
import com.opencsv.CSVWriter;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "KB-Main";
    private static final String EXCEL_TS_FORMAT = "yyyy-MM-dd HH:mm:ss.SSS";

    private EditText    m_participantText;
    private Button      m_startButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        m_participantText = (EditText) findViewById(R.id.activity_main_participant);
        m_startButton = (Button) findViewById(R.id.activity_main_start_trials);

        m_participantText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                // do nothing
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                // do nothing
            }

            @Override
            public void afterTextChanged(Editable editable) {
                m_startButton.setEnabled(editable.length() > 0);
            }
        });
        m_startButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startTrialsClicked();
            }
        });
    }

    private void startTrialsClicked() {
        Log.d(TAG, "Start Trials clicked.");
        Intent intent = new Intent(this, TrialActivity.class);
        intent.putExtra(TrialActivity.PARTICIPANT_ID_PARAM, m_participantText.getText().toString());
        m_participantText.setText(null);
        startActivity(intent);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (KeyboardApp.getDatabase().getWordCount() == 0) {
            DatabaseLoaderTask loaderTask = new DatabaseLoaderTask(this);
            loaderTask.execute();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case R.id.main_menu_save_data:
                saveTrialDataSelected();
                return true;
            case R.id.main_menu_clear_data:
                clearTrialDataSelected();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void saveTrialDataSelected() {
        final File outputFile = getSaveFile();
        if (outputFile == null) {
            return;
        }


        CSVWriter out = null;
        try {
            out = new CSVWriter(new FileWriter(outputFile));
            writeTrialDataHeader(out);

            Database db = KeyboardApp.getDatabase();
            List<Session> sessions = db.getAllSessions();
            for (Session session : sessions) {
                List<Trial> trials = KeyboardApp.getDatabase().getTrialsForSession(session.getSessionId());

                for (Trial trial : trials) {
                    writeTrialDataRow(session, trial, out);
                }
            }

            Toast toast = Toast.makeText(this, getString(R.string.main_save_data_complete_prefix) + outputFile.getName(), Toast.LENGTH_LONG);
            toast.show();
        } catch (IOException e) {
            Log.e(TAG, "Failed to save trial data.", e);
            Toast toast = Toast.makeText(this, R.string.main_save_failed, Toast.LENGTH_SHORT);
            toast.show();
        } finally {
            if (out != null) {
                try {
                    out.close();
                } catch (IOException e) {
                    Log.e(TAG, "Failed to close trial data file.", e);
                }
            }
        }
    }

    private void writeTrialDataHeader(CSVWriter out) {
        out.writeNext(new String[] {
                // Session fields
                "session_id",
                "participant_id",
                "session_start",
                "status,",
                // Trial fields
                "trial_id",
                "keyboard_type",
                "target_word",
                "entered_word",
                "entry_method",
                "trial_start",
                "trial_end",
                "duration_ms",
        });
    }

    private void writeTrialDataRow(Session session, Trial trial, CSVWriter out) {
        out.writeNext(new String[] {
                Long.toString(session.getSessionId()),
                session.getParticipantId(),
                session.hasStarted() ? DateFormat.format(EXCEL_TS_FORMAT, session.getStartTimestamp()).toString() : null,
                session.getStatus().toString(),
                Long.toString(trial.getTrialId()),
                trial.getKeyboardType().toString(),
                trial.getTargetWord(),
                trial.getEnteredWord(),
                trial.hasEnded() ? trial.getEntryMethod().toString() : null,
                trial.hasEnded() ? DateFormat.format(EXCEL_TS_FORMAT, trial.getStartTimestamp()).toString() : null,
                trial.hasEnded() ? DateFormat.format(EXCEL_TS_FORMAT, trial.getEndTimestamp()).toString() : null,
                trial.hasEnded() ? Integer.toString(trial.getDurationMs()) : null,
        });
    }

    // Storage Permissions
    private static final int REQUEST_EXTERNAL_STORAGE = 1;
    private static final String[] PERMISSIONS_STORAGE = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };

    /**
     * Checks if the app has permission to write to device storage
     *
     * If the app does not has permission then the user will be prompted to grant permissions
     */
    private boolean verifyStoragePermissions() {
        // Check if we have write permission
        int permission = ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);

        if (permission != PackageManager.PERMISSION_GRANTED) {
            // We don't have permission so prompt the user
            ActivityCompat.requestPermissions(
                    this,
                    PERMISSIONS_STORAGE,
                    REQUEST_EXTERNAL_STORAGE
            );
            return false;
        }

        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String permissions[],
                                           @NonNull int[] grantResults) {
        if (requestCode == REQUEST_EXTERNAL_STORAGE) {
            // If request is cancelled, the result arrays are empty.
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                saveTrialDataSelected();
            } else {
                Toast toast = Toast.makeText(this, R.string.main_save_data_permission_denied, Toast.LENGTH_SHORT);
                toast.show();
            }
        } else {
            Log.w(TAG, "Unexpected request permission result " + requestCode);
        }
    }

    private File getSaveFile() {
        if (!verifyStoragePermissions()) {
            return null;
        }

        String state = Environment.getExternalStorageState();
        if (!Environment.MEDIA_MOUNTED.equals(state)) {
            Toast toast = Toast.makeText(this, R.string.main_no_external_storage_toast, Toast.LENGTH_SHORT);
            toast.show();
            return null;
        }

        String fileName = "KeyboardTrialData_" +
                DateFormat.format("yyyyMMdd-HHmmss", System.currentTimeMillis()) +
                ".csv";
        File directory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);

        if (!directory.exists() && !directory.mkdir()) {
            Log.e(TAG, "Unable to create '" + directory.getAbsolutePath() + "'");
            Toast toast = Toast.makeText(this, R.string.main_save_failed, Toast.LENGTH_SHORT);
            toast.show();
            return null;
        }

        return new File(directory, fileName);
    }

    private void clearTrialDataSelected() {
        // TODO: This is dangerous so pop a confirmation dialog.
        final Database db = KeyboardApp.getDatabase();
        db.doInTransaction(new Runnable() {
            @Override
            public void run() {
                int numSessions = db.deleteAllSessions();
                int numTrials = db.deleteAllTrials();

                Log.i(TAG, "Cleared data: {sessions=" + numSessions + ", trials=" + numTrials + "}");
                Toast toast = Toast.makeText(MainActivity.this,
                        R.string.main_data_cleared_toast,
                        Toast.LENGTH_SHORT);
                toast.show();
            }
        });

    }
}
