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

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.tracknalysis.common.concurrent.GracefulShutdownThread;
import net.tracknalysis.common.util.TimeUtil;
import net.tracknalysis.tracklogger.config.Configuration;
import net.tracknalysis.tracklogger.config.ConfigurationFactory;
import net.tracknalysis.tracklogger.dataprovider.AccelData;
import net.tracknalysis.tracklogger.dataprovider.EcuData;
import net.tracknalysis.tracklogger.dataprovider.LocationData;
import net.tracknalysis.tracklogger.dataprovider.TimingData;
import net.tracknalysis.tracklogger.dataprovider.TrackLoggerDataProviderCoordinator;
import net.tracknalysis.tracklogger.dataprovider.android.TrackLoggerDataProviderCoordinatorFactory;
import net.tracknalysis.tracklogger.R;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.widget.TextView;

/**
 * @author David Valeri
 */
public class LogActivity extends Activity implements OnCancelListener {
    
    private static final Logger LOG = LoggerFactory.getLogger(LogActivity.class);
    
    private static final int ACTION_REQUEST_BT_ENABLE = 1;
    
    private static final DataProviderCoordinatorHandler DATA_PROVIDER_COORDINATOR_HANDLER = 
            new DataProviderCoordinatorHandler();
    
    private BluetoothAdapter btAdapter;
    
    private Configuration config = ConfigurationFactory.getInstance().getConfiguration();    
    
    // Timing
    private volatile Long lapStartReceivedTime;
    private volatile Long sessionStartReceivedTime;
    private volatile Long previousBestLapTime;
    private volatile List<Long> previousBestSplitTimes;
    
    // Wake
    private WakeLock wakeLock;
    
    // Data Coordinator
    private TrackLoggerDataProviderCoordinator dataProviderCoordinator;
    
    // Display
    private DisplayTask displayTask;
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
     * Vertical acceleration in m/s^2/
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
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
        wakeLock = pm.newWakeLock(
                PowerManager.SCREEN_BRIGHT_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP,
                LogActivity.class.getName());
        
        btAdapter = BluetoothAdapter.getDefaultAdapter();
        
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
        
