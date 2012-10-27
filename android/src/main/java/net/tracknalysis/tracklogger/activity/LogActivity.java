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

import java.lang.ref.WeakReference;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.tracknalysis.common.android.notification.AndroidNotificationStrategy;
import net.tracknalysis.common.concurrent.GracefulShutdownThread;
import net.tracknalysis.common.notification.NotificationStrategy;
import net.tracknalysis.common.util.TimeUtil;
import net.tracknalysis.tracklogger.config.Configuration;
import net.tracknalysis.tracklogger.config.ConfigurationFactory;
import net.tracknalysis.tracklogger.dataprovider.DataProviderCoordinator;
import net.tracknalysis.tracklogger.dataprovider.DataProviderCoordinator.DataProviderCoordinatorNotificationType;
import net.tracknalysis.tracklogger.dataprovider.android.DataProviderCoordinatorManagerService;
import net.tracknalysis.tracklogger.dataprovider.android.DataProviderCoordinatorManagerService.LocalBinder;
import net.tracknalysis.tracklogger.model.AccelData;
import net.tracknalysis.tracklogger.model.EcuData;
import net.tracknalysis.tracklogger.model.LocationData;
import net.tracknalysis.tracklogger.model.TimingData;
import net.tracknalysis.tracklogger.provider.TrackLoggerData;
import net.tracknalysis.tracklogger.R;

import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences.Editor;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

/**
 * @author David Valeri
 */
public class LogActivity extends BaseActivity implements OnCancelListener {
    
    private static final Logger LOG = LoggerFactory.getLogger(LogActivity.class);
    
    public static final int ACTION_REQUEST_BT_ENABLE = 1;
    public static final int ACTION_REQUEST_PICK_SPLIT_MARKER_SET = 2;
    
    private static final AtomicInteger displayTaskCounter = new AtomicInteger();
    
    private static final String SPLIT_MARKER_SET_URI_PREF_KEY = "SPLIT_MARKER_SET_URI_PREF_KEY";
    
    private BluetoothAdapter btAdapter;
    
    private final Configuration config = ConfigurationFactory.getInstance().getConfiguration();  
    
    private volatile ServiceConnection serviceConnection;
    private volatile boolean bound;
    
    private volatile boolean ignoreStopLogging = false;
    
    private NotificationStrategy<DataProviderCoordinatorNotificationType> notificationStrategy;
    
    private Uri splitMarkerSetUri = null;
    
    // Timing
    private volatile long lapNumberCounter = 1;
    private volatile Long lapStartReceivedTime;
    private volatile Long sessionStartReceivedTime;
    private volatile Long previousBestLapTime;
    private volatile List<Long> previousBestSplitTimes;
    
    // Data Coordinator
    private DataProviderCoordinatorManagerService dpcManagerService;
    
    // Display
    private DisplayTask displayTask;
    private Dialog setupSessionDialog;
    private TextView splitMarkerSetNameTextView;
    private Dialog initDataProviderCoordinatorDialog;
    private ProgressDialog waitingForStartTriggerDialog;
        
    /**
     *  Elapsed time in the current lap.
     */
    private TextView elapsedLapTime;
    
    /**
     *  The current lap number.
     */
    private TextView lapNumber;
    
    /**
     * Total elapsed time in the session since the start of logging.
     */
    private TextView elapsedSessionTime;
    
    /**
     * Elapsed time of the last split/segment.
     */
    private TextView lastSplitTime;
    
    /**
     * Delta of elapsed time of the last split/segment from the best time for the last split/segment.
     */
    private TextView lastSplitTimeDelta;
    
    /**
     * Elapsed time of the last lap.
     */
    private TextView lastLapTime;
    
    /**
     * Delta of elapsed time of the last lap from the best lap time.
     */
    private TextView lastLapTimeDelta;
    
    /**
     * The index of the current split/segment.
     */
    private TextView splitIndex;
    
    /**
     * The elapsed time of the best lap this session.
     */
    private TextView bestLapTime;
    
