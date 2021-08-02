/*
 * (C) Copyright 2015-2017 by MSDK Development Team
 *
 * This software is dual-licensed under either
 *
 * (a) the terms of the GNU Lesser General Public License version 2.1 as published by the Free
 * Software Foundation
 *
 * or (per the licensee's choosing)
 *
 * (b) the terms of the Eclipse Public License v1.0 as published by the Eclipse Foundation.
 */

package io.github.msdk.datamodel;

import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Implementation of the Feature interface.
 *
 * @since 0.0.8
 */
public class SimpleFeature implements Feature {

  private @Nonnull Double mz;
  private @Nonnull Float retentionTime;
  private @Nullable Float area, height, snRatio, score;
  private @Nullable Chromatogram chromatogram;
  private @Nullable List<MsScan> msmsSpectra;
  private @Nullable IonAnnotation ionAnnotation;

  /** {@inheritDoc} */
  @Override
  public Double getMz() {
    return mz;
  }

  /** {@inheritDoc} */
  @Override
  public Float getRetentionTime() {
    return retentionTime;
  }

  /** {@inheritDoc} */
  @Override
  public Float getArea() {
    return area;
  }

  /** {@inheritDoc} */
  @Override
  public Float getHeight() {
    return height;
  }

  /** {@inheritDoc} */
  @Override
  public Float getSNRatio() {
    return snRatio;
  }

  /** {@inheritDoc} */
  @Override
  public Float getScore() {
    return score;
  }

  /** {@inheritDoc} */
  @Override
  public Chromatogram getChromatogram() {
    return chromatogram;
  }

  /** {@inheritDoc} */
  @Override
  public List<MsScan> getMSMSSpectra() {
    return msmsSpectra;
  }

  /** {@inheritDoc} */
  @Override
  public IonAnnotation getIonAnnotation() {
    return ionAnnotation;
  }


  /**
   * <p>Setter for the field <code>mz</code>.</p>
   *
   * @param mz a {@link Double} object.
   */
  public void setMz(Double mz) {
    this.mz = mz;
  }

  /**
   * <p>Setter for the field <code>retentionTime</code>.</p>
   *
   * @param retentionTime a {@link Float} object.
   */
  public void setRetentionTime(Float retentionTime) {
    this.retentionTime = retentionTime;
  }

  /**
   * <p>Setter for the field <code>area</code>.</p>
   *
   * @param area a {@link Float} object.
   */
  public void setArea(Float area) {
    this.area = area;
  }

  /**
   * <p>Setter for the field <code>height</code>.</p>
   *
   * @param height a {@link Float} object.
   */
  public void setHeight(Float height) {
    this.height = height;
  }

  /**
   * <p>setSNRatio.</p>
   *
   * @param snRatio a {@link Float} object.
   */
  public void setSNRatio(Float snRatio) {
    this.snRatio = snRatio;
  }

  /**
   * <p>Setter for the field <code>score</code>.</p>
   *
   * @param score a {@link Float} object.
   */
  public void setScore(Float score) {
    this.score = score;
  }

  /**
   * <p>Setter for the field <code>chromatogram</code>.</p>
   *
   * @param chromatogram a {@link Chromatogram} object.
   */
  public void setChromatogram(Chromatogram chromatogram) {
    this.chromatogram = chromatogram;
  }

  /**
   * <p>setMSMSSpectra.</p>
   *
   * @param msmsSpectra a {@link List} object.
   */
  public void setMSMSSpectra(List<MsScan> msmsSpectra) {
    this.msmsSpectra = msmsSpectra;
  }

  /**
   * <p>Setter for the field <code>ionAnnotation</code>.</p>
   *
   * @param ionAnnotation a {@link IonAnnotation} object.
   */
  public void setIonAnnotation(IonAnnotation ionAnnotation) {
    this.ionAnnotation = ionAnnotation;
  }
  
}
