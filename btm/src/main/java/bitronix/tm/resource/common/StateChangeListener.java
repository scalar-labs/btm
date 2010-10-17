/*
 * Bitronix Transaction Manager
 *
 * Copyright (c) 2010, Bitronix Software.
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
package bitronix.tm.resource.common;

/**
 * {@link XAStatefulHolder} state change listener interface.
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
