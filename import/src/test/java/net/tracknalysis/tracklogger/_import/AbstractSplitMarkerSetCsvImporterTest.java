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
package net.tracknalysis.tracklogger._import;

import static org.easymock.EasyMock.*;
import static org.junit.Assert.*;

import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedList;
import java.util.List;

import org.easymock.Capture;
import org.easymock.CaptureType;
import org.junit.Before;
import org.junit.Test;

import net.tracknalysis.common.notification.NotificationListener;
import net.tracknalysis.tracklogger._import.AbstractSplitMarkerSetCsvImporter;
import net.tracknalysis.tracklogger._import.SplitMarkerSetImporter.ImportProgress;
import net.tracknalysis.tracklogger._import.SplitMarkerSetImporter.SplitMarkerSetImporterNotificationType;
import net.tracknalysis.tracklogger.model.SplitMarker;

/**
 * @author David Valeri
 */
public class AbstractSplitMarkerSetCsvImporterTest {
    
    private NotificationListener<SplitMarkerSetImporterNotificationType> notificationStrategy;
    
    @Before
    @SuppressWarnings("unchecked")
    public void setup() {
        notificationStrategy = createMock(NotificationListener.class);
    }
    
    @Test
    public void testValidFile() {
        
        SplitMarkerSetCsvImporterTestHarness harness = new SplitMarkerSetCsvImporterTestHarness(
                notificationStrategy,
                AbstractSplitMarkerSetCsvImporterTest.class
                        .getResourceAsStream("csv-splitmarker-valid.csv"));
        
        Capture<ImportProgress> progressCapture = new Capture<SplitMarkerSetImporter.ImportProgress>(CaptureType.ALL);
        
        notificationStrategy.onNotification(SplitMarkerSetImporterNotificationType.IMPORT_STARTING);
        expectLastCall();
        notificationStrategy.onNotification(SplitMarkerSetImporterNotificationType.IMPORT_STARTED);
        expectLastCall();
        notificationStrategy.onNotification(
                eq(SplitMarkerSetImporterNotificationType.IMPORT_PROGRESS),
                capture(progressCapture));
        expectLastCall();
        notificationStrategy.onNotification(
                eq(SplitMarkerSetImporterNotificationType.IMPORT_PROGRESS),
                capture(progressCapture));
        expectLastCall();
        notificationStrategy.onNotification(
                eq(SplitMarkerSetImporterNotificationType.IMPORT_PROGRESS),
                capture(progressCapture));
        expectLastCall();
        notificationStrategy.onNotification(
                eq(SplitMarkerSetImporterNotificationType.IMPORT_PROGRESS),
                capture(progressCapture));
        expectLastCall();
        notificationStrategy.onNotification(eq(SplitMarkerSetImporterNotificationType.IMPORT_FINISHED), eq(0));
        expectLastCall();
        
        replay(notificationStrategy);
        
        harness.doImport();
        
        verify(notificationStrategy);
        
        List<SplitMarker> splitMarkers = harness.getSplitMarkers();
        assertEquals(4, splitMarkers.size());
        
        assertEquals("marker 1", splitMarkers.get(0).getName());
        assertEquals(0, splitMarkers.get(0).getLatitiude(), 0);
        assertEquals(-1, splitMarkers.get(0).getLongitude(), 0);
        
        assertEquals("marker 2", splitMarkers.get(1).getName());
        assertEquals(-1, splitMarkers.get(1).getLatitiude(), 0);
        assertEquals(0, splitMarkers.get(1).getLongitude(), 0);
        
        assertEquals("marker 3", splitMarkers.get(2).getName());
        assertEquals(-1, splitMarkers.get(2).getLatitiude(), 0);
        assertEquals(1, splitMarkers.get(2).getLongitude(), 0);
        
        assertEquals("marker 4", splitMarkers.get(3).getName());
        assertEquals(-70, splitMarkers.get(3).getLatitiude(), 0);
        assertEquals(70.1234567890, splitMarkers.get(3).getLongitude(), 0);
    }
    
