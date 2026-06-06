/**
 * Created by OpenCode on 2026-05-09 .
 */
package rename_with_links;

import dfile.file.FileUtilities;
import dfile.shortcut.IShortcutTargetUpdater;
import dfile.shortcut.WinShortcutUpdater_PS_COM_WScript_Shell01;
import dlog.log.Log;
import duser_input_output.AUserInputOutput;
import dutil.exception.UserRequestedTermination;
import dutil.exception.exceptions.MissingExternalValueException;
import dutil.exception.exceptions.MissingValueException;
import mslinks.ShellLink;
import org.apache.commons.io.FileUtils;
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

import static dfile.file.FileUtilities.checkIsExistingFile;
import static dfile.file.FileUtilities.checkIsExistingFolder;
import static dfile.file.FileUtilities.getCanonicalPath;
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
import static rename_with_links.RenameWithLinksMain.MAX_VERBOSITY;


// @formatter:off


@Listeners(MockitoTestNGListener.class)
public class RenameWithLinksTest {

  
  @Mock
  private AUserInputOutput mockUserIO;

  @Mock
  private Log mockScreenLog;

  @Mock
  private Log mockUserLog;

  @Mock
  private Log mockDevLog;

  private AppContext mockAppContext;

  private Path tempDir;
  
  private IShortcutTargetUpdater shortcutTargetUpdater;
  

  @BeforeClass
  void beforeClass() throws IOException {

    this.tempDir = Files.createTempDirectory("RenameWithLinksTest_");
    
    this.shortcutTargetUpdater = WinShortcutUpdater_PS_COM_WScript_Shell01.newInstance(false
                                                                                                         , null);
  }

  @AfterClass
  void afterClass() throws IOException {

    FileUtils.deleteDirectory(this.tempDir.toFile());
    
    this.shortcutTargetUpdater = null;
  }

  @BeforeMethod
  void beforeMethod() {

    lenient().when(this.mockUserIO.outChars(anyString())).thenAnswer(i -> i.getArgument(0));

    lenient().when(this.mockUserIO.warnChars(anyString())).thenAnswer(i -> i.getArgument(0));

    this.mockAppContext = AppContext.newAppContext(this.mockUserIO, this.mockScreenLog, this.mockUserLog, this.mockDevLog);
    
    this.mockAppContext.currentVerbosity = MAX_VERBOSITY;
  }
  
  @AfterMethod
  void afterMethod() {
    
    // Commented out because the tmptestExecute folder must be deleted only if the test succeeds, to allow for its
    // inspection if the test fails. So this deletion is done by the test method itself if it completes.
    //
    //    deleteTmpTestExecutePath();
    
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

    lenient().when(this.mockUserIO.in(anyString(), anyString(), anyString())).thenReturn(null);

    final RenameWithLinks app = RenameWithLinks.newInstance(this.mockAppContext);

    Assert.expectThrows(UserRequestedTermination.class, () -> app.run(this.shortcutTargetUpdater));
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

    Assert.expectThrows(UserRequestedTermination.class, () -> app.run(this.shortcutTargetUpdater));
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

    Assert.expectThrows(UserRequestedTermination.class, () -> app.run(this.shortcutTargetUpdater));
  }

  // ====== Invalid input scenarios ======

  @Test
  void nonExistentSourcePath() {

    // 5.2 : in() returns a non-existent path
    // → MissingExternalValueException

    final String nonExistentPath = getCanonicalPath(new File(this.tempDir.toFile(), "does_not_exist.txt"));

    lenient().when(this.mockUserIO.in(anyString(), anyString(), anyString())).thenReturn(nonExistentPath);

    final RenameWithLinks app = RenameWithLinks.newInstance(this.mockAppContext);

    Assert.expectThrows(MissingExternalValueException.class, () -> app.run(this.shortcutTargetUpdater));
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

    Assert.expectThrows(InvalidPathException.class, () -> app.run(this.shortcutTargetUpdater));
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
    
    app.run(this.shortcutTargetUpdater);
    
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

    app.run(this.shortcutTargetUpdater);

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

    app.run(this.shortcutTargetUpdater);

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

    app.run(this.shortcutTargetUpdater);

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

    app.run(this.shortcutTargetUpdater);

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
    
    app.run(this.shortcutTargetUpdater);
    
    Assert.assertTrue(destinationFile.exists(), "Rename should complete despite corrupted " + WIN_SHORTCUT_EXTENSION);
  }
  
