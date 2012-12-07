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

import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * Exception related utilities.
 *
 * @author Stephane Nicoll
 */
public final class ExceptionUtils {

    private ExceptionUtils() {
    }

    /**
     * Returns the stack trace of the specified {@link Throwable}.
     *
     * @param t the throwable
     * @return the string representation of the stack trace
     */
    public static String getStackTrace(Throwable t) {
        final StringWriter sw = new StringWriter();
        final PrintWriter pw = new PrintWriter(sw, true);
        t.printStackTrace(pw);
        return sw.getBuffer().toString();
    }
}
