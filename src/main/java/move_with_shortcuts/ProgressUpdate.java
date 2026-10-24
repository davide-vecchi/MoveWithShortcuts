/**
 * Created by Davide on 2026-10-24 .
 */
package move_with_shortcuts;


import jakarta.validation.constraints.NotBlank;


// @formatter:off


/**
 * One progress update : the current phase, the total number of items when known, and the percentage.
 *
 * @param phase The phase label.<br>
 *
 * @param total The total number of items, or {@code 0} when it is not known yet.<br>
 *
 * @param percentage The percentage, or a negative value when it is not known yet.
 */
record ProgressUpdate(@NotBlank String phase, int total, int percentage) {}
