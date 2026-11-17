package edu.cit.checkedin.section;

/**
 * How a weekly session is held (FR-012). In-person and hybrid sessions use a room layout
 * (seat map), online and field sessions do not (FR-015, Phase 3).
 */
public enum DeliveryMode {
    IN_PERSON,
    ONLINE,
    HYBRID,
    FIELD
}
