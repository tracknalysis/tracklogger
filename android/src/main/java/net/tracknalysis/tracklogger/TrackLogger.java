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
package net.tracknalysis.tracklogger;

import java.io.File;

import org.apache.log4j.Logger;

import net.tracknalysis.tracklogger.config.Configuration;
import net.tracknalysis.tracklogger.config.ConfigurationChangeEvent;
import net.tracknalysis.tracklogger.config.ConfigurationChangeListener;
import net.tracknalysis.tracklogger.config.ConfigurationFactory;
import net.tracknalysis.tracklogger.config.DefaultConfigurationFactory;
import net.tracknalysis.tracklogger.config.android.AndroidConfiguration;
import net.tracknalysis.tracklogger.dataprovider.android.DataProviderCoordinatorFactory;
import net.tracknalysis.tracklogger.dataprovider.android.DefaultDataProviderCoordinatorFactory;
import net.tracknalysis.tracklogger.dataprovider.android.DataProviderCoordinatorFactoryService;
import net.tracknalysis.tracklogger.dataprovider.android.DataProviderCoordinatorFactoryService.LocalBinder;

import de.mindpipe.android.logging.log4j.LogConfigurator;
import android.app.Application;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Environment;
import android.os.IBinder;

/**
 * @author David Valeri
 */
public class TrackLogger extends Application implements ConfigurationChangeListener {
    
    private static LogConfigurator LOG_CONFIGURATOR;
    
    public static final String SESSION_EXPORT_ACTION = TrackLogger.class
            .getPackage().getName() + "." + "SESSION_EXPORT_ACTION";
    
    private ServiceConnection serviceConnection;
    
    @Override
    public void onCreate() {
        super.onCreate();
        
        DefaultConfigurationFactory.setConfiguration(new AndroidConfiguration(this));
        Configuration configuration = ConfigurationFactory.getInstance().getConfiguration();
        
        configureLogging(configuration);
        configuration.addConfigurationChangeListenerListener(this);
        
        startService(new Intent(this, DataProviderCoordinatorFactoryService.class));
        
        serviceConnection = new ServiceConnection() {

            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                LocalBinder binder = (LocalBinder) service;
                DataProviderCoordinatorFactory
                        .setInstance(new DefaultDataProviderCoordinatorFactory(
                                binder.getService()));
            }

            @Override
            public void onServiceDisconnected(ComponentName arg0) {
                DataProviderCoordinatorFactory
                        .setInstance(null);
            }
        };
        
        bindService(new Intent(this,
                DataProviderCoordinatorFactoryService.class),
                serviceConnection, BIND_NOT_FOREGROUND);
    }
    
    @Override
    public void onTerminate() {
        unbindService(serviceConnection);
        stopService(new Intent(this, DataProviderCoordinatorFactoryService.class));
        super.onTerminate();
    }

    @Override
    public void onConfigurationChange(ConfigurationChangeEvent event) {
        if (event.isLogToFileChanged() || event.isRootLogLevelChanged()) {
            configureLogging(event.getConfiguration());
        }
    }
    
    protected synchronized void configureLogging(Configuration configuration) {
        Logger.getRootLogger().removeAllAppenders();
        
        File outputDir = new File(Environment.getExternalStorageDirectory(), "TrackLogger");
        if (!outputDir.exists()) {
            if (!outputDir.mkdirs()) {
                throw new RuntimeException("Could not create log output directory.");
            }
        }
        
        File logFile = new File(outputDir, "tracklog.log");
        
        if (LOG_CONFIGURATOR == null) {
            LOG_CONFIGURATOR = new LogConfigurator();
        }
        
        LOG_CONFIGURATOR.setUseFileAppender(configuration.isLogToFile());
        LOG_CONFIGURATOR.setFileName(logFile.getAbsolutePath());
        LOG_CONFIGURATOR.setRootLevel(configuration.getRootLogLevel());
        LOG_CONFIGURATOR.setMaxFileSize(1024 * 1024 * 5);
        LOG_CONFIGURATOR.configure();
    }
}
