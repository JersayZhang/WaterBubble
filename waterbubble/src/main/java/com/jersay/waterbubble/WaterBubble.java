package com.jersay.waterbubble;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Shader;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.View;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Created by Jersay on 2018/11/28.
 */
public class WaterBubble extends View {

    private int mBubbleMaxRadius = 30;//气泡最大半径(px)
    private int mBubbleMinRadius = 5;//气泡最小半径(px)
    private int mBubbleMaxNumber = 30;//气泡最大数量
    private int mBubbleRefreshTime = 20;//气泡刷新间隔
    private int mBubbleMaxSpeedY = 5;//气泡最大上升速度
    private int mBubbleAlpha = 128;//气泡透明度

    private float mBottleWidth;//瓶子宽度
    private float mBottleHeight;//瓶子高度
    private float mBottleBorder;//瓶子边框宽度
    private float mBottleRadius;//瓶子底部圆角半径
    private float mBottleCapRadius;//瓶子顶部圆角半径
    private float mWaterHeight;//水的高度

    private RectF mContentRectF;//实际可用的内容区域
    private RectF mWaterRectF;//水占用的区域

    private Path mBottlePath;//瓶子路径
    private Path mWaterPath;//水路径

    private Paint mBottlePaint;//瓶子画笔
    private Paint mWaterPaint;//水画笔
    private Paint mBubblePaint;//气泡画笔

    private ArrayList<BubbleBean> mBubbles = new ArrayList<>();//气泡数据源
    private Random mRandom = new Random();
    private Thread mBubbleThread;

    public WaterBubble(Context context) {
        this(context, null);
    }

    public WaterBubble(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public WaterBubble(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        mWaterRectF = new RectF();

        mBottleWidth = Utils.dp2px(this.getContext(), 130);
        mBottleHeight = Utils.dp2px(this.getContext(), 260);
        mBottleBorder = Utils.dp2px(this.getContext(), 8);
        mBottleRadius = Utils.dp2px(this.getContext(), 15);
        mBottleCapRadius = Utils.dp2px(this.getContext(), 5);

        mWaterHeight = Utils.dp2px(this.getContext(), 240);

        mBottlePath = new Path();
        mWaterPath = new Path();

        mBottlePaint = new Paint();
        mBottlePaint.setAntiAlias(true);//开启抗锯齿
        mBottlePaint.setColor(getResources().getColor(R.color.color_ADFF2F));//设置画笔颜色
        mBottlePaint.setStyle(Paint.Style.STROKE);//设置填充模式为stroke
        mBottlePaint.setStrokeWidth(mBottleBorder);//设置画笔宽度
        mBottlePaint.setStrokeCap(Paint.Cap.ROUND);//设置线帽为round

        mWaterPaint = new Paint();
        mWaterPaint.setAntiAlias(true);

        initBubble();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);

        //获取实际可用内容区域
        mContentRectF = new RectF(getPaddingLeft(), getPaddingTop(), w - getPaddingRight(),
                h - getPaddingBottom());

        //获取瓶子距离父view上下左右的距离
        float bl = mContentRectF.centerX() - mBottleWidth / 2;
        float bt = mContentRectF.centerY() - mBottleHeight / 2;
        float br = mContentRectF.centerX() + mBottleWidth / 2;
        float bb = mContentRectF.centerY() + mBottleHeight / 2;

        /*
         计算瓶子路径
         */
        mBottlePath.reset();
        //1、画笔移动起点到瓶子左上角位置
        mBottlePath.moveTo(bl - mBottleCapRadius, bt - mBottleCapRadius);
        //2、画二次贝塞尔曲线作为瓶子顶部左侧的弧形边沿
        mBottlePath.quadTo(bl, bt - mBottleCapRadius, bl, bt);
        //3、继续到底部
        mBottlePath.lineTo(bl, bb - mBottleRadius);
        //4、画二次贝塞尔曲线作为瓶子底部左侧的弧形边沿
        mBottlePath.quadTo(bl, bb, bl + mBottleRadius, bb);
        //5、计算底部路径
        mBottlePath.lineTo(br - mBottleRadius, bb);
        //继续计算完瓶子右侧部分
        mBottlePath.quadTo(br, bb, br, bb - mBottleRadius);
        mBottlePath.lineTo(br, bt);
        mBottlePath.quadTo(br, bt - mBottleCapRadius, br + mBottleCapRadius, bt - mBottleCapRadius);

        /*
        计算水的路径，类似于瓶子路径计算
         */
        mWaterPath.reset();
        mWaterPath.moveTo(bl, bb - mWaterHeight);
        mWaterPath.lineTo(bl, bb - mBottleRadius);
        mWaterPath.quadTo(bl, bb, bl + mBottleRadius, bb);
        mWaterPath.lineTo(br - mBottleRadius, bb);
        mWaterPath.quadTo(br, bb, br, bb - mBottleRadius);
        mWaterPath.lineTo(br, bb - mWaterHeight);
        mWaterPath.close();

        mWaterRectF.set(bl, bb - mWaterHeight, br, bb);

        //给水设置一个从上到下的渐变
        LinearGradient gradient = new LinearGradient(mWaterRectF.centerX(), mWaterRectF.top,
                mWaterRectF.centerX(), mWaterRectF.bottom,
                getResources().getColor(R.color.color_00F5FF),
                getResources().getColor(R.color.color_00E5EE),
                Shader.TileMode.CLAMP);
        mWaterPaint.setShader(gradient);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawPath(mWaterPath, mWaterPaint);
        canvas.drawPath(mBottlePath, mBottlePaint);
        drawBubble(canvas);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        startBubbleThread();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        stopBubbleThread();
    }

