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

import java.util.Arrays;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.tracknalysis.common.android.io.BtSocketManager;
import net.tracknalysis.common.android.io.BtSocketManager.BtProfile;
import net.tracknalysis.common.android.notification.AndroidNotificationStrategy;
import net.tracknalysis.common.concurrent.GracefulShutdownThread;
import net.tracknalysis.common.io.SocketManager;
import net.tracknalysis.location.LocationManager;
import net.tracknalysis.location.Route;
import net.tracknalysis.location.Waypoint;
import net.tracknalysis.location.nmea.NmeaLocationManager;
import net.tracknalysis.tracklogger.config.Configuration;
import net.tracknalysis.tracklogger.config.ConfigurationFactory;
import net.tracknalysis.tracklogger.dataprovider.AccelData;
import net.tracknalysis.tracklogger.dataprovider.AccelDataProvider;
import net.tracknalysis.tracklogger.dataprovider.EcuData;
import net.tracknalysis.tracklogger.dataprovider.EcuDataProvider;
import net.tracknalysis.tracklogger.dataprovider.LocationData;
import net.tracknalysis.tracklogger.dataprovider.LocationDataProvider;
import net.tracknalysis.tracklogger.dataprovider.TimingData;
import net.tracknalysis.tracklogger.dataprovider.TimingDataProvider;
import net.tracknalysis.tracklogger.dataprovider.TrackLoggerDataProviderCoordinator;
import net.tracknalysis.tracklogger.dataprovider.android.AndroidAccelDataProvider;
import net.tracknalysis.tracklogger.dataprovider.android.AndroidTrackLoggerDataProviderCoordinator;
import net.tracknalysis.tracklogger.dataprovider.ecu.MegasquirtEcuDataProvider;
import net.tracknalysis.tracklogger.dataprovider.location.LocationManagerLocationDataProvider;
import net.tracknalysis.tracklogger.dataprovider.timing.RouteManagerTimingDataProvider;
import net.tracknalysis.tracklogger.R;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.view.WindowManager;
import android.widget.TextView;

/**
 * @author David Valeri
 */
public class LogActivity extends Activity implements OnCancelListener {
    
    private static final Logger LOG = LoggerFactory.getLogger(LogActivity.class);
    
    private static final int ACTION_REQUEST_BT_ENABLE = 0;
    
    private BluetoothAdapter btAdapter;
    
    // Accel
    private AccelDataProvider accelDataProvider;
    
    // Location
    private SocketManager gpsSocketManager;
    private LocationManager locationManager;
    private LocationDataProvider locationDataProvider;
    
    // ECU
    private SocketManager ecuSocketManager;
    private EcuDataProvider ecuDataProvider;
    
    // Timing
    private TimingDataProvider timingDataProvider;
    
    // Wake
    private WakeLock wakeLock;
    
    // Display
    private DisplayTask displayTask;
    private Dialog initDataProviderCoordinatorDialog;
    private ProgressDialog waitingForStartTriggerDialog;
    
    private TrackLoggerDataProviderCoordinator dataProviderCoordinator;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
        wakeLock = pm.newWakeLock(
                PowerManager.SCREEN_BRIGHT_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP,
                LogActivity.class.getName());
        
        btAdapter = BluetoothAdapter.getDefaultAdapter();
        displayTask = new DisplayTask();
        
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
        
        setContentView(R.layout.log);
        
        if (btAdapter == null) {
            // TODO no BT
        }
        
