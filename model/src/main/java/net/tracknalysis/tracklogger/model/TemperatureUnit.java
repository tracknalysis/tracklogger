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
 * Enum of supported temperature units and conversions.
 *
 * @author David Valeri
 */
public enum TemperatureUnit {
    
    F(
            new ScaleFunction() {
                
                @Override
                public double scale(double value) {
                    return (value * 9 / 5) + 32;
                }
                
                @Override
                public float scale(float value) {
                    return (value * 9 / 5) + 32;
                }
            }),
    C(
            new ScaleFunction() {
                @Override
                public double scale(double value) {
                    return value;
                }
                
                @Override
                public float scale(float value) {
                    return value;
                }
            });
    
    private final ScaleFunction fromCelsiusScaleFunction;
    
    private TemperatureUnit(ScaleFunction fromCelsiusScaleFunction) {
        this.fromCelsiusScaleFunction = fromCelsiusScaleFunction;
    }
    
    public double fromCelsius(double value) {
        return fromCelsiusScaleFunction.scale(value);
    }
    
    public float fromCelsius(float value) {
        return fromCelsiusScaleFunction.scale(value);
    }
}
