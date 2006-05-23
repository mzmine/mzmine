/**
 * 
 */
package net.sf.mzmine.util;

import net.sf.mzmine.interfaces.Scan;


/**
 *
 */
public interface RawDataAcceptor {

    public String getTaskDescription();
    
    void addScan(Scan scan);
    
}
