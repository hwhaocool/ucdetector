/**
 * Copyright (c) 2010 Joerg Spieler
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.ucdetector.search;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IImportDeclaration;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.search.IJavaSearchConstants;
import org.eclipse.jdt.core.search.SearchMatch;
import org.eclipse.jdt.core.search.SearchPattern;
import org.eclipse.jdt.core.search.SearchRequestor;
import org.eclipse.osgi.util.NLS;
import org.eclipse.search.core.text.TextSearchEngine;
import org.eclipse.search.core.text.TextSearchMatchAccess;
import org.eclipse.search.core.text.TextSearchRequestor;
import org.eclipse.search.ui.text.FileTextSearchScope;
import org.ucdetector.Log;
import org.ucdetector.Messages;
import org.ucdetector.UCDInfo;
import org.ucdetector.UCDetectorPlugin;
import org.ucdetector.iterator.TypeContainer;
import org.ucdetector.preferences.Prefs;
import org.ucdetector.report.ReportParam;
import org.ucdetector.util.JavaElementUtil;
import org.ucdetector.util.MarkerFactory;
import org.ucdetector.util.StopWatch;

/**
 * Search for class, methods, fields using the eclipse search mechanism
 * <p>
 * @author Joerg Spieler
 * @since 2008-02-29
 */
public class SearchManager {
  private static final boolean DEBUG = Log.isDebugOption("org.ucdetector/debug/search"); //$NON-NLS-1$
  /** Information for user, that we are searching for final stuff */
  private static final String SEARCH_FINAL_MESSAGE = "final"; //$NON-NLS-1$
  /** Get Information about code lines in source code files   */
  private final LineManger lineManger = new LineManger();
  /** Show progress to user */
  private final UCDProgressMonitor monitor;
  /** Number of classes, methods, fields to search */
  private final int searchTotal;
  /** Number UCDetector problems already found */
  private int markerCreated;
  /** Number of classes, methods, fields already searched */
  private int search = 0;
  /** shortcut to skip methods and fields of classes which have no references */
  private final List<IType> noRefTypes = new ArrayList<IType>();
  /** Skip search for enum constants, because they are used by value() or valueOf() */
  private final UsedByValueEnumsCache usedByValueEnumsCache = new UsedByValueEnumsCache();
  /** contains all exceptions happened during search */
  private final List<IStatus> searchProblems = new ArrayList<IStatus>();
  /** Factory to create markers */
  private final MarkerFactory markerFactory;
  /** handle final stuff   */
  private final FinalHandler finalHandler;

  public SearchManager(UCDProgressMonitor monitor, int searchTotal, MarkerFactory markerFactory) {
    this.monitor = monitor;
    this.searchTotal = searchTotal;
    this.markerFactory = markerFactory;
    this.finalHandler = new FinalHandler(markerFactory);
    ReportParam.lineManager = lineManger;// Hack :-(
  }

  /**
   * Start searching for classes, methods, fields
   * @param typeContainers classes to search
   */
  public final void search(Set<TypeContainer> typeContainers) {
    logStart(typeContainers);
    try {
      int pos = 0;
      for (TypeContainer container : typeContainers) {
        if (monitor.isCanceled()) {
          return;
        }
        pos++;
        if (Log.isDebug()) {
          Log.debug(getProgress(typeContainers, pos, container));
        }
        else if (pos == 1 || pos % 10 == 0 || pos == typeContainers.size()) {
          Log.info(getProgress(typeContainers, pos, container));
        }
        search(container);
      }
    }
    catch (OperationCanceledException e) {
      Log.info("Stop searching because: " + UCDProgressMonitor.CANCEL_MESSAGE); //$NON-NLS-1$
    }
    Log.info("Search end: " + UCDInfo.getNow(true)); //$NON-NLS-1$
    if (searchProblems.size() > 0) {
      IStatus[] stati = searchProblems.toArray(new IStatus[searchProblems.size()]);
      MultiStatus status = new MultiStatus(UCDetectorPlugin.ID, IStatus.ERROR, stati, stati.length
          + " errors happened during UCDetection", null); //$NON-NLS-1$
      UCDetectorPlugin.logToEclipseLog(status);
    }
  }

