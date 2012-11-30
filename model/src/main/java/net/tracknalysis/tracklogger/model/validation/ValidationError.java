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
 * Represents a validation error type coupled with the field to which it applies.
 *
 * @author David Valeri
 */
public class ValidationError {
    
    private final Field field;
    private final ValidationErrorType validationErrorType;
    
    public ValidationError(String field, ValidationErrorType validationErrorType) {
        this(new DefaultField(field), validationErrorType);
    }
    
    public ValidationError(Field field, ValidationErrorType validationErrorType) {
        this.field = field;
        this.validationErrorType = validationErrorType;
    }

    public Field getField() {
        return field;
    }
    
    public ValidationErrorType getValidationErrorType() {
        return validationErrorType;
    }
    
    public String getValidationErrorKey() {
        return getValidationErrorKey(field, validationErrorType);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((field == null) ? 0 : field.hashCode());
        result = prime
                * result
                + ((validationErrorType == null) ? 0 : validationErrorType
                        .hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        ValidationError other = (ValidationError) obj;
        if (field == null) {
            if (other.field != null)
                return false;
        } else if (!field.equals(other.field))
            return false;
        if (validationErrorType == null) {
            if (other.validationErrorType != null)
                return false;
        } else if (!validationErrorType.equals(other.validationErrorType))
            return false;
        return true;
    }
    
    public static String getValidationErrorKey(String field, ValidationErrorType validationErrorType) {
        return field + "." + validationErrorType.getErrorTypeCode();
    }
    
    public static String getValidationErrorKey(Field field, ValidationErrorType validationErrorType) {
        return field.getFieldName() + "." + validationErrorType.getErrorTypeCode();
    }
}
