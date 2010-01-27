/**
 * Copyright (c) 2010 Joerg Spieler
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.ucdetector;

import java.net.URL;
import java.text.DateFormat;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.jdt.internal.ui.JavaPluginImages;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;
import org.osgi.service.prefs.BackingStoreException;

/**
 * Default Activator-class of this plug-ins
 */
public class UCDetectorPlugin extends AbstractUIPlugin {
  public static final String IMAGE_FINAL = "IMAGE_FINAL"; //$NON-NLS-1$
  public static final String IMAGE_UCD = "IMAGE_UCD"; //$NON-NLS-1$
  public static final String IMAGE_COMMENT = "IMAGE_COMMENT"; //$NON-NLS-1$
  public static final String IMAGE_TODO = "IMAGE_TODO"; //$NON-NLS-1$
  public static final String IMAGE_CYCLE = "IMAGE_CYCLE"; //$NON-NLS-1$
  /**
   * See MANIFEST.MF: Bundle-SymbolicName, and .project
   */
  public static final String ID = "org.ucdetector"; //$NON-NLS-1$
  // The shared instance.
  private static UCDetectorPlugin plugin;
  private static boolean isHeadlessMode = false;

  /**
   * Internal id for eclipse help
   * see /org.ucdetector/help/contexts.xml
   * http://www.eclipse.org/articles/article.php?file=Article-AddingHelpToRCP/index.html
   */
  public static final String HELP_ID = ID + ".ucd_context_id";//$NON-NLS-1$
  public static final String HELP_ID_PREFERENCES = ID + ".ucd_context_id_preferences";//$NON-NLS-1$
  private final DateFormat dateFormat = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.MEDIUM, Locale
      .getDefault());

  public UCDetectorPlugin() {
    UCDetectorPlugin.plugin = this;
  }

  private void dumpInformation() {
    Log.logInfo("\tStart     : " + getNow()); //$NON-NLS-1$
    Log.logInfo("\tOS        : " + getAboutOS()); //$NON-NLS-1$ 
    Log.logInfo("\tJava      : " + getAboutJavaVersion()); //$NON-NLS-1$ 
    Log.logInfo("\tEclipse   : " + getAboutEclipseVersion()); //$NON-NLS-1$
    Log.logInfo("\tUCDetector: " + getAboutUCDVersion()); //$NON-NLS-1$
    Log.logInfo("\tHome      : " + getAboutEclipseHome()); //$NON-NLS-1$ 
    Log.logInfo("\tLogfile   : " + getAboutLogfile()); //$NON-NLS-1$ 
    Log.logInfo("\tWorkspace : " + getAboutWorkspace()); //$NON-NLS-1$ 
    Log.logInfo(getPreferencesAsString());
  }

  @Override
  public void start(BundleContext context) throws Exception {
    super.start(context);
    dumpInformation();
  }

  public static String getPreferencesAsString() {
    StringBuilder sb = new StringBuilder();
    sb.append("\r\nUCDetector Plugin Preferences:\r\n"); //$NON-NLS-1$
    Map<String, String> preferences = getPreferences();
    sb.append(preferences.size());
    sb.append(" preferences are different from default preferences:\r\n"); //$NON-NLS-1$
    Set<Entry<String, String>> entrySet = preferences.entrySet();
    for (Entry<String, String> entry : entrySet) {
      sb.append(String.format("\t%s=%s%n", entry.getKey(), entry.getValue())); //$NON-NLS-1$
    }
    return sb.toString();
  }

  public static Map<String, String> getPreferences() {
    Map<String, String> result = new LinkedHashMap<String, String>();
    try {
      IEclipsePreferences node = new InstanceScope().getNode(ID);
      // All preferences: node = new DefaultScope().getNode(UCDetectorPlugin.ID);
      String[] propertyNames = node.keys();
      for (String propertyName : propertyNames) {
        result.put(propertyName, node.get(propertyName, null));
      }
    }
    catch (BackingStoreException ex) {
      result.put("EXCEPTION", ex.getMessage()); //$NON-NLS-1$
      Log.logError("Can't get preferences", ex); //$NON-NLS-1$
    }
    return result;
  }

  public static void dumpList(String listString, String separator) {
    Log.logInfo(listString //
        .replace("[", separator) //$NON-NLS-1$ 
        .replace(", ", separator) //$NON-NLS-1$
        .replace("]", "")); //$NON-NLS-1$ //$NON-NLS-2$
  }

  /**
   * This method is called when the plug-in is stopped
   */
  @Override
  public void stop(BundleContext context) throws Exception {
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
    registry.put(IMAGE_UCD, getUcdImage("ucd.gif")); //$NON-NLS-1$
    registry.put(IMAGE_FINAL, JavaPluginImages.DESC_OVR_FINAL);
    registry.put(IMAGE_COMMENT, getEclipseImage("org.eclipse.jdt.ui/icons/full/etool16/comment_edit.gif")); //$NON-NLS-1$
    registry.put(IMAGE_TODO, getEclipseImage("org.eclipse.ui.ide/icons/full/elcl16/showtsk_tsk.gif")); //$NON-NLS-1$
    registry.put(IMAGE_CYCLE, UCDetectorPlugin.getEclipseImage("org.eclipse.jdt.ui/icons/full/elcl16/refresh_nav.gif")); //$NON-NLS-1$
  }

  private ImageDescriptor getUcdImage(String icon) {
    IPath path = new Path("icons").append("/" + icon); //$NON-NLS-1$ //$NON-NLS-2$
    return JavaPluginImages.createImageDescriptor(getDefault().getBundle(), path, true);
  }

  public static ImageDescriptor getEclipseImage(String icon) {
    try {
      URL url = new URL("platform:/plugin/" + icon); //$NON-NLS-1$
      return ImageDescriptor.createFromURL(FileLocator.resolve(url));
    }
    catch (Exception ex) {
      Log.logError("Can't get eclipse image", ex); //$NON-NLS-1$
      return null;
    }
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
    UCDetectorPlugin.logStatus(status);
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

  public DateFormat getDateFormat() {
    return dateFormat;
  }

  // -------------------------------------------------------------------------
  public static String getAboutOS() {
    return System.getProperty("os.name") + " - " //$NON-NLS-1$ //$NON-NLS-2$
        + System.getProperty("os.version"); //$NON-NLS-1$
  }

  public static String getAboutJavaVersion() {
    return System.getProperty("java.runtime.version"); //$NON-NLS-1$
  }

  public static String getAboutEclipseVersion() {
    return System.getProperty("osgi.framework.version"); //$NON-NLS-1$
  }

  public static String getAboutUCDVersion() {
    return (String) getDefault().getBundle().getHeaders().get("Bundle-Version"); //$NON-NLS-1$
  }

  public static String getAboutEclipseHome() {
    String eclipseHome = System.getProperty("osgi.install.area"); //$NON-NLS-1$
    if (eclipseHome != null && eclipseHome.startsWith("file:")) { //$NON-NLS-1$
      return eclipseHome.substring("file:".length()); //$NON-NLS-1$
    }
    return eclipseHome;
  }

  public static String getAboutLogfile() {
    return System.getProperty("osgi.logfile"); //$NON-NLS-1$
  }

  public static String getAboutWorkspace() {
    return System.getProperty("osgi.instance.area"); //$NON-NLS-1$
  }

  public static String getHostName() {
    try {
      return java.net.InetAddress.getLocalHost().getHostName();
    }
    catch (Exception e) {
      return "?"; //$NON-NLS-1$
    }
  }
}