  /**
   * Tests {@link RenameWithLinks#execute(File, File, File, IShortcutTargetUpdater)} as follows :<ul>
   *
   * <li> 1) Under {@code src\test\resources\} creates subfolders :<ol>
   *         <li>{@code testExecute\sub1\sub2-to-move\sub3-with-file\sub4\}.</li>
   *         <li>{@code testExecute\sub1\sub2a-to-stay\}.</li>
   *         <li>{@code testExecute\subA\subB\}.</li></ol></li>
   *
   * <li> 2) Creates 2 files as follows :<ol>
   *         <li>In {@code sub3-with-file} folder creates file {@code PointedToAndToMove.txt}.</li>
   *         <li>In {@code sub2a-to-stay}  folder creates file {@code PointedToAndToStay.txt}.</li></ol>
   *
   *         <ul><li>So the tree under {@code src\test\resources\} will be :<pre>
   *                 | textExecute\
   *                 | --- sub1\
   *                 | --- --- sub2-to-move\
   *                 | --- --- --- sub3-with-file\
   *                 | --- --- --- --- PointedToAndToMove.txt
   *                 | --- --- --- --- sub4\
   *                 | --- --- sub2a-to-stay\
   *                 | --- --- --- PointedToAndToStay.txt
   *                 | --- subA\
   *                 | --- --- subB\
   *                 | --- PointedToAndToMove.txt.lnk
   *                 | --- PointedToAndToStay.txt.lnk</pre></li></ul></li>
   *
   * <li> 3) Verifies the 2 test prerequisites that :<ol>
   *         <li>A shortcut named {@code PointedToAndToMove.txt.lnk} exists in {@code src\test\resources\textExecute\}
   *             having file {@code PointedToAndToMove.txt} as target.</li>
   *         <li>A shortcut named {@code PointedToAndToStay.txt.lnk} exists in {@code src\test\resources\textExecute\}
   *             having file {@code PointedToAndToStay.txt} as target.</li></ol></li>
   *
   * <li> 4) Invokes {@link RenameWithLinks#execute(File, File, File, IShortcutTargetUpdater) the tested method} passing :<ol>
   *         <li>Folder {@code testExecute\sub1\sub2-to-move\} as the file / folder to rename / move ({@code
   *             originalFileOrFolder} param).</li>
   *         <li>Folder {@code testExecute\subA\subB\sub2-moved\} as the destination file / folder of the rename / move ({@code
   *             destinationFileOrFolder} param), which is thus expected to get created.</li>
   *         <li>Folder {@code testExecute\} as the folder under which to recursively search for shortcuts having as target
   *             the absoulte path of file {@code
   *             testExecute\sub1\sub2-to-move\sub3-with-file\PointedToAndToMove.txt} ({@code searchFolder} param).</li></ol>
   *         <ul><li>Now the tree must have become :<pre>
   *                 | textExecute\
   *                 | --- sub1\
   *                 | --- --- sub2a-to-stay\
   *                 | --- --- --- PointedToAndToStay.txt
   *                 | --- subA\
   *                 | --- --- subB\
   *                 | --- --- --- sub2-moved\
   *                 | --- --- --- --- sub3-with-file\
   *                 | --- --- --- --- --- PointedToAndToMove.txt
   *                 | --- --- --- --- --- sub4\
   *                 | --- PointedToAndToMove.txt.lnk
   *                 | --- PointedToAndToStay.txt.lnk</pre></li></ul></li>
   *
   * <li> 5) Verifies that now the folder {@code sub2-to-move\} no longer exists under folder {@code testExecute\sub1\}
   *         and now exists as {@code testExecute\subA\subB\sub2-moved\} .</li>
   *
   * <li> 6) Verifies that folder {@code sub2a-to-stay\} still exists under {@code textExecute\sub1\} and it still
   *         contains file "PointedToAndToStay.txt".</li>
   *
   * <li> 7) Verifies that folder {@code sub2-moved\sub3-with-file\} now exists under {@code textExecute\subA\subB\} and
   *         contains file "PointedToAndToMove.txt".</li>
   *
   * <li> 8) Verifies that now the target of shortcut file {@code textExecute\PointedToAndToMove.txt.lnk} has become the
   *         absoulte path of {@code testExecute\subA\subB\sub2-moved\sub3-with-file\PointedToAndToMove.txt}.</li>
   *
   * <li> 9) Verifies that the target of shortcut file {@code textExecute\PointedToAndToStay.txt.lnk} is still the
   *         absoulte path of {@code testExecute\sub1\sub2a-to-stay\PointedToAndToStay.txt}.</li>
   *
   * <li>10) For cleanup, deletes the folders {@code sub1} and {@code subA} that were created under {@code testExecute\}.</li></ul>
   */
  @Test
  void testExecute01() throws IOException {
    
    lenient().when(this.mockUserIO.in(anyString(), anyString(), anyString()))
             .thenReturn("y");
    
    String sTmp;
    
    // 1 : Under "src\test\resources\" create subfolders :
    //
    //     1 : "testExecute\sub1\sub2-to-move\sub3-with-file\sub4\".
    //
    //     2 : "testExecute\sub1\sub2a-to-stay\".
    //
    //     3 : "testExecute\subA\subB\".
    
    final Path testExecute = createTmpTestExecutePath();
    
    sTmp = checkIsExistingFolder(testExecute.toFile());
    
    Assert.assertNull(sTmp, "Test prerequisite not satisfied : a folder named "
                                             + dq(testExecute.getFileName().toString())
                                             + " must exist in folder "
                                             + dq(testExecute.getParent().toAbsolutePath().toString())
                                             + " (" + sTmp + ").");
    
    final Path sub1 = Files.createDirectories(Path.of(testExecute.toString()
                                                       , "sub1"));
    
    final Path sub2ToMove = Files.createDirectories(Path.of(sub1.toString()
                                                             , "sub2-to-move"));
    
    Path sub3WithFile = Files.createDirectories(Path.of(sub2ToMove.toString()
                                                         , "sub3-with-file"));
    
    Files.createDirectories(Path.of(sub3WithFile.toString(), "sub4"));
    
    final Path sub2aToStay = Files.createDirectories(Path.of(sub1.toString()
                                                              , "sub2a-to-stay"));
    
    final Path subB = Files.createDirectories(Path.of(testExecute.toString()
                                                        , "subA", "subB"));
    // 2 : Create 2 files as follows :
    //
    //   1 : In "sub3-with-file" folder create file "PointedToAndToMove.txt".
    //
    //   2 : In "sub2a-to-stay"  folder create file "PointedToAndToStay.txt".
    
    Path pointedToAndToMove = Files.createFile(Path.of(sub3WithFile.toString()
                                                         , "PointedToAndToMove.txt"));
    
    final Path pointedToAndToStay = Files.createFile(Path.of(sub2aToStay.toString()
                                                               , "PointedToAndToStay.txt"));
    // 3 : Verify the 2 test prerequisites that :
    //
    //   3.1 : A shortcut named "PointedToAndToMove.txt.lnk" exists in "textExecute\" having file "PointedToAndToMove.txt"
    //         as target :
    //
    
    final Path shortcutToFileToMove = Path.of(testExecute.toString(), "PointedToAndToMove.txt.lnk");
    
    sTmp = checkIsExistingFile(shortcutToFileToMove.toFile());
    
    Assert.assertNull(sTmp, "Test prerequisite not satisfied : a shortcut file named "
                                           + dq(shortcutToFileToMove.getFileName().toString()) + " must exist in folder "
                                           + dq(shortcutToFileToMove.getParent().toAbsolutePath().toString()) + " (" + sTmp + ").");
    
    sTmp = this.shortcutTargetUpdater.readTarget(shortcutToFileToMove.toFile());
    
    Assert.assertEquals(sTmp, pointedToAndToMove.toAbsolutePath().toString()
                    , "Test prerequisite not satisfied: the shortcut file " + dq(shortcutToFileToMove.toAbsolutePath().toString())
                             + " does not have the expected target.");
    //
    //   3.2 : A shortcut named "PointedToAndToStay.txt.lnk" exists in "textExecute\" having file "PointedToAndToStay.txt"
    //         as target :
    
    final Path shortcutToFileToStay = Path.of(testExecute.toString(), "PointedToAndToStay.txt.lnk");
    
    sTmp = checkIsExistingFile(shortcutToFileToStay.toFile());
    
    Assert.assertNull(sTmp, "Test prerequisite not satisfied : a shortcut file named "
                                           + dq(shortcutToFileToStay.getFileName().toString())
                                           + " must exist in folder " + dq(shortcutToFileToStay.getParent().toAbsolutePath().toString())
                                           + " (" + sTmp + ").");
    
    sTmp = this.shortcutTargetUpdater.readTarget(shortcutToFileToStay.toFile());
    
    Assert.assertEquals(sTmp, pointedToAndToStay.toAbsolutePath().toString()
                     , "Test prerequisite not satisfied: the shortcut file " + dq(shortcutToFileToStay.toAbsolutePath().toString())
                              + " does not have the expected target.");
    
    // 4 : Invoke the tested method, to make it :
    //
    //    - make folder "testExecute\sub1\sub2-to-move\" become "testExecute\subA\subB\sub2-moved\";
    //
    //    - recursively search folder "testExecute\" for shortcuts having as target the absoulte path of file
    //      "testExecute\sub1\sub2-to-move\sub3-with-file\PointedToAndToMove.txt";
    //
    //    - find shortcut "testExecute\PointedToAndToMove.txt.lnk";
    //
    //    - update its target,
    //      from "testExecute\sub1\sub2-to-move\sub3-with-file\PointedToAndToMove.txt"
    //      to   "textExecute\subA\subB\sub2-moved\sub3-with-file\PointedToAndToMove.txt"
    //  :
    
    final RenameWithLinks app = RenameWithLinks.newInstance(this.mockAppContext);
    
    app.askBeforeProcessingShortcuts = false;
    
    final Path subA =      Path.of(testExecute.toString(), "subA");
    
    final Path sub2moved = Path.of(subA.toString(), "subB", "sub2-moved");
    
    app.execute(sub2ToMove. toFile(),sub2moved.toFile()
                     , testExecute.toFile(),                       this.shortcutTargetUpdater);
    
    // 5 : Verify that now the folder "sub2-to-move\" no longer exists under folder "testExecute\sub1\" and now exists
    //     as "testExecute\subA\subB\sub2-moved\" :
    
    Assert.assertFalse(sub2ToMove.toFile().exists()
                       , "The folder to move (" + dq(sub2ToMove.toString())
                                + ") still exists. That is wrong, it should have been moved to become "
                                + dq(sub2moved.toString()) + ".");
    
    Assert.assertTrue(sub2moved.toFile().exists()
                      , "The supposedly moved folder (" + dq(sub2moved.toString())
                               + ") does not exist. That is wrong, folder " + dq(sub2ToMove.toString())
                               + " should have been moved to become that.");
    
    // 6 : Verify that folder "sub2a-to-stay" still exists under "textExecute\sub1\" and it still contains file
    //     "PointedToAndToStay.txt" :
    
    Assert.assertTrue(sub2aToStay.toFile().exists()
                      , "The folder " + dq(sub2aToStay.toString())
                               + " no longer exists. That is wrong, it should have not been moved or deleted.");
    
    Assert.assertTrue(pointedToAndToStay.toFile().exists()
                      , "The folder " + dq(sub2aToStay.toString())
                               + " no longer contains file " + dq(sub2aToStay.toString())
                               + ". That is wrong, it should have not been moved or deleted.");
    
    // 7 : Verify that folder {@code sub2-moved\sub3-with-file\} now exists under {@code textExecute\subA\subB\} and
    //     contains file "PointedToAndToMove.txt" :
    
    sub3WithFile = Path.of(subB.toString(), sub2moved.getFileName().toString()
                                                          , sub3WithFile.getFileName().toString());
    
    Assert.assertTrue(sub3WithFile.toFile().exists()
                      , "The folder " + dq(sub3WithFile.toString())
                               + " does not exist. That is wrong, the original "
                               + dq(sub2ToMove.toAbsolutePath().toString()) + " should have become it.");
    
    pointedToAndToMove = Path.of(sub3WithFile.toString(), pointedToAndToMove.getFileName().toString());
    
    Assert.assertTrue(pointedToAndToMove.toFile().exists()
                      , "The file " + dq(pointedToAndToMove.toString())
                               + " does not exist. That is wrong, it should have been moved to that folder when the original "
                               + dq(sub2ToMove.toAbsolutePath().toString()) + " became " + dq(sub2moved.toString()) + ".");
    
    // 8 : Verify that now the target of shortcut file "textExecute\PointedToAndToMove.txt.lnk" has become the absoulte
    //     path of "testExecute\subA\subB\sub2-moved\sub3-with-file\PointedToAndToMove.txt" :
    
    sTmp = this.shortcutTargetUpdater.readTarget(shortcutToFileToMove.toFile());
    
    Assert.assertEquals(sTmp, Path.of(subB.toString()
                                                    , "sub2-moved", "sub3-with-file", "PointedToAndToMove.txt").toAbsolutePath().toString()
                     , "The shortcut file " + dq(shortcutToFileToMove.toAbsolutePath().toString())
                              + " does not have the expected target.");
    
    // 9 : Verify that the target of shortcut file "textExecute\PointedToAndToStay.txt.lnk" is still the absoulte path
    //     of "testExecute\sub1\sub2a-to-stay\PointedToAndToStay.txt".
    
    sTmp = this.shortcutTargetUpdater.readTarget(shortcutToFileToStay.toFile());
    
    Assert.assertEquals(sTmp, Path.of(testExecute.toString(), "PointedToAndToStay.txt").toAbsolutePath().toString()
                                                    , "The shortcut file " + dq(shortcutToFileToStay.toAbsolutePath().toString())
                                                             + " does not have the expected target.");
    // 10 : Cleanup :
    
    final List<File> deleted = FileUtilities.deleteAllFolders(testExecute.toFile());
    
    Assert.assertEquals(I(deleted.size()), TWO_I
                     , "Error during test cleanup : the two folders that must have been created and now deleted are :"
                                + NLT + dq(sub1.toString()) + NL + "and"
                                + NLT + dq(subA.toString()) + "."
                                + NL  + "Instead, the following " + deleted.size() + " folders were deleted :"
                                + NLT + listToString(deleted, EMPTY, TAB, EMPTY, NLT2));
    
    assertContains(deleted, sub1.toFile(), dq(sub1.toString()) + " was not among the " + deleted.size() + " folders deleted during test cleanup.");
    
    // The tmptestExecute folder must be deleted only if the test succeeds, to allow for its inspection if the test
    // fails. So this deletion is done by the test method itself if it completes, instead of in and after* medhod of
    // the test cycle.
    
    deleteTmpTestExecutePath();
  }
  
