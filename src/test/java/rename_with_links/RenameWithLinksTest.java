/**
 * Created by OpenCode on 2026-05-09 .
 */
package rename_with_links;

import application.AAppContext;
import dfile.file.FileUtilities;
import dfile.shortcut.IShortcutTargetUpdater;
import dfile.shortcut.IShortcutsTargetUpdater;
import dfile.shortcut.WinShortcutUpdater_PS_COM_WScript_Shell01;
import dfile.shortcut.WinShortcutsUpdater_PSScriptsMulti;
import dlog.log.Log;
import duser_input_output.AUserInputOutput;
import dutil.exception.UserRequestedTermination;
import dutil.exception.exceptions.MissingExternalValueException;
import dutil.exception.exceptions.MissingValueException;
import jakarta.validation.constraints.NotNull;
import mslinks.ShellLink;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.mockito.Mock;
import org.mockito.testng.MockitoTestNGListener;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.List;

import static application.AAppContext.MAX_VERBOSITY;
import static dfile.file.FileUtilities.checkIsExistingFile;
import static dfile.file.FileUtilities.checkIsExistingFolder;
import static dfile.file.FileUtilities.getCanonicalPath;
import static dlog.log.Log.writeLogsHeaders;
import static dutil.list.ListUtilities.assertContains;
import static dutil.list.text.TextListUtilities.listToString;
import static dutil.number.NumberUtilities.I;
import static dutil.number.NumberUtilities.TWO_I;
import static dutil.string.TextUtilities.NL;
import static dutil.string.TextUtilities.NLT;
import static dutil.string.TextUtilities.NLT2;
import static dutil.string.TextUtilities.S;
import static dutil.string.TextUtilities.TAB;
import static dutil.string.TextUtilities.dq;
import static dutil.system.OSUtilities.WIN_SHORTCUT_EXTENSION;
import static org.apache.commons.io.FilenameUtils.EXTENSION_SEPARATOR;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.testng.Assert.assertNotNull;


// @formatter:off


@Listeners(MockitoTestNGListener.class)
public class RenameWithLinksTest {
  
  
  private static final String APP_NAME =  RenameWithLinksTest.class.getSimpleName();
  
  private static final String APP_DESCR = RenameWithLinksTest.class.getName();
  
  @Mock
  private AUserInputOutput mockUserIO;

  private Log screenLog;

  private Log userLog;

  private Log devLog;

  private AAppContext mockAppContext;

  private Path tempDir;
  
  private IShortcutTargetUpdater shortcutTargetUpdater;
  
  private IShortcutsTargetUpdater shortcutsTargetUpdater;
  
  
  @BeforeClass
  void beforeClass() throws IOException {

    this.tempDir = Files.createTempDirectory(RenameWithLinksTest.class.getSimpleName() + "_");

    final Path logDir = Path.of("LOG");

    this.screenLog = new Log(APP_NAME + " - screen log",    logDir.resolve(APP_NAME + "_screen-log.LOG").toString(), true);

    this.userLog =   new Log(APP_NAME + " - user log",      logDir.resolve(APP_NAME + "_user-log.LOG").toString(),   true);

    this.devLog =    new Log(APP_NAME + " - developer log", logDir.resolve(APP_NAME + "_dev-log.LOG").toString(), true);
    
    writeLogsHeaders(this.screenLog, this.userLog, this.devLog, APP_NAME, APP_DESCR);
  }

  @AfterClass
  void afterClass() throws Exception {

    this.mockAppContext.close();

    FileUtils.deleteDirectory(this.tempDir.toFile());

    this.shortcutTargetUpdater = null;

    this.shortcutsTargetUpdater = null;
  }

  @BeforeMethod
  void beforeMethod() {

    lenient().when(this.mockUserIO.outChars(anyString())).thenAnswer(i -> i.getArgument(0));

    lenient().when(this.mockUserIO.warnChars(anyString())).thenAnswer(i -> i.getArgument(0));

    this.mockAppContext = AppContext.newAppContext(this.mockUserIO, this.screenLog, this.userLog, this.devLog);
    
    this.mockAppContext.currentVerbosity = MAX_VERBOSITY;
    
    this.shortcutTargetUpdater =  WinShortcutUpdater_PS_COM_WScript_Shell01.newInstance(false
                                                                                                          , null);
    
    this.shortcutsTargetUpdater = WinShortcutsUpdater_PSScriptsMulti.newInstance(this.mockAppContext);
  }
  
  @AfterMethod
  void afterMethod() {
    
    // Commented out because the tmpTestExecute01 folder must be deleted only if the test succeeds, to allow for its
    // inspection if the test fails. So this deletion is done by the test method itself if it completes.
    //
    //    deleteTmpTestExecute01Path();
    
  }
  
  // ====== Construction ======

  @Test
  void factoryReturnsNonNullInstance() {

    assertNotNull(RenameWithLinks.newInstance(this.mockAppContext));
  }

  @Test
  void factoryThrowsOnNullContext() {

    Assert.expectThrows(MissingValueException.class, () -> RenameWithLinks.newInstance(null));
  }

  // ====== Cancel scenarios (mock userIO only) ======

