/*
 * (C) Copyright 2015-2016 by MSDK Development Team
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

package io.github.mzmine.modules.io.import_rawdata_mzml.msdk.data;

import java.util.ArrayList;

/**
 * <p>MzMLProductList class.</p>
 *
 */
public class MzMLProductList {

  private ArrayList<MzMLProduct> products;

  /**
   * <p>Constructor for MzMLProductList.</p>
   */
  public MzMLProductList() {
    this.products = new ArrayList<>();
  }

  /**
   * <p>Getter for the field <code>products</code>.</p>
   *
   * @return a {@link ArrayList} object.
   */
  public ArrayList<MzMLProduct> getProducts() {
    return products;
  }

  /**
   * <p>addProduct.</p>
   *
   * @param product a {@link MzMLProduct} object.
   */
  public void addProduct(MzMLProduct product) {
    products.add(product);
  }

}
