/**
 * Created by Davide on 2026-10-23 .
 */
package move_with_shortcuts;


import dlog.log.Log;
import dutil.exception.exceptions.InvalidValueException;
import dutil.value_holder.ObjectAndDescr;
import org.apache.commons.io.FileUtils;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static dfile.file.FileUtilities.getCanonicalPath;
import static dutil.string.TextUtilities.NL;
import static dutil.string.TextUtilities.NL2;


// @formatter:off


/**
 * Tests for the GUI support classes : the registry command construction, the configuration parameter and the GUI
 * controller. It never creates Swing components, so it runs headless.
 */
public class MoveWithShortcutsGuiTest {


  /**
   * A temporary folder for this test class.
   */
  private Path tempDir;
  
  /**
   * The log to pass to the objects under test.
   */
  private Log devLog;
  
  
  /**
   * Creates the temporary folder and the log.
   *
   * @throws IOException If the temporary folder cannot be created.
   */
  @BeforeClass
  void beforeClass() throws IOException {
  
    this.tempDir = Files.createTempDirectory(MoveWithShortcutsGuiTest.class.getSimpleName() + "_");
    
    this.devLog = new Log(MoveWithShortcutsGuiTest.class.getSimpleName() + " - developer log"
                        , MoveWithShortcutsGuiTest.class.getSimpleName() + "_dev-log.LOG", true);
  }
  
  /**
   * Closes the log and deletes the temporary folder.
   *
   * @throws IOException If the temporary folder cannot be deleted.
   */
  @AfterClass
  void afterClass() throws IOException {
  
    this.devLog.close();
    
    FileUtils.deleteDirectory(this.tempDir.toFile());
  }
  
  
  /**
   * Tests that the registry command quotes the executable, the classpath and the placeholder.
   */
  @Test
  void buildCommandQuotesAllPaths() {
  
    final String javawPath = "C:\\Program Files\\Java\\bin\\javaw.exe";
    
    final String classpath = "C:\\MoveWithShortcuts\\MoveWithShortcuts.jar";
    
    final String expected = "\"" + javawPath + "\" -cp \"" + classpath + "\" "
                          + ExplorerContextMenu.GUI_MAIN_CLASS + " \"%1\"";
                          
    Assert.assertEquals(ExplorerContextMenu.buildCommand(javawPath, classpath), expected);
  }
  
  /**
   * Tests that a {@code reg add} command for the default value uses {@code /ve}.
   */
  @Test
  void buildRegAddArgsUsesVeForDefaultValue() {
  
    final List<String> args = ExplorerContextMenu.buildRegAddArgs(
                                        ExplorerContextMenu.FILES_VERB_KEY, null
                                      , ExplorerContextMenu.MENU_LABEL);
                                      
    Assert.assertEquals(args, List.of("add", ExplorerContextMenu.FILES_VERB_KEY, "/ve", "/d"
                                    , ExplorerContextMenu.MENU_LABEL, "/f"));
  }
  
  /**
   * Tests that a {@code reg add} command for a named value uses {@code /v}.
   */
  @Test
  void buildRegAddArgsUsesVForNamedValue() {
  
    final List<String> args = ExplorerContextMenu.buildRegAddArgs(
                                        ExplorerContextMenu.FOLDERS_VERB_KEY
                                      , ExplorerContextMenu.MULTI_SELECT_MODEL
                                      , ExplorerContextMenu.MULTI_SELECT_MODEL_VALUE);
                                      
    Assert.assertEquals(args, List.of("add", ExplorerContextMenu.FOLDERS_VERB_KEY
                                    , "/v", ExplorerContextMenu.MULTI_SELECT_MODEL
                                    , "/d", ExplorerContextMenu.MULTI_SELECT_MODEL_VALUE, "/f"));
  }
  
  /**
   * Tests that a {@code reg add} command escapes the double quotes of the value, because {@code reg} would otherwise
   * treat them as syntax.
   */
  @Test
  void buildRegAddArgsEscapesDoubleQuotesInValue() {
  
    final String value = "\"C:\\Program Files\\javaw.exe\" -cp \"C:\\MWS\\MoveWithShortcuts.jar\" main \"%1\"";
    
    final List<String> args = ExplorerContextMenu.buildRegAddArgs(
                                        ExplorerContextMenu.FILES_VERB_KEY + "\\command", null, value);
                                        
    Assert.assertEquals(args.get(4)
                      , "\\\"C:\\Program Files\\javaw.exe\\\" -cp \\\"C:\\MWS\\MoveWithShortcuts.jar\\\" main"
                      + " \\\"%1\\\"");
  }
  
