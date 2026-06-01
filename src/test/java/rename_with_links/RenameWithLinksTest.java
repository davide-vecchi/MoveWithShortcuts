/**
 * Created by OpenCode on 2026-05-09 .
 */
package rename_with_links;

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
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;

import static dfile.file.FileUtilities.getCanonicalPath;
import static dutil.string.TextUtilities.S;
import static dutil.system.OSUtilities.WIN_SHORTCUT_EXTENSION;
import static org.apache.commons.io.FilenameUtils.EXTENSION_SEPARATOR;
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
  
  // TODO @@@ FIXME Fails after replacing dependency com.github.vatbub/mslinks with org.jabref/mslinks. Must investigate and update this test accordingly.
  @Test(enabled = false, description = "Fails after replacing dependency com.github.vatbub/mslinks with org.jabref/mslinks. Must investigate and update this test accordingly.")
  void matchingLinkUpdated() throws Throwable {

    // Create a .lnk that points to the source file, verify it's updated

    final File srcFile = Files.createTempFile(this.tempDir, "lnkSrc_", EXTENSION_SEPARATOR + "txt").toFile();

    Files.writeString(srcFile.toPath(), "lnk test");

    final File searchDir = Files.createTempDirectory(this.tempDir, "lnkSearch4_").toFile();

    final File lnkFile = new File(searchDir, "shortcut" + WIN_SHORTCUT_EXTENSION);

    ShellLink.createLink(getCanonicalPath(srcFile)).saveTo(getCanonicalPath(lnkFile));

    final File destinationFileOrFolder = new File(this.tempDir.toFile(), "lnk_renamed" + EXTENSION_SEPARATOR + "txt");

    lenient().when(this.mockUserIO.in(anyString(), anyString(), anyString()))
             .thenReturn(getCanonicalPath(srcFile))
             .thenReturn(getCanonicalPath(destinationFileOrFolder))
             .thenReturn(getCanonicalPath(searchDir));

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

    final File srcFile = Files.createTempFile(this.tempDir, "corruptSrc_", EXTENSION_SEPARATOR + "txt").toFile();

    Files.writeString(srcFile.toPath(), "corrupt");

    final File searchDir = Files.createTempDirectory(this.tempDir, "lnkSearch6_").toFile();

    final File corruptedLnk = new File(searchDir, "bad" + WIN_SHORTCUT_EXTENSION);

    Files.writeString(corruptedLnk.toPath(), "this is not a valid shortcut");

    final File destinationFileOrFolder = new File(this.tempDir.toFile(), "corrupt_renamed" + EXTENSION_SEPARATOR + "txt");
    
    final String verbosity = S(MAX_VERBOSITY);
    
    lenient().when(this.mockUserIO.in(anyString(), anyString(), anyString()))
             .thenReturn(getCanonicalPath(srcFile))
             .thenReturn(getCanonicalPath(destinationFileOrFolder))
             .thenReturn(getCanonicalPath(searchDir))
             .thenReturn(verbosity);

    final RenameWithLinks app = RenameWithLinks.newInstance(this.mockAppContext);

    app.run(this.shortcutTargetUpdater);

    Assert.assertTrue(destinationFileOrFolder.exists(), "Rename should complete despite corrupted " + WIN_SHORTCUT_EXTENSION);
  }

}