        periodicUiUpdateRunnable = new Runnable() {
            
            @Override
            public void run() {
                AccelData accelData = dataProviderCoordinator.getCurrentAccelData();
                LocationData locationData = dataProviderCoordinator.getCurrentLocationData();
                EcuData ecuData = null;
                
                
                ecuData = dataProviderCoordinator.getCurrentEcuData();
                
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
                    setTextIfShown(accelUpdateFreq, "%.3f", dataProviderCoordinator.getAccelDataUpdateFrequency());
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
                            dataProviderCoordinator.getLocationDataUpdateFrequency());
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
                    setTextIfShown(ecuUpdateFreq, "%.3f", dataProviderCoordinator.getEcuDataUpdateFrequency());
                    setTextIfShown(rpm, "%d", ecuData.getRpm());
                    setTextIfShown(map, "%.0f", ecuData.getManifoldAbsolutePressure());
                    setTextIfShown(tp, "%.0f", ecuData.getThrottlePosition() * 100);
                    setTextIfShown(afr, "%.1f", ecuData.getAirFuelRatio());
                    setTextIfShown(mat, "%.1f", ecuData.getManifoldAirTemperature());
                    setTextIfShown(clt, "%.1f", ecuData.getCoolantTemperature());
                    setTextIfShown(ignAdv, "%.1f", ecuData.getIgnitionAdvance());
                    setTextIfShown(batV, "%.1f", ecuData.getBatteryVoltage());
                }
            }
        };
        
        displayTask = new DisplayTask();
        
        if (btAdapter == null) {
            onError(R.string.error_bt_not_supported, R.string.error_alert_title);
        } else {
            if (!btAdapter.isEnabled()) {
                startActivityForResult(
                        new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE),
                        ACTION_REQUEST_BT_ENABLE);
            } else {
                initDataProviderCoordinator();
            }
        }
    }
    
    @Override
    protected void onStart() {
        super.onStart();
        wakeLock.acquire();
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        
    }
    
    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onStop() {
        wakeLock.release();
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        cleanup();
        
        super.onDestroy();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case ACTION_REQUEST_BT_ENABLE:
                if (resultCode == Activity.RESULT_OK) {
                    initDataProviderCoordinator();
                } else {
                    onError(R.string.error_bt_not_enabled, R.string.error_alert_title);
                }
                break;
            default:
                LOG.error(
                        "Got activity result that the LogActivity did not initiate.  "
                                + "Request code {}, result code {}, intent {}.",
                        new Object[] {requestCode, resultCode, data});
                finish();
        }
    }
    
    @Override
    public void onCancel(DialogInterface dialog) {
        if (dialog == initDataProviderCoordinatorDialog || dialog == waitingForStartTriggerDialog) {
            cleanup();
            finish();
        }
    }
    
    protected void cleanup() {
        if (dataProviderCoordinator != null) {
            dataProviderCoordinator.stop();
        }
        
        if (displayTask != null) {
            displayTask.cancel();
        }
        
        if (initDataProviderCoordinatorDialog != null) {
            initDataProviderCoordinatorDialog.dismiss();
        }
        
        if (waitingForStartTriggerDialog != null) {
            initDataProviderCoordinatorDialog.dismiss();
        }
        
        DATA_PROVIDER_COORDINATOR_HANDLER.setActivity(null);
    }
    
    protected void initDataProviderCoordinator() {
        try {
            DATA_PROVIDER_COORDINATOR_HANDLER.setActivity(this);
            
            dataProviderCoordinator = TrackLoggerDataProviderCoordinatorFactory
                    .getInstance().getTrackLoggerDataProviderCoordinator(
                            DATA_PROVIDER_COORDINATOR_HANDLER,
                            getApplication(), btAdapter);
            
            initDataProviderCoordinatorDialog.show();
            
            // Do the start(s) in another thread so that we don't block the UI thread.  Notifications
            // are used to apprise the UI of the progress, but we still capture and log any exceptions
            // that are thrown.
            Thread t = new Thread() {
                public void run() {
                    try {
                        dataProviderCoordinator.start();
                    } catch (Exception e) {
                        LOG.error("Error starting location manager and data provider coordinator.", e);
                    }
                };
            };
            t.start();
            
        } catch (Exception e) {
            LOG.error("Fatal error while initializing data providers and coordinator.", e);
            onInitDataProviderCoordinatorError();
        }
    }
    
    protected void onError(int errorMessage, int title) {
        initDataProviderCoordinatorDialog.dismiss();
        waitingForStartTriggerDialog.dismiss();
        
        cleanup();
        
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder
                .setMessage(errorMessage)
                .setTitle(title)
                .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.dismiss();
                        finish();
                    }
                });
        
        if (!isFinishing()) {
            builder.create().show();
        }
    }
    
    protected void onInitDataProviderCoordinatorError() {
        initDataProviderCoordinatorDialog.dismiss();
        waitingForStartTriggerDialog.dismiss();
        
        cleanup();
        
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder
                .setMessage("Error initializing data providers.  Check the logs for more details.")
                .setTitle("TrackLogger Error")
                .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.dismiss();
                        finish();
                    }
                });
        
        if (!isFinishing()) {
            builder.create().show();
        }
    }
    
    protected void onDataProviderCoordinatorLoggingError() {
        initDataProviderCoordinatorDialog.dismiss();
        waitingForStartTriggerDialog.dismiss();
        
        cleanup();
        
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder
                .setMessage("Error recording log data.  Check the logs for more details.")
                .setTitle("TrackLogger Error")
                .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.dismiss();
                        finish();
                    }
                });
        
        if (!isFinishing()) {
            builder.create().show();
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
    
    protected void setColorIfShown(TextView view, Integer color) {
        if (view != null) {
            view.setTextColor(color);
        }
    }
    
    protected void updateNonEventBasedUiFields() {
        runOnUiThread(periodicUiUpdateRunnable);
    }
    
    protected void updateEventBasedUiFields(TimingData timingData) {
        if (timingData.getLap() == 0) {
            sessionStartReceivedTime = timingData.getDataRecivedTime();
            lapStartReceivedTime = timingData.getDataRecivedTime();
        }
        
        if (timingData.getLapTime() != null) {
            // All subsequent timing events for a lap completion will
            // have a non-null lap time.  The timing start is handled
            // above.
            lapStartReceivedTime = timingData.getDataRecivedTime();
            setTextIfShown(lastLapTime, TimeUtil.formatDuration(timingData.getLapTime(),
                    false, true));
            
            long lapDelta = previousBestLapTime == null ? 0 : timingData.getBestLapTime() - timingData.getLapTime();
            setTextIfShown(lastLapTimeDelta, 
                    TimeUtil.formatDuration(lapDelta, false, true));
            
            int lapDeltaColor;
            if (lapDelta <=0) {
                lapDeltaColor = 0x00FF00;
            } else {
                lapDeltaColor = 0xFF0000;
            }
            setColorIfShown(lastLapTimeDelta, lapDeltaColor);
            
            setTextIfShown(bestLapTime, TimeUtil.formatDuration(timingData.getBestLapTime(), false, true));
            
            previousBestLapTime = timingData.getBestLapTime();
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
            if (splitDelta <=0) {
                splitDeltaColor = 0x00FF00;
            } else {
                splitDeltaColor = 0xFF0000;
            }
            setColorIfShown(lastSplitTimeDelta, splitDeltaColor);
            
            setTextIfShown(bestSplitTime, 
                    TimeUtil.formatDuration(
                            timingData.getBestSplitTimes().get(timingData.getSplitIndex()), false, true));
            
            
            previousBestSplitTimes = timingData.getBestSplitTimes();
        }
        
        setTextIfShown(splitIndex, "%d", timingData.getSplitIndex() + 1);
        
        setTextIfShown(lapNumber, "%d", timingData.getLap() + 1);
    }
    
    /**
     * A simple task to handle updating the regular data on the screen that is not event critical
     * (that is not timing data).  We don't use the activity thread from Android here because we
     * want to take advantage of the graceful shutdown logic in {@link GracefulShutdownThread}.
     */
    protected class DisplayTask extends GracefulShutdownThread {
        @Override
        public void run() {
            while(run) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    if (run) {
                        LOG.error("Interrupted display task while running.", e);
                    } else {
                        LOG.info("Interrupted display task while running.", e);
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
    
    protected static class DataProviderCoordinatorHandler extends Handler {
        
        private volatile LogActivity logActivity;
        
        protected void setActivity(LogActivity logActivity) {
            this.logActivity = logActivity;
        }
        
        @Override
        public void handleMessage(Message msg) {
            if (logActivity != null) {
            
                LOG.debug("Handling data provider coordinator message: {}.", msg);
                
                TrackLoggerDataProviderCoordinator.NotificationType type = 
                        TrackLoggerDataProviderCoordinator.NotificationType.fromInt(msg.what);
                
                switch (type) {
                    case START_FAILED:
                        logActivity.onInitDataProviderCoordinatorError();
                        break;
                    case READY_PROGRESS:
                        Object[] data = (Object[]) msg.obj;
                        if (data[0] != null) {
                            TextView locationStatusView = (TextView) logActivity.initDataProviderCoordinatorDialog
                                    .findViewById(R.id.locationStatus);
                            
                            locationStatusView.setText("OK");
                        }
                        
                        if(data[1] != null) {
                            TextView accelStatusView = (TextView) logActivity.initDataProviderCoordinatorDialog
                                    .findViewById(R.id.accelStatus);
                            
                            accelStatusView.setText("OK");
                        }
                        
                        if(data[2] != null) {
                            TextView ecuStatusView = (TextView) logActivity.initDataProviderCoordinatorDialog
                                    .findViewById(R.id.ecuStatus);
                            
                            ecuStatusView.setText("OK");
                        }
                        
                        break;
                    case READY:
                        logActivity.initDataProviderCoordinatorDialog.dismiss();
                        logActivity.waitingForStartTriggerDialog.show();
                        logActivity.dataProviderCoordinator.startLogging();
                        logActivity.displayTask.start();
                        break;
                    case TIMING_START_TRIGGER_FIRED:
                        logActivity.initDataProviderCoordinatorDialog.dismiss();
                        logActivity.waitingForStartTriggerDialog.dismiss();
                        break;
                    case TIMING_DATA_UPDATE:
                        TimingData timingData = (TimingData) msg.obj;
                        logActivity.updateEventBasedUiFields(timingData);
                    case LOGGING_FAILED:
                        logActivity.onDataProviderCoordinatorLoggingError();
                        break;
                }
            }
        }
    }
}

