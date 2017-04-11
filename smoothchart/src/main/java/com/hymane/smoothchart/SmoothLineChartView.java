package com.hymane.smoothchart;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.support.annotation.DrawableRes;
import android.support.annotation.IntDef;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.List;

/**
 * Author   :hymane
 * Email    :hymanme@163.com
 * Create at 2017-04-08
 * Description:
 */
public class SmoothLineChartView extends View {
    public static final int NODE_STYLE_CIRCLE = 0;
    public static final int NODE_STYLE_RING = 1;

    public static final int TOUCH_MIN_DISTANCE = 35;
    private static final int CHART_COLOR = 0xFF0099CC;//默认线条颜色
    private static final int CIRCLE_SIZE = 8;//默认节点圆直径
    private static final int SELECTED_CIRCLE_SIZE = 15;//默认选中的节点圆直径
    private static final int STROKE_SIZE = 2;//默认节点圆环宽度
    private static final float SMOOTHNESS = 0.35f; // the higher the smoother, but don't go over 0.5
    private static final int TEXT_POSITION_OFFSET = 2;

    private final Context mContext;
    private final Paint mPaint;//画笔
    private final Path mPath;//曲线路径
    private final float mCircleSize;//节点圆直径
    private final float mSelectedCircleSize;//选中节点圆直径
    private final float mStrokeSize;
    private float mBorder;
    private int mNodeStyle;//节点样式
    private int mLineColor;//曲线颜色
    private int mCircleColor;//圆环颜色
    private int mInnerCircleColor;//节点圆内环填充颜色,仅在节点为圆环时起作用
    private int mTextColor;
    private int mTextSize;
    private int mTextOffset;
    //自定义最小Y值
    private boolean mCustomAxisMin;
    //自定义最大Y值
    private boolean mCustomAxisMax;
    //是否绘制曲线投影区域
    private boolean mEnableDrawArea;
    //是否显示高亮标签
    private boolean mEnableShowTag;
    private boolean mCustomBorder;
    private int mDrawAreaColor;
    //坐标点集合
    private List<PointF> mPoints = new ArrayList<PointF>();
    private List<Float> mValues;//节点数据集
    private List<String> mXData;
    private Bitmap mTagBitmap;
    private Bitmap mTagBitmapReverse;
    private Drawable mTagDrawable;
    private float mMinY;    //最小y刻度值
    private float mMaxY;    //最大y刻度值
    private OnChartClickListener mChartClickListener;
    private int mSelectedNode = -1;

    @IntDef({NODE_STYLE_CIRCLE, NODE_STYLE_RING})
    @Retention(RetentionPolicy.SOURCE)
    @interface NodeStyle {
    }

    public SmoothLineChartView(Context context) {
        this(context, null, 0);
    }