  private static void logStart(Set<TypeContainer> typeContainers) {
    int methodsToDetect = 0;
    int fieldsToDetect = 0;
    for (TypeContainer container : typeContainers) {
      methodsToDetect += container.getMethods().size();
      fieldsToDetect += container.getFields().size();
    }
    Log.info("Detection start      : " + UCDInfo.getNow(true)); //$NON-NLS-1$
    Log.info("    Classes to detect: " + typeContainers.size()); //$NON-NLS-1$
    Log.info("    Methods to detect: " + methodsToDetect); //$NON-NLS-1$
    Log.info("    Fields  to detect: " + fieldsToDetect); //$NON-NLS-1$
  }

  private void search(TypeContainer container) {
    if (container.getType() != null) {
      searchAndHandleException(container.getType());
    }
    for (IMethod method : container.getMethods()) {
      searchAndHandleException(method);
    }
    for (IField field : container.getFields()) {
      searchAndHandleException(field);
    }
  }

  /**
   * Search a member
   */
  private void searchAndHandleException(IMember member) {
    monitor.setActiveSearchElement(member);
    checkForCancel();
    search++;
    try {
      if (member instanceof IType) {
        searchSpecific((IType) member);
      }
      else if (member instanceof IMethod) {
        searchSpecific((IMethod) member);
      }
      else if (member instanceof IField) {
        searchSpecific((IField) member);
      }
    }
    catch (OperationCanceledException ex) {
      throw ex;
    }
    //  Fix [ 2810802 ] UCDetector crashes with an Exception
    catch (Exception ex) {
      String message = String.format("An exception occurred searching %s %s: %s", // //$NON-NLS-1$
          JavaElementUtil.getMemberTypeString(member), JavaElementUtil.getElementName(member), ex);
      Log.error(message, ex);
      Status status = new Status(IStatus.ERROR, UCDetectorPlugin.ID, IStatus.ERROR, message, ex);
      markerFactory.reportDetectionProblem(status);
      searchProblems.add(status);
      if (searchProblems.size() > 100) {
        throw new OperationCanceledException("Stopped searching. To many Exceptions!"); //$NON-NLS-1$
      }
    }
  }

  /**
   * Message shown in the progress dialog like:<br>
   *        <code>Search   50 of   75 types. Markers   25. Exceptions  0. Class FinalHandler - 2013-04-11 23:48:04.420</code>
   */
  @SuppressWarnings("boxing")
  private String getProgress(Set<TypeContainer> typeContainers, int pos, TypeContainer container) {
    return String.format("Search %4s of %4s types. Markers %4s. Exceptions %2s. Class %s - %s", //$NON-NLS-1$
        pos, typeContainers.size(), markerCreated, searchProblems.size(),//
        JavaElementUtil.getTypeName(container.getType()), UCDInfo.getNow(true));
  }

  /**
   * Message shown in the progress dialog like:<br>
   *        <code>Found 2. Done 7/764. Detecting: Method Classname.methodName() - check override/implements</code>
   */
  private void updateMonitorMessage(IJavaElement element, String details, String searchInfo) {
    checkForCancel();
    String javaElement = JavaElementUtil.getElementName(element);
    Object[] bindings = new Object[] { Integer.valueOf(markerCreated), Integer.valueOf(search),
        Integer.valueOf(searchTotal), searchInfo, javaElement, details };
    String message = NLS.bind(Messages.SearchManager_Monitor, bindings);
    monitor.subTask(message);
  }

  /**
   * Search types
   */
  private void searchSpecific(IType type) throws CoreException {
    monitor.worked(1);
    String searchInfo = JavaElementUtil.getMemberTypeString(type);
    updateMonitorMessage(type, Messages.SearchManager_SearchReferences, searchInfo);
    StopWatch watch = new StopWatch(type);
    int found = searchImpl(type, searchInfo, false);
    watch.end("    Calculate reference marker"); //$NON-NLS-1$
    if (found == 0) {
      addNoRefTypes(type);
    }
  }

  private void addNoRefTypes(IType type) {
    Log.info("No references found for type: %s", JavaElementUtil.getElementName(type)); //$NON-NLS-1$
    noRefTypes.add(type);
  }

