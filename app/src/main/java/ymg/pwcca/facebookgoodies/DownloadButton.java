package ymg.pwcca.facebookgoodies;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.ContextWrapper;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ScrollView;
import java.lang.ref.WeakReference;

/** Shared icon, activity lookup and progress UI for the native sidebar action. */
final class DownloadButton {
    private DownloadButton() {}
    static Drawable icon(int pixels) { return new Arrow(pixels); }
    static Activity activity(Context context) {
        for (int depth = 0; depth < 16; depth++) {
            if (context instanceof Activity) return (Activity) context;
            if (!(context instanceof ContextWrapper)) return null;
            Context next = ((ContextWrapper) context).getBaseContext();
            if (next == context) return null;
            context = next;
        }
        return null;
    }
    // These views use Facebook's context; module resource IDs cannot be resolved by the host.
    @android.annotation.SuppressLint("SetTextI18n")
    static VideoDownloader.Listener progress(Activity owner, VideoDownloader downloader, HostState state) {
        TextView text = new TextView(owner);
        int pad = dp(owner, 20); text.setPadding(pad, pad / 2, pad, pad / 2);
        text.setText("Reading current video…"); text.setTextSize(14);
        text.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);
        ScrollView scroll=new ScrollView(owner);scroll.addView(text);
        AlertDialog dialog = new AlertDialog.Builder(owner).setTitle("Download video").setView(scroll)
                .setNegativeButton("Cancel", (d, which) -> downloader.cancel()).create();
        dialog.setCanceledOnTouchOutside(false);
        dialog.setOnCancelListener(d -> downloader.cancel());
        dialog.show();
        scroll.getLayoutParams().height=Math.min(dp(owner,360),owner.getResources().getDisplayMetrics().heightPixels/2);scroll.requestLayout();
        WeakReference<AlertDialog> weakDialog = new WeakReference<>(dialog);
        WeakReference<TextView> weakText = new WeakReference<>(text);
        Context app = owner.getApplicationContext();
        return new VideoDownloader.Listener() {
            @Override public void progress(String message) {
                TextView label = weakText.get();
                if (label != null && label.isAttachedToWindow()) label.setText(message);
            }
            @Override public void finished(String message, boolean success) {
                AlertDialog ui = weakDialog.get();
                if (ui != null && ui.isShowing()) {
                    TextView label=weakText.get();if(label!=null)label.append("\n\n"+message);
                    ui.getButton(AlertDialog.BUTTON_NEGATIVE).setText("Close");
                    ui.getButton(AlertDialog.BUTTON_NEGATIVE).setOnClickListener(view->ui.dismiss());
                    ui.setOnCancelListener(null);
                }
                if (success) state.downloads.incrementAndGet();
                else state.downloadFailures.incrementAndGet();
                Toast.makeText(app, message, Toast.LENGTH_LONG).show();
            }
        };
    }
    private static int dp(Context c, int value) { return Math.round(value * c.getResources().getDisplayMetrics().density); }
    private static final class Arrow extends Drawable {
        final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        final int size;
        Arrow(int size) { this.size = size; }
        @Override public int getIntrinsicWidth() { return size; }
        @Override public int getIntrinsicHeight() { return size; }
        @Override public void draw(Canvas canvas) {
            canvas.save(); canvas.translate(getBounds().left, getBounds().top);
            canvas.scale(getBounds().width() / 24f, getBounds().height() / 24f);
            paint.setColor(0xffffffff); paint.setStrokeWidth(2); paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeCap(Paint.Cap.ROUND); paint.setStrokeJoin(Paint.Join.ROUND);
            Path p = new Path(); p.moveTo(12, 3); p.lineTo(12, 15); p.moveTo(7, 10); p.lineTo(12, 15); p.lineTo(17, 10);
            p.moveTo(4, 17); p.lineTo(4, 21); p.lineTo(20, 21); p.lineTo(20, 17); canvas.drawPath(p, paint); canvas.restore();
        }
        @Override public void setAlpha(int alpha) { paint.setAlpha(alpha); }
        @Override public void setColorFilter(ColorFilter filter) { paint.setColorFilter(filter); }
        @Override public int getOpacity() { return PixelFormat.TRANSLUCENT; }
    }
}
