/*
 * Bitronix Transaction Manager
 *
 * Copyright (c) 2010, Bitronix Software.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA 02110-1301 USA
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
 * @author lorban
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
