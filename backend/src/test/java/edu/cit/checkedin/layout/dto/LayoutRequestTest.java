package edu.cit.checkedin.layout.dto;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import edu.cit.checkedin.layout.CellType;
import edu.cit.checkedin.layout.RoomType;

/** Checks how layout input is cleaned up and checked (FR-017, FR-019). */
class LayoutRequestTest {

    private static Validator validator;

    @BeforeAll
    static void createValidator() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    @Test
    void theRoomNameIsTrimmedAndWrittenInCapitals() {
        CreateLayoutRequest request = new CreateLayoutRequest("  nge   207 ", RoomType.COMPUTER_LAB, 6, 8);

        assertThat(request.roomName()).isEqualTo("NGE 207");
        assertThat(problemsWith(request)).isEmpty();
    }

    @Test
    void aGridLargerThan20By20IsRefused() {
        CreateLayoutRequest request = new CreateLayoutRequest("NGE207", RoomType.COMPUTER_LAB, 21, 0);

        assertThat(problemsWith(request)).containsExactlyInAnyOrder(
                "The number of rows must be from 1 to 20.", "The number of columns must be from 1 to 20.");
    }

    @Test
    void symbolsInTheRoomNameAreRefused() {
        CreateLayoutRequest request = new CreateLayoutRequest("NGE/207", RoomType.LECTURE_ROOM, 1, 1);

        assertThat(problemsWith(request)).containsExactly(
                "The room name can only have letters, numbers, spaces and hyphens, for example NGE207.");
    }

    @Test
    void seatNumbersAreCleanedAndAnEmptyNumberMeansAutomatic() {
        assertThat(new CellRequest(CellType.WORKSTATION, " pc-12 ").seatLabel()).isEqualTo("PC-12");
        assertThat(new CellRequest(CellType.WORKSTATION, "   ").seatLabel()).isNull();
    }

    @Test
    void everyCellInTheGridIsChecked() {
        SaveLayoutRequest request = new SaveLayoutRequest("NGE207", RoomType.COMPUTER_LAB, 1, 2, List.of(List.of(
                new CellRequest(CellType.WORKSTATION, "12"), new CellRequest(CellType.WORKSTATION, "#13"))));

        assertThat(problemsWith(request)).containsExactly(
                "A seat number can only have letters, numbers and hyphens, for example 12 or PC-12.");
    }

    private static <T> List<String> problemsWith(T request) {
        return validator.validate(request).stream().map(ConstraintViolation::getMessage).toList();
    }
}
