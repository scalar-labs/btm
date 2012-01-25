/*
 * Bitronix Transaction Manager
 *
 * Copyright (c) 2011, Bitronix Software.
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
package bitronix.tm.utils;

import javax.transaction.xa.XAException;

/**
 * Exception analyzers are used to extract non-standard information from vendor exceptions.
 *
 * @author lorban
 */
public interface ExceptionAnalyzer extends Service {

    /**
     * Extract information from a vendor's XAException that isn't available through standard APIs.
     * @param ex the {@link XAException} to analyze.
     * @return extra error details as a human-readable string, or null if nothing extra was found.
     */
    public String extractExtraXAExceptionDetails(XAException ex);

}
