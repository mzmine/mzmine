package net.sf.mzmine.util.maths;

@FunctionalInterface
public interface Transform {

  public static final Transform LOG = Math::log;

  public double transform(double v);
}
