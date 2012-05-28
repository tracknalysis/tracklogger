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

import org.apache.log4j.Level;

import de.mindpipe.android.logging.log4j.LogConfigurator;
import android.app.Application;
import android.os.Environment;

/**
 * @author David Valeri
 */
public class TrackLogger extends Application {
    
    private static LogConfigurator LOG_CONFIGURATOR;
    
    @Override
    public void onCreate() {
        super.onCreate();
        
        File outputDir = new File(Environment.getExternalStorageDirectory(), "TrackLog");
        if (!outputDir.exists()) {
            if (!outputDir.mkdirs()) {
                throw new RuntimeException("Could not create log output directory.");
            }
        }
        
        File logFile = new File(outputDir, "tracklog.log");
        
        LOG_CONFIGURATOR = new LogConfigurator();
        LOG_CONFIGURATOR.setUseFileAppender(false);
        LOG_CONFIGURATOR.setFileName(logFile.getAbsolutePath());
        LOG_CONFIGURATOR.setRootLevel(Level.DEBUG);
        LOG_CONFIGURATOR.configure();
    }
}