  @Test
  void cancelAtFirstPrompt() {

    // 5.1 : Check OS is Windows → passes
    // 5.2 : in() returns null (cancel)
    // → UserRequestedTermination

    lenient().when(this.mockUserIO.in(anyString(), anyString(), anyString()))
             .thenReturn(null);

    final RenameWithLinks app = RenameWithLinks.newInstance(this.mockAppContext);

    Assert.expectThrows(UserRequestedTermination.class, () -> app.run(this.shortcutsTargetUpdater));
  }

  @Test
  void cancelAtSecondPrompt() throws IOException {

    // 5.2 : existing path → temp file exists
    // 5.3 : in() returns null (cancel at new name)
    // → UserRequestedTermination

    final File existingFile = Files.createTempFile(this.tempDir, "cancel2_", ".txt").toFile();

    lenient().when(this.mockUserIO.in(anyString(), anyString(), anyString()))
             .thenReturn(getCanonicalPath(existingFile))
             .thenReturn(null);

    final RenameWithLinks app = RenameWithLinks.newInstance(this.mockAppContext);

    Assert.expectThrows(UserRequestedTermination.class
                           , () -> app.run(this.shortcutsTargetUpdater));
  }

  @Test
  void cancelAtThirdPrompt() throws IOException {

    // 5.2 : existing path → temp file
    // 5.3 : new name → any string
    // 5.4 : in() returns null (cancel at search path)
    // → UserRequestedTermination

    final File existingFile = Files.createTempFile(this.tempDir, "cancel3_", ".txt").toFile();

    lenient().when(this.mockUserIO.in(anyString(), anyString(), anyString()))
             .thenReturn(getCanonicalPath(existingFile))
             .thenReturn("renamed_file.txt")
             .thenReturn(null);

    final RenameWithLinks app = RenameWithLinks.newInstance(this.mockAppContext);

    Assert.expectThrows(UserRequestedTermination.class
                           , () -> app.run(this.shortcutsTargetUpdater));
  }

  // ====== Invalid input scenarios ======

  @Test
  void nonExistentSourcePath() {

    // 5.2 : in() returns a non-existent path
    // → MissingExternalValueException

    final String nonExistentPath = getCanonicalPath(new File(this.tempDir.toFile(), "does_not_exist.txt"));

    lenient().when(this.mockUserIO.in(anyString(), anyString(), anyString())).thenReturn(nonExistentPath);

    final RenameWithLinks app = RenameWithLinks.newInstance(this.mockAppContext);

    Assert.expectThrows(MissingExternalValueException.class, () -> app.run(this.shortcutsTargetUpdater));
  }

  @Test
  void searchPathIsFile() throws IOException {

    // 5.2 : existing path → temp dir
    // 5.3 : new name → any string
    // 5.4 : search dir → a file, not a directory
    // → InvalidPathException

    final File existingDir = Files.createTempDirectory(this.tempDir, "searchTest_").toFile();

    final File aFile = Files.createTempFile(this.tempDir, "notADir_", ".txt").toFile();

    lenient().when(this.mockUserIO.in(anyString(), anyString(), anyString()))
             .thenReturn(getCanonicalPath(existingDir))
             .thenReturn("renamed")
             .thenReturn(getCanonicalPath(aFile));

    final RenameWithLinks app = RenameWithLinks.newInstance(this.mockAppContext);

    Assert.expectThrows(InvalidPathException.class, () -> app.run(this.shortcutsTargetUpdater));
  }

  // ====== Integration: file system operations ======

  @Test
  void renameFile() throws Throwable {
    
    // Full happy path: rename a real file
    
    final File srcFile = Files.createTempFile(this.tempDir, "src_", ".txt").toFile();
    
    Files.writeString(srcFile.toPath(), "test content");
    
    final File searchDir = Files.createTempDirectory(this.tempDir, "lnkSearch_").toFile();
    
    final File destinationFileOrFolder = new File(this.tempDir.toFile(), "renamed_file.txt");
    
    final String verbosity = S(MAX_VERBOSITY);
    
    lenient().when(this.mockUserIO.in(anyString(), anyString(), anyString()))
             .thenReturn(getCanonicalPath(srcFile))
             .thenReturn(getCanonicalPath(destinationFileOrFolder))
             .thenReturn(getCanonicalPath(searchDir))
             .thenReturn(verbosity);
    
    final RenameWithLinks app = RenameWithLinks.newInstance(this.mockAppContext);
    
    app.run(this.shortcutsTargetUpdater);
    
    Assert.assertFalse(srcFile.exists(), "Source file should have been renamed");
    
    Assert.assertTrue(destinationFileOrFolder.exists(), "New file should exist");
    
    Assert.assertEquals(Files.readString(destinationFileOrFolder.toPath()), "test content");
  }

