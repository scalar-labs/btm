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

public  class   Time    extends java.util.Date
{
    public  Time(int hour, int minute, int second)  {}
    public  Time(long time) {}

    @Deprecated
    public  int 	getDate() { return 0; }
    @Deprecated
    public  int 	getDay() { return 0; }
    @Deprecated
    public  int 	getMonth() { return 0; }
    @Deprecated
    public  int 	getYear() { return 0; }
    @Deprecated
    public  void 	setDate(int i) {  }
    @Deprecated
    public  void 	setMonth(int i) {  }
    public  void 	setTime(long time) {  }
    @Deprecated
    public  void 	setYear(int i) {  }
    public  String 	toString() { return null; }
    public  static Time 	valueOf(String s) { return null; }

}
