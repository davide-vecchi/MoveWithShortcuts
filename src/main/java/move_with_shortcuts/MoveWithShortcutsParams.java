/**
 * Created by Davide on 2026-10-22 .
 */
package move_with_shortcuts;


import dlog.log.Log;
import dparam.AParams;
import dparam.ParamMono;
import dparam.pvdc.change_loader.AValueChangeTextReader;
import dparam.pvdc.change_loader.EmptyValueChangeTextReader;
import dutil.string.value_parser.NeutralStringParser;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;

import java.io.Serial;
import java.util.Map;


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
   * The shortcuts updater implementation ID, or the default if the parameter is missing.
   */
  @Getter
  private final ParamMono<Object, String> shortcutsUpdater;
  
  
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
  }
  
  
  /**
   * Populates the configuration parameters from the configuration map.
   */
  @Override
  public void populate() {
  
    this.shortcutsUpdater.setValueParser(new NeutralStringParser())
                        .loadOptionalValue(ShortcutsUpdaterKind.defaultKind().getId());
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
}
