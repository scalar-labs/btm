/*
 * Bitronix Transaction Manager
 *
 * Copyright (c) 2012, Bitronix Software.
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

package bitronix.tm.resource.common;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import bitronix.tm.utils.ClassLoaderUtils;
import bitronix.tm.utils.CryptoEngine;
import bitronix.tm.utils.PropertyUtils;

/**
 * @author Brett Wooldridge
 */
final class XAFactoryHelper {
    private final static Logger log = LoggerFactory.getLogger(XAFactoryHelper.class);

    private final static String PASSWORD_PROPERTY_NAME = "password";

    private XAFactoryHelper() {
        // This class is not instantiable.
    }

    static Object createXAFactory(ResourceBean bean) throws Exception {
        String className = bean.getClassName();
        if (className == null)
            throw new IllegalArgumentException("className cannot be null");
        Class<?> xaFactoryClass = ClassLoaderUtils.loadClass(className);
        Object xaFactory = xaFactoryClass.newInstance();

        for (Map.Entry<Object, Object> entry : bean.getDriverProperties().entrySet()) {
            String name = (String) entry.getKey();
            Object value = entry.getValue();

            if (name.endsWith(PASSWORD_PROPERTY_NAME)) {
                value = decrypt(value.toString());
            }

            if (log.isDebugEnabled()) { log.debug("setting vendor property '" + name + "' to '" + value + "'"); }
            PropertyUtils.setProperty(xaFactory, name, value);
        }
        return xaFactory;
    }

    private static String decrypt(String resourcePassword) throws Exception {
        int startIdx = resourcePassword.indexOf("{");
        int endIdx = resourcePassword.indexOf("}");

        if (startIdx != 0 || endIdx == -1)
            return resourcePassword;

        String cipher = resourcePassword.substring(1, endIdx);
        if (log.isDebugEnabled()) { log.debug("resource password is encrypted, decrypting " + resourcePassword); }
        return CryptoEngine.decrypt(cipher, resourcePassword.substring(endIdx + 1));
    }
}