        if (!btAdapter.isEnabled()) {
            startActivityForResult(
                    new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE),
                    ACTION_REQUEST_BT_ENABLE);
        } else {
            initDataProviderCoordinator();
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
    public void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        switch (requestCode) {
            case ACTION_REQUEST_BT_ENABLE:
                if (resultCode == Activity.RESULT_OK) {
                    initDataProviderCoordinator();
                } else {
                    // TODO no BT
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
        if (dialog == initDataProviderCoordinatorDialog || dialog == waitingForStartTriggerDialog) {
            finish();
        }
    }
    
    protected void cleanup() {
        if (dataProviderCoordinator != null) {
            dataProviderCoordinator.stop();
        }
        
        if (accelDataProvider != null) {
            accelDataProvider.stop();
        }
        
        if (locationDataProvider != null) {
            locationDataProvider.stop();
        }
        
        if (timingDataProvider != null) {
            timingDataProvider.stop();
        }
        
        if (locationManager != null) {
            locationManager.stop();
        }
        silentSocketManagerDisconnect(gpsSocketManager);
        
        if (ecuDataProvider != null) {
            ecuDataProvider.stop();
        }
        silentSocketManagerDisconnect(ecuSocketManager);
        
        if (displayTask != null) {
            displayTask.cancel();
        }
        
        if (initDataProviderCoordinatorDialog != null) {
            initDataProviderCoordinatorDialog.dismiss();
        }
        
        if (waitingForStartTriggerDialog != null) {
            initDataProviderCoordinatorDialog.dismiss();
        }
    }
    
    protected void initDataProviderCoordinator() {
        Configuration config = ConfigurationFactory.getInstance().getConfiguration();
        
        try {
            // Handles events from the data provider coordinator
            Handler dataProviderCoordinatorHandler = new Handler() {
                @Override
                public void handleMessage(Message msg) {
                    LOG.debug("Handling data provider coordinator message: {}.", msg);
                    
                    TrackLoggerDataProviderCoordinator.NotificationType type = 
                            TrackLoggerDataProviderCoordinator.NotificationType.fromInt(msg.what);
                    
                    switch (type) {
                        case START_FAILED:
                            onInitDataProviderCoordinatorError();
                            break;
                        case READY_PROGRESS:
                            Object[] data = (Object[]) msg.obj;
                            if (data[0] != null) {
                                TextView locationStatusView = (TextView) initDataProviderCoordinatorDialog
                                        .findViewById(R.id.locationStatus);
                                
                                locationStatusView.setText("OK");
                            }
                            
                            if(data[1] != null) {
                                TextView accelStatusView = (TextView) initDataProviderCoordinatorDialog
                                        .findViewById(R.id.accelStatus);
                                
                                accelStatusView.setText("OK");
                            }
                            
                            if(data[2] != null) {
                                TextView ecuStatusView = (TextView) initDataProviderCoordinatorDialog
                                        .findViewById(R.id.ecuStatus);
                                
                                ecuStatusView.setText("OK");
                            }
                            
                            break;
                        case READY:
                            initDataProviderCoordinatorDialog.dismiss();
                            waitingForStartTriggerDialog.show();
                            dataProviderCoordinator.startLogging();
                            displayTask.start();
                            break;
                        case TIMING_START_TRIGGER_FIRED:
                            initDataProviderCoordinatorDialog.dismiss();
                            waitingForStartTriggerDialog.dismiss();
                            
                            break;
                        case LOGGING_FAILED:
                            onDataProviderCoordinatorLoggingError();
                            break;
                    }
                }
            };
            
            // Handles events from the location manager
            Handler locationManagerHandler = new Handler() {
                @Override
                public void handleMessage(Message msg) {
                    LOG.debug("Handling location manager message: {}.", msg);
                    
                    NmeaLocationManager.NotificationType type = 
                            NmeaLocationManager.NotificationType.fromInt(msg.what);
                    
                    switch (type) {
                        case START_FAILED:
                            onInitDataProviderCoordinatorError();
                            break;
                    }
                }
            };
            
            gpsSocketManager = new BtSocketManager(config.getLocationBtAddress(),
                    btAdapter, BtProfile.SPP);
            locationManager = new NmeaLocationManager(gpsSocketManager,
                    new AndroidNotificationStrategy(locationManagerHandler));
            
            
            locationDataProvider = new LocationManagerLocationDataProvider(locationManager);

            SensorManager sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
            WindowManager windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
            accelDataProvider = new AndroidAccelDataProvider(sensorManager, windowManager);

            if (config.isEcuLoggingEnabled()) {
                ecuSocketManager = new BtSocketManager(config.getEcuBtAddress(),
                        btAdapter, BtProfile.SPP);
                ecuDataProvider = new MegasquirtEcuDataProvider(ecuSocketManager);
            }
            
            // TODO route from external source
            Route route = new Route("My Route", Arrays.asList(
                    new Waypoint("1", 38.979896545410156d, -77.54102325439453d),
                    new Waypoint("2", 38.98295974731445d, -77.53973388671875d),
                    new Waypoint("3", 38.982906341552734d, -77.54007720947266d),
                    new Waypoint("4", 38.972618103027344d, -77.54145050048828d),
                    new Waypoint("5", 38.97257995605469d, -77.5412826538086d)));
            
            timingDataProvider = new RouteManagerTimingDataProvider(
                    locationManager.getRouteManager(), route);
            
            dataProviderCoordinator = new AndroidTrackLoggerDataProviderCoordinator(
                    dataProviderCoordinatorHandler, getApplication(), accelDataProvider, 
                    locationDataProvider, ecuDataProvider, timingDataProvider);
            
            initDataProviderCoordinatorDialog.show();
            
            // Do the start(s) in another thread so that we don't block the UI thread.  Notifications
            // are used to apprise the UI of the progress, but we still capture and log any exceptions
            // that are thrown.
            Thread t = new Thread() {
                public void run() {
                    try {
                        locationManager.start();
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
    
    protected void onInitDataProviderCoordinatorError() {
        initDataProviderCoordinatorDialog.dismiss();
        waitingForStartTriggerDialog.dismiss();
        
        cleanup();
        
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder
                .setMessage("Error initializing data providers.  Check the logs for more details.")
                .setTitle("TrackLog Error")
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
                .setTitle("TrackLog Error")
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
    
    protected void silentSocketManagerDisconnect(SocketManager socketManager) {
        if (socketManager != null) {
            try {
                socketManager.disconnect();
            } catch (Exception e1) {
                LOG.warn("Error disconnecting BlueTooth connection with manager.", e1);
            }
        }
    }
    
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
                
                LogActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        TextView tv;

                        AccelData accelData = accelDataProvider
                                .getCurrentData();
                        LocationData gpsData = locationDataProvider
                                .getCurrentData();
                        TimingData timingData = timingDataProvider
                                .getCurrentData();
                        EcuData ecuData = null;
                        
                        if (ecuDataProvider != null) {
                            ecuData = ecuDataProvider.getCurrentData();
                        }
                        
                        if (timingData != null) {
                            tv = (TextView) findViewById(R.id.lap);
                            tv.setText(String.valueOf(timingData.getLap()));
                            
                            tv = (TextView) findViewById(R.id.splitIndex);
                            tv.setText(String.valueOf(timingData.getSplitIndex()));
                            
                            if (timingData.getLapTime() != null) {
                                tv = (TextView) findViewById(R.id.lapTime);
                                tv.setText(String.valueOf(timingData.getLapTime()));
                            }
                        }

                        if (accelData != null) {
                            
                            tv = (TextView) findViewById(R.id.accelUpdateFreq);
                            tv.setText(String.format("%.3f",
                                    accelDataProvider.getUpdateFrequency()));

                            tv = (TextView) findViewById(R.id.lateralA);
                            tv.setText(String.format("%.3f",
                                    accelData.getLateral()));

                            tv = (TextView) findViewById(R.id.verticalA);
                            tv.setText(String.format("%.3f",
                                    accelData.getVertical()));

                            tv = (TextView) findViewById(R.id.longitudinalA);
                            tv.setText(String.format("%.3f",
                                    accelData.getLongitudinal()));
                        }

                        if (gpsData != null) {
                            tv = (TextView) findViewById(R.id.gpsUpdateFreq);
                            tv.setText(String.format("%.3f",
                                    locationDataProvider.getUpdateFrequency()));

                            tv = (TextView) findViewById(R.id.latitude);
                            tv.setText(String.format("%.8f",
                                    gpsData.getLatitude()));

                            tv = (TextView) findViewById(R.id.longitude);
                            tv.setText(String.format("%.8f",
                                    gpsData.getLongitude()));

                            tv = (TextView) findViewById(R.id.altitude);
                            tv.setText(String.format("%.3f",
                                    gpsData.getAltitude()));
                        }

                        if (ecuData != null) {
                            tv = (TextView) findViewById(R.id.ecuUpdateFreq);
                            tv.setText(String.valueOf(ecuDataProvider
                                    .getUpdateFrequency()));

                            tv = (TextView) findViewById(R.id.rpm);
                            tv.setText(String.valueOf(ecuData.getRpm()));

                            tv = (TextView) findViewById(R.id.throttle);
                            tv.setText(String.valueOf(ecuData
                                    .getThrottlePosition()));

                            tv = (TextView) findViewById(R.id.map);
                            tv.setText(String.valueOf(ecuData
                                    .getManifoldAbsolutePressure()));
                        }
                    }
                });
            }
        }
    }
}

