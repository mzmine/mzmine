/**
 * 
 */
package net.sf.mzmine.util;

import net.sf.mzmine.interfaces.Scan;


/**
 *
 */
public interface RawDataAcceptor {

    void addScan(Scan scan, int index);
    
}
