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

public  class   Timestamp    extends java.util.Date
{
    public  Timestamp(int year, int month, int date, int hour, int minute, int second, int nano)    {}
    public  Timestamp(long time)    {}

    public  boolean 	after(Timestamp ts) { return false; }
    public  boolean 	before(Timestamp ts)    { return false; }
    public  int 	compareTo(Date o)   { return 0; }
    public  int 	compareTo(Timestamp ts) { return 0; }
    public  boolean 	equals(Object ts)   { return false; }
    public  boolean 	equals(Timestamp ts)    { return false; }
    public  int 	getNanos()  { return 0; }
    public  long 	getTime()   { return 0L; }
    public  void 	setNanos(int n) {  }
    public  void 	setTime(long time)  { }
    public  String 	toString()  { return null; }
    public  static Timestamp 	valueOf(String s)   { return null; }

}
