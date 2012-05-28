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
package net.tracknalysis.tracklogger.dataprovider;

/**
 * @author David Valeri
 */
public class AccelData extends AbstractData {
    
    private float lateral;
    private float vertical;
    private float longitudinal;
    
    protected AccelData() {
    }
    
    public float getLateral() {
        return lateral;
    }
    
    protected void setLateral(float lateral) {
        this.lateral = lateral;
    }
    
    public float getVertical() {
        return vertical;
    }
    
    protected void setVertical(float vertical) {
        this.vertical = vertical;
    }
    
    public float getLongitudinal() {
        return longitudinal;
    }
    
    protected void setLongitudinal(float longitudinal) {
        this.longitudinal = longitudinal;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("AccelData [lateral=");
        builder.append(lateral);
        builder.append(", vertical=");
        builder.append(vertical);
        builder.append(", longitudinal=");
        builder.append(longitudinal);
        builder.append(", getDataRecivedTime()=");
        builder.append(getDataRecivedTime());
        builder.append("]");
        return builder.toString();
    }
    
    public static class AccelDataBuilder extends AbstractDataBuilder<AccelData> {
        
        private float lateral;
        private float vertical;
        private float longitudinal;
        
        public float getLateral() {
            return lateral;
        }
        
        public void setLateral(float lateral) {
            this.lateral = lateral;
        }
        
        public float getVertical() {
            return vertical;
        }
        
        public void setVertical(float vertical) {
            this.vertical = vertical;
        }
        
        public float getLongitudinal() {
            return longitudinal;
        }
        
        public void setLongitudinal(float longitudinal) {
            this.longitudinal = longitudinal;
        }

        @Override
        protected AccelData doBuild() {
            AccelData newData = new AccelData();
            
            newData.setLateral(getLateral());
            newData.setLongitudinal(getLongitudinal());
            newData.setVertical(getVertical());
            
            return newData;
        }
    }
}
