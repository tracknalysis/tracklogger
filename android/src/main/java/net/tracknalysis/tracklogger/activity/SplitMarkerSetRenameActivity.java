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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.tracknalysis.tracklogger.R;
import net.tracknalysis.tracklogger.provider.TrackLoggerData;
import net.tracknalysis.tracklogger.provider.TrackLoggerDataUtil;
import android.content.ContentValues;
import android.database.Cursor;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

/**
 * Activity to rename a split marker set.
 *
 * @author David Valeri
 */
public class SplitMarkerSetRenameActivity extends BaseActivity {
    
    private static final Logger LOG = LoggerFactory.getLogger(SplitMarkerSetRenameActivity.class);
    
    private EditText splitMarkerSetNameEditText;
    private TextView promptText;
    private Button okButton;
    private Button cancelButton;
    private String currentName;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        if (!TrackLoggerData.SplitMarkerSet.ITEM_TYPE.equals(getContentResolver().getType(getIntent().getData()))) {
            LOG.error(
                    "Wrong content type for URI [{}].  Was expecting [{}] but found [{}].",
                    new Object[] {
                            TrackLoggerData.SplitMarkerSet.ITEM_TYPE,
                            getIntent().getData(), getContentResolver().getType(getIntent().getData())});
            
            onTerminalError(R.string.split_marker_set_rename_error_not_found);
        } else {
            Cursor cursor = null;
            
            try {
                cursor = getContentResolver().query(getIntent().getData(), null, null, null, null);
                
                if (cursor.getCount() != 1) {
                    LOG.error(
                            "Wrong number of split marker sets found for URI [{}].  Was expecting 1 but found [{}].",
                            getIntent().getData(), cursor.getCount());
                    
                    onTerminalError(R.string.split_marker_set_rename_error_not_found);
                } else {
                    cursor.moveToFirst();
                    
                    currentName = cursor
                            .getString(cursor
                                    .getColumnIndex(TrackLoggerData.SplitMarkerSet.COLUMN_NAME_NAME));

                    setContentView(R.layout.split_marker_set_rename);

                    splitMarkerSetNameEditText = (EditText) findViewById(R.id.split_marker_set_rename_name_edit_text);
                    promptText = (TextView) findViewById(R.id.split_marker_set_rename_prompt_text);

                    cancelButton = (Button) findViewById(R.id.split_marker_set_rename_cancel_button);
                    okButton = (Button) findViewById(R.id.split_marker_set_rename_ok_button);

                    okButton.setOnClickListener(new OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            onOkButtonClicked();
                        }
                    });

                    cancelButton.setOnClickListener(new OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            onCancelButtonClicked();
                        }
                    });

                    promptText.setText(getString(
                            R.string.split_marker_set_rename_prompt,
                            currentName));

                    splitMarkerSetNameEditText.setText(currentName);
                }
            } finally {
                if (cursor != null) {
                    cursor.close();
                }
            }   
        }
    }
    
    private void onOkButtonClicked() {
        String newName = splitMarkerSetNameEditText.getText().toString();
        
        if (!currentName.equals(newName)) {
        
            if (!TrackLoggerDataUtil.isValidSplitMarkerSetName(newName)) {
                LOG.error("[{}] is an invalid split marker set name.", newName);
                onNonTerminalError(R.string.split_marker_set_rename_error_invalid_name);
            } else {
                if (TrackLoggerDataUtil.isDuplicateSplitMarkerSetName(getContentResolver(), newName)) {
                    LOG.error("[{}] is a duplicate split marker set name.", newName);
                    onNonTerminalError(R.string.split_marker_set_rename_error_duplicate);
                } else {
                    ContentValues cvs = new ContentValues();
                    cvs.put(TrackLoggerData.SplitMarkerSet.COLUMN_NAME_NAME, newName);
                    int count = getContentResolver().update(getIntent().getData(), cvs, null, (String[]) null); 
                    if (count != 1) {
                        LOG.error("Updated the wrong number of rows.  Expecting 1 but got [{}]", count);
                        onNonTerminalError();
                    } else {
                        Toast.makeText(
                                getApplicationContext(),
                                getString(
                                        R.string.split_marker_set_rename_toast,
                                        currentName, newName),
                                Toast.LENGTH_LONG).show();
                        finish();
                    }
                }
            }
        } else {
            finish();
        }
    }
    
    private void onCancelButtonClicked() {
        finish();
    }
}