    public SmoothLineChartView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SmoothLineChartView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        mContext = context;
        float scale = context.getResources().getDisplayMetrics().density;
        mCircleSize = scale * CIRCLE_SIZE;
        mStrokeSize = scale * STROKE_SIZE;
        mSelectedCircleSize = scale * SELECTED_CIRCLE_SIZE;
        mEnableDrawArea = true;
        mEnableShowTag = true;
        mDrawAreaColor = (CHART_COLOR & 0xFFFFFF) | 0x10000000;
        mLineColor = CHART_COLOR;
        mCircleColor = CHART_COLOR;
        mInnerCircleColor = Color.WHITE;
        mTextColor = Color.WHITE;
        mTextSize = DensityUtils.sp2px(context, 12);
        mTextOffset = TEXT_POSITION_OFFSET;
        mNodeStyle = NODE_STYLE_CIRCLE;
        mPaint = new Paint();
        mPaint.setAntiAlias(true);
        mPath = new Path();
        mBorder = 2 * mCircleSize;
    }

    /***
     * 设置路径节点
     */
    public void setData(List<Float> yValues, List<String> xValue) {
        if (yValues == null || xValue == null) {
            throw new IllegalArgumentException("valuse can not be null");
        } else if (yValues.size() != xValue.size()) {
            throw new IllegalArgumentException("yValues's size should be same as xValue's");
        }
        mValues = yValues;
        mPoints.clear();
        if (yValues != null && yValues.size() > 0) {
            mMaxY = mCustomAxisMax ? mMaxY : yValues.get(0);
            mMinY = mCustomAxisMin ? mMinY : yValues.get(0);
            for (float y : yValues) {
                if (y > mMaxY) {
                    mMaxY = y;
                }
                if (y < mMinY) {
                    mMinY = y;
                }
            }
        }
        this.mXData = xValue;
        invalidate();
    }

    public void add(float value, String xString) {
        mValues.add(value);
        if (value > mMaxY) {
            if (!mCustomAxisMax)
                mMaxY = value;
        }
        if (value < mMinY) {
            if (!mCustomAxisMin)
                mMinY = value;
        }
        this.mXData.add(xString);
        invalidate();
    }

    public void remove(int position) {
        mValues.remove(position);
        if (mSelectedNode == position) {
            mSelectedNode = -1;
        }
        if (mValues != null && mValues.size() > 0) {
            mMaxY = mCustomAxisMax ? mMaxY : mValues.get(0);
            mMinY = mCustomAxisMin ? mMinY : mValues.get(0);
            for (float y : mValues) {
                if (y > mMaxY) {
                    if (!mCustomAxisMax)
                        mMaxY = y;
                }
                if (y < mMinY) {
                    if (!mCustomAxisMin)
                        mMinY = y;
                }
            }
        }

    }

    public void removeAll() {
        mValues.clear();
        mMaxY = 0;
        mMinY = 0;
    }

    /***
     * 绘制曲线
     * @param canvas
     */
    public void draw(Canvas canvas) {
        super.draw(canvas);
        if (mValues == null || mValues.size() == 0)
            return;

        //节点个数
        int size = mValues.size();
        //曲线实际绘制区域的宽和高，避免绘制到区域外
        final float height = getMeasuredHeight() - 2 * mBorder;
        final float width = getMeasuredWidth() - 3 * mBorder;

        final float dX = mValues.size() > 1 ? mValues.size() - 1 : (2);
        final float dY = (mMaxY - mMinY) > 0 ? (mMaxY - mMinY) : (2);

        mPath.reset();
        mPaint.setStrokeWidth(mStrokeSize);

        mPoints.clear();
        //计算点的坐标,并保存点的集合
        for (int i = 0; i < size; i++) {
            float x = 2 * mBorder + i * width / dX;
            float y = mBorder + height - (mValues.get(i) - mMinY) * height / dY;
            mPoints.add(new PointF(x, y));
        }

        //计算曲线路径
        float lX = 0, lY = 0;
        mPath.moveTo(mPoints.get(0).x, mPoints.get(0).y);
        for (int i = 1; i < size; i++) {
            PointF current = mPoints.get(i);//当前点

            //计算第一个控制点
            PointF pre = mPoints.get(i - 1); //上一个节点
            float x1 = pre.x + lX;
            float y1 = pre.y + lY;

            //计算第二个控制点
            PointF next = mPoints.get(i + 1 < size ? i + 1 : i);//下一个节点
            lX = (next.x - pre.x) / 2 * SMOOTHNESS;        // (lX,lY) is the slope of the reference line
            lY = (next.y - pre.y) / 2 * SMOOTHNESS;
            float x2 = current.x - lX;
            float y2 = current.y - lY;

            // add line
            mPath.cubicTo(x1, y1, x2, y2, current.x, current.y);
        }

        //绘制曲线
        mPaint.setColor(mLineColor);
        mPaint.setStyle(Paint.Style.STROKE);
        canvas.drawPath(mPath, mPaint);

        //绘制曲线的投影区域
        if (mEnableDrawArea && size > 0) {
            mPaint.setStyle(Paint.Style.FILL);
            mPaint.setColor(mDrawAreaColor);
            mPath.lineTo(mPoints.get(size - 1).x, height + mBorder);
            mPath.lineTo(mPoints.get(0).x, height + mBorder);
            mPath.close();
            canvas.drawPath(mPath, mPaint);
        }
        //绘制选中节点高亮
        if (mSelectedNode != -1 && mSelectedNode < size) {
            mPaint.setColor((mCircleColor & 0xFFFFFF) | 0x30000000);
            mPaint.setStyle(Paint.Style.FILL_AND_STROKE);
            final PointF point = mPoints.get(mSelectedNode);
            canvas.drawCircle(point.x, point.y, mSelectedCircleSize / 2, mPaint);
        }
        //绘制节点
        mPaint.setColor(mCircleColor);
        mPaint.setStyle(Paint.Style.FILL_AND_STROKE);
        for (PointF point : mPoints) {
            canvas.drawCircle(point.x, point.y, mCircleSize / 2, mPaint);
        }
        //绘制圆环内圆填充
        if (mNodeStyle == NODE_STYLE_RING) {
            mPaint.setStyle(Paint.Style.FILL);
            mPaint.setColor(mInnerCircleColor);
            for (PointF point : mPoints) {
                canvas.drawCircle(point.x, point.y, (mCircleSize - mStrokeSize) / 2, mPaint);
            }
        }
        //绘制高亮tag
        if (mEnableShowTag && mSelectedNode != -1 && mSelectedNode < size) {
            final PointF point = mPoints.get(mSelectedNode);
            final float tagOffsetY = point.y - mTagBitmap.getHeight() * 1.5f;
            if (mTagBitmap != null) {
                if (tagOffsetY > 0) {
                    canvas.drawBitmap(mTagBitmap, point.x - mTagBitmap.getWidth() / 2, tagOffsetY, mPaint);
                } else {
                    canvas.drawBitmap(mTagBitmapReverse, point.x - mTagBitmap.getWidth() / 2, point.y + mTagBitmap.getHeight() * 0.5f, mPaint);
                }
            }
            mPaint.setTextSize(DensityUtils.sp2px(mContext, mTextSize));
            mPaint.setStrokeWidth(0);
            mPaint.setColor(mTextColor);
            final String text = mValues.get(mSelectedNode) + "";
            final Rect textRect = new Rect();
            mPaint.getTextBounds(text, 0, text.length(), textRect);
            Paint.FontMetricsInt fontMetrics = mPaint.getFontMetricsInt();
            float baseline = (fontMetrics.top - fontMetrics.bottom) / 2 - fontMetrics.top;
            float yOffset = 0;
            if (tagOffsetY <= 0) {
                yOffset = mTagBitmap == null ? mCircleSize * 2 : mTagBitmap.getHeight() + mTextOffset;
            } else {
                yOffset = mTagBitmap == null ? mCircleSize * 2 : mTagBitmap.getHeight() * -1 - mTextOffset;
            }
            canvas.drawText(text, point.x - textRect.width() * 0.5f, point.y + baseline + yOffset, mPaint);
        }

        //绘制x刻度
        for (int i = 0; i < mPoints.size(); i++) {
            mPaint.setTextSize(DensityUtils.sp2px(mContext, mTextSize));
            mPaint.setStrokeWidth(0);
            mPaint.setColor(Color.RED);
            final Rect textRect = new Rect();
            mPaint.getTextBounds(mXData.get(i), 0, mXData.get(i).length(), textRect);
            canvas.drawText(mXData.get(i), 0, 4, mPoints.get(i).x - textRect.width() * 0.5f, getMeasuredHeight(), mPaint);
        }
        //绘制Y刻度
        Paint.FontMetricsInt fontMetrics = mPaint.getFontMetricsInt();
        float baseline = (fontMetrics.top - fontMetrics.bottom) / 2 - fontMetrics.top;
        final String top = mValues.get(0) + "";
        final String min = "" + mMinY;
        canvas.drawText(top, 0, top.length(), 0, mPoints.get(0).y + baseline, mPaint);
        canvas.drawText(min, 0, min.length(), 0, mBorder + height + baseline, mPaint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                break;
            case MotionEvent.ACTION_MOVE:
                break;
            case MotionEvent.ACTION_UP:
                mSelectedNode = checkClicked(event.getX(), event.getY());
                if (mSelectedNode != -1) {
                    if (mChartClickListener != null) {
                        mChartClickListener.onClick(mSelectedNode, mValues.get(mSelectedNode));
                    }
                    invalidate();
                }
                break;
        }
        return true;
    }

    private int checkClicked(float x, float y) {
        final int size = mPoints.size();
        for (int i = 0; i < size; i++) {
            final PointF p = mPoints.get(i);
            if (x >= p.x - TOUCH_MIN_DISTANCE && x < p.x + TOUCH_MIN_DISTANCE
                    && y >= p.y - TOUCH_MIN_DISTANCE && y < p.y + TOUCH_MIN_DISTANCE) {
                return i;
            }
        }
        return -1;
    }

    public float getMinY() {
        return mMinY;
    }

    public void setMinY(float mMinY) throws IllegalArgumentException {
        mCustomAxisMin = true;
        if (mCustomAxisMax && mMaxY < mMinY) {
            throw new IllegalArgumentException("mMinY must be smaller than mMaxY");
        }
        this.mMinY = mMinY;
        invalidate();
    }

    public float getMaxY() {
        return mMaxY;
    }

    public void setMaxY(float mMaxY) throws IllegalArgumentException {
        mCustomAxisMax = true;
        if (mCustomAxisMin && mMaxY < mMinY) {
            throw new IllegalArgumentException("mMinY must be smaller than mMaxY");
        }
        this.mMaxY = mMaxY;
        invalidate();
    }

    public boolean isCustomAxisMin() {
        return mCustomAxisMin;
    }

    public void setCustomAxisMin(boolean mCustomAxisMin) {
        this.mCustomAxisMin = mCustomAxisMin;
        invalidate();
    }

    public boolean isCustomAxisMax() {
        return mCustomAxisMax;
    }

    public void setCustomAxisMax(boolean mCustomAxisMax) {
        this.mCustomAxisMax = mCustomAxisMax;
        invalidate();
    }

    public boolean isEnableDrawArea() {
        return mEnableDrawArea;
    }

    public void enableDrawArea(boolean mEnableDrawArea) {
        this.mEnableDrawArea = mEnableDrawArea;
        invalidate();
    }

    public int getDrawAreaColor() {
        return mDrawAreaColor;
    }

    public void setDrawAreaColor(int mDrawAreaColor) {
        this.mDrawAreaColor = mDrawAreaColor;
    }

    public int getLineColor() {
        return mLineColor;
    }

    public void setLineColor(int mLineColor) {
        this.mLineColor = mLineColor;
    }

    public int getCircleColor() {
        return mCircleColor;
    }

    public void setCircleColor(int mCircleColor) {
        this.mCircleColor = mCircleColor;
    }

    public int getInnerCircleColor() {
        return mInnerCircleColor;
    }

    public void setInnerCircleColor(int mInnerCircleColor) {
        this.mInnerCircleColor = mInnerCircleColor;
    }

    public int getNodeStyle() {
        return mNodeStyle;
    }

    public void setNodeStyle(@NodeStyle int mNodeStyle) {
        this.mNodeStyle = mNodeStyle;
        invalidate();
    }

    public OnChartClickListener getChartClickListener() {
        return mChartClickListener;
    }

    public void setOnChartClickListener(OnChartClickListener mChartClickListener) {
        this.mChartClickListener = mChartClickListener;
    }

    public Drawable getTagDrawable() {
        return mTagDrawable;
    }

    public void setTagDrawable(Drawable drawable) {
        this.mTagDrawable = drawable;
        mTagBitmap = Bitmap.createBitmap(((BitmapDrawable) mTagDrawable).getBitmap());
        Matrix matrix = new Matrix();
        matrix.postRotate(180);
        mTagBitmapReverse = Bitmap.createBitmap(mTagBitmap, 0, 0, mTagBitmap.getWidth(), mTagBitmap.getHeight(), matrix, true);
        if (!mCustomBorder) {
            mBorder = mTagBitmap.getWidth() * 0.5f;
        }
    }

    public void setTagDrawable(@DrawableRes int drawableSrc) {
        this.mTagDrawable = mContext.getResources().getDrawable(drawableSrc);
        mTagBitmap = Bitmap.createBitmap(((BitmapDrawable) mTagDrawable).getBitmap());
        Matrix matrix = new Matrix();
        matrix.postRotate(180);
        mTagBitmapReverse = Bitmap.createBitmap(mTagBitmap, 0, 0, mTagBitmap.getWidth(), mTagBitmap.getHeight(), matrix, true);
        if (!mCustomBorder) {
            mBorder = mTagBitmap.getWidth() * 0.5f;
        }
    }

    public int getTextColor() {
        return mTextColor;
    }

    public void setTextColor(int mTextColor) {
        this.mTextColor = mTextColor;
    }

    public int getTextSize() {
        return mTextSize;
    }

    public void setTextSize(int mTextSize) {
        this.mTextSize = mTextSize;
    }

    public int getTextOffset() {
        return mTextOffset;
    }

    public void setTextOffset(int mTextOffset) {
        this.mTextOffset = mTextOffset;
    }

    public boolean isShowTag() {
        return mEnableShowTag;
    }

    public void enableShowTag(boolean enableShowTag) {
        this.mEnableShowTag = enableShowTag;
        invalidate();
    }

    public boolean isCustomBorder() {
        return mCustomBorder;
    }

    public void setCustomBorder(boolean mCustomBorder) {
        this.mCustomBorder = mCustomBorder;
    }

    public float getBorder() {
        return mBorder;
    }

    /***
     * 自定义视图边界
     * @param mBorder
     */
    public void setBorder(float mBorder) {
        this.mBorder = mBorder;
        mCustomBorder = true;
    }
}
