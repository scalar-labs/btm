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
package bitronix.tm.internal;

import bitronix.tm.TransactionManagerServices;
import bitronix.tm.utils.Decoder;

import javax.transaction.xa.XAException;
import java.util.ArrayList;
import java.util.List;

/**
 * Subclass of {@link javax.transaction.SystemException} supporting nested {@link Throwable}s.
 *
 * @author lorban
 */
public class BitronixMultiSystemException extends BitronixSystemException {

    private List<? extends Exception> exceptions = new ArrayList<Exception>();
    private List<XAResourceHolderState> resourceStates = new ArrayList<XAResourceHolderState>();

    public BitronixMultiSystemException(String string, List<? extends Exception> exceptions, List<XAResourceHolderState> resourceStates) {
        super(string);
        this.exceptions = exceptions;
        this.resourceStates = resourceStates;
    }

    public String getMessage() {
        StringBuffer errorMessage = new StringBuffer();
        errorMessage.append("collected ");
        errorMessage.append(exceptions.size());
        errorMessage.append(" exception(s):");
        for (int i = 0; i < exceptions.size(); i++) {
            errorMessage.append(System.getProperty("line.separator"));
            Throwable throwable = (Throwable) exceptions.get(i);
            String message = throwable.getMessage();
            XAResourceHolderState holderState = (XAResourceHolderState) resourceStates.get(i);

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

    public boolean isUnilateralRollback() {
        for (int i = 0; i < exceptions.size(); i++) {
            Throwable throwable = (Throwable) exceptions.get(i);
            if (!(throwable instanceof BitronixRollbackSystemException))
                return false;
        }
        return true;
    }

    /**
     * Get the list of exceptions that have been thrown during execution.
     * @return the list of exceptions that have been thrown during execution.
     */
    public List<? extends Exception> getExceptions() {
        return exceptions;
    }

    /**
     * Get the list of XAResourceHolderStates which threw an exception during execution.
     * This list always contains exactly one resource per exception present in {@link #getExceptions} list.
     * Indices of both list always match a resource against the exception it threw.
     * @return the list of resource which threw an exception during execution.
     */
    public List<XAResourceHolderState> getResourceStates() {
        return resourceStates;
    }

}
