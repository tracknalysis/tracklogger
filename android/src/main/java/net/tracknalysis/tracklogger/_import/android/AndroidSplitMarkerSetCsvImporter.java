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
package net.tracknalysis.tracklogger._import.android;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.content.ContentProviderOperation;
import android.content.ContentProviderResult;
import android.content.Context;

import net.tracknalysis.common.notification.NotificationStrategy;
import net.tracknalysis.tracklogger._import.AbstractSplitMarkerSetCsvImporter;
import net.tracknalysis.tracklogger.provider.TrackLoggerData;

/**
 * @author David Valeri
 */
public class AndroidSplitMarkerSetCsvImporter extends AbstractSplitMarkerSetCsvImporter {
    
    private static final Logger LOG = LoggerFactory.getLogger(AndroidSplitMarkerSetCsvImporter.class);

    private final Context context;
    private final File csvFile;
    private ArrayList<ContentProviderOperation> operations = new ArrayList<ContentProviderOperation>();
    private int id = -1;
    
    public AndroidSplitMarkerSetCsvImporter(
            NotificationStrategy<SplitMarkerSetImporterNotificationType> notificationStrategy,
            Context context, File csvFile) {
        super(notificationStrategy);
        this.context = context;
        this.csvFile = csvFile;
    }
    
    @Override
    protected void createSplitMarkerSet(String name) {
        operations.add(ContentProviderOperation.newInsert(TrackLoggerData.SplitMarkerSet.CONTENT_URI)
                .withValue(TrackLoggerData.SplitMarkerSet.COLUMN_NAME_NAME, name)
                .build());
    }

    @Override
    protected void createSplitMarker(String name, double lat, double lon) {
        operations.add(ContentProviderOperation.newInsert(TrackLoggerData.SplitMarker.CONTENT_URI)
                // Back reference to the set insertion
                .withValueBackReference(TrackLoggerData.SplitMarker.COLUMN_NAME_SPLIT_MARKER_SET_ID, 0)
                // Index can be size - 1 because the set is always the first element in the list
                .withValue(TrackLoggerData.SplitMarker.COLUMN_NAME_ORDER_INDEX, operations.size() - 1)
                .withValue(TrackLoggerData.SplitMarker.COLUMN_NAME_NAME, name)
                .withValue(TrackLoggerData.SplitMarker.COLUMN_NAME_LATITUDE, lat)
                .withValue(TrackLoggerData.SplitMarker.COLUMN_NAME_LONGITUDE, lon)
                .build());
    }
    
    @Override
    protected void commitTx() throws Exception {
        try {
            ContentProviderResult[] results = context.getContentResolver()
                    .applyBatch(TrackLoggerData.AUTHORITY, operations);
            id = Integer.valueOf(results[0].uri.getPathSegments().get(
                    TrackLoggerData.SplitMarkerSet.ID_PATH_POSITION));
        } catch (Exception e) {
            LOG.error("Error applying batch to content provider.  Batch contained [" + operations + "].");
            throw e;
        }
    }

    @Override
    protected InputStream getInputStream() throws IOException {
        if (csvFile != null) {
            return new BufferedInputStream(new FileInputStream(csvFile));
        } else {
            throw new NullPointerException("csvFile is null.");
        }
    }

    @Override
    public String getName() {
        if (csvFile != null) {
            String name = csvFile.getName();
            int lastDotIndex = name.lastIndexOf(".");
            if (name.lastIndexOf(".") != -1) {
                name = name.substring(0, lastDotIndex);
            }
            return name;
        } else {
            throw new NullPointerException("csvFile is null.");
        }
    }

    @Override
    public String getImportDescription() {
        // TODO i18n of the string literals used here!
        if (csvFile != null) {
            return "Split Marker Set CSV Import: " + csvFile.getName();
        } else {
            return "Split Marker Set CSV Import: Unknown."; 
        }
    }
    
    @Override
    public int getId() {
        return id;
    }
}