    /**
     * The elapsed time of the best time for the current split/segment this session.
     */
    private TextView bestSplitTime;
    
    
    /**
     * Update frequency of the acceleration data in Hz.
     */
    private TextView accelUpdateFreq;
    
    /**
     * Longitudinal acceleration in m/s^2.
     */
    private TextView lonAccel;
    
    /**
     * Lateral acceleration in m/s^2.
     */
    private TextView latAccel;
    
    /**
     * Vertical acceleration in m/s^2.
     */
    private TextView vertAccel;
    
    
    /**
     * Update frequency of the location data in Hz.
     */
    private TextView locationUpdateFreq;
    
    /**
     * Latitude in degrees.
     */
    private TextView lat;
    
    /**
     * Longitude in degrees.
     */
    private TextView lon;
    
    /**
     * Altitude in meters.
     */
    private TextView alt;
    
    /**
     * Bearing in degrees.
     */
    private TextView bearing;
    
    /**
     * Speed in m/s.
     */
    private TextView speed;
    
    
    /**
     * Update frequency of ECU data in Hz.
     */
    private TextView ecuUpdateFreq;
    
    /**
     * RPMs.
     */
    private TextView rpm;
    
    /**
     * MAP in KPa.
     */
    private TextView map;
    
    /**
     * Throttle position in %.
     */
    private TextView tp;
    
    /**
     * AFR.
     */
    private TextView afr;
    
    /**
     * MAT in degrees Celsius.
     */
    private TextView mat;
    
    /**
     * CLT in degrees Celsius.
     */
    private TextView clt;
    
    /**
     * Ignition advance in degrees.
     */
    private TextView ignAdv;
    
    /**
     * Battery voltage in Volts.
     */
    private TextView batV;
    
    /**
     * Runnable that executes on a regular basis to provide updates to non-event based 
     * UI fields (mostly the ones that aren't tied to timing data).
     */
    Runnable periodicUiUpdateRunnable;
    
    /**
     * Sets if {@code stopLogging} is ignored on {@link #cleanup(boolean)}.  Added to support
     * lifecycle testing.
     */
    public void setIgnoreStopLogging(boolean ignoreStopLogging) {
        this.ignoreStopLogging = ignoreStopLogging;
    }
    
    /**
     * Returns the dialog for configuring the session.  Used in testing
     * to interrogate the content of the dialog in response to events.
     */
    public Dialog getSetupSessionDialog() {
        return setupSessionDialog;
    }
    
    /**
     * Returns the dialog for initializing the data provider coordinator.  Used in testing
     * to interrogate the content of the dialog in response to events.
     */
    public Dialog getInitDataProviderCoordinatorDialog() {
        return initDataProviderCoordinatorDialog;
    }

    /**
     * Returns the dialog for waiting for the data provider coordinator start trigger to fire.  Used in testing
     * to interrogate the content of the dialog in response to events.
     */
    public ProgressDialog getWaitingForStartTriggerDialog() {
        return waitingForStartTriggerDialog;
    }
    
