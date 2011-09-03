/**
   Copyright: 2011 Android Aalto

   This file is part of BookingRoom.

   BookingRoom is free software; you can redistribute it and/or modify
   it under the terms of the GNU General Public License as published by
   the Free Software Foundation; either version 3 of the License, or
   (at your option) any later version.

   BookingRoom is distributed in the hope that it will be useful,
   but WITHOUT ANY WARRANTY; without even the implied warranty of
   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
   GNU General Public License for more details.

   You should have received a copy of the GNU General Public License
   along with BookingRoom. If not, see <http://www.gnu.org/licenses/>.
 */

/* 
 * Notice: Code modified from com.android.calendar.CalendarView class.
 *         The original code can be found at: http://android.git.kernel.org/?p=platform/packages/apps/Calendar.git;a=summary
 */

package org.androidaalto.bookingroom.view;

import org.androidaalto.bookingroom.MeetingActivity;
import org.androidaalto.bookingroom.R;
import org.androidaalto.bookingroom.logic.MeetingInfo;
import org.androidaalto.bookingroom.logic.MeetingManager;

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Path;
import android.graphics.Path.Direction;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.text.format.DateUtils;
import android.text.format.Time;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class WeekView extends View {

    private static final String TAG = WeekView.class.getSimpleName();

    static private class DayHeader {
        int cell;
        String dateString;
    }

    private DayHeader[] dayHeaders = new DayHeader[32];

    // For drawing to an off-screen Canvas
    private Bitmap mOffscreenBitmap;
    private Canvas mOffscreenCanvas;

    private boolean mRedrawScreen = true;
    private boolean mRemeasure = true;
    private int mBitmapHeight;
    private int mGridAreaHeight;
    private int mCellHeight;
    private int mScrollStartY;
    private int mPreviousDirection;
    private int mPreviousDistanceX;
    private int mNumHours = 10;

    // Pre-allocate these objects and re-use them
    private Rect mRect = new Rect();
    private RectF mRectF = new RectF();
    private Paint mPaintBorder = new Paint();
    private Rect mSrcRect = new Rect();
    private Rect mDestRect = new Rect();
    private Paint mPaint = new Paint();
    private Paint mEventTextPaint = new Paint();
    private Path mPath = new Path();
    private Paint mSelectionPaint = new Paint();

    Time mBaseDate;
    private Time mCurrentTime;
    private int mHoursWidth;
    private int mEventTextAscent;
    private int mEventTextHeight;
    private String[] mHourStrs = {
            "00", "01", "02", "03", "04", "05",
            "06", "07", "08", "09", "10", "11", "12", "13", "14", "15", "16",
            "17", "18", "19", "20", "21", "22", "23", "00"
    };
    private String[] mDayStrs;
    private String[] mDayStrs2Letter;
    private Resources mResources;
    private int mViewWidth;
    private int mViewHeight;
    private int mCellWidth;
    private int mNumDays = 7;
    private int mViewStartX;
    private int mViewStartY;
    private int mMaxViewStartY;
    private int mFirstCell;
    private int mFirstHour = -1;
    private int mFirstHourOffset;
    private int mHoursTextHeight;
    private int mBannerPlusMargin;
    private int mFirstJulianDay;
    private int mLastJulianDay;
    private int mStartDay;
    private int mDateStrWidth;
    private int mFirstDate;
    private int mMonthLength;

    private List<MeetingInfo> mMeetings;

    /**
     * Selected Julian day
     */
    private int mSelectionDay;
    private int mSelectionHour;
    private ArrayList<MeetingGeometry> mSelectedMeetings = new ArrayList<MeetingGeometry>();
    private boolean mComputeSelectedMeeting;
    private MeetingInfo mSelectedMeetingInfo;
    private MeetingGeometry mSelectedMeetingGeometry;

    // private MeetingGeometry mMeetingGeometry;

    private static float mScale = 0; // Used for supporting different screen
                                     // densities

    private static int HOURS_FONT_SIZE = 12;
    private static int NORMAL_FONT_SIZE = 12;
    private static int EVENT_TEXT_FONT_SIZE = 12;
    private static int MIN_CELL_WIDTH_FOR_TEXT = 27;
    private static float MIN_EVENT_HEIGHT = 15.0F; // in pixels
    private static float SMALL_ROUND_RADIUS = 3.0F;

    /**
     * The initial state of the touch mode when we enter this view.
     */
    private static final int TOUCH_MODE_INITIAL_STATE = 0;

    /**
     * Indicates we just received the touch event and we are waiting to see if
     * it is a tap or a scroll gesture.
     */
    private static final int TOUCH_MODE_DOWN = 1;

    /**
     * Indicates the touch gesture is a vertical scroll
     */
    private static final int TOUCH_MODE_VSCROLL = 0x20;

    /**
     * Indicates the touch gesture is a horizontal scroll
     */
    private static final int TOUCH_MODE_HSCROLL = 0x40;

    private int mTouchMode = TOUCH_MODE_INITIAL_STATE;

    /**
     * The selection modes are HIDDEN, PRESSED, SELECTED, and LONGPRESS.
     */
    private static final int SELECTION_HIDDEN = 0;
    private static final int SELECTION_PRESSED = 1;
    private static final int SELECTION_SELECTED = 2;
    private static final int SELECTION_LONGPRESS = 3;

    private int mSelectionMode = SELECTION_HIDDEN;

    private static int HORIZONTAL_SCROLL_THRESHOLD = 50;

    /* The extra space to leave above the text in normal events */
    private static final int NORMAL_TEXT_TOP_MARGIN = 2;
    private static final int DAY_GAP = 1;
    private static final int HOUR_GAP = 1;

    private static final int HOURS_LEFT_MARGIN = 2;
    private static final int HOURS_RIGHT_MARGIN = 4;
    private static final int HOURS_MARGIN = HOURS_LEFT_MARGIN + HOURS_RIGHT_MARGIN;
    private static final int MAX_EVENT_TEXT_LEN = 500;

    public static final int MINUTES_PER_HOUR = 60;
    public static final int MINUTES_PER_DAY = MINUTES_PER_HOUR * 24;

    private static int mGridAreaBackgroundColor;
    private static int mGridAreaSelectedColor;
    private static int mHourBackgroundColor;
    private static int mHourLabelColor;
    private static int mGridLineHorizontalColor;
    private static int mGridLineVerticalColor;
    private static int mDateBannerBackgroundColor;
    private static int mDateBannerTextColor;
    private static int mSaturdayColor;
    private static int mSundayColor;
    private static int mEventTextColor;
    private static int mMeetingBackgroundColor;
    private static int mHourSelectedColor;
    private static int mCalendarDateSelected;

    private float[] mCharWidths = new float[MAX_EVENT_TEXT_LEN];

    private Pattern drawTextSanitizerFilter = Pattern.compile("[\t\n],");

    private GestureDetector mGestureDetector;

    private boolean mOnFlingCalled;

    private boolean mScrolling;

    private Map<MeetingGeometry, MeetingInfo> mMeetingsGeometryInfoMap = new HashMap<MeetingGeometry, MeetingInfo>();

    private Context mContext;

    /**
     * @param context
     */
    public WeekView(Context context) {
        super(context);
        init(context);
    }

    /**
     * @param context
     * @param attrs
     */
    public WeekView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    /**
     * @param context
     * @param attrs
     * @param defStyle
     */
    public WeekView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context);
    }

    /**
     * Initializes all the parameters needed to draw this view.
     * 
     * @param context
     */
    private void init(Context context) {
        mContext = context;

        calculateScaleFonts();

        MeetingGeometry.setMinEventHeight(MIN_EVENT_HEIGHT);
        MeetingGeometry.setHourGap(HOUR_GAP);

        mResources = this.getContext().getResources();

        mGestureDetector = new GestureDetector(context, new GestureListener(this));

        initTimeAndDates();

        initColors();

        initDayStrings();

        recalc();

        loadMeetings(mBaseDate);
    }

    /**
     * 
     */
    private void loadMeetings(Time initialTime) {
        Log.d(TAG, "BEGIN: Loading events on " + initialTime.format("%Y-%m-%d %H:%M:%S"));
        mMeetings = MeetingManager.getMeetings(initialTime, mNumDays);
        mMeetingsGeometryInfoMap = new HashMap<MeetingGeometry, MeetingInfo>();
        for (MeetingInfo meetingInfo : mMeetings) {
            mMeetingsGeometryInfoMap.put(new MeetingGeometry(), meetingInfo);
        }
        Log.d(TAG, "END: Loading events.");
    }

    private void initTimeAndDates() {
        mStartDay = Utils.getFirstDayOfWeek();

        mCurrentTime = new Time();
        long currentTime = System.currentTimeMillis();
        mCurrentTime.set(currentTime);

        mBaseDate = new Time();
        long millis = System.currentTimeMillis();
        mBaseDate.set(millis);

        Paint p = mPaint;
        p.setAntiAlias(true);
        // Figure out how much space we need for the 3-letter names
        // in the worst case.
        p.setTextSize(NORMAL_FONT_SIZE);
        p.setTypeface(Typeface.DEFAULT_BOLD);
        String[] hoursStrs = {
                " 00", " 24"
        };
        mHoursWidth = computeMaxStringWidth(0, hoursStrs, p);
        mHoursWidth += HOURS_MARGIN;
    }

    /**
     * Initializes day strings and calculates the space needed to display them
     */
    private void initDayStrings() {
        // Allocate space for 2 weeks worth of weekday names so that we can
        // easily start the week display at any week day.
        mDayStrs = new String[14];

        // Also create an array of 2-letter abbreviations.
        mDayStrs2Letter = new String[14];

        for (int i = Calendar.SUNDAY; i <= Calendar.SATURDAY; i++) {
            int index = i - Calendar.SUNDAY;
            // e.g. Tue for Tuesday
            mDayStrs[index] = DateUtils.getDayOfWeekString(i, DateUtils.LENGTH_MEDIUM);
            mDayStrs[index + 7] = mDayStrs[index];
            // e.g. Tu for Tuesday
            mDayStrs2Letter[index] = DateUtils.getDayOfWeekString(i, DateUtils.LENGTH_SHORT);

            // If we don't have 2-letter day strings, fall back to 1-letter.
            if (mDayStrs2Letter[index].equals(mDayStrs[index])) {
                mDayStrs2Letter[index] = DateUtils.getDayOfWeekString(i, DateUtils.LENGTH_SHORTEST);
            }

            mDayStrs2Letter[index + 7] = mDayStrs2Letter[index];
        }

        Paint p = mPaint;
        p.setAntiAlias(true);
        // Figure out how much space we need for the 3-letter names
        // in the worst case.
        p.setTextSize(NORMAL_FONT_SIZE);
        p.setTypeface(Typeface.DEFAULT_BOLD);
        String[] dateStrs = {
                " 28", " 30"
        };
        mDateStrWidth = computeMaxStringWidth(0, dateStrs, p);
        mDateStrWidth += computeMaxStringWidth(0, mDayStrs, p);
    }

    private void initColors() {
        mGridAreaBackgroundColor = mResources.getColor(R.color.calendar_grid_area_background);
        mGridLineHorizontalColor = mResources
                .getColor(R.color.calendar_grid_line_horizontal_color);
        mGridLineVerticalColor = mResources
                .getColor(R.color.calendar_grid_line_vertical_color);
        mHourBackgroundColor = mResources.getColor(R.color.calendar_hour_background);
        mHourLabelColor = mResources.getColor(R.color.calendar_hour_label);
        mDateBannerBackgroundColor = mResources.getColor(R.color.calendar_date_banner_background);
        mSaturdayColor = mResources.getColor(R.color.week_saturday);
        mSundayColor = mResources.getColor(R.color.week_sunday);
        mDateBannerTextColor = mResources.getColor(R.color.calendar_date_banner_text_color);
        mEventTextColor = mResources.getColor(R.color.calendar_event_text_color);
        mMeetingBackgroundColor = mResources.getColor(R.color.meeting_background_color);
        mHourSelectedColor = mResources.getColor(R.color.calendar_hour_selected);
        mGridAreaSelectedColor = mResources.getColor(R.color.calendar_grid_area_selected);
        mCalendarDateSelected = mResources.getColor(R.color.calendar_date_selected);
    }

    private void calculateScaleFonts() {
        if (mScale == 0) {
            mScale = getContext().getResources().getDisplayMetrics().density;
            if (mScale != 1) {
                NORMAL_FONT_SIZE *= mScale;
                EVENT_TEXT_FONT_SIZE *= mScale;
                HOURS_FONT_SIZE *= mScale;
                MIN_CELL_WIDTH_FOR_TEXT *= mScale;
                MIN_EVENT_HEIGHT *= mScale;

                SMALL_ROUND_RADIUS *= mScale;
            }
        }
    }

    /**
     * 
     */
    private void recalc() {
        // Set the base date to the beginning of the week if we are displaying
        // 7 days at a time.
        if (mNumDays == 7) {
            int dayOfWeek = mBaseDate.weekDay;
            int diff = dayOfWeek - mStartDay;
            if (diff != 0) {
                if (diff < 0) {
                    diff += 7;
                }
                mBaseDate.monthDay -= diff;
            }
            mBaseDate.hour = 0;
            mBaseDate.minute = 0;
            mBaseDate.second = 0;
            mBaseDate.normalize(true /* ignore isDst */);
        }

        long start = mBaseDate.normalize(true /* use isDst */);
        mFirstJulianDay = Time.getJulianDay(start, mBaseDate.gmtoff);
        mLastJulianDay = mFirstJulianDay + mNumDays - 1;

        mMonthLength = mBaseDate.getActualMaximum(Time.MONTH_DAY);
        mFirstDate = mBaseDate.monthDay;
    }

    private int computeMaxStringWidth(int currentMax, String[] strings, Paint p) {
        float maxWidthF = 0.0f;

        int len = strings.length;
        for (int i = 0; i < len; i++) {
            float width = p.measureText(strings[i]);
            maxWidthF = Math.max(width, maxWidthF);
        }
        int maxWidth = (int) (maxWidthF + 0.5);
        if (maxWidth < currentMax) {
            maxWidth = currentMax;
        }
        return maxWidth;
    }

    /*
     * (non-Javadoc)
     * @see android.view.View#onDraw(android.graphics.Canvas)
     */
    @Override
    protected void onDraw(Canvas viewCanvas) {
        Log.d(TAG, "WeekView.onDraw()");
        if (mRemeasure) {
            remeasure(getWidth(), getHeight());
            mRemeasure = false;
        }

        if (mRedrawScreen && mOffscreenCanvas != null) {
            drawFullWeekView(mOffscreenCanvas);
            mRedrawScreen = false;
        }

        // TODO: handle scrolling

        if (mOffscreenBitmap != null) {
            copyBitmapToCanvas(mOffscreenBitmap, viewCanvas);
        }

        drawFixedAreas(viewCanvas);
    }

    /**
     * @param viewCanvas
     */
    private void drawFixedAreas(Canvas canvas) {
        Paint p = mPaint;
        Rect r = mRect;

        if (mNumDays > 1) {
            drawDayHeaderLoop(r, canvas, p);
        }
    }

    /**
     * @param r
     * @param canvas
     * @param p
     */
    private void drawDayHeaderLoop(Rect r, Canvas canvas, Paint p) {
        clearDayBannerBackground(r, canvas, p);

        // Draw a highlight on the selected day (if any), but only if we are
        // displaying more than one day.
        if (mSelectionMode != SELECTION_HIDDEN) {
            if (mNumDays > 1) {
                p.setColor(mCalendarDateSelected);
                r.top = 0;
                r.bottom = mBannerPlusMargin;
                int daynum = mSelectionDay - mFirstJulianDay;
                r.left = mHoursWidth + daynum * (mCellWidth + DAY_GAP);
                r.right = r.left + mCellWidth;
                canvas.drawRect(r, p);
            }
        }

        p.setTextSize(NORMAL_FONT_SIZE);
        p.setTextAlign(Paint.Align.CENTER);
        int x = mHoursWidth;
        int deltaX = mCellWidth + DAY_GAP;
        int cell = mFirstJulianDay;

        String[] dayNames;
        if (mDateStrWidth < mCellWidth) {
            dayNames = mDayStrs;
        } else {
            dayNames = mDayStrs2Letter;
        }

        p.setTypeface(Typeface.DEFAULT_BOLD);
        p.setAntiAlias(true);
        for (int day = 0; day < mNumDays; day++, cell++) {
            drawDayHeader(dayNames[day + mStartDay], day, cell, x, canvas, p);
            x += deltaX;
        }

    }

    private void clearDayBannerBackground(Rect r, Canvas canvas, Paint p) {
        p.setColor(mDateBannerBackgroundColor);
        r.top = 0;
        r.bottom = mBannerPlusMargin;
        r.left = 0;
        r.right = mHoursWidth + mNumDays * (mCellWidth + DAY_GAP);
        canvas.drawRect(r, p);

        // Fill the extra space on the right side with the default background
        r.left = r.right;
        r.right = mViewWidth;
        p.setColor(mGridAreaBackgroundColor);
        canvas.drawRect(r, p);
    }

    /**
     * @param string
     * @param day
     * @param cell
     * @param x
     * @param canvas
     * @param p
     */
    private void drawDayHeader(String dateStr, int day, int cell, int x, Canvas canvas, Paint p) {
        float xCenter = x + mCellWidth / 2.0f;

        if (Utils.isSaturday(day, mStartDay)) {
            p.setColor(mSaturdayColor);
        } else if (Utils.isSunday(day, mStartDay)) {
            p.setColor(mSundayColor);
        } else {
            p.setColor(mDateBannerTextColor);
        }

        int dateNum = mFirstDate + day;
        if (dateNum > mMonthLength) {
            dateNum -= mMonthLength;
        }

        String dateNumStr;
        // Add a leading zero if the date is a single digit
        if (dateNum < 10) {
            dateNumStr = "0" + dateNum;
        } else {
            dateNumStr = String.valueOf(dateNum);
        }

        DayHeader header = dayHeaders[day];
        if (header == null || header.cell != cell) {
            // The day header string is regenerated on every draw during drag
            // and fling animation.
            // Caching day header since formatting the string takes surprising
            // long time.

            dayHeaders[day] = new DayHeader();
            dayHeaders[day].cell = cell;
            dayHeaders[day].dateString = getResources().getString(
                    R.string.weekday_day, dateStr, dateNumStr);
        }
        dateStr = dayHeaders[day].dateString;

        float y = mBannerPlusMargin - 7;
        canvas.drawText(dateStr, xCenter, y, p);
    }

    /**
     * @param bitmap
     * @param canvas
     */
    private void copyBitmapToCanvas(Bitmap bitmap, Canvas canvas) {
        // Copy the scrollable region from the big bitmap to the canvas.
        Rect src = mSrcRect;
        Rect dest = mDestRect;

        src.top = mViewStartY;
        src.bottom = mViewStartY + mGridAreaHeight;
        src.left = 0;
        src.right = mViewWidth;

        dest.top = mFirstCell;
        dest.bottom = mViewHeight;
        dest.left = 0;
        dest.right = mViewWidth;

        Log.d(TAG, "copying bitmap to screen: " );
        Log.d(TAG, "\ttop: " + src.top);
        Log.d(TAG, "\tbottom: " + src.bottom);
        canvas.save();
        canvas.clipRect(dest);
        canvas.drawColor(0, PorterDuff.Mode.CLEAR);
        canvas.drawBitmap(bitmap, src, dest, null);
        canvas.restore();
    }

    /**
     * @param canvas
     */
    private void drawFullWeekView(Canvas canvas) {
        Log.d(TAG, "BEGIN: Drawing full week");
        Paint p = mPaint;
        Rect r = mRect;
        int lineY = mCurrentTime.hour * (mCellHeight + HOUR_GAP)
                + ((mCurrentTime.minute * mCellHeight) / 60)
                + 1;

        drawGridBackground(r, canvas, p);
        drawHours(r, canvas, p);

        drawWeekMeetings(r, canvas, p, lineY);
        Log.d(TAG, "END: Drawing full week");
    }

    /**
     * @param r
     * @param canvas
     * @param p
     * @param lineY
     */
    private void drawWeekMeetings(Rect r, Canvas canvas, Paint p, int lineY) {
        int x = mHoursWidth;
        int deltaX = mCellWidth + DAY_GAP;
        int currentJulianDay = mFirstJulianDay;
        for (int day = 0; day < mNumDays; day++, currentJulianDay++) {
            drawDayMeetings(currentJulianDay, x, HOUR_GAP, canvas, p);
            x += deltaX;
        }
    }

    /**
     * @param cell
     * @param x
     * @param hourGap
     * @param canvas
     * @param p
     */
    private void drawDayMeetings(int date, int left, int top, Canvas canvas, Paint p) {
        // Draw meetings right to the hours
        Paint eventTextPaint = mEventTextPaint;
        int cellWidth = mCellWidth;
        int cellHeight = mCellHeight;

        // TODO: Check only the meetings for the given date.
        for (MeetingGeometry geometry : mMeetingsGeometryInfoMap.keySet()) {
            MeetingInfo meeting = mMeetingsGeometryInfoMap.get(geometry);
            if (!geometry.computeEventRect(date, left, top, cellWidth, meeting)) {
                continue;
            }

            mMeetingsGeometryInfoMap.put(geometry, meeting);
            RectF rf = drawMeetingRect(geometry, canvas, p, eventTextPaint);
            drawMeetingText(meeting, rf, canvas, eventTextPaint, NORMAL_TEXT_TOP_MARGIN);
        }
    }

    /**
     * @param meeting
     * @param canvas
     * @param p
     * @param eventTextPaint
     * @return
     */
    private RectF drawMeetingRect(MeetingGeometry geometry, Canvas canvas, Paint p,
            Paint eventTextPaint) {
        int color = mMeetingBackgroundColor;

        // TODO: If this event is selected, then use the selection color
        p.setColor(color);
        eventTextPaint.setColor(mEventTextColor);

        RectF rf = mRectF;
        rf.top = geometry.top;
        rf.bottom = geometry.bottom;
        rf.left = geometry.left;
        rf.right = geometry.right - 1;

        Log.d(TAG, "Drawing meeting from (" + rf.left + ", " + rf.top + ") to (" + rf.right + ", "
                + rf.bottom + ").");

        canvas.drawRoundRect(rf, SMALL_ROUND_RADIUS, SMALL_ROUND_RADIUS, p);

        // Draw a darker border
        float[] hsv = new float[3];
        Color.colorToHSV(p.getColor(), hsv);
        hsv[1] = 1.0f;
        hsv[2] *= 0.75f;
        mPaintBorder.setColor(Color.HSVToColor(hsv));
        canvas.drawRoundRect(rf, SMALL_ROUND_RADIUS, SMALL_ROUND_RADIUS, mPaintBorder);

        rf.left += 2;
        rf.right -= 2;

        return rf;
    }

    /**
     * @param meeting
     * @param rf
     * @param canvas
     * @param eventTextPaint
     * @param normalTextTopMargin
     */
    private void drawMeetingText(MeetingInfo meeting, RectF rf, Canvas canvas, Paint p,
            int topMargin) {
        float width = rf.right - rf.left;
        float height = rf.bottom - rf.top;

        // Leave one pixel extra space between lines
        int lineHeight = mEventTextHeight + 1;

        // If the rectangle is too small for text, then return
        if (width < MIN_CELL_WIDTH_FOR_TEXT || height <= lineHeight) {
            return;
        }

        // Truncate the event title to a known (large enough) limit
        String text = meeting.getTitle();

        text = drawTextSanitizer(text);

        int len = text.length();
        if (len > MAX_EVENT_TEXT_LEN) {
            text = text.substring(0, MAX_EVENT_TEXT_LEN);
            len = MAX_EVENT_TEXT_LEN;
        }

        // Figure out how much space the event title will take, and create a
        // String fragment that will fit in the rectangle. Use multiple lines,
        // if available.
        p.getTextWidths(text, mCharWidths);
        String fragment = text;
        float top = rf.top + mEventTextAscent + topMargin;
        int start = 0;

        // Leave one pixel extra space at the bottom
        while (start < len && height >= (lineHeight + 1)) {
            boolean lastLine = (height < 2 * lineHeight + 1);
            // Skip leading spaces at the beginning of each line
            do {
                char c = text.charAt(start);
                if (c != ' ')
                    break;
                start += 1;
            } while (start < len);

            float sum = 0;
            int end = start;
            for (int ii = start; ii < len; ii++) {
                char c = text.charAt(ii);

                // If we found the end of a word, then remember the ending
                // position.
                if (c == ' ') {
                    end = ii;
                }
                sum += mCharWidths[ii];
                // If adding this character would exceed the width and this
                // isn't the last line, then break the line at the previous
                // word. If there was no previous word, then break this word.
                if (sum > width) {
                    if (end > start && !lastLine) {
                        // There was a previous word on this line.
                        fragment = text.substring(start, end);
                        start = end;
                        break;
                    }

                    // This is the only word and it is too long to fit on
                    // the line (or this is the last line), so take as many
                    // characters of this word as will fit.
                    fragment = text.substring(start, ii);
                    start = ii;
                    break;
                }
            }

            // If sum <= width, then we can fit the rest of the text on
            // this line.
            if (sum <= width) {
                fragment = text.substring(start, len);
                start = len;
            }

            canvas.drawText(fragment, rf.left + 1, top, p);

            top += lineHeight;
            height -= lineHeight;
        }
    }

    /**
     * @param r
     * @param canvas
     * @param p
     */
    private void drawHours(Rect r, Canvas canvas, Paint p) {
        clearHourBackground(r, canvas, p);

        // Draw a highlight on the selected hour (if needed)
        if (mSelectionMode != SELECTION_HIDDEN) {
            p.setColor(mHourSelectedColor);
            r.top = mSelectionHour * (mCellHeight + HOUR_GAP);
            r.bottom = r.top + mCellHeight + 2 * HOUR_GAP;
            r.left = 0;
            r.right = mHoursWidth;
            canvas.drawRect(r, p);

            boolean drawBorder = false;
            if (!drawBorder) {
                r.top += HOUR_GAP;
                r.bottom -= HOUR_GAP;
            }

            // Also draw the highlight on the grid
            p.setColor(mGridAreaSelectedColor);
            int daynum = mSelectionDay - mFirstJulianDay;
            r.left = mHoursWidth + daynum * (mCellWidth + DAY_GAP);
            r.right = r.left + mCellWidth;
            canvas.drawRect(r, p);

            // Draw a border around the highlighted grid hour.
            if (drawBorder) {
                Path path = mPath;
                r.top += HOUR_GAP;
                r.bottom -= HOUR_GAP;
                path.reset();
                path.addRect(r.left, r.top, r.right, r.bottom, Direction.CW);
                canvas.drawPath(path, mSelectionPaint);
            }

            // TODO: saveSelectionPosition
        }

        p.setColor(mHourLabelColor);
        p.setTextSize(HOURS_FONT_SIZE);
        p.setTypeface(Typeface.DEFAULT_BOLD);
        p.setTextAlign(Paint.Align.RIGHT);
        p.setAntiAlias(true);

        int right = mHoursWidth - HOURS_RIGHT_MARGIN;
        int y = HOUR_GAP + mHoursTextHeight;

        for (int i = 0; i < 24; i++) {
            String time = mHourStrs[i];
            canvas.drawText(time, right, y, p);
            y += mCellHeight + HOUR_GAP;
        }
    }

    private void clearHourBackground(Rect r, Canvas canvas, Paint p) {
        p.setColor(mHourBackgroundColor);
        r.top = 0;
        r.bottom = 24 * (mCellHeight + HOUR_GAP) + HOUR_GAP;
        r.left = 0;
        r.right = mHoursWidth;
        canvas.drawRect(r, p);
        // Fill the bottom left corner with the default grid background
        r.top = r.bottom;
        r.bottom = mBitmapHeight;
        p.setColor(mGridAreaBackgroundColor);
        canvas.drawRect(r, p);
    }

    /**
     * @param r
     * @param canvas
     * @param p
     */
    private void drawGridBackground(Rect r, Canvas canvas, Paint p) {
        Paint.Style savedStyle = p.getStyle();

        clearViewBackground(r, canvas, p);

        drawHorizontalGridLines(canvas, p);

        drawVerticalGridLines(canvas, p);

        // Restore the saved style.
        p.setStyle(savedStyle);
        p.setAntiAlias(true);
    }

    private void drawVerticalGridLines(Canvas canvas, Paint p) {
        p.setColor(mGridLineVerticalColor);
        p.setStyle(Style.STROKE);
        p.setStrokeWidth(0);
        p.setAntiAlias(false);
        float startY = 0;
        float stopY = HOUR_GAP + 24 * (mCellHeight + HOUR_GAP);
        float deltaX = mCellWidth + DAY_GAP;
        float x = mHoursWidth + mCellWidth;
        for (int day = 0; day < mNumDays; day++) {
            Log.d(TAG, "Drawing day " + day + " line at: " + x);
            canvas.drawLine(x, startY, x, stopY, p);
            x += deltaX;
        }
    }

    private void drawHorizontalGridLines(Canvas canvas, Paint p) {
        p.setColor(mGridLineHorizontalColor);
        p.setStyle(Style.STROKE);
        p.setStrokeWidth(0);
        p.setAntiAlias(false);
        float startX = mHoursWidth;
        float stopX = mHoursWidth + (mCellWidth + DAY_GAP) * mNumDays;
        float y = 0;
        float deltaY = mCellHeight + HOUR_GAP;
        for (int hour = 0; hour <= 24; hour++) {
            Log.d(TAG, "Drawing hour " + hour + " line at: " + y);
            canvas.drawLine(startX, y, stopX, y, p);
            y += deltaY;
        }
    }

    private void clearViewBackground(Rect r, Canvas canvas, Paint p) {
        p.setColor(mGridAreaBackgroundColor);
        r.top = 0;
        r.bottom = mBitmapHeight;
        r.left = 0;
        r.right = mViewWidth;
        canvas.drawRect(r, p);
    }

    /**
     * @param width
     * @param height
     */
    private void remeasure(int width, int height) {
        mGridAreaHeight = height - mFirstCell;
        mCellHeight = (mGridAreaHeight - ((mNumHours + 1) * HOUR_GAP)) / mNumHours;
        mMaxViewStartY = mBitmapHeight - mGridAreaHeight;
        int usedGridAreaHeight = (mCellHeight + HOUR_GAP) * mNumHours + HOUR_GAP;
        int bottomSpace = mGridAreaHeight - usedGridAreaHeight;
        MeetingGeometry.setHourHeight(mCellHeight);

        mFirstCell = mBannerPlusMargin;

        createOffscreenBitmapAndCanvas(width, bottomSpace);

        if (mFirstHour == -1) {
            initFirstHour();
            mFirstHourOffset = 0;
        }
        
        mViewStartY = mFirstHour * (mCellHeight + HOUR_GAP) - mFirstHourOffset;
    }

    private void createOffscreenBitmapAndCanvas(int width, int bottomSpace) {
        // Create an off-screen bitmap that we can draw into.
        mBitmapHeight = HOUR_GAP + 24 * (mCellHeight + HOUR_GAP) + bottomSpace;
        if ((mOffscreenBitmap == null || mOffscreenBitmap.getHeight() < mBitmapHeight) && width > 0
                &&
                mBitmapHeight > 0) {
            if (mOffscreenBitmap != null) {
                mOffscreenBitmap.recycle();
            }
            mOffscreenBitmap = Bitmap.createBitmap(width, mBitmapHeight, Bitmap.Config.RGB_565);
            mOffscreenCanvas = new Canvas(mOffscreenBitmap);
        }
    }

    private void initFirstHour() {
        mFirstHour = mSelectionHour - mNumHours / 2;
        if (mFirstHour < 0) {
            mFirstHour = 0;
        } else if (mFirstHour + mNumHours > 24) {
            mFirstHour = 24 - mNumHours;
        }
    }

    @Override
    protected void onSizeChanged(int width, int height, int oldw, int oldh) {
        mViewWidth = width;
        mViewHeight = height;
        int gridAreaWidth = width - mHoursWidth;
        mCellWidth = (gridAreaWidth - (mNumDays * DAY_GAP)) / mNumDays;

        Paint p = new Paint();
        p.setTextSize(HOURS_FONT_SIZE);
        mHoursTextHeight = (int) Math.abs(p.ascent());

        p.setTextSize(NORMAL_FONT_SIZE);
        int bannerTextHeight = (int) Math.abs(p.ascent());
        if (mNumDays > 1) {
            mBannerPlusMargin = bannerTextHeight + 14;
        } else {
            mBannerPlusMargin = 0;
        }

        p.setTextSize(EVENT_TEXT_FONT_SIZE);
        float ascent = -p.ascent();
        mEventTextAscent = (int) Math.ceil(ascent);
        float totalHeight = ascent + p.descent();
        mEventTextHeight = (int) Math.ceil(totalHeight);

        remeasure(width, height);
    }

    // Sanitize a string before passing it to drawText or else we get little
    // squares. For newlines and tabs before a comma, delete the character.
    // Otherwise, just replace them with a space.
    private String drawTextSanitizer(String string) {
        Matcher m = drawTextSanitizerFilter.matcher(string);
        string = m.replaceAll(",").replace('\n', ' ').replace('\n', ' ');
        return string;
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        int action = ev.getAction();

        switch (action) {
            case MotionEvent.ACTION_DOWN:
                mGestureDetector.onTouchEvent(ev);
                return true;

            case MotionEvent.ACTION_MOVE:
                mGestureDetector.onTouchEvent(ev);
                return true;

            case MotionEvent.ACTION_UP:
                mGestureDetector.onTouchEvent(ev);
                if (mOnFlingCalled) {
                    return true;
                }
                if ((mTouchMode & TOUCH_MODE_HSCROLL) != 0) {
                    mTouchMode = TOUCH_MODE_INITIAL_STATE;
                    if (Math.abs(mViewStartX) > HORIZONTAL_SCROLL_THRESHOLD) {
                        // TODO: The user has gone beyond the threshold so
                        // switch
                        // views
                        mViewStartX = 0;
                        return true;
                    } else {
                        // Not beyond the threshold so invalidate which will
                        // cause
                        // the view to snap back. Also call recalc() to ensure
                        // that we have the correct starting date and title.
                        recalc();
                        invalidate();
                        mViewStartX = 0;
                    }
                }

                // If we were scrolling, then reset the selected hour so that it
                // is visible.
                if (mScrolling) {
                    mScrolling = false;
                    resetSelectedHour();
                    mRedrawScreen = true;
                    invalidate();
                }
                return true;

                // This case isn't expected to happen.
            case MotionEvent.ACTION_CANCEL:
                mGestureDetector.onTouchEvent(ev);
                mScrolling = false;
                resetSelectedHour();
                return true;

            default:
                if (mGestureDetector.onTouchEvent(ev)) {
                    return true;
                }
                return super.onTouchEvent(ev);
        }
    }

    // This is called after scrolling stops to move the selected hour
    // to the visible part of the screen.
    private void resetSelectedHour() {
        if (mSelectionHour < mFirstHour + 1) {
            mSelectionHour = mFirstHour + 1;
            mSelectedMeetingInfo = null;
            mSelectedMeetings.clear();
            mComputeSelectedMeeting = true;
        } else if (mSelectionHour > mFirstHour + mNumHours - 3) {
            mSelectionHour = mFirstHour + mNumHours - 3;
            mSelectedMeetingInfo = null;
            mSelectedMeetings.clear();
            mComputeSelectedMeeting = true;
        }
    }

    /**
     * @param ev
     */
    public void doSingleTapUp(MotionEvent ev) {
        int x = (int) ev.getX();
        int y = (int) ev.getY();
        int selectedDay = mSelectionDay;
        int selectedHour = mSelectionHour;

        boolean validPosition = setSelectionFromPosition(x, y);
        if (!validPosition) {
            // return if the touch wasn't on an area of concern
            return;
        }

        mSelectionMode = SELECTION_SELECTED;
        mRedrawScreen = true;
        invalidate();

        if (mSelectedMeetingInfo != null) {
            // If the tap is on an event, launch the "View meeting" view to edit
            // it
        } else if (mSelectedMeetingInfo == null && selectedDay == mSelectionDay
                && selectedHour == mSelectionHour) {
            // If the tap is on an already selected hour slot, then jump to
            // "View meeting"
            switchToAddMeetingView();
        }
    }

    /**
     * 
     */
    private void switchToAddMeetingView() {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setClassName(mContext, MeetingActivity.class.getName());
        mContext.startActivity(intent);
    }

    /**
     * Sets mSelectionDay and mSelectionHour based on the (x,y) touch position.
     * If the touch position is not within the displayed grid, then this method
     * returns false.
     * 
     * @param x the x position of the touch
     * @param y the y position of the touch
     * @return true if the touch position is valid
     */
    private boolean setSelectionFromPosition(int x, int y) {
        if (x < mHoursWidth) {
            return false;
        }

        int day = (x - mHoursWidth) / (mCellWidth + DAY_GAP);
        if (day >= mNumDays) {
            day = mNumDays - 1;
        }
        day += mFirstJulianDay;
        int hour = (y - mFirstCell - mFirstHourOffset) / (mCellHeight + HOUR_GAP);
        hour += mFirstHour;
        mSelectionHour = hour;
        mSelectionDay = day;
        findSelectedEvent(x, y);
        Log.i("Cal", "setSelectionFromPosition( " + x + ", " + y + " ) day: "
                + day
                + " hour: " + hour
                + " mFirstCell: " + mFirstCell + " mFirstHourOffset: " +
                mFirstHourOffset);
        if (mSelectedMeetingInfo != null) {
            Log.i(TAG, "  num events: " + mSelectedMeetings.size() + " event: "
                    + mSelectedMeetingInfo.getTitle());
        }
        return true;
    }

    /**
     * @param x
     * @param y
     */
    private void findSelectedEvent(int x, int y) {
        int date = mSelectionDay;
        int cellWidth = mCellWidth;
        Set<MeetingGeometry> events = mMeetingsGeometryInfoMap.keySet();
        int left = mHoursWidth + (mSelectionDay - mFirstJulianDay) * (cellWidth + DAY_GAP);
        int top = 0;
        mSelectedMeetingInfo = null;

        mSelectedMeetings.clear();

        // Adjust y for the scrollable bitmap
        y += mViewStartY - mFirstCell;

        // Use a region around (x,y) for the selection region
        Rect region = mRect;
        region.left = x - 10;
        region.right = x + 10;
        region.top = y - 10;
        region.bottom = y + 10;

        for (MeetingGeometry geometry : events) {
            MeetingInfo event = mMeetingsGeometryInfoMap.get(geometry);
            // Compute the event rectangle.
            if (!geometry.computeEventRect(date, left, top, cellWidth, event)) {
                continue;
            }

            // If the event intersects the selection region, then add it to
            // mSelectedMeetings.
            if (geometry.eventIntersectsSelection(region)) {
                mSelectedMeetings.add(geometry);
            }
        }

        // If there are any events in the selected region, then assign the
        // closest one to mSelectedMeeting.
        if (mSelectedMeetings.size() > 0) {
            int len = mSelectedMeetings.size();
            MeetingInfo closestMeetingInfo = null;
            MeetingGeometry closestMeetingGeometry = null;
            float minDist = mViewWidth + mViewHeight; // some large distance
            for (int index = 0; index < len; index++) {
                MeetingGeometry geometry = mSelectedMeetings.get(index);
                float dist = geometry.pointToEvent(x, y);
                if (dist < minDist) {
                    minDist = dist;
                    closestMeetingGeometry = geometry;
                    closestMeetingInfo = mMeetingsGeometryInfoMap.get(geometry);
                }
            }
            mSelectedMeetingInfo = closestMeetingInfo;
            mSelectedMeetingGeometry = closestMeetingGeometry;

            // Keep the selected hour and day consistent with the selected
            // event. That is, snap it to the closest one. They could be
            // different if we touched on an empty hour
            // slot very close to an event in the previous hour slot. In
            // that case we will select the nearby event.
            int startDay = mSelectedMeetingInfo.getStartDay();
            int endDay = mSelectedMeetingInfo.getEndDay();
            if (mSelectionDay < startDay) {
                mSelectionDay = startDay;
            } else if (mSelectionDay > endDay) {
                mSelectionDay = endDay;
            }

            int startHour = mSelectedMeetingInfo.getStart().hour;
            int endHour = mSelectedMeetingInfo.getEnd().hour;
            if (mSelectionHour < startHour) {
                mSelectionHour = startHour;
            } else if (mSelectionHour > endHour) {
                mSelectionHour = endHour;
            }
        }
    }

    /**
     * @param ev
     */
    public void doLongPress(MotionEvent ev) {
        int x = (int) ev.getX();
        int y = (int) ev.getY();

        boolean validPosition = setSelectionFromPosition(x, y);
        if (!validPosition) {
            // return if the touch wasn't on an area of concern
            return;
        }

        mSelectionMode = SELECTION_LONGPRESS;
        mRedrawScreen = true;
        invalidate();

        if (mSelectedMeetingInfo != null) {
            // If the tap is on an event, launch the "View meeting" view to edit
            // it
        } else if (mSelectedMeetingInfo == null) {
            switchToAddMeetingView();
        }
    }

    /**
     * @param e1
     * @param e2
     * @param distanceX
     * @param distanceY
     */
    public void doScroll(MotionEvent e1, MotionEvent e2, float deltaX, float deltaY) {
        Log.d(TAG, "WeekView.doScroll()");
        // Use the distance from the current point to the initial touch instead
        // of deltaX and deltaY to avoid accumulating floating-point rounding
        // errors. Also, we don't need floats, we can use ints.
        int distanceX = (int) e1.getX() - (int) e2.getX();
        int distanceY = (int) e1.getY() - (int) e2.getY();

        // If we haven't figured out the predominant scroll direction yet,
        // then do it now.
        if (mTouchMode == TOUCH_MODE_DOWN) {
            int absDistanceX = Math.abs(distanceX);
            int absDistanceY = Math.abs(distanceY);
            mScrollStartY = mViewStartY;
            mPreviousDistanceX = 0;
            mPreviousDirection = 0;

            // If the x distance is at least twice the y distance, then lock
            // the scroll horizontally. Otherwise scroll vertically.
            if (absDistanceX >= 2 * absDistanceY) {
                Log.d(TAG, "Horizontal scroll");
                mTouchMode = TOUCH_MODE_HSCROLL;
                mViewStartX = distanceX;
                // TODO: initNextView;
            } else {
                Log.d(TAG, "Vertical scroll");
                mTouchMode = TOUCH_MODE_VSCROLL;
            }
        } else if ((mTouchMode & TOUCH_MODE_HSCROLL) != 0) {
            // We are already scrolling horizontally, so check if we
            // changed the direction of scrolling so that the other week
            // is now visible.
            mViewStartX = distanceX;
            if (distanceX != 0) {
                int direction = (distanceX > 0) ? 1 : -1;
                if (direction != mPreviousDirection) {
                    // The user has switched the direction of scrolling
                    // so re-init the next view
                    // TODO: initNextView(-mViewStartX);
                    mPreviousDirection = direction;
                }
            }

            // If we have moved at least the HORIZONTAL_SCROLL_THRESHOLD,
            // then change the title to the new day (or week), but only
            // if we haven't already changed the title.
            // TODO: Change the title
            mPreviousDistanceX = distanceX;
        }

        if ((mTouchMode & TOUCH_MODE_VSCROLL) != 0) {
            mViewStartY = mScrollStartY + distanceY;
            if (mViewStartY < 0) {
                mViewStartY = 0;
            } else if (mViewStartY > mMaxViewStartY) {
                mViewStartY = mMaxViewStartY;
            }
            computeFirstHour();
        }

        mScrolling = true;

        if (mSelectionMode != SELECTION_HIDDEN) {
            mSelectionMode = SELECTION_HIDDEN;
            mRedrawScreen = true;
        }
        Log.d(TAG, "invalidating");
        invalidate();
    }

    /**
     * Recomputes the first full hour that is visible on screen after the screen
     * is scrolled.
     */
    private void computeFirstHour() {
        // Compute the first full hour that is visible on screen
        mFirstHour = (mViewStartY + mCellHeight + HOUR_GAP - 1) / (mCellHeight + HOUR_GAP);
        mFirstHourOffset = mFirstHour * (mCellHeight + HOUR_GAP) - mViewStartY;
    }

    /**
     * @param e1
     * @param e2
     * @param velocityX
     * @param velocityY
     */
    public void doFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
        // TODO Auto-generated method stub

    }

    /**
     * @param ev
     */
    public void doDown(MotionEvent ev) {
        mTouchMode = TOUCH_MODE_DOWN;
        mViewStartX = 0;
        mOnFlingCalled = false;
//        getHandler().removeCallbacks(mContinueScroll);
    }
}
