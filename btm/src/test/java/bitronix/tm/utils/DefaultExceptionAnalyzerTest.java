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
package bitronix.tm.utils;

import bitronix.tm.internal.BitronixXAException;
import junit.framework.TestCase;
import oracle.jdbc.xa.OracleXAException;

import javax.transaction.xa.XAException;

/**
 * @author Ludovic Orban
 */
public class DefaultExceptionAnalyzerTest extends TestCase {

    public void testExtract() throws Exception {
        DefaultExceptionAnalyzer analyzer = new DefaultExceptionAnalyzer();
        
        assertNull(analyzer.extractExtraXAExceptionDetails(new BitronixXAException("XA error", XAException.XA_HEURCOM)));
        assertEquals("ORA-1234", analyzer.extractExtraXAExceptionDetails(new OracleXAException(1234)));
    }
}
