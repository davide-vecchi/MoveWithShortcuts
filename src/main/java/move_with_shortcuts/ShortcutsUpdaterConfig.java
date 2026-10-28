/**
 * Created by Davide on 2026-10-22 .
 */
package move_with_shortcuts;


import application.AAppContext;
import dutil.string.value_parser.BooleanStringParser;
import jakarta.validation.constraints.NotNull;

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.CodeSource;
import java.util.Map;

import static dfile.file.FileUtilities.calcPath;
import static dfile.file.FileUtilities.getCanonicalPath;
import static dfile.file.FileUtilities.getCanonicalPathAsDescr;
import static dutil.object.ObjectUtilities.assertNull;
import static dutil.properties.PropertiesUtilities.readProperties;
import static dutil.properties.PropertiesUtilities.toMap;
import static dutil.string.TextUtilities.NL;
import static dutil.string.TextUtilities.NL2T;
import static dutil.string.TextUtilities.SLASH;
import static dutil.string.TextUtilities.dq;


/**
 * Reads the optional MWS configuration file, located next to the deployed JAR or, when running from the IDE, in the
 * module's resources.
 */
final class ShortcutsUpdaterConfig {


  /**
   * The name of the configuration file.
   */
  static final String CONFIG_FILE_NAME = "MoveWithShortcuts-Config.TXT";
  
  
  /**
   * Private constructor to prevent instantiation.
   */
  private ShortcutsUpdaterConfig() {}
  
  
  /**
   * Reads the configuration parameters from the configuration file.
   *
   * @param appContext The application context to use for logging and warnings.
   *
   * @return The populated and validated parameters, or {@code null} if there is no usable configuration file.
   */
  static MoveWithShortcutsParams readConfiguredParams(@NotNull AAppContext appContext) {
  
    final File configFile = findConfigFile();
    
    MoveWithShortcutsParams result = null;
    
    if (configFile != null) {
    
      try {
      
        final Map<String, String> configurationMap = toMap(readProperties(getCanonicalPath(
                                                                                       configFile)));
        if (! configurationMap.isEmpty()) {
        
          final MoveWithShortcutsParams params = new MoveWithShortcutsParams(
                                                                        configurationMap
                                                          , "Configuration file "
                                                                         + dq(getCanonicalPathAsDescr(configFile))
                                                                       , appContext.devLog);
          params.populate();
          
          params.validate();
          
          result = params;
        }
      }
      catch (RuntimeException e) {
      
        appContext.warnUser(NL + "Could not use the configuration file " + dq(getCanonicalPathAsDescr(configFile))
                                       + " : " + e.getLocalizedMessage() + NL2T + "Using the default values.");
      }
    }
    return result;
  }
  
  /**
   * Returns the shortcuts updater ID configured in the configuration file, or {@code null} if there is no usable
   * configuration file.
   *
   * @param appContext The application context to use for logging and warnings.
   *
   * @return The configured ID, or {@code null}.
   */
  static String readConfiguredUpdaterId(@NotNull AAppContext appContext) {
  
    final MoveWithShortcutsParams params = readConfiguredParams(appContext);
    
    return params == null ? null : params.getShortcutsUpdaterId();
  }
  
  /**
   * Tells whether the configuration file enables the Windows Explorer context-menu integration, defaulting to {@code
   * true} when there is no usable configuration file.
   *
   * @param appContext The application context to use for logging and warnings.
   *
   * @return Whether the integration is enabled.
   */
  static boolean readCreateContextMenu(@NotNull AAppContext appContext) {
  
    final MoveWithShortcutsParams params = readConfiguredParams(appContext);
    
    return params == null || params.isCreateContextMenu();
  }
  
  /**
   * Tells whether the configuration file selects console mode, defaulting to {@code false} when there is no usable
   * configuration file.
   *
   * @return Whether console mode is selected.
   */
  static boolean readConsole() {
  
    final File configFile = findConfigFile();
    
    boolean result = false;
    
    if (configFile != null) {
    
      try {
      
        final Map<String, String> configurationMap = toMap(readProperties(getCanonicalPath(configFile)));
        
        final String value = configurationMap.get(MoveWithShortcutsParams.CONSOLE_PARAM_NAME);
        
        if (value != null) {
        
          result = new BooleanStringParser().parse(value).booleanValue();
        }
      }
      catch (RuntimeException e) {
      
        result = false;
      }
    }
    return result;
  }
  
  /**
   * Finds the configuration file : next to the running JAR, or in the module resources when running from the IDE.
   *
   * @return The existing configuration file, or {@code null}.
   */
  private static File findConfigFile() {
  
    File result = null;
    
    final CodeSource codeSource = ShortcutsUpdaterConfig.class.getProtectionDomain().getCodeSource();
    
    if (codeSource != null) {
    
      final URL locationUrl = codeSource.getLocation();
      
      if (locationUrl != null) {
      
        try {
        
          final File location = new File(locationUrl.toURI());
          
          if (location.isDirectory()) {
          
            final URL resource = ShortcutsUpdaterConfig.class.getResource(SLASH + CONFIG_FILE_NAME);
            
            if (resource != null) {
            
              final File resourceFile = new File(resource.toURI());
              
              if (resourceFile.isFile()) {
              
                result = resourceFile;
              }
            }
            if (result == null) {
            
              final File projectFolder = location.getParentFile() == null ? null
                                                                          : location.getParentFile().getParentFile();
              if (projectFolder != null) {
              
                final File sourceFile = new File(
                                         projectFolder
                                        , calcPath("src", "main", "resources", CONFIG_FILE_NAME));
                if (sourceFile.isFile()) {
                
                  result = sourceFile;
                }
              }
            }
          }
          else {
          
            final File parentFolder = location.getParentFile();
            
            if (parentFolder != null) {
            
              final File externalFile = new File(parentFolder, CONFIG_FILE_NAME);
              
              if (externalFile.isFile()) {
              
                result = externalFile;
              }
            }
          }
        }
        catch (URISyntaxException e) {
          
          assertNull(result);
        }
      }
    }
    return result;
  }
  
}
