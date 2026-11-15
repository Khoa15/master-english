package com.masterenglish.app.ui.common;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.os.SystemClock;
import android.view.View;
import android.view.ViewTreeObserver;

/**
 * Frosted glass for the floating navigation. Each frame the content behind the pill is drawn into a
 * tiny bitmap and stretched back up, which gives a blurred backdrop without GPU blur (works on every
 * supported Android version). The pill keeps a white tint and a light edge.
 */
public final class GlassNav {

    /** Backdrop is captured at 1/8 size; stretching it back up is what blurs it. */
    private static final float CAPTURE_SCALE = 1f / 8f;

    /** Minimum time between backdrop captures (about 30 per second). */
    private static final long CAPTURE_INTERVAL_MS = 33L;

    private GlassNav() {
    }

    public static void install(final View content, final View pill) {
        final GlassDrawable glass = new GlassDrawable(pill.getResources().getDisplayMetrics().density);
        pill.setBackground(glass);
        final float d = pill.getResources().getDisplayMetrics().density;
        // The default outline of a translucent drawable has alpha 0, which makes the shadow nearly invisible.
        pill.setOutlineProvider(new android.view.ViewOutlineProvider() {
            @Override
            public void getOutline(View view, android.graphics.Outline outline) {
                outline.setRoundRect(0, 0, view.getWidth(), view.getHeight(), view.getHeight() / 2f);
                outline.setAlpha(1f);
            }
        });
        pill.setClipToOutline(false);
        pill.setElevation(10f * d);
        if (android.os.Build.VERSION.SDK_INT >= 28) {
            pill.setOutlineSpotShadowColor(0x3D1E2A24);
            pill.setOutlineAmbientShadowColor(0x141E2A24);
        }
        if (pill.getParent() instanceof android.view.ViewGroup) {
            ((android.view.ViewGroup) pill.getParent()).setClipChildren(false);
        }
        final boolean[] capturing = {false};
        final boolean[] dark = {false};
        glass.onLuma = luma -> {
            // Hysteresis so the colours do not flicker when the backdrop sits near the threshold.
            boolean next = dark[0] ? luma < 0.55f : luma < 0.42f;
            if (next != dark[0]) {
                dark[0] = next;
                applyTheme(pill, glass, next);
            }
        };
        final long[] lastCapture = {-1L};
        pill.getViewTreeObserver().addOnPreDrawListener(() -> {
            long now = SystemClock.uptimeMillis();
            // Drawing the content can invalidate it again; never re-enter the capture. Animated content
            // behind the bar changes every frame, so the backdrop is refreshed at most ~30 times a second.
            boolean due = lastCapture[0] < 0 || now - lastCapture[0] >= CAPTURE_INTERVAL_MS;
            if (due && !capturing[0]) {
                lastCapture[0] = now;
                capturing[0] = true;
                try {
                    capture(content, pill, glass);
                } finally {
                    capturing[0] = false;
                }
            }
            return true;
        });
    }

    /** Light icons, white disc and a darker glass when the backdrop is dark; ink icons and disc otherwise. */
    private static void applyTheme(View pill, GlassDrawable glass, boolean dark) {
        glass.setDark(dark);
        if (pill instanceof com.google.android.material.bottomnavigation.BottomNavigationView) {
            com.google.android.material.bottomnavigation.BottomNavigationView nav =
                    (com.google.android.material.bottomnavigation.BottomNavigationView) pill;
            nav.setItemIconTintList(androidx.core.content.ContextCompat.getColorStateList(pill.getContext(),
                    dark ? com.masterenglish.app.R.color.nav_item_tint_dark : com.masterenglish.app.R.color.nav_item_tint));
            nav.setItemBackgroundResource(dark
                    ? com.masterenglish.app.R.drawable.bg_nav_active_dark
                    : com.masterenglish.app.R.drawable.bg_nav_active);
        }
    }

    private static void capture(View content, View pill, GlassDrawable glass) {
        int width = pill.getWidth();
        int height = pill.getHeight();
        if (width <= 0 || height <= 0 || content.getWidth() <= 0 || content.getHeight() <= 0) {
            return;
        }
        int bw = Math.max(1, Math.round(width * CAPTURE_SCALE));
        int bh = Math.max(1, Math.round(height * CAPTURE_SCALE));
        Bitmap bitmap = glass.bitmapFor(bw, bh);
        Canvas canvas = new Canvas(bitmap);
        // Start from the theme background: screens that leave areas unpainted would otherwise read as black.
        android.util.TypedValue tv = new android.util.TypedValue();
        int bg = pill.getContext().getTheme().resolveAttribute(android.R.attr.colorBackground, tv, true)
                && tv.type >= android.util.TypedValue.TYPE_FIRST_COLOR_INT
                && tv.type <= android.util.TypedValue.TYPE_LAST_COLOR_INT ? tv.data : 0xFFF5F3EE;
        canvas.drawColor(bg);
        canvas.scale(CAPTURE_SCALE, CAPTURE_SCALE);
        // The content and the pill share a parent, so the pill's offset maps the backdrop under it.
        canvas.translate(-pill.getLeft(), -pill.getTop());
        content.draw(canvas);
        // Only redraw the pill when the backdrop actually changed. Invalidating on every capture
        // schedules another pre-draw pass, which captures and invalidates again without end.
        if (glass.backdropChanged(bitmap)) {
            glass.invalidateSelf();
        }
    }

    /** Draws the captured backdrop clipped to a rounded pill, then a white tint and a light edge. */
    static final class GlassDrawable extends Drawable {

