/* 
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package java.sql;

import java.util.Iterator;

public class SQLException extends Exception implements Iterable<Throwable>
{
    public  SQLException()  {}
    public  SQLException(String reason) {}
    public  SQLException(String reason, String SQLState)    {}
    public  SQLException(String reason, String SQLState, int vendorCode)    {}
    public  SQLException(String reason, String sqlState, int vendorCode, Throwable cause)   {}
    public  SQLException(String reason, String sqlState, Throwable cause)   {}
    public  SQLException(String reason, Throwable cause)    {}
    public  SQLException(Throwable cause)   {}
    
    public  int 	getErrorCode() { return 0; }
    public  SQLException 	getNextException() { return null; }
    public  String 	getSQLState() { return null; }
    public  Iterator<Throwable> 	iterator() { return null; }
    public  void 	setNextException(SQLException ex)  { }

}

