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
package net.tracknalysis.tracklogger.export;

/**
 * @author David Valeri
 */
public class ExportProgress {
    private int currentRecordIndex;
    private int totalRecords;
    
    public ExportProgress(int currentRecordIndex, int totalRecords) {
        super();
        this.currentRecordIndex = currentRecordIndex;
        this.totalRecords = totalRecords;
    }

    public int getCurrentRecordIndex() {
        return currentRecordIndex;
    }

    public void setCurrentRecordIndex(int currentRecordIndex) {
        this.currentRecordIndex = currentRecordIndex;
    }

    public int getTotalRecords() {
        return totalRecords;
    }

    public void setTotalRecords(int totalRecords) {
        this.totalRecords = totalRecords;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("ExportProgress [currentRecordIndex=");
        builder.append(currentRecordIndex);
        builder.append(", totalRecords=");
        builder.append(totalRecords);
        builder.append("]");
        return builder.toString();
    }
}