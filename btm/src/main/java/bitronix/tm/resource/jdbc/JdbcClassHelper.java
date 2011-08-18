package bitronix.tm.resource.jdbc;

import java.lang.reflect.Method;
import java.sql.Connection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JdbcClassHelper {

    private final static Logger log = LoggerFactory.getLogger(JdbcClassHelper.class);

    private final static int DETECTION_TIMEOUT = 5; // seconds

	private static Map<Class<Connection>, Integer> connectionClassVersions = new ConcurrentHashMap<Class<Connection>, Integer>();
	private static Map<Class<Connection>, Method> isValidMethods = new ConcurrentHashMap<Class<Connection>, Method>();

	public static int detectJdbcVersion(Connection connection) {
		@SuppressWarnings("unchecked")
		Class<Connection> connectionClass = (Class<Connection>) connection.getClass();

		Integer jdbcVersionDetected = connectionClassVersions.get(connectionClass);
        if (jdbcVersionDetected != null)
            return jdbcVersionDetected;

        try {
            Method isValidMethod = connectionClass.getMethod("isValid", new Class[]{Integer.TYPE});
            isValidMethod.invoke(connection, new Object[] {new Integer(DETECTION_TIMEOUT)}); // test invoke
            jdbcVersionDetected = 4;
            isValidMethods.put(connectionClass, isValidMethod);
        } catch (Exception ex) {
            jdbcVersionDetected = 3;
        } catch (AbstractMethodError er) {
            // this happens if the driver implements JDBC 3 but runs on JDK 1.6+ (which embeds the JDBC 4 interfaces)
            jdbcVersionDetected = 3;
        }

        connectionClassVersions.put(connectionClass, jdbcVersionDetected);
        if (log.isDebugEnabled()) { log.debug("detected JDBC connection class '" + connectionClass + "' is version " + jdbcVersionDetected + " type"); }

        return jdbcVersionDetected;
	}

	public static Method getIsValidMethod(Connection connection) {
		detectJdbcVersion(connection);
		return isValidMethods.get(connection.getClass());
	}

	
}
