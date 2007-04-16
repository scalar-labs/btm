package bitronix.tm.resource.common;

/**
 * {@link XAStatefulHolder} state change listener interface.
 * <p>&copy; Bitronix 2005, 2006, 2007</p>
 *
 * @author lorban
 */
public interface StateChangeListener {

    /**
     * Fired when the internal state of a {@link XAStatefulHolder} is changing.
     * @param source the {@link XAStatefulHolder} changing state.
     * @param oldState the old state of the {@link XAStatefulHolder}.
     * @param newState the new state of the {@link XAStatefulHolder}.
     */
    public void stateChanged(XAStatefulHolder source, int oldState, int newState);

}
