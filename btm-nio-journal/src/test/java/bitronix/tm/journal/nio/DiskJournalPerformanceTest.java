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

import bitronix.tm.journal.DiskJournal;
import bitronix.tm.journal.Journal;

/**
 * Classic journal specific performance and load tests.
 *
 * @author juergen kellerer, 2011-04-30
 */
public class DiskJournalPerformanceTest extends AbstractJournalPerformanceTest {

    @Override
    protected int getLogCallsPerEmitter() {
        return 25; // needs to be reduced
    }

    @Override
    protected Journal getJournal() {
        return new DiskJournal();
    }
}