    @Test
    public void testExtraTokensFile() {
        SplitMarkerSetCsvImporterTestHarness harness = new SplitMarkerSetCsvImporterTestHarness(
                notificationStrategy,
                AbstractSplitMarkerSetCsvImporterTest.class
                        .getResourceAsStream("csv-splitmarker-invalid-line-tokens.csv"));
        
        notificationStrategy.onNotification(SplitMarkerSetImporterNotificationType.IMPORT_STARTING);
        expectLastCall();
        notificationStrategy.onNotification(SplitMarkerSetImporterNotificationType.IMPORT_STARTED);
        expectLastCall();
        notificationStrategy.onNotification(SplitMarkerSetImporterNotificationType.IMPORT_FAILED);
        expectLastCall();
        
        replay(notificationStrategy);
        
        harness.doImport();
        
        verify(notificationStrategy);
    }
    
    @Test
    public void testBlankLineFile() {
        SplitMarkerSetCsvImporterTestHarness harness = new SplitMarkerSetCsvImporterTestHarness(
                notificationStrategy,
                AbstractSplitMarkerSetCsvImporterTest.class
                        .getResourceAsStream("csv-splitmarker-invalid-blank-line.csv"));
        
        notificationStrategy.onNotification(SplitMarkerSetImporterNotificationType.IMPORT_STARTING);
        expectLastCall();
        notificationStrategy.onNotification(SplitMarkerSetImporterNotificationType.IMPORT_STARTED);
        expectLastCall();
        notificationStrategy.onNotification(SplitMarkerSetImporterNotificationType.IMPORT_FAILED);
        expectLastCall();
        
        replay(notificationStrategy);
        
        harness.doImport();
        
        verify(notificationStrategy);
    }
    
    @Test
    public void testNumberFormatFile() {
        SplitMarkerSetCsvImporterTestHarness harness = new SplitMarkerSetCsvImporterTestHarness(
                notificationStrategy,
                AbstractSplitMarkerSetCsvImporterTest.class
                        .getResourceAsStream("csv-splitmarker-invalid-number-format.csv"));
        
        notificationStrategy.onNotification(SplitMarkerSetImporterNotificationType.IMPORT_STARTING);
        expectLastCall();
        notificationStrategy.onNotification(SplitMarkerSetImporterNotificationType.IMPORT_STARTED);
        expectLastCall();
        notificationStrategy.onNotification(SplitMarkerSetImporterNotificationType.IMPORT_FAILED);
        expectLastCall();
        
        replay(notificationStrategy);
        
        harness.doImport();
        
        verify(notificationStrategy);
    }

    protected static final class SplitMarkerSetCsvImporterTestHarness extends AbstractSplitMarkerSetCsvImporter {
        
        private final InputStream is;
        
        private String splitMarkerSetName;
        private List<SplitMarker> splitMarkers = new LinkedList<SplitMarker>();

        protected SplitMarkerSetCsvImporterTestHarness(
                NotificationListener<SplitMarkerSetImporterNotificationType> notificationStrategy,
                InputStream is) {
            super(notificationStrategy);
            this.is = is;
        }

        @Override
        public String getImportDescription() {
            return "blah";
        }

        @Override
        public String getName() {
            return splitMarkerSetName;
        }

        @Override
        public int getId() {
            return 0;
        }

        @Override
        protected void createSplitMarkerSet(String name) {
            this.splitMarkerSetName = name;
        }

        @Override
        protected void createSplitMarker(String name, double lat, double lon) {
            SplitMarker marker = new SplitMarker();
            marker.setName(name);
            marker.setLatitiude(lat);
            marker.setLongitude(lon);
            
            splitMarkers.add(marker);
        }

        @Override
        protected InputStream getInputStream() throws IOException {
            return is;
        }
        
        public List<SplitMarker> getSplitMarkers() {
            return splitMarkers;
        }
    }
}
