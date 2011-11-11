package bitronix.tm.resource.common;

import java.util.List;

/**
 * @author Ludovic Orban
 */
public class XAPoolHelper {

    public static List<XAStatefulHolder> getXAResourceHolders(XAPool xaPool) {
        return xaPool.getXAResourceHolders();
    }

}
