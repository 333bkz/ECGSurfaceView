package com.bkz.ecg;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.WindowManager;

import java.util.ArrayList;
import java.util.List;

public class ECGSurfaceView extends SurfaceView implements SurfaceHolder.Callback, Runnable {
    private final SurfaceHolder holder;//控制器
    private Paint paint;
    private boolean isDrawing;
    private int width, height;
    private int screenNumber;//满屏所需点
    private final int offset = 5;//点之间间隔
    private final int max = 4096;
    private Canvas lockCanvas = null;
    private List<Integer> data = new ArrayList<>();

    public ECGSurfaceView(Context context) {
        this(context, null);
    }

    public ECGSurfaceView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ECGSurfaceView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        holder = getHolder();
        holder.setFormat(PixelFormat.TRANSLUCENT);
        holder.addCallback(this);
        setFocusable(true);
        setFocusableInTouchMode(true);//焦点
        this.setKeepScreenOn(true);//保持屏幕长亮
        width = getScreenWidth(context);
        paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setStrokeWidth(2);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        //固定宽高
        height = width / 2;
        screenNumber = width / offset;
        setMeasuredDimension(width, height);
    }

    public static int getScreenWidth(Context context) {
        WindowManager wm = (WindowManager) context
                .getSystemService(Context.WINDOW_SERVICE);
        DisplayMetrics outMetrics = new DisplayMetrics();
        wm.getDefaultDisplay().getMetrics(outMetrics);
        return outMetrics.widthPixels;
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        start();
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        isDrawing = false;
    }

    @Override
    public void run() {
        if (isDrawing) {
            running();
        }
    }

    public void stop() {
        isDrawing = false;
    }

    public void start() {
        if (!isDrawing) {
            isDrawing = true;
            new Thread(this).start();
        }
    }

    private void running() {
//        drawBackground(canvas);
        paint.setColor(Color.YELLOW);
        while (isDrawing) {
            long before = System.currentTimeMillis();
            try {
                lockCanvas = holder.lockCanvas();
                lockCanvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
                synchronized (ECGSurfaceView.class) {
                    lockCanvas.drawLines(drawScreenLines(), paint);
                }
                before = System.currentTimeMillis() - before;
                if (before < 60) {
                    Thread.sleep(60 - before);
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (lockCanvas != null) {
                    holder.unlockCanvasAndPost(lockCanvas);
                }
            }
        }
    }

    /**
     * 始终画集合内最后的满屏数据 -> 折线平移速度与设置数据{@link #setData}频率成正比
     * @return 折线集合
     */
    private float[] drawScreenLines() {
        List<Integer> drawData = data;
        if (data.size() >= screenNumber) {
            drawData = data.subList(data.size() - screenNumber, data.size());
        }

        final int size = drawData.size();
        final float[] pts = new float[(size - 1) * 4];
        int startX = 0;
        int startY = 0;

        if (size < screenNumber) {
            for (int i = 0; i < data.size() - 5; i++) {
                if (i == 0) {
                    startX = offset * i;
                    startY = ecgConvert(drawData.get(i));
                } else {
                    int currentX = offset * i;
                    int currentY = ecgConvert(drawData.get(i));
                    pts[(i - 1) * 4] = startX;
                    pts[(i - 1) * 4 + 1] = startY;
                    pts[(i - 1) * 4 + 2] = currentX;
                    pts[(i - 1) * 4 + 3] = currentY;
                    startX = currentX;
                    startY = currentY;
                }
            }
        } else {
            for (int i = 0; i < size; i++) {
                if (i == 0) {
                    startX = offset * i;
                    startY = ecgConvert(drawData.get(i));
                } else if (i < size - 1) {
                    int currentX = offset * i;
                    int currentY = ecgConvert(drawData.get(i));
                    pts[(i - 1) * 4] = startX;
                    pts[(i - 1) * 4 + 1] = startY;
                    pts[(i - 1) * 4 + 2] = currentX;
                    pts[(i - 1) * 4 + 3] = currentY;
                    startX = currentX;
                    startY = currentY;
                }
            }
        }
        return pts;
    }

    private void drawBackground(Canvas canvas) {
        paint.setColor(Color.GRAY);
        int verticalSpace = width / 10;
        for (int i = 1; i < 10; i++) {
            canvas.drawLine(i * verticalSpace, 0, i * verticalSpace, getHeight(), paint);
        }
        int horizontalSpace = height / 5;
        for (int i = 1; i < 5; i++) {
            canvas.drawLine(0, i * horizontalSpace, getWidth(), i * horizontalSpace, paint);
        }
    }

    public void setData(int value) {
        synchronized (ECGSurfaceView.class) {
            if (data != null) {
                data.add(value);
            }
        }
    }

    /**
     * 将心电数据转换成用于显示的Y坐标
     */
    private int ecgConvert(int value) {
        value = max - value;
        value = value * height / max;
        return value;
    }
}