  @Test
  void renameFolder() throws Throwable {

    final File srcFolder = Files.createTempDirectory(this.tempDir, "srcFolder_").toFile();

    final File innerFile = new File(srcFolder, "inner.txt");

    Files.writeString(innerFile.toPath(), "inner content");

    final File searchDir = Files.createTempDirectory(this.tempDir, "lnkSearch2_").toFile();

    final File newFolder = new File(this.tempDir.toFile(), "renamed_folder");
    
    final String verbosity = S(MAX_VERBOSITY);
    
    lenient().when(this.mockUserIO.in(anyString(), anyString(), anyString()))
             .thenReturn(getCanonicalPath(srcFolder))
             .thenReturn(getCanonicalPath(newFolder))
             .thenReturn(getCanonicalPath(searchDir))
             .thenReturn(verbosity);

    final RenameWithLinks app = RenameWithLinks.newInstance(this.mockAppContext);

    app.run(this.shortcutsTargetUpdater);

    Assert.assertFalse(srcFolder.exists(), "Source folder should have been renamed");

    Assert.assertTrue(newFolder.exists(), "New folder should exist");

    Assert.assertTrue(newFolder.isDirectory());

    Assert.assertTrue(new File(newFolder, "inner.txt").exists());
  }

  @Test
  void createParentDirs() throws Throwable {

    // New path has a parent that does not exist yet

    final File srcFile = Files.createTempFile(this.tempDir, "srcParent_", ".txt").toFile();

    Files.writeString(srcFile.toPath(), "parent test");

    final File searchDir = Files.createTempDirectory(this.tempDir, "lnkSearch3_").toFile();

    final File destinationFileOrFolder = new File(this.tempDir.toFile(), "newParent/sub/renamed.txt");
    
    final String verbosity = S(MAX_VERBOSITY);
    
    lenient().when(this.mockUserIO.in(anyString(), anyString(), anyString()))
             .thenReturn(getCanonicalPath(srcFile))
             .thenReturn(getCanonicalPath(destinationFileOrFolder))
             .thenReturn(getCanonicalPath(searchDir))
             .thenReturn(verbosity);

    final RenameWithLinks app = RenameWithLinks.newInstance(this.mockAppContext);

    app.run(this.shortcutsTargetUpdater);

    Assert.assertFalse(srcFile.exists(), "Source file should have been renamed");

    Assert.assertTrue(destinationFileOrFolder.exists(), "New file should exist");

    Assert.assertEquals(Files.readString(destinationFileOrFolder.toPath()), "parent test");
  }
  
  @Test
  void matchingLinkUpdated() throws Throwable {

    // Create a .lnk that points to the source file, verify it's updated

    final File srcFile = Files.createTempFile(this.tempDir, "lnkSrc_", EXTENSION_SEPARATOR + "txt").toFile();

    Files.writeString(srcFile.toPath(), "lnk test");

    final File searchDir = Files.createTempDirectory(this.tempDir, "lnkSearch4_").toFile();

    final File lnkFile = new File(searchDir, "shortcut" + WIN_SHORTCUT_EXTENSION);

    ShellLink.createLink(getCanonicalPath(srcFile)).saveTo(getCanonicalPath(lnkFile));

    final File destinationFileOrFolder = new File(this.tempDir.toFile(), "lnk_renamed" + EXTENSION_SEPARATOR + "txt");
    
    final String verbosity = S(MAX_VERBOSITY);
    
    lenient().when(this.mockUserIO.in(anyString(), anyString(), anyString()))
             .thenReturn(getCanonicalPath(srcFile))
             .thenReturn(getCanonicalPath(destinationFileOrFolder))
             .thenReturn(getCanonicalPath(searchDir))
             .thenReturn(verbosity);

    final RenameWithLinks app = RenameWithLinks.newInstance(this.mockAppContext);

    app.run(this.shortcutsTargetUpdater);

    Assert.assertTrue(destinationFileOrFolder.exists(), "New file should exist");

    final ShellLink updatedLnk = new ShellLink(lnkFile);

    Assert.assertEquals(updatedLnk.resolveTarget(), getCanonicalPath(destinationFileOrFolder));
  }

  @Test
  void nonMatchingLinkUntouched() throws Throwable {

    // Create a .lnk pointing to a different file (not the one being renamed)

    final File srcFile = Files.createTempFile(this.tempDir, "otherSrc_", EXTENSION_SEPARATOR + "txt").toFile();

    Files.writeString(srcFile.toPath(), "other");

    final File otherFile = Files.createTempFile(this.tempDir, "otherTarget_", ".txt").toFile();

    final File searchDir = Files.createTempDirectory(this.tempDir, "lnkSearch5_").toFile();

    final File lnkFile = new File(searchDir, "other" + WIN_SHORTCUT_EXTENSION);

    ShellLink.createLink(getCanonicalPath(otherFile)).saveTo(getCanonicalPath(lnkFile));

    final File destinationFileOrFolder = new File(this.tempDir.toFile(), "other_renamed" + EXTENSION_SEPARATOR + "txt");
    
    final String verbosity = S(MAX_VERBOSITY);

    lenient().when(this.mockUserIO.in(anyString(), anyString(), anyString()))
             .thenReturn(getCanonicalPath(srcFile))
             .thenReturn(getCanonicalPath(destinationFileOrFolder))
             .thenReturn(getCanonicalPath(searchDir))
             .thenReturn(verbosity);

    final RenameWithLinks app = RenameWithLinks.newInstance(this.mockAppContext);

    app.run(this.shortcutsTargetUpdater);

    // The .lnk should still point to the other file

    final ShellLink unchangedLnk = new ShellLink(lnkFile);

    Assert.assertEquals(unchangedLnk.resolveTarget(), getCanonicalPath(otherFile));
  }

