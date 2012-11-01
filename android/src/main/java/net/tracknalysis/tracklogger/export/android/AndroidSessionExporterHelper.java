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
package net.tracknalysis.tracklogger.export.android;

import android.database.Cursor;

/**
 * Utility class providing helper methods to exporter implementations.
 *
 * @author David Valeri
 */
class AndroidSessionExporterHelper {
    
    private AndroidSessionExporterHelper() {
    }
    
    static Double getDoubleOrNull(int columnIndex, Cursor cursor) {
        return cursor.isNull(columnIndex) ? null : cursor.getDouble(columnIndex);
    }
    
    static Float getFloatOrNull(int columnIndex, Cursor cursor) {
        return cursor.isNull(columnIndex) ? null : cursor.getFloat(columnIndex);
    }
    
    static Integer getIntegerOrNull(int columnIndex, Cursor cursor) {
        return cursor.isNull(columnIndex) ? null : cursor.getInt(columnIndex);
    }
    
    static Long getLongOrNull(int columnIndex, Cursor cursor) {
        return cursor.isNull(columnIndex) ? null : cursor.getLong(columnIndex);
    }
}
