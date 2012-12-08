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
package bitronix.tm.integration.tomcat55;

import bitronix.tm.TransactionManagerServices;
import org.apache.catalina.Lifecycle;
import org.apache.catalina.LifecycleEvent;
import org.apache.catalina.LifecycleListener;

import java.util.logging.Logger;

public class BTMLifecycleListener implements LifecycleListener {

    private final static Logger log = Logger.getLogger(BTMLifecycleListener.class.getName());

    public void lifecycleEvent(LifecycleEvent event) {
        if (Lifecycle.BEFORE_START_EVENT.equals(event.getType())) {
            log.info("Starting Bitronix Transaction Manager");
            TransactionManagerServices.getTransactionManager();
        }
        else if (Lifecycle.AFTER_STOP_EVENT.equals(event.getType())) {
            log.info("Shutting down Bitronix Transaction Manager");
            TransactionManagerServices.getTransactionManager().shutdown();
        }
    }

}