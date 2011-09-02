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
package bitronix.tm.resource.common;

import bitronix.tm.internal.XAResourceHolderState;

/**
 * This is a thread-safe visitor of a collection of {@link XAResourceHolderState}s
 * guaranteed to be called within the context of a lock.
 *
 * @author brettw
 */
public interface XAResourceHolderStateVisitor {

    /**
     * Called when visiting all {@link XAResourceHolderState}s.
     * @param xaResourceHolderState the currently visited {@link XAResourceHolderState}
     * @return return <code>true</code> to continue visitation, <code>false</code> to stop visitation
     */
    boolean visit(XAResourceHolderState xaResourceHolderState);
}
