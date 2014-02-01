package com.conceptualideas.cyanlockscreen;

import android.graphics.*;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.SystemClock;
import android.view.HapticFeedbackConstants;
import android.view.View;
import com.android.internal.widget.LockPatternUtils;
import com.android.internal.widget.LockPatternView;
import de.robv.android.xposed.XC_MethodHook;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import static de.robv.android.xposed.XposedHelpers.*;

/**
 * Created with IntelliJ IDEA.
 * User: keyston
 * Date: 1/18/14
 * Time: 6:05 PM
 */
public class LockPatternViewHook {


    private float mDiameterFactor = 0.10f; // TODO: move to attr

    private Paint mPaint;
    private Paint mPathPaint;

    private long mAnimatingPeriodStart;

    private int mBitmapWidth;
    private int mBitmapHeight;
    private final float mHitFactor = 0.6f;

    private float mSquareWidth;
    private float mSquareHeight;
    private byte mPatternSize = LockPatternUtilsHook.PATTERN_SIZE_DEFAULT;
    /**
     * How many milliseconds we spend animating each circle of a lock pattern
     * if the animating mode is set.  The entire animation should take this
     * constant * the length of the pattern to complete.
     */
    private static final int MILLIS_PER_CIRCLE_ANIMATING = 7;

    private ArrayList<LockPatternView.Cell> mPattern = new ArrayList<LockPatternView.Cell>(mPatternSize * mPatternSize);


    /**
     * Lookup table for the circles of the pattern we are currently drawing.
     * This will be the cells of the complete pattern unless we are animating,
     * in which case we use this to hold the cells we are drawing for the in
     * progress animation.
     */
    private boolean[][] mPatternDrawLookup = new boolean[mPatternSize][mPatternSize];

    private boolean mVisibleDots = true;
    private boolean mShowErrorPath = true;

    private static Map<Integer, LockPatternViewHook> sDelegates = new HashMap<Integer, LockPatternViewHook>();
    private WeakReference<LockPatternView> view;
    private Bitmap mBitmapBtnDefault;
    private Bitmap mBitmapBtnTouched;
    private Bitmap mBitmapCircleDefault;
    private Bitmap mBitmapCircleGreen;
    private Bitmap mBitmapCircleRed;
    private Bitmap mBitmapArrowGreenUp;
    private Bitmap mBitmapArrowRedUp;
    private Matrix mCircleMatrix;
    private LockPatternUtils mLockPatternUtils;
    private LockPatternView.DisplayMode mPatternDisplayMode;

    public static LockPatternViewHook hook(LockPatternView view) {
        int id = view.getId();
        if (!sDelegates.containsKey(id)) {
            sDelegates.put(id, new LockPatternViewHook());
        }
        LockPatternViewHook delegate = sDelegates.get(id);

        delegate.setView(view);
        return delegate;
    }


    private static LockPatternViewHook hook(XC_MethodHook.MethodHookParam methodHookParam) {
        return hook((LockPatternView) methodHookParam.thisObject);
    }

