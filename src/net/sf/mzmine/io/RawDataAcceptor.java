/**
 * 
 */
package net.sf.mzmine.io;

import net.sf.mzmine.data.Scan;


/**
 *
 */
public interface RawDataAcceptor {

    void addScan(Scan scan, int index);
    
}
