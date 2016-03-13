package corp.katet.evernote.image;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.graphics.Rect;
import android.os.AsyncTask;
import android.support.v4.app.FragmentActivity;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageView;

import java.util.Map;

import corp.katet.evernote.R;
import corp.katet.evernote.util.BitmapCache;
import corp.katet.evernote.util.Callback;

public abstract class ImageRetriever extends AsyncTask<String, Void, Map.Entry<String, Bitmap>> {

    protected int size;
    protected String format;
    protected ImageView mCurrentThumbView, mExpandedImageView;
    protected BitmapCache mBitmapCache;
    protected FragmentActivity context;
    protected Callback<Bitmap> callback;

    public ImageRetriever(int size, String format, int imageViewId, FragmentActivity context,
                          Callback<Bitmap> callback) {
        this.size = size;
        this.format = format;
        this.context = context;
        this.mExpandedImageView = (ImageView) context.findViewById(imageViewId);
        this.callback = callback;
        this.mBitmapCache = BitmapCache.getInstance(context.getSupportFragmentManager(),
                context.getResources().getInteger(R.integer.max_memory_size));
    }

    /**
     * onPostExecute displays the results of the AsyncTask once they have been fully
     * processed and generated
     */
    @Override
    protected void onPostExecute(Map.Entry<String, Bitmap> resultImage) {
        if (!isCancelled() && resultImage != null && resultImage.getValue() != null) {
            mBitmapCache.addBitmapToMemoryCache(resultImage.getKey(), resultImage.getValue());
            mExpandedImageView.setImageBitmap(resultImage.getValue());
        }
    }

    // Hold a reference to the current animator,
    // so that it can be canceled mid-way
    private Animator mCurrentAnimator;

    public abstract void loadBitmap(String imageKey);

    // The system "short" animation time duration, in milliseconds.
    // This duration is ideal for subtle animations or animations
    // that occur very frequently.
    final int SHORT_ANIMATION_DURATION = 1500;

    float currentStartScale;
    Rect currentStartBounds;

    public void zoomImageFromThumb(final ImageView thumbView, final String thumbKey) {

        // If there's an animation in progress, cancel it
        // immediately and proceed with this one
        if (mCurrentAnimator != null) {
            mCurrentAnimator.cancel();
        }

        // Load the high-resolution "zoomed-in" image
        loadBitmap(thumbKey);

        // Calculate the starting and ending bounds for the zoomed-in image.
        // This step involves lots of math. Yay, math.
        currentStartBounds = new Rect();
        final Rect finalBounds = new Rect();
        final Point globalOffset = new Point();

        // The start bounds are the global visible rectangle of the thumbnail,
        // and the final bounds are the global visible rectangle of the container view.
        // Also set the container's view offset as the origin for the bounds,
        // since that's the origin for the positioning animation properties (X, Y).
        thumbView.getGlobalVisibleRect(currentStartBounds);
        ((View) mExpandedImageView.getParent()).getGlobalVisibleRect(finalBounds, globalOffset);
        currentStartBounds.offset(-globalOffset.x, -globalOffset.y);
        finalBounds.offset(-globalOffset.x, -globalOffset.y);

        // Adjust the start bounds to the same aspect ratio as the final
        // bounds using the "center crop" technique. This prevents undesirable
        // stretching during the animation. Also calculate the start scaling
        // factor (the end scaling factor is always 1.0).
        if ((float) finalBounds.width() / finalBounds.height()
                > (float) currentStartBounds.width() / currentStartBounds.height()) {
            // Extend start bound horizontally
            currentStartScale = (float) currentStartBounds.height() / finalBounds.height();
            float startWidth = currentStartScale * finalBounds.width();
            float deltaWidth = (startWidth - currentStartBounds.width()) / 2;
            currentStartBounds.left -= deltaWidth;
            currentStartBounds.right += deltaWidth;
        } else {
            // Extend start bounds vertically
            currentStartScale = (float) currentStartBounds.width() / finalBounds.width();
            float startHeight = currentStartScale * finalBounds.height();
            float deltaHeight = (startHeight - currentStartBounds.height()) / 2;
            currentStartBounds.top -= deltaHeight;
            currentStartBounds.bottom += deltaHeight;
        }

        // Hide the thumbnail and show the zoomed-in view. When the animation begins,
        // the zoomed-in view will be positioned in the place of the thumbnail.
        mExpandedImageView.setVisibility(View.VISIBLE);

        // Set the pivot point for SCALE_X and SCALE_Y transformations
        // to the top-left corner of the zoomed-in view (the default
        // is the center of the view)
        mExpandedImageView.setPivotX(0f);
        mExpandedImageView.setPivotY(0f);

        // Construct and run the parallel animation of the four translation and
        // scale properties (X, Y, SCALE_X, and SCALE_Y)
        AnimatorSet set = new AnimatorSet();
        set
                .play(ObjectAnimator.ofFloat(mExpandedImageView, View.X,
                        currentStartBounds.left, finalBounds.left))
                .with(ObjectAnimator.ofFloat(mExpandedImageView, View.Y,
                        currentStartBounds.top, finalBounds.top))
                .with(ObjectAnimator.ofFloat(mExpandedImageView, View.SCALE_X, currentStartScale, 1f))
                .with(ObjectAnimator.ofFloat(mExpandedImageView, View.SCALE_Y, currentStartScale, 1f));
        set.setDuration(SHORT_ANIMATION_DURATION);
        set.setInterpolator(new DecelerateInterpolator());
        set.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                mCurrentAnimator = null;
            }

            @Override
            public void onAnimationCancel(Animator animation) {
                mCurrentAnimator = null;
            }
        });
        set.start();
        mCurrentAnimator = set;
        mCurrentThumbView = thumbView;
    }

    public void zoomBackImageToThumb() {
        // Upon clicking the zoomed-in image, it should zoom back down
        // to the original bounds and show the thumbnail instead of the expanded image
        final float startScaleFinal = currentStartScale;
        if (mCurrentAnimator != null) {
            mCurrentAnimator.cancel();
        }

        // Animate the four positioning/sizing properties in parallel,
        // back to their original values
        AnimatorSet set = new AnimatorSet();
        set.play(ObjectAnimator.ofFloat(mExpandedImageView, View.X, currentStartBounds.left))
                .with(ObjectAnimator.ofFloat(mExpandedImageView, View.Y, currentStartBounds.top))
                .with(ObjectAnimator.ofFloat(mExpandedImageView,
                        View.SCALE_X, startScaleFinal))
                .with(ObjectAnimator.ofFloat(mExpandedImageView,
                        View.SCALE_Y, startScaleFinal));
        set.setDuration(SHORT_ANIMATION_DURATION);
        set.setInterpolator(new DecelerateInterpolator());
        set.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                mExpandedImageView.setVisibility(View.GONE);
                mCurrentAnimator = null;
            }

            @Override
            public void onAnimationCancel(Animator animation) {
                mExpandedImageView.setVisibility(View.GONE);
                mCurrentAnimator = null;
            }
        });
        set.start();
        mCurrentAnimator = set;
    }

    public ImageView getCurrentThumbView() {
        return mCurrentThumbView;
    }
}
