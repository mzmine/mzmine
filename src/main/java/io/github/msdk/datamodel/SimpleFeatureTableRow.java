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

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.google.common.base.Preconditions;

import io.github.msdk.MSDKRuntimeException;

/**
 * Implementation of FeatureTableRow. Backed by a non-thread safe Map.
 */
public class SimpleFeatureTableRow implements FeatureTableRow {

  private final @Nonnull FeatureTable featureTable;
  private final @Nonnull Map<Sample, Feature> features = new HashMap<>();
  private @Nullable Integer charge;

  /**
   * <p>Constructor for SimpleFeatureTableRow.</p>
   *
   * @param featureTable a {@link FeatureTable} object.
   */
  public SimpleFeatureTableRow(@Nonnull FeatureTable featureTable) {
    Preconditions.checkNotNull(featureTable);
    this.featureTable = featureTable;
  }

  /** {@inheritDoc} */
  @Override
  public @Nonnull FeatureTable getFeatureTable() {
    return featureTable;
  }

  /** {@inheritDoc} */
  @Override
  public Double getMz() {
    Collection<Feature> allFeatures = features.values();
    double averageMz = allFeatures.stream().mapToDouble(Feature::getMz).average().getAsDouble();
    return averageMz;
  }

  /** {@inheritDoc} */
  @Override
  public Float getRT() {
    synchronized (features) {
      Collection<Feature> allFeatures = features.values();
      float averageRt = (float) allFeatures.stream().mapToDouble(Feature::getRetentionTime)
          .average().getAsDouble();
      return averageRt;
    }
  }

  /** {@inheritDoc} */
  @Override
  public Integer getCharge() {
    return charge;
  }

  /**
   * <p>Setter for the field <code>charge</code>.</p>
   *
   * @param charge a {@link Integer} object.
   */
  public void setCharge(@Nullable Integer charge) {
    this.charge = charge;
  }

  /** {@inheritDoc} */
  @Override
  public Feature getFeature(Sample sample) {
    synchronized (features) {
      return features.get(sample);
    }
  }

  /** {@inheritDoc} */
  @Override
  public Feature getFeature(Integer index) {
    assert featureTable != null;
    synchronized (features) {
      List<Sample> samples = featureTable.getSamples();
      return getFeature(samples.get(index));
    }
  }

  /**
   * <p>setFeature.</p>
   *
   * @param sample a {@link Sample} object.
   * @param feature a {@link Feature} object.
   */
  public void setFeature(@Nonnull Sample sample, @Nonnull Feature feature) {
    synchronized (features) {
      if (featureTable != null) {
        List<Sample> allSamples = featureTable.getSamples();
        if (!allSamples.contains(sample)) {
          throw new MSDKRuntimeException(
              "Cannot add feature, because the feature table does not contain sample "
                  + sample.getName());
        }
      }
      features.put(sample, feature);
    }
  }


}
