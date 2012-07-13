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
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.log4j.Logger;

import net.tracknalysis.tracklogger._import.android.SplitMarkerSetFileFormat;
import net.tracknalysis.tracklogger._import.android.SplitMarkerSetImporterService;
import net.tracknalysis.tracklogger.config.Configuration;
import net.tracknalysis.tracklogger.config.ConfigurationChangeEvent;
import net.tracknalysis.tracklogger.config.ConfigurationChangeListener;
import net.tracknalysis.tracklogger.config.ConfigurationFactory;
import net.tracknalysis.tracklogger.config.DefaultConfigurationFactory;
import net.tracknalysis.tracklogger.config.android.AndroidConfiguration;
import net.tracknalysis.tracklogger.dataprovider.android.DataProviderCoordinatorManagerService;
import net.tracknalysis.tracklogger.export.android.SessionExporterService;
import net.tracknalysis.tracklogger.provider.TrackLoggerData;

import de.mindpipe.android.logging.log4j.LogConfigurator;
import android.app.Application;
import android.content.Intent;
import android.os.Environment;

/**
 * @author David Valeri
 */
public class TrackLogger extends Application implements ConfigurationChangeListener {
    
    private static final AtomicInteger NOTIFICATION_COUNTER = new AtomicInteger();
    private static LogConfigurator LOG_CONFIGURATOR;
    
    /**
     * Start the activity to configure and trigger the export of data from a
     * session.
     * 
     * <p>
     * Input: {@link Intent#getData} is the session URI from which to export data. See
     * {@link TrackLoggerData.Session#SESSION_ITEM_TYPE}.
     * <p>
     * Output: nothing.
     */
    public static final String ACTION_SESSION_EXPORT_CONFIG = TrackLogger.class
            .getPackage().getName() + "." + "SESSION_EXPORT_CONFIG_ACTION";
    
    /**
     * Start the export process for a session in the background.
     * 
     * <p>
     * Input: {@link Intent#getData} is the session URI from which to export data. See
     * {@link TrackLoggerData.Session#SESSION_ITEM_TYPE}.
     * <p>Output: nothing.
     * <p>Extras:
     * <ul>
     *   <li>{@link SessionExporterService#EXTRA_EXPORT_FORMAT} - (String) The optional identifier for the desired export format.</li>
     *   <li>{@link SessionExporterService#EXTRA_EXPORT_START_LAP} - (int) The optional value for the lap to start exporting from.</li>
     *   <li>{@link SessionExporterService#EXTRA_EXPORT_STOP_LAP} - (int) The optional value for the lap to stop exporting on.</li>
     * </ul> 
     */
    public static final String ACTION_SESSION_EXPORT = TrackLogger.class
            .getPackage().getName() + "." + "SESSION_EXPORT_ACTION";
    
    /**
     * Start the import process for a split marker set in the background.
     * 
     * <p>
     * Input: {@link Intent#getData} is the URI from which to import data.
     * <p>Output: nothing.
     * <p>Extras:
     * <ul>
     *   <li>{@link SplitMarkerSetImporterService#EXTRA_IMPORT_FORMAT} - (int) The identifier for the format of the import file.  See {@link SplitMarkerSetFileFormat}.</li>
     *   <li>{@link SplitMarkerSetImporterService#EXTRA_NAME} - (String) The optional value for the name of the imported split marker set.</li>
     * </ul> 
     */
    public static final String ACTION_SPLIT_MARKER_SET_IMPORT = TrackLogger.class
            .getPackage().getName() + "." + "SPLIT_MARKER_SET_IMPORT";
    
    /**
     * Start the activity that configures the import of a split marker set.
     *
     * <p>Input: nothing.
     * <p>Output: nothing.
     */
    public static final String ACTION_SPLIT_MARKER_SET_CONFIGURE_IMPORT = TrackLogger.class
            .getPackage().getName() + "." + "SPLIT_MARKER_SET_CONFIGURE_IMPORT";
    
    /**
     * Start the activity that renames an entity.
     *
     * <p>Input: {@link Intent#getData} is the URI for the data entity to rename.
     * <p>Output: nothing.
     */
    public static final String ACTION_RENAME = TrackLogger.class
            .getPackage().getName() + "." + "RENAME";
    
    public static int getUniqueNotificationId() {
        return NOTIFICATION_COUNTER.getAndIncrement();
    }
    
    @Override
    public void onCreate() {
        super.onCreate();
        
        DefaultConfigurationFactory.setConfiguration(new AndroidConfiguration(this));
        Configuration configuration = ConfigurationFactory.getInstance().getConfiguration();
        
        configureLogging(configuration);
        configuration.addConfigurationChangeListenerListener(this);
    }
    
    @Override
    public void onTerminate() {
        stopService(new Intent(this, DataProviderCoordinatorManagerService.class));
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
