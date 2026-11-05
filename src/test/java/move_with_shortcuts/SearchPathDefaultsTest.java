/**
 * Created by Davide on 2026-11-04 .
 */
package move_with_shortcuts;


import org.apache.commons.io.FileUtils;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static dfile.file.FileUtilities.getCanonicalPath;
import static java.lang.System.getProperty;


// @formatter:off


/**
 * Tests for {@link SearchPathDefaults}.
 */
public class SearchPathDefaultsTest {


  /**
   * A temporary folder for this test class.
   */
  private Path tempDir;
  
  
  /**
   * Creates the temporary folder.
   *
   * @throws IOException If the temporary folder cannot be created.
   */
  @BeforeClass
  void beforeClass() throws IOException {
  
    this.tempDir = Files.createTempDirectory(SearchPathDefaultsTest.class.getSimpleName() + "_");
  }
  
  /**
   * Deletes the temporary folder.
   *
   * @throws IOException If the temporary folder cannot be deleted.
   */
  @AfterClass
  void afterClass() throws IOException {
  
    FileUtils.deleteDirectory(this.tempDir.toFile());
  }
  
  
  /**
   * Tests that an absent value resolves to the user's home folder without a warning.
   */
  @Test
  void absentValueUsesUserHome() {
  
    final SearchPathDefaults.Resolution resolution = SearchPathDefaults.resolveDefaultSearchPath(null, "test source");
    
    Assert.assertEquals(resolution.path(), getProperty("user.home"));
    
    Assert.assertNull(resolution.warning());
  }
  
  /**
   * Tests that a blank value resolves to the user's home folder with a warning.
   */
  @Test
  void blankValueUsesUserHomeWithWarning() {
  
    final SearchPathDefaults.Resolution resolution = SearchPathDefaults.resolveDefaultSearchPath("  ", "test source");
    
    Assert.assertEquals(resolution.path(), getProperty("user.home"));
    
    Assert.assertNotNull(resolution.warning());
  }
  
  /**
   * Tests that an existing folder is used without a warning.
   *
   * @throws IOException If the folder cannot be created.
   */
  @Test
  void existingFolderIsUsed() throws IOException {
  
    final Path folder = Files.createDirectory(this.tempDir.resolve("searchFolder"));
    
    final SearchPathDefaults.Resolution resolution =
                                    SearchPathDefaults.resolveDefaultSearchPath(folder.toString(), "test source");
                                    
    Assert.assertEquals(resolution.path(), getCanonicalPath(folder.toFile()));
    
    Assert.assertNull(resolution.warning());
  }
  
  /**
   * Tests that a missing folder falls back to the user's home folder with a warning.
   */
  @Test
  void missingFolderFallsBack() {
  
    final String missing = this.tempDir.resolve("missing").toString();
    
    final SearchPathDefaults.Resolution resolution =
                                    SearchPathDefaults.resolveDefaultSearchPath(missing, "test source");
                                    
    Assert.assertEquals(resolution.path(), getProperty("user.home"));
    
    Assert.assertNotNull(resolution.warning());
  }
  
  /**
   * Tests that a file is not accepted as a folder and falls back with a warning.
   *
   * @throws IOException If the file cannot be created.
   */
  @Test
  void fileFallsBack() throws IOException {
  
    final Path file = Files.writeString(this.tempDir.resolve("file.txt"), "content");
    
    final SearchPathDefaults.Resolution resolution =
                                    SearchPathDefaults.resolveDefaultSearchPath(file.toString(), "test source");
                                    
    Assert.assertEquals(resolution.path(), getProperty("user.home"));
    
    Assert.assertNotNull(resolution.warning());
  }
  
  /**
   * Tests that a relative path resolves against the process working folder.
   */
  @Test
  void relativePathResolvesAgainstWorkingFolder() {
  
    final SearchPathDefaults.Resolution resolution =
                                    SearchPathDefaults.resolveDefaultSearchPath(".", "test source");
                                    
    Assert.assertEquals(resolution.path(), getCanonicalPath(new File(".")));
    
    Assert.assertNull(resolution.warning());
  }
}
