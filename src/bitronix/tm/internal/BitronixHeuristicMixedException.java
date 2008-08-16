package bitronix.tm.internal;

import javax.transaction.HeuristicMixedException;

/**
 * Subclass of {@link javax.transaction.HeuristicMixedException} supporting nested {@link Throwable}s.
 * <p>&copy; <a href="http://www.bitronix.be">Bitronix Software</a></p>
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
