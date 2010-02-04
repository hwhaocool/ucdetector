/**
 * Copyright (c) 2010 Joerg Spieler
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.ucdetector.cycle.model;

import java.util.List;

import org.eclipse.swt.graphics.Image;

/**
 * Interface for all CycleBaseElement, shown in the cycle view
 */
public interface ICycleBaseElement {
  /**
   * @return parent of the ICycleBaseElement in the tree
   */
  ICycleBaseElement getParent();

  /**
   * @return Text for the ICycleBaseElement
   */
  String getText();

  /**
   * @return Image for the ICycleBaseElement
   */
  Image getImage();

  // -------------------------------------------------------------------------
  // CHILDREN
  // -------------------------------------------------------------------------

  /**
   * @return children of the ICycleBaseElement in the tree, never returns
   *         <code>null</code>
   */
  List<? extends ICycleBaseElement> getChildren();

  /**
   * @return <code>true</code> when there are more than one child
   */
  boolean hasChildren();

  /**
   * @return number of children
   */
  int getChildrenSize();

  public ICycleBaseElement getNextMatch();

}
