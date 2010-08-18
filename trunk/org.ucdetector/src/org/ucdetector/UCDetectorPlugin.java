/**
 * Copyright (c) 2010 Joerg Spieler
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.ucdetector;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.preferences.DefaultScope;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.jdt.internal.ui.JavaPluginImages;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.internal.ide.IDEWorkbenchPlugin;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.service.prefs.BackingStoreException;
import org.ucdetector.Log.LogLevel;
import org.ucdetector.preferences.Prefs;

/**
 * Default Activator-class of this plug-ins
 */
@SuppressWarnings("nls")
public class UCDetectorPlugin extends AbstractUIPlugin implements IPropertyChangeListener {
  public static final String IMAGE_FINAL = "IMAGE_FINAL";
  public static final String IMAGE_UCD = "IMAGE_UCD";
  public static final String IMAGE_COMMENT = "IMAGE_COMMENT";
  public static final String IMAGE_TODO = "IMAGE_TODO";
  public static final String IMAGE_CYCLE = "IMAGE_CYCLE";
  /**
   * See MANIFEST.MF: Bundle-SymbolicName, and .project
   */
  public static final String ID = "org.ucdetector";
  // The shared instance.
  private static UCDetectorPlugin plugin;
  private static boolean isHeadlessMode = false;

  /**
   * Internal id for eclipse help
   * see /org.ucdetector/help/contexts.xml
   * http://www.eclipse.org/articles/article.php?file=Article-AddingHelpToRCP/index.html
   */
  public static final String HELP_ID = ID + ".ucd_context_id";
  public static final String HELP_ID_PREFERENCES = ID + ".ucd_context_id_preferences";
  //private final DateFormat dateFormat = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.MEDIUM, Locale.getDefault());
  private final DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
  private static final String SEPARATOR = "-----------------------------------------------------------------------------"; //$NON-NLS-1$

  public UCDetectorPlugin() {
    UCDetectorPlugin.plugin = this;
  }

  private void dumpInformation() {
    Log.logInfo(SEPARATOR);
    Log.logInfo("Starting UCDetector Plug-In version " + getAboutUCDVersion());
    Log.logInfo(SEPARATOR);
    Log.logInfo("Time      : " + getNow());
    Log.logInfo("OS        : " + getAboutOS());
    Log.logInfo("Java      : " + getAboutJavaVersion());
    Log.logInfo("Eclipse   : " + getAboutEclipseVersion());
    Log.logInfo("Home      : " + getAboutEclipseHome());
    Log.logInfo("Logfile   : " + getAboutLogfile());
    Log.logInfo("Workspace : " + getAboutWorkspace());
    Log.logInfo("Log level : " + Log.getAcitveLogLevel().toString());
    Log.logInfo(getPreferencesAsString());
    Log.logInfo(SEPARATOR);
  }

  @Override
  public void start(BundleContext context) throws Exception {
    super.start(context);
    dumpInformation();
    getPreferenceStore().addPropertyChangeListener(this);
  }

  /** @return all available preferences and all and all preferences which are different from default preferences   */
  public static String getPreferencesAsString() {
    StringBuilder sb = new StringBuilder();
    Map<String, String> deltaPreferences = getDeltaPreferences();
    sb.append(String.format("%s UCDetector preferences are different from default preferences:", //
        "" + deltaPreferences.size()));
    Set<Entry<String, String>> entrySet = deltaPreferences.entrySet();
    for (Entry<String, String> entry : entrySet) {
      sb.append(String.format("%n\t%s=%s", entry.getKey(), entry.getValue()));
    }
    return sb.toString();
  }

  /** @return All preferences which are different from default preferences, without internal preferences  */
  public static Map<String, String> getDeltaPreferences() {
    Map<String, String> allDeltas = getPreferencesImpl(new InstanceScope().getNode(ID));
    Set<String> keySetClone = new HashSet<String>(allDeltas.keySet());
    for (String key : keySetClone) {
      if (key.startsWith(Prefs.INTERNAL)) {
        allDeltas.remove(key);
      }
    }
    return allDeltas;
  }

