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

import javax.transaction.RollbackException;

/**
 * Subclass of {@link javax.transaction.RollbackException} supporting nested {@link Throwable}s.
 *
 * @author lorban
 */
public class BitronixRollbackException extends RollbackException {

    public BitronixRollbackException(String string) {
        super(string);
    }

    public BitronixRollbackException(String string, Throwable t) {
        super(string);
        initCause(t);
    }

}
