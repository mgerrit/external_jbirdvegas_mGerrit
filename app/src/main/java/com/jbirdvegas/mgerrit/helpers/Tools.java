package com.jbirdvegas.mgerrit.helpers;

/*
 * Copyright (C) 2013 Android Open Kang Project (AOKP)
 *  Author: Jon Stanford (JBirdVegas), 2013
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.util.TypedValue;
import android.view.View;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.ListView;
import android.widget.TextView;
import com.android.volley.Cache;
import com.android.volley.NetworkResponse;
import com.android.volley.toolbox.HttpHeaderParser;
import com.jbirdvegas.mgerrit.SigninActivity;
import com.jbirdvegas.mgerrit.activities.DiffViewer;
import com.jbirdvegas.mgerrit.fragments.PrefsFragment;
import com.nhaarman.listviewanimations.appearance.SingleAnimationAdapter;
import com.nhaarman.listviewanimations.appearance.simple.SwingBottomInAnimationAdapter;
import com.jbirdvegas.mgerrit.R;
import com.jbirdvegas.mgerrit.objects.FileInfo;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.io.File;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

import de.greenrobot.event.EventBus;

public class Tools {

    private static final String GERRIT_DATETIME_FORMAT = "yyyy-MM-dd hh:mm:ss.SSS";
    private static final String HUMAN_READABLE_DATETIME_FORMAT = "MMMM dd, yyyy '%s' hh:mm:ss aa";
    private static final String HUMAN_READABLE_DATE_FORMAT = "MMMM dd, yyyy";
    private static final String HUMAN_READABLE_TIME_FORMAT = "hh:mm aa";
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss.SSS").withLocale(Locale.US);

    public static String stackTraceToString(Throwable e) {
        StringBuilder sb = new StringBuilder(0);
        sb.append(e.getLocalizedMessage()).append("\n\n");
        for (StackTraceElement element : e.getStackTrace()) {
            sb.append("-- ")
                .append(element.toString())
                .append('\n');
        }
        return sb.toString();
    }

    /**
     * Enables or disables listview animations. This simply toggles the
     *  adapter, initialising a new adapter if necessary.
     * @param enable Whether to enable animations on the listview
     * @return The outer-most adapter that was applied to the list view.
     *  a non-null instance of animAdapter if enable, defaultAdapter otherwise.
     */
    public static BaseAdapter toggleAnimations(boolean enable, ListView lv,
                                           SingleAnimationAdapter animAdapter,
                                           BaseAdapter defaultAdapter) {
        if (enable) {
            if (animAdapter == null) {
                animAdapter = new SwingBottomInAnimationAdapter(defaultAdapter);
                animAdapter.setAbsListView(lv);
            }
            lv.setAdapter(animAdapter);
            return animAdapter;
        } else if (defaultAdapter != null) {
            lv.setAdapter(defaultAdapter);
            return defaultAdapter;
        }
        else return null;
    }

    /**
     * Queries the active network and determine if it has Internet connectivity.
     * @param context Application or activity context
     * @return Whether we are connected to the internet, regardless of the network type
     */
    public static boolean isConnected(Context context) {
        ConnectivityManager cm =
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        return activeNetwork != null && activeNetwork.isConnectedOrConnecting();
    }


    /**
     * PrettyPrint the Gerrit provided timestamp
     * format into a more human readable format
     *
     * I have no clue what the ten zeros after the seconds are good for as
     * the exact same ten zeros are in all databases regardless of the stamp's
     * time or timezone... it comes from Google so we just handle oddities downstream :(
     * from "2013-06-09 19:47:40.000"
     * to Jun 09, 2013 07:47 40ms PM
     *
     * @return String representation of the date
     *         example: Jun 09, 2013 07:47 40ms PM
     */
    @SuppressWarnings("SimpleDateFormatWithoutLocale")
    public static String prettyPrintDateTime(Context context, String date,
                                             TimeZone serverTimeZone, TimeZone localTimeZone) {
        try {
            SimpleDateFormat currentDateFormat
                    = new SimpleDateFormat(GERRIT_DATETIME_FORMAT, Locale.US);
            DateFormat humanDateFormat = new SimpleDateFormat(
                    String.format(HUMAN_READABLE_DATETIME_FORMAT,
                            context.getString(R.string.at)),
                    Locale.getDefault());
            // location of server
            currentDateFormat.setTimeZone(serverTimeZone);
            // local location
            humanDateFormat.setTimeZone(localTimeZone);
            return humanDateFormat.format(currentDateFormat.parse(date));
        } catch (ParseException e) {
            e.printStackTrace();
            return date;
        }
    }

    @SuppressWarnings("SimpleDateFormatWithoutLocale")
    public static String prettyPrintTime(Context context, String date,
                                         TimeZone serverTimeZone, TimeZone localTimeZone) {
        try {
            SimpleDateFormat currentDateFormat
                    = new SimpleDateFormat(GERRIT_DATETIME_FORMAT, Locale.US);
            DateFormat humanDateFormat = new SimpleDateFormat(HUMAN_READABLE_TIME_FORMAT, Locale.getDefault());
            // location of server
            currentDateFormat.setTimeZone(serverTimeZone);
            // local location
            humanDateFormat.setTimeZone(localTimeZone);
            return humanDateFormat.format(currentDateFormat.parse(date));
        } catch (ParseException e) {
            e.printStackTrace();
            return date;
        }
    }

    public static void colorPath(Resources r, TextView view,
                                 String statusText, boolean usingLightTheme) {
        FileInfo.Status status = FileInfo.Status.getValue(statusText);
        int green = r.getColor(R.color.text_green);
        int red = r.getColor(R.color.text_red);

        if (status == FileInfo.Status.ADDED) {
            view.setTextColor(green);
        } else if (status == FileInfo.Status.DELETED) {
            view.setTextColor(red);
        } else {
            // Need to determine from the current theme what the default color is and set it back
            if (usingLightTheme) {
                view.setTextColor(r.getColor(R.color.text_light));
            } else {
                view.setTextColor(r.getColor(R.color.text_dark));
            }
        }
    }

    /**
     * @param filePath A path to a file
     * @return The short file name from a full file path
     */
    public static String getFileName(String filePath) {
        // Remove the path from the filename
        int idx = filePath.lastIndexOf("/");
        return idx >= 0 ? filePath.substring(idx + 1) : filePath;
    }

    public static String getWebAddress(Context context, int commitNumber) {
        return String.format("%s#/c/%d/", PrefsFragment.getCurrentGerrit(context), commitNumber);
    }

    public static Intent createShareIntent(Context context, String changeid, int changeNumber) {
        String webAddress = Tools.getWebAddress(context, changeNumber);
        return createShareIntent(context, changeid, webAddress);
    }

    public static Intent createShareIntent(Context context, String changeid, String webAddress) {
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
        intent.putExtra(Intent.EXTRA_SUBJECT,
                String.format(context.getResources().getString(R.string.commit_shared_from_mgerrit),
                        changeid));
        intent.putExtra(Intent.EXTRA_TEXT, webAddress + " #mGerrit");
        return intent;
    }

    public static String getFileExtention(String filename) {
        int start = filename.lastIndexOf(".");
        return filename.substring(++start);
    }

    public static boolean isImage(String filePath) {
        String fileExtention = getFileExtention(filePath);
        return fileExtention.equals("png") || fileExtention.equals("jpg");
    }

    public static String getRevisionUrl(Context context, Integer changeNumber, Integer patchSetNumber) {
        String ps;
        if (patchSetNumber == null || patchSetNumber < 1) ps = "current";
        else ps = String.valueOf(patchSetNumber);
        return String.format("%schanges/%d/revisions/%s/patch?zip",
                PrefsFragment.getCurrentGerrit(context),
                changeNumber, ps);
    }

    /**
     * Extracts a {@link com.android.volley.Cache.Entry} from a {@link com.android.volley.NetworkResponse}.
     * Cache-control headers are ignored.
     * @param response The network response to parse headers from
     * @return a cache entry for the given response, or null if the response is not cacheable.
     * @link http://stackoverflow.com/questions/16781244/android-volley-jsonobjectrequest-caching
     */
    public static Cache.Entry parseIgnoreCacheHeaders(NetworkResponse response,
                                                      long cacheRefreshTime,
                                                      long cacheExpiresTime) {
        long now = System.currentTimeMillis();

        Map<String, String> headers = response.headers;
        long serverDate = 0;
        String headerValue;

        headerValue = headers.get("Date");
        if (headerValue != null) {
            serverDate = HttpHeaderParser.parseDateAsEpoch(headerValue);
        }

        final long softExpire = now + cacheRefreshTime;
        final long ttl = now + cacheExpiresTime;

        Cache.Entry entry = new Cache.Entry();
        entry.data = response.data;
        entry.etag = null; // Not worried about etag
        entry.softTtl = softExpire;
        entry.ttl = ttl;
        entry.serverDate = serverDate;
        entry.responseHeaders = headers;
        return entry;
    }

    public static DateTime parseDate(String dateStr, TimeZone serverTimeZone, TimeZone localTimeZone) {
        try {
            DateTime d = DATE_TIME_FORMATTER.withZone(DateTimeZone.forTimeZone(serverTimeZone)).parseDateTime(dateStr);
            return d.withZone(DateTimeZone.forTimeZone(localTimeZone)).withMillisOfDay(0);
        } catch (IllegalArgumentException e) {
            return new DateTime(0);
        }
    }

    public static void launchSignin(Context context) {
        // We don't want to open the sign in screen multiple times
        // If the sign in activity is open it will be registered.
        if (!EventBus.getDefault().isRegistered(SigninActivity.class) && !SigninActivity.isActive()) {
            // We have an invalid username or password so launch the sign in activity to request a new one
            Intent intent = new Intent(context, SigninActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
            intent.putExtra(SigninActivity.CLOSE_ON_SUCCESSFUL_SIGNIN, true);
            context.startActivity(intent);
        }
    }

    public static int getResIdFromAttribute(final Context activity, final int attr)
    {
        if(attr==0) return 0;
        final TypedValue typedvalueattr = new TypedValue();
        activity.getTheme().resolveAttribute(attr, typedvalueattr, true);
        return typedvalueattr.resourceId;
    }


    // See: http://stackoverflow.com/questions/18200811/android-clear-cache-programmatically
    public static void trimCache(Context context) {
        try {
            File dir = context.getCacheDir();
            if (dir != null && dir.isDirectory()) {
                deleteDir(dir);
            }
        } catch (Exception e) {
            // TODO: handle exception
        }
    }
    public static boolean deleteDir(File dir) {
        if (dir != null && dir.isDirectory()) {
            String[] children = dir.list();
            for (int i = 0; i < children.length; i++) {
                boolean success = deleteDir(new File(dir, children[i]));
                if (!success) {
                    return false;
                }
            }
        }

        // The directory is now empty so delete it
        return dir.delete();
    }

    // launches internal diff viewer
    public static void launchDiffViewer(Context context, Integer changeNumber,
                                        Integer patchSetNumber, String filePath) {
        Intent diffIntent = new Intent(context, DiffViewer.class);
        diffIntent.putExtra(DiffViewer.CHANGE_NUMBER_TAG, changeNumber);
        diffIntent.putExtra(DiffViewer.PATCH_SET_NUMBER_TAG, patchSetNumber);
        diffIntent.putExtra(DiffViewer.FILE_PATH_TAG, filePath);
        context.startActivity(diffIntent);
    }

    public static void launchDiffInBrowser(Context context, Integer changeNumber, Integer patchset,
                                           String filePath) {
        Intent browserIntent;
        if (patchset == null || filePath == null) {
            Tools.getWebAddress(context, changeNumber);
            browserIntent = new Intent(Intent.ACTION_VIEW,
                    Uri.parse(Tools.getWebAddress(context, changeNumber)));
        } else {
            String base =  "%s#/c/%d/%d/%s,unified";
            browserIntent = new Intent(
                    Intent.ACTION_VIEW, Uri.parse(String.format(base,
                    PrefsFragment.getCurrentGerrit(context),
                    changeNumber)));
        }

        context.startActivity(browserIntent);
    }

    public static void launchDiffOptionDialog(final Context context, final Integer changeNumber,
                                              final Integer patchset,
                                              final String filePath) {
        View checkBoxView = View.inflate(context, R.layout.diff_option_checkbox, null);
        final CheckBox checkBox = (CheckBox) checkBoxView.findViewById(R.id.checkbox);

        AlertDialog.Builder ad = new AlertDialog.Builder(context)
                .setTitle(R.string.choose_diff_view)
                .setView(checkBoxView)
                .setPositiveButton(R.string.context_menu_view_diff_viewer, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        if (checkBox.isChecked()) {
                            PrefsFragment.setDiffDefault(context, PrefsFragment.DiffModes.INTERNAL);
                        }
                        launchDiffViewer(context, changeNumber, patchset, filePath);
                    }
                })
                .setNegativeButton(
                        R.string.context_menu_diff_view_in_browser, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                if (checkBox.isChecked()) {
                                    PrefsFragment.setDiffDefault(context, PrefsFragment.DiffModes.EXTERNAL);
                                }
                                launchDiffInBrowser(context, changeNumber, patchset, filePath);
                            }
                        });
        ad.create().show();
    }
}
