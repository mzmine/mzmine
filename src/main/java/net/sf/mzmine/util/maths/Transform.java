package net.sf.mzmine.util.maths;

/**
 * Different math transformations
 * 
 * @author Robin Schmid (robinschmid@uni-muenster.de)
 *
 */
@FunctionalInterface
public interface Transform {

  public static final Transform LOG = Math::log;

  public double transform(double v);
}
