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
package bitronix.tm;

import bitronix.tm.utils.DefaultExceptionAnalyzer;
import bitronix.tm.utils.ExceptionAnalyzer;
import junit.framework.TestCase;

import javax.transaction.xa.XAException;

/**
 *
 * @author Ludovic Orban
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

        @Override
        public String extractExtraXAExceptionDetails(XAException ex) {
            return "";
        }

        @Override
        public void shutdown() {
        }
    }

}
