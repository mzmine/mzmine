/**
 * 
 */
package net.sf.mzmine.methods;

import java.io.Serializable;

/**
 *
 */
public interface Method {

    
    public String getMethodDescription();
    
    public Serializable getParameters();
}
