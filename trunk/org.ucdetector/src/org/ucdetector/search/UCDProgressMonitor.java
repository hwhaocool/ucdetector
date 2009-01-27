package org.ucdetector.search;

import org.eclipse.core.runtime.IProgressMonitor;
import org.ucdetector.Log;
import org.ucdetector.UCDetectorPlugin;

/**
 *
 */
public class UCDProgressMonitor implements IProgressMonitor {
  private final IProgressMonitor delegate;
  private String taskName = "?"; //$NON-NLS-1$

  public UCDProgressMonitor(IProgressMonitor delegate) {
    this.delegate = delegate;
  }

  public void beginTask(String beginTaskName, int totalWork) {
    this.taskName = beginTaskName;
    Log.logDebug("\n--------------------------------------------"); //$NON-NLS-1$
    Log.logInfo("Start task: " + beginTaskName); //$NON-NLS-1$
    Log.logDebug(UCDetectorPlugin.getPreferencesAsString());
    delegate.beginTask(beginTaskName, totalWork);
  }

  public void done() {
    Log.logInfo("End task: " + taskName); //$NON-NLS-1$
    Log.logDebug("--------------------------------------------\n"); //$NON-NLS-1$
    delegate.done();
  }

  public void internalWorked(double work) {
    delegate.internalWorked(work);
  }

  public boolean isCanceled() {
    return delegate.isCanceled();
  }

  public void setCanceled(boolean value) {
    Log.logInfo("Task canceled: " + taskName); //$NON-NLS-1$
    delegate.setCanceled(value);
  }

  public void setTaskName(String name) {
    delegate.setTaskName(name);
  }

  public void subTask(String name) {
    if (Log.DEBUG) {
      Log.logDebug(name);
    }
    delegate.subTask(name);
  }

  public void worked(int work) {
    delegate.worked(work);
  }

  @Override
  public String toString() {
    return delegate.toString();
  }

  @Override
  public boolean equals(Object obj) {
    return delegate.equals(obj);
  }

  @Override
  public int hashCode() {
    return delegate.hashCode();
  }
}