    public static void init() {
        findAndHookMethod(LockPatternView.class, "clearPatternDrawLookup", new ReplaceMethodHook() {
            @Override
            protected Object replaceHookedMethod(MethodHookParam methodHookParam) throws Throwable {
                hook(methodHookParam).clearPatternDrawLookup();
                return null;
            }
        });

        findAndHookMethod(LockPatternView.class, "onSizeChanged", int.class, int.class, int.class, int.class, new ReplaceMethodHook() {
            @Override
            protected Object replaceHookedMethod(MethodHookParam methodHookParam) throws Throwable {
                Object[] args = methodHookParam.args;
                int w = (Integer) args[0];
                int h = (Integer) args[1];
                int oldW = (Integer) args[2];
                int oldH = (Integer) args[3];
                hook(methodHookParam).onSizeChanged(w, h, oldW, oldH);
                return null;
            }
        });

        findAndHookMethod(LockPatternView.class, "getSuggestedMinimumWidth", new ReplaceMethodHook() {
            @Override
            protected Object replaceHookedMethod(MethodHookParam methodHookParam) throws Throwable {
                return hook(methodHookParam).getSuggestedMinimumWidth();
            }
        });
        findAndHookMethod(LockPatternView.class, "getSuggestedMinimumHeight", new ReplaceMethodHook() {
            @Override
            protected Object replaceHookedMethod(MethodHookParam methodHookParam) throws Throwable {
                return hook(methodHookParam).getSuggestedMinimumHeight();
            }
        });

        findAndHookMethod(LockPatternView.class, "onDraw", Canvas.class, new ReplaceMethodHook() {
            @Override
            protected Object replaceHookedMethod(MethodHookParam methodHookParam) throws Throwable {

                hook(methodHookParam).onDraw((Canvas) methodHookParam.args[0]);
                return null;
            }
        });
        findAndHookMethod(LockPatternView.class, "drawCircle", Canvas.class, int.class, int.class, boolean.class, new ReplaceMethodHook() {
            @Override
            protected Object replaceHookedMethod(MethodHookParam methodHookParam) throws Throwable {
                final Object[] args = methodHookParam.args;
                Canvas canvas = (Canvas) args[0];
                int leftX = (Integer) args[1];
                int topY = (Integer) args[2];
                boolean partOfPattern = (Boolean) args[3];
                hook(methodHookParam).drawCircle(canvas, leftX, topY, partOfPattern);
                return null;
            }
        });

        findAndHookMethod(LockPatternView.class, "detectAndAddHit", float.class, float.class, new ReplaceMethodHook() {
            @Override
            protected Object replaceHookedMethod(MethodHookParam methodHookParam) throws Throwable {
                float x = (Float) methodHookParam.args[0];
                float y = (Float) methodHookParam.args[1];
                return hook(methodHookParam).detectAndAddHit(x, y);
            }
        });

        findAndHookMethod(LockPatternView.class, "checkForNewHit", float.class, float.class, new ReplaceMethodHook() {
            @Override
            protected Object replaceHookedMethod(MethodHookParam methodHookParam) throws Throwable {
                float x = (Float) methodHookParam.args[0];
                float y = (Float) methodHookParam.args[1];
                return hook(methodHookParam).checkForNewHit(x, y);
            }
        });

        findAndHookMethod(LockPatternView.class, "setDisplayMode", LockPatternView.DisplayMode.class, new MethodHook() {
            @Override
            protected void afterHookedMethodX(MethodHookParam param) throws Throwable {
                hook(param).setDisplayMode((LockPatternView.DisplayMode) param.args[0]);

            }
        });
        findAndHookMethod(LockPatternView.class, "resetPattern", new MethodHook() {
            @Override
            protected void afterHookedMethodX(MethodHookParam param) throws Throwable {
                hook(param).resetPattern();

            }
        });

        findAndHookMethod(LockPatternView.class, "onSaveInstanceState", new MethodHook(true) {
            @Override
            protected void afterHookedMethodX(MethodHookParam param) throws Throwable {
                hook(param).onSaveInstanceState(param);
            }
        });
        findAndHookMethod(LockPatternView.class, "onRestoreInstanceState", Parcelable.class, new MethodHook() {
            private SavedState ss;

            @Override
            protected void beforeHookedMethodX(MethodHookParam param) throws Throwable {
                Parcelable state = (Parcelable) param.args[0];
                final SavedState ss = (SavedState) state;
                param.args[0] = ((SavedState) state).getSuperState();

            }

            @Override
            protected void afterHookedMethodX(MethodHookParam param) throws Throwable {
                hook(param).onRestoreInstanceState(ss);


            }
        });
        LockPatternCell.init();

    }

    private void resetPattern() {
        mPattern.clear();
        mPatternDisplayMode = LockPatternView.DisplayMode.Correct;
    }

    public void setDisplayMode(LockPatternView.DisplayMode displayMode) {
        mPatternDisplayMode = displayMode;
        mPattern = (ArrayList<LockPatternView.Cell>) getObjectField(getView(), "mPattern");
        mAnimatingPeriodStart = getLongField(getView(), "mAnimatingPeriodStart");
    }

    /**
     * @return the current pattern lockscreen size.
     */
    public int getLockPatternSize() {
        return mPatternSize;
    }

    public void setVisibleDots(boolean visibleDots) {
        mVisibleDots = visibleDots;
    }

    public boolean isVisibleDots() {
        return mVisibleDots;
    }

    public void setShowErrorPath(boolean showErrorPath) {
        mShowErrorPath = showErrorPath;
    }

