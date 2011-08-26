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

import java.text.DecimalFormat;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * A Java representation of the SQL TIMESTAMP type. It provides the capability
 * to represent the SQL TIMESTAMP nanosecond value, in addition to the regular
 * date/time value which has millisecond resolution.
 * <p>
 * The Timestamp class consists of a regular Date/Time value, where only the
 * integral seconds value is stored, plus a nanoseconds value where the
 * fractional seconds are stored.
 * <p>
 * The addition of the nanosecond value field to the Timestamp object makes it
 * significantly different from the java.util.Date object which it extends.
 * Users should be cautious in their use of Timestamp objects and should not
 * assume that they are interchangeable with java.util.Date objects when used
 * outside the confines of the java.sql package.
 * 
 */
public class Timestamp extends Date {

    private static final long serialVersionUID = 2745179027874758501L;

    // The nanoseconds time value of the Timestamp
    private int nanos;

    /**
     * @deprecated Please use the constructor Timestamp( long ) Returns a
     *             Timestamp corresponding to the time specified by the supplied
     *             values for Year, Month, Date, Hour, Minutes, Seconds and
     *             Nanoseconds
     * @param theYear
     *            specified as the year minus 1900
     * @param theMonth
     *            specified as an integer in the range 0 - 11
     * @param theDate
     *            specified as an integer in the range 1 - 31
     * @param theHour
     *            specified as an integer in the range 0 - 23
     * @param theMinute
     *            specified as an integer in the range 0 - 59
     * @param theSecond
     *            specified as an integer in the range 0 - 59
     * @param theNano
     *            which defines the nanosecond value of the timestamp specified
     *            as an integer in the range 0 - 999,999,999
     * @throws IllegalArgumentException
     *             if any of the parameters is out of range
     */
    public Timestamp(int theYear, int theMonth, int theDate, int theHour,
            int theMinute, int theSecond, int theNano)
            throws IllegalArgumentException {
        super(theYear, theMonth, theDate, theHour, theMinute, theSecond);
        if (theNano < 0 || theNano > 999999999) {
            throw new IllegalArgumentException();
        }
        nanos = theNano;
    }

    /**
     * Returns a Timestamp object corresponding to the time represented by a
     * supplied time value.
     * 
     * @param theTime
     *            a time value in the format of milliseconds since the Epoch
     *            (January 1 1970 00:00:00.000 GMT)
     */
    public Timestamp(long theTime) {
        super(theTime);
        /*
         * Now set the time for this Timestamp object - which deals with the
         * nanosecond value as well as the base time
         */
        this.setTime(theTime);
    }

