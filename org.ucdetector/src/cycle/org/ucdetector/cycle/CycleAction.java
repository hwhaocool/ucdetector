/**
 * Copyright (c) 2010 Joerg Spieler
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.ucdetector.cycle;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jface.action.IAction;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PartInitException;
import org.ucdetector.Messages;
import org.ucdetector.UCDetectorPlugin;
import org.ucdetector.action.AbstractUCDetectorAction;
import org.ucdetector.iterator.AbstractUCDetectorIterator;

/**
 * Run detect cycles action
 */
public class CycleAction extends AbstractUCDetectorAction { // NO_UCD
  /** see extension point="org.eclipse.ui.popupMenus" */
  static final String ID = "org.ucdetector.cycle.CycleAction"; //$NON-NLS-1$

  @Override
  protected AbstractUCDetectorIterator createIterator() {
    return new CycleIterator();
  }

  private IStatus status = null;

  @Override
  protected IStatus postIteration() {
    Display.getDefault().asyncExec(new Runnable() {
      @Override
      public void run() {
        try {
          UCDetectorPlugin.getActivePage().showView(CycleView.ID);
          CycleView view = CycleView.getInstance();
          if (view != null) {
            view.refresh();
          }
        }
        catch (PartInitException e) {
          status = UCDetectorPlugin.logToEclipseLog(Messages.CycleAction_cant_open_editor, e);
        }
      }
    });
    return status;
  }

  /**
   * We don't permit this action for fields, methods, package declaration...
   */
  @Override
  protected void handleJavaElementSelections(IAction action) {
    super.handleJavaElementSelections(action);
    for (IJavaElement javaElement : getSelections()) {
      if (javaElement.getElementType() > IJavaElement.TYPE) {
        action.setEnabled(false);
      }
    }
  }
}
