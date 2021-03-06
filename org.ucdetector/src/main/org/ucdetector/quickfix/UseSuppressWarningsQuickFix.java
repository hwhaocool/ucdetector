/**
 * Copyright (c) 2010 Joerg Spieler
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.ucdetector.quickfix;

import org.eclipse.core.resources.IMarker;
import org.eclipse.jdt.core.dom.BodyDeclaration;
import org.eclipse.jdt.internal.ui.JavaPluginImages;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IRegion;
import org.eclipse.swt.graphics.Image;
import org.ucdetector.Messages;

/**
 * 'Fixes' code by adding @SuppressWarnings("ucd")
 * <p>
 * @author Joerg Spieler
 * @since 2009-07-04
 */
class UseSuppressWarningsQuickFix extends AbstractUCDQuickFix {

  protected UseSuppressWarningsQuickFix(IMarker marker) {
    super(marker);
  }

  /**
   * We should do here complicate stuff, like creating and inserting nodes,
   * search for existing @SuppressWarnings. Like in
   * {@link org.eclipse.jdt.internal.ui.text.correction.SuppressWarningsSubProcessor}
   * <p>
   * This code inserts the annotation at the beginning of the line and then
   * adds a new line, which should work in 95%!
   */
  @Override
  public int runImpl(BodyDeclaration nodeToChange) throws BadLocationException {
    IRegion region = doc.getLineInformationOfOffset(charStart);
    String indent = guessIndent(region);
    String declarationLine = doc.get(region.getOffset(), 0);
    String twoLines = indent + "@SuppressWarnings(\"ucd\")" //$NON-NLS-1$
        + getLineDelimitter() + declarationLine;
    doc.replace(region.getOffset(), 0, twoLines);
    return region.getOffset() + indent.length();
  }

  @Override
  public Image getImage() {
    return JavaUI.getSharedImages().getImage(JavaPluginImages.IMG_OBJS_ANNOTATION);
  }

  @Override
  public String getLabel() {
    return Messages.UseAnnotation_UCD_QuickFix_label;
  }

  @Override
  public String getDescription() {
    return null;
  }
}
