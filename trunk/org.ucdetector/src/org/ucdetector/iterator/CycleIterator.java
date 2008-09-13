/**
 * Copyright (c) 2008 Joerg Spieler
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.ucdetector.iterator;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IType;
import org.ucdetector.Log;
import org.ucdetector.Messages;
import org.ucdetector.cycle.search.CycleSearchManager;
import org.ucdetector.preferences.Prefs;
import org.ucdetector.util.JavaElementUtil;

/**
 * search for class cycles
 */
public class CycleIterator extends AbstractUCDetectorIterator {
  private final List<IType> types = new ArrayList<IType>();

  @Override
  protected void handleType(IType type) throws CoreException {
    boolean ignore = false;
    // Fix [ 2103655 ] Detect cycles does not show anything
    // Don't use "isUCDetectionInClasses()" here!
    if (isPrivate(type) || type.isLocal() || type.isAnonymous()
        || Prefs.filterType(type)) {
      ignore = true;
    }
    // ignore classes declared inside a class
    else if (!type.getCompilationUnit().findPrimaryType().equals(type)) {
      ignore = true;
    }
    if (DEBUG) {
      Log.logDebug("CycleIterator type " + JavaElementUtil.asString(type) //$NON-NLS-1$
          + (ignore ? " NOT" : " ") + " Added"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
    }
    if (!ignore) {
      this.types.add(type);
    }
  }

  /**
   * Don't iterate methods and fields
   */
  @Override
  protected boolean doTypeChildren(IType type) {
    return false;
  }

  @Override
  public void handleEndGlobal(IJavaElement[] objects) throws CoreException {
    getMonitor().beginTask(Messages.CycleIterator_MONITOR_INFO,
        types.size() * 2);
    CycleSearchManager cycleSearchManager = new CycleSearchManager(
        getMonitor(), types, selections);
    cycleSearchManager.search();
  }

  @Override
  public int getElelementsToDetectCount() {
    return types.size();
  }

  @Override
  public String getJobName() {
    return Messages.CycleIterator_JobName;
  }
}
