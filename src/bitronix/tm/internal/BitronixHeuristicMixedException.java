package bitronix.tm.internal;

import javax.transaction.HeuristicMixedException;

/**
 * Subclass of {@link javax.transaction.HeuristicMixedException} supporting nested {@link Throwable}s.
 * <p>&copy; Bitronix 2005, 2006, 2007</p>
 *
 * @author lorban
 */
public class BitronixHeuristicMixedException extends HeuristicMixedException  {
    
    public BitronixHeuristicMixedException(String string) {
        super(string);
    }

    public BitronixHeuristicMixedException(String string, Throwable t) {
        super(string);
        initCause(t);
    }

}