    /**
     * Returns the notification strategy used by the activity. Used in testing.
     */
    public NotificationStrategy<DataProviderCoordinatorNotificationType> getNotificationStrategy() {
        return notificationStrategy;
    }
    
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case ACTION_REQUEST_BT_ENABLE:
                if (resultCode == Activity.RESULT_OK) {
                    initDataProviderCoordinator();
                } else {
                    onTerminalError(R.string.error_bt_not_enabled);
                }
                break;
            case ACTION_REQUEST_PICK_SPLIT_MARKER_SET:
                if (resultCode == Activity.RESULT_OK) {
                    splitMarkerSetUri = data.getData();
                    updateSplitMarkerSetNameTextView();
                }
                break;
            default:
                LOG.error(
                        "Got activity result that the LogActivity did not initiate.  "
                                + "Request code {}, result code {}, intent {}.",
                        new Object[] {requestCode, resultCode, data});
        }
    }
    
    @Override
    public void onCancel(DialogInterface dialog) {
        if (dialog == initDataProviderCoordinatorDialog
                || dialog == waitingForStartTriggerDialog
                || dialog == setupSessionDialog) {
            finish();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        ignoreStopLogging = false;
        
        btAdapter = BluetoothAdapter.getDefaultAdapter();
        
        initSetupSessionDialog();
        
        initDataProviderCoordinatorDialog = new Dialog(this);
        initDataProviderCoordinatorDialog.setContentView(R.layout.log_wait_for_ready);
        initDataProviderCoordinatorDialog.setTitle("Waiting for data providers...");
        initDataProviderCoordinatorDialog.setCancelable(true);
        initDataProviderCoordinatorDialog.setOnCancelListener(this);
        
        waitingForStartTriggerDialog = new ProgressDialog(LogActivity.this);
        waitingForStartTriggerDialog.setTitle("Logging Start");
        waitingForStartTriggerDialog.setCancelable(true);
        waitingForStartTriggerDialog.setMessage("Waiting for logging trigger...");
        waitingForStartTriggerDialog.setOnCancelListener(this);
        
        setContentView(config.getLogLayoutId());
        
        initDisplayViews();
        
        periodicUiUpdateRunnable = new Runnable() {
            
            @Override
            public void run() {
                
                try {
                    DataProviderCoordinator dpcService = dpcManagerService.getInstance();
                    
                    AccelData accelData = dpcService.getCurrentAccelData();
                    LocationData locationData = dpcService.getCurrentLocationData();
                    EcuData ecuData = null;
                    
                    
                    ecuData = dpcService.getCurrentEcuData();
                    
                    final long currentTime = System.currentTimeMillis();
                    
                    if (sessionStartReceivedTime != null) {
                        long sessionElapsedTime = currentTime - sessionStartReceivedTime;
                        setTextIfShown(elapsedSessionTime,
                                TimeUtil.formatDuration(sessionElapsedTime,
                                        false, false));
                        
                    }
                    
                    if (lapStartReceivedTime != null) {
                        long lapElapsedTime = currentTime - lapStartReceivedTime;
                        setTextIfShown(elapsedLapTime, TimeUtil.formatDuration(
                                lapElapsedTime, false, true));
                    }
                    
                    if (accelData == null) {
                        setTextIfShown(accelUpdateFreq, "N/A");
                        setTextIfShown(lonAccel, "N/A");
                        setTextIfShown(latAccel, "N/A");
                        setTextIfShown(vertAccel, "N/A");
                    } else {
                        setTextIfShown(accelUpdateFreq, "%.3f", dpcService.getAccelDataUpdateFrequency());
                        setTextIfShown(lonAccel, "%.3f", accelData.getLongitudinal());
                        setTextIfShown(latAccel, "%.3f", accelData.getLateral());
                        setTextIfShown(vertAccel, "%.3f", accelData.getVertical());
                    }
                    
                    if (locationData == null) {
                        setTextIfShown(locationUpdateFreq, "N/A");
                        setTextIfShown(lat, "N/A");
                        setTextIfShown(lon, "N/A");
                        setTextIfShown(alt, "N/A");
                        setTextIfShown(bearing, "N/A");
                        setTextIfShown(speed, "N/A");
                    } else {
                        setTextIfShown(locationUpdateFreq, "%.3f",
                                dpcService.getLocationDataUpdateFrequency());
                        setTextIfShown(lat, "%.8f", locationData.getLatitude());
                        setTextIfShown(lon, "%.8f", locationData.getLongitude());
                        setTextIfShown(alt, "%.3f", locationData.getAltitude());
                        setTextIfShown(bearing, "%.3f", locationData.getBearing());
                        setTextIfShown(speed, "%.3f", locationData.getSpeed());
                    }
                    
                    if (ecuData == null) {
                        setTextIfShown(ecuUpdateFreq, "N/A");
                        setTextIfShown(rpm, "N/A");
                        setTextIfShown(map, "N/A");
                        setTextIfShown(tp, "N/A");
                        setTextIfShown(afr, "N/A");
                        setTextIfShown(mat, "N/A");
                        setTextIfShown(clt, "N/A");
                        setTextIfShown(ignAdv, "N/A");
                        setTextIfShown(batV, "N/A");
                    } else {
                        setTextIfShown(ecuUpdateFreq, "%.3f", dpcService.getEcuDataUpdateFrequency());
                        setTextIfShown(rpm, "%d", ecuData.getRpm());
                        setTextIfShown(map, "%.0f", ecuData.getManifoldAbsolutePressure());
                        setTextIfShown(tp, "%.0f", ecuData.getThrottlePosition() * 100);
                        setTextIfShown(afr, "%.1f", ecuData.getAirFuelRatio());
                        setTextIfShown(mat, "%.1f", ecuData.getManifoldAirTemperature());
                        setTextIfShown(clt, "%.1f", ecuData.getCoolantTemperature());
                        setTextIfShown(ignAdv, "%.1f", ecuData.getIgnitionAdvance());
                        setTextIfShown(batV, "%.1f", ecuData.getBatteryVoltage());
                    }
                } catch (IllegalStateException e) {
                    LOG.warn(
                            "Scheduled runnable on UI thread was still executing when the data "
                                    + "provider coordinator service was destroyed.  This is likely just a timing"
                                    + " issue; however, this still warrants a look as a possible cause of errors if the"
                                    + " exception was triggered outside of the data provider coordinator.",
                            e);
                } catch (RuntimeException e) {
                    LOG.error("Error updating LogActivity UI components.", e);
                    throw e;
                }
            }
        };
        
        notificationStrategy = new AndroidNotificationStrategy<DataProviderCoordinator.DataProviderCoordinatorNotificationType>(
                new DataProviderCoordinatorHandler(this));
    }

    @Override
    protected void onResume() {
        super.onResume();
        
        displayTask = new DisplayTask();
        
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        
        // Setup the service connection.  This guy will trigger the remaining UI setup / updates on
        // the successful bind.
        serviceConnection = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                // TODO deal with rebinding.
                bound = true;
                LocalBinder binder = (LocalBinder) service;
                dpcManagerService = binder.getService();
                
                if (btAdapter == null) {
                    onTerminalError(R.string.error_bt_not_supported);
                } else {
                    if (!btAdapter.isEnabled()) {
                        // BT is off so the coordinator cannot possibly be working.  We will uninit it
                        // and then rebuild it after turning BT back on.
                        if (dpcManagerService.isInitialized()) {
                            dpcManagerService.uninitialize();
                            dpcManagerService = null;
                        }
                        startActivityForResult(
                                new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE),
                                ACTION_REQUEST_BT_ENABLE);
                    } else {
                        if (dpcManagerService.isInitialized()) {
                            initDataProviderCoordinator();
                        } else {
                            setupSessionDialog.show();
                        }
                    }
                }
            }

            @Override
            public void onServiceDisconnected(ComponentName arg0) {
                bound = false;
            }
        };
        
        if (!bindService(
                new Intent(getApplicationContext(), DataProviderCoordinatorManagerService.class),
                serviceConnection,
                BIND_AUTO_CREATE)) {
            LOG.error("Could not bind to DataProviderCoordinatorManagerService.");
            onInitDataProviderCoordinatorError();
        }
    }

    @Override
    protected void onPause() {
        Editor editor = getPreferences(MODE_PRIVATE).edit();
        if (splitMarkerSetUri == null) {
            editor.remove(SPLIT_MARKER_SET_URI_PREF_KEY);
        } else {
            editor.putString(SPLIT_MARKER_SET_URI_PREF_KEY, splitMarkerSetUri.toString());
        }
        editor.commit();
        
        cleanup(false);
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        // Always cleanup, but also only stop logging if the activity is finishing rather than
        // being destroyed by the OS for resource reasons.
        cleanup(isFinishing());
        super.onDestroy();
    }

    private void cleanup(boolean stopLogging) {
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        
        if (displayTask != null) {
            displayTask.cancel();
        }
        
        if (setupSessionDialog != null) {
            setupSessionDialog.dismiss();
        }
        
        if (initDataProviderCoordinatorDialog != null) {
            initDataProviderCoordinatorDialog.dismiss();
        }
        
        if (waitingForStartTriggerDialog != null) {
            waitingForStartTriggerDialog.dismiss();
        }
        
        getDialogManager().onDestroy();
        
        if (dpcManagerService != null && dpcManagerService.isInitialized()) {
            
            dpcManagerService.getInstance().unRegister(notificationStrategy);
            
            if (stopLogging && !ignoreStopLogging) {
                dpcManagerService.uninitialize();
                stopService(new Intent(getApplicationContext(), DataProviderCoordinatorManagerService.class));
            }
        }
        
        if (bound) {
            unbindService(serviceConnection);
            bound = false;
        }
    }
    
    private void initSetupSessionDialog() {
        setupSessionDialog = new Dialog(this);
        setupSessionDialog.setContentView(R.layout.log_setup_session);
        setupSessionDialog.setTitle("Configure Session");
        setupSessionDialog.setCancelable(true);
        setupSessionDialog.setOnCancelListener(this);
        
        Button chooseSplitMarkerSetButton = (Button) setupSessionDialog
                .findViewById(R.id.log_setup_session_split_marker_set_name_choose_button);
        chooseSplitMarkerSetButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivityForResult(new Intent(Intent.ACTION_PICK,
                        TrackLoggerData.SplitMarkerSet.CONTENT_URI),
                        ACTION_REQUEST_PICK_SPLIT_MARKER_SET);     
            }
        });
        
        Button startButton = (Button) setupSessionDialog
                .findViewById(R.id.log_setup_session_start_button);
        startButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (splitMarkerSetUri == null) {
                    onNonTerminalError(R.string.log_setup_session_error_split_marker_set);
                } else {
                    initDataProviderCoordinator();
                }
            }
        });
        
        splitMarkerSetNameTextView = (TextView) setupSessionDialog
                .findViewById(R.id.log_setup_session_split_marker_set_name_text_view);
        
        String splitMarkerSetUriString = getPreferences(MODE_PRIVATE)
                .getString(SPLIT_MARKER_SET_URI_PREF_KEY, null);
        
        if (splitMarkerSetUriString == null) {
            splitMarkerSetUri = null;
        } else {
            splitMarkerSetUri = Uri.parse(splitMarkerSetUriString);
        }
        updateSplitMarkerSetNameTextView();
    }
    
    private void initDisplayViews() {
        elapsedLapTime = (TextView) findViewById(R.id.log_elapsed_lap_time_value);
        lapNumber = (TextView) findViewById(R.id.log_lap_number_value);
        elapsedSessionTime = (TextView) findViewById(R.id.log_elapsed_session_time_value);
        lastSplitTime = (TextView) findViewById(R.id.log_last_split_time_value);
        lastSplitTimeDelta = (TextView) findViewById(R.id.log_last_split_time_delta_value);
        lastLapTime = (TextView) findViewById(R.id.log_last_lap_time_value);
        lastLapTimeDelta = (TextView) findViewById(R.id.log_last_lap_time_delta_value);
        splitIndex = (TextView) findViewById(R.id.log_split_index_value);
        bestLapTime = (TextView) findViewById(R.id.log_best_lap_time_value);
        bestSplitTime = (TextView) findViewById(R.id.log_best_split_time_value);
        
        accelUpdateFreq = (TextView) findViewById(R.id.log_accel_update_frequency_value);
        lonAccel = (TextView) findViewById(R.id.log_lon_accel_value);
        latAccel = (TextView) findViewById(R.id.log_lat_accel_value);
        vertAccel = (TextView) findViewById(R.id.log_vert_accel_value);
        
        locationUpdateFreq = (TextView) findViewById(R.id.log_location_update_frequency_value);
        lat = (TextView) findViewById(R.id.log_lat_value);
        lon = (TextView) findViewById(R.id.log_lon_value);
        alt = (TextView) findViewById(R.id.log_alt_value);
        bearing = (TextView) findViewById(R.id.log_bearing_value);
        speed = (TextView) findViewById(R.id.log_speed_value);
        
        ecuUpdateFreq = (TextView) findViewById(R.id.log_ecu_update_frequency_value);
        rpm = (TextView) findViewById(R.id.log_rpm_value);
        map = (TextView) findViewById(R.id.log_map_value);
        tp = (TextView) findViewById(R.id.log_tp_value);
        afr = (TextView) findViewById(R.id.log_afr_value);
        mat = (TextView) findViewById(R.id.log_mat_value);
        clt = (TextView) findViewById(R.id.log_clt_value);
        ignAdv = (TextView) findViewById(R.id.log_ign_adv_value);
        batV = (TextView) findViewById(R.id.log_batv_value);
    }
    
    /**
     * Initializes the {@link DataProviderCoordinator} through the
     * {@link DataProviderCoordinatorManagerService} if not already initialized
     * and registers for notifications. 
     */
    private void initDataProviderCoordinator() {
        try {
            if (!dpcManagerService.isInitialized()) {
                if (splitMarkerSetUri != null) {
                    startService(new Intent(getApplicationContext(), DataProviderCoordinatorManagerService.class));
                    dpcManagerService.initialize(getApplication(), btAdapter,
                            splitMarkerSetUri);
                    dpcManagerService.getInstance().register(notificationStrategy);
                    dpcManagerService.getInstance().startAsynch();
                } else {
                    LOG.error("Missing split marker set URI while initializing data providers and coordinator.");
                    onInitDataProviderCoordinatorError();
                }
            } else {
                // Register for notifications. As soon as we do this, we will
                // receive a notification of the coordinator's current state. 
                // From there we launch into the next phase of initialization.
                dpcManagerService.getInstance().register(notificationStrategy);
            }
        } catch (Exception e) {
            LOG.error("Fatal error while initializing data providers and coordinator.", e);
            onInitDataProviderCoordinatorError();
        }
    }
    
    /**
     * Reset and show {@link #initDataProviderCoordinatorDialog}.
     */
    private void showInitDataProviderCoordinatorDialog() {
        ((TextView) initDataProviderCoordinatorDialog
                .findViewById(R.id.log_wait_for_ready_location_status))
                .setText(R.string.log_wait_for_ready_text_waiting);
        
        ((TextView) initDataProviderCoordinatorDialog
                .findViewById(R.id.log_wait_for_ready_accel_status))
                .setText(R.string.log_wait_for_ready_text_waiting);
        
        ((TextView) initDataProviderCoordinatorDialog
                .findViewById(R.id.log_wait_for_ready_ecu_status))
                .setText(R.string.log_wait_for_ready_text_waiting);
        
        initDataProviderCoordinatorDialog.show();
    }
    
    private void onInitDataProviderCoordinatorError() {     
        cleanup(true);
        onTerminalError(R.string.log_error_init);
    }
    
    private void onDataProviderCoordinatorLoggingError() {      
        cleanup(true);
        onTerminalError(R.string.log_error_recording);
    }
    
    /**
     * Refreshes the split marker set name field in the setup dialog based on the current
     * value of {@link #splitMarkerSetUri}.
     */
    private void updateSplitMarkerSetNameTextView() {
        String splitMarkerSetName = null;
        
        if (splitMarkerSetUri != null) {
            splitMarkerSetName = getSplitMarkerSetName(splitMarkerSetUri);
        }
        
        if (splitMarkerSetName == null) {
            splitMarkerSetName = getString(R.string.log_setup_session_split_marker_set_name_prompt);
        }
        
        splitMarkerSetNameTextView.setText(splitMarkerSetName);
    }
    
    /**
     * Returns the name of the split marker set or {@code null} if the split marker set cannot be found.
     *
     * @param splitMarkerSetUri the URI of the set to get the name of
     */
    private String getSplitMarkerSetName(Uri splitMarkerSetUri) {
        Cursor cursor = null;
        
        try {
            cursor = getContentResolver().query(splitMarkerSetUri, null, null, null, null);
            
            if (cursor.getCount() != 1) {
                return null;
            } else {
                cursor.moveToFirst();
                return cursor.getString(cursor.getColumnIndex(TrackLoggerData.SplitMarkerSet.COLUMN_NAME_NAME));
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }
    
    /**
     * Sets the formatted text of the view if it is not {@code null}.
     *
     * @param view the view to set the text of
     * @param format the format string of the text to set
     * @param args the arguments to the format string
     *
     * @see String#format(String, Object...)
     */
    protected void setTextIfShown(TextView view, String format, Object... args) {
        if (view != null) {
            view.setText(String.format(format, args));
        }
    }
    
    /**
     * Sets the color the view if it is not {@code null}.
     *
     * @param view the view to set the color of
     * @param color the color to set
     */
    protected void setColorIfShown(TextView view, Integer color) {
        if (view != null) {
            view.setTextColor(color);
        }
    }
    
    /**
     * Updates the UI fields that are driven by a periodic timer.
     */
    private void updateNonEventBasedUiFields() {
        runOnUiThread(periodicUiUpdateRunnable);
    }
    
    private void updateEventBasedUiFields(TimingData timingData) {
        sessionStartReceivedTime = timingData.getInitialLapStartDataReceivedTime();
        lapStartReceivedTime = timingData.getLastLapStartDataReceivedTime();
        
        if (timingData.getLapTime() != null) {
            // After the first lap, all subsequent timing events for a lap completion will
            // have a non-null lap time.
            setTextIfShown(lastLapTime, TimeUtil.formatDuration(timingData.getLapTime(),
                    false, true));
            
            long lapDelta = (previousBestLapTime == null ? 0 : timingData.getLapTime() - previousBestLapTime);
            setTextIfShown(lastLapTimeDelta, 
                    TimeUtil.formatDuration(lapDelta, false, true));
            
            int lapDeltaColor;
            if (lapDelta < 0) {
                lapDeltaColor = Color.GREEN;
            } else if(lapDelta == 0) {
                lapDeltaColor = Color.WHITE;
            } else {
                lapDeltaColor = Color.RED;
            }
            setColorIfShown(lastLapTimeDelta, lapDeltaColor);
            
            setTextIfShown(bestLapTime, TimeUtil.formatDuration(timingData.getBestLapTime(), false, true));
            
            previousBestLapTime = timingData.getBestLapTime();
            
            lapNumberCounter++;
        }
        
        if (timingData.getSplitTime() != null) {
            
            setTextIfShown(lastSplitTime, TimeUtil.formatDuration(timingData.getSplitTime(),
                    false, true));
            
            long splitDelta;
            if (previousBestSplitTimes == null) {
                splitDelta = 0;
            } else {
                splitDelta = previousBestSplitTimes.get(timingData.getSplitIndex()) == null 
                        ? 0 : timingData.getSplitTime() - previousBestSplitTimes.get(timingData.getSplitIndex());  
            }
            setTextIfShown(lastSplitTimeDelta, 
                    TimeUtil.formatDuration(splitDelta, false, true));
            
            int splitDeltaColor;
            if (splitDelta < 0) {
                splitDeltaColor = Color.GREEN;
            } else if(splitDelta == 0) {
                splitDeltaColor = Color.WHITE;
            } else {
                splitDeltaColor = Color.RED;
            }
            setColorIfShown(lastSplitTimeDelta, splitDeltaColor);
            
            setTextIfShown(bestSplitTime, 
                    TimeUtil.formatDuration(
                            timingData.getBestSplitTimes().get(timingData.getSplitIndex()), false, true));
            
            
            previousBestSplitTimes = timingData.getBestSplitTimes();
        }
        
        setTextIfShown(splitIndex, "%d", timingData.getSplitIndex() + 1);
        
        setTextIfShown(lapNumber, "%d", lapNumberCounter);
    }
    
    /**
     * A simple task to handle updating the regular data on the screen that is not event critical
     * (that is not timing data).  We don't use the activity thread from Android here because we
     * want to take advantage of the graceful shutdown logic in {@link GracefulShutdownThread}.
     */
    private class DisplayTask extends GracefulShutdownThread {
        
        public DisplayTask() {
            setName("LogActivity-DisplayTask-" + displayTaskCounter.getAndIncrement());
        }
        
        @Override
        public void run() {
            while(run) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    if (run) {
                        LOG.error("Interrupted display task while running.", e);
                    } else {
                        LOG.info("Interrupted display task while not running.", e);
                    }
                }
                
                try {
                    updateNonEventBasedUiFields();
                } catch (Exception e) {
                    LOG.error("Error while updating UI.", e);
                }
            }
        }
    }
    
    private static class DataProviderCoordinatorHandler extends Handler {
        
        private final WeakReference<LogActivity>  logActivityRef;
        
        protected DataProviderCoordinatorHandler(LogActivity logActivity) {
            logActivityRef = new WeakReference<LogActivity>(logActivity);
        }
        
        @SuppressWarnings("incomplete-switch")
        @Override
        public synchronized void handleMessage(Message msg) {
            LogActivity logActivity = logActivityRef.get();
            
            if (logActivity != null) {
            
                LOG.debug("Handling data provider coordinator message: {}.", msg);
                
                DataProviderCoordinator.DataProviderCoordinatorNotificationType type = 
                        DataProviderCoordinator.DataProviderCoordinatorNotificationType.fromInt(msg.what);
                
                try {
                    switch (type) {
                        case STARTING:
                            logActivity.getSetupSessionDialog().dismiss();
                            logActivity.showInitDataProviderCoordinatorDialog();
                            break;
                        case START_FAILED:
                            logActivity.onInitDataProviderCoordinatorError();
                            break;
                        case READY_PROGRESS:
                            Object[] data = (Object[]) msg.obj;
                            if (data[0] != null) {
                                TextView locationStatusView = (TextView) logActivity
                                        .getInitDataProviderCoordinatorDialog()
                                        .findViewById(
                                                R.id.log_wait_for_ready_location_status);
                                
                                locationStatusView.setText(R.string.log_wait_for_ready_text_ready);
                            }
                            
                            if(data[1] != null) {
                                TextView accelStatusView = (TextView) logActivity
                                        .getInitDataProviderCoordinatorDialog()
                                        .findViewById(
                                                R.id.log_wait_for_ready_accel_status);
                                
                                accelStatusView.setText(R.string.log_wait_for_ready_text_ready);
                            }
                            
                            if(data[2] != null) {
                                TextView ecuStatusView = (TextView) logActivity
                                        .getInitDataProviderCoordinatorDialog()
                                        .findViewById(
                                                R.id.log_wait_for_ready_ecu_status);
                                
                                ecuStatusView.setText(R.string.log_wait_for_ready_text_ready);
                            }
                            
                            break;
                        case READY:
                            logActivity.getInitDataProviderCoordinatorDialog().dismiss();
                            logActivity.getWaitingForStartTriggerDialog().show();
                            logActivity.displayTask.start();
                            break;
                        case TIMING_START_TRIGGER_FIRED:
                            logActivity.getInitDataProviderCoordinatorDialog().dismiss();
                            logActivity.waitingForStartTriggerDialog.dismiss();
                            if (!logActivity.displayTask.isAlive()) {
                                logActivity.displayTask.start();
                            }
                            break;
                        case TIMING_DATA_UPDATE:
                            if (!logActivity.displayTask.isAlive()) {
                                logActivity.displayTask.start();
                            }
                            TimingData timingData = (TimingData) msg.obj;
                            logActivity.updateEventBasedUiFields(timingData);
                            break;
                        case LOGGING_FAILED:
                            logActivity.onDataProviderCoordinatorLoggingError();
                            break;
                    }
                } catch (Exception e) {
                    LOG.error("Error handling message " + msg + ".", e);
                    logActivity.onDataProviderCoordinatorLoggingError();
                }
            }
        }
    }
}

