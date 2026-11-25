package edu.cit.checkedin.layout;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Database access for room layouts. Spring Data writes the SQL from the method names.
 * "@EntityGraph" loads the cells and the creator in the same query, so building the
 * response does not need one extra query per layout.
 */
public interface RoomLayoutRepository extends JpaRepository<RoomLayout, Long> {

    /** FR-021: layouts of every professor whose room name contains the text, sorted by room name. */
    @EntityGraph(attributePaths = {"seats", "createdBy"})
    List<RoomLayout> findByRoomNameContainingIgnoreCaseOrderByRoomNameAsc(String text);

    /** One layout with all its cells and its creator. */
    @EntityGraph(attributePaths = {"seats", "createdBy"})
    Optional<RoomLayout> findWithSeatsByLayoutId(Long layoutId);

    /** FR-021: room names are unique, so this finds the one layout of a room (used for the duplicate message). */
    @EntityGraph(attributePaths = "createdBy")
    Optional<RoomLayout> findByRoomName(String roomName);
}
