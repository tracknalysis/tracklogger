package net.tracknalysis.tracklogger.test;

import org.apache.log4j.Level;

import net.tracknalysis.tracklogger.R;
import net.tracknalysis.tracklogger.activity.LogActivity;
import net.tracknalysis.tracklogger.config.Configuration;
import net.tracknalysis.tracklogger.config.ConfigurationFactory;
import net.tracknalysis.tracklogger.provider.TrackLoggerData;
import android.app.Dialog;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.test.ActivityInstrumentationTestCase2;
import android.widget.TextView;

public abstract class AbstractLogActivityTest extends ActivityInstrumentationTestCase2<LogActivity> {
    
    protected Configuration configuration;
    protected Context context;
    
    protected TextView locationStatus;
    protected TextView accelStatus;
    protected TextView ecuStatus;
    
    protected TextView elapsedLapTime;
    protected TextView lapNumber;
    protected TextView elapsedSessionTime;
    protected TextView lastSplitTime;
    protected TextView lastSplitTimeDelta;
    protected TextView lastLapTime;
    protected TextView lastLapTimeDelta;
    
    public AbstractLogActivityTest() {
        super(LogActivity.class);
    }
    
    @Override
    protected final void setUp() throws Exception {
        super.setUp();
        
        context = this.getInstrumentation().getTargetContext().getApplicationContext();
        
        configuration = ConfigurationFactory.getInstance().getConfiguration();
        configuration.setRootLogLevel(Level.INFO);
        configuration.setLogToFile(false);
        configuration.setTestMode(true);
        configuration.setEcuIoLogEnabled(false);
        
        configuration.setLogLayoutId(R.layout.log_default);
        
        setupSplitMarkerSet();
        
        init();
    }
    
    protected abstract void init() throws Exception;
    
    protected void triggerStart(final LogActivity logActivity) throws Throwable {
        // Fake the user picking a split marker set and then clicking the start button.
        runTestOnUiThread(new Runnable() {
            @Override
            public void run() {
                logActivity.onActivityResult(
                        LogActivity.ACTION_REQUEST_PICK_SPLIT_MARKER_SET,
                        LogActivity.RESULT_OK,
                        new Intent(Intent.ACTION_VIEW, ContentUris.withAppendedId(
                                TrackLoggerData.SplitMarkerSet.CONTENT_URI, 1)));
                
                assertFalse(logActivity
                        .getString(
                                R.string.log_setup_session_split_marker_set_name_prompt)
                        .equals(((TextView) logActivity
                                .getSetupSessionDialog()
                                .findViewById(
                                        R.id.log_setup_session_split_marker_set_name_text_view))
                                .getText()));
                
                logActivity.getSetupSessionDialog()
                        .findViewById(R.id.log_setup_session_start_button)
                        .performClick();
            }
        });
    }
    
