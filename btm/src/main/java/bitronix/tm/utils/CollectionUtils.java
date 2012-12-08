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

import java.util.Collection;

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
