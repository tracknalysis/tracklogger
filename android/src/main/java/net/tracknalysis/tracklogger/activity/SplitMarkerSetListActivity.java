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

import java.util.ArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.tracknalysis.tracklogger.R;
import net.tracknalysis.tracklogger.TrackLogger;
import net.tracknalysis.tracklogger.provider.TrackLoggerData;
import net.tracknalysis.tracklogger.provider.TrackLoggerDataUtil;
import android.app.Dialog;
import android.content.ContentProviderOperation;
import android.content.ContentUris;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.Toast;

/**
 * Activity showing all known split marker sets and allowing for general actions on split marker sets.
 *
 * @author David Valeri
 */
public class SplitMarkerSetListActivity extends BaseListActivity {
    
    private static final Logger LOG = LoggerFactory.getLogger(SplitMarkerSetListActivity.class);
    
    protected static final int CONTEXT_MENU_ITEM_CONFIRM_DIALOG_ID = 0;
    protected static final int ERROR_DIALOG_ID = 1;
    
    private Dialog contextMenuItemConfirmDialog;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        if (!Intent.ACTION_VIEW.equals(getIntent().getAction())  && !Intent.ACTION_PICK.equals(getIntent().getAction())) {
            LOG.error("Unsupported action.  Expected [{}] or [{}] but received [{}].", new Object[] {
                    Intent.ACTION_VIEW,
                    Intent.ACTION_PICK,
                    getIntent().getAction()});
            
            onTerminalError();
        } else {
        
            Cursor splitMarkerSetCursor = managedQuery(
                    TrackLoggerData.SplitMarkerSet.CONTENT_URI, null, null, null,
                    null);
            
            SimpleCursorAdapter adapter = new SimpleCursorAdapter(
                    this,
                    R.layout.split_marker_set_list_item,
                    splitMarkerSetCursor,
                    new String[] {
                            TrackLoggerData.SplitMarkerSet._ID,
                            TrackLoggerData.SplitMarkerSet.COLUMN_NAME_NAME},
                    new int[] {
                            R.id.splitMarkerSetId,
                            R.id.splitMarkerSetName});
            
            setContentView(R.layout.split_marker_set_list);
            setListAdapter(adapter);
            
            getListView().setOnCreateContextMenuListener(this);
        }
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        
        if (Intent.ACTION_PICK.equals(getIntent().getAction())) {
            Toast.makeText(
                    getApplicationContext(),
                    getString(R.string.split_marker_set_list_pick_instructions),
                    Toast.LENGTH_LONG).show();
        }
    }
    
    @Override
    protected void onDestroy() {
        if (contextMenuItemConfirmDialog != null) {
            contextMenuItemConfirmDialog.dismiss();
        }
        
        super.onDestroy();
    }
    
    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        Intent intent = new Intent(Intent.ACTION_VIEW, createSplitMarkerSetUri((int) id));
        if (getIntent().getAction() == Intent.ACTION_VIEW) {
            startActivity(intent);
        } else if (getIntent().getAction() == Intent.ACTION_PICK) {
            setResult(RESULT_OK, intent);
            finish();
        }
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.split_marker_set_list_options_menu, menu);
        return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.split_marker_set_list_menu_import:
                startActivity(new Intent(TrackLogger.ACTION_SPLIT_MARKER_SET_CONFIGURE_IMPORT));
                return true;
            case R.id.split_marker_set_list_menu_new:
                startActivity(new Intent(Intent.ACTION_INSERT, TrackLoggerData.SplitMarkerSet.CONTENT_URI));
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
    
    @Override
    public void onCreateContextMenu(ContextMenu menu, View view, ContextMenuInfo menuInfo) {
        
        AdapterView.AdapterContextMenuInfo info;
        try {
            info = (AdapterView.AdapterContextMenuInfo) menuInfo;
        } catch (ClassCastException e) {
            LOG.error("Could not cast [{}] to [{}].", menuInfo,
                    AdapterView.AdapterContextMenuInfo.class);
            return;
        }

        Cursor cursor = (Cursor) getListAdapter().getItem(info.position);
        if (cursor == null) {
            LOG.error("The cursor is not available from the list adapter.");
            return;
        }

        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.split_marker_set_list_item_menu, menu);

        String splitMarkerSetName = cursor.getString(cursor
                .getColumnIndex(TrackLoggerData.SplitMarkerSet.COLUMN_NAME_NAME));
        
        menu.setHeaderTitle(splitMarkerSetName);
        
        for (int i = 0; i < menu.size(); i++) {
            MenuItem menuItem = menu.getItem(i);
            switch (menuItem.getItemId()) {
                case R.id.split_marker_set_list_item_menu_delete:
                    try {
                        info = (AdapterView.AdapterContextMenuInfo) menuItem.getMenuInfo();

                        if (isInUse((int) info.id)) {
                            menuItem.setEnabled(false);
                        }
                    } catch (ClassCastException e) {
                        LOG.error("Could not cast [{}] to [{}].", menuItem.getMenuInfo(),
                                AdapterView.AdapterContextMenuInfo.class);
                        menuItem.setEnabled(false);
                    }       
            }
        }
    }
    
    @Override
    public boolean onContextItemSelected(MenuItem item) {
        
        AdapterView.AdapterContextMenuInfo info;

        try {
            info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        } catch (ClassCastException e) {
            LOG.error("Could not cast [{}] to [{}].", item.getMenuInfo(),
                    AdapterView.AdapterContextMenuInfo.class);
            return false;
        }
        
        Cursor cursor = (Cursor) getListAdapter().getItem(info.position);
        if (cursor == null) {
            LOG.error("The cursor is not available from the list adapter.");
            return false;
        }
        
        String splitMarkerSetName = cursor.getString(cursor
                .getColumnIndex(TrackLoggerData.SplitMarkerSet.COLUMN_NAME_NAME));
        
        switch (item.getItemId()) {
            case R.id.split_marker_set_list_item_menu_delete:
                confirmDelete((int) info.id, splitMarkerSetName);
                return true;
            case R.id.split_marker_set_list_item_menu_duplicate:
                duplicate((int) info.id);
                return true;
            case R.id.split_marker_set_list_item_menu_rename:
                startActivity(new Intent(TrackLogger.ACTION_RENAME,
                        createSplitMarkerSetUri((int) info.id)));
                return true;
            case R.id.split_marker_set_list_item_menu_view:
                startActivity(new Intent(Intent.ACTION_VIEW,
                        createSplitMarkerSetUri((int) info.id)));
                return true;
            default:
                return super.onContextItemSelected(item);
        }
    }
    
    private boolean isInUse(int splitMarkerSetId) {
        Cursor sessionCursor = null;
        try {
            sessionCursor = getContentResolver().query(
                    TrackLoggerData.Session.CONTENT_URI,
                    null,
                    TrackLoggerData.Session.COLUMN_NAME_SPLIT_MARKER_SET_ID
                            + " = ?",
                    new String[] { String.valueOf(splitMarkerSetId) },
                    null);
            
            return sessionCursor.getCount() != 0;
        } finally {
            if (sessionCursor != null) {
                sessionCursor.close();
            }
        }
    }
    
    private Uri createSplitMarkerSetUri(int id) {
        return ContentUris.withAppendedId(TrackLoggerData.SplitMarkerSet.CONTENT_URI, id);
    }
    
    private void duplicate(int id) {
        Uri splitMarkerSetUri = createSplitMarkerSetUri(id);
        
        final ArrayList<ContentProviderOperation> operations = new ArrayList<ContentProviderOperation>();
        
        
        Cursor splitMarkerSetCursor = null;
        Cursor splitMarkerCursor = null;
        
        try {
            splitMarkerSetCursor = getContentResolver().query(
                    splitMarkerSetUri, null, null, null, null);
            
            if (splitMarkerSetCursor.getCount() != 1) {
                LOG.error(
                        "Wrong number of split marker sets found.  Expected 1 but found [{}].",
                        splitMarkerSetCursor.getCount());
                onNonTerminalError();
            } else {
                splitMarkerSetCursor.moveToFirst();
                final String name = splitMarkerSetCursor
                        .getString(splitMarkerSetCursor
                                .getColumnIndex(TrackLoggerData.SplitMarkerSet.COLUMN_NAME_NAME));
                
                String newName = TrackLoggerDataUtil
                        .createUniqueSplitMarkerSetName(getContentResolver(),
                                name + " Copy");
                
                if (newName == null) {
                    onNonTerminalError();
                } else if (!TrackLoggerDataUtil.isValidSplitMarkerSetName(newName)) {
                    LOG.error("[{}] is an invalid split marker set name.", newName);
                    onNonTerminalError();
                } else {
                    operations.add(ContentProviderOperation
                            .newInsert(
                                    TrackLoggerData.SplitMarkerSet.CONTENT_URI)
                            .withValue(
                                    TrackLoggerData.SplitMarkerSet.COLUMN_NAME_NAME,
                                    newName).build());
                    
                    splitMarkerCursor = getContentResolver().query(
                            TrackLoggerData.SplitMarker.CONTENT_URI,
                            null,
                            TrackLoggerData.SplitMarker.COLUMN_NAME_SPLIT_MARKER_SET_ID
                                    + " = ?",
                            new String[] { String
                                    .valueOf(id) }, null);
                    
                    splitMarkerCursor.moveToFirst();
                    
                    final int nameIndex = splitMarkerCursor
                            .getColumnIndex(TrackLoggerData.SplitMarker.COLUMN_NAME_NAME);
                    final int latIndex = splitMarkerCursor
                            .getColumnIndex(TrackLoggerData.SplitMarker.COLUMN_NAME_LATITUDE);
                    final int lonIndex = splitMarkerCursor
                            .getColumnIndex(TrackLoggerData.SplitMarker.COLUMN_NAME_LONGITUDE);
                    
                    while(!splitMarkerCursor.isAfterLast()) {
                        operations.add(ContentProviderOperation.newInsert(TrackLoggerData.SplitMarker.CONTENT_URI)
                                // Back reference to the set insertion
                                .withValueBackReference(TrackLoggerData.SplitMarker.COLUMN_NAME_SPLIT_MARKER_SET_ID, 0)
                                // Index can be size - 1 because the set is always the first element in the list
                                .withValue(TrackLoggerData.SplitMarker.COLUMN_NAME_ORDER_INDEX, operations.size() - 1)
                                .withValue(TrackLoggerData.SplitMarker.COLUMN_NAME_NAME,
                                        splitMarkerCursor.getString(nameIndex))
                                .withValue(TrackLoggerData.SplitMarker.COLUMN_NAME_LATITUDE,
                                        splitMarkerCursor.getDouble(latIndex))
                                .withValue(TrackLoggerData.SplitMarker.COLUMN_NAME_LONGITUDE,
                                        splitMarkerCursor.getDouble(lonIndex))
                                .build());
                        
                        splitMarkerCursor.move(1);
                    }
                    
                    getContentResolver().applyBatch(TrackLoggerData.AUTHORITY, operations);
                    
                    Toast.makeText(
                            this,
                            getString(
                                    R.string.split_marker_set_list_duplicated_notification,
                                    name, newName), Toast.LENGTH_LONG).show();
                }
                
            }
        } catch (Exception e) {
            LOG.error(
                    "Error applying batch operations for split marker set clone.  Operations included ["
                            + operations + "].", e);
            onNonTerminalError();
        } finally {
            if (splitMarkerSetCursor != null) {
                splitMarkerSetCursor.close();
            }
            
            if (splitMarkerCursor != null) {
                splitMarkerCursor.close();
            }
        }
    }
    
    private void confirmDelete(final int splitMarkerSetId, final String splitMarkerSetName) {
        if (contextMenuItemConfirmDialog != null) {
            contextMenuItemConfirmDialog.dismiss();
        }
        
        contextMenuItemConfirmDialog = getDialogManager().createConfirmDialog(
                this,
                new Runnable() {
                    @Override
                    public void run() {
                        delete(splitMarkerSetId, splitMarkerSetName);
                    }
                },
                null,
                R.string.app_name,
                R.string.split_marker_set_list_confirm_delete_prompt,
                new Object[] { splitMarkerSetName });
        
        contextMenuItemConfirmDialog.show();
    }
    
    private void delete(final int splitMarkerSetId, final String splitMarkerSetName) {
        final Uri splitMarkerSetUri = createSplitMarkerSetUri(splitMarkerSetId);
        
        try {
            int count = getContentResolver().delete(splitMarkerSetUri, null, null);
            
            if (count != 1) {
                LOG.error(
                        "Deleted wrong number of rows.  Expecting 1 but got [{}].",
                        count);
                onNonTerminalError();
            } else {
                Toast.makeText(
                        getApplicationContext(),
                        getString(
                                R.string.split_marker_set_list_deleted_notification,
                                new Object[] { splitMarkerSetName }),
                        Toast.LENGTH_LONG).show();
            }
        } catch (Exception e) {
            LOG.error(
                    "Error deleting session URI [" + splitMarkerSetUri + "].",
                    e);
            onNonTerminalError();
        }
    }
}
