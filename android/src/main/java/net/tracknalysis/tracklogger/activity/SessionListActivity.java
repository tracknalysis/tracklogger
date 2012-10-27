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

import java.text.DateFormat;
import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.tracknalysis.tracklogger.R;
import net.tracknalysis.tracklogger.TrackLogger;
import net.tracknalysis.tracklogger.provider.TrackLoggerData;
import net.tracknalysis.tracklogger.provider.TrackLoggerDataUtil;
import android.app.Dialog;
import android.content.ContentUris;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.Toast;

/**
 * @author David Valeri
 */
public class SessionListActivity extends BaseListActivity {
    
    private static final Logger LOG = LoggerFactory.getLogger(SessionListActivity.class);
    
    private Dialog contextMenuItemConfirmDialog;
    private DateFormat dateFormat;
    private DateFormat timeFormat;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        dateFormat = android.text.format.DateFormat.getMediumDateFormat(this);
        timeFormat = android.text.format.DateFormat.getTimeFormat(this);
        
        String[] projection = new String[] {
                TrackLoggerData.Session._ID,
                TrackLoggerData.Session.COLUMN_NAME_START_DATE,
                TrackLoggerData.Session.COLUMN_NAME_LAST_MODIFIED_DATE};
        
        Cursor cursor = managedQuery(TrackLoggerData.Session.CONTENT_URI,
                projection, null, null, TrackLoggerData.Session.COLUMN_NAME_START_DATE + " ASC");
        
        String[] from = new String[] {
                TrackLoggerData.Session._ID,
                TrackLoggerData.Session.COLUMN_NAME_START_DATE};
        
        int[] to = new int[] {
                R.id.sessionId,
                R.id.createdDate};
        
        SimpleCursorAdapter adapter = new SimpleCursorAdapter(
                  this, R.layout.session_list_item, cursor, from, to);

        adapter.setViewBinder(new SimpleCursorAdapter.ViewBinder() {
            @Override
            public boolean setViewValue(View view, Cursor cursor, int columnIndex) {
                int startTimeColumnIndex = cursor.getColumnIndex(TrackLoggerData.Session.COLUMN_NAME_START_DATE);
                boolean bound = false;
                
                if (startTimeColumnIndex == columnIndex) {
                    String startDateString = formatSqlDateString(cursor.getString(startTimeColumnIndex));
                    
                    if (view instanceof TextView) {
                        ((TextView) view).setText(startDateString);
                    } else {
                        throw new IllegalStateException("This binder can only bind the start time to a text view.");
                    }
                    
                    bound = true;
                }
                
                return bound;
            }
        });
        
        setContentView(R.layout.session_list);
        setListAdapter(adapter);
        
        getListView().setOnCreateContextMenuListener(this);
    }
    
    @Override
    protected void onDestroy() {
        if (contextMenuItemConfirmDialog != null) {
            contextMenuItemConfirmDialog.dismiss();
        }
        super.onDestroy();
    }
    
    @Override
    public void onCreateContextMenu(ContextMenu menu, View view, ContextMenuInfo menuInfo) {
        
        AdapterView.AdapterContextMenuInfo info;
        try {
            info = (AdapterView.AdapterContextMenuInfo) menuInfo;
        } catch (ClassCastException e) {
            LOG.error("Could not cast {} to {}.", menuInfo,
                    AdapterView.AdapterContextMenuInfo.class);
            return;
        }

        Cursor cursor = (Cursor) getListAdapter().getItem(info.position);
        if (cursor == null) {
            LOG.error("The cursor is not available from the list adapter.");
            return;
        }

        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.session_list_item_menu, menu);

        menu.setHeaderTitle(getString(R.string.session_list_context_menu_title, cursor.getInt(0)));
    }
    
    @Override
    public boolean onContextItemSelected(MenuItem item) {
        
        AdapterView.AdapterContextMenuInfo info;

        try {
            info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        } catch (ClassCastException e) {
            LOG.error("Could not cast {} to {}.", item.getMenuInfo(),
                    AdapterView.AdapterContextMenuInfo.class);
            return false;
        }
        
        Cursor cursor = (Cursor) getListAdapter().getItem(info.position);
        if (cursor == null) {
            LOG.error("The cursor is not available from the list adapter.");
            return false;
        }
        
        Uri sessionUri = createSessionUri((int) info.id);
        
        switch (item.getItemId()) {
            case R.id.session_list_item_menu_view:
                // TODO view session activity
                return true;
            case R.id.session_list_item_menu_export:
                startActivity(new Intent(TrackLogger.ACTION_SESSION_CONFIGURE_EXPORT, sessionUri));
                return true;
            case R.id.session_list_item_menu_delete:
                String sqlStartDateString = cursor.getString(cursor
                        .getColumnIndex(TrackLoggerData.Session.COLUMN_NAME_START_DATE));
                confirmDelete((int) info.id, formatSqlDateString(sqlStartDateString));
                return true;
            default:
                return super.onContextItemSelected(item);
        }
    }
    
    private Uri createSessionUri(int id) {
        return ContentUris.withAppendedId(TrackLoggerData.Session.CONTENT_URI, id);
    }
    
    private String formatSqlDateString(String sqlDateString) {
        Date startDate = TrackLoggerDataUtil.parseSqlDate(sqlDateString);
        return dateFormat.format(startDate) + " " + timeFormat.format(startDate);
    }
    
    private void confirmDelete(final int sessionId, final String sessionStartDate) {
        if (contextMenuItemConfirmDialog != null) {
            contextMenuItemConfirmDialog.dismiss();
        }
        
        contextMenuItemConfirmDialog = getDialogManager().createConfirmDialog(
                this,
                new Runnable() {
                    @Override
                    public void run() {
                        delete(sessionId, sessionStartDate);
                    }
                },
                null,
                R.string.app_name,
                R.string.session_list_confirm_delete_prompt,
                new Object[] { sessionId, sessionStartDate });
        
        contextMenuItemConfirmDialog.show();
    }
    
    private void delete(int sessionId, String sessionStartDate) {
        final Uri sessionUri = createSessionUri(sessionId);
        try {
            int count = getContentResolver().delete(sessionUri, null, null);
            
            if (count != 1) {
                LOG.error(
                        "Deleted wrong number of rows.  Expecting 1 but got [{}].",
                        count);
                onNonTerminalError();
            } else {
                Toast.makeText(
                        this,
                        getString(R.string.session_list_deleted_notification,
                                sessionId, sessionStartDate),
                        Toast.LENGTH_LONG).show();
            }
        } catch (Exception e) {
            LOG.error(
                    "Error deleting session URI [" + sessionUri + "].",
                    e);
            onNonTerminalError();
        }
    }
}