  /**
   * Search method
   */
  private void searchSpecific(IMethod method) throws CoreException {
    monitor.worked(1);
    IType type = JavaElementUtil.getTypeFor(method, false);
    if (type.isAnonymous()) {
      return;// Ignore anonymous types
    }
    if (noRefTypes.contains(type)) {
      return; // Ignore types, which have no references
    }
    if (JavaElementUtil.isMethodOfJavaLangObject(method)) {
      return; // Ignore methods from java.lang.Object
    }
    if (JavaElementUtil.isSerializationMethod(method)) {
      return; // Ignore serialization methods
    }
    int line = lineManger.getLine(method);
    if (line == LineManger.LINE_NOT_FOUND) {
      logIgnore("Ignore method " + method.getElementName()); //$NON-NLS-1$
      return;
    }
    String searchInfo = JavaElementUtil.getMemberTypeString(method);
    updateMonitorMessage(method, "override/implements", searchInfo); //$NON-NLS-1$

    // it is very expensive to call this method!!!
    StopWatch stop = new StopWatch(method);
    boolean isOverriddenMethod = JavaElementUtil.isOverriddenMethod(method);
    stop.end("    Calculate if is overridden method"); //$NON-NLS-1$

    StopWatch watch = new StopWatch(method);
    if (!isOverriddenMethod) {
      updateMonitorMessage(method, SEARCH_FINAL_MESSAGE, searchInfo);
      boolean created = finalHandler.createFinalMarker(method, line);
      watch.end("    Calculate method final marker"); //$NON-NLS-1$
      if (created) {
        markerCreated++;
      }
    }
    updateMonitorMessage(method, Messages.SearchManager_SearchReferences, searchInfo);
    searchImpl(method, searchInfo, isOverriddenMethod);
    watch.end("    searchImpl"); //$NON-NLS-1$
  }

  /**
   * Search field
   */
  private void searchSpecific(IField field) throws CoreException {
    monitor.worked(1);
    int line = lineManger.getLine(field);
    if (line == LineManger.LINE_NOT_FOUND) {
      logIgnore("Ignore field " + field.getElementName()); //$NON-NLS-1$
      return;
    }
    String searchInfo = JavaElementUtil.getMemberTypeString(field);
    updateMonitorMessage(field, SEARCH_FINAL_MESSAGE, searchInfo);
    StopWatch watch = new StopWatch(field);
    if (JavaElementUtil.isSerializationField(field)) {
      return;
    }
    // We create final markers even for classes which have no references
    boolean created = finalHandler.createFinalMarker(field, line);
    watch.end("    Calculate field final marker"); //$NON-NLS-1$
    if (created) {
      markerCreated++;
    }
    if (Flags.isPrivate(field.getFlags())) {
      return;
    }
    IType type = JavaElementUtil.getTypeFor(field, false);
    if (noRefTypes.contains(type)) {
      return;
    }
    if (type.isAnonymous()) {
      return; // Ignore anonymous classes
    }
    if (usedByValueEnumsCache.contains(type)) {
      return;// See bug 2900561: enum detection, or don't create "unnecessary marker" for enum constants
    }
    updateMonitorMessage(field, Messages.SearchManager_SearchReferences, searchInfo);
    int found = searchImpl(field, searchInfo, false);
    watch.end("    searchImpl"); //$NON-NLS-1$
    if (found > 0 && !hasReadAccess(field)) {
      String message = NLS.bind(Messages.MarkerFactory_MarkerReferenceFieldNeverRead,
          new Object[] { JavaElementUtil.getElementName(field) });
      // found=0 needed here, to create reference marker!
      markerFactory.createReferenceMarker(field, message, line, 0);
    }
  }

  /** Search lazy to avoid time consuming: JavaElementUtil.isUsedBySpecialEnumMethods() */
  private static final class UsedByValueEnumsCache {
    private final List<IType> alreadySearched = new ArrayList<IType>();
    private final List<IType> usedByValueEnums = new ArrayList<IType>();

    boolean contains(IType enumType) throws CoreException {
      if (enumType.isEnum() && !alreadySearched.contains(enumType)) {
        alreadySearched.add(enumType);
        if (JavaElementUtil.isUsedBySpecialEnumMethods(enumType)) {
          usedByValueEnums.add(enumType);
        }
      }
      return usedByValueEnums.contains(enumType);
    }
  }

  private static void logIgnore(String message) {
    if (Log.isDebug()) {
      Log.debug(message);
    }
  }