    public boolean isShowErrorPath() {
        return mShowErrorPath;
    }

    /**
     * Set the pattern size of the lockscreen
     *
     * @param size The pattern size.
     */
    public void setLockPatternSize(byte size) {
        mPatternSize = size;
        LockPatternCell.updateSize(size);
        mPattern = new ArrayList<LockPatternView.Cell>(size * size);

        mPatternDrawLookup = new boolean[size][size];
        setPatternDrawLookupField();
        setObjectField(getView(), "mPattern", mPattern);

    }


    /**
     * Set the LockPatternUtil instance used to encode a pattern to a string
     *
     * @param utils The instance.
     */
    public void setLockPatternUtils(LockPatternUtils utils) {
        mLockPatternUtils = utils;
    }

    private void setPatternDrawLookupField() {
        setObjectField(getView(), "mPatternDrawLookup", mPatternDrawLookup);
    }

    private LockPatternView getView() {
        return view.get();
    }

    public void setView(LockPatternView view) {
        this.view = new WeakReference<LockPatternView>(view);

        mBitmapWidth = getIntField(view, "mBitmapWidth");
        mBitmapHeight = getIntField(view, "mBitmapHeight");
        mPaint = (Paint) getObjectField(view, "mPaint");
        mPathPaint = (Paint) getObjectField(view, "mPathPaint");

        mBitmapBtnDefault = (Bitmap) getObjectField(view, "mBitmapBtnDefault");
        mBitmapBtnTouched = (Bitmap) getObjectField(view, "mBitmapBtnTouched");
        mBitmapCircleDefault = (Bitmap) getObjectField(view, "mBitmapCircleDefault");
        mBitmapCircleGreen = (Bitmap) getObjectField(view, "mBitmapCircleGreen");
        mBitmapCircleRed = (Bitmap) getObjectField(view, "mBitmapCircleRed");

        mBitmapArrowGreenUp = (Bitmap) getObjectField(view, "mBitmapArrowGreenUp");
        mBitmapArrowRedUp = (Bitmap) getObjectField(view, "mBitmapArrowRedUp");
        mCircleMatrix = (Matrix) getObjectField(view, "mCircleMatrix");

    }

    /**
     * Clear the pattern lookup table.
     */
    private void clearPatternDrawLookup() {
        for (int i = 0; i < mPatternSize; i++) {
            for (int j = 0; j < mPatternSize; j++) {
                mPatternDrawLookup[i][j] = false;
            }
        }
        setPatternDrawLookupField();
    }

    public void onSizeChanged(int w, int h, int oldW, int oldH) {


        LockPatternView v = getView();
        final int width = w - v.getPaddingLeft() - v.getPaddingRight();
        mSquareWidth = width / (float) mPatternSize;
        setFloatField(v, "mSquareWidth", mSquareWidth);

        final int height = h - v.getPaddingTop() - v.getPaddingBottom();
        mSquareHeight = height / (float) mPatternSize;
        setFloatField(v, "mSquareHeight", mSquareHeight);
    }


