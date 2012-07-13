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
import net.tracknalysis.tracklogger.TrackLogger;
import net.tracknalysis.tracklogger.provider.TrackLoggerData;
import android.app.ListActivity;
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
import android.widget.Toast;

/**
 * @author David Valeri
 */
public class SessionListActivity extends ListActivity {
    
    private static final Logger LOG = LoggerFactory.getLogger(SessionListActivity.class);
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
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

        setListAdapter(adapter);
        
        getListView().setOnCreateContextMenuListener(this);
        
        if (cursor.getCount() == 0) {
            Toast.makeText(getApplicationContext(), "No stored sessions.",
                    Toast.LENGTH_SHORT).show();
        }
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

        menu.setHeaderTitle("Session " + cursor.getString(0));
    }
    
    @Override
    public boolean onContextItemSelected(MenuItem item) {
        
        AdapterView.AdapterContextMenuInfo info;

        try {
            info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        } catch (ClassCastException e) {
            // TODO Log
            return false;
        }

        Uri sessionUri = ContentUris.withAppendedId(TrackLoggerData.Session.CONTENT_URI, info.id);
        
        switch (item.getItemId()) {
            case R.id.launchSessionView:
                // TODO view session activity
                return true;
            case R.id.launchSessionExport:
                
                startActivity(new Intent(TrackLogger.ACTION_SESSION_EXPORT_CONFIG, sessionUri));
                // TODO export session activity
                return true;
            case R.id.launchSessionDelete:
                // TODO delete session activity
                return true;
            default:
                return super.onContextItemSelected(item);
        }
    }
}
