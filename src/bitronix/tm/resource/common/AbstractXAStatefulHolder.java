package bitronix.tm.resource.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import bitronix.tm.utils.Decoder;

import java.util.List;
import java.util.ArrayList;

/**
 * Implementation of all services required by a {@link XAStatefulHolder}.
 * <p>&copy; <a href="http://www.bitronix.be">Bitronix Software</a></p>
 *
 * @author lorban
 */
public abstract class AbstractXAStatefulHolder implements XAStatefulHolder {

    private final static Logger log = LoggerFactory.getLogger(AbstractXAStatefulHolder.class);

    private int state = STATE_IN_POOL;
    private List stateChangeEventListeners = new ArrayList();


    public synchronized int getState() {
        return state;
    }

    public void setState(int state) {
        int oldState = this.state;
        fireStateChanging(oldState, state);

        synchronized (this) {
            if (oldState == state)
                throw new IllegalArgumentException("cannot switch state from " + Decoder.decodeXAStatefulHolderState(oldState) +
                        " to " + Decoder.decodeXAStatefulHolderState(state));

            if (log.isDebugEnabled()) log.debug("state changing from " + Decoder.decodeXAStatefulHolderState(oldState) +
                    " to " + Decoder.decodeXAStatefulHolderState(state) + " in " + this);

            this.state = state;
        }

        fireStateChanged(oldState, state);
    }

    public void addStateChangeEventListener(StateChangeListener listener) {
        stateChangeEventListeners.add(listener);
    }

    public void removeStateChangeEventListener(StateChangeListener listener) {
        stateChangeEventListeners.remove(listener);
    }

    private void fireStateChanging(int currentState, int futureState) {
        if (log.isDebugEnabled()) log.debug("notifying " + stateChangeEventListeners.size() +
                " stateChangeEventListener(s) about state changing from " + Decoder.decodeXAStatefulHolderState(currentState) +
                " to " + Decoder.decodeXAStatefulHolderState(futureState) + " in " + this);

        for (int i = 0; i < stateChangeEventListeners.size(); i++) {
            StateChangeListener stateChangeListener = (StateChangeListener) stateChangeEventListeners.get(i);
            stateChangeListener.stateChanging(this, currentState, futureState);
        }
    }

    private void fireStateChanged(int oldState, int newState) {
        if (log.isDebugEnabled()) log.debug("notifying " + stateChangeEventListeners.size() +
                " stateChangeEventListener(s) about state changed from " + Decoder.decodeXAStatefulHolderState(oldState) +
                " to " + Decoder.decodeXAStatefulHolderState(newState) + " in " + this);

        for (int i = 0; i < stateChangeEventListeners.size(); i++) {
            StateChangeListener stateChangeListener = (StateChangeListener) stateChangeEventListeners.get(i);
            stateChangeListener.stateChanged(this, oldState, newState);
        }
    }
}