  /**
   * Tests that a {@code reg query} command for a named value uses {@code /v}.
   */
  @Test
  void buildRegQueryArgsUsesVForNamedValue() {
  
    final List<String> args = ExplorerContextMenu.buildRegQueryArgs(
                                        ExplorerContextMenu.FOLDERS_VERB_KEY
                                      , ExplorerContextMenu.MULTI_SELECT_MODEL);
                                      
    Assert.assertEquals(args, List.of("query", ExplorerContextMenu.FOLDERS_VERB_KEY
                                    , "/v", ExplorerContextMenu.MULTI_SELECT_MODEL));
  }
  
  /**
   * Tests that the context-menu icon value points to the packaged ICO.
   */
  @Test
  void iconValuePointsToTheIco() {
  
    final String icon = ExplorerContextMenu.iconValue();
    
    Assert.assertNotNull(icon);
    
    Assert.assertTrue(icon.contains("MWS.ico"));
    
    Assert.assertTrue(icon.startsWith("\""));
  }
  
  /**
   * Tests that the context-menu parameter defaults to {@code true}.
   */
  @Test
  void createContextMenuDefaultsToTrue() {
  
    Assert.assertTrue(createParams(Map.of(MoveWithShortcutsParams.SHORTCUTS_UPDATER_PARAM_NAME
                                        , ShortcutsUpdaterKind.defaultKind().getId()))
                                                .isCreateContextMenu());
  }
  
  /**
   * Tests that the context-menu parameter reads {@code false}.
   */
  @Test
  void createContextMenuReadsFalse() {
  
    Assert.assertFalse(createParams(Map.of(MoveWithShortcutsParams.CREATE_CONTEXT_MENU_PARAM_NAME, "false"))
                                                .isCreateContextMenu());
  }
  
  /**
   * Tests that the context-menu parameter accepts {@code yes}.
   */
  @Test
  void createContextMenuReadsYes() {
  
    Assert.assertTrue(createParams(Map.of(MoveWithShortcutsParams.CREATE_CONTEXT_MENU_PARAM_NAME, "yes"))
                                                .isCreateContextMenu());
  }
  
  /**
   * Tests that the context-menu parameter rejects an invalid value.
   */
  @Test
  void createContextMenuRejectsInvalidValue() {
  
    Assert.expectThrows(InvalidValueException.class
                      , () -> createParams(Map.of(MoveWithShortcutsParams.CREATE_CONTEXT_MENU_PARAM_NAME, "maybe")));
  }
  
  /**
   * Tests that the console parameter defaults to {@code false}.
   */
  @Test
  void consoleDefaultsToFalse() {
  
    Assert.assertFalse(createParams(Map.of(MoveWithShortcutsParams.SHORTCUTS_UPDATER_PARAM_NAME
                                         , ShortcutsUpdaterKind.defaultKind().getId()))
                                                .isConsole());
  }
  
  /**
   * Tests that the console parameter reads {@code true}.
   */
  @Test
  void consoleReadsTrue() {
  
    Assert.assertTrue(createParams(Map.of(MoveWithShortcutsParams.CONSOLE_PARAM_NAME, "true")).isConsole());
  }
  
  /**
   * Tests that the console parameter rejects an invalid value.
   */
  @Test
  void consoleRejectsInvalidValue() {
  
    Assert.expectThrows(InvalidValueException.class
                      , () -> createParams(Map.of(MoveWithShortcutsParams.CONSOLE_PARAM_NAME, "maybe")));
  }
  
  /**
   * Tests the console/GUI mode decision.
   */
  @Test
  void consoleModeDecisionFollowsFlagsAndConfiguration() {
  
    Assert.assertTrue(MoveWithShortcutsMain.isConsoleRequested(true, false, false));
    
    Assert.assertFalse(MoveWithShortcutsMain.isConsoleRequested(false, true, true));
    
    Assert.assertTrue(MoveWithShortcutsMain.isConsoleRequested(false, false, true));
    
    Assert.assertFalse(MoveWithShortcutsMain.isConsoleRequested(false, false, false));
  }
  
  /**
   * Tests that the usage text mentions the launch modes and the help option.
   */
  @Test
  void usageTextMentionsLaunchModes() {
  
    final String usage = MoveWithShortcutsMain.getUsageText();
    
    Assert.assertTrue(usage.contains("-console"));
    
    Assert.assertTrue(usage.contains("-gui"));
    
    Assert.assertTrue(usage.contains("-help"));
    
    Assert.assertTrue(usage.contains("-from"));
  }
  
  /**
   * Tests that the usage formatter adds one empty line before each option description.
   */
  @Test
  void formatUsageTextAddsBlankLinesBeforeOptions() {
  
    final String raw = "Usage: test" + NL + "  -first first" + NL + "  -second second";
    
    final String formatted = MoveWithShortcutsMain.formatUsageText(raw);
    
    Assert.assertTrue(formatted.contains(NL2 + "  -first"));
    
    Assert.assertTrue(formatted.contains(NL2 + "  -second"));
  }
  
