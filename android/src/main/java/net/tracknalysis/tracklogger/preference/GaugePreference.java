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
package net.tracknalysis.tracklogger.preference;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.tracknalysis.tracklogger.R;
import net.tracknalysis.tracklogger.config.Configuration;
import net.tracknalysis.tracklogger.config.ConfigurationFactory;
import net.tracknalysis.tracklogger.config.Configuration.DisplayGauge;
import net.tracknalysis.tracklogger.model.validation.BasicValidationErrorType;
import net.tracknalysis.tracklogger.model.validation.ValidationError;
import net.tracknalysis.tracklogger.model.validation.ValidationErrorException;
import net.tracknalysis.tracklogger.model.validation.ValidationResults;
import net.tracknalysis.tracklogger.view.GaugeConfiguration;
import net.tracknalysis.tracklogger.view.GaugeConfiguration.GaugeConfigurationBuilder;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.preference.DialogPreference;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.TextView;

/**
 * Preference screen for setting the properties of a {@link GaugePreference}.
 *
 * @author David Valeri
 */
public class GaugePreference extends DialogPreference implements TextWatcher, OnCheckedChangeListener {
    
    private static final String TITLE_FIELD_NAME = "title";
    private static final String MIN_VALUE_FIELD_NAME = "minValue";
    private static final String MAX_VALUE_FIELD_NAME = "maxValue";
    private static final String MAJOR_SCALE_MARK_DELTA_FIELD_NAME = "majorScaleMarkDelta";
    private static final String MINOR_SCALE_MARK_SEGMENTS_PER_MAJOR_SCALE_MARK_FIELD_NAME = 
            "minorScaleMarkSegmentsPerMajorScaleMark";
    private static final String SCALE_MARK_LABEL_SCALE_FACTOR_FIELD_NAME = "scaleMarkLabelScaleFactor";
    private static final String SCALE_MARK_LABEL_PRECISION_FIELD_NAME = "scaleMarkLabelPrecision";
    private static final String SHOW_VALUE_FIELD_NAME = "showValue";
    private static final String VALUE_PRECISION_FIELD_NAME = "valuePrecision";
    private static final String MIN_CRITICAL_VALUE_FIELD_NAME = "minCriticalValue";
    private static final String MIN_WARNING_VALUE_FIELD_NAME = "minWarningValue";
    private static final String MAX_WARNING_VALUE_FIELD_NAME = "maxWarningValue";
    private static final String MAX_CRITICAL_VALUE_FIELD_NAME = "maxCriticalValue";
    private static final String USE_ALERT_COLOT_GRADIENT_FIELD_NAME = "useAlertColorGradient";
    
    private static final Map<String, Integer> VALIDATION_ERROR_MAP;
    
