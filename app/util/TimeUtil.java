package util;
import java.time.*;
import java.time.format.DateTimeFormatter;
public final class TimeUtil {
    private static final ZoneId ET = ZoneId.of("America/Toronto");
    public static ZonedDateTime toET(String isoZ) {
        return ZonedDateTime.parse(isoZ).withZoneSameInstant(ET);
    }
    private TimeUtil() {}
}
