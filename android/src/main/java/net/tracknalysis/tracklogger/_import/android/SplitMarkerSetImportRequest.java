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
package net.tracknalysis.tracklogger._import.android;

/**
 * @author David Valeri
 */
class SplitMarkerSetImportRequest {
    
    private final SplitMarkerSetFileFormat format;
    private final String resource;
    private final String name;
    
    public SplitMarkerSetImportRequest(SplitMarkerSetFileFormat format,
            String resource, String name) {
        this.format = format;
        this.resource = resource;
        this.name = name;
    }

    public SplitMarkerSetFileFormat getFormat() {
        return format;
    }

    public String getResource() {
        return resource;
    }

    public String getName() {
        return name;
    }
}
