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
package bitronix.tm.resource.common;

import bitronix.tm.utils.ClassLoaderUtils;
import bitronix.tm.utils.CryptoEngine;
import bitronix.tm.utils.PropertyUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

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
