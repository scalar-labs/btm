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
package bitronix.tm.resource.jdbc.proxy;

import java.lang.reflect.Method;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Map;

/**
 * @author Brett Wooldridge
 */
public class ResultSetJavaProxy extends JavaProxyBase<ResultSet> {

    private final static Map<String, Method> selfMethodMap = createMethodMap(ConnectionJavaProxy.class);

    private Statement statement;

    public ResultSetJavaProxy(Statement statement, ResultSet resultSet) {
		initialize(statement, resultSet);
	}

    public ResultSetJavaProxy() {
    	// Default constructor
    }

    void initialize(Statement statement, ResultSet resultSet) {
        this.statement = statement;
        this.delegate = resultSet;
    }

    /* Overridden methods of java.sql.ResultSet */

    public Statement getStatement() throws SQLException {
    	return statement;
    }

    /* Overridden methods of JavaProxyBase */

	@Override
	protected Map<String, Method> getMethodMap() {
		return selfMethodMap;
	}
}