  /**
   * @return The path of folder {@code src\test\resources\testExecute\}, which is used by some tests and must exist (and
   *         have no <u>subfolders</u> in it) when such tests start. It is not used directly, a {@link #getTmpTestExecutePath()
   *         copy of it} {@link #createTmpTestExecutePath() is made} and the tests work on the copy, which is then {@link #deleteTmpTestExecutePath()
   *         deleted} after the test.
   */
  private static Path getTestExecutePath() {
    
    return Path.of("src", "test", "resources", "testExecute");
  }
  
  /**
   * @return The path of folder {@code tmpTestExecute\}, which is used by some tests. It is in {@link #getTestExecutePath()
   * the same folder} as the {@code src\test\resources\testExecute\} folder.
   */
  private static Path getTmpTestExecutePath() {
    
    return Path.of(getTestExecutePath().getParent().toString(), "tmpTestExecute");
  }
  
  /**
   * Creates the {@link #getTmpTestExecutePath() tmpTestExecute\} folder by making a copy of {@link #getTestExecutePath()
   * the original} in the same folder. If the folder to create already exists, deletes it first.
   *
   * @return The path of the created folder.
   */
  private static Path createTmpTestExecutePath() throws IOException {
    
    final Path destPath = deleteTmpTestExecutePath();
    
    FileUtils.copyDirectory(getTestExecutePath().toFile(), destPath.toFile(), false);
    
    return destPath;
  }
  
  /**
   * Deletes the {@link #getTmpTestExecutePath() tmpTestExecute\} folder if it exists.
   */
  private static Path deleteTmpTestExecutePath() throws IOException {
    
    final Path folder = getTmpTestExecutePath();
    
    try {
      FileUtils.deleteDirectory(folder.toFile());
      //Files.deleteIfExists(folder);
    }
    catch (IOException e) {
      
      folder.toFile().deleteOnExit();
      
      throw e;
    }
    return folder;
  }
  
}