        private final Paint bitmapPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
        private final Paint tintPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint edgePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint basePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint hairPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint sheenPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint glowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final RectF hair = new RectF();
        private final RectF glowRect = new RectF();
        private final android.graphics.Path clip = new android.graphics.Path();
        private final RectF rect = new RectF();
        private final RectF edge = new RectF();
        private final float density;
        interface LumaListener {
            void onLuma(float luma);
        }

        LumaListener onLuma;
        private boolean dark;
        private Bitmap bitmap;
        private int[] lastPixels;

        GlassDrawable(float density) {
            this.density = density;
            basePaint.setColor(0x00000000);
            // CSS: border 1px rgba(255,255,255,0.65).
            hairPaint.setStyle(Paint.Style.STROKE);
            hairPaint.setStrokeWidth(density);
            hairPaint.setColor(0xCCFFFFFF);
            tintPaint.setColor(0xB8FFFFFF);
            // CSS inset rim: inset 0 1px 1.5px .5px white .9 (top) and inset 0 -2px 4px white .25 (bottom).
            edgePaint.setStyle(Paint.Style.STROKE);
            edgePaint.setStrokeWidth(1.5f * density);
            glowPaint.setStyle(Paint.Style.STROKE);
            // Design: backdrop-filter saturate(1.8) on the blurred backdrop.
            android.graphics.ColorMatrix saturate = new android.graphics.ColorMatrix();
            saturate.setSaturation(1f);
            bitmapPaint.setColorFilter(new android.graphics.ColorMatrixColorFilter(saturate));

        }

        Bitmap bitmapFor(int width, int height) {
            if (bitmap == null || bitmap.getWidth() != width || bitmap.getHeight() != height) {
                bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
                lastPixels = null;
            }
            return bitmap;
        }

        /** Compares the freshly captured backdrop with the previous one (tiny, so this is cheap). */
        boolean backdropChanged(Bitmap captured) {
            int[] pixels = new int[captured.getWidth() * captured.getHeight()];
            captured.getPixels(pixels, 0, captured.getWidth(), 0, 0, captured.getWidth(), captured.getHeight());
            boolean changed = !java.util.Arrays.equals(pixels, lastPixels);
            lastPixels = pixels;
            if (changed && onLuma != null && pixels.length > 0) {
                float sum = 0f;
                for (int c : pixels) {
                    sum += (0.299f * ((c >> 16) & 0xFF) + 0.587f * ((c >> 8) & 0xFF) + 0.114f * (c & 0xFF)) / 255f;
                }
                onLuma.onLuma(sum / pixels.length);
            }
            return changed;
        }

        void setDark(boolean dark) {
            this.dark = dark;
            // Darker glass under white icons so they keep their contrast on dark screens.
            basePaint.setColor(dark ? 0x80101216 : 0x00000000);
            invalidateSelf();
        }

        @Override
        protected void onBoundsChange(Rect bounds) {
            rect.set(bounds);
            float radius = Math.min(bounds.width(), bounds.height()) / 2f;
            clip.reset();
            clip.addRoundRect(rect, radius, radius, android.graphics.Path.Direction.CW);
            hair.set(rect.left + density / 2f, rect.top + density / 2f, rect.right - density / 2f, rect.bottom - density / 2f);
            float inset = density * 1.25f;
            edge.set(rect.left + inset, rect.top + inset, rect.right - inset, rect.bottom - inset);
            // Specular rim: thin on top, bright and thick towards the bottom, like a lit glass edge.
            edgePaint.setShader(new android.graphics.LinearGradient(0, rect.top, 0, rect.bottom,
                    new int[]{0xE6FFFFFF, 0x00FFFFFF, 0x00FFFFFF, 0x40FFFFFF},
                    new float[]{0f, 0.3f, 0.7f, 1f}, android.graphics.Shader.TileMode.CLAMP));
            // CSS: radial-gradient(130% 100% at 50% 0%, .45 -> .22 at 45% -> .15), an ellipse from the top centre.
            android.graphics.RadialGradient radial = new android.graphics.RadialGradient(0f, 0f, 1f,
                    new int[]{0x73FFFFFF, 0x38FFFFFF, 0x26FFFFFF},
                    new float[]{0f, 0.45f, 1f}, android.graphics.Shader.TileMode.CLAMP);
            android.graphics.Matrix m = new android.graphics.Matrix();
            m.setScale(rect.width() * 1.3f, rect.height());
            m.postTranslate(rect.centerX(), rect.top);
            radial.setLocalMatrix(m);
            sheenPaint.setShader(radial);
        }

        @Override
        public void draw(Canvas canvas) {
            canvas.save();
            canvas.clipPath(clip);
            if (bitmap != null) {
                canvas.drawBitmap(bitmap, null, rect, bitmapPaint);
            }
            canvas.drawRect(rect, basePaint);
            canvas.drawRect(rect, tintPaint);
            canvas.restore();
            float hr = Math.min(hair.width(), hair.height()) / 2f;
            canvas.drawRoundRect(hair, hr, hr, hairPaint);
        }

        @Override
        public void setAlpha(int alpha) {
            bitmapPaint.setAlpha(alpha);
        }

        @Override
        public void setColorFilter(ColorFilter colorFilter) {
            bitmapPaint.setColorFilter(colorFilter);
        }

        @Override
        public void getOutline(android.graphics.Outline outline) {
            // The shadow follows the pill's rounded shape.
            outline.setRoundRect(getBounds(), Math.min(getBounds().width(), getBounds().height()) / 2f);
            outline.setAlpha(1f);
        }

        @Override
        public int getOpacity() {
            return PixelFormat.TRANSLUCENT;
        }
    }
}
