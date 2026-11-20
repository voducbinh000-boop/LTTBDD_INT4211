package com.example.quanlychitieu.ui.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import androidx.core.content.ContextCompat;

import com.example.quanlychitieu.R;

/** Biểu đồ cột thu nhập (xanh) và chi tiêu (đỏ) theo 12 tháng, thích ứng Dark Mode. */
public class BarChartView extends View {
    private long[] income = new long[12];
    private long[] expense = new long[12];
    private final Paint bar = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint label = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint line = new Paint();
    private final RectF r = new RectF();

    public BarChartView(Context c, AttributeSet a) {
        super(c, a);
        label.setTextAlign(Paint.Align.CENTER);
    }

    public void setData(long[] income, long[] expense) {
        this.income = income;
        this.expense = expense;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        label.setColor(ContextCompat.getColor(getContext(), R.color.text_gray));
        line.setColor(ContextCompat.getColor(getContext(), R.color.divider));
        int incomeColor = ContextCompat.getColor(getContext(), R.color.income);
        int expenseColor = ContextCompat.getColor(getContext(), R.color.expense);

        float d = getResources().getDisplayMetrics().density;
        label.setTextSize(10 * d);
        float left = 8 * d, right = getWidth() - 8 * d;
        float top = 8 * d, bottom = getHeight() - 22 * d;

        long max = 1;
        for (int i = 0; i < 12; i++) max = Math.max(max, Math.max(income[i], expense[i]));

        canvas.drawLine(left, bottom, right, bottom, line);
        float slot = (right - left) / 12f;
        float bw = slot * 0.32f;

        for (int i = 0; i < 12; i++) {
            float cx = left + slot * i + slot / 2f;
            float hi = (bottom - top) * income[i] / max;
            float he = (bottom - top) * expense[i] / max;
            bar.setColor(incomeColor);
            r.set(cx - bw, bottom - hi, cx, bottom);
            canvas.drawRoundRect(r, 3 * d, 3 * d, bar);
            bar.setColor(expenseColor);
            r.set(cx, bottom - he, cx + bw, bottom);
            canvas.drawRoundRect(r, 3 * d, 3 * d, bar);
            canvas.drawText(String.valueOf(i + 1), cx, getHeight() - 6 * d, label);
        }
    }
}
