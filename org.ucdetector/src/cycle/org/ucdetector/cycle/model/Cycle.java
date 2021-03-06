/**
 * Copyright (c) 2010 Joerg Spieler
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.ucdetector.cycle.model;

import java.util.LinkedList;
import java.util.List;

import org.eclipse.swt.graphics.Image;
import org.ucdetector.Messages;
import org.ucdetector.UCDetectorPlugin;

/**
 * A Cycle holds a List of classes. This classes build a circular reference
 * 
 * <pre>
 * SearchResultRoot
 *   |- SearchResult
 *     |- <font color="red">Cycle</font>
 *       |- CycleType
 *         |- CycleMember
 *           |- CycleRegion
 * </pre>
 */
public class Cycle extends CycleBaseElement {
  private final LinkedList<CycleType> cycleList;
  private final SearchResult parent;

  public Cycle(SearchResult parent, LinkedList<CycleType> cycleList) {
    if (cycleList.size() < 2) {
      throw new IllegalArgumentException("Cycle must have 2 Elements"); //$NON-NLS-1$
    }
    this.parent = parent;
    this.cycleList = cycleList;
    for (CycleType cycleType : cycleList) {
      cycleType.setParent(this);
    }
  }

  /**
   * @param other cycle which may be included
   * @return <code>true</code> when A-B-C-A contains C-A
   */
  public boolean contains(Cycle other) {
    if (other.cycleList.size() > cycleList.size()) {
      return false;
    }
    for (int iThis = 0; iThis < cycleList.size() * 2; iThis++) {
      boolean same = true;
      for (int iOther = 0; iOther < other.cycleList.size(); iOther++) {
        int offset = (iThis + iOther) % cycleList.size();
        CycleType type = cycleList.get(offset);
        CycleType ohter = other.cycleList.get(iOther);
        if (!type.getJavaElement().equals(ohter.getJavaElement())) {
          same = false;
        }
      }
      if (same) {
        return true;
      }
    }
    return false;
  }

  /**
   * A-B-C-A is the same as B-C-A-B
   */
  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof Cycle)) {
      return false;
    }
    Cycle other = (Cycle) obj;
    if (cycleList.size() != other.cycleList.size()) {
      return false;
    }
    return contains(other);
  }

  @Override
  public int hashCode() {
    return cycleList.hashCode();
  }

  /**
   * A-B-C-A -&gt; B-C-A-B
   */
  public void rotate() {
    cycleList.addLast(cycleList.removeFirst());
  }

  @Override
  public List<CycleType> getChildren() {
    return cycleList;
  }

  @Override
  public Image getImage() {
    return UCDetectorPlugin.getImage(UCDetectorPlugin.IMAGE_CYCLE);
  }

  @Override
  public SearchResult getParent() {
    return parent;
  }

  private int getMatchCount() {
    int result = 0;
    for (CycleType cycleType : cycleList) {
      result += cycleType.getMatchCount();
    }
    return result;
  }

  @Override
  public String getText() {
    StringBuilder sb = new StringBuilder();
    sb.append(Messages.Cycle_Name).append(": "); //$NON-NLS-1$
    for (CycleType cycleType : cycleList) {
      sb.append(cycleType.getJavaElement().getElementName()).append('-');
    }
    sb.append(cycleList.getFirst().getJavaElement().getElementName());
    sb.append(" - ").append(getChildrenSize()).append(" classes"); //$NON-NLS-1$ //$NON-NLS-2$
    sb.append(" - ").append(getMatchCount()).append(" matches"); //$NON-NLS-1$ //$NON-NLS-2$
    return sb.toString();
  }
}