  private void checkForCancel() {
    monitor.throwIfIsCanceled();
  }

  public int getMarkerCreated() {
    return markerCreated;
  }

  /**
   * @return <code>true</code>, when a field has read access
   */
  private static boolean hasReadAccess(IField field) throws CoreException {
    SearchPattern pattern = SearchPattern.createPattern(field, IJavaSearchConstants.READ_ACCESSES);
    CountSearchRequestor requestor = new CountSearchRequestor();
    JavaElementUtil.runSearch(pattern, requestor);
    return requestor.isFound();
  }

  /**
    * Search for references create marker
    */
  private int searchImpl(IMember member, String searchInfo, boolean isOverriddenMethod) throws CoreException {
    int line = lineManger.getLine(member);
    checkForCancel();
    if (line == LineManger.LINE_NOT_FOUND) {
      return 0;
    }
    VisibilityHandler visibilityHandler = new VisibilityHandler(markerFactory, member);
    UCDSearchRequestor foundResult = searchJavaImpl(member, visibilityHandler);
    int found = foundResult.found;
    int foundInTextFiles = 0;
    boolean isTestOnlyMatches = found > 0 && (found == foundResult.foundTest);
    if (found == 0 || isTestOnlyMatches) {
      foundInTextFiles = searchTextImpl(member, visibilityHandler);
    }
    // System.out.println("found: " + found + " - " + foundResult.foundTest);
    boolean created = false;
    if (isTestOnlyMatches && foundInTextFiles == 0) {
      created = markerFactory.createReferenceMarkerTestOnly(member, line);
      if (created) {
        markerCreated++;
      }
    }
    found += foundInTextFiles;
    // Fix for BUG 1925549:  Exclude overridden methods from visibility detection
    if (!isOverriddenMethod) {
      created = visibilityHandler.createMarker(line, found);
      if (created) {
        markerCreated++;
      }
    }
    Object[] bindings = new Object[] { searchInfo, JavaElementUtil.getElementName(member), Integer.valueOf(found) };
    String markerMessage = NLS.bind(Messages.MarkerFactory_MarkerReference, bindings);
    if (member instanceof IMethod) {
      IMethod method = (IMethod) member;
      // [ 2743872 ] Don't check for constructors called only 1 time
      if (method.isConstructor() && found > 0) {
        return found;
      }
      // [ 2826216 ] Unused interface methods are not detected
      // [ 2225016 ] Don't create "0 references marker" for overridden methods
      boolean isInterfaceMethod = JavaElementUtil.isInterfaceMethod(method);
      if (!isInterfaceMethod && isOverriddenMethod) {
        return found;
      }
    }
    if (found > Prefs.getWarnLimit()) {
      return found;
    }
    // Fix for BUG 2808853: Don't create "0 references marker" for classes with main methods
    if (member instanceof IType && JavaElementUtil.hasMainMethod((IType) member)) {
      if (Prefs.isFilterClassWithMainMethod()) {
        Log.info("No marker, because class has main() method: %s", JavaElementUtil.getElementName(member)); //$NON-NLS-1$
        return found;
      }
    }
    // 2016-04-15: Don't create "0 references marker" for overridden methods
    if (!isOverriddenMethod) {
      created = markerFactory.createReferenceMarker(member, markerMessage, line, found);
    }
    if (created) {
      markerCreated++;
    }
    return found;
  }

  /**
   * Search for java references
   */
  private UCDSearchRequestor searchJavaImpl(IMember member, VisibilityHandler visibilityHandler) throws CoreException {
    checkForCancel();
    SearchPattern pattern = SearchPattern.createPattern(member, IJavaSearchConstants.REFERENCES);
    UCDSearchRequestor requestor = new UCDSearchRequestor(member, visibilityHandler, lineManger);
    boolean isSearchException = JavaElementUtil.runSearch(pattern, requestor);
    // Let's be pessimistic and handle an Exception as "reference found"!
    if (isSearchException && requestor.found == 0) {
      requestor.found = 1;
    }
    return requestor;
  }

