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
package bitronix.tm.timer;

import bitronix.tm.BitronixTransaction;
import bitronix.tm.internal.BitronixSystemException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;

/**
 * This task is used to mark a transaction as timed-out.
 *
 * @author lorban
 */
public class TransactionTimeoutTask extends Task {

    private final static Logger log = LoggerFactory.getLogger(TransactionTimeoutTask.class);

    private final BitronixTransaction transaction;

    public TransactionTimeoutTask(BitronixTransaction transaction, Date executionTime, TaskScheduler scheduler) {
        super(executionTime, scheduler);
        this.transaction = transaction;
    }

    public Object getObject() {
        return transaction;
    }

    public void execute() throws TaskException {
        try {
            if (log.isDebugEnabled()) log.debug("marking " + transaction + " as timed out");
            transaction.timeout();
        } catch (BitronixSystemException ex) {
            throw new TaskException("failed to timeout " + transaction, ex);
        }
    }

    public String toString() {
        return "a TransactionTimeoutTask on " + transaction + " scheduled for " + getExecutionTime();
    }

}