    static {
        HashMap<String, Integer> tempValidationErrorMap = new HashMap<String, Integer>();
        
        tempValidationErrorMap.put(
                ValidationError.getValidationErrorKey(
                        MIN_VALUE_FIELD_NAME,
                        BasicValidationErrorType.RELATIVE_RANGE_CONSTRAINT),
                R.string.preference_gauge_min_value_validation_error_relative_range);
        tempValidationErrorMap.put(
                ValidationError.getValidationErrorKey(
                        MIN_VALUE_FIELD_NAME,
                        BasicValidationErrorType.NOT_NULL),
                R.string.preference_gauge_min_value_validation_error_not_null);
        
        tempValidationErrorMap.put(
                ValidationError.getValidationErrorKey(
                        MAX_VALUE_FIELD_NAME,
                        BasicValidationErrorType.RELATIVE_RANGE_CONSTRAINT),
                R.string.preference_gauge_max_value_validation_error_relative_range);
        tempValidationErrorMap.put(
                ValidationError.getValidationErrorKey(
                        MAX_VALUE_FIELD_NAME,
                        BasicValidationErrorType.NOT_NULL),
                R.string.preference_gauge_max_value_validation_error_not_null);
        
        tempValidationErrorMap.put(
                ValidationError.getValidationErrorKey(
                        MIN_CRITICAL_VALUE_FIELD_NAME,
                        BasicValidationErrorType.RELATIVE_RANGE_CONSTRAINT),
                R.string.preference_gauge_alert_value_error_relative_range);
        
        tempValidationErrorMap.put(
                ValidationError.getValidationErrorKey(
                        MIN_WARNING_VALUE_FIELD_NAME,
                        BasicValidationErrorType.RELATIVE_RANGE_CONSTRAINT),
                R.string.preference_gauge_alert_value_error_relative_range);
        
        tempValidationErrorMap.put(
                ValidationError.getValidationErrorKey(
                        MAX_WARNING_VALUE_FIELD_NAME,
                        BasicValidationErrorType.RELATIVE_RANGE_CONSTRAINT),
                R.string.preference_gauge_alert_value_error_relative_range);
        
        tempValidationErrorMap.put(
                ValidationError.getValidationErrorKey(
                        MAX_CRITICAL_VALUE_FIELD_NAME,
                        BasicValidationErrorType.RELATIVE_RANGE_CONSTRAINT),
                R.string.preference_gauge_alert_value_error_relative_range);
        
        tempValidationErrorMap.put(
                ValidationError.getValidationErrorKey(
                        MAJOR_SCALE_MARK_DELTA_FIELD_NAME,
                        BasicValidationErrorType.RANGE_CONSTRAINT),
                R.string.preference_gauge_major_scale_mark_delta_validation_error_range);
        tempValidationErrorMap.put(
                ValidationError.getValidationErrorKey(
                        MAJOR_SCALE_MARK_DELTA_FIELD_NAME,
                        BasicValidationErrorType.NOT_NULL),
                R.string.preference_gauge_major_scale_mark_delta_validation_error_not_null);
        
        tempValidationErrorMap.put(
                ValidationError.getValidationErrorKey(
                        MINOR_SCALE_MARK_SEGMENTS_PER_MAJOR_SCALE_MARK_FIELD_NAME,
                        BasicValidationErrorType.RANGE_CONSTRAINT),
                R.string.preference_gauge_minor_scale_mark_segments_per_major_scale_mark_validation_error_range);
        tempValidationErrorMap.put(
                ValidationError.getValidationErrorKey(
                        MINOR_SCALE_MARK_SEGMENTS_PER_MAJOR_SCALE_MARK_FIELD_NAME,
                        BasicValidationErrorType.NOT_NULL),
                R.string.preference_gauge_minor_scale_mark_segments_per_major_scale_mark_validation_error_not_null);
        
        
        tempValidationErrorMap.put(
                ValidationError.getValidationErrorKey(
                        SCALE_MARK_LABEL_SCALE_FACTOR_FIELD_NAME,
                        BasicValidationErrorType.RANGE_CONSTRAINT),
                R.string.preference_gauge_scale_mark_label_scale_factor_error_range);


        tempValidationErrorMap.put(
                ValidationError.getValidationErrorKey(
                        SCALE_MARK_LABEL_PRECISION_FIELD_NAME,
                        BasicValidationErrorType.NOT_NULL),
                R.string.preference_gauge_scale_mark_label_precision_error_not_null);
        tempValidationErrorMap.put(
                ValidationError.getValidationErrorKey(
                        SCALE_MARK_LABEL_PRECISION_FIELD_NAME,
                        BasicValidationErrorType.RANGE_CONSTRAINT),
                R.string.preference_gauge_scale_mark_label_precision_error_range);
        
        
        tempValidationErrorMap.put(
                ValidationError.getValidationErrorKey(
                        VALUE_PRECISION_FIELD_NAME,
                        BasicValidationErrorType.NOT_NULL),
                R.string.preference_gauge_value_precision_title_error_not_null);
        tempValidationErrorMap.put(
                ValidationError.getValidationErrorKey(
                        VALUE_PRECISION_FIELD_NAME,
                        BasicValidationErrorType.RANGE_CONSTRAINT),
                R.string.preference_gauge_value_precision_title_error_range);
        
        VALIDATION_ERROR_MAP = Collections.unmodifiableMap(tempValidationErrorMap);
    }
    
    private final Configuration config;
    private final DisplayGauge displayGauge;
    
    private volatile boolean disableValidation = true;
    
    private Map<String, TextView> fieldToViewMap = new HashMap<String, TextView>();
    
