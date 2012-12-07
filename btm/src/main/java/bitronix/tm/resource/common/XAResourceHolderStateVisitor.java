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
package bitronix.tm.resource.common;

import bitronix.tm.internal.XAResourceHolderState;

/**
 * This is a thread-safe visitor of a collection of {@link XAResourceHolderState}s
 * guaranteed to be called within the context of a lock.
 *
 * @author brettw
 */
public interface XAResourceHolderStateVisitor {

    /**
     * Called when visiting all {@link XAResourceHolderState}s.
     * @param xaResourceHolderState the currently visited {@link XAResourceHolderState}
     * @return return <code>true</code> to continue visitation, <code>false</code> to stop visitation
     */
    boolean visit(XAResourceHolderState xaResourceHolderState);
}
