/**
 * Copyright (c) 2010 Joerg Spieler
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.ucdetector.search;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IInitializer;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.search.IJavaSearchConstants;
import org.eclipse.jdt.core.search.SearchMatch;
import org.eclipse.jdt.core.search.SearchPattern;
import org.eclipse.jdt.core.search.SearchRequestor;
import org.ucdetector.Log;
import org.ucdetector.preferences.Prefs;
import org.ucdetector.util.JavaElementUtil;
import org.ucdetector.util.MarkerFactory;

/**
 * Helper class to detect if fields or methods could use the keyword
 * <code>final</code>.
 * <p>
 * @author Joerg Spieler
 * @since 2008-05-08
 */
class FinalHandler {
  private final MarkerFactory markerFactory;

  FinalHandler(MarkerFactory markerFactory) {
    this.markerFactory = markerFactory;
  }

  /**
   * Create a marker: "Use final for method myMethod()"
   * @return <code>true</code>, if a marker was created
   */
  boolean createFinalMarker(IMethod method, int line) throws CoreException {
    int flags = method.getFlags();
    if (line == LineManger.LINE_NOT_FOUND //
        || !Prefs.isCheckUseFinalMethod()//
        || Flags.isPrivate(flags) //
        || Flags.isStatic(flags) //
        || Flags.isAbstract(flags) //
        || Flags.isFinal(flags) //
        || method.isConstructor()//
        || method.isMainMethod()//
        // [2826205] Can't make interface methods final
        || JavaElementUtil.isInterfaceMethod(method) //
    ) {
      return false;
    }
    IType type = JavaElementUtil.getTypeFor(method, false);
    if (!JavaElementUtil.hasSubClasses(type)) {
      return false;
    }
    return markerFactory.createFinalMarker(method, line);
  }

  /**
   * Create a marker: "Use final for method myMethod()"
   * @return <code>true</code>, if a marker was created
   */
  boolean createFinalMarker(IField field, int line) throws CoreException {
    int flags = field.getFlags();
    if (line == LineManger.LINE_NOT_FOUND //
        || !Prefs.isCheckUseFinalField() //
        || Flags.isFinal(flags)//
        || Flags.isVolatile(flags)// A field can be either final or volatile, not both
        || field.isEnumConstant()// No modifier allowed for enum constants!
        || JavaElementUtil.isInterfaceField(field) // [ 3396856 ] Fields in interfaces are already final
    ) {
      return false;
    }
    if (!canMakeFinal(field)) {
      return false;
    }
    return markerFactory.createFinalMarker(field, line);
  }

  // -------------------------------------------------------------------------
  // Field
  // -------------------------------------------------------------------------
  /**
   * @return <code>true</code>, when a field can use the keyword final
   */
  private static boolean canMakeFinal(IField field) throws CoreException {
    SearchPattern pattern = SearchPattern.createPattern(field, IJavaSearchConstants.WRITE_ACCESSES);
    CanMakeFinalRequestor requestor = new CanMakeFinalRequestor(field);
    JavaElementUtil.runSearch(pattern, requestor);
    return !requestor.fieldHasWriteAccessFromMethod;
  }

  /**
   * Checks, if a field has write access from the same class excluded:
   * <ul>
   * <li>Write access by constructors</li>
   * <li>Ignore write access by field declaration</li>
   * <li>Ignore write access by static initializer: <code>static {CONSTANT=1}</code></li>
   * <li>Ignore write access by instance initializer: <code>{field = 1}</code></li>
   * </ul>
   */
  private static final class CanMakeFinalRequestor extends SearchRequestor {
    boolean fieldHasWriteAccessFromMethod = false;
    private final IField field;

    CanMakeFinalRequestor(IField field) {
      this.field = field;
    }

    @Override
    public String toString() {
      return field.getElementName() + ".hasWriteAccess=" + fieldHasWriteAccessFromMethod; //$NON-NLS-1$
    }

    @Override
    public void acceptSearchMatch(SearchMatch match) {
      try {
        Object matchElement = match.getElement();
        if (!(matchElement instanceof IJavaElement)) {
          return;
        }
        IJavaElement javaElement = (IJavaElement) matchElement;
        if (javaElement instanceof IMethod) {
          IMethod method = (IMethod) javaElement;
          // 2776029  final for fields initialized in subclass constructors
          // ignore write access from a constructor to a instance variable in the same class
          if (!Flags.isStatic(field.getFlags()) && method.isConstructor()) {
            if (JavaElementUtil.isInSameType(field, javaElement)) {
              return;
            }
          }
        }
        // ignore write access from the field declaration
        else if (javaElement instanceof IField) {
          return;
        }
        // static initializer: static {}
        // instance initializer: {}
        else if (javaElement instanceof IInitializer) {
          return;
        }
      }
      catch (JavaModelException e) {
        Log.info("Problem for search match %s: %s", match, e); //$NON-NLS-1$
      }
      this.fieldHasWriteAccessFromMethod = true;
      throw new OperationCanceledException("Cancel Search: Field is not final");//$NON-NLS-1$
    }
  }
}
