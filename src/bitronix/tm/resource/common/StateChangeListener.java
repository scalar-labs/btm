package bitronix.tm.resource.common;

/**
 * {@link XAStatefulHolder} state change listener interface.
 * <p>&copy; <a href="http://www.bitronix.be">Bitronix Software</a></p>
 *
 * @author lorban
 */
public interface StateChangeListener {

    /**
     * Fired when the internal state of a {@link XAStatefulHolder} has changed.
     * @param source the {@link XAStatefulHolder} changing state.
     * @param oldState the old state of the {@link XAStatefulHolder}.
     * @param newState the new state of the {@link XAStatefulHolder}.
     */
    public void stateChanged(XAStatefulHolder source, int oldState, int newState);

    /**
     * Fired before the internal state of a {@link XAStatefulHolder} has changed.
     * @param source the {@link XAStatefulHolder} changing state.
     * @param currentState the current state of the {@link XAStatefulHolder}.
     * @param futureState the future state of the {@link XAStatefulHolder}.
     */
    public void stateChanging(XAStatefulHolder source, int currentState, int futureState);

}
