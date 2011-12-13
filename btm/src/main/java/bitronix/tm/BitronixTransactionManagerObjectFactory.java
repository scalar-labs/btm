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
package bitronix.tm;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.naming.Name;
import javax.naming.Context;
import javax.naming.spi.ObjectFactory;
import java.util.Hashtable;

/**
 * {@link BitronixTransactionManager} object factory for JNDI references.
 *
 * @author lorban
 */
public class BitronixTransactionManagerObjectFactory implements ObjectFactory {

    private final static Logger log = LoggerFactory.getLogger(BitronixTransactionManagerObjectFactory.class);

    /**
     * Since there can be only one transaction manager per VM instance, this method always returns a reference
     * to the unique BitronixTransactionManager object.
     * @see BitronixTransactionManager
     * @return the unique transaction manager instance.
     */
    public Object getObjectInstance(Object obj, Name name, Context nameCtx, Hashtable<?,?> environment) throws Exception {
        if (log.isDebugEnabled()) log.debug("returning the unique transaction manager instance");
        return TransactionManagerServices.getTransactionManager();
    }


}
