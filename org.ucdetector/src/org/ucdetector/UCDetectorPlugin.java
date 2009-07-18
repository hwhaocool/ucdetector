/**
 * Copyright (c) 2009 Joerg Spieler
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.ucdetector;

import java.text.DateFormat;
import java.util.Date;
import java.util.Locale;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
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

/**
 * Default Activator-class of this plug-ins
 */
public class UCDetectorPlugin extends AbstractUIPlugin {
  public static final String IMAGE_FINAL = "IMAGE_FINAL"; //$NON-NLS-1$
  public static final String IMAGE_UCD = "IMAGE_UCD"; //$NON-NLS-1$
  /** org.eclipse.jdt.ui\etool16\comment_edit.gif */
  public static final String IMAGE_COMMENT = "IMAGE_COMMENT"; //$NON-NLS-1$
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
  public static final String HELP_ID_PREFERENCES = ID
      + ".ucd_context_id_preferences";//$NON-NLS-1$
  public static final DateFormat DATE_FORMAT = DateFormat.getDateTimeInstance(
      DateFormat.MEDIUM, DateFormat.MEDIUM, Locale.getDefault());

  public UCDetectorPlugin() {
    UCDetectorPlugin.plugin = this;
  }

  @Override
  public void start(BundleContext context) throws Exception {
    super.start(context);
    dumpInformation();
  }

  private void dumpInformation() {
    Object version = getBundle().getHeaders().get("Bundle-Version"); //$NON-NLS-1$
    Log.logInfo("\tUCD version : " + version); //$NON-NLS-1$
    Log.logInfo("\tTime        : " + //  //$NON-NLS-1$
        DATE_FORMAT.format(new Date()));
    Log.logInfo("\tjava.version: " + // //$NON-NLS-1$
        System.getProperty("java.version")); //$NON-NLS-1$
    Log.logInfo("\tEclipse     : " + // //$NON-NLS-1$
        System.getProperty("osgi.framework.version")); //$NON-NLS-1$
    Log.logInfo("\tLogfile     : " + // //$NON-NLS-1$
        System.getProperty("osgi.logfile")); //$NON-NLS-1$
    Log.logInfo(getPreferencesAsString());
  }

  public static String getPreferencesAsString() {
    StringBuilder sb = new StringBuilder();
    sb.append("\r\nUCDetector Plugin Preferences:\r\n"); //$NON-NLS-1$
    String[] propertyNames = plugin.getPluginPreferences().propertyNames();
    sb.append(propertyNames.length);
    sb.append(" preferences are different from default preferences:\r\n"); //$NON-NLS-1$
    for (String propertyName : propertyNames) {
      sb.append("\t").append(propertyName).append("="); //$NON-NLS-1$ //$NON-NLS-2$
      sb.append(plugin.getPluginPreferences().getString(propertyName));
      sb.append("\r\n");//$NON-NLS-1$
    }
    return sb.toString();
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
  public static void handleOutOfMemoryError(OutOfMemoryError e)
      throws CoreException {
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
    registry.put(IMAGE_COMMENT, getUcdImage("comment_edit.gif")); //$NON-NLS-1$
    registry.put(IMAGE_UCD, getUcdImage("ucd.gif")); //$NON-NLS-1$
    registry.put(IMAGE_FINAL, JavaPluginImages.DESC_OVR_FINAL);
  }

  private ImageDescriptor getUcdImage(String icon) {
    IPath path = new Path("icons").append("/" + icon); //$NON-NLS-1$ //$NON-NLS-2$
    return JavaPluginImages.createImageDescriptor(getDefault().getBundle(),
        path, true);
  }

  /**
   * @param status which is be logged to default log
   */
  public static void logStatus(IStatus status) {
    UCDetectorPlugin ucd = getDefault();
    if (ucd != null && ucd.getLog() != null) {
      ucd.getLog().log(status);
    }
    if (status.getSeverity() == IStatus.ERROR) {
      Log.logError(status.getMessage(), status.getException());
    }
    else if (status.getSeverity() == IStatus.WARNING) {
      Log.logWarn(status.getMessage());
    }
    else if (status.getSeverity() == IStatus.INFO) {
      Log.logInfo(status.getMessage());
    }
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
    return DATE_FORMAT.format(new Date());
  }

  // -------------------------------------------------------------------------

  public static IWorkbenchPage getActivePage() {
    return getActiveWorkbenchWindow().getActivePage();
  }

  private static IWorkbenchWindow getActiveWorkbenchWindow() {
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
}