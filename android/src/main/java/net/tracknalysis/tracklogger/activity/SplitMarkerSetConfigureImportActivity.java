/**
 * Copyright 2012 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this software except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.tracknalysis.tracklogger.activity;

import java.io.File;
import java.util.List;

import group.pals.android.lib.ui.filechooser.FileChooserActivity;
import group.pals.android.lib.ui.filechooser.io.LocalFile;
import net.tracknalysis.tracklogger.R;
import net.tracknalysis.tracklogger.TrackLogger;
import net.tracknalysis.tracklogger._import.android.SplitMarkerSetFileFormat;
import net.tracknalysis.tracklogger._import.android.SplitMarkerSetImporterService;
import net.tracknalysis.tracklogger.config.ConfigurationFactory;
import net.tracknalysis.tracklogger.provider.TrackLoggerData;
import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcelable;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

/**
 * Activity for configuring and enqueuing the import of splt marker sets.
 * 
 * @author David Valeri
 */
public class SplitMarkerSetConfigureImportActivity extends Activity {
    
    /**
     * The ID of the file chooser activity launched from this activity.
     */
    private static final int CHOOSE_IMPORT_FILE_ACTIVITY = 0;
    
    /**
     * The key of the selected file path in the bundle for storing activity state.
     */
    private static String FILE_PATH_STATE_KEY = "FILE_PATH_STATE_KEY";
    
    /**
     * Array of spinner index to matching format enumeration.
     */
    private static final SplitMarkerSetFileFormat[] splitMarkerSetFileFormatArray = new SplitMarkerSetFileFormat[] {
        SplitMarkerSetFileFormat.CSV_1_0 };
    
    private Spinner importFormatSpinner;
    private File file;
    private TextView fileNameTextView;
    private EditText splitMarkerSetNameEditText;
    private Dialog errorDialog;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        setContentView(R.layout.split_marker_set_configure_import);
        
        importFormatSpinner = (Spinner) findViewById(R.id.split_marker_set_configure_import_format_spinner);
        fileNameTextView = (TextView) findViewById(R.id.split_marker_set_configure_import_file_name_text_view);
        splitMarkerSetNameEditText = (EditText) findViewById(R.id.split_marker_set_configure_import_name_edit_text);
        
        Button importButton = (Button) findViewById(R.id.split_marker_set_configure_import_button);
        importButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                onImportButtonClicked();
            }
        });
        
        Button chooseFileButton = (Button) findViewById(R.id.split_marker_set_configure_import_file_name_button);
        chooseFileButton.setOnClickListener(new OnClickListener() {
            
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(), FileChooserActivity.class);
                intent.putExtra(FileChooserActivity._Rootpath,
                        (Parcelable) new LocalFile(ConfigurationFactory
                                .getInstance().getConfiguration()
                                .getDataDirectory()));
                
                startActivityForResult(intent, CHOOSE_IMPORT_FILE_ACTIVITY);     
            }
        });
    }
    
    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        
        String filePath = savedInstanceState.getString(FILE_PATH_STATE_KEY); 
        if (filePath != null) {
            file = new File(filePath);
            fileNameTextView.setText(file.getName());
        }
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case CHOOSE_IMPORT_FILE_ACTIVITY:
                if (resultCode == RESULT_OK) {
                    @SuppressWarnings("unchecked")
                    List<LocalFile> files = (List<LocalFile>)
                        data.getSerializableExtra(FileChooserActivity._Results);
                    
                    file = files.get(0);
                    fileNameTextView.setText(file.getName());
                }
                break;
        }
    }
    
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        
        if (file != null) {
            outState.putString(FILE_PATH_STATE_KEY, file.getAbsolutePath());
        }
    }
    
    @Override
    protected void onDestroy() {
        if (errorDialog != null) {
            errorDialog.dismiss();
        }
        super.onDestroy();
    }
    
    /**
     * Handle what happens when the user clicks the import button in the activity.
     */
    protected void onImportButtonClicked() {
        if (file == null) {
            errorDialog = ActivityUtil
                    .showErrorDialog(
                            SplitMarkerSetConfigureImportActivity.this,
                            false,
                            R.string.app_name,
                            R.string.split_marker_set_config_import_missing_source,
                            (Object[]) null);
            return;
        } else if (importFormatSpinner.getSelectedItemPosition() == AdapterView.INVALID_POSITION) {
            errorDialog = ActivityUtil
                    .showErrorDialog(
                            SplitMarkerSetConfigureImportActivity.this,
                            false,
                            R.string.app_name,
                            R.string.split_marker_set_config_import_invalid_format,
                            (Object[]) null);
            return;
        } else {
        
            Intent intent = new Intent(TrackLogger.ACTION_SPLIT_MARKER_SET_IMPORT);
            intent.putExtra(
                    SplitMarkerSetImporterService.EXTRA_IMPORT_FORMAT,
                    splitMarkerSetFileFormatArray[importFormatSpinner
                            .getSelectedItemPosition()].getId());
            intent.setData(Uri.fromFile(file));
            if (splitMarkerSetNameEditText.getText().length() != 0) {
                
                Cursor splitMarkerSetCursor = managedQuery(
                        TrackLoggerData.SplitMarkerSet.CONTENT_URI,
                        null,
                        TrackLoggerData.SplitMarkerSet.COLUMN_NAME_NAME + " = ?",
                        new String[] {splitMarkerSetNameEditText.getText().toString()},
                        null);
                
                if (splitMarkerSetCursor.getCount() != 0) {
                    errorDialog = ActivityUtil
                            .showErrorDialog(
                                    SplitMarkerSetConfigureImportActivity.this,
                                    false,
                                    R.string.app_name,
                                    R.string.split_marker_set_config_import_duplicate_name,
                                    splitMarkerSetNameEditText
                                            .getText().toString());
                    return;
                }
                
                intent.putExtra(
                        SplitMarkerSetImporterService.EXTRA_NAME,
                        splitMarkerSetNameEditText.getText().toString());
            }
            
            startService(intent);
            
            file = null;
            splitMarkerSetNameEditText.setText(null);
            fileNameTextView.setText(null);
            
            Toast.makeText(getApplicationContext(), "Import queued.",
                    Toast.LENGTH_SHORT).show();
            
            finish();
        }
    }
}
