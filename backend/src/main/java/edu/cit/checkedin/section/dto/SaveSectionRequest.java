package edu.cit.checkedin.section.dto;

import java.util.Locale;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import edu.cit.checkedin.section.Semester;

/**
 * JSON for creating (POST) or editing (PUT) a section (FR-010, FR-011), e.g.
 * { "subjectCode": "IT342", "subjectTitle": "Systems Integration and Architecture 1",
 *   "sectionCode": "G01", "semester": "FIRST", "schoolYear": "2026-2027" }.
 * Codes are cleaned up first (spaces trimmed, capital letters), so "it342 " is saved as "IT342".
 *
 * @param subjectCode  e.g. IT342 (at most 15 characters)
 * @param subjectTitle e.g. Systems Integration and Architecture 1 (at most 120 characters)
 * @param sectionCode  e.g. G01 (at most 10 characters)
 * @param semester     FIRST, SECOND or SUMMER
 * @param schoolYear   two years in a row, e.g. 2026-2027
 */
public record SaveSectionRequest(
        @NotBlank(message = "Please enter the subject code, for example IT342.")
        @Size(max = 15, message = "The subject code can have at most 15 characters.")
        @Pattern(regexp = "^$|^[A-Z0-9 -]+$",
                message = "The subject code can only have letters, numbers, spaces and hyphens, for example IT342.")
        String subjectCode,

        @NotBlank(message = "Please enter the subject title.")
        @Size(max = 120, message = "The subject title can have at most 120 characters.")
        String subjectTitle,

        @NotBlank(message = "Please enter the section code, for example G01.")
        @Size(max = 10, message = "The section code can have at most 10 characters.")
        @Pattern(regexp = "^$|^[A-Z0-9-]+$",
                message = "The section code can only have letters, numbers and hyphens, for example G01.")
        String sectionCode,

        @NotNull(message = "Please choose the semester.")
        Semester semester,

        // Empty is let through here so that only the "Please enter..." message shows for a blank year
        @NotBlank(message = "Please enter the school year, for example 2026-2027.")
        @Pattern(regexp = "^$|^\\d{4}-\\d{4}$", message = "Please enter the school year as two years, for example 2026-2027.")
        String schoolYear) {

    /** Runs before the checks above: trims spaces and writes the codes in capital letters. */
    public SaveSectionRequest {
        subjectCode = cleanCode(subjectCode);
        subjectTitle = (subjectTitle == null) ? null : subjectTitle.trim().replaceAll("\\s+", " ");
        sectionCode = cleanCode(sectionCode);
        schoolYear = (schoolYear == null) ? null : schoolYear.trim();
    }

    private static String cleanCode(String code) {
        return (code == null) ? null : code.trim().replaceAll("\\s+", " ").toUpperCase(Locale.ROOT);
    }
}