  /** @return All available preferences */
  public static Map<String, String> getAllPreferences() {
    return getPreferencesImpl(new DefaultScope().getNode(ID));
  }

  private static Map<String, String> getPreferencesImpl(IEclipsePreferences ePrefs) {
    Map<String, String> result = new LinkedHashMap<String, String>();
    try {
      String[] propertyNames = ePrefs.keys();
      Arrays.sort(propertyNames);
      for (String propertyName : propertyNames) {
        result.put(propertyName, ePrefs.get(propertyName, null));
      }
    }
    catch (BackingStoreException ex) {
      result.put("EXCEPTION", ex.getMessage());
      Log.logError("Can't get preferences for " + ePrefs, ex);
    }
    return result;
  }

  public static void dumpList(String listString, String separator) {
    Log.logInfo(listString.replace("[", separator).replace(", ", separator).replace("]", ""));
  }

  /**
   * This method is called when the plug-in is stopped
   */
  @Override
  public void stop(BundleContext context) throws Exception {
    Log.logInfo("Stopping UCDetector Plug-In at " + getNow());
    super.stop(context);
    UCDetectorPlugin.plugin = null;
  }

  /**
   * During search there can be <code>OutOfMemoryErrors</code>.
   * This method creates an messages for the user.
   * @param e {@link OutOfMemoryError} , which is wrapped in a {@link CoreException}
   * @throws CoreException which contains the {@link OutOfMemoryError}
   */
  public static void handleOutOfMemoryError(OutOfMemoryError e) throws CoreException {
    Status status = logErrorAndStatus(Messages.OutOfMemoryError_Hint, e);
    throw new CoreException(status);
  }

  /**
  * @return the shared instance.
  */
  public static UCDetectorPlugin getDefault() {
    return UCDetectorPlugin.plugin;
  }

  // ---------------------------------------------------------------------------
  // IMAGES
  // ---------------------------------------------------------------------------
  @Override
  protected void initializeImageRegistry(ImageRegistry registry) {
    super.initializeImageRegistry(registry);
    registry.put(IMAGE_UCD, createImage(ID, "icons/ucd.gif"));
    registry.put(IMAGE_FINAL, createImage(JavaUI.ID_PLUGIN, "icons/full/ovr16/final_co.gif"));
    registry.put(IMAGE_COMMENT, createImage(JavaUI.ID_PLUGIN, "icons/full/etool16/comment_edit.gif"));
    registry.put(IMAGE_CYCLE, createImage(JavaUI.ID_PLUGIN, "icons/full/elcl16/refresh_nav.gif"));
    registry.put(IMAGE_TODO, createImage(IDEWorkbenchPlugin.IDE_WORKBENCH, "icons/full/elcl16/showtsk_tsk.gif"));
  }

  private static ImageDescriptor createImage(String bundleName, String icon) {
    IPath path = new Path(icon);
    // Other example: AbstractUIPlugin.imageDescriptorFromPlugin("plugin.name", "icons/xxx.gif");
    Bundle bundle = Platform.getBundle(bundleName);
    return JavaPluginImages.createImageDescriptor(bundle, path, true);
  }

  /**
   * @param status which is be logged to default log
   */
  public static void logStatus(IStatus status) {
    UCDetectorPlugin ucd = getDefault();
    if (ucd != null && ucd.getLog() != null) {
      ucd.getLog().log(status);
    }
    Log.logStatus(status);
  }

  public static Status logErrorAndStatus(String message, Throwable ex) {
    Status status = new Status(IStatus.ERROR, ID, IStatus.ERROR, message, ex);
    logStatus(status);
    return status;
  }

  public static final Image getSharedImage(String id) {
    ISharedImages sharedImages = PlatformUI.getWorkbench().getSharedImages();
    return sharedImages.getImage(id);
  }

  public static Image getImage(String key) {
    return getDefault().getImageRegistry().get(key);
  }

  public static ImageDescriptor getImageDescriptor(String key) {
    return getDefault().getImageRegistry().getDescriptor(key);
  }

