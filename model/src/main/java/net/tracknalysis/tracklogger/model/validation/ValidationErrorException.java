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
 * An exception that can be thrown to indicate that the provided data did not meet validation
 * criteria.  The exception contains a {@link ValidationResults} for the errors that caused the
 * exception.
 *
 * @author David Valeri
 */
public class ValidationErrorException extends Exception {

    private static final long serialVersionUID = 356393008407486215L;

    private ValidationResults validationResults;

    public ValidationErrorException(ValidationResults validationResults) {
        this(null, validationResults);
    }

    public ValidationErrorException(String message, ValidationResults validationResults) {
        super(message);
        
        if (validationResults == null) {
            throw new NullPointerException("validationResults cannot be null.");
        }
        
        this.validationResults = validationResults;
    }
    
    public ValidationResults getValidationResults() {
        return validationResults;
    }
}
