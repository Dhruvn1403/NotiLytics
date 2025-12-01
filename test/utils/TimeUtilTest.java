package utils;

import org.junit.Test;

import java.time.ZonedDateTime;

import static org.junit.Assert.*;

public class TimeUtilTest {

    @Test
    public void toET_convertsSuccessfully() {
        ZonedDateTime out = TimeUtil.toET("2024-01-01T10:00:00Z");

        assertNotNull(out);
        assertEquals("America/Toronto", out.getZone().getId());
    }
}