    /**
     * Determines whether the point x, y will add a new point to the current
     * pattern (in addition to finding the cell, also makes heuristic choices
     * such as filling in gaps based on current pattern).
     *
     * @param x The x coordinate.
     * @param y The y coordinate.
     */
    private LockPatternView.Cell detectAndAddHit(float x, float y) {
        final LockPatternView.Cell cell = checkForNewHit(x, y);
        LockPatternView v = getView();
        if (cell != null) {

            // check for gaps in existing pattern
            final ArrayList<LockPatternView.Cell> pattern = mPattern;
            if (!pattern.isEmpty()) {
                final LockPatternView.Cell lastCell = pattern.get(pattern.size() - 1);
                int dRow = cell.getRow() - lastCell.getRow();
                int dColumn = cell.getColumn() - lastCell.getColumn();

                int fillInRow = lastCell.getRow();
                int fillInColumn = lastCell.getColumn();

                if (dRow == 0 || dColumn == 0 || Math.abs(dRow) == Math.abs(dColumn)) {
                    while (true) {
                        fillInRow += Integer.signum(dRow);
                        fillInColumn += Integer.signum(dColumn);
                        if (fillInRow == cell.getRow() && fillInColumn == cell.getColumn()) break;
                        LockPatternView.Cell fillInGapCell = LockPatternCell.of(fillInRow, fillInColumn, mPatternSize);
                        if (!mPatternDrawLookup[fillInGapCell.getRow()][fillInGapCell.getColumn()]) {

                            callMethod(v, "addCellToPattern", fillInGapCell);
                        }
                    }
                }
            }

            callMethod(v, "addCellToPattern", cell);
            if (getBooleanField(v, "mEnableHapticFeedback")) {
                v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY,
                        HapticFeedbackConstants.FLAG_IGNORE_VIEW_SETTING
                                | HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING);
            }
            return cell;
        }
        return null;
    }


    protected void onDraw(Canvas canvas) {
        final ArrayList<LockPatternView.Cell> pattern = mPattern;
        final int count = pattern.size();
        final boolean[][] drawLookup = mPatternDrawLookup;


        final LockPatternView v = getView();

        final LockPatternView.DisplayMode displayMode = mPatternDisplayMode;
        if (displayMode == LockPatternView.DisplayMode.Animate) {

            // figure out which circles to draw

            // + 1 so we pause on complete pattern
            final int oneCycle = (count + 1) * MILLIS_PER_CIRCLE_ANIMATING;
            final int spotInCycle = (int) (SystemClock.elapsedRealtime() -
                    mAnimatingPeriodStart) % oneCycle;
            final int numCircles = spotInCycle / MILLIS_PER_CIRCLE_ANIMATING;

            clearPatternDrawLookup();
            for (int i = 0; i < numCircles; i++) {
                final LockPatternView.Cell cell = pattern.get(i);
                drawLookup[cell.getRow()][cell.getColumn()] = true;
            }

            // figure out in progress portion of ghosting line

            final boolean needToUpdateInProgressPoint = numCircles > 0
                    && numCircles < count;

            if (needToUpdateInProgressPoint) {
                final float percentageOfNextCircle =
                        ((float) (spotInCycle % MILLIS_PER_CIRCLE_ANIMATING)) /
                                MILLIS_PER_CIRCLE_ANIMATING;

                final LockPatternView.Cell currentCell = pattern.get(numCircles - 1);
                // TODO CHECK FOR PERFORMANCE
                final float centerX = (Float) callMethod(v, "getCenterXForColumn", currentCell.getColumn());
                final float centerY = (Float) callMethod(v, "getCenterYForRow", currentCell.getRow());

                final LockPatternView.Cell nextCell = pattern.get(numCircles);
                final float dx = percentageOfNextCircle *
                        ((Float) callMethod(v, "getCenterXForColumn", nextCell.getColumn()) - centerX);
                final float dy = percentageOfNextCircle *
                        ((Float) callMethod(v, "getCenterYForRow", nextCell.getRow()) - centerY);
                setFloatField(v, "mInProgressX", centerX + dx);
                setFloatField(v, "mInProgressY", centerY + dy);
            }
            // TODO: Infinite loop here...
            v.invalidate();
        }

        final float squareWidth = mSquareWidth;
        final float squareHeight = mSquareHeight;

        float radius = (squareWidth * mDiameterFactor * 0.5f);
        mPathPaint.setStrokeWidth(radius);

        final Path currentPath = (Path) getObjectField(v, "mCurrentPath");
        currentPath.rewind();

        // draw the circles
        final int paddingTop = v.getPaddingTop();
        final int paddingLeft = v.getPaddingLeft();

        for (int i = 0; i < mPatternSize; i++) {
            float topY = paddingTop + i * squareHeight;
            //float centerY = mPaddingTop + i * mSquareHeight + (mSquareHeight / 2);
            for (int j = 0; j < mPatternSize; j++) {
                float leftX = paddingLeft + j * squareWidth;
                drawCircle(canvas, (int) leftX, (int) topY, drawLookup[i][j]);
            }
        }

        // TODO: the path should be created and cached every time we hit-detect a cell
        // only the last segment of the path should be computed here
        // draw the path of the pattern (unless the user is in progress, and
        // we are in stealth mode)
        final boolean drawPath = ((!v.isInStealthMode() && displayMode != LockPatternView.DisplayMode.Wrong)
                || (displayMode == LockPatternView.DisplayMode.Wrong && mShowErrorPath));


        // draw the arrows associated with the path (unless the user is in progress, and
        // we are in stealth mode)
        boolean oldFlag = (mPaint.getFlags() & Paint.FILTER_BITMAP_FLAG) != 0;
        mPaint.setFilterBitmap(true); // draw with higher quality since we render with transforms
        if (drawPath) {
            for (int i = 0; i < count - 1; i++) {
                LockPatternView.Cell cell = pattern.get(i);
                LockPatternView.Cell next = pattern.get(i + 1);

                // only draw the part of the pattern stored in
                // the lookup table (this is only different in the case
                // of animation).
                if (!drawLookup[next.getRow()][next.getColumn()]) {
                    break;
                }

                float leftX = paddingLeft + cell.getColumn() * squareWidth;
                float topY = paddingTop + cell.getRow() * squareHeight;

                callMethod(v, "drawArrow", canvas, leftX, topY, cell, next);
            }
        }

        if (drawPath) {
            boolean anyCircles = false;
            for (int i = 0; i < count; i++) {
                LockPatternView.Cell cell = pattern.get(i);

                // only draw the part of the pattern stored in
                // the lookup table (this is only different in the case
                // of animation).
                if (!drawLookup[cell.getRow()][cell.getColumn()]) {
                    break;
                }
                anyCircles = true;

                float centerX = (Float) callMethod(v, "getCenterXForColumn", cell.getColumn());
                float centerY = (Float) callMethod(v, "getCenterYForRow", cell.getRow());
                if (i == 0) {
                    currentPath.moveTo(centerX, centerY);
                } else {
                    currentPath.lineTo(centerX, centerY);
                }
            }

            // add last in progress section
            if ((getBooleanField(v, "mPatternInProgress") || displayMode == LockPatternView.DisplayMode.Animate)
                    && anyCircles) {
                currentPath.lineTo(getFloatField(v, "mInProgressX"), getFloatField(v, "mInProgressY"));
            }
            canvas.drawPath(currentPath, mPathPaint);
        }

        mPaint.setFilterBitmap(oldFlag); // restore default flag
    }


    protected int getSuggestedMinimumWidth() {
        // View should be large enough to contain side-by-side target bitmaps
        return mPatternSize * mBitmapWidth;
    }


    protected int getSuggestedMinimumHeight() {
        // View should be large enough to contain side-by-side target bitmaps
        return mPatternSize * mBitmapWidth;
    }


    // helper method to find which cell a point maps to
    private LockPatternView.Cell checkForNewHit(float x, float y) {

        final int rowHit = getRowHit(y);
        if (rowHit < 0) {
            return null;
        }
        final int columnHit = getColumnHit(x);
        if (columnHit < 0) {
            return null;
        }

        if (mPatternDrawLookup[rowHit][columnHit]) {
            return null;
        }
        return LockPatternCell.of(rowHit, columnHit, mPatternSize);
    }

    /**
     * Helper method to find the row that y falls into.
     *
     * @param y The y coordinate
     * @return The row that y falls in, or -1 if it falls in no row.
     */
    private int getRowHit(float y) {

        final LockPatternView v = getView();
        final float squareHeight = mSquareHeight;
        float hitSize = squareHeight * mHitFactor;


        float offset = v.getPaddingTop() + (squareHeight - hitSize) / 2f;
        for (int i = 0; i < mPatternSize; i++) {

            final float hitTop = offset + squareHeight * i;
            if (y >= hitTop && y <= hitTop + hitSize) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Helper method to find the column x fallis into.
     *
     * @param x The x coordinate.
     * @return The column that x falls in, or -1 if it falls in no column.
     */
    private int getColumnHit(float x) {
        final LockPatternView v = getView();
        final float squareWidth = mSquareWidth;
        float hitSize = squareWidth * mHitFactor;

        float offset = v.getPaddingLeft() + (squareWidth - hitSize) / 2f;
        for (int i = 0; i < mPatternSize; i++) {

            final float hitLeft = offset + squareWidth * i;
            if (x >= hitLeft && x <= hitLeft + hitSize) {
                return i;
            }
        }
        return -1;
    }

    /**
     * @param canvas
     * @param leftX
     * @param topY
     * @param partOfPattern Whether this circle is part of the pattern.
     */
    private void drawCircle(Canvas canvas, int leftX, int topY, boolean partOfPattern) {
        Bitmap outerCircle;
        Bitmap innerCircle;
        final LockPatternView v = getView();
        final LockPatternView.DisplayMode displayMode = (LockPatternView.DisplayMode) getObjectField(v, "mPatternDisplayMode");
        if (!partOfPattern || (v.isInStealthMode() && displayMode != LockPatternView.DisplayMode.Wrong)
                || (displayMode == LockPatternView.DisplayMode.Wrong && !mShowErrorPath)) {
            if (!mVisibleDots) {
                return;
            }
            // unselected circle
            outerCircle = mBitmapCircleDefault;
            innerCircle = mBitmapBtnDefault;
        } else if (getBooleanField(v, "mPatternInProgress")) {
            // user is in middle of drawing a pattern
            outerCircle = mBitmapCircleGreen;
            innerCircle = mBitmapBtnTouched;
        } else if (displayMode == LockPatternView.DisplayMode.Wrong) {
            // the pattern is wrong
            outerCircle = mBitmapCircleRed;
            innerCircle = mBitmapBtnDefault;
        } else if (displayMode == LockPatternView.DisplayMode.Correct ||
                displayMode == LockPatternView.DisplayMode.Animate) {
            // the pattern is correct
            outerCircle = mBitmapCircleGreen;
            innerCircle = mBitmapBtnDefault;
        } else {
            throw new IllegalStateException("unknown display mode " + displayMode);
        }

        final int width = mBitmapWidth;
        final int height = mBitmapHeight;

        final float squareWidth = mSquareWidth;
        final float squareHeight = mSquareHeight;

        int offsetX = (int) ((squareWidth - width) / 2f);
        int offsetY = (int) ((squareHeight - height) / 2f);

        // Allow circles to shrink if the view is too small to hold them.
        float sx = Math.min(mSquareWidth / mBitmapWidth, 1.0f);
        float sy = Math.min(mSquareHeight / mBitmapHeight, 1.0f);

        mCircleMatrix.setTranslate(leftX + offsetX, topY + offsetY);
        mCircleMatrix.preTranslate(mBitmapWidth / 2, mBitmapHeight / 2);
        mCircleMatrix.preScale(sx, sy);
        mCircleMatrix.preTranslate(-mBitmapWidth / 2, -mBitmapHeight / 2);

        canvas.drawBitmap(outerCircle, mCircleMatrix, mPaint);
        canvas.drawBitmap(innerCircle, mCircleMatrix, mPaint);
        // LockPatternView.
    }


    private void onSaveInstanceState(XC_MethodHook.MethodHookParam methodHookParam) {
        Parcelable superState = (Parcelable) methodHookParam.getResult();

        methodHookParam.setResult(new SavedState(superState, mVisibleDots, mShowErrorPath));

    }

    private void onRestoreInstanceState(SavedState savedState) {

        mVisibleDots = savedState.isVisibleDots();
        mShowErrorPath = savedState.isShowErrorPath();


    }

    private static class SavedState extends View.BaseSavedState {

        private final boolean mVisibleDots;
        private final boolean mShowErrorPath;

        /**
         * Constructor called from {@link LockPatternView#onSaveInstanceState()}
         */
        private SavedState(Parcelable superState, boolean visibleDots, boolean showErrorPath) {
            super(superState);

            mVisibleDots = visibleDots;
            mShowErrorPath = showErrorPath;
        }

        /**
         * Constructor called from {@link #CREATOR}
         */
        private SavedState(Parcel in) {
            super(in);

            mVisibleDots = (Boolean) in.readValue(null);
            mShowErrorPath = (Boolean) in.readValue(null);
        }

        public boolean isVisibleDots() {
            return mVisibleDots;
        }

        public boolean isShowErrorPath() {
            return mShowErrorPath;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            super.writeToParcel(dest, flags);

            dest.writeValue(mVisibleDots);
            dest.writeValue(mShowErrorPath);
        }

        public static final Parcelable.Creator<SavedState> CREATOR =
                new Creator<SavedState>() {
                    public SavedState createFromParcel(Parcel in) {
                        return new SavedState(in);
                    }

                    public SavedState[] newArray(int size) {
                        return new SavedState[size];
                    }
                };
    }


}
