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
            if (log.isDebugEnabled()) { log.debug("marking " + transaction + " as timed out"); }
            transaction.timeout();
        } catch (BitronixSystemException ex) {
            throw new TaskException("failed to timeout " + transaction, ex);
        }
    }

    public String toString() {
        return "a TransactionTimeoutTask on " + transaction + " scheduled for " + getExecutionTime();
    }

}
