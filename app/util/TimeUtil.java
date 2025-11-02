package util;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

public final class TimeUtil {
    private static final ZoneId ET = ZoneId.of("America/Toronto");
    private static final DateTimeFormatter ISO = DateTimeFormatter.ISO_DATE_TIME;

    private TimeUtil() {}

    public static ZonedDateTime toET(String iso) {
        return ZonedDateTime.parse(iso, ISO).withZoneSameInstant(ET);
    }
}
