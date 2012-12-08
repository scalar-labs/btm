/*
 * Copyright (C) 2006-2013 Bitronix Software (http://www.bitronix.be)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package bitronix.tm.twopc;

import bitronix.tm.TransactionManagerServices;
import bitronix.tm.internal.XAResourceHolderState;
import bitronix.tm.utils.Decoder;

import javax.transaction.xa.XAException;
import java.util.Collections;
import java.util.List;

/**
 * Thrown when a phase exection has thrown one or more exception(s).  
 *
 * @author Ludovic Orban
 */
public class PhaseException extends Exception {

    private final List<Exception> exceptions;
    private final List<XAResourceHolderState> resourceStates;

    public PhaseException(List<Exception> exceptions, List<XAResourceHolderState> resourceStates) {
        this.exceptions = Collections.unmodifiableList(exceptions);
        this.resourceStates = Collections.unmodifiableList(resourceStates);
    }

    public String getMessage() {
        StringBuilder errorMessage = new StringBuilder();
        errorMessage.append("collected ");
        errorMessage.append(exceptions.size());
        errorMessage.append(" exception(s):");
        for (int i = 0; i < exceptions.size(); i++) {
            errorMessage.append(System.getProperty("line.separator"));
            Throwable throwable = exceptions.get(i);
            String message = throwable.getMessage();
            XAResourceHolderState holderState = resourceStates.get(i);

            if (holderState != null) {
                errorMessage.append(" [");
                errorMessage.append(holderState.getUniqueName());
                errorMessage.append(" - ");
            }
            errorMessage.append(throwable.getClass().getName());
            if (throwable instanceof XAException) {
                XAException xaEx = (XAException) throwable;
                errorMessage.append("(");
                errorMessage.append(Decoder.decodeXAExceptionErrorCode(xaEx));
                String extraErrorDetails = TransactionManagerServices.getExceptionAnalyzer().extractExtraXAExceptionDetails(xaEx);
                if (extraErrorDetails != null) errorMessage.append(" - ").append(extraErrorDetails);
                errorMessage.append(")");
            }
            errorMessage.append(" - ");
            errorMessage.append(message);
            errorMessage.append("]");
        }

        return errorMessage.toString();
    }

    /**
     * Get the list of exceptions that have been thrown during a phase execution.
     * @return the list of exceptions that have been thrown during a phase execution.
     */
    public List<Exception> getExceptions() {
        return exceptions;
    }

    /**
     * Get the list of resource which threw an exception during a phase execution.
     * This list always contains exactly one resource per exception present in {@link #getExceptions} list.
     * Indices of both list always match a resource against the exception it threw.
     * @return the list of resource which threw an exception during a phase execution.
     */
    public List<XAResourceHolderState> getResourceStates() {
        return resourceStates;
    }
}