    /**
     * 初始化气泡
     */
    private void initBubble() {
        mBubblePaint = new Paint();
        mBubblePaint.setColor(getResources().getColor(R.color.color_FFFFFF));//设置气泡颜色
        mBubblePaint.setAlpha(mBubbleAlpha);//设置气泡透明度
    }

    /**
     * 绘制气泡
     *
     * @param canvas canves
     */
    private void drawBubble(Canvas canvas) {
        List<BubbleBean> list = new ArrayList<>(mBubbles);
        for (BubbleBean bubbleBean : list) {
            if (null == bubbleBean) continue;
            canvas.drawCircle(bubbleBean.x, bubbleBean.y, bubbleBean.radius, mBubblePaint);
        }
    }

    /**
     * 开始气泡线程
     */
    private void startBubbleThread() {
        stopBubbleThread();
        mBubbleThread = new Thread() {
            @Override
            public void run() {
                while (true) {
                    try {
                        Thread.sleep(mBubbleRefreshTime);
                        createBubble();
                        refreshBubble();
                        postInvalidate();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                        break;
                    }
                }
            }
        };
        mBubbleThread.start();
    }

    /**
     * 停止气泡线程
     */
    private void stopBubbleThread() {
        if (null == mBubbleThread) return;
        mBubbleThread.interrupt();
        mBubbleThread = null;
    }

    /**
     * 创建气泡
     */
    private void createBubble() {
        if (null == mContentRectF) return;
        if (mBubbles.size() >= mBubbleMaxNumber) {
            return;
        }
        if (mRandom.nextFloat() < 0.95) {
            return;
        }
        BubbleBean bubbleBean = new BubbleBean();
        /*
        在最大半径和最小半径之间随机生成一个半径
         */
        int radius = mRandom.nextInt(mBubbleMaxRadius - mBubbleMinRadius);
        radius += mBubbleMinRadius;
        /*
        随机生成一个上升速度
         */
        float speedY = mRandom.nextFloat() * mBubbleMaxSpeedY;
        while (speedY < 1) {
            speedY = mRandom.nextFloat() * mBubbleMaxSpeedY;
        }
       /*
        随机生成一个平移速度
        */
        float speedX = mRandom.nextFloat() - 0.5f;
        while (speedX == 0) {
            speedX = mRandom.nextFloat() - 0.5f;
        }
        bubbleBean.radius = radius;
        bubbleBean.speedY = speedY;
        bubbleBean.speedX = speedX * 2;
        bubbleBean.x = mWaterRectF.centerX();
        bubbleBean.y = mWaterRectF.bottom - radius - mBottleBorder / 2;
        mBubbles.add(bubbleBean);
    }

    /**
     * 刷新气泡位置，移除超出区域的气泡
     */
    private void refreshBubble() {
        List<BubbleBean> list = new ArrayList<>(mBubbles);
        for (BubbleBean bubbleBean : list) {
            if (bubbleBean.y - bubbleBean.speedY <= mWaterRectF.top + bubbleBean.radius) {
                mBubbles.remove(bubbleBean);
            } else {
                int i = mBubbles.indexOf(bubbleBean);
                if (bubbleBean.x + bubbleBean.speedX <= mWaterRectF.left + bubbleBean.radius + mBottleBorder / 2) {
                    bubbleBean.x = mWaterRectF.left + bubbleBean.radius + mBottleBorder / 2;
                } else if (bubbleBean.x + bubbleBean.speedX >= mWaterRectF.right - bubbleBean.radius - mBottleBorder / 2) {
                    bubbleBean.x = mWaterRectF.right - bubbleBean.radius - mBottleBorder / 2;
                } else {
                    bubbleBean.x = bubbleBean.x + bubbleBean.speedX;
                }
                bubbleBean.y = bubbleBean.y - bubbleBean.speedY;
                mBubbles.set(i, bubbleBean);
            }
        }
    }
}
