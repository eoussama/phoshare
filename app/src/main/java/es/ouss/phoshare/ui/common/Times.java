package es.ouss.phoshare.ui.common;

import android.content.Context;
import android.text.format.DateFormat;
import android.text.format.DateUtils;

import java.util.Locale;

import es.ouss.phoshare.R;

/** Dates the way people say them. */
public final class Times {

    private Times() {
    }

    /** "Just now", "5 min. ago", "Yesterday", "Oct 3"... */
    public static CharSequence ago(Context context, long time) {
        long now = System.currentTimeMillis();
        if (now - time < DateUtils.MINUTE_IN_MILLIS) {
            return context.getString(R.string.time_just_now);
        }
        return DateUtils.getRelativeTimeSpanString(time, now, DateUtils.MINUTE_IN_MILLIS, DateUtils.FORMAT_ABBREV_RELATIVE);
    }

    /** "October 2026", in the device's language and order. */
    public static CharSequence monthYear(long time) {
        return DateFormat.format(DateFormat.getBestDateTimePattern(Locale.getDefault(), "MMMMyyyy"), time);
    }
}