  /**
   * Tests that a rename of an existing file is accepted and requires the move.
   *
   * @throws IOException If the test files cannot be created.
   */
  @Test
  void buildRequestAcceptsExistingOriginal() throws IOException {
  
    final File searchFolder = newFolder("acceptsExistingOriginal");
    
    final File original = new File(searchFolder, "old.txt");
    
    Files.writeString(original.toPath(), "content");
    
    final File destination = new File(searchFolder, "new.txt");
    
    final ObjectAndDescr<GuiMoveRequest> built = MoveWithShortcutsGuiController.buildRequest(
                                                       original.getPath(), destination.getPath()
                                                     , searchFolder.getPath());
                                                     
    Assert.assertNull(built.description);
    
    Assert.assertNotNull(built.object);
    
    Assert.assertTrue(built.object.mustMove());
    
    Assert.assertEquals(built.object.originalFileOrFolder().getPath(), original.getPath());
    
    Assert.assertEquals(built.object.destinationFileOrFolder().getPath(), destination.getPath());
    
    Assert.assertEquals(built.object.searchFolder().getPath(), searchFolder.getPath());
  }
  
  /**
   * Tests that an already-done move is accepted and does not require the move.
   *
   * @throws IOException If the test files cannot be created.
   */
  @Test
  void buildRequestAcceptsAlreadyDoneMove() throws IOException {
  
    final File searchFolder = newFolder("acceptsAlreadyDoneMove");
    
    final File original = new File(searchFolder, "missing.txt");
    
    final File destination = new File(searchFolder, "already-there.txt");
    
    Files.writeString(destination.toPath(), "content");
    
    final ObjectAndDescr<GuiMoveRequest> built = MoveWithShortcutsGuiController.buildRequest(
                                                       original.getPath(), destination.getPath()
                                                     , searchFolder.getPath());
                                                     
    Assert.assertNull(built.description);
    
    Assert.assertNotNull(built.object);
    
    Assert.assertFalse(built.object.mustMove());
  }
  
  /**
   * Tests that blank values are rejected with a description.
   */
  @Test
  void buildRequestRejectsBlankValues() {
  
    final File searchFolder = this.tempDir.toFile();
    
    final ObjectAndDescr<GuiMoveRequest> built = MoveWithShortcutsGuiController.buildRequest(
                                                       " ", "destination", searchFolder.getPath());
                                                       
    Assert.assertNotNull(built.description);
    
    Assert.assertNull(built.object);
  }
  
  /**
   * Tests that a destination equal to the original is rejected with a description.
   *
   * @throws IOException If the test files cannot be created.
   */
  @Test
  void buildRequestRejectsSameOriginalAndDestination() throws IOException {
  
    final File searchFolder = newFolder("rejectsSamePath");
    
    final File original = new File(searchFolder, "same.txt");
    
    Files.writeString(original.toPath(), "content");
    
    final ObjectAndDescr<GuiMoveRequest> built = MoveWithShortcutsGuiController.buildRequest(
                                                       original.getPath(), original.getPath()
                                                     , searchFolder.getPath());
                                                     
    Assert.assertNotNull(built.description);
    
    Assert.assertNull(built.object);
  }
  
  /**
   * Tests that a search path that is a file is rejected with a description.
   *
   * @throws IOException If the test files cannot be created.
   */
  @Test
  void buildRequestRejectsSearchPathThatIsNotAFolder() throws IOException {
  
    final File searchFolder = newFolder("rejectsSearchFile");
    
    final File original = new File(searchFolder, "old.txt");
    
    Files.writeString(original.toPath(), "content");
    
    final File destination = new File(searchFolder, "new.txt");
    
    final File searchFile = new File(searchFolder, "not-a-folder.txt");
    
    Files.writeString(searchFile.toPath(), "content");
    
    final ObjectAndDescr<GuiMoveRequest> built = MoveWithShortcutsGuiController.buildRequest(
                                                       original.getPath(), destination.getPath()
                                                     , searchFile.getPath());
                                                     
    Assert.assertNotNull(built.description);
    
    Assert.assertNull(built.object);
  }
  
  /**
   * Tests that the default search path is the user's home folder when no original path is given.
   */
  @Test
  void defaultSearchPathUsesUserHomeWhenNoOriginal() {
  
    Assert.assertEquals(MoveWithShortcutsGuiController.defaultSearchPath(null), System.getProperty("user.home"));
    
    Assert.assertEquals(MoveWithShortcutsGuiController.defaultSearchPath("  "), System.getProperty("user.home"));
  }
  
