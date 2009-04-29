/**
 * Copyright (c) 2009 Joerg Spieler All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse
 * Public License v1.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.ucdetector.search;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.ucdetector.Log;

/**
 * Wrap an IProgressMonitor and do logging, when IProgressMonitor methods
 * are called
 */
public class UCDProgressMonitor implements IProgressMonitor {
  private String taskName = ""; //$NON-NLS-1$
  private final IProgressMonitor delegate;
  private double lastWork;

  public UCDProgressMonitor(IProgressMonitor delegate) {
    this.delegate = delegate;
  }

  public UCDProgressMonitor() {
    this(new NullProgressMonitor());
  }

  public void beginTask(String name, int totalWork) {
    Log.logInfo("Task.beginTask " + name); //$NON-NLS-1$
    this.taskName = name;
    delegate.beginTask(taskName, totalWork);
  }

  public void done() {
    Log.logInfo("Task.done " + taskName); //$NON-NLS-1$
    delegate.done();
  }

  public void internalWorked(double work) {
    // nothing useful!
    if (work != lastWork) {
      Log.logInfo("Task.internalWorked " + work); //$NON-NLS-1$
    }
    lastWork = work;
    delegate.internalWorked(work);
  }

  public boolean isCanceled() {
    return delegate.isCanceled();
  }

  public void setCanceled(boolean value) {
    Log.logInfo("Task.setCanceled: " + value); //$NON-NLS-1$
    delegate.setCanceled(value);
  }

  public void setTaskName(String name) {
    Log.logInfo("Task.setTaskName " + name); //$NON-NLS-1$
    this.taskName = name;
    delegate.setTaskName(name);
  }

  public void subTask(String name) {
    Log.logInfo("Task.subTask: '" + name);//$NON-NLS-1$ 
    //    if (Log.DEBUG) {
    //      Log.logDebug(name);
    //    }
    //    else if (UCDetectorPlugin.isHeadlessMode()) {
    //    }
    delegate.subTask(name);
  }

  public void worked(int work) {
    // nothing usefull!
    // Log.logInfo("Task.worked: " + work);//$NON-NLS-1$
    delegate.worked(work);
  }
}