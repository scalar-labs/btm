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

    private List exceptions = new ArrayList();
    private List resourceStates = new ArrayList();

    public BitronixMultiSystemException(String string, List exceptions, List resourceStates) {
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
    public List getExceptions() {
        return exceptions;
    }

    /**
     * Get the list of XAResourceHolderStates which threw an exception during execution.
     * This list always contains exactly one resource per exception present in {@link #getExceptions} list.
     * Indices of both list always match a resource against the exception it threw.
     * @return the list of resource which threw an exception during execution.
     */
    public List getResourceStates() {
        return resourceStates;
    }

}