    protected void setupSplitMarkerSet() {
        /*
        new Route("My Route", Arrays.asList(
                new Waypoint("1", 38.979896545410156d, -77.54102325439453d),
                new Waypoint("2", 38.98295974731445d, -77.53973388671875d),
                new Waypoint("3", 38.982906341552734d, -77.54007720947266d),
                new Waypoint("4", 38.972618103027344d, -77.54145050048828d),
                new Waypoint("5", 38.97257995605469d, -77.5412826538086d)));
         */
        
        try {
            context.getContentResolver().delete(
                    ContentUris.withAppendedId(
                            TrackLoggerData.SplitMarkerSet.CONTENT_URI, 1),
                    null, null);
                        
            ContentValues splitMarkerSetCvs = new ContentValues();
            splitMarkerSetCvs.put(TrackLoggerData.SplitMarkerSet.COLUMN_NAME_NAME, "TEST");
            splitMarkerSetCvs.put(TrackLoggerData.SplitMarkerSet._ID, 1);
            context.getContentResolver().insert(
                    TrackLoggerData.SplitMarkerSet.CONTENT_URI, splitMarkerSetCvs);
            
            ContentValues splitMarkerCvs = new ContentValues();
            splitMarkerCvs.put(TrackLoggerData.SplitMarker.COLUMN_NAME_LATITUDE, 38.979896545410156d);
            splitMarkerCvs.put(TrackLoggerData.SplitMarker.COLUMN_NAME_LONGITUDE, -77.54102325439453d);
            splitMarkerCvs.put(TrackLoggerData.SplitMarker.COLUMN_NAME_ORDER_INDEX, 0);
            splitMarkerCvs.put(TrackLoggerData.SplitMarker.COLUMN_NAME_SPLIT_MARKER_SET_ID, 1);
            context.getContentResolver().insert(
                    TrackLoggerData.SplitMarker.CONTENT_URI, splitMarkerCvs);
            
            splitMarkerCvs.put(TrackLoggerData.SplitMarker.COLUMN_NAME_LATITUDE, 38.98295974731445d);
            splitMarkerCvs.put(TrackLoggerData.SplitMarker.COLUMN_NAME_LONGITUDE, -77.53973388671875d);
            splitMarkerCvs.put(TrackLoggerData.SplitMarker.COLUMN_NAME_ORDER_INDEX, 1);
            context.getContentResolver().insert(
                    TrackLoggerData.SplitMarker.CONTENT_URI, splitMarkerCvs);
            
            splitMarkerCvs.put(TrackLoggerData.SplitMarker.COLUMN_NAME_LATITUDE, 38.982906341552734d);
            splitMarkerCvs.put(TrackLoggerData.SplitMarker.COLUMN_NAME_LONGITUDE, -77.54007720947266d);
            splitMarkerCvs.put(TrackLoggerData.SplitMarker.COLUMN_NAME_ORDER_INDEX, 2);
            context.getContentResolver().insert(
                    TrackLoggerData.SplitMarker.CONTENT_URI, splitMarkerCvs);
            
            splitMarkerCvs.put(TrackLoggerData.SplitMarker.COLUMN_NAME_LATITUDE, 38.972618103027344d);
            splitMarkerCvs.put(TrackLoggerData.SplitMarker.COLUMN_NAME_LONGITUDE, -77.54145050048828d);
            splitMarkerCvs.put(TrackLoggerData.SplitMarker.COLUMN_NAME_ORDER_INDEX, 3);
            context.getContentResolver().insert(
                    TrackLoggerData.SplitMarker.CONTENT_URI, splitMarkerCvs);
            
            splitMarkerCvs.put(TrackLoggerData.SplitMarker.COLUMN_NAME_LATITUDE, 38.97257995605469d);
            splitMarkerCvs.put(TrackLoggerData.SplitMarker.COLUMN_NAME_LONGITUDE, -77.5412826538086d);
            splitMarkerCvs.put(TrackLoggerData.SplitMarker.COLUMN_NAME_ORDER_INDEX, 4);
            context.getContentResolver().insert(
                    TrackLoggerData.SplitMarker.CONTENT_URI, splitMarkerCvs);
        } catch (Exception e) {
            // Ignore it, we just want to make sure one exists.
        }
    }
    
    protected void initializeUiFields(LogActivity logActivity) {
        elapsedLapTime = (TextView) logActivity.findViewById(R.id.log_elapsed_lap_time_value);
        lapNumber = (TextView) logActivity.findViewById(R.id.log_lap_number_value);
        elapsedSessionTime = (TextView) logActivity.findViewById(R.id.log_elapsed_session_time_value);
        lastSplitTime = (TextView) logActivity.findViewById(R.id.log_last_split_time_value);
        lastSplitTimeDelta = (TextView) logActivity.findViewById(R.id.log_last_split_time_delta_value);
        lastLapTime = (TextView) logActivity.findViewById(R.id.log_last_lap_time_value);
        lastLapTimeDelta = (TextView) logActivity.findViewById(R.id.log_last_lap_time_delta_value);
        
        Dialog initDataProviderCoordinatorDialog = logActivity.getInitDataProviderCoordinatorDialog();
        locationStatus = (TextView) initDataProviderCoordinatorDialog
                .findViewById(R.id.log_wait_for_ready_location_status);
        accelStatus = (TextView) initDataProviderCoordinatorDialog
                .findViewById(R.id.log_wait_for_ready_accel_status);
        ecuStatus = (TextView) initDataProviderCoordinatorDialog
                .findViewById(R.id.log_wait_for_ready_ecu_status);
    }

}
