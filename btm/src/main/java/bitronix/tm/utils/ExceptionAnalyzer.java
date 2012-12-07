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

import javax.transaction.xa.XAException;

/**
 * Exception analyzers are used to extract non-standard information from vendor exceptions.
 *
 * @author lorban
 */
public interface ExceptionAnalyzer extends Service {

    /**
     * Extract information from a vendor's XAException that isn't available through standard APIs.
     * @param ex the {@link XAException} to analyze.
     * @return extra error details as a human-readable string, or null if nothing extra was found.
     */
    public String extractExtraXAExceptionDetails(XAException ex);

}
