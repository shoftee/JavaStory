package javastory.tools;

import java.util.Date;
import java.util.TimeZone;

/**
 * Provides a suite of tools for manipulating Korean Timestamps.
 *
 * @author Frz
 * @since Revision 746
 * @version 1.0
 */
public class FiletimeUtil {

    public final static long ITEM_EXPIRATION = getItemExpiration();
    public final static long ITEM_EXPIRATION_2 = getItemExpiration2();

    private static long getItemExpiration() {
        return 150842304000000000L;
    }

    private static long getItemExpiration2() {
        return 94354848000000000L;
    }
    
    // 100-ns intervals from 1/1/1601 -> 1/1/1970
    private final static long FILETIME_UNIXTIME_OFFSET = 116444736000000000L; 

    /**
     * Converts a Unix Timestamp into File Time
     *
     * @param unixtime the timestamp in UNIX time
     * @return A 64-bit the file time timestamp
     */
    public static long getFiletime(final long unixtime) {
        return ((unixtime * 10000) + FILETIME_UNIXTIME_OFFSET);
    }

    public static boolean isDST() {
        return TimeZone.getDefault().inDaylightTime(new Date());
    }

    public static long getFileTimestamp(long unixtime, boolean roundToMinutes) {
        if (isDST()) {
            unixtime -= 3600000L;
        }
        long time;
        if (roundToMinutes) {
            time = (unixtime / 1000 / 60) * 600000000;
        } else {
            time = unixtime * 10000;
        }
        return time + FILETIME_UNIXTIME_OFFSET;
    }
}
