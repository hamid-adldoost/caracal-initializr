
package com.caracal.initializr;

import java.util.Enumeration;
import java.util.ResourceBundle;

/**
 *
 * @author Hamid
 */
public class InitializrReaderUtility {

    static ResourceBundle rb = ResourceBundle.getBundle("initializr");

    public static String getResourceProperity(String key) {
        return rb.getString(key);
    }

    public static Enumeration<String> getResourceKeys() {
        return rb.getKeys();
    }

}