  /**
   * Search in text files
   */
  private int searchTextImpl(IMember member, VisibilityHandler visibilityHandler) throws CoreException {
    checkForCancel();
    if (!Prefs.isUCDetectionInLiterals() || !(member instanceof IType)) {
      return 0;
    }
    // Only search if visibility is public
    if (!Flags.isPublic(member.getFlags())) {
      return 0;
    }
    IType type = (IType) member;
    // Classes declared in plugin.xml for example must be public!
    // Search class names in text file now works also for nested classes: com.example.Foo$NestedClass
    if (type.isAnonymous() || type.isLocal() || !Flags.isPublic(type.getFlags())) {
      return 0;
    }
    String searchInfo = JavaElementUtil.getMemberTypeString(member);

    updateMonitorMessage(type, Messages.SearchManager_SearchClassNameAsLiteral, searchInfo);
    FileTextSearchScope scope = FileTextSearchScope.newWorkspaceScope(Prefs.getFilePatternLiteralSearch(), /*exclude bin dir */
        false);
    List<String> searchStrings = new ArrayList<String>();
    if (Prefs.isUCDetectionInLiteralsFullClassName()) {
      String fullClassName = type.getFullyQualifiedName();
      searchStrings.add(fullClassName);
      Log.debug("Text search of full classname '%s'", fullClassName);// //$NON-NLS-1$
    }
    if (Prefs.isUCDetectionInLiteralsSimpleClassName()) {
      String simpleClassName = type.getElementName();
      searchStrings.add(simpleClassName);
      Log.debug("Text search of simple classname '%s'", simpleClassName);// //$NON-NLS-1$
    }
    int requestorFound = 0;
    for (String searchString : searchStrings) {
      if (searchString == null || searchString.length() == 0) {
        continue;
      }
      Pattern searchPattern = Pattern.compile(Pattern.quote(searchString));
      UCDFileSearchRequestor requestor = new UCDFileSearchRequestor(type, searchString, visibilityHandler);
      try {
        // 2011-06-28: When next line fails in headless mode,
        // restore class from svn: org.ucdetector.search.UCDTextSearchVisitor
        // TextSearchEngine.create().search(scope, requestor, searchPattern, null);
        // 2012-02-26: Used "createDefault()" to avoid Exception in headless mode
        TextSearchEngine.createDefault().search(scope, requestor, searchPattern, null);
      }
      catch (OperationCanceledException e) {
        Log.info("Text search canceled"); //$NON-NLS-1$
      }
      catch (OutOfMemoryError e) {
        UCDetectorPlugin.handleOutOfMemoryError(e);
      }
      // bug fix [ 2373808 ]: Classes found by text search should have no markers
      if (requestor.matchedFiles.size() > 0) {
        if (Log.isDebug()) {
          Log.debug("Matches found searching class name '%s' in text files: %s", searchString, requestor.matchedFiles); //$NON-NLS-1$
        }
        addNoRefTypes(type);
      }
      requestorFound += requestor.matchedFiles.size();
    }
    return requestorFound;
  }

  /**
   * text search in files
   */
  private static final class UCDFileSearchRequestor extends TextSearchRequestor {
    final List<String> matchedFiles = new ArrayList<String>();
    final VisibilityHandler visibilityHandler;
    final String searchString;
    private final IType startType;

    @Override
    public String toString() {
      return String.format("'%s' found=%s", searchString, Integer.valueOf(matchedFiles.size())); //$NON-NLS-1$
    }

    UCDFileSearchRequestor(IType startType, String searchString, VisibilityHandler visibilityHandler) {
      this.startType = startType;
      this.searchString = searchString;
      this.visibilityHandler = visibilityHandler;
    }

    /**
     * Search for className or packageName.className, check character
     * before and after match, if it is a JavaIdentifier
     */
    @SuppressWarnings("boxing")
    @Override
    public boolean acceptPatternMatch(TextSearchMatchAccess matchAccess) throws CoreException {
      char beforeChar = getCharBefore(matchAccess);
      char afterChar = getCharAfter(matchAccess);
      boolean isValidCharBefore = Character.isJavaIdentifierStart(beforeChar);
      boolean isValidCharAfter = Character.isJavaIdentifierPart(afterChar);
      boolean isClassNamMatchOk = !isValidCharBefore && !isValidCharAfter;
      if (DEBUG) {
        int offset = matchAccess.getMatchOffset();
        int length = matchAccess.getMatchLength();
        String match = matchAccess.getFileContent(offset, length);
        Log.debug("    TEXT MATCH {%s%s%s}. isMatchOk: %s. in: %s", //$NON-NLS-1$
            beforeChar, match, afterChar, isClassNamMatchOk, matchAccess.getFile());
      }
      if (isClassNamMatchOk) {
        matchedFiles.add(matchAccess.getFile().getFullPath().toString());
      }
      IJavaElement matchJavaElement = JavaCore.create(matchAccess.getFile());
      visibilityHandler.checkVisibility(matchJavaElement);
      checkCancelSearch(startType, matchedFiles.size(), -1, visibilityHandler);
      return true;
    }

