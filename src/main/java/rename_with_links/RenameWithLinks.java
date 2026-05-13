/**
 * Created by OpenCode on 2026-05-08 .
 *
 * @formatter:off
 */
package rename_with_links;


import dutil.exception.UserRequestedTermination;
import dutil.exception.exceptions.InvalidExternalValueException;
import dutil.exception.exceptions.MissingExternalValueException;
import dutil.system.OSUtilities;
import jakarta.validation.constraints.NotNull;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import mslinks.ShellLink;
import mslinks.ShellLinkException;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.SystemUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Collection;

import static dfile.file.FileUtilities.getCanonicalPath;
import static dutil.exception.ExceptionUtilities.getFullDescriptionWithRootCause;
import static dutil.object.ObjectUtilities.assertNonNull;
import static dutil.object.ObjectUtilities.assertTrue;
import static dutil.string.TextUtilities.NL;
import static dutil.string.TextUtilities.NL2T;
import static dutil.string.TextUtilities.dq;
import static dutil.system.OSUtilities.WIN_SHORTCUT_EXTENSION;
import static org.apache.commons.lang3.StringUtils.EMPTY;


@Getter
@ToString
public final class RenameWithLinks {
  

  static final String CANCEL_CHARS = "Cc/";
  
  @EqualsAndHashCode.Exclude
  @ToString.Exclude
  private final @NotNull AppContext appContext;
  
  
  private RenameWithLinks(@NotNull AppContext appContext) {
  
    this.appContext = assertNonNull(appContext);
  }
  
  
  public static RenameWithLinks newInstance(@NotNull AppContext appContext) {
  
    return new RenameWithLinks(appContext);
  }
  
  
  public void run() throws UserRequestedTermination, IOException {
  
    // 5.1 : Check OS is Windows :
    
    assertTrue(SystemUtils.IS_OS_WINDOWS, "The OS is not Windows. Instead it is :" + NL2T + OSUtilities.getDescription());
    
    // 5.2 : Ask for existing file/folder path :
    
    final String existingPath = this.appContext.userIO.in("RenameWithLinks : enter the path of the file or folder to rename :"
                                                   , EMPTY, CANCEL_CHARS);
    if (existingPath == null) {
    
      throw new UserRequestedTermination();
    }
    final File existingFileOrFolder = new File(existingPath);
    
    if (! existingFileOrFolder.exists()) {
    
      throw new MissingExternalValueException(dq(existingPath) + " does not exist.");
    }
    // 5.3 : Ask for new name/path (may include a different path â†’ move) :
    
    final String newName = this.appContext.userIO.in("Enter the new name for the file or folder (may include a path) :"
                                , EMPTY, CANCEL_CHARS);
    if (newName == null) {
    
      throw new UserRequestedTermination();
    }
    final File newFile = new File(newName);
    
    // 5.4 : Ask for search path for .lnk files :
    
    final String searchPath = this.appContext.userIO.in("Enter the path to scan for " + WIN_SHORTCUT_EXTENSION + " shortcuts to update :"
                                                 , EMPTY, CANCEL_CHARS);
    if (searchPath == null) {
    
      throw new UserRequestedTermination();
    }
    final File searchDir = new File(searchPath);
    
    if (! searchDir.exists() || ! searchDir.isDirectory()) {
    
      throw new InvalidExternalValueException(dq(searchPath) + " does not exist or is not a directory.");
    }
    // 5.5 : Rename / move the file or folder :
    
    this.appContext.outUser(NL + "Renaming " + dq(existingPath) + " to " + dq(newName) + "...");
    
    if (! newFile.getParentFile().exists()) {
    
      FileUtils.forceMkdir(newFile.getParentFile());
    }
    Files.move(existingFileOrFolder.toPath(), newFile.toPath()
         , StandardCopyOption.REPLACE_EXISTING);
                       
    // 5.6 : Recursively scan the search path for .lnk files :
    
    final Collection<File> lnkFiles = FileUtils.listFiles(searchDir, new String[]{"lnk"}, true);
    
    this.appContext.outUser("Found " + lnkFiles.size() + " shortcut file(s) in " + dq(searchPath) + ". Checking their targets...");
              
    // 5.7 : For each .lnk whose target matches the original path, update it :
    
    for (final File lnk : lnkFiles) {
    
      try {
      
        final ShellLink sl = new ShellLink(lnk);
        
        if (getCanonicalPath(existingFileOrFolder).equalsIgnoreCase(sl.resolveTarget())) {
        
          OSUtilities.updateTargetPath(lnk, getCanonicalPath(newFile));
          
          this.appContext.outUser("Updated target of " + dq(getCanonicalPath(lnk)) + " from " + dq(getCanonicalPath(existingFileOrFolder)) + " to " + dq(getCanonicalPath(newFile)) + ".");
        }
      }
      catch (IOException | ShellLinkException e) {
      
        this.appContext.warnUser("  Warning: could not read shortcut " + getCanonicalPath(lnk) + ": " + e.getMessage());
        
        this.appContext.outUserLog(getFullDescriptionWithRootCause(e));
      }
    }
    this.appContext.outUser(NL + "Done.");
  }
  
}
