package utils;

import org.junit.jupiter.api.Test;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeParseException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Full coverage test for TimeUtil utility.
 * Covers:
 *  - Correct timezone conversion
 *  - Handling of Z (UTC) suffix
 *  - Invalid format exception case
 * Author: Varun Oza
 */
public class TimeUtilTest {

    @Test
    void testToET_ConvertsToTorontoTime() {
        String utcTime = "2024-03-10T15:00:00Z"; // before DST switch
        ZonedDateTime result = TimeUtil.toET(utcTime);

        assertNotNull(result);
        assertEquals(ZoneId.of("America/Toronto"), result.getZone());
    }

    @Test
    void testToET_WithOffset() {
        String utcOffset = "2024-06-01T12:00:00+00:00"; // summer time (EDT)
        ZonedDateTime result = TimeUtil.toET(utcOffset);

        assertNotNull(result);
        assertEquals(ZoneId.of("America/Toronto"), result.getZone());
    }

    @Test
    void testToET_InvalidFormatThrows() {
        String badIso = "invalid-date-string";
        assertThrows(DateTimeParseException.class, () -> TimeUtil.toET(badIso));
    }
}
