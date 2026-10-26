/**
 * Created by Davide on 2026-10-26 .
 */
package move_with_shortcuts;


import jakarta.validation.constraints.NotNull;


// @formatter:off


/**
 * One execution text update for the GUI operation-detail area.
 *
 * @param kind The kind of the text.<br>
 *
 * @param text The text to show.
 */
record GuiTextUpdate(@NotNull GuiTextKind kind, @NotNull String text) {}