  @Test
  void corruptedLinkSkipped() throws Throwable {

    // A corrupted .lnk should be skipped with a warning

    final File originalFile = Files.createTempFile(this.tempDir, "corruptSrc_", EXTENSION_SEPARATOR + "txt").toFile();
    
    Files.writeString(originalFile.toPath(), "corrupt");
    
    final File searchFolder = Files.createTempDirectory(this.tempDir, "lnkSearch6_").toFile();
    
    final File corruptedLnk = new File(searchFolder, "bad" + WIN_SHORTCUT_EXTENSION);
    
    Files.writeString(corruptedLnk.toPath(), "this is not a valid shortcut");
    
    final File destinationFile = new File(this.tempDir.toFile(), "corrupt_renamed" + EXTENSION_SEPARATOR + "txt");
    
    final String verbosity = S(MAX_VERBOSITY);
    
    lenient().when(this.mockUserIO.in(anyString(), anyString(), anyString()))
             .thenReturn(getCanonicalPath(originalFile))
             .thenReturn(getCanonicalPath(destinationFile))
             .thenReturn(getCanonicalPath(searchFolder))
             .thenReturn(verbosity);
    
    final RenameWithLinks app = RenameWithLinks.newInstance(this.mockAppContext);
    
    app.run(this.shortcutsTargetUpdater);
    
    Assert.assertTrue(destinationFile.exists(), "Rename should complete despite corrupted " + WIN_SHORTCUT_EXTENSION);
  }
  
