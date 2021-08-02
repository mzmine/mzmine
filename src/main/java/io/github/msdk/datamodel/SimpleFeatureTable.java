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

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nonnull;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;

/**
 * Implementation of the FeatureTable interface.
 */
public class SimpleFeatureTable implements FeatureTable {

  private final @Nonnull ArrayList<FeatureTableRow> featureTableRows = new ArrayList<>();
  private final @Nonnull ArrayList<Sample> featureTableSamples = new ArrayList<>();

  /** {@inheritDoc} */
  @Override
  public @Nonnull List<FeatureTableRow> getRows() {
    return ImmutableList.copyOf(featureTableRows);
  }

  /**
   * <p>addRow.</p>
   *
   * @param row a {@link FeatureTableRow} object.
   */
  public void addRow(@Nonnull FeatureTableRow row) {
    Preconditions.checkNotNull(row);
    synchronized (featureTableRows) {
      featureTableRows.add(row);
    }
  }

  /**
   * <p>removeRow.</p>
   *
   * @param row a {@link FeatureTableRow} object.
   */
  public void removeRow(@Nonnull FeatureTableRow row) {
    Preconditions.checkNotNull(row);
    synchronized (featureTableRows) {
      featureTableRows.remove(row);
    }
  }


  /** {@inheritDoc} */
  @Override
  public @Nonnull List<Sample> getSamples() {
    return ImmutableList.copyOf(featureTableSamples);
  }

  /**
   * {@inheritDoc}
   *
   * @param samples a {@link List} object.
   */
  public @Nonnull void setSamples(List<Sample> samples) {
    this.featureTableSamples.clear();
    this.featureTableSamples.addAll(samples);
  }


  /** {@inheritDoc} */
  @Override
  public void dispose() {
    // Do nothing
  }


}
