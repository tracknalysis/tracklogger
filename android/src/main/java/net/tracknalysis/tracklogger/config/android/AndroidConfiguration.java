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
package net.tracknalysis.tracklogger.config.android;

import java.io.File;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import net.tracknalysis.tracklogger.R;
import net.tracknalysis.tracklogger.config.Configuration;
import net.tracknalysis.tracklogger.config.ConfigurationChangeEvent;
import net.tracknalysis.tracklogger.config.ConfigurationChangeEvent.ConfigurationChangeEventBuilder;
import net.tracknalysis.tracklogger.config.ConfigurationChangeListener;

import org.apache.log4j.Level;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Environment;
import android.preference.PreferenceManager;

/**
 * @author David Valeri
 */
public class AndroidConfiguration implements Configuration, OnSharedPreferenceChangeListener {
    
    private static final Logger LOG = LoggerFactory.getLogger(AndroidConfiguration.class);
    
    private static final String ROOT_LOG_LEVEL_KEY = "app.log.level";
    private static final String LOG_TO_FILE_KEY = "app.log.file.enable";
    private static final String ECU_ENABLED_KEY = "ecu.enabled";
    private static final String ECU_BT_ADDRESS_KEY = "ecu.bt.address";
    private static final String LOCATION_BT_ADDRESS_KEY = "location.bt.address";
    private static final String LOG_LAYOUT_ID_KEY = "log.layout.id";
    private static final String DATA_DIR_KEY = "data.dir.path";
    
    private final SharedPreferences sharedPrefs;
    
    private final List<ConfigurationChangeListener> listeners = 
            new CopyOnWriteArrayList<ConfigurationChangeListener>();
    
    private final Context context;
    
    public AndroidConfiguration(Context context) {
        super();
        sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);
        sharedPrefs.registerOnSharedPreferenceChangeListener(this);
        this.context = context;
    }
    
    
    @Override
    protected void finalize() throws Throwable {
        sharedPrefs.unregisterOnSharedPreferenceChangeListener(this);
        listeners.clear();
        super.finalize();
    }

    @Override
    public Level getRootLogLevel() {
        return Level.toLevel(sharedPrefs.getString(
                ROOT_LOG_LEVEL_KEY, Level.WARN.toString()));
    }
    
    @Override
    public void setRootLogLevel(Level level) {
        if (level == null) {
            throw new NullPointerException("level cannot be null.");
        }
        
        Editor editor = sharedPrefs.edit();
        editor.putString(ROOT_LOG_LEVEL_KEY, level.toString());
        editor.commit();
    }
    
    @Override
    public boolean isLogToFile() {
        return sharedPrefs.getBoolean(LOG_TO_FILE_KEY, true);
    }
    
    @Override
    public void setLogToFile(boolean enabled) {
        Editor editor = sharedPrefs.edit();
        editor.putBoolean(LOG_TO_FILE_KEY, enabled);
        editor.commit();
    }

    @Override
    public boolean isEcuEnabled() {
        return sharedPrefs.getBoolean(ECU_ENABLED_KEY, false);
    }
    
    @Override
    public String getEcuBtAddress() {
        return sharedPrefs.getString(ECU_BT_ADDRESS_KEY, "");
    }
    
    @Override
    public String getLocationBtAddress() {
        return sharedPrefs.getString(LOCATION_BT_ADDRESS_KEY, "");
    }
    
    @Override
    public int getLogLayoutId() {
        String name = sharedPrefs.getString(LOG_LAYOUT_ID_KEY, context.getResources()
                .getResourceName(R.layout.log_default));
        
        return context.getResources().getIdentifier(name, null, null); 
    }
    
    @Override
    public void setLogLayoutId(int id) {
        Editor editor = sharedPrefs.edit();
        editor.putString(LOG_LAYOUT_ID_KEY, context.getResources().getResourceName(id));
        editor.commit();   
    }
    
    @Override
    public String getDataDirectory() {
        File defaultFile = new File(Environment.getExternalStorageDirectory(), context.getString(R.string.app_name));
        return sharedPrefs.getString(DATA_DIR_KEY, defaultFile.getAbsolutePath());
    }

    @Override
    public void addConfigurationChangeListenerListener(
            ConfigurationChangeListener listener) {
        if (!listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    @Override
    public void removeConfigurationChangeListener(
            ConfigurationChangeListener listener) {
        listeners.remove(listener);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
            String key) {
        
        LOG.trace("Notifying configuration change listeners of change to key {}", key);
        
        long startTime = System.currentTimeMillis();
        
        ConfigurationChangeEventBuilder builder = new ConfigurationChangeEventBuilder();
        builder.setConfiguration(this);
        
        if (ROOT_LOG_LEVEL_KEY.equals(key)) {
            builder.setRootLogLevelChanged(true);
        } else if (LOG_TO_FILE_KEY.equals(key)) {
            builder.setLogToFileChanged(true);
        } else if (ECU_ENABLED_KEY.equals(key)) {
            builder.setEcuEnabledChanged(true);
        } else if (ECU_BT_ADDRESS_KEY.equals(true)) {
            builder.setEcuBtAddressChanged(true);
        } else if (LOCATION_BT_ADDRESS_KEY.equals(key)) {
            builder.setLocationBtAddressChanged(true);
        }
        
        ConfigurationChangeEvent event = builder.build();
        
        for (ConfigurationChangeListener listener : listeners) {
            try {
                listener.onConfigurationChange(event);
            } catch (Exception e) {
                LOG.error("Error during notifcation of configuration change listener.", e);
            }
        }
        
        if (LOG.isDebugEnabled()) {
            LOG.debug("Notified {} configuration change listeners in {}ms.", 
                    listeners.size(), System.currentTimeMillis() - startTime);
        }
    }
}
