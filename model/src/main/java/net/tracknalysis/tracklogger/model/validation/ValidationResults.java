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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Represents the results of a validation activity. Similar to JSR303 in
 * purpose, but designed to avoid using JSR303 until I can figure out a good way
 * to use it in a mobile environment.
 * 
 * @author David Valeri
 */
public class ValidationResults {

    private final Set<ValidationError> validationErrors = new HashSet<ValidationError>();
    
    public ValidationResults addValidationError(ValidationError validationError) {
        if (validationError == null) {
            throw new IllegalArgumentException("validationError cannot be null.");
        }
        
        validationErrors.add(validationError);
        return this;
    }
    
    public boolean hasValidationErrors() {
        return !validationErrors.isEmpty();
    }
    
    public Collection<ValidationError> getValidationErrors() {
        return new ArrayList<ValidationError>(validationErrors);
    }
    
    public Map<String, List<ValidationError>> getValidationErrorsByFieldNames() {
        
        Map<String, List<ValidationError>> keyedErrors = new HashMap<String, List<ValidationError>>();
        
        for (ValidationError validationError : validationErrors) {
            List<ValidationError> list = keyedErrors.get(validationError.getField().getFieldName());
            if (list == null) {
                list = new LinkedList<ValidationError>();
                keyedErrors.put(validationError.getField().getFieldName(), list);
            }
            
            list.add(validationError);
        }
        
        return keyedErrors;
    }
}
