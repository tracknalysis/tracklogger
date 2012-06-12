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
package net.tracknalysis.tracklogger.config;

import org.apache.log4j.Level;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

/**
 * @author David Valeri
 */
public class AndroidConfiguration implements Configuration {
    
    private final SharedPreferences sharedPrefs;
    
    public AndroidConfiguration(Context context) {
        super();
        sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);
    }

    @Override
    public Level getDefaultLogLevel() {
        return Level.toLevel(sharedPrefs.getString(
                "log.level", Level.INFO.toString()));
    }
    
    @Override
    public boolean isLogToFile() {
        return sharedPrefs.getBoolean("log.file.enable", false);
    }

    @Override
    public boolean isEcuLoggingEnabled() {
        return sharedPrefs.getBoolean("ecu.enabled", false);
    }
    
    @Override
    public String getEcuBtAddress() {
        return sharedPrefs.getString("ecu.bt.address", "");
    }
    
    @Override
    public String getLocationBtAddress() {
        return sharedPrefs.getString("location.bt.address", "");
    }
}
