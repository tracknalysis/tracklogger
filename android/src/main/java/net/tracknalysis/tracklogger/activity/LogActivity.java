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
import net.tracknalysis.common.io.SocketManager;
import net.tracknalysis.location.LocationManager;
import net.tracknalysis.location.Route;
import net.tracknalysis.location.Waypoint;
import net.tracknalysis.location.nmea.NmeaLocationManager;
import net.tracknalysis.tracklogger.dataprovider.AccelData;
import net.tracknalysis.tracklogger.dataprovider.AccelDataProvider;
import net.tracknalysis.tracklogger.dataprovider.AndroidTrackLoggerDataProviderCoordinator;
import net.tracknalysis.tracklogger.dataprovider.EcuData;
import net.tracknalysis.tracklogger.dataprovider.EcuDataProvider;
import net.tracknalysis.tracklogger.dataprovider.LocationData;
import net.tracknalysis.tracklogger.dataprovider.LocationDataProvider;
import net.tracknalysis.tracklogger.dataprovider.TimingDataProvider;
import net.tracknalysis.tracklogger.dataprovider.TrackLoggerDataProviderCoordinator;
import net.tracknalysis.tracklogger.dataprovider.AndroidAccelDataProvider;
import net.tracknalysis.tracklogger.dataprovider.ecu.MegasquirtEcuDataProvider;
import net.tracknalysis.tracklogger.dataprovider.location.LocationManagerLocationDataProvider;
import net.tracknalysis.tracklogger.dataprovider.timing.RouteManagerTimingDataProvider;
import net.tracknalysis.tracklogger.R;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.hardware.SensorManager;
import android.os.AsyncTask;
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
    private BluetoothDevice btGpsDevice;
    private LocationManager locationManager;
    private LocationDataProvider locationDataProvider;
    
    // ECU
    private SocketManager ecuSocketManager;
    private BluetoothDevice btEcuDevice;
    private EcuDataProvider ecuDataProvider;
    
    // Timing
    private TimingDataProvider timingDataProvider;
    
    // Wake
    private WakeLock wakeLock;
    
    // Display
    private DisplayTask displayTask;
    private ProgressDialog initDataProviderCoordinatorProgressDialog;
    
    private TrackLoggerDataProviderCoordinator dataProviderCoordinator;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
        wakeLock = pm.newWakeLock(
                PowerManager.SCREEN_BRIGHT_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP,
                LogActivity.class.getName());
        
        btAdapter = BluetoothAdapter.getDefaultAdapter();
        displayTask = new DisplayTask();
        
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
        if (dataProviderCoordinator != null) {
            dataProviderCoordinator.stop();
        }
        
        if (accelDataProvider != null) {
            accelDataProvider.stop();
        }
        
        if (locationDataProvider != null) {
            locationDataProvider.stop();
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
            displayTask.cancel(true);
        }
        
        if (initDataProviderCoordinatorProgressDialog != null) {
            initDataProviderCoordinatorProgressDialog.dismiss();
        }
        
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
        if (dialog == initDataProviderCoordinatorProgressDialog) {
            finish();
        }
    }
    
    protected void initDataProviderCoordinator() {
        btGpsDevice = btAdapter.getRemoteDevice("00:1C:88:13:F5:64");
        btEcuDevice = btAdapter.getRemoteDevice("00:12:6F:25:68:DE");
        
        try {
            gpsSocketManager = new BtSocketManager(btGpsDevice.getAddress(),
                    btAdapter, BtProfile.SPP);
            locationManager = new NmeaLocationManager(gpsSocketManager);
            
            
            locationDataProvider = new LocationManagerLocationDataProvider(locationManager);

            SensorManager sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
            WindowManager windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
            accelDataProvider = new AndroidAccelDataProvider(sensorManager, windowManager);

            ecuSocketManager = new BtSocketManager(btEcuDevice.getAddress(),
                    btAdapter, BtProfile.SPP);
            ecuDataProvider = new MegasquirtEcuDataProvider(ecuSocketManager);
            
            Route route = new Route("My Route", Arrays.asList(
                    new Waypoint("1", 38.979896545410156d, -77.54102325439453d),
                    new Waypoint("2", 38.98295974731445d, -77.53973388671875d),
                    new Waypoint("3", 38.982906341552734d, -77.54007720947266d),
                    new Waypoint("4", 38.972618103027344d, -77.54145050048828d),
                    new Waypoint("5", 38.97257995605469d, -77.5412826538086d)));
            
            timingDataProvider = new RouteManagerTimingDataProvider(locationManager.getRouteManager(), route);
            
            // Handles events from the data provider coordinator
            Handler dataProviderCoordinatorHandler = new Handler() {
                @Override
                public void handleMessage(Message msg) {
                    
                    if (msg.what == TrackLoggerDataProviderCoordinator.NotificationType
                            .READY_PROGRESS.getNotificationTypeId()) {
                        // TODO display feedback to user
                    } else if (msg.what == TrackLoggerDataProviderCoordinator.NotificationType
                            .READY.getNotificationTypeId()) {
                        initDataProviderCoordinatorProgressDialog.dismiss();
                        dataProviderCoordinator.startLogging();
                    } else if (msg.what == TrackLoggerDataProviderCoordinator.NotificationType
                            .LOGGING_START_TRIGGER_FIRED.getNotificationTypeId()) {
                        initDataProviderCoordinatorProgressDialog.dismiss();
                        displayTask.execute();
                    } else if (msg.what == TrackLoggerDataProviderCoordinator.NotificationType
                            .LOGGING_FAILED.getNotificationTypeId()) {
                        onDataProviderCoordinatorLoggingError();
                    }
                }
            };
            
            dataProviderCoordinator = new AndroidTrackLoggerDataProviderCoordinator(
                    dataProviderCoordinatorHandler, getApplication(), accelDataProvider, 
                    locationDataProvider, ecuDataProvider, timingDataProvider);
            
            // Do the start(s) in another thread so that we don't block the UI. 
            Thread t = new Thread() {
                public void run() {
                    locationManager.start();
                    dataProviderCoordinator.start();
                };
            };
            t.start();
            
            initDataProviderCoordinatorProgressDialog = 
                    ProgressDialog.show(
                            this, 
                            "Waiting for data providers...",
                            null,
                            true,
                            true,
                            this);
            
        } catch (Exception e) {
            LOG.error("Fatal error while initializing data providers and coordinator.", e);
            onInitDataProviderCoordinatorError();
        }
    }
    
    protected void onInitDataProviderCoordinatorError() {
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
    
    protected class DisplayTask extends AsyncTask<Void, Void, Void> {
       
        @Override
        protected Void doInBackground(Void... params) {
            // TODO, can't use isCancelled inside the task as it doesn't change state until the thread stops
            while(!isCancelled()) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    if (!isCancelled()) {
                        LOG.error("Interrupted display task while running.", e);
                    } else {
                        LOG.debug("Interrupted display task while running.", e);
                    }
                }
                
                publishProgress();
            }
            
            return null;
        }
        
        protected void onProgressUpdate(Void... progress) {
            TextView tv;
            
            AccelData accelData = accelDataProvider.getCurrentData();
            LocationData gpsData = locationDataProvider.getCurrentData();
            EcuData ecuData = ecuDataProvider.getCurrentData();
            
            if (accelData != null) {
                tv = (TextView) findViewById(R.id.accelUpdateFreq);
                tv.setText(String.valueOf(accelDataProvider.getUpdateFrequency()));
                
                tv = (TextView) findViewById(R.id.lateralA);
                tv.setText(String.valueOf(accelData.getLateral()));
                
                tv = (TextView) findViewById(R.id.verticalA);
                tv.setText(String.valueOf(accelData.getVertical()));
                
                tv = (TextView) findViewById(R.id.longitudinalA);
                tv.setText(String.valueOf(accelData.getLongitudinal()));
            }
            
            if (gpsData != null) {
                tv = (TextView) findViewById(R.id.gpsUpdateFreq);
                tv.setText(String.valueOf(locationDataProvider.getUpdateFrequency()));
                
                tv = (TextView) findViewById(R.id.latitude);
                tv.setText(String.valueOf(gpsData.getLatitude()));
                
                tv = (TextView) findViewById(R.id.longitude);
                tv.setText(String.valueOf(gpsData.getLongitude()));
                
                tv = (TextView) findViewById(R.id.altitude);
                tv.setText(String.valueOf(gpsData.getAltitude()));
            }
            
            if (ecuData != null) {
                tv = (TextView) findViewById(R.id.ecuUpdateFreq);
                tv.setText(String.valueOf(ecuDataProvider.getUpdateFrequency()));
                
                tv = (TextView) findViewById(R.id.rpm);
                tv.setText(String.valueOf(ecuData.getRpm()));
                
                tv = (TextView) findViewById(R.id.throttle);
                tv.setText(String.valueOf(ecuData.getThrottlePosition()));
                
                tv = (TextView) findViewById(R.id.map);
                tv.setText(String.valueOf(ecuData.getManifoldAbsolutePressure()));
            }
        }
    }
}

