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
 * Activity to edit the details of a split marker.
 *
 * @author David Valeri
 */
public class SplitMarkerEditActivity extends BaseActivity {
    
    private static final Logger LOG = LoggerFactory.getLogger(SplitMarkerEditActivity.class);
    
    private EditText splitMarkerNameEditText;
    private TextView promptText;
    private Button okButton;
    private Button cancelButton;
    private String currentName;
    private int splitMarkerSetId;
    private int orderIndex;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        if (!TrackLoggerData.SplitMarker.ITEM_TYPE.equals(getContentResolver().getType(getIntent().getData()))) {
            LOG.error(
                    "Wrong content type for URI [{}].  Was expecting [{}] but found [{}].",
                    new Object[] {
                            TrackLoggerData.SplitMarkerSet.ITEM_TYPE,
                            getIntent().getData(), getContentResolver().getType(getIntent().getData())});
            
            onTerminalError(R.string.split_marker_edit_error_not_found);
        } else {
            Cursor cursor = null;
            
            try {
                cursor = getContentResolver().query(getIntent().getData(), null, null, null, null);
                
                if (cursor.getCount() != 1) {
                    LOG.error(
                            "Wrong number of split markers found for URI [{}].  Was expecting 1 but found [{}].",
                            getIntent().getData(), cursor.getCount());
                    
                    onTerminalError(R.string.split_marker_edit_error_not_found);
                } else {
                    cursor.moveToFirst();
                    
                    currentName = cursor
                            .getString(cursor
                                    .getColumnIndex(TrackLoggerData.SplitMarker.COLUMN_NAME_NAME));
                    
                    splitMarkerSetId = cursor
                            .getInt(cursor
                                    .getColumnIndex(TrackLoggerData.SplitMarker.COLUMN_NAME_SPLIT_MARKER_SET_ID));
                    
                    orderIndex = cursor
                            .getInt(cursor
                                    .getColumnIndex(TrackLoggerData.SplitMarker.COLUMN_NAME_ORDER_INDEX));

                    setContentView(R.layout.split_marker_edit);

                    promptText = (TextView) findViewById(R.id.split_marker_edit_prompt_text);
                    splitMarkerNameEditText = (EditText) findViewById(R.id.split_marker_edit_name_edit_text);
                    cancelButton = (Button) findViewById(R.id.split_marker_edit_cancel_button);
                    okButton = (Button) findViewById(R.id.split_marker_edit_ok_button);

                    promptText.setText(getString(
                            R.string.split_marker_edit_prompt,
                            currentName));
                    
                    splitMarkerNameEditText.setText(currentName);
                    
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
                }
            } finally {
                if (cursor != null) {
                    cursor.close();
                }
            }   
        }
    }
    
    private void onOkButtonClicked() {
        String newName = splitMarkerNameEditText.getText().toString();
        
        boolean valid = true;
        
        if(!isNameValid(newName)) {
            LOG.error("[{}] is an invalid split marker name.", newName);
            
            onNonTerminalError(R.string.split_marker_edit_error_invalid_name);
            
            valid = false;
        } else if (isNameDuplicate(newName)) {
            LOG.error("[{}] is a duplicate split marker name.", newName);
            
            onNonTerminalError(R.string.split_marker_edit_error_duplicate_name);
            
            valid = false;
        }
        
        if (valid) {
            if (newName.length() == 0) {
                newName = null;
            }
            
            ContentValues cvs = new ContentValues();
            cvs.put(TrackLoggerData.SplitMarkerSet.COLUMN_NAME_NAME, newName);
            int count = getContentResolver().update(getIntent().getData(), cvs, null, (String[]) null); 
            
            if (count != 1) {
                LOG.error("Updated the wrong number of rows.  Expecting 1 but got [{}]", count);
                
                onTerminalError();
            } else {
                int resourceId = R.string.split_marker_edit_success_notification;
                Object[] args = new Object[] {newName};
                
                if (newName == null) {
                    resourceId = R.string.split_marker_edit_success_no_name_notification;
                    args = new Object[] {orderIndex + 1};
                }
                    
                Toast.makeText(
                        getApplicationContext(),
                        getString(resourceId, args),
                        Toast.LENGTH_LONG).show();
                finish();
            }
        }
    }
    
    private void onCancelButtonClicked() {
        finish();
    }
    
    private boolean isNameValid(String newName) {
        return !(newName.length() > 50 || newName.matches("^[\\s]+$"));
    }
    
    private boolean isNameDuplicate(String newName) {
        Cursor cursor = null;
        try {
     
            cursor = getContentResolver().query(
                    TrackLoggerData.SplitMarker.CONTENT_URI,
                    null,
                    TrackLoggerData.SplitMarker.COLUMN_NAME_NAME + " = ? AND " 
                            + TrackLoggerData.SplitMarker.COLUMN_NAME_SPLIT_MARKER_SET_ID + " = ?",
                    new String[] {newName, String.valueOf(splitMarkerSetId)},
                    null);
            
            return cursor.moveToFirst();
        } catch (RuntimeException e) {
            LOG.error("Error checking for duplicate split marker name.", e);
            return true;
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }
}
