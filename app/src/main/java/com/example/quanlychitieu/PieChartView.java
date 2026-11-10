package com.example.quanlychitieu;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

/** Biểu đồ tròn (dạng vòng) tự vẽ bằng Canvas, không cần thư viện ngoài. */
public class PieChartView extends View {
    public static final int[] COLORS = {
            0xFFE53935, 0xFFF5B83D, 0xFFFF8A65, 0xFF913D40, 0xFF4DB6AC, 0xFF7986CB, 0xFFBDBDBD
    };

    private long[] values = new long[0];
    private String centerTop = "", centerBottom = "";
    private final Paint arc = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint text = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();

    public PieChartView(Context c, AttributeSet a) {
        super(c, a);
        arc.setStyle(Paint.Style.STROKE);
        text.setTextAlign(Paint.Align.CENTER);
    }

    public void setData(long[] values, String top, String bottom) {
        this.values = values;
        this.centerTop = top;
        this.centerBottom = bottom;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        float d = getResources().getDisplayMetrics().density;
        float size = Math.min(getWidth(), getHeight());
        float stroke = size * 0.2f;
        float cx = getWidth() / 2f, cy = getHeight() / 2f;
        float r = size / 2f - stroke / 2f - 4 * d;
        rect.set(cx - r, cy - r, cx + r, cy + r);
        arc.setStrokeWidth(stroke);

        long total = 0;
        for (long v : values) total += v;

        if (total == 0) {
            arc.setColor(0xFFEFE3E4);
            canvas.drawCircle(cx, cy, r, arc);
        } else {
            float start = -90;
            for (int i = 0; i < values.length; i++) {
                float sweep = values[i] * 360f / total;
                arc.setColor(COLORS[i % COLORS.length]);
                canvas.drawArc(rect, start, Math.max(sweep - 1f, 0.5f), false, arc);
                start += sweep;
            }
        }

        text.setColor(0xFF3B1F21);
        text.setFakeBoldText(true);
        text.setTextSize(16 * d);
        canvas.drawText(centerTop, cx, cy, text);
        text.setFakeBoldText(false);
        text.setColor(0xFF8A7476);
        text.setTextSize(12 * d);
        canvas.drawText(centerBottom, cx, cy + 18 * d, text);
    }
}