    /**
     * Returns true if this timestamp object is later than the supplied
     * timestamp, otherwise returns false.
     * 
     * @param theTimestamp
     *            the timestamp to compare with this timestamp object
     * @return true if this timestamp object is later than the supplied
     *         timestamp, false otherwise
     */
    public boolean after(Timestamp theTimestamp) {
        long thisTime = this.getTime();
        long compareTime = theTimestamp.getTime();

        // If the time value is later, the timestamp is later
        if (thisTime > compareTime) {
            return true;
        }
        // If the time value is earlier, the timestamp is not later
        else if (thisTime < compareTime) {
            return false;
        }
        /*
         * Otherwise the time values are equal in which case the nanoseconds
         * value determines whether this timestamp is later...
         */
        else if (this.getNanos() > theTimestamp.getNanos()) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * Returns true if this timestamp object is earlier than the supplied
     * timestamp, otherwise returns false.
     * 
     * @param theTimestamp
     *            the timestamp to compare with this timestamp object
     * @return true if this timestamp object is earlier than the supplied
     *         timestamp, false otherwise
     */
    public boolean before(Timestamp theTimestamp) {
        long thisTime = this.getTime();
        long compareTime = theTimestamp.getTime();

        // If the time value is later, the timestamp is later
        if (thisTime < compareTime) {
            return true;
        }
        // If the time value is earlier, the timestamp is not later
        else if (thisTime > compareTime) {
            return false;
        }
        /*
         * Otherwise the time values are equal in which case the nanoseconds
         * value determines whether this timestamp is later...
         */
        else if (this.getNanos() < theTimestamp.getNanos()) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * Compares this Timestamp object with a supplied Timestamp object
     * 
     * @param theObject
     *            the timestamp to compare with this timestamp object, passed in
     *            as an Object
     * @return 0 if the two Timestamp objects are equal in time, a value <0 if
     *         this Timestamp object is before the supplied Timestamp and a
     *         value >0 if this Timestamp object is after the supplied Timestamp
     * @throws ClassCastException
     *             if the supplied object is not a Timestamp object
     */
    public int compareTo(Date theObject) throws ClassCastException {
        return this.compareTo((Timestamp) theObject);
    }

    /**
     * Compares this Timestamp object with a supplied Timestamp object
     * 
     * @param theTimestamp
     *            the timestamp to compare with this timestamp object, passed in
     *            as a Timestamp
     * @return 0 if the two Timestamp objects are equal in time, a value <0 if
     *         this Timestamp object is before the supplied Timestamp and a
     *         value >0 if this Timestamp object is after the supplied Timestamp
     */
    public int compareTo(Timestamp theTimestamp) {
        int result = super.compareTo(theTimestamp);
        if (result == 0) {
            int thisNano = this.getNanos();
            int thatNano = theTimestamp.getNanos();
            if (thisNano > thatNano) {
                return 1;
            } else if (thisNano == thatNano) {
                return 0;
            } else {
                return -1;
            }
        }
        return result;
    }

    /**
     * Tests to see if this timestamp is equal to a supplied object.
     * 
     * @param theObject
     * @return true if this Timestamp object is equal to the supplied Timestamp
     *         object false if the object is not a Timestamp object or if the
     *         object is a Timestamp but represents a different instant in time
     */
    public boolean equals(Object theObject) {
        if (theObject instanceof Timestamp) {
            return equals((Timestamp) theObject);
        }
        return false;
    }

    /**
     * Tests to see if this timestamp is equal to a supplied timestamp.
     * 
     * @param theTimestamp
     *            the timestamp to compare with this timestamp object, passed in
     *            as an Object
     * @return true if this Timestamp object is equal to the supplied Timestamp
     *         object
     */
    public boolean equals(Timestamp theTimestamp) {
        if (theTimestamp == null) {
            return false;
        }
        return (this.getTime() == theTimestamp.getTime())
                && (this.getNanos() == theTimestamp.getNanos());
    }

    /**
     * Gets this Timestamp's nanosecond value
     * 
     * @return The timestamp's nanosecond value, an integer between 0 and
     *         999,999,999
     */
    public int getNanos() {
        return nanos;
    }

    /**
     * Returns the time represented by this Timestamp object, as a long value
     * containing the number of milliseconds since the Epoch (January 1 1970,
     * 00:00:00.000 GMT)
     */
    public long getTime() {
        long theTime = super.getTime();
        theTime = theTime + (nanos / 1000000);
        return theTime;
    }

    /**
     * Sets the nanosecond value for this timestamp
     */
    public void setNanos(int n) throws IllegalArgumentException {
    }

    /**
     * Sets the time represented by this Timestamp object to the supplied time,
     * defined as the number of milliseconds since the Epoch (January 1 1970,
     * 00:00:00.000 GMT)
     */
    public void setTime(long theTime) {
        /*
         * Deal with the nanoseconds value. The supplied time is in milliseconds -
         * so we must extract the milliseconds value and multiply by 1000000 to
         * get nanoseconds. Things are more complex if theTime value is
         * negative, since then the time value is the time before the Epoch but
         * the nanoseconds value of the Timestamp must be positive - so we must
         * take the "raw" milliseconds value and subtract it from 1000 to get to
         * the true nanoseconds value Simultaneously, recalculate the time value
         * to the exact nearest second and reset the Date time value
         */
        int milliseconds = (int) (theTime % 1000);
        theTime = theTime - milliseconds;
        if (milliseconds < 0) {
            theTime = theTime - 1000;
            milliseconds = 1000 + milliseconds;
        }
        super.setTime(theTime);
        setNanos(milliseconds * 1000000);
    }

    /**
     * Returns the timestamp formatted as a String in the JDBC Timestamp Escape
     * format, which is of the form "yyyy-mm-dd hh:mm:ss.nnnnnnnnn"
     * 
     * @return A string representing the instant defined by the Timestamp, in
     *         JDBC Timestamp escape format
     */
    public String toString() { return null; }

    /*
     * Private method to format the time
     */
    private String format(int date, int digits) {
        StringBuffer dateStringBuffer = new StringBuffer(String.valueOf(date));
        while (dateStringBuffer.length() < digits) {
            dateStringBuffer = dateStringBuffer.insert(0, '0');
        }
        return dateStringBuffer.toString();
    }

    /*
     * Private method to strip trailing '0' characters from a string. @param
     * inputString the starting string @return a string with the trailing zeros
     * stripped - will leave a single 0 at the beginning of the string
     */
    private String stripTrailingZeros(String inputString) { return null; }

    /**
     * Creates a Timestamp object with a time value equal to the time specified
     * by a supplied String holding the time in JDBC timestamp escape format,
     * which is of the form "yyyy-mm-dd hh:mm:ss.nnnnnnnnn"
     * 
     * @param s
     *            the String containing a time in JDBC timestamp escape format
     * @return A timestamp object with time value as defined by the supplied
     *         String
     */
    public static Timestamp valueOf(String s) throws IllegalArgumentException { return null; }

}

