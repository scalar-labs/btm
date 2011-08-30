/*
 * Bitronix Transaction Manager
 *
 * Copyright (c) 2011, Juergen Kellerer.
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

package bitronix.tm.journal.nio;

import bitronix.tm.TransactionManagerServices;
import bitronix.tm.journal.Journal;
import org.junit.Test;

/**
 * Nio journal specific performance and load tests.
 *
 * @author juergen kellerer, 2011-04-30
 */
public class NioJournalPerformanceTest extends AbstractJournalPerformanceTest {

    public Journal getJournal() {
        return new NioJournal();
    }

    @Test
    @Override
    public void testLogPerformance() throws Exception {
        ((NioJournal) journal).setSkipForce(false);
        super.testLogPerformance();
    }

    @Test
    @Override
    public void testLogPerformanceWithoutFsync() throws Exception {
        TransactionManagerServices.getConfiguration().setForcedWriteEnabled(false);
        ((NioJournal) journal).setSkipForce(true);
        super.testLogPerformance();
    }
}