  /**
   * Tests {@link RenameWithLinks#execute(File, File, List, IShortcutsTargetUpdater)} as follows :<ul>
   *
   * <li> 1) Under {@code src\test\resources\} creates subfolders :<ol>
   *         <li>{@code testExecute01\sub1\sub2-to-move\sub3-with-file\sub4\}.</li>
   *         <li>{@code testExecute01\sub1\sub2a-to-stay\}.</li>
   *         <li>{@code testExecute01\subA\subBToReceiveMoved\}.</li></ol></li>
   *
   * <li> 2) Creates 2 files as follows :<ol>
   *         <li>In {@code sub3-with-file} folder creates file {@code PointedToAndToMove.txt}.</li>
   *         <li>In {@code sub2a-to-stay}  folder creates file {@code PointedToAndToStay.txt}.</li></ol>
   *
   *         <ul><li>So the tree under {@code src\test\resources\} will be :<pre>
   *                 | testExecute01\
   *                 | --- sub1\
   *                 | --- --- sub2-to-move\
   *                 | --- --- --- sub3-with-file\
   *                 | --- --- --- --- PointedToAndToMove.txt
   *                 | --- --- --- --- sub4\
   *                 | --- --- sub2a-to-stay\
   *                 | --- --- --- PointedToAndToStay.txt
   *                 | --- subA\
   *                 | --- --- subBToReceiveMoved\
   *                 | --- PointedToAndToMove.txt.lnk
   *                 | --- PointedToAndToStay.txt.lnk
   *                 | --- sub2-to-move.lnk
   *                 | --- sub3-with-file.lnk
   *                 | --- sub4.lnk</pre></li></ul></li>
   *
   * <li> 3) Verifies the 5 test prerequisites that :<ul>
   *
   *         <li>1) A shortcut named {@code PointedToAndToMove.txt.lnk} exists in {@code testExecute01\} having file {@code
   *                tmpTestExecute01\sub1\sub2-to-move\sub3-with-file\PointedToAndToMove.txt} as target.</li>
   *
   *         <li>2) A shortcut named {@code PointedToAndToStay.txt.lnk} exists in {@code testExecute01\} having file {@code
   *                tmpTestExecute01\sub1\sub2a-to-stay\PointedToAndToStay.txt} as target.</li>
   *
   *         <li>3) A shortcut named {@code sub2-to-move.lnk} exists in {@code testExecute01\} having folder {@code
   *                tmpTestExecute01\sub1\sub2-to-move} as target.</li>
   *
   *         <li>4) A shortcut named {@code sub3-with-file.lnk} exists in {@code testExecute01\} having folder {@code
   *                tmpTestExecute01\sub1\sub2-to-move\sub3-with-file} as target.</li>
   *
   *         <li>5) A shortcut named {@code sub4.lnk} exists in {@code testExecute01\} having folder {@code sub4} as
   *                target.</li></ul></li>
   *
   * <li> 4) Invokes {@link RenameWithLinks#execute(File, File, List, IShortcutsTargetUpdater) the tested method} passing :<ul>
   *
   *         <li>1) Folder {@code testExecute01\sub1\sub2-to-move\} as the file / folder to rename / move ({@code
   *                originalFileOrFolder} param).</li>
   *
   *         <li>2) Folder {@code testExecute01\subA\subBToReceiveMoved\sub2-moved\} as the destination file / folder of
   *                the rename / move ({@code destinationFileOrFolder} param), which is thus expected to get created.</li>
   *
   *         <li>3) Folder {@code testExecute01\} as the folder under which to recursively search for shortcuts having as
   *                target the absoulte path of file {@code
   *                testExecute01\sub1\sub2-to-move\sub3-with-file\PointedToAndToMove.txt} ({@code searchFolder} param).</li></ul>
   *
   *         <ul><li>Now the tree must have become :<pre>
   *                 | testExecute01\
   *                 | --- sub1\
   *                 | --- --- sub2a-to-stay\
   *                 | --- --- --- PointedToAndToStay.txt
   *                 | --- subA\
   *                 | --- --- subBToReceiveMoved\
   *                 | --- --- --- sub2-moved\
   *                 | --- --- --- --- sub3-with-file\
   *                 | --- --- --- --- --- PointedToAndToMove.txt
   *                 | --- --- --- --- --- sub4\
   *                 | --- PointedToAndToMove.txt.lnk
   *                 | --- PointedToAndToStay.txt.lnk
   *                 | --- sub2-to-move.lnk
   *                 | --- sub3-with-file.lnk
   *                 | --- sub4.lnk</pre></li></ul></li>
   *
   * <li> 5) Verifies that now the folder {@code sub2-to-move\} no longer exists under folder {@code testExecute01\sub1\}
   *         and now exists as {@code testExecute01\subA\subBToReceiveMoved\sub2-moved\} .</li>
   *
   * <li> 6) Verifies that folder {@code sub2a-to-stay\} still exists under {@code testExecute01\sub1\} and it still
   *         contains file "PointedToAndToStay.txt".</li>
   *
   * <li> 7) Verifies that folder {@code sub2-moved\sub3-with-file\} now exists under {@code testExecute01\subA\subBToReceiveMoved\} and
   *         contains file "PointedToAndToMove.txt".</li>
   *
   * <li> 8) Verifies that the 4 shortcuts that were originally pointing to moved / renamed files / folders now point to
   *         the new locations :<ul>
   *
   *         <li>1) Now the target of shortcut file {@code testExecute01\PointedToAndToMove.txt.lnk} has become the
   *                absoulte path of
   *                {@code testExecute01\subA\subBToReceiveMoved\sub2-moved\sub3-with-file\PointedToAndToMove.txt}.</li>
   *
   *         <li>2) Now the target of shortcut file {@code testExecute01\sub4.lnk} has become the absoulte path of {@code
   *                testExecute01\subA\subBToReceiveMoved\sub2-moved\sub3-with-file\sub4\}.</li>
   *
   *         <li>3) Now the target of shortcut file {@code testExecute01\sub3-with-file.lnk} has become the absoulte
   *                path of {@code testExecute01\subA\subBToReceiveMoved\sub2-moved\sub3-with-file\}.</li>
   *
   *         <li>4) Now the target of shortcut file {@code testExecute01\sub2-to-move.lnk} has become the absoulte path
   *                of {@code testExecute01\subA\subBToReceiveMoved\sub2-moved\}.</li></ul></li>
   *
   * <li> 9) Verifies that the target of shortcut file {@code testExecute01\PointedToAndToStay.txt.lnk} is still the
   *         absoulte path of {@code testExecute01\sub1\sub2a-to-stay\PointedToAndToStay.txt}.</li>
   *
   * <li>10) For cleanup, deletes the folders {@code sub1} and {@code subA} that were created under {@code testExecute01\}.</li></ul>
   */
  @Test
  void testExecute01() throws IOException, UserRequestedTermination {
    
    lenient().when(this.mockUserIO.in(anyString(), anyString(), anyString()))
             .thenReturn("y");
    
    // 1 : Under "src\test\resources\" create subfolders :
    //
    //     1 : "testExecute01\sub1\sub2-to-move\sub3-with-file\sub4\".
    //
    //     2 : "testExecute01\sub1\sub2a-to-stay\".
    //
    //     3 : "testExecute01\subA\subBToReceiveMoved\".
    
    final Path testExecute01 = createTmpTestExecute01Path();
    
    final String sTmp = checkIsExistingFolder(testExecute01.toFile());
    
    Assert.assertNull(sTmp, "Step 1 : Test prerequisite not satisfied : a folder named "
                                             + dq(testExecute01.getFileName().toString()) + NL
                                             + "must exist in folder "
                                             + dq(testExecute01.getParent().toAbsolutePath().toString()) + NL
                                             + "(" + sTmp + ").");
    
    final Path sub1 = Files.createDirectories(Path.of(testExecute01.toString()
                                                       , "sub1"));
    
    final Path sub2ToMove = Files.createDirectories(Path.of(sub1.toString()
                                                             , "sub2-to-move"));
    
    Path sub3WithFile = Files.createDirectories(Path.of(sub2ToMove.toString()
                                                         , "sub3-with-file"));
    
    final Path sub4 = Path.of(sub3WithFile.toString(), "sub4");
    
    Files.createDirectories(sub4);
    
    final Path sub2aToStay = Files.createDirectories(Path.of(sub1.toString()
                                                              , "sub2a-to-stay"));
    
    final Path subBToReceiveMoved = Files.createDirectories(Path.of(testExecute01.toString()
                                                        , "subA", "subB-to-receive-moved"));
    // 2 : Create 2 files as follows :
    //
    //   1 : In "sub3-with-file" folder create file "PointedToAndToMove.txt".
    //
    //   2 : In "sub2a-to-stay"  folder create file "PointedToAndToStay.txt".
    
    Path pointedToAndToMove = Files.createFile(Path.of(sub3WithFile.toString()
                                                         , "PointedToAndToMove.txt"));
    
    final Path pointedToAndToStayFile = Path.of(sub2aToStay.toString(), "PointedToAndToStay.txt");
    
    final Path pointedToAndToStay = Files.createFile(pointedToAndToStayFile);
    
    // 3 : Verify the 5 test prerequisites that :
    //
    //   3.1 : A shortcut named "PointedToAndToMove.txt.lnk" exists in "testExecute01\" having file
    //         "PointedToAndToMove.txt" as target :
    //
    
    final Path shortcutToFileToMove = Path.of(testExecute01.toString(), "PointedToAndToMove.txt.lnk");
    
    assertShortcutToPathExists(shortcutToFileToMove, pointedToAndToMove
          , "Step 3.1a : Test prerequisite not satisfied"
         , "Step 3.1b : Test prerequisite not satisfied");
    
    //
    //   3.2 : A shortcut named "PointedToAndToStay.txt.lnk" exists in "testExecute01\" having file
    //         "PointedToAndToStay.txt" as target :
    
    final Path shortcutToFileToStay = Path.of(testExecute01.toString(), "PointedToAndToStay.txt.lnk");
    
    assertShortcutToPathExists(shortcutToFileToStay, pointedToAndToStay
     , "Step 3.2a : Test prerequisite not satisfied"
    , "Step 3.2b : Test prerequisite not satisfied");
    
    //   3.3 : A shortcut named "sub2-to-move.lnk" exists in "testExecute01\" having folder
    //         "tmpTestExecute01\sub1\sub2-to-move" as target :
    
    final Path shortcutTo_sub2ToMove = Path.of(testExecute01.toString(), "sub2-to-move.lnk");
    
    assertShortcutToPathExists(shortcutTo_sub2ToMove, sub2ToMove
    , "Step 3.3a : Test prerequisite not satisfied"
    ,"Step 3.3b : Test prerequisite not satisfied: the shortcut file "
                               + dq(shortcutTo_sub2ToMove.toAbsolutePath().toString()) + NL
                               + "does not have the expected target.");
    
    //   3.4 : A shortcut named "sub3-with-file.lnk" exists in "testExecute01\" having folder
    //         "tmpTestExecute01\sub1\sub2-to-move\sub3-with-file" as target :
    
    final Path shortcutTo_sub3WithFile = Path.of(testExecute01.toString(), "sub3-with-file.lnk");
    
    assertShortcutToPathExists(shortcutTo_sub3WithFile, sub3WithFile
    , "Step 3.4a : Test prerequisite not satisfied"
    ,"Step 3.4b : Test prerequisite not satisfied: the shortcut file "
                              + dq(shortcutTo_sub3WithFile.toAbsolutePath().toString()) + NL
                              + "does not have the expected target.");
    
    //   3.5 : A shortcut named "sub4.lnk" exists in "testExecute01" having folder "sub4" as target :
    
    final Path shortcutTo_sub4 = Path.of(testExecute01.toString(), "sub4.lnk");
    
    assertShortcutToPathExists(shortcutTo_sub4, sub4
    , "Step 3.5a : Test prerequisite not satisfied"
    ,"Step 3.5b : Test prerequisite not satisfied: the shortcut file "
                              + dq(shortcutTo_sub4.toAbsolutePath().toString()) + NL
                              + "does not have the expected target.");
    
    // 4 : Invoke the tested method, to make it :
    //
    //    - make folder "testExecute01\sub1\sub2-to-move\" become "testExecute01\subA\subBToReceiveMoved\sub2-moved\";
    //
    //    - recursively search folder "testExecute01\" for shortcuts having as target the absoulte path of file
    //      "testExecute01\sub1\sub2-to-move\sub3-with-file\PointedToAndToMove.txt";
    //
    //    - find shortcut "testExecute01\PointedToAndToMove.txt.lnk";
    //
    //    - update its target,
    //      from "testExecute01\sub1\sub2-to-move\sub3-with-file\PointedToAndToMove.txt"
    //      to   "testExecute01\subA\subBToReceiveMoved\sub2-moved\sub3-with-file\PointedToAndToMove.txt"
    //  :
    
    final RenameWithLinks app = RenameWithLinks.newInstance(this.mockAppContext);
    
    app.askBeforeProcessingShortcuts = false;
    
    final Path subA =      Path.of(testExecute01.toString(), "subA");
    
    final Path sub2moved = Path.of(subA.toString(), "subB-to-receive-moved", "sub2-moved");
    
    app.execute(sub2ToMove.toFile(),sub2moved. toFile()
                     , testExecute01.toFile()                   , this.shortcutsTargetUpdater);
    
    // 5 : Verify that now the folder "sub2-to-move\" no longer exists under folder "testExecute01\sub1\" and now exists
    //     as "testExecute01\subA\subBToReceiveMoved\sub2-moved\" :
    
    Assert.assertFalse(sub2ToMove.toFile().exists()
                       , "Step 5a : The folder to move (" + dq(sub2ToMove.toString())
                                + ")" + NL + "still exists. That is wrong, it should have been moved to become "
                                + dq(sub2moved.toString()) + ".");
    
    Assert.assertTrue(sub2moved.toFile().exists()
                      , "Step 5b : The supposedly moved folder (" + dq(sub2moved.toString()) + ") does not exist." + NL
                               + "That is wrong, folder " + dq(sub2ToMove.toString()) + NL + " should have been moved to become that.");
    
    // 6 : Verify that folder "sub2a-to-stay" still exists under "testExecute01\sub1\" and it still contains file
    //     "PointedToAndToStay.txt" :
    
    Assert.assertTrue(sub2aToStay.toFile().exists()
                      , "Step 6a : The folder " + dq(sub2aToStay.toString()) + NL
                               + "no longer exists. That is wrong, it should have not been moved or deleted.");
    
    Assert.assertTrue(pointedToAndToStay.toFile().exists()
                      , "Step 6b : The folder " + dq(sub2aToStay.toString()) + NL
                               + "no longer contains file " + dq(sub2aToStay.toString()) + NL
                               + ". That is wrong, it should have not been moved or deleted.");
    
    // 7 : Verify that folder {@code sub2-moved\sub3-with-file\} now exists under {@code testExecute01\subA\subBToReceiveMoved\} and
    //     contains file "PointedToAndToMove.txt" :
    
    sub3WithFile = Path.of(subBToReceiveMoved.toString(), sub2moved.getFileName().toString()
                                                          , sub3WithFile.getFileName().toString());
    
    Assert.assertTrue(sub3WithFile.toFile().exists()
                      , "Step 7a : The folder " + dq(sub3WithFile.toString()) + NL
                               + "does not exist. That is wrong, the original "
                               + dq(sub2ToMove.toAbsolutePath().toString()) + " should have become it.");
    
    pointedToAndToMove = Path.of(sub3WithFile.toString(), pointedToAndToMove.getFileName().toString());
    
    Assert.assertTrue(pointedToAndToMove.toFile().exists()
                      , "Step 7b : The file " + dq(pointedToAndToMove.toString()) + NL
                               + "does not exist. That is wrong, it should have been moved to that folder when the original "
                               + dq(sub2ToMove.toAbsolutePath().toString()) + " became " + dq(sub2moved.toString()) + ".");
    
    // 8 : Verify that the 4 shortcuts that were originally pointing to moved / renamed files / folders now point to the
    //     new locations :
    
    //   8.1 : Verify that now the target of shortcut file "testExecute01\PointedToAndToMove.txt.lnk" has become the
    //         absoulte path of "testExecute01\subA\subBToReceiveMoved\sub2-moved\sub3-with-file\PointedToAndToMove.txt" :
    
    assertShortcutToPathExists(shortcutToFileToMove
              , Path.of(subBToReceiveMoved.toString(), "sub2-moved", "sub3-with-file"
                                            , "PointedToAndToMove.txt")
    , "Step 8.1a","Step 8.1b");
    
    //   8.2 : Verify that now the target of shortcut file "testExecute01\sub4.lnk" has become the absoulte path of
    //         "testExecute01\subA\subBToReceiveMoved\sub2-moved\sub3-with-file\sub4\" :
    
    assertShortcutToPathExists(shortcutTo_sub4
              , Path.of(subBToReceiveMoved.toString(), "sub2-moved", "sub3-with-file"
                                            , "sub4")
    , "Step 8.2a","Step 8.2b");
    
    //   8.3 : Verify that now the target of shortcut file "testExecute01\sub3-with-file.lnk" has become the absoulte
    //         path of "testExecute01\subA\subBToReceiveMoved\sub2-moved\sub3-with-file\" :
    
    assertShortcutToPathExists(shortcutTo_sub3WithFile
              , Path.of(subBToReceiveMoved.toString(), "sub2-moved", "sub3-with-file")
    , "Step 8.3a","Step 8.3b");
    
    //   8.4 : Verify that now the target of shortcut file "testExecute01\sub2-to-move.lnk" has become the absoulte path
    //         of "testExecute01\subA\subBToReceiveMoved\sub2-moved\" :
    
    assertShortcutToPathExists(shortcutTo_sub2ToMove
              , Path.of(subBToReceiveMoved.toString(), "sub2-moved")
    , "Step 8.4a","Step 8.4b");
    
    // 9 : Verify that the target of shortcut file "testExecute01\PointedToAndToStay.txt.lnk" is still the absoulte path
    //     of "testExecute01\sub1\sub2a-to-stay\PointedToAndToStay.txt".
    
    assertShortcutToPathExists(shortcutToFileToStay, pointedToAndToStayFile
    , "Step 9a","Step 9b");
    
    // 10 : Cleanup :
    
    final List<File> deleted = FileUtilities.deleteAllFolders(testExecute01.toFile());

    Assert.assertEquals(I(deleted.size()), TWO_I
                     , "Step 10 : Error during test cleanup : the two folders that must have been created and now deleted are :"
                                + NLT + dq(sub1.toString()) + NL + "and"
                                + NLT + dq(subA.toString()) + "."
                                + NL  + "Instead, the following " + deleted.size() + " folders were deleted :"
                                + NLT + listToString(deleted, EMPTY, TAB, EMPTY, NLT2));

    assertContains(deleted, sub1.toFile(), "Step 10a : " + dq(sub1.toString()) + " was not among the " + deleted.size() + " folders deleted during test cleanup.");

    assertContains(deleted, subA.toFile(), "Step 10b : " + dq(subA.toString()) + " was not among the " + deleted.size() + " folders deleted during test cleanup.");
    
    // The tmpTestExecute01 folder must be deleted only if the test succeeds, to allow for its inspection if the test
    // fails. So this deletion is done by the test method itself if it completes, instead of in and after* medhod of the
    // test cycle :
    
    deleteTmpTestExecute01Path();
  }
  
