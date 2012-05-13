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

import bitronix.tm.utils.DefaultExceptionAnalyzer;
import bitronix.tm.utils.ExceptionAnalyzer;
import junit.framework.TestCase;

import javax.transaction.xa.XAException;

/**
 *
 * @author lorban
 */
public class ExceptionAnalyzerTest extends TestCase {

    public void testExceptionAnalyzer() throws Exception {
        assertEquals(DefaultExceptionAnalyzer.class, TransactionManagerServices.getExceptionAnalyzer().getClass());

        TransactionManagerServices.clear();

        TransactionManagerServices.getConfiguration().setExceptionAnalyzer("nonexistentClass");
        assertEquals(DefaultExceptionAnalyzer.class, TransactionManagerServices.getExceptionAnalyzer().getClass());

        TransactionManagerServices.clear();

        TransactionManagerServices.getConfiguration().setExceptionAnalyzer(TestExceptionAnalyzer.class.getName());
        assertEquals(TestExceptionAnalyzer.class, TransactionManagerServices.getExceptionAnalyzer().getClass());
    }
    
    public static class TestExceptionAnalyzer implements ExceptionAnalyzer {

        public String extractExtraXAExceptionDetails(XAException ex) {
            return "";
        }

        public void shutdown() {
        }
    }

}
