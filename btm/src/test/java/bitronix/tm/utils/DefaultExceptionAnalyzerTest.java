package bitronix.tm.utils;

import bitronix.tm.internal.BitronixXAException;
import junit.framework.TestCase;
import oracle.jdbc.xa.OracleXAException;

import javax.transaction.xa.XAException;

/**
 * @author lorban
 */
public class DefaultExceptionAnalyzerTest extends TestCase {

    public void testExtract() throws Exception {
        DefaultExceptionAnalyzer analyzer = new DefaultExceptionAnalyzer();
        
        assertNull(analyzer.extractExtraXAExceptionDetails(new BitronixXAException("XA error", XAException.XA_HEURCOM)));
        assertEquals("ORA-1234", analyzer.extractExtraXAExceptionDetails(new OracleXAException(1234)));
    }
}