  /**
   * Assert that the given {@code shortcut} exists on the filesystem and has the given {@code expectedTarget}.
   *
   * @param shortcut                   The shortcut {@link File#isFile() file} to assert on.<br>
   *
   * @param expectedTarget             The {@link IShortcutTargetUpdater#readTarget target} that the given {@code
   *                                   shortcut} must have for the assertion to succeed. Its {@link Path#toAbsolutePath()
   *                                   absolute form} will be used.<br>
   *
   * @param prefixMsgShortcutNotFound  The prefix for the message to show if the assertion that the given {@code
   *                                   shortcut} exists fails. May be {@code null} or {@link StringUtils#isEmpty empty}.<br>
   *
   * @param prefixMsgTargetNotMatching The prefix for the message to show if the assertion that the given {@code
   *                                   shortcut} has the {@code expectedTarget}  May be {@code null} or {@link StringUtils#isEmpty
   *                                   empty}.
   */
  private void assertShortcutToPathExists(@NotNull Path   shortcut,                 @NotNull Path expectedTarget
                                                 , String prefixMsgShortcutNotFound
                                                 , String prefixMsgTargetNotMatching) {
    
    String sTmp = checkIsExistingFile(shortcut.toFile());
    
    Assert.assertNull(sTmp, prefixMsgShortcutNotFound + " : A shortcut file named "
                                             + dq(shortcut.getFileName().toString()) + NL + "must exist in folder "
                                             + dq(shortcut.getParent().toAbsolutePath().toString())
                                             + NL + "(" + sTmp + ").");
    
    sTmp = this.shortcutTargetUpdater.readTarget(shortcut.toFile());
    
    Assert.assertEquals(sTmp, expectedTarget.toAbsolutePath().toString()
                                     , prefixMsgTargetNotMatching + " : The shortcut file "
                                                + dq(shortcut.toAbsolutePath().toString()) + NL
                                                + "does not have the expected target.");
  }
  
