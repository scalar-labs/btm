package bitronix.tm.utils;

import junit.framework.TestCase;

import java.util.Properties;
import java.util.Map;

import bitronix.tm.utils.PropertyException;
import bitronix.tm.utils.PropertyUtils;

/**
 * <p></p>
 * <p>&copy; Bitronix 2005, 2006</p>
 *
 * @author lorban
 */
public class PropertyUtilsTest extends TestCase {

    public void testSmartSetProperties() throws Exception {
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
        assertEquals(true, destination.getABoolean());
        PropertyUtils.setProperty(destination, "aWriteOnlyInt", "20");

        PrivateDestination privateDestination = new PrivateDestination();
        try {
            PropertyUtils.setProperty(privateDestination, "subDestination.props.key", "value");
            fail("it is not possible to set the 'subDestination' property, PropertyException should have been thrown");
        } catch (PropertyException ex) {
            assertEquals("cannot set property 'subDestination.props.key' - 'subDestination' is null and cannot be auto-filled", ex.getMessage());
        }
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

        assertEquals(6, map.size());
        assertEquals("one", map.get("props.number1"));
        assertEquals("two", map.get("props.number2"));
        assertEquals(new Integer(10), map.get("anInteger"));
        assertEquals(new Boolean(true), map.get("aBoolean"));
        assertEquals(new Boolean(false), map.get("anotherBoolean"));
        assertNull(map.get("subDestination"));
    }

    public static class Destination {
        private Properties props;
        private Destination subDestination;
        private int anInteger;
        private int aWriteOnlyInt;
        private boolean aBoolean;
        private boolean anotherBoolean;

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

        public boolean getABoolean() {
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
