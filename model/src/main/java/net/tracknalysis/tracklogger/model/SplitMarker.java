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
 * @author David Valeri
 */
public final class SplitMarker {
    private Integer id;
    private Integer splitMarkerSetId;
    private String name;
    private double latitiude;
    private double longitude;
    
    public Integer getId() {
        return id;
    }
    
    public void setId(Integer id) {
        this.id = id;
    }
    
    public Integer getSplitMarkerSetId() {
        return splitMarkerSetId;
    }

    public void setSplitMarkerSetId(Integer splitMarkerSetId) {
        this.splitMarkerSetId = splitMarkerSetId;
    }

    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public double getLatitiude() {
        return latitiude;
    }
    
    public void setLatitiude(double latitiude) {
        this.latitiude = latitiude;
    }
    
    public double getLongitude() {
        return longitude;
    }
    
    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }
}