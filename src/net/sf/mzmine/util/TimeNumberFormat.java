/**
 * 
 */
package net.sf.mzmine.util;

import java.text.DateFormat;
import java.text.FieldPosition;
import java.text.NumberFormat;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;


/**
 * NumberFormat implementation to format a given time in seconds
 */
public class TimeNumberFormat extends NumberFormat {

    private static DateFormat rtFormat = new SimpleDateFormat("m:ss");
    
    /**
     * @see java.text.NumberFormat#format(double, java.lang.StringBuffer, java.text.FieldPosition)
     */
    public StringBuffer format(double arg0, StringBuffer arg1,
            FieldPosition arg2) {
        return rtFormat.format(arg0 * 1000, arg1, arg2);
    }

    /**
     * @see java.text.NumberFormat#format(long, java.lang.StringBuffer, java.text.FieldPosition)
     */
    public StringBuffer format(long arg0, StringBuffer arg1,
            FieldPosition arg2) {
        return rtFormat.format(arg0 * 1000, arg1, arg2);
    }

    /**
     * @see java.text.NumberFormat#parse(java.lang.String, java.text.ParsePosition)
     */
    public Number parse(String arg0, ParsePosition arg1) {
        return rtFormat.parse(arg0, arg1).getTime() / 1000;
    }

}