    private static char getCharBefore(TextSearchMatchAccess match) {
      int offset = match.getMatchOffset();
      return (offset == 0) ? '\n' : match.getFileContentChar(offset - 1);
    }

    private static char getCharAfter(TextSearchMatchAccess match) {
      int offset = match.getMatchOffset();
      int length = match.getMatchLength();
      boolean fileEnd = (offset + length) >= match.getFileContentLength();
      return fileEnd ? '\n' : match.getFileContentChar(offset + length);
    }
  }

  /**
   * search java references
   */
  private static final class UCDSearchRequestor extends SearchRequestor {
    int found = 0;
    int foundTest = 0;
    private final IMember searchStart;
    private final VisibilityHandler visibilityHandler;
    @SuppressWarnings("unused")
    private final LineManger lineManager;

    @Override
    public String toString() {
      return String.format("%s found=%s, foundTest=%s", searchStart.getElementName(), Integer.valueOf(found), //$NON-NLS-1$
          Integer.valueOf(foundTest));
    }

    UCDSearchRequestor(IMember searchStart, VisibilityHandler visibilityHandler, LineManger lineManager) {
      this.searchStart = searchStart;
      this.visibilityHandler = visibilityHandler;
      this.lineManager = lineManager;
    }

    @Override
    public void acceptSearchMatch(SearchMatch match) {
      if (ignoreMatch(match)) {
        return;
      }
      this.found++;
      IJavaElement matchJavaElement = (IJavaElement) match.getElement();
      //      checkUnusedBoolean(match, matchJavaElement);
      if (Prefs.isDetectTestOnly() && JavaElementUtil.isTestCode(matchJavaElement)) {
        foundTest++;
      }
      visibilityHandler.checkVisibility(matchJavaElement);
      checkCancelSearch(searchStart, found, foundTest, visibilityHandler);
      //      parseMatch(match, matchJavaElement);
    }

    //TODO: request 2893808: Check for unnecessary (boolean) parameters
    //           Bug 2926266: Method cannot be private if called on subclass
    //    @SuppressWarnings("nls")
    //    private void parseMatch(SearchMatch match, IJavaElement matchElement) {
    //      if (match instanceof MethodReferenceMatch) {
    //        MethodReferenceMatch method = (MethodReferenceMatch) match;
    //        System.out.println("method: " + method);
    //        int offset = method.getOffset();
    //        try {
    //          String code = lineManager.getPieceOfCode(matchElement, offset);
    //          System.out.println("code: " + code);
    //          ASTParser parser = UCDetectorPlugin.newASTParser();
    //          ICompilationUnit compilationUnit = JavaElementUtil.getTypeFor(matchElement, true).getCompilationUnit();
    //          parser.setSource(compilationUnit); // compilationUnit needed for resolve bindings!
    //          parser.setKind(ASTParser.K_COMPILATION_UNIT);
    //          parser.setResolveBindings(true);
    //          ASTNode ast = parser.createAST(null);
    //          ASTVisitor visitor = new MethodInvocationVisitor(offset);
    //          ast.accept(visitor);
    //        }
    //        catch (Exception e) {
    //          e.printStackTrace();
    //        }
    //      }
    //    }
    //
    //    @SuppressWarnings("nls")
    //    private static final class MethodInvocationVisitor extends ASTVisitor {
    //      private final int offset;
    //
    //      private MethodInvocationVisitor(int offset) {
    //        this.offset = offset;
    //      }
    //
    //      @Override
    //      public boolean visit(MethodDeclaration declaration) {
    //        int start = declaration.getStartPosition();
    //        int length = declaration.getLength();
    //        boolean found = start < offset && offset <= (start + length);
    //        //        System.out.printf("found %s: %s%n", found, declaration.getName());
    //        return found;
    //      }
    //
    //      @Override
    //      public boolean visit(MethodInvocation node) {
    //        System.out.println("methodBinding: " + node.getName());
    //        System.out.println("MethodInvocation: " + node.toString());
    //        IMethodBinding methodBinding = node.resolveMethodBinding();
    //        System.out.println("methodBinding: " + methodBinding);
    //        IMethod methodFound = (IMethod) methodBinding.getJavaElement();
    //        if (methodFound.isBinary()) {
    //          return true;
    //        }
    //        // if (visitedMethods.contains(methodFound)) return true;
    //        System.out.println("methodFound: " + methodFound.getDeclaringType().getElementName());
    //        return true;
    //      }
    //    }

