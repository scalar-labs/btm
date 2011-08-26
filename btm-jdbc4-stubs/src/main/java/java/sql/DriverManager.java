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


public  class   DriverManager
{
    public  static void 	deregisterDriver(Driver driver) throws  SQLException {}
    public  static Connection 	getConnection(String url)   throws  SQLException { return null; }
    public  static Connection 	getConnection(String url, java.util.Properties info)  throws  SQLException { return null; }
    public  static Connection 	getConnection(String url, String user, String password) throws  SQLException { return null; }
    public  static Driver 	getDriver(String url)   throws  SQLException { return null; }
    public  static java.util.Enumeration<Driver> 	getDrivers() { return null; }
    public  static int 	getLoginTimeout() { return 0; }
    public  static java.io.PrintStream 	getLogStream() { return null; }
    public  static java.io.PrintWriter 	getLogWriter() { return null; }
    public  static void 	println(String message) {}
    public  static void 	registerDriver(Driver driver)   throws  SQLException {}
    public  static void 	setLoginTimeout(int seconds) {}
    public  static void 	setLogStream(java.io.PrintStream out) {}
    public  static void 	setLogWriter(java.io.PrintWriter out) {}
}