  /**
   * @return The path of folder {@code src\test\resources\testExecute01\}, which is used by some tests and must exist (and
   *         have no <u>subfolders</u> in it) when such tests start. It is not used directly, a {@link #getTmpTestExecute01Path ()
   *         copy of it} {@link #createTmpTestExecute01Path() is made} and the tests work on the copy, which is then {@link #deleteTmpTestExecute01Path ()
   *         deleted} after the test.
   */
  private static Path getTestExecute01Path() {
    
    return Path.of("src", "test", "resources", "testExecute01");
  }
  
  /**
   * @return The path of folder {@code tmpTestExecute01\}, which is used by some tests. It is in {@link #getTestExecute01Path ()
   * the same folder} as the {@code src\test\resources\testExecute01\} folder.
   */
  private static Path getTmpTestExecute01Path() {
    
    return Path.of(getTestExecute01Path().getParent().toString(), "tmpTestExecute01");
  }
  
  /**
   * Creates the {@link #getTmpTestExecute01Path () tmpTestExecute01\} folder by making a copy of {@link #getTestExecute01Path ()
   * the original} in the same folder. If the folder to create already exists, deletes it first.
   *
   * @return The path of the created folder.
   */
  private static Path createTmpTestExecute01Path() throws IOException {
    
    final Path destPath = deleteTmpTestExecute01Path();
    
    FileUtils.copyDirectory(getTestExecute01Path().toFile(), destPath.toFile(), false);
    
    return destPath;
  }
  
  /**
   * Deletes the {@link #getTmpTestExecute01Path() tmpTestExecute01\} folder if it exists.
   */
  private static Path deleteTmpTestExecute01Path() throws IOException {
    
    final Path folder = getTmpTestExecute01Path();
    
    try {
      
      FileUtils.deleteDirectory(folder.toFile());
    }
    catch (IOException e) {
      
      folder.toFile().deleteOnExit();
      
      throw e;
    }
    return folder;
  }
  
}
