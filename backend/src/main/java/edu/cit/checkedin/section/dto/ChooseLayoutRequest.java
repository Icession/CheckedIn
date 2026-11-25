package edu.cit.checkedin.section.dto;

import jakarta.validation.constraints.NotNull;

/**
 * JSON for choosing or changing the room layout of an in-person or hybrid session (FR-015), e.g. { "layoutId": 4 }.
 *
 * @param layoutId the layout to use; any professor's layout may be chosen (FR-021)
 */
public record ChooseLayoutRequest(
        @NotNull(message = "Please choose a room layout.")
        Long layoutId) {
}
