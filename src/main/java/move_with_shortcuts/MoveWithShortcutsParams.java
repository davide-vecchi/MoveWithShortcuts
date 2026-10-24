/**
 * Created by Davide on 2026-10-22 .
 */
package move_with_shortcuts;


import dlog.log.Log;
import dparam.AParams;
import dparam.ParamMono;
import dparam.pvdc.change_loader.AValueChangeTextReader;
import dparam.pvdc.change_loader.EmptyValueChangeTextReader;
import dutil.string.value_parser.BooleanStringParser;
import dutil.string.value_parser.NeutralStringParser;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;

import java.io.Serial;
import java.util.Map;

import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;


/**
 * The configuration parameters of MWS that can be read from its configuration file.
 */
final class MoveWithShortcutsParams extends AParams<Object> {


  @Serial
  private static final long serialVersionUID = 1L;
  
  /**
   * The name of the parameter selecting the shortcuts updater implementation.
   */
  static final String SHORTCUTS_UPDATER_PARAM_NAME = "ShortcutsUpdater";
  
  /**
   * The name of the parameter that enables or disables the Windows Explorer context-menu integration.
   */
  static final String CREATE_CONTEXT_MENU_PARAM_NAME = "CreateContextMenu";
  
  /**
   * The name of the parameter that selects console mode instead of the default GUI mode.
   */
  static final String CONSOLE_PARAM_NAME = "Console";
  
  /**
   * The shortcuts updater implementation ID, or the default if the parameter is missing.
   */
  @Getter
  private final ParamMono<Object, String> shortcutsUpdater;
  
  /**
   * Whether MWS must create or update the Windows Explorer context-menu integration at startup, or the default if the
   * parameter is missing.
   */
  @Getter
  private final ParamMono<Object, Boolean> createContextMenu;
  
  /**
   * Whether MWS must start in console mode instead of the default GUI mode, or the default if the parameter is
   * missing.
   */
  @Getter
  private final ParamMono<Object, Boolean> console;
  
  
  /**
   * @param configurationMap {@link #configurationMap}.<br>
   *
   * @param sourceDescr {@link #sourceDescr}.<br>
   *
   * @param log {@link #log}.
   */
  MoveWithShortcutsParams(@NotNull Map<String, String> configurationMap, String sourceDescr, @NotNull Log log) {
  
    super(configurationMap, null, sourceDescr, log);
    
    this.shortcutsUpdater = new ParamMono<>(this, SHORTCUTS_UPDATER_PARAM_NAME);
    
    this.createContextMenu = new ParamMono<>(this, CREATE_CONTEXT_MENU_PARAM_NAME);
    
    this.console = new ParamMono<>(this, CONSOLE_PARAM_NAME);
  }
  
  
  /**
   * Populates the configuration parameters from the configuration map.
   */
  @Override
  public void populate() {
  
    this.shortcutsUpdater.setValueParser(new NeutralStringParser())
                        .loadOptionalValue(ShortcutsUpdaterKind.defaultKind().getId());
                        
    this.createContextMenu.setValueParser(new BooleanStringParser())
                          .loadOptionalValue(TRUE);
                          
    this.console.setValueParser(new BooleanStringParser())
                .loadOptionalValue(FALSE);
  }
  
  /**
   * Validates the configuration parameters.
   */
  @Override
  public void validate() {
  
    // The selected implementation ID is validated when the updater is chosen.
  }
  
  /**
   * @see AParams#getDefaultValueChangeReader()
   */
  @Override
  protected @NotNull AValueChangeTextReader<Object, ?> getDefaultValueChangeReader() {
  
    return new EmptyValueChangeTextReader<>(this.log);
  }
  
  /**
   * Returns the selected shortcuts updater implementation ID.
   *
   * @return The ID.
   */
  String getShortcutsUpdaterId() {
  
    return this.shortcutsUpdater.value;
  }
  
  /**
   * Tells whether MWS must create or update the Windows Explorer context-menu integration at startup.
   *
   * @return Whether the integration must be created or updated.
   */
  boolean isCreateContextMenu() {
  
    return this.createContextMenu.value.booleanValue();
  }
  
  /**
   * Tells whether MWS must start in console mode instead of the default GUI mode.
   *
   * @return Whether console mode is selected.
   */
  boolean isConsole() {
  
    return this.console.value.booleanValue();
  }
}
