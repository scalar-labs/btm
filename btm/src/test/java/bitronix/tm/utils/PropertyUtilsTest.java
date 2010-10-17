/*
 * Bitronix Transaction Manager
 *
 * Copyright (c) 2010, Bitronix Software.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA 02110-1301 USA
 */
package bitronix.tm.utils;

import junit.framework.TestCase;

import java.util.Properties;
import java.util.Map;

/**
 *
 * @author lorban
 */
public class PropertyUtilsTest extends TestCase {

    public void testSetProperties() throws Exception {
        Destination destination = new Destination();

        PropertyUtils.setProperty(destination, "props.key", "value");
        assertEquals("value", destination.getProps().getProperty("key"));
        PropertyUtils.setProperty(destination, "subDestination.props.key", "value");
        assertEquals("value", destination.getSubDestination().getProps().getProperty("key"));
        PropertyUtils.setProperty(destination, "anInteger", "10");
        assertEquals(10, destination.getAnInteger());
        PropertyUtils.setProperty(destination, "subDestination.anInteger", "20");
        assertEquals(20, destination.getSubDestination().getAnInteger());
        PropertyUtils.setProperty(destination, "aBoolean", "true");
        assertEquals(true, destination.isABoolean());
        PropertyUtils.setProperty(destination, "aWriteOnlyInt", "20");

        PrivateDestination privateDestination = new PrivateDestination();
        try {
            PropertyUtils.setProperty(privateDestination, "subDestination.props.key", "value");
            fail("it is not possible to set the 'subDestination' property, PropertyException should have been thrown");
        } catch (PropertyException ex) {
            assertEquals("cannot set property 'subDestination.props.key' - 'subDestination' is null and cannot be auto-filled", ex.getMessage());
        }
    }

    public void testSetPropertiesObjectLongKey() throws Exception {
        PrivateDestination destination = new PrivateDestination();
        
        PropertyUtils.setProperty(destination, "props.key", "value1");
        PropertyUtils.setProperty(destination, "props.a.dotted.key", "value2");

        assertEquals("value1", destination.getProps().get("key"));
        assertEquals("value2", destination.getProps().get("a.dotted.key"));
    }

    public void testSmartGetProperties() throws Exception {
        Destination destination = new Destination();
        destination.setAnInteger(10);
        destination.setABoolean(true);

        Properties props = new Properties();
        props.setProperty("number1", "one");
        props.setProperty("number2", "two");
        destination.setProps(props);

        Map map = PropertyUtils.getProperties(destination);

        assertEquals(12, map.size());
        assertEquals("one", map.get("props.number1"));
        assertEquals("two", map.get("props.number2"));
        assertEquals(new Integer(10), map.get("anInteger"));
        assertEquals(new Boolean(true), map.get("aBoolean"));
        assertEquals(new Boolean(false), map.get("anotherBoolean"));
        assertNull(map.get("subDestination"));
    }

    public void testSetPrimitiveTypes() throws Exception {
        Destination destination = new Destination();

        PropertyUtils.setProperty(destination, "aString", "this is my string");
        PropertyUtils.setProperty(destination, "aBoolean", "true");
        PropertyUtils.setProperty(destination, "aByte", "100");
        PropertyUtils.setProperty(destination, "aShort", "20000");
        PropertyUtils.setProperty(destination, "anInteger", "300000");
        PropertyUtils.setProperty(destination, "aLong", "4000000");
        PropertyUtils.setProperty(destination, "aFloat", "3.14");
        PropertyUtils.setProperty(destination, "aDouble", "0.654987");

        assertEquals("this is my string", destination.getAString());
        assertEquals(true, destination.isABoolean());
        assertEquals(100, destination.getAByte());
        assertEquals(20000, destination.getAShort());
        assertEquals(300000, destination.getAnInteger());
        assertEquals(4000000, destination.getALong());
        assertEquals(3.14f, destination.getAFloat(), 0.01f);
        assertEquals(0.654987, destination.getADouble(), 0.000001);
    }

    public void testGetPrimitiveTypes() throws Exception {
        Destination destination = new Destination();
        destination.setAString("this is my string");
        destination.setABoolean(true);
        destination.setAByte((byte) 100);
        destination.setAShort((short) 20000);
        destination.setAnInteger(300000);
        destination.setALong(4000000L);
        destination.setAFloat(3.14f);
        destination.setADouble(0.654987);

        assertEquals("this is my string", PropertyUtils.getProperty(destination, "aString"));
        assertEquals(Boolean.TRUE, PropertyUtils.getProperty(destination, "aBoolean"));
        assertEquals(new Byte((byte) 100), PropertyUtils.getProperty(destination, "aByte"));
        assertEquals(new Short((short) 20000), PropertyUtils.getProperty(destination, "aShort"));
        assertEquals(new Integer(300000), PropertyUtils.getProperty(destination, "anInteger"));
        assertEquals(new Long(4000000L), PropertyUtils.getProperty(destination, "aLong"));
        assertEquals(new Float(3.14f), PropertyUtils.getProperty(destination, "aFloat"));
        assertEquals(new Double(0.654987), PropertyUtils.getProperty(destination, "aDouble"));
    }

    public static class Destination {
        private Properties props;
        private Destination subDestination;
        private int anInteger;
        private int aWriteOnlyInt;
        private boolean aBoolean;
        private boolean anotherBoolean;
        private String aString;
        private byte aByte;
        private short aShort;
        private long aLong;
        private float aFloat;
        private double aDouble;

        public Properties getProps() {
            return props;
        }

        public void setProps(Properties props) {
            this.props = props;
        }

        public Destination getSubDestination() {
            return subDestination;
        }

        public void setSubDestination(Destination subDestination) {
            this.subDestination = subDestination;
        }

        public int getAnInteger() {
            return anInteger;
        }

        public void setAnInteger(int anInteger) {
            this.anInteger = anInteger;
        }

        public void setAWriteOnlyInt(int aWriteOnlyInt) {
            this.aWriteOnlyInt = aWriteOnlyInt;
        }

        public boolean isABoolean() {
            return aBoolean;
        }

        public void setABoolean(boolean aBoolean) {
            this.aBoolean = aBoolean;
        }

        public boolean isAnotherBoolean() {
            return anotherBoolean;
        }

        public void setAnotherBoolean(boolean anotherBoolean) {
            this.anotherBoolean = anotherBoolean;
        }

        public String getAString() {
            return aString;
        }

        public void setAString(String aString) {
            this.aString = aString;
        }

        public byte getAByte() {
            return aByte;
        }

        public void setAByte(byte aByte) {
            this.aByte = aByte;
        }

        public short getAShort() {
            return aShort;
        }

        public void setAShort(short aShort) {
            this.aShort = aShort;
        }

        public long getALong() {
            return aLong;
        }

        public void setALong(long aLong) {
            this.aLong = aLong;
        }

        public float getAFloat() {
            return aFloat;
        }

        public void setAFloat(float aFloat) {
            this.aFloat = aFloat;
        }

        public double getADouble() {
            return aDouble;
        }

        public void setADouble(double aDouble) {
            this.aDouble = aDouble;
        }
    }

    private class PrivateDestination {
        private Properties props;
        private PrivateDestination subDestination;

        public Properties getProps() {
            return props;
        }

        public void setProps(Properties props) {
            this.props = props;
        }

        public PrivateDestination getSubDestination() {
            return subDestination;
        }

        public void setSubDestination(PrivateDestination subDestination) {
            this.subDestination = subDestination;
        }
    }

}
