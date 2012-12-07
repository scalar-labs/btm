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
package bitronix.tm.journal;

import java.io.IOException;

/**
 * Thrown by {@link TransactionLogCursor} when an integrity check fails upon reading a record.
 *
 * @author lorban
 */
public class CorruptedTransactionLogException extends IOException {
	private static final long serialVersionUID = 560393397990915916L;

	public CorruptedTransactionLogException(String s) {
        super(s);
    }
}