    private TextView titleTextView;
    private TextView minValueTextView;
    private TextView maxValueTextView;
    private TextView majorScaleMarkDeltaTextView;
    private TextView minorScaleMarkSegmentsPerMajorScaleMarkTextView;
    private TextView scaleMarkLabelScaleFactorTextView;
    private TextView scaleMarkLabelPrecisionTextView;
    private CheckBox showValueCheckBox;
    private TextView valuePrecisionTextView;
    private TextView minCriticalValueTextView;
    private TextView minWarningValueTextView;
    private TextView maxWarningValueTextView;
    private TextView maxCriticalValueTextView;
    private CheckBox useAlertColorGradientCheckBox;

    public GaugePreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        setPersistent(false);
        setDialogLayoutResource(R.layout.preference_gauge);
        
        config = ConfigurationFactory.getInstance().getConfiguration();
        
        displayGauge = DisplayGauge.fromRootKey(getKey());
    }
    
    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        // Do nothing
    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
        // Do nothing
    }

    @Override
    public void afterTextChanged(Editable s) {
        onUserEdit();
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        onUserEdit();
    }
    
    @Override
    protected void onBindDialogView(View view) {
        super.onBindDialogView(view);
        
        disableValidation = true;
        
        titleTextView = (TextView) view.findViewById(R.id.preference_gauge_title_value);
        titleTextView.addTextChangedListener(this);
        fieldToViewMap.put(TITLE_FIELD_NAME, titleTextView);


        minValueTextView = (TextView) view.findViewById(R.id.preference_gauge_min_value);
        minValueTextView.addTextChangedListener(this);
        fieldToViewMap.put(MIN_VALUE_FIELD_NAME, minValueTextView);
        
        maxValueTextView = (TextView) view.findViewById(R.id.preference_gauge_max_value);
        maxValueTextView.addTextChangedListener(this);
        fieldToViewMap.put(MAX_VALUE_FIELD_NAME, maxValueTextView);

        
        majorScaleMarkDeltaTextView = (TextView) view.findViewById(R.id.preference_gauge_major_scale_mark_delta_value);
        majorScaleMarkDeltaTextView.addTextChangedListener(this);
        fieldToViewMap.put(MAJOR_SCALE_MARK_DELTA_FIELD_NAME, majorScaleMarkDeltaTextView);
        
        minorScaleMarkSegmentsPerMajorScaleMarkTextView = (TextView) view
                .findViewById(R.id.preference_gauge_minor_scale_mark_segments_per_major_scale_mark_value);
        minorScaleMarkSegmentsPerMajorScaleMarkTextView.addTextChangedListener(this);
        fieldToViewMap.put(
                MINOR_SCALE_MARK_SEGMENTS_PER_MAJOR_SCALE_MARK_FIELD_NAME,
                minorScaleMarkSegmentsPerMajorScaleMarkTextView);
        
        scaleMarkLabelScaleFactorTextView = (TextView) view
                .findViewById(R.id.preference_gauge_scale_mark_label_scale_factor_value);
        scaleMarkLabelScaleFactorTextView.addTextChangedListener(this);
        fieldToViewMap.put(
                SCALE_MARK_LABEL_SCALE_FACTOR_FIELD_NAME,
                scaleMarkLabelScaleFactorTextView);
        
        scaleMarkLabelPrecisionTextView = (TextView) view
                .findViewById(R.id.preference_gauge_scale_mark_label_precision_value);
        scaleMarkLabelPrecisionTextView.addTextChangedListener(this);
        fieldToViewMap.put(
                SCALE_MARK_LABEL_PRECISION_FIELD_NAME,
                scaleMarkLabelPrecisionTextView);
        
        
        showValueCheckBox = (CheckBox) view.findViewById(R.id.preference_gauge_show_value_value);
        showValueCheckBox.setOnCheckedChangeListener(this);
        fieldToViewMap.put(SHOW_VALUE_FIELD_NAME, showValueCheckBox);
        
        valuePrecisionTextView = (TextView) view
                .findViewById(R.id.preference_gauge_value_precision_value);
        valuePrecisionTextView.addTextChangedListener(this);
        fieldToViewMap.put(
                VALUE_PRECISION_FIELD_NAME,
                valuePrecisionTextView);
        
        
        minCriticalValueTextView = (TextView) view.findViewById(R.id.preference_gauge_min_critical_value);
        minCriticalValueTextView.addTextChangedListener(this);
        fieldToViewMap.put(MIN_CRITICAL_VALUE_FIELD_NAME, minCriticalValueTextView);
        
        minWarningValueTextView = (TextView) view.findViewById(R.id.preference_gauge_min_warning_value);
        minWarningValueTextView.addTextChangedListener(this);
        fieldToViewMap.put(MIN_WARNING_VALUE_FIELD_NAME, minWarningValueTextView);
        
        maxWarningValueTextView = (TextView) view.findViewById(R.id.preference_gauge_max_warning_value);
        maxWarningValueTextView.addTextChangedListener(this);
        fieldToViewMap.put(MAX_WARNING_VALUE_FIELD_NAME, maxWarningValueTextView);
        
        maxCriticalValueTextView = (TextView) view.findViewById(R.id.preference_gauge_max_critical_value);
        maxCriticalValueTextView.addTextChangedListener(this);
        fieldToViewMap.put(MAX_CRITICAL_VALUE_FIELD_NAME, maxCriticalValueTextView);
        
        
        useAlertColorGradientCheckBox = (CheckBox) view.findViewById(R.id.preference_gauge_use_alert_color_gradient_value);
        useAlertColorGradientCheckBox.setOnCheckedChangeListener(this);
        fieldToViewMap.put(USE_ALERT_COLOT_GRADIENT_FIELD_NAME, useAlertColorGradientCheckBox);
        
        bind(config.getGaugeConfiguration(displayGauge));
        
        disableValidation = false;
    }
    
    @Override
    protected void onDialogClosed(boolean positiveResult) {
        super.onDialogClosed(positiveResult);
        
        if (positiveResult) {
            try {
                config.setGaugeConfiguration(displayGauge, bind());
            } catch (ValidationErrorException e) {
                // Shouldn't get here, so just give up if we do
                throw new RuntimeException(e);
            }            
        }
    }
    
    private void onUserEdit() {
        if (!disableValidation) {
            Button positiveButton = ((AlertDialog) getDialog()).getButton(DialogInterface.BUTTON_POSITIVE);
            try {
                for (TextView view : fieldToViewMap.values()) {
                    view.setError(null);
                }
                bind();
                positiveButton.setEnabled(true);
            } catch (ValidationErrorException e) {
                positiveButton.setEnabled(false);
                handleValidationErrors(e.getValidationResults());
            }
        }
    }
    
    private void handleValidationErrors(ValidationResults validationResults) {
        if (validationResults.hasValidationErrors()) {
            
            Map<String, List<ValidationError>> currentErrors = validationResults
                    .getValidationErrorsByFieldNames();
            
            for (Map.Entry<String, List<ValidationError>> entry : currentErrors.entrySet()) {
                TextView view = fieldToViewMap.get(entry.getKey());
                
                String errorMessages = "";
                
                for (ValidationError validationError : entry.getValue()) {
                    Integer errorMessageId = VALIDATION_ERROR_MAP.get(validationError.getValidationErrorKey());
                    if (errorMessages.length() != 0) {
                        errorMessages += " ";
                    }
                    
                    if (errorMessageId == null) {
                        errorMessages += getContext().getText(R.string.preference_gauge_validation_error);
                    } else {
                        errorMessages += getContext().getText(errorMessageId);
                    }
                    
                    view.setError(errorMessages);
                }
            }
        }
    }
    
    private void bind(GaugeConfiguration gaugeConfiguration) {
        titleTextView.setText(gaugeConfiguration.getTitle());
        
        minValueTextView.setText(String.format("%.2f", gaugeConfiguration.getMinValue()));
        maxValueTextView.setText(String.format("%.2f", gaugeConfiguration.getMaxValue()));
        
        majorScaleMarkDeltaTextView.setText(String.format("%.2f", gaugeConfiguration.getMajorScaleMarkDelta()));
        minorScaleMarkSegmentsPerMajorScaleMarkTextView
                .setText(String.format("%d", gaugeConfiguration
                        .getMinorScaleMarkSegmentsPerMajorScaleMark()));
        if (gaugeConfiguration.getScaleMarkLabelScaleFactor() != null) {
            scaleMarkLabelScaleFactorTextView.setText(String.format("%.2f",
                    gaugeConfiguration.getScaleMarkLabelScaleFactor()));
        }
        
        scaleMarkLabelPrecisionTextView.setText(String.format("%d", gaugeConfiguration.getScaleMarkLabelPrecision()));
        
        showValueCheckBox.setChecked(gaugeConfiguration.isShowValue());
        
        valuePrecisionTextView.setText(String.format("%d", gaugeConfiguration.getValuePrecision()));
        
        if (gaugeConfiguration.isMinCriticalEnabled()) {
            minCriticalValueTextView.setText(String.format("%.2f", gaugeConfiguration.getMinCriticalValue()));
        }
        
        if (gaugeConfiguration.isMinWarningEnabled()) {
            minWarningValueTextView.setText(String.format("%.2f", gaugeConfiguration.getMinWarningValue()));
        }
        
        if (gaugeConfiguration.isMaxWarningEnabled()) {
            maxWarningValueTextView.setText(String.format("%.2f", gaugeConfiguration.getMaxWarningValue()));
        }
        
        if (gaugeConfiguration.isMaxCriticalEnabled()) {
            maxCriticalValueTextView.setText(String.format("%.2f", gaugeConfiguration.getMaxCriticalValue()));
        }
        
        useAlertColorGradientCheckBox.setChecked(gaugeConfiguration.isUseAlertColorGradient());
    }
    
    private GaugeConfiguration bind() throws ValidationErrorException {
        GaugeConfigurationBuilder builder = new GaugeConfigurationBuilder();
        
        if (titleTextView.getText().length() != 0) {
            builder.setTitle(titleTextView.getText().toString());
        }
        
        if (minValueTextView.getText().length() != 0) {
            builder.setMinValue(Float.valueOf(minValueTextView.getText().toString()));
        }
        
        if (maxValueTextView.getText().length() != 0) {
            builder.setMaxValue(Float.valueOf(maxValueTextView.getText().toString()));
        }
        
        if (majorScaleMarkDeltaTextView.getText().length() != 0) {
            builder.setMajorScaleMarkDelta(Float.valueOf(majorScaleMarkDeltaTextView.getText().toString()));
        }
        
        if (minorScaleMarkSegmentsPerMajorScaleMarkTextView.getText().length() != 0) {
            builder.setMinorScaleMarkSegmentsPerMajorScaleMark(Integer
                    .valueOf(minorScaleMarkSegmentsPerMajorScaleMarkTextView
                            .getText().toString()));
        }
        
        if (scaleMarkLabelScaleFactorTextView.getText().length() != 0) {
            builder.setScaleMarkLabelScaleFactor(Float
                    .valueOf(scaleMarkLabelScaleFactorTextView.getText().toString()));
        }
        
        if (scaleMarkLabelPrecisionTextView.getText().length() != 0) {
            builder.setScaleMarkLabelPrecision(Integer
                    .valueOf(scaleMarkLabelPrecisionTextView.getText().toString()));
        }
        
        builder.setShowValue(showValueCheckBox.isChecked());
        
        if (valuePrecisionTextView.getText().length() != 0) {
            builder.setValuePrecision(Integer
                    .valueOf(valuePrecisionTextView.getText().toString()));
        }
        
        if (minCriticalValueTextView.getText().length() != 0) {
            builder.setMinCriticalValue(Float.valueOf(minCriticalValueTextView.getText().toString()));
        }
        
        if (minWarningValueTextView.getText().length() != 0) {
            builder.setMinWarningValue(Float.valueOf(minWarningValueTextView.getText().toString()));
        }
        
        if (maxWarningValueTextView.getText().length() != 0) {
            builder.setMaxWarningValue(Float.valueOf(maxWarningValueTextView.getText().toString()));
        }
        
        if (maxCriticalValueTextView.getText().length() != 0) {
            builder.setMaxCriticalValue(Float.valueOf(maxCriticalValueTextView.getText().toString()));
        }
        
        builder.setUseAlertColorGradient(useAlertColorGradientCheckBox.isChecked());
        
        return builder.build();
    }
}
