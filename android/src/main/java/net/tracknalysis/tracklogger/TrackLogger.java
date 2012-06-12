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

import net.tracknalysis.tracklogger.config.AndroidConfiguration;
import net.tracknalysis.tracklogger.config.Configuration;
import net.tracknalysis.tracklogger.config.ConfigurationFactory;
import net.tracknalysis.tracklogger.config.DefaultConfigurationFactory;

import de.mindpipe.android.logging.log4j.LogConfigurator;
import android.app.Application;
import android.os.Environment;

/**
 * @author David Valeri
 */
public class TrackLogger extends Application {
    
    private static LogConfigurator LOG_CONFIGURATOR;
    
    public static final String SESSION_EXPORT_ACTION = TrackLogger.class
            .getPackage().getName() + "." + "SESSION_EXPORT_ACTION";
    
    @Override
    public void onCreate() {
        super.onCreate();
        
        DefaultConfigurationFactory.setConfiguration(new AndroidConfiguration(this));
        Configuration configuration = ConfigurationFactory.getInstance().getConfiguration();
        
        File outputDir = new File(Environment.getExternalStorageDirectory(), "TrackLogger");
        if (!outputDir.exists()) {
            if (!outputDir.mkdirs()) {
                throw new RuntimeException("Could not create log output directory.");
            }
        }
        
        File logFile = new File(outputDir, "tracklog.log");
        
        LOG_CONFIGURATOR = new LogConfigurator();
        LOG_CONFIGURATOR.setUseFileAppender(configuration.isLogToFile());
        LOG_CONFIGURATOR.setFileName(logFile.getAbsolutePath());
        LOG_CONFIGURATOR.setRootLevel(configuration.getDefaultLogLevel());
        LOG_CONFIGURATOR.setMaxFileSize(1024 * 1024 * 5);
        LOG_CONFIGURATOR.configure();
    }
}
