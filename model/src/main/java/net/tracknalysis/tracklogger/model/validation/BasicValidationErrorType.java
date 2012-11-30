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
package net.tracknalysis.tracklogger.model.validation;

/**
 * Basic validation error types.  These cover most validation rules.
 *
 * @author David Valeri
 */
public enum BasicValidationErrorType implements ValidationErrorType {
    
    RANGE_CONSTRAINT("validation.basic.outOfRange"),
    RELATIVE_RANGE_CONSTRAINT("validation.basic.relativeConstraint"),
    INVALID_FORMAT("validation.basic.invalidFormat"),
    NOT_NULL("validation.basic.notNull");
    
    private final String errorTypeCode;
    
    private BasicValidationErrorType(String errorTypeCode) {
        this.errorTypeCode = errorTypeCode;
    }

    @Override
    public String getErrorTypeCode() {
        return errorTypeCode;
    }
}
