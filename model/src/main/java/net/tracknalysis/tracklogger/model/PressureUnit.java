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
package net.tracknalysis.tracklogger.model;

/**
 * Enum of supported pressure units and conversions.
 *
 * @author David Valeri
 */
public enum PressureUnit {
    
    PSI(
            new ScaleFunction() {
                @Override
                public double scale(double value) {
                    return value * .145037737738d;
                }
                
                @Override
                public float scale(float value) {
                    return value * .145037737738f;
                }
            }),
    KPA(
            new ScaleFunction() {
                @Override
                public double scale(double value) {
                    return value;
                }
                
                public float scale(float value) {
                    return value;
                }
            }),
    BAR(
            new ScaleFunction() {
                @Override
                public double scale(double value) {
                    return value * 0.01d;
                }
                
                public float scale(float value) {
                    return value * 0.01f;
                }
            });
    
    private final ScaleFunction fromKPaScaleFunction;
    
    private PressureUnit(ScaleFunction fromKPaScaleFunction) {
        this.fromKPaScaleFunction = fromKPaScaleFunction;
    }
    
    public double fromKPa(double value) {
        return fromKPaScaleFunction.scale(value);
    }
    
    public float fromKPa(float value) {
        return fromKPaScaleFunction.scale(value);
    }
}
