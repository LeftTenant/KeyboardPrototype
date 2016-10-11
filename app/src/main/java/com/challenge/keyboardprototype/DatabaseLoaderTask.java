package com.challenge.keyboardprototype;

import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.util.Log;
import com.challenge.keyboardprototype.db.Database;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * Loads the database asynchronously while displaying a progress dialog.
 *
 * Created by lpayne on 2016-10-10.
 */
public class DatabaseLoaderTask extends AsyncTask<Void, Integer, Void> {
    private static final String TAG = "KB-DictLoad";
    private Context         m_context;
    private ProgressDialog  m_progressDialog;
    private int m_numWords;

    public DatabaseLoaderTask(Context context) {
        m_context = context;
    }

    @Override
    protected Void doInBackground(Void... params) {
        final Database db = KeyboardApp.getDatabase();
        db.doInTransaction(new Runnable() {
            @Override
            public void run() {
                BufferedReader reader = null;
                try {
                    reader = new BufferedReader(
                            new InputStreamReader(m_context.getAssets().open("dictionary.csv")));

                    // Read each line of the source word data and write it to the dictionary table.
                    String line;
                    int numWords = 0;
                    while ((line = reader.readLine()) != null) {
                        String[] tokens = line.split(",");
                        db.insertWord(tokens[0].toLowerCase(), Integer.valueOf(tokens[1]));
                        ++numWords;
                        publishProgress(numWords);
                    }
                    Log.i(TAG, "Inserted " + numWords + " words into dictionary.");
                } catch (IOException e) {
                    throw new IllegalStateException("Unable to read dictionary file from assets.", e);
                } finally {
                    if (reader != null) {
                        try {
                            reader.close();
                        } catch (IOException e) {
                            Log.e(TAG, "Unable to close dictionary file from assets.", e);
                        }
                    }

                }
            }
        });

        return null;
    }

    @Override
    protected void onPreExecute() {
        m_progressDialog = new ProgressDialog(m_context);
        m_progressDialog.setTitle(m_context.getString(R.string.database_loader_progress_title));
        m_progressDialog.setMessage(m_context.getString(R.string.database_loader_loading_message));
        m_progressDialog.setIndeterminate(true);
        m_progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        m_progressDialog.setButton(ProgressDialog.BUTTON_NEUTRAL, "Dismiss", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        m_progressDialog.setCancelable(false);
        m_progressDialog.show();
        m_progressDialog.getButton(ProgressDialog.BUTTON_NEUTRAL).setEnabled(false);
    }

    @Override
    protected void onProgressUpdate(Integer... values) {
        m_numWords = values[0];
    }

    @Override
    protected void onPostExecute(Void aVoid) {
        m_progressDialog.setCancelable(true);
        m_progressDialog.setCanceledOnTouchOutside(true);
        m_progressDialog.getButton(ProgressDialog.BUTTON_NEUTRAL).setEnabled(true);
        m_progressDialog.setIndeterminate(false);
        m_progressDialog.setMax(m_numWords);
        m_progressDialog.setProgress(m_numWords);
        m_progressDialog.setMessage(m_context.getString(R.string.database_loader_loaded_message));
    }
}
