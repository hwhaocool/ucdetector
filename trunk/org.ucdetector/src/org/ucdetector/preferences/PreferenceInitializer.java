/**
 * Copyright (c) 2009 Joerg Spieler
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.ucdetector.preferences;

import static org.ucdetector.preferences.WarnLevel.IGNORE;
import static org.ucdetector.preferences.WarnLevel.WARNING;

import java.io.File;

import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.jface.preference.IPreferenceStore;
import org.ucdetector.Log;
import org.ucdetector.UCDetectorPlugin;

/**
 * Class used to initialize default preference values.
 * See: {@link  org.ucdetector.preferences.Prefs#LIST_SEPARATOR}
 */
public class PreferenceInitializer extends AbstractPreferenceInitializer { // NO_UCD
  private static final String WARN = WARNING.name();
  private static final String SOURCE_FOLDER_FILTER //
  = "*test*,generated,"; //$NON-NLS-1$
  private static final String PACKAGE_FILTER //
  = "*test*,"; //$NON-NLS-1$
  private static final String CLASS_FILTER //
  = "Test*,*Test"; //$NON-NLS-1$
  private static final String METHOD_FILTER //
  = "test*,*Test"; //$NON-NLS-1$
  private static final String FIELD_FILTER //
  = "test*,*Test"; //$NON-NLS-1$
  private static final String FILE_PATTERN_LITERAL_SEARCH //
  = "*.xml,"; //$NON-NLS-1$
  private static final String REPORT_DEFAULT_NAME = "UCDetetorReport.html"; //$NON-NLS-1$

  @Override
  public void initializeDefaultPreferences() {
    IPreferenceStore store = UCDetectorPlugin.getDefault().getPreferenceStore();
    // FILTER ------------------------------------------------------------------
    store.setDefault(Prefs.FILTER_SOURCE_FOLDER, SOURCE_FOLDER_FILTER);
    store.setDefault(Prefs.FILTER_PACKAGE, PACKAGE_FILTER);
    store.setDefault(Prefs.FILTER_CLASS, CLASS_FILTER);
    store.setDefault(Prefs.FILTER_METHOD, METHOD_FILTER);
    store.setDefault(Prefs.FILTER_FIELD, FIELD_FILTER);
    store.setDefault(Prefs.FILTER_BEAN_METHOD, true);
    // WHAT TO DETECT ----------------------------------------------------------
    store.setDefault(Prefs.WARN_LIMIT, 0);
    store.setDefault(Prefs.ANALYZE_CLASSES, WARN);
    store.setDefault(Prefs.ANALYZE_MEHTODS, WARN);
    store.setDefault(Prefs.ANALYZE_FIELDS, WARN);
    store.setDefault(Prefs.DETECT_TEST_ONLY, true);
    // CLASS NAMES IN FILES ----------------------------------------------------
    store.setDefault(Prefs.ANALYZE_LITERALS_CHECK, true);
    store.setDefault(Prefs.ANALYZE_CHECK_FULL_CLASS_NAME, true);
    store.setDefault(Prefs.ANALYZE_LITERALS, FILE_PATTERN_LITERAL_SEARCH);
    // OTHER -------------------------------------------------------------------
    String report;
    try {
      File workspace = Platform.getLocation().toFile();
      report = new File(workspace, REPORT_DEFAULT_NAME).getCanonicalPath();
    }
    catch (Exception e) {
      report = REPORT_DEFAULT_NAME;
      Log.logError("Can't get report file name", e); //$NON-NLS-1$
    }
    store.setDefault(Prefs.REPORT_FILE, report);
    // KEYWORDS ----------------------------------------------------------------
    store.setDefault(Prefs.ANALYZE_FINAL_METHOD, IGNORE.name());
    store.setDefault(Prefs.ANALYZE_FINAL_FIELD, IGNORE.name());
    // VISIBILITY --------------------------------------------------------------
    // methods
    store.setDefault(Prefs.ANALYZE_VISIBILITY_PROTECTED_CLASSES, WARN);
    store.setDefault(Prefs.ANALYZE_VISIBILITY_PRIVATE_CLASSES, WARN);
    // classes
    store.setDefault(Prefs.ANALYZE_VISIBILITY_PROTECTED_METHODS, WARN);
    store.setDefault(Prefs.ANALYZE_VISIBILITY_PRIVATE_METHODS, WARN);
    // fields
    store.setDefault(Prefs.ANALYZE_VISIBILITY_PROTECTED_FIELDS, WARN);
    store.setDefault(Prefs.ANALYZE_VISIBILITY_PRIVATE_FIELDS, WARN);
    // constants
    store.setDefault(Prefs.ANALYZE_VISIBILITY_PROTECTED_CONSTANTS, WARN);
    store.setDefault(Prefs.ANALYZE_VISIBILITY_PRIVATE_CONSTANTS, WARN);
  }
}
