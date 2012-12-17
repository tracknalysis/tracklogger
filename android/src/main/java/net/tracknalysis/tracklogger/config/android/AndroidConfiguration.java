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
import net.tracknalysis.tracklogger.model.PressureUnit;
import net.tracknalysis.tracklogger.model.SpeedUnit;
import net.tracknalysis.tracklogger.model.TemperatureUnit;
import net.tracknalysis.tracklogger.model.validation.ValidationErrorException;
import net.tracknalysis.tracklogger.view.GaugeConfiguration;
import net.tracknalysis.tracklogger.view.GaugeConfiguration.GaugeConfigurationBuilder;

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
    private static final String ECU_ENABLED_KEY = "ecu.enable";
    private static final String ECU_BT_ADDRESS_KEY = "ecu.bt.address";
    private static final String ECU_IO_LOG_ENABLED_KEY = "ecu.io.log.enable";
    private static final String LOCATION_BT_ADDRESS_KEY = "location.bt.address";
    private static final String LOG_LAYOUT_ID_KEY = "log.layout.id";
    private static final String DISPLAY_UNITS_SPEED_KEY = "display.units.speed";
    private static final String DISPLAY_UNITS_TEMPERATURE_KEY = "display.units.temperature";
    private static final String DISPLAY_UNITS_PRESSURE_KEY = "display.units.pressure";
    private static final String DATA_DIR_KEY = "app.data.dir.path";
    private static final String TEST_MODE_KEY = "app.testmode";
    
    private static final String KEY_FRAGMENT_SEPARATOR = ".";
    private static final String GAUGE_TITLE_KEY_FRAGMENT = "title";
    private static final String GAUGE_MIN_VALUE_KEY_FRAGMENT = "minValue";
    private static final String GAUGE_MAX_VALUE_KEY_FRAGMENT = "maxValue";
    private static final String GAUGE_MAJOR_SCALE_MARK_DELTA_KEY_FRAGMENT = "majorScaleMarkDelta";
    private static final String GAUGE_MINOR_SCALE_MARK_SEGMENTS_PER_MAJOR_SCALE_MARK_KEY_FRAGMENT = "minorScaleMarkSegmentsPerMajorScaleMark";
    private static final String GAUGE_SCALE_MARK_LABEL_SCALE_FACTOR_KEY_FRAGMENT = "scaleMarkLabelScaleFactor";
    private static final String GAUGE_SCALE_MARK_LABEL_PRECISION_KEY_FRAGMENT = "scaleMarkLabelPrecision";
    private static final String GAUGE_SHOW_VALUE_KEY_FRAGMENT = "showValue";
    private static final String GAUGE_VALUE_PRECISION_KEY_FRAGMENT = "valuePrecision";
    private static final String GAUGE_MIN_CRITICAL_VALUE_KEY_FRAGMENT = "minCriticalValue";
    private static final String GAUGE_MIN_WARNING_VALUE_KEY_FRAGMENT = "minWarningValue";
    private static final String GAUGE_MAX_WARNING_VALUE_KEY_FRAGMENT = "maxWarningValue";
    private static final String GAUGE_MAX_CRITICAL_VALUE_KEY_FRAGMENT = "maxCriticalValue";
    private static final String GAUGE_USE_ALERT_COLOR_GRADIENT_KEY_FRAGMENT = "useAlertColorGradient";
    
    private static final Level ROOT_LOG_LEVEL_DEFAULT = Level.WARN; 
    private static final boolean LOG_TO_FILE_DEFAULT = true;
    private static final boolean ECU_ENABLED_DEFAULT = false;
    private static final boolean ECU_IO_LOG_ENABLED_DEFAULT = false;
    private static final int LOG_LAYOUT_ID_DEFAULT = R.layout.log_default;
    private static final SpeedUnit DISPLAY_UNITS_SPEED_DEFAULT = SpeedUnit.MPH;
    private static final TemperatureUnit DISPLAY_UNITS_TEMPERATURE_DEFAULT = TemperatureUnit.F;
    private static final PressureUnit DISPLAY_UNITS_PRESSURE_DEFAULT = PressureUnit.PSI;
    
    private final SharedPreferences sharedPrefs;
    
    private final List<ConfigurationChangeListener> listeners = 
            new CopyOnWriteArrayList<ConfigurationChangeListener>();
    
    private final Context context;
    
    public AndroidConfiguration(Context context) {
        super();
        sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);
        sharedPrefs.registerOnSharedPreferenceChangeListener(this);
        this.context = context;
        
        initDefaults();
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
                ROOT_LOG_LEVEL_KEY, ROOT_LOG_LEVEL_DEFAULT.toString()));
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
        return sharedPrefs.getBoolean(LOG_TO_FILE_KEY, LOG_TO_FILE_DEFAULT);
    }
    
    @Override
    public void setLogToFile(boolean enabled) {
        Editor editor = sharedPrefs.edit();
        editor.putBoolean(LOG_TO_FILE_KEY, enabled);
        editor.commit();
    }

    @Override
    public boolean isEcuEnabled() {
        return sharedPrefs.getBoolean(ECU_ENABLED_KEY, ECU_ENABLED_DEFAULT);
    }
    
    @Override
    public void setEcuEnabled(boolean enabled) {
        Editor editor = sharedPrefs.edit();
        editor.putBoolean(ECU_ENABLED_KEY, enabled);
        editor.commit();
    }
    
    @Override
    public String getEcuBtAddress() {
        return sharedPrefs.getString(ECU_BT_ADDRESS_KEY, "");
    }
    
    @Override
    public boolean isEcuIoLogEnabled() {
    	return sharedPrefs.getBoolean(ECU_IO_LOG_ENABLED_KEY, false);
    }
    
    @Override
    public void setEcuIoLogEnabled(boolean enabled) {
    	Editor editor = sharedPrefs.edit();
        editor.putBoolean(ECU_IO_LOG_ENABLED_KEY, enabled);
        editor.commit();
    }
    
    @Override
    public String getLocationBtAddress() {
        return sharedPrefs.getString(LOCATION_BT_ADDRESS_KEY, "");
    }
    
    @Override
    public int getLogLayoutId() {
        String name = sharedPrefs.getString(LOG_LAYOUT_ID_KEY, context.getResources()
                .getResourceName(LOG_LAYOUT_ID_DEFAULT));
        
        return context.getResources().getIdentifier(name, null, null); 
    }
    
    @Override
    public void setLogLayoutId(int id) {
        Editor editor = sharedPrefs.edit();
        editor.putString(LOG_LAYOUT_ID_KEY, context.getResources().getResourceName(id));
        editor.commit();   
    }
    
    @Override
    public SpeedUnit getDisplaySpeedUnit() {
        String value = sharedPrefs.getString(DISPLAY_UNITS_SPEED_KEY, DISPLAY_UNITS_SPEED_DEFAULT.name());
        
        SpeedUnit unit;
        try {
            unit = SpeedUnit.valueOf(value);
        } catch (IllegalArgumentException e) {
            LOG.error("Unknown unit name [" + value + "].  Defaulting to MPH.");
            unit = SpeedUnit.MPH;
            setDisplaySpeedUnit(unit);
        }
        
        return unit;
    }

    @Override
    public void setDisplaySpeedUnit(SpeedUnit unit) {
        Editor editor = sharedPrefs.edit();
        editor.putString(DISPLAY_UNITS_SPEED_KEY, unit.name());
        editor.commit();
    }

    @Override
    public TemperatureUnit getDisplayTemperatureUnit() {
        String value = sharedPrefs.getString(DISPLAY_UNITS_TEMPERATURE_KEY, DISPLAY_UNITS_TEMPERATURE_DEFAULT.name());
        
        TemperatureUnit unit;
        try {
            unit = TemperatureUnit.valueOf(value);
        } catch (IllegalArgumentException e) {
            LOG.error("Unknown unit name [" + value + "].  Defaulting to F.");
            unit = TemperatureUnit.F;
            setDisplayTemperatureUnit(unit);
        }
        
        return unit;
    }

    @Override
    public void setDisplayTemperatureUnit(TemperatureUnit unit) {
        Editor editor = sharedPrefs.edit();
        editor.putString(DISPLAY_UNITS_TEMPERATURE_KEY, unit.name());
        editor.commit();
    }

    @Override
    public PressureUnit getDisplayPressureUnit() {
        String value = sharedPrefs.getString(DISPLAY_UNITS_PRESSURE_KEY, DISPLAY_UNITS_PRESSURE_DEFAULT.name());
        
        PressureUnit unit;
        try {
            unit = PressureUnit.valueOf(value);
        } catch (IllegalArgumentException e) {
            LOG.error("Unknown unit name [" + value + "].  Defaulting to PSI.");
            unit = PressureUnit.PSI;
            setDisplayPressureUnit(unit);
        }
        
        return unit;
    }

    @Override
    public void setDisplayPressureUnit(PressureUnit unit) {
        Editor editor = sharedPrefs.edit();
        editor.putString(DISPLAY_UNITS_PRESSURE_KEY, unit.name());
        editor.commit();
    }
    
    @Override
    public GaugeConfiguration getGaugeConfiguration(DisplayGauge displayGauge) {
        GaugeConfigurationBuilder builder = new GaugeConfigurationBuilder();
        
        builder.setTitle(sharedPrefs.getString(displayGauge.getRootKey() + KEY_FRAGMENT_SEPARATOR
                + GAUGE_TITLE_KEY_FRAGMENT, null));
        
        builder.setMinValue(sharedPrefs.getFloat(displayGauge.getRootKey() + KEY_FRAGMENT_SEPARATOR
                + GAUGE_MIN_VALUE_KEY_FRAGMENT, 0f));
        
        builder.setMaxValue(sharedPrefs.getFloat(displayGauge.getRootKey() + KEY_FRAGMENT_SEPARATOR
                + GAUGE_MAX_VALUE_KEY_FRAGMENT, 0f));
        
        builder.setMajorScaleMarkDelta(sharedPrefs.getFloat(displayGauge.getRootKey() + KEY_FRAGMENT_SEPARATOR
                + GAUGE_MAJOR_SCALE_MARK_DELTA_KEY_FRAGMENT, 0f));
        
        builder.setMinorScaleMarkSegmentsPerMajorScaleMark(sharedPrefs.getInt(displayGauge.getRootKey()
                + KEY_FRAGMENT_SEPARATOR
                + GAUGE_MINOR_SCALE_MARK_SEGMENTS_PER_MAJOR_SCALE_MARK_KEY_FRAGMENT, 0));
        
        if (sharedPrefs.contains(displayGauge.getRootKey()
                + KEY_FRAGMENT_SEPARATOR
                + GAUGE_SCALE_MARK_LABEL_SCALE_FACTOR_KEY_FRAGMENT)) {
            builder.setScaleMarkLabelScaleFactor(sharedPrefs.getFloat(
                    displayGauge.getRootKey() + KEY_FRAGMENT_SEPARATOR
                            + GAUGE_SCALE_MARK_LABEL_SCALE_FACTOR_KEY_FRAGMENT,
                    0f));
        }
        
        builder.setScaleMarkLabelPrecision(sharedPrefs.getInt(
                displayGauge.getRootKey() + KEY_FRAGMENT_SEPARATOR
                        + GAUGE_SCALE_MARK_LABEL_PRECISION_KEY_FRAGMENT, 0));
        
        builder.setShowValue(sharedPrefs.getBoolean(displayGauge.getRootKey() + KEY_FRAGMENT_SEPARATOR
                + GAUGE_SHOW_VALUE_KEY_FRAGMENT, false));
        
        builder.setValuePrecision(sharedPrefs.getInt(displayGauge.getRootKey() + KEY_FRAGMENT_SEPARATOR
                + GAUGE_VALUE_PRECISION_KEY_FRAGMENT, 0));
        
        if (sharedPrefs.contains(displayGauge.getRootKey() + KEY_FRAGMENT_SEPARATOR
                + GAUGE_MIN_CRITICAL_VALUE_KEY_FRAGMENT)) {
            builder.setMinCriticalValue(sharedPrefs.getFloat(displayGauge.getRootKey() + KEY_FRAGMENT_SEPARATOR
                    + GAUGE_MIN_CRITICAL_VALUE_KEY_FRAGMENT, 0f));
        }
        
        if (sharedPrefs.contains(displayGauge.getRootKey() + KEY_FRAGMENT_SEPARATOR
                + GAUGE_MIN_WARNING_VALUE_KEY_FRAGMENT)) {
            builder.setMinWarningValue(sharedPrefs.getFloat(displayGauge.getRootKey() + KEY_FRAGMENT_SEPARATOR
                    + GAUGE_MIN_WARNING_VALUE_KEY_FRAGMENT, 0f));
        }
        
        if (sharedPrefs.contains(displayGauge.getRootKey() + KEY_FRAGMENT_SEPARATOR
                + GAUGE_MAX_WARNING_VALUE_KEY_FRAGMENT)) {
            
            builder.setMaxWarningValue(sharedPrefs.getFloat(displayGauge.getRootKey() + KEY_FRAGMENT_SEPARATOR
                    + GAUGE_MAX_WARNING_VALUE_KEY_FRAGMENT, 0f));
        }
        
        if (sharedPrefs.contains(displayGauge.getRootKey() + KEY_FRAGMENT_SEPARATOR
                + GAUGE_MAX_CRITICAL_VALUE_KEY_FRAGMENT)) {
            builder.setMaxCriticalValue(sharedPrefs.getFloat(displayGauge.getRootKey() + KEY_FRAGMENT_SEPARATOR
                    + GAUGE_MAX_CRITICAL_VALUE_KEY_FRAGMENT, 0f));
        }
        
        builder.setUseAlertColorGradient(sharedPrefs.getBoolean(displayGauge.getRootKey() + KEY_FRAGMENT_SEPARATOR
                + GAUGE_USE_ALERT_COLOR_GRADIENT_KEY_FRAGMENT, false));
        
        try {
            return builder.build();
        } catch (ValidationErrorException e) {
            throw new RuntimeException(
                    "Error initializing gauge configuration.  The stored data violates validation rules.",
                    e);
        }
    }
    
    @Override
    public void setGaugeConfiguration(DisplayGauge displayGauge, GaugeConfiguration gaugeConfiguration) {
        
        Editor editor = sharedPrefs.edit();
        
        editor.putString(displayGauge.getRootKey() + KEY_FRAGMENT_SEPARATOR
                + GAUGE_TITLE_KEY_FRAGMENT, gaugeConfiguration.getTitle());
        
        editor.putFloat(displayGauge.getRootKey() + KEY_FRAGMENT_SEPARATOR
                + GAUGE_MIN_VALUE_KEY_FRAGMENT, gaugeConfiguration.getMinValue());
        
        editor.putFloat(displayGauge.getRootKey() + KEY_FRAGMENT_SEPARATOR
                + GAUGE_MAX_VALUE_KEY_FRAGMENT, gaugeConfiguration.getMaxValue());
        
        editor.putFloat(displayGauge.getRootKey() + KEY_FRAGMENT_SEPARATOR
                + GAUGE_MAJOR_SCALE_MARK_DELTA_KEY_FRAGMENT, gaugeConfiguration.getMajorScaleMarkDelta());
        
        editor.putInt(
                displayGauge.getRootKey()
                        + KEY_FRAGMENT_SEPARATOR
                        + GAUGE_MINOR_SCALE_MARK_SEGMENTS_PER_MAJOR_SCALE_MARK_KEY_FRAGMENT,
                gaugeConfiguration.getMinorScaleMarkSegmentsPerMajorScaleMark());
        
        if (gaugeConfiguration.getScaleMarkLabelScaleFactor() != null) {
            editor.putFloat(
                    displayGauge.getRootKey() + KEY_FRAGMENT_SEPARATOR
                            + GAUGE_SCALE_MARK_LABEL_SCALE_FACTOR_KEY_FRAGMENT,
                    gaugeConfiguration.getScaleMarkLabelScaleFactor());
        } else {
            editor.remove(displayGauge.getRootKey() + KEY_FRAGMENT_SEPARATOR
                    + GAUGE_SCALE_MARK_LABEL_SCALE_FACTOR_KEY_FRAGMENT);
        }
        
        editor.putInt(displayGauge.getRootKey() + KEY_FRAGMENT_SEPARATOR
                + GAUGE_SCALE_MARK_LABEL_PRECISION_KEY_FRAGMENT,
                gaugeConfiguration.getScaleMarkLabelPrecision());
        
        editor.putBoolean(
                displayGauge.getRootKey() + KEY_FRAGMENT_SEPARATOR
                        + GAUGE_SHOW_VALUE_KEY_FRAGMENT,
                gaugeConfiguration.isShowValue());
        
        editor.putInt(displayGauge.getRootKey() + KEY_FRAGMENT_SEPARATOR
                        + GAUGE_VALUE_PRECISION_KEY_FRAGMENT, gaugeConfiguration.getValuePrecision());
        
        
        if (gaugeConfiguration.getMinCriticalValue() != null) {
            editor.putFloat(displayGauge.getRootKey() + KEY_FRAGMENT_SEPARATOR
                    + GAUGE_MIN_CRITICAL_VALUE_KEY_FRAGMENT, gaugeConfiguration.getMinCriticalValue());
        } else {
            editor.remove(displayGauge.getRootKey() + KEY_FRAGMENT_SEPARATOR
                    + GAUGE_MIN_CRITICAL_VALUE_KEY_FRAGMENT);
        }
        
        if (gaugeConfiguration.getMinWarningValue() != null) {
            editor.putFloat(displayGauge.getRootKey() + KEY_FRAGMENT_SEPARATOR
                    + GAUGE_MIN_WARNING_VALUE_KEY_FRAGMENT, gaugeConfiguration.getMinWarningValue());
        } else {
            editor.remove(displayGauge.getRootKey() + KEY_FRAGMENT_SEPARATOR
                    + GAUGE_MIN_WARNING_VALUE_KEY_FRAGMENT);
        }

        if (gaugeConfiguration.getMaxWarningValue() != null) {
            editor.putFloat(displayGauge.getRootKey() + KEY_FRAGMENT_SEPARATOR
                    + GAUGE_MAX_WARNING_VALUE_KEY_FRAGMENT, gaugeConfiguration.getMaxWarningValue());
        } else {
            editor.remove(displayGauge.getRootKey() + KEY_FRAGMENT_SEPARATOR
                    + GAUGE_MAX_WARNING_VALUE_KEY_FRAGMENT);
        }
        
        if (gaugeConfiguration.getMaxCriticalValue() != null) {
            editor.putFloat(displayGauge.getRootKey() + KEY_FRAGMENT_SEPARATOR
                    + GAUGE_MAX_CRITICAL_VALUE_KEY_FRAGMENT, gaugeConfiguration.getMaxCriticalValue());
        } else {
            editor.remove(displayGauge.getRootKey() + KEY_FRAGMENT_SEPARATOR
                    + GAUGE_MAX_CRITICAL_VALUE_KEY_FRAGMENT);
        }
        
        editor.putBoolean(
                displayGauge.getRootKey() + KEY_FRAGMENT_SEPARATOR
                        + GAUGE_USE_ALERT_COLOR_GRADIENT_KEY_FRAGMENT,
                gaugeConfiguration.isUseAlertColorGradient());

        editor.commit();
    }

    @Override
    public String getDataDirectory() {
        File defaultFile = new File(Environment.getExternalStorageDirectory(), context.getString(R.string.app_name));
        return sharedPrefs.getString(DATA_DIR_KEY, defaultFile.getAbsolutePath());
    }
    
    @Override
    public boolean isTestMode() {
        return sharedPrefs.getBoolean(TEST_MODE_KEY, false);
    }
    
    @Override
    public void setTestMode(boolean testMode) {
        Editor editor = sharedPrefs.edit();
        editor.putBoolean(TEST_MODE_KEY, testMode);
        editor.commit();   
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
    
    private void initDefaults() {
        if (!sharedPrefs.contains(ROOT_LOG_LEVEL_KEY)) {
            setRootLogLevel(ROOT_LOG_LEVEL_DEFAULT);
        }
        
        if (!sharedPrefs.contains(LOG_TO_FILE_KEY)) {
            setLogToFile(LOG_TO_FILE_DEFAULT);
        }
        
        if (!sharedPrefs.contains(ECU_ENABLED_KEY)) {
            setEcuEnabled(ECU_ENABLED_DEFAULT);
        }
        
        if (!sharedPrefs.contains(ECU_IO_LOG_ENABLED_KEY)) {
            setEcuIoLogEnabled(ECU_IO_LOG_ENABLED_DEFAULT);
        }
        
        if (!sharedPrefs.contains(LOG_LAYOUT_ID_KEY)) {
            setLogLayoutId(LOG_LAYOUT_ID_DEFAULT);
        }
        
        if (!sharedPrefs.contains(DISPLAY_UNITS_SPEED_KEY)) {
            setDisplaySpeedUnit(DISPLAY_UNITS_SPEED_DEFAULT);
        }
        
        if (!sharedPrefs.contains(DISPLAY_UNITS_TEMPERATURE_KEY)) {
            setDisplayTemperatureUnit(DISPLAY_UNITS_TEMPERATURE_DEFAULT);
        }
        
        if (!sharedPrefs.contains(DISPLAY_UNITS_PRESSURE_KEY)) {
            setDisplayPressureUnit(DISPLAY_UNITS_PRESSURE_DEFAULT);
        }
        
        try {
            if (!gaugeConfigurationExists(DisplayGauge.SPEED)) {
                GaugeConfigurationBuilder builder = new GaugeConfigurationBuilder();
                builder
                        .setTitle(context.getString(R.string.general_speed_short))
                        .setMinValue(0f)
                        .setMaxValue(160f)
                        .setMajorScaleMarkDelta(20f)
                        .setMinorScaleMarkSegmentsPerMajorScaleMark(4)
                        .setValuePrecision(1);
                
                setGaugeConfiguration(DisplayGauge.SPEED, builder.build());
            }
            
            if (!gaugeConfigurationExists(DisplayGauge.RPM)) {
                GaugeConfigurationBuilder builder = new GaugeConfigurationBuilder();
                builder
                        .setTitle(context.getString(R.string.general_rpm_short))
                        .setMinValue(0f)
                        .setMaxValue(8000f)
                        .setMajorScaleMarkDelta(1000f)
                        .setMinorScaleMarkSegmentsPerMajorScaleMark(5)
                        .setScaleMarkLabelScaleFactor(1000f)
                        .setMaxCriticalValue(7200f);
                
                setGaugeConfiguration(DisplayGauge.RPM, builder.build());
            }
            
            if (!gaugeConfigurationExists(DisplayGauge.MAP)) {
                GaugeConfigurationBuilder builder = new GaugeConfigurationBuilder();
                builder
                        .setTitle(context.getString(R.string.general_map_short))
                        .setMinValue(0f)
                        .setMaxValue(200f)
                        .setMajorScaleMarkDelta(20f)
                        .setMinorScaleMarkSegmentsPerMajorScaleMark(4);
                
                setGaugeConfiguration(DisplayGauge.MAP, builder.build());
            }
            
            if (!gaugeConfigurationExists(DisplayGauge.MGP)) {
                GaugeConfigurationBuilder builder = new GaugeConfigurationBuilder();
                builder
                        .setTitle(context.getString(R.string.general_mgp_short))
                        .setMinValue(-25f)
                        .setMaxValue(20f)
                        .setMajorScaleMarkDelta(5f)
                        .setMinorScaleMarkSegmentsPerMajorScaleMark(5);
                setGaugeConfiguration(DisplayGauge.MGP, builder.build());
            }
            
            if (!gaugeConfigurationExists(DisplayGauge.TP)) {
                GaugeConfigurationBuilder builder = new GaugeConfigurationBuilder();
                builder
                        .setMinValue(0f)
                        .setMaxValue(100f)
                        .setMajorScaleMarkDelta(10f)
                        .setMinorScaleMarkSegmentsPerMajorScaleMark(5)
                        .setValuePrecision(1)
                        .setTitle(context.getString(R.string.general_tp_short));
                
                setGaugeConfiguration(DisplayGauge.TP, builder.build());
            }
            
            if (!gaugeConfigurationExists(DisplayGauge.AFR)) {
                GaugeConfigurationBuilder builder = new GaugeConfigurationBuilder();
                builder
                        .setTitle(context.getString(R.string.general_afr_short))
                        .setMinValue(10f)
                        .setMaxValue(20f)
                        .setMajorScaleMarkDelta(1f)
                        .setValuePrecision(1)
                        .setMinorScaleMarkSegmentsPerMajorScaleMark(1);
                
                setGaugeConfiguration(DisplayGauge.AFR, builder.build());
            }
            
            if (!gaugeConfigurationExists(DisplayGauge.MAT)) {
                GaugeConfigurationBuilder builder = new GaugeConfigurationBuilder();
                builder
                        .setTitle(context.getString(R.string.general_mat_short))
                        .setMinValue(40f)
                        .setMaxValue(250f)
                        .setMajorScaleMarkDelta(20f)
                        .setMinorScaleMarkSegmentsPerMajorScaleMark(4);
                
                setGaugeConfiguration(DisplayGauge.MAT, builder.build());
            }
            
            if (!gaugeConfigurationExists(DisplayGauge.CLT)) {
                GaugeConfigurationBuilder builder = new GaugeConfigurationBuilder();
                builder
                        .setTitle(context.getString(R.string.general_clt_short))
                        .setMinValue(100f)
                        .setMaxValue(240f)
                        .setMajorScaleMarkDelta(20f)
                        .setMinorScaleMarkSegmentsPerMajorScaleMark(4)
                        .setMinCriticalValue(160f)
                        .setMinWarningValue(170f)
                        .setMaxWarningValue(210f)
                        .setMaxCriticalValue(220f)
                        .setUseAlertColorGradient(true);
                
                setGaugeConfiguration(DisplayGauge.CLT, builder.build());
            }
            
            if (!gaugeConfigurationExists(DisplayGauge.IGN_ADV)) {
                GaugeConfigurationBuilder builder = new GaugeConfigurationBuilder();
                builder
                        .setTitle(context.getString(R.string.general_ign_adv_short))
                        .setMinValue(0f)
                        .setMaxValue(40f)
                        .setMajorScaleMarkDelta(10f)
                        .setMinorScaleMarkSegmentsPerMajorScaleMark(5);
                
                setGaugeConfiguration(DisplayGauge.IGN_ADV, builder.build());
            }
            
            if (!gaugeConfigurationExists(DisplayGauge.BAT_V)) {
                GaugeConfigurationBuilder builder = new GaugeConfigurationBuilder();
                builder
                        .setTitle(context.getString(R.string.general_batv_short))
                        .setMinValue(10f)
                        .setMaxValue(16f)
                        .setMajorScaleMarkDelta(1f)
                        .setMinorScaleMarkSegmentsPerMajorScaleMark(2)
                        .setValuePrecision(1)
                        .setMinCriticalValue(11f)
                        .setMinWarningValue(11.5f)
                        .setMaxWarningValue(14.5f)
                        .setMaxCriticalValue(15f)
                        .setUseAlertColorGradient(true);
                
                setGaugeConfiguration(DisplayGauge.BAT_V, builder.build());
            }
        } catch (ValidationErrorException e) {
            String message = "Error initializing gauge configuration.  The defaults violate validation rules.";
            throw new RuntimeException(message, e);
        }
        
        // Don't default test mode or data dir properties.
    }
    
    private boolean gaugeConfigurationExists(DisplayGauge displayGauge) {
        return sharedPrefs.contains(displayGauge.getRootKey()
                + KEY_FRAGMENT_SEPARATOR + GAUGE_MIN_VALUE_KEY_FRAGMENT);
    }
}