  /**
   * Tests that the default search path is the parent folder of a selected file.
   *
   * @throws IOException If the test file cannot be created.
   */
  @Test
  void defaultSearchPathUsesParentOfFile() throws IOException {
  
    final File folder = newFolder("searchFileParent");
    
    final File file = new File(folder, "file.txt");
    
    Files.writeString(file.toPath(), "content");
    
    Assert.assertEquals(MoveWithShortcutsGuiController.defaultSearchPath(file.getPath()), getCanonicalPath(folder));
  }
  
  /**
   * Tests that the default search path is the parent folder of a selected folder.
   *
   * @throws IOException If the test folder cannot be created.
   */
  @Test
  void defaultSearchPathUsesParentOfFolder() throws IOException {
  
    final File folder = newFolder("searchFolderParent");
    
    final File child = new File(folder, "child");
    
    Files.createDirectories(child.toPath());
    
    Assert.assertEquals(MoveWithShortcutsGuiController.defaultSearchPath(child.getPath()), getCanonicalPath(folder));
  }
  
  /**
   * Tests that the default search path falls back to the user's home folder when the path has no parent.
   */
  @Test
  void defaultSearchPathUsesUserHomeForPathWithoutParent() {
  
    Assert.assertEquals(MoveWithShortcutsGuiController.defaultSearchPath("relative.txt")
                      , System.getProperty("user.home"));
  }
  
  /**
   * Tests that the log folder leaf is the original leaf, including its extension.
   */
  @Test
  void logLeafUsesOriginalLeaf() {
  
    Assert.assertEquals(MoveWithShortcutsLogs.leafOf("C:\\a\\Budget.xlsx"), "Budget.xlsx");
    
    Assert.assertEquals(MoveWithShortcutsLogs.leafOf("C:\\a\\my folder\\"), "my folder");
    
    Assert.assertEquals(MoveWithShortcutsLogs.leafOf(null), "MoveWithShortcuts");
  }
  
  /**
   * Tests that the log folder leaf replaces the characters that Windows does not allow.
   */
  @Test
  void logLeafSanitizesInvalidChars() {
  
    Assert.assertEquals(MoveWithShortcutsLogs.leafOf("C:\\a\\a:b*?.txt"), "a_b__.txt");
    
    Assert.assertEquals(MoveWithShortcutsLogs.leafOf("C:\\a\\name."), "name");
  }
  
  /**
   * Tests that the per-instance log folder uses the original leaf and is unique.
   *
   * @throws IOException If the test folders cannot be created.
   */
  @Test
  void instanceLogFolderUsesLeafAndIsUnique() throws IOException {
  
    final File parent = newFolder("logs");
    
    final File first = MoveWithShortcutsLogs.createInstanceLogFolder(parent, "C:\\a\\Budget.xlsx");
    
    final File second = MoveWithShortcutsLogs.createInstanceLogFolder(parent, "C:\\a\\Budget.xlsx");
    
    Assert.assertNotNull(first);
    
    Assert.assertNotNull(second);
    
    Assert.assertTrue(first.isDirectory());
    
    Assert.assertTrue(second.isDirectory());
    
    Assert.assertTrue(first.getName().contains("_Budget.xlsx"));
    
    Assert.assertTrue(second.getName().contains("_Budget.xlsx"));
    
    Assert.assertNotEquals(first.getName(), second.getName());
  }
  
  /**
   * Tests that a log file name is placed in the log folder when one is given.
   */
  @Test
  void logFileNameUsesFolderWhenGiven() {
  
    final File folder = new File(this.tempDir.toFile(), "log");
    
    Assert.assertEquals(MoveWithShortcutsLogs.fileName(null, "test.LOG"), "test.LOG");
    
    Assert.assertEquals(MoveWithShortcutsLogs.fileName(folder, "test.LOG")
                      , new File(folder, "test.LOG").getPath());
  }
  
  /**
   * Creates and populates the configuration parameters from the given map.
   *
   * @param configurationMap The configuration map.
   *
   * @return The populated and validated parameters.
   */
  private MoveWithShortcutsParams createParams(Map<String, String> configurationMap) {
  
    final MoveWithShortcutsParams result = new MoveWithShortcutsParams(configurationMap, "test", this.devLog);
    
    result.populate();
    
    result.validate();
    
    return result;
  }
  
  /**
   * Creates a new empty folder under this test class' temporary folder.
   *
   * @param name The folder name.
   *
   * @return The new folder.
   *
   * @throws IOException If the folder cannot be created.
   */
  private File newFolder(String name) throws IOException {
  
    final Path folder = this.tempDir.resolve(name + "_" + System.nanoTime());
    
    Files.createDirectories(folder);
    
    return folder.toFile();
  }
}
