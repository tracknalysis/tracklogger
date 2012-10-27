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
import android.annotation.TargetApi;
import android.app.Dialog;
import android.content.ContentProviderOperation;
import android.content.ContentProviderResult;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Intent;
import android.database.ContentObserver;
import android.database.Cursor;
import android.database.DataSetObserver;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MenuItem.OnMenuItemClickListener;
import android.view.MotionEvent;
import android.view.View;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.CompoundButton;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.ItemizedOverlay;
import com.google.android.maps.MapActivity;
import com.google.android.maps.MapController;
import com.google.android.maps.MapView;
import com.google.android.maps.OverlayItem;
import com.google.android.maps.Projection;

/**
 * Activity showing the split markers in a split marker set and allowing for the editing of the split markers in
 * the split marker set.
 *
 * @author David Valeri
 */
public class SplitMarkerSetViewActivity extends MapActivity {

    private static final Logger LOG = LoggerFactory
            .getLogger(SplitMarkerSetViewActivity.class);

    private final DialogManager dialogManager = new DialogManager();
    private MapView mapView;
    private SplitMarkerItemizedOverlay itemizedOverlay;
    private ToggleButton satelliteToggle;
    private int splitMarkerSetId;
    private boolean inUse;
    private Dialog confirmDialog;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (Intent.ACTION_EDIT.equals(getIntent().getAction())  
                || Intent.ACTION_VIEW.equals(getIntent().getAction())) {
            
            // If the action is edit or view, we expect to be working with an existing set.
            // so we pull it from the DB and populate the activity. 
            
            if (!TrackLoggerData.SplitMarkerSet.ITEM_TYPE
                    .equals(getContentResolver().getType(getIntent().getData()))) {
                LOG.error(
                        "Wrong content type for URI [{}].  Was expecting [{}] but found [{}].",
                        new Object[] {
                                getIntent().getData(),
                                TrackLoggerData.SplitMarkerSet.ITEM_TYPE,
                                getContentResolver().getType(getIntent().getData()) });
                dialogManager.onTerminalError(this, R.string.split_marker_set_view_error_not_found);
            } else {
                populateFromUri(getIntent().getData());
            }
            
        } else if (Intent.ACTION_INSERT.equals(getIntent().getAction())) {
            // If the action is insert, we need to create a new split marker set and then
            // load it as if we were viewing or editing it.
            LOG.error("[{}] is not a supported action yet.", Intent.ACTION_INSERT);

            ContentValues cvs = new ContentValues();
            cvs.put(TrackLoggerData.SplitMarkerSet.COLUMN_NAME_NAME,
                    TrackLoggerDataUtil
                            .createUniqueSplitMarkerSetName(
                                    getContentResolver(),
                                    getString(R.string.split_marker_set_view_default_new_split_marker_set_name)));
            Uri splitMarkerSetUri = getContentResolver().insert(
                    TrackLoggerData.SplitMarkerSet.CONTENT_URI, cvs);
            getIntent().setAction(Intent.ACTION_VIEW);
            getIntent().setData(splitMarkerSetUri);
            populateFromUri(splitMarkerSetUri);
            startActivity(new Intent(TrackLogger.ACTION_RENAME, splitMarkerSetUri));
        } else {
            // Unsupported action.  We only handle view, edit, or insert.
            LOG.error(
                    "Wrong action.  Was expecting [{}], [{}], or [{}] but found [{}].",
                    new Object[] {
                            Intent.ACTION_VIEW,
                            Intent.ACTION_EDIT,
                            Intent.ACTION_INSERT,
                            getIntent().getAction() });
    
            dialogManager.onTerminalError(this, R.string.split_marker_set_view_error_not_found);
        }
    }

    @Override
    protected void onDestroy() {
        dialogManager.onDestroy();
        if (confirmDialog != null) {
            confirmDialog.dismiss();
        }
        super.onDestroy();
    }

    @Override
    protected boolean isRouteDisplayed() {
        return false;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.split_marker_set_view_options_menu, menu);
        return true;
    }
    
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        for (int i = 0; i < menu.size(); i++) {
            MenuItem menuItem = menu.getItem(i);
            switch (menuItem.getItemId()) {
                case R.id.split_marker_set_view_menu_new:
                    menuItem.setEnabled(!inUse);
                    break;
            }
        }
        return true; 
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.split_marker_set_view_menu_new:
                itemizedOverlay.inAddMode = true;
                Toast.makeText(getApplicationContext(),
                        R.string.split_marker_set_view_new_marker_instruction,
                        Toast.LENGTH_LONG).show();
                return true;
            case R.id.split_marker_set_view_menu_zoom_to_show_all:
                zoomToShowAll();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
    
    /**
     * Sets up the activity based on the URI of an existing split marker set in the DB.
     *
     * @param uri the URI of the split marker set
     */
    @TargetApi(11)
    private void populateFromUri(Uri uri) {
        Cursor splitMarkerSetCursor = null;

        try {
            splitMarkerSetCursor = getContentResolver().query(
                    getIntent().getData(), null, null, null, null);

            if (splitMarkerSetCursor.getCount() != 1) {
                LOG.error(
                        "Wrong number of split marker sets found for URI [{}].  Was expecting 1 but found [{}].",
                        getIntent().getData(),
                        splitMarkerSetCursor.getCount());

                dialogManager.onTerminalError(this, R.string.split_marker_set_view_error_not_found);
            } else {
                splitMarkerSetCursor.moveToFirst();

                splitMarkerSetId = splitMarkerSetCursor
                        .getInt(splitMarkerSetCursor
                                .getColumnIndexOrThrow(TrackLoggerData.SplitMarkerSet._ID));

                Cursor splitMarkerCursor = managedQuery(
                        TrackLoggerData.SplitMarker.CONTENT_URI,
                        null,
                        TrackLoggerData.SplitMarker.COLUMN_NAME_SPLIT_MARKER_SET_ID
                                + " = ?",
                        new String[] { String
                                .valueOf(splitMarkerSetId) }, null);
                
                Cursor sessionCursor = null;
                try {
                    sessionCursor = getContentResolver().query(
                            TrackLoggerData.Session.CONTENT_URI,
                            null,
                            TrackLoggerData.Session.COLUMN_NAME_SPLIT_MARKER_SET_ID
                                    + " = ?",
                            new String[] { String.valueOf(splitMarkerSetId) },
                            null);
                    
                    inUse = sessionCursor.getCount() != 0;
                } finally {
                    if (sessionCursor != null) {
                        sessionCursor.close();
                    }
                }

                setContentView(R.layout.split_marker_set_view);
                
                mapView = (MapView) findViewById(R.id.split_marker_set_view_map_view);
                
                itemizedOverlay = new SplitMarkerItemizedOverlay(splitMarkerCursor);
                
                if (Build.VERSION.SDK_INT >= 11) {
                    // Large paths are not rendered when using hardware acceleration.
                    // Disable where possible in the API until the issue is resolved.
                    // http://code.google.com/p/android/issues/detail?id=24023
                    mapView.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
                }

                mapView.getOverlays().add(itemizedOverlay);

                satelliteToggle = (ToggleButton) findViewById(R.id.split_marker_set_view_satellite_toggle_button);
                satelliteToggle
                        .setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                            public void onCheckedChanged(
                                    CompoundButton buttonView,
                                    boolean isChecked) {
                                if (isChecked) {
                                    mapView.setSatellite(true);
                                } else {
                                    mapView.setSatellite(false);
                                }
                            }
                        });
                
                zoomToShowAll();
            }
        } finally {
            if (splitMarkerSetCursor != null) {
                splitMarkerSetCursor.close();
            }
        }
    }
    
    private void zoomToShowAll() {
        itemizedOverlay.zoomToShowAll();
    }

    /**
     * Overlay for drawing the split markers on the map and handling touch
     * events on the markers.
     */
    private final class SplitMarkerItemizedOverlay extends
            ItemizedOverlay<OverlayItem> implements
            View.OnCreateContextMenuListener {

        private final ArrayList<SplitMarkerOverlayItem> overlayItems = new ArrayList<SplitMarkerOverlayItem>();
        private final ContentObserver contentObserver;
        private final DataSetObserver dataSetObserver;
        private final Cursor cursor;

        private Drawable defaultMarker = null;
        private Drawable startFinishMarker = null;
        private int itemIndex = -1;
        private Point originalTouchPoint = null;
        private boolean inDrag = false;
        private int xDragTouchOffset = 0;
        private int yDragTouchOffset = 0;
        private int dragDeadzone = 10;
        private boolean inAddMode = false;

        public SplitMarkerItemizedOverlay(Cursor cursor) {
            super(boundCenterBottom(getResources().getDrawable(
                    R.drawable.split_marker_icon)));
            this.defaultMarker = boundCenterBottom(getResources().getDrawable(
                    R.drawable.split_marker_icon));
            this.startFinishMarker = boundCenterBottom(getResources().getDrawable(
                    R.drawable.split_marker_icon_start));
            contentObserver = new SplitMarkerItemOverlayContentObserver();
            dataSetObserver = new SplitMarkerItemOverlayDataSetObserver();
            this.cursor = cursor;
            this.cursor.registerContentObserver(contentObserver);
            this.cursor.registerDataSetObserver(dataSetObserver);
            update();
        }

        @Override
        public int size() {
            return overlayItems.size();
        }

        @Override
        public boolean onTouchEvent(MotionEvent event, MapView mapView) {
            final int action = event.getAction();
            final int x = (int) event.getX();
            final int y = (int) event.getY();
            boolean result = false;

            if (action == MotionEvent.ACTION_DOWN) {
                if (!inAddMode) {
                    int index = 0;
                    for (OverlayItem item : overlayItems) {
                        Point p = mapView.getProjection().toPixels(
                                item.getPoint(), null);

                        if (hitTest(item, defaultMarker, x - p.x, y - p.y)) {
                            result = true;
                            itemIndex = index;

                            xDragTouchOffset = x - p.x;
                            yDragTouchOffset = y - p.y;
                            originalTouchPoint = new Point(x, y);

                            // If this overlay is going to handle the event, we also
                            // want to handle context menus.
                            mapView.setOnCreateContextMenuListener(this);

                            break;
                        }
                        index++;
                    }
                } else {
                    result = true;
                }
            } else if (action == MotionEvent.ACTION_MOVE && itemIndex != -1) {
                if (!inDrag
                        && (Math.abs(x - originalTouchPoint.x) > dragDeadzone || Math
                                .abs(y - originalTouchPoint.y) > dragDeadzone)) {
                    inDrag = true;
                }

                if (inDrag) {
                    
                    if (!inUse) {
                        GeoPoint newGeoPoint = mapView.getProjection().fromPixels(
                                x - xDragTouchOffset, y - yDragTouchOffset);
                        move(itemIndex, newGeoPoint, false);
                    }
                }
                result = true;
            } else if (action == MotionEvent.ACTION_UP && itemIndex != -1) {
                if (inDrag) {
                    
                    if (!inUse) {
                        GeoPoint newGeoPoint = mapView.getProjection().fromPixels(
                                x - xDragTouchOffset, y - yDragTouchOffset);
                        move(itemIndex, newGeoPoint, true);
                    } else {
                        Toast.makeText(
                                mapView.getContext(),
                                getString(R.string.split_marker_set_view_marker_moved_while_in_use),
                                Toast.LENGTH_LONG).show();
                    }
                } else {
                    mapView.showContextMenu();
                    // After we render the context menu, we no longer want responsibility
                    // for the context menu on the map view.  There is no getter for the original
                    // listener if we clobbered it before so we just set it to null.
                    mapView.setOnCreateContextMenuListener(null);
                }
                originalTouchPoint = null;
                inDrag = false;
                xDragTouchOffset = 0;
                yDragTouchOffset = 0;
                itemIndex = -1;

                result = true;
            } else if (action == MotionEvent.ACTION_UP && inAddMode) {
                add(mapView.getProjection().fromPixels(x, y));
                inAddMode = false;
            }

            return (result || super.onTouchEvent(event, mapView));
        }

        @Override
        public void draw(Canvas canvas, MapView mapView, boolean shadow) {
            if (!shadow && overlayItems.size() > 0) {

                GeoPoint lastPoint = null;
                lastPoint = overlayItems.get(overlayItems.size() - 1)
                        .getPoint();
                for (OverlayItem overlayItem : overlayItems) {
                    GeoPoint thisPoint = overlayItem.getPoint();

                    Projection projection = mapView.getProjection();
                    Point startingPoint = projection.toPixels(lastPoint, null);
                    Point endingPoint = projection.toPixels(thisPoint, null);

                    Path path = new Path();
                    path.moveTo(startingPoint.x, startingPoint.y);
                    path.lineTo(endingPoint.x, endingPoint.y);

                    Paint paint = new Paint();
                    paint.setStyle(Paint.Style.STROKE);
                    paint.setColor(Color.RED);
                    paint.setStrokeWidth(3f);

                    canvas.drawPath(path, paint);

                    lastPoint = thisPoint;
                }
            }

            super.draw(canvas, mapView, shadow);
        }

        @Override
        public void onCreateContextMenu(ContextMenu menu, View v,
                ContextMenuInfo menuInfo) {
            
            final MapView mapView = (MapView) v;
            final SplitMarkerOverlayItem item = overlayItems.get(itemIndex);
            final int itemToDeleteIndex = itemIndex;
            
            MenuInflater inflater = new MenuInflater(mapView.getContext());
            inflater.inflate(R.menu.split_marker_set_view_item_menu, menu);
            menu.setHeaderTitle(item.getTitle() + " - " + item.getSnippet());

            for (int i = 0; i < menu.size(); i++) {
                MenuItem menuItem = menu.getItem(i);
                switch (menuItem.getItemId()) {
                    case R.id.split_marker_set_view_item_menu_delete:
                        if (size() <= 1 || inUse) {
                            menuItem.setEnabled(false);
                        } else {
                            menuItem.setOnMenuItemClickListener(new OnMenuItemClickListener() {
                                @Override
                                public boolean onMenuItemClick(MenuItem item) {
                                    confirmDelete(itemToDeleteIndex);
                                    return true;
                                }
                            });
                        }
                        break;
                    case R.id.split_marker_set_view_item_menu_edit:
                        menuItem.setIntent(new Intent(
                                Intent.ACTION_EDIT,
                                ContentUris.withAppendedId(
                                        TrackLoggerData.SplitMarker.CONTENT_URI,
                                        item.getSplitMarkerId())));
                        break;
                    default:
                        LOG.warn("Unknown menu item with ID [{}].",
                                menuItem.getItemId());
                }
            }
        }
        
        /**
         * Zooms and centers the map to show all markers in this layer.
         * 
         * Solution adapted from <a href="http://stackoverflow.com/questions/8254164/how-to-show-all-markers-in-mapview">http://stackoverflow.com/questions/8254164/how-to-show-all-markers-in-mapview</a>
         * under the CC BY-SA 3.0 license. 
         */
        public void zoomToShowAll() {
            
            if (size() > 0) {
                int minLat = Integer.MAX_VALUE;
                int minLon = Integer.MAX_VALUE;
                int maxLat = Integer.MIN_VALUE;
                int maxLon = Integer.MIN_VALUE;
    
                for (SplitMarkerOverlayItem item : overlayItems) {
                    
                    GeoPoint point = item.getPoint();
    
                    int lat = point.getLatitudeE6();
                    int lon = point.getLongitudeE6();
    
                    maxLat = Math.max(lat, maxLat);
                    minLat = Math.min(lat, minLat);
                    maxLon = Math.max(lon, maxLon);
                    minLon = Math.min(lon, minLon);
                 }
                
                MapController controller = mapView.getController();
                
                controller.zoomToSpan(Math.abs(maxLat - minLat), Math.abs(maxLon - minLon));
                controller.animateTo(new GeoPoint((maxLat + minLat) / 2, (maxLon + minLon) / 2 ));
            }
        }

        @Override
        protected OverlayItem createItem(int i) {
            return overlayItems.get(i);
        }
        
        private void update() {
            overlayItems.clear();
            
            cursor.moveToFirst();
            while (!cursor.isAfterLast()) {
                double lat = cursor.getDouble(cursor.getColumnIndexOrThrow(
                        TrackLoggerData.SplitMarker.COLUMN_NAME_LATITUDE));
                double lon = cursor.getDouble(cursor.getColumnIndexOrThrow(
                        TrackLoggerData.SplitMarker.COLUMN_NAME_LONGITUDE));
                String name = cursor.getString(cursor.getColumnIndexOrThrow(
                        TrackLoggerData.SplitMarker.COLUMN_NAME_NAME));
                int ordinal = cursor.getInt(cursor.getColumnIndexOrThrow(
                        TrackLoggerData.SplitMarker.COLUMN_NAME_ORDER_INDEX));
                int id = cursor.getInt(cursor.getColumnIndexOrThrow(
                        TrackLoggerData.SplitMarker._ID));
            
                GeoPoint geopoint = new GeoPoint((int) (lat * 1E6), (int) (lon * 1E6));
                    
                Drawable marker = cursor.isFirst() ? startFinishMarker : defaultMarker;
                      
                overlayItems.add(new SplitMarkerOverlayItem(
                        geopoint, 
                        getString(R.string.split_marker_set_view_marker_title, ordinal + 1),
                        name,
                        marker,
                        id));

                cursor.move(1);
            }
            
            populate();
            
            mapView.invalidate();
        }
        
        private void handleError() {
            dialogManager.onNonTerminalError(SplitMarkerSetViewActivity.this);
            // Restore the view from the data source to eliminate any failed state from the user.
            cursor.requery();
            update();
        }
        
        private void move(int itemToMoveIndex, GeoPoint newPoint, boolean save) {
            
            SplitMarkerOverlayItem oldItem = overlayItems.get(itemToMoveIndex);
            
            if (save) {
                // If saving, don't bother redrawing the marker in the last location as the update will
                // trigger an automatic refresh through the content listener.
                ContentValues cv = new ContentValues();
                cv.put(TrackLoggerData.SplitMarker.COLUMN_NAME_LATITUDE, newPoint.getLatitudeE6() * 1e-6);
                cv.put(TrackLoggerData.SplitMarker.COLUMN_NAME_LONGITUDE, newPoint.getLongitudeE6() * 1e-6);
                
                Uri splitMarkerUri = ContentUris.withAppendedId(
                        TrackLoggerData.SplitMarker.CONTENT_URI,
                        oldItem.getSplitMarkerId());
                
                try {
                    int rows = getContentResolver().update(
                            splitMarkerUri,
                            cv,
                            null,
                            null);
                    
                    if (rows != 1) {
                        LOG.error(
                                "Error moving split marker with URI [{}].  Wrong number of rows affected",
                                splitMarkerUri);
                        handleError();
                    } else {
                        String name = oldItem.getSnippet() != null ? oldItem.getSnippet() : oldItem.getTitle();
                        Toast.makeText(
                                mapView.getContext(),
                                getString(
                                        R.string.split_marker_set_view_marker_moved_notification,
                                        name), Toast.LENGTH_SHORT).show();
                    }
                } catch (Exception e) {
                    LOG.error("Error moving split marker with URI [" + splitMarkerUri + "].", e);
                    handleError();
                }
            } else {
                Drawable marker = itemToMoveIndex == 0 ? startFinishMarker : defaultMarker;
                SplitMarkerOverlayItem newItem = new SplitMarkerOverlayItem(
                        newPoint, oldItem.getTitle(),
                        oldItem.getSnippet(), marker, oldItem.getSplitMarkerId());

                overlayItems.set(itemIndex, newItem);
                populate();
            }
        }
        
        private void add(GeoPoint point) {
            ContentValues cv = new ContentValues();
            cv.put(TrackLoggerData.SplitMarker.COLUMN_NAME_SPLIT_MARKER_SET_ID, splitMarkerSetId);
            cv.put(TrackLoggerData.SplitMarker.COLUMN_NAME_ORDER_INDEX, size());
            cv.put(TrackLoggerData.SplitMarker.COLUMN_NAME_LATITUDE, point.getLatitudeE6() * 1e-6);
            cv.put(TrackLoggerData.SplitMarker.COLUMN_NAME_LONGITUDE, point.getLongitudeE6() * 1e-6);
            
            try {
                getContentResolver().insert(TrackLoggerData.SplitMarker.CONTENT_URI, cv);
                
                Toast.makeText(
                        mapView.getContext(),
                        getString(
                                R.string.split_marker_set_view_marker_added_notification,
                                size()), Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                LOG.error("Error adding split marker.", e);
                handleError();
            }
        }
        
        private void confirmDelete(final int itemToDeleteIndex) {
            SplitMarkerOverlayItem item = overlayItems.get(itemToDeleteIndex);
            confirmDialog = dialogManager.createConfirmDialog(
                    SplitMarkerSetViewActivity.this,
                    new Runnable() {
                        @Override
                        public void run() {
                            delete(itemToDeleteIndex);
                        }
                    },
                    null,
                    R.string.app_name,
                    R.string.split_marker_set_view_confirm_delete_marker_prompt,
                    new Object[] { item.getSnippet() != null ? item.getSnippet() : item.getTitle() });
        }
        
        private void delete(int itemToDeleteIndex) {
            final SplitMarkerOverlayItem itemToDelete = overlayItems.get(itemToDeleteIndex);
            
            final Uri splitMarkerToDeleteUri = ContentUris
                    .withAppendedId(
                            TrackLoggerData.SplitMarker.CONTENT_URI,
                            itemToDelete.getSplitMarkerId());
            
            ArrayList<ContentProviderOperation> operations = 
                    new ArrayList<ContentProviderOperation>();

            try {
                
                operations.add(ContentProviderOperation.newDelete(splitMarkerToDeleteUri).build());
                                
                for (int i = itemToDeleteIndex; i < size(); i++) {
                    SplitMarkerOverlayItem itemToReorder = overlayItems.get(i);
                    
                    Uri splitMarkerToReorderUri = ContentUris
                            .withAppendedId(
                                    TrackLoggerData.SplitMarker.CONTENT_URI,
                                    itemToReorder.getSplitMarkerId());
                    
                    operations.add(ContentProviderOperation
                            .newUpdate(splitMarkerToReorderUri)
                            .withValue(TrackLoggerData.SplitMarker.COLUMN_NAME_ORDER_INDEX, i - 1)
                            .build());
                }
                
                ContentProviderResult[] results = getContentResolver()
                        .applyBatch(TrackLoggerData.AUTHORITY, operations);

                if (results.length != operations.size()) {
                    LOG.error(
                            "Error deleting split marker with URI [{}] and reordring remaining markers.  " +
                                    "Wrong number of rows affected.  Operations were [{}].",
                            splitMarkerToDeleteUri,
                            operations);
                    handleError();
                } else {
                    Toast.makeText(
                            mapView.getContext(),
                            mapView.getContext().getString(
                                    R.string.split_marker_set_view_marker_deleted_notification,
                                    new Object[] { itemToDelete.getTitle() }),
                            Toast.LENGTH_SHORT).show();
                }
            } catch (Exception e) {
                LOG.error(
                        "Error deleting split marker with URI [" + splitMarkerToDeleteUri
                                + "].  Operations were [" + operations + "].",
                        e);
                handleError();
            }
        }
        
        private class SplitMarkerItemOverlayDataSetObserver extends DataSetObserver {
            @Override
            public void onChanged() {
                // TODO: What to do here?
            }
            
            @Override
            public void onInvalidated() {
                // TODO: What to do here?
            }
        }
        
        private class SplitMarkerItemOverlayContentObserver extends ContentObserver {

            public SplitMarkerItemOverlayContentObserver() {
                super(new Handler());
            }
            
            @Override
            public boolean deliverSelfNotifications() {
                return true;
            }
            
            @Override
            public void onChange(boolean selfChange) {
                cursor.requery();
                update();
            }
        }
    }
    
    /**
     * Overlay item that also keeps track of the underlying split marker ID in the content resolver.
     */
    public static class SplitMarkerOverlayItem extends OverlayItem {
        
        private final int splitMarkerId;
        private Drawable marker;

        
        public SplitMarkerOverlayItem(GeoPoint point, String title,
                String snippet, Drawable marker, int splitMarkerId) {
            super(point, title, snippet);
            this.splitMarkerId = splitMarkerId;
            this.marker = marker;
        }      
        
        public int getSplitMarkerId() {
            return splitMarkerId;
        }
        
        @Override
        public Drawable getMarker(int arg0) {
            return marker;
        }
    }
}