    /**
     * @return <code>true</code> if the found match should be ignored
     * Ignore matches:
     * <ul>
     * <li>matches in jars</li>
     * <li>matches caused by compile problems</li>
     * <li>no IJavaElement matches like javadoc</li>
     * <li>Ignore import, because it maybe an unnecessary import!</li>
     * <li>Ignore type matches referred by itself</li>
     * </ul>
     */
    private boolean ignoreMatch(SearchMatch match) {
      IJavaElement matchJavaElement = defaultIgnoreMatch(match);
      if (matchJavaElement == null) {
        return true;
      }
      // Ignore import, because it maybe an unnecessary import!
      // See OnlyImportDeclarationReferenceExample
      if (matchJavaElement instanceof IImportDeclaration) {
        IImportDeclaration importDecl = (IImportDeclaration) matchJavaElement;
        try {
          // Bug fix: Static imports are not recognized - ID: 2783734
          return !Flags.isStatic(importDecl.getFlags());
        }
        catch (JavaModelException ex) {
          Log.error("Can't get flags of: " + importDecl.getElementName(), ex); //$NON-NLS-1$
          return false;
        }
      }
      // Ignore type matches referred by itself
      // See UnusedClassUsedByItself, UsedByInnerClass
      if (searchStart instanceof IType) {
        IType searchStartType = (IType) searchStart;
        IType matchPrimaryType = JavaElementUtil.getTypeFor(matchJavaElement, false);
        if (matchPrimaryType.equals(searchStartType)) {
          return true;
        }
      }
      // Bug 2864967: Ignore references for recursive methods
      if (matchJavaElement.equals(searchStart)) {
        return true;
      }
      return false;
    }
  }

  /**
   * Check conditions from preferences and
   * cancel search by throwing a {@link OperationCanceledException}
   * when necessary
   */
  private static void checkCancelSearch(IMember startElement, int found, int foundTest,
      VisibilityHandler visibilityHandler) {
    if (Prefs.isDetectTestOnly() && (found == foundTest)) {
      return; // Continue searching, because all matches are matches in test code
    }
    if (found <= Prefs.getWarnLimit()) {
      return; // Continue searching to reach warn limit
    }
    if (Prefs.isVisibilityCheck(startElement) && !visibilityHandler.isMaxVisibilityFoundPublic()) {
      return; // Continue searching to find a match in another package
    }
    //    if (Log.isDebug()) {
    //      Log.debug("    Cancel search for: %s - Warn limit reached", JavaElementUtil.getElementName(startElement)); //$NON-NLS-1$
    //    }
    throw new OperationCanceledException("Cancel Search: Warn limit reached");//$NON-NLS-1$
  }

  /**
   * @param match search match, which should be checked
   * @return <code>true</code> if the found match should be ignored
   * Ignore matches:
   * <ul>
   * <li>matches in jars</li>
   * <li>matches caused by compile problems</li>
   * <li>no IJavaElement matches like javadoc</li>
   * </ul>
   */
  public static IJavaElement defaultIgnoreMatch(SearchMatch match) {
    // Ignore javadoc matches.
    // See JavaDocExample
    if (match.isInsideDocComment()) {
      return null;
    }
    // Ignore matches in jars, or matches caused by compile problems
    // See ReferenceInJarExample
    if (match.getAccuracy() == SearchMatch.A_INACCURATE) {
      return null;
    }
    Object matchElement = match.getElement();
    // Ignore no IJavaElement matches
    if (!(matchElement instanceof IJavaElement)) {
      return null;
    }
    return (IJavaElement) matchElement;
  }
}