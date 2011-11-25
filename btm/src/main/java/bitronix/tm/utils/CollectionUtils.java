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
package bitronix.tm.utils;

import java.util.Collection;
import java.util.Iterator;

/**
 * <p>{@link Collection} helper functions.</p>
 *
 * @author lorban
 */
public class CollectionUtils {

    /**
     * Check if a collection contains a specific object by searching for it by identity
     * instead of by using equals/hashcode.
     * @param collection the collection to search in.
     * @param toBeFound the object to search for.
     * @return true if the collection contains the object, false otherwise.
     */
    public static boolean containsByIdentity(Collection<?> collection, Object toBeFound) {
        for (Object o : collection) {
            if (o == toBeFound)
                return true;
        }
        return false;
    }

}