  public static String getNow() {
    return getDefault().getDateFormat().format(new Date());
  }

  // -------------------------------------------------------------------------

  public static IWorkbenchPage getActivePage() {
    return getActiveWorkbenchWindow().getActivePage();
  }

  public static IWorkbenchWindow getActiveWorkbenchWindow() {
    return getDefault().getWorkbench().getWorkbenchWindows()[0];
  }

  public static Shell getShell() {
    return getActiveWorkbenchWindow().getShell();
  }

  protected static void setHeadlessMode(boolean isHeadlessMode) {
    UCDetectorPlugin.isHeadlessMode = isHeadlessMode;
  }

  public static boolean isHeadlessMode() {
    return UCDetectorPlugin.isHeadlessMode;
  }

  private DateFormat getDateFormat() {
    return dateFormat;
  }

  public static void closeSave(Closeable closable) {
    if (closable != null) {
      try {
        closable.close();
      }
      catch (Exception e) {
        // ignore
      }
    }
  }

  // -------------------------------------------------------------------------
  public static String getAboutOS() {
    return System.getProperty("os.name") + " - " + System.getProperty("os.version");
  }

  public static String getAboutJavaVersion() {
    return System.getProperty("java.runtime.version");
  }

  public static String getAboutEclipseVersion() {
    return System.getProperty("osgi.framework.version");
  }

  public static String getAboutUCDVersion() {
    return (String) getDefault().getBundle().getHeaders().get("Bundle-Version");
  }

  public static String getAboutEclipseHome() {
    String eclipseHome = System.getProperty("osgi.install.area");
    if (eclipseHome != null && eclipseHome.startsWith("file:")) {
      return eclipseHome.substring("file:".length());
    }
    return eclipseHome;
  }

  public static String getAboutLogfile() {
    return System.getProperty("osgi.logfile");
  }

  public static String getAboutWorkspace() {
    return System.getProperty("osgi.instance.area");
  }

  public static String getHostName() {
    try {
      return java.net.InetAddress.getLocalHost().getHostName();
    }
    catch (Exception e) {
      return "?";
    }
  }

  public void propertyChange(PropertyChangeEvent event) {
    String property = event.getProperty();
    String newValue = event.getNewValue().toString();
    if (property.equals(Prefs.LOG_LEVEL)) {
      Log.activeLogLevel = LogLevel.valueOf(newValue);
    }
    if (property.equals(Prefs.LOG_TO_ECLIPSE)) {
      Log.logToEclipse = Boolean.parseBoolean(newValue);
    }
  }

  /**
   * 
   * [ 3025571 ] Exception loading modes: Malformed  &#92;uxxxx encoding
   * <p>
   * java.util.Properties.load() fails, because of file names containing Strings (file names)
   *  which are similar to unicode signs
   * <p>
   * @param isFile <code>true</code>, when it is a file, <code>false</code>, when it is a resource in classpath
   * @param modeFileName name of mode file or mode resource
   * @return Map containing key value pairs loaded from modeFileName
   */
  public static Map<String, String> loadModeFile(boolean isFile, String modeFileName) {
    Map<String, String> result = new HashMap<String, String>();
    BufferedReader reader = null;
    try {
      InputStream in = isFile ? new FileInputStream(modeFileName) : Prefs.class.getResourceAsStream(modeFileName);
      reader = new BufferedReader(new InputStreamReader(in));
      String line = null;
      while ((line = reader.readLine()) != null) {
        line = line.trim();
        int index = line.indexOf('=');
        if (line.startsWith("#") || index == -1) { //$NON-NLS-1$
          continue;// comment
        }
        String key = line.substring(0, index);
        String value = (line.length() == index ? "" : line.substring(index + 1)); //$NON-NLS-1$
        result.put(key, value);
      }
    }
    catch (IOException ex) {
      String message = NLS.bind(Messages.ModesPanel_CantSetPreferences, modeFileName);
      UCDetectorPlugin.logErrorAndStatus(message, ex);
    }
    finally {
      UCDetectorPlugin.closeSave(reader);
    }
    return result;
  }
}
