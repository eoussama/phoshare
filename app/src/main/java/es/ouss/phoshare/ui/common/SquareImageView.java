package es.ouss.phoshare.ui.common;

import android.content.Context;
import android.util.AttributeSet;

import androidx.annotation.Nullable;

import com.google.android.material.imageview.ShapeableImageView;

/** An image as tall as it is wide: posts are shown square, like on the cameras the logo draws. */
public class SquareImageView extends ShapeableImageView {

    public SquareImageView(Context context) {
        super(context);
    }

    public SquareImageView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public SquareImageView(Context context, @Nullable AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, widthMeasureSpec);
    }
}
