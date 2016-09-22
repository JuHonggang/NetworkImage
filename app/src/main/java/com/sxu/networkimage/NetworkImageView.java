package com.sxu.networkimage;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.drawable.Animatable;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.widget.ImageView;

import com.facebook.common.executors.CallerThreadExecutor;
import com.facebook.common.references.CloseableReference;
import com.facebook.datasource.BaseDataSubscriber;
import com.facebook.datasource.DataSource;
import com.facebook.datasource.DataSubscriber;
import com.facebook.drawee.backends.pipeline.Fresco;
import com.facebook.drawee.controller.BaseControllerListener;
import com.facebook.drawee.controller.ControllerListener;
import com.facebook.drawee.drawable.ScalingUtils;
import com.facebook.drawee.generic.GenericDraweeHierarchy;
import com.facebook.drawee.generic.GenericDraweeHierarchyBuilder;
import com.facebook.drawee.generic.RoundingParams;
import com.facebook.drawee.interfaces.DraweeController;
import com.facebook.drawee.view.SimpleDraweeView;
import com.facebook.imagepipeline.core.ImagePipeline;
import com.facebook.imagepipeline.image.CloseableBitmap;
import com.facebook.imagepipeline.image.CloseableImage;
import com.facebook.imagepipeline.image.ImageInfo;
import com.facebook.imagepipeline.request.ImageRequest;
import com.facebook.imagepipeline.request.ImageRequestBuilder;

/*******************************************************************************
 * FileName: NetworkImageView
 *
 * Description: 二次封装图片加载器，简化替换图片加载器的过程(推荐使用传递ImageView的成员函数，因为大多数
 *              图片加载器不是以View的形式出现的)
 *
 * Author: juhg
 *
 * Version: v1.0
 *
 * Date: 16/9/20
 *******************************************************************************/
public class NetworkImageView extends SimpleDraweeView {

    private int mPlaceHolder;
    private int mErrorHolder;
    private int mShape;
    private int mRadius;
    private int mBorderWidth;
    private int mBorderColor;

    private Matrix drawMatrix;
    public static final int SHAPE_NORMAL = 0;
    public static final int SHAPE_CIRCLE = 1;
    public static final int SHAPE_ROUND = 2;

    public NetworkImageView(Context context) {
        super(context);
    }

    public NetworkImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
        TypedArray arrays = context.obtainStyledAttributes(attrs, R.styleable.NetworkImageView);
        mPlaceHolder = arrays.getResourceId(R.styleable.NetworkImageView_placeHolder, 0);
        mErrorHolder = arrays.getResourceId(R.styleable.NetworkImageView_errorHolder, 0);
        mShape = arrays.getInt(R.styleable.NetworkImageView_shape, SHAPE_NORMAL);
        mRadius = arrays.getDimensionPixelSize(R.styleable.NetworkImageView_radius, 0);
        mBorderWidth = arrays.getDimensionPixelSize(R.styleable.NetworkImageView_shapeBorderWidth, 0);
        mBorderColor = arrays.getColor(R.styleable.NetworkImageView_borderColor, Color.WHITE);
        arrays.recycle();
    }

    public NetworkImageView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public NetworkImageView(Context context, GenericDraweeHierarchy hierarchy) {
        super(context, hierarchy);
    }

    public void displayImage(int resId) {
        setImageResource(resId);
    }

    public void displayImage(String url) {
        displayImage(url, mPlaceHolder, mErrorHolder);
    }

    public void displayImage(String url, ImageView.ScaleType scaleType) {
        displayImage(url, mPlaceHolder, mErrorHolder, scaleType);
    }

    public void displayImage(String url, int placeHolder, int failureImage) {
        displayImage(url, placeHolder, failureImage, getScaleType());
    }

    public void displayImage(String url, int placeHolder, int failureImage, ImageView.ScaleType scaleType) {
        RoundingParams roundingParams = null;
        if (mShape == SHAPE_CIRCLE) {
            roundingParams = RoundingParams.asCircle();
            roundingParams.setBorder(mBorderColor, mBorderWidth);
        } else if (mShape == SHAPE_ROUND){
            roundingParams = RoundingParams.fromCornersRadius(mRadius);
            roundingParams.setBorder(mBorderColor, mBorderWidth);
        } else {
            /**
             * Nothing
             */
        }
        GenericDraweeHierarchyBuilder builder = new GenericDraweeHierarchyBuilder(getResources());
        if (placeHolder != 0) {
            builder.setPlaceholderImage(placeHolder);
        }
        if (failureImage != 0) {
            builder.setFailureImage(failureImage);
        }
        if (mShape != SHAPE_NORMAL) {
            builder.setRoundingParams(roundingParams);
        }
        GenericDraweeHierarchy hierarchy = builder.build();
        setScaleType(hierarchy, scaleType);
        setHierarchy(hierarchy);
        setImageURI(Uri.parse(url));
    }

    /**
     * 设置图片的显示方式
     *
     * 注意: 对于不同的图片加载库,此函数的第一个参数可能不同,封装时需要进行修改
     * @param hierarchy
     * @param scaleType
     */
    private void setScaleType(GenericDraweeHierarchy hierarchy, ImageView.ScaleType scaleType) {
        if (hierarchy != null && scaleType != null) {
            switch (scaleType) {
                case CENTER:
                    hierarchy.setActualImageScaleType(ScalingUtils.ScaleType.CENTER);
                    break;
                case CENTER_CROP:
                    hierarchy.setActualImageScaleType(ScalingUtils.ScaleType.CENTER_CROP);
                    break;
                case CENTER_INSIDE:
                    hierarchy.setActualImageScaleType(ScalingUtils.ScaleType.CENTER_INSIDE);
                    break;
                case FIT_CENTER:
                    hierarchy.setActualImageScaleType(ScalingUtils.ScaleType.FIT_CENTER);
                    break;
                case FIT_START:
                    hierarchy.setActualImageScaleType(ScalingUtils.ScaleType.FIT_START);
                    break;
                case FIT_END:
                    hierarchy.setActualImageScaleType(ScalingUtils.ScaleType.FIT_END);
                    break;
                case FIT_XY:
                    hierarchy.setActualImageScaleType(ScalingUtils.ScaleType.FIT_XY);
                    break;
                default:
                    break;
            }
        }
    }

    /**
     * 监听图片的加载过程，对于只需要加载完成时的情况
     * @param url
     * @param listener
     */
    public void loadImage(String url, final LoadingListener listener) {
        ControllerListener controllerListener = new BaseControllerListener<ImageInfo>() {
            @Override
            public void onFinalImageSet(String id, @Nullable ImageInfo imageInfo, @Nullable Animatable anim) {
                if (listener != null) {
                    if (imageInfo != null) {
                        listener.loadComplete();
                    } else {
                        listener.loadFailure();
                    }
                }
            }

            @Override
            public void onIntermediateImageSet(String id, @Nullable ImageInfo imageInfo) {

            }

            @Override
            public void onFailure(String id, Throwable throwable) {
                if (listener != null) {
                    listener.loadFailure();
                }
            }
        };

        DraweeController controller = Fresco.newDraweeControllerBuilder()
                .setControllerListener(controllerListener)
                .setUri(Uri.parse(url))
                .build();
        setController(controller);
    }

    /**
     * 下载图片并监听下载过程
     * @param url
     * @param listener
     */
    public static void downloadImage(Context context, String url, final DownloadListener listener) {
        DataSubscriber dataSubscriber = new BaseDataSubscriber<CloseableReference<CloseableBitmap>>() {
            @Override
            public void onNewResultImpl(DataSource<CloseableReference<CloseableBitmap>> dataSource) {
                if (listener != null) {
                    if (dataSource.isFinished()) {
                        CloseableReference<CloseableBitmap> imageReference = dataSource.getResult();
                        if (imageReference != null) {
                            final CloseableReference<CloseableBitmap> closeableReference = imageReference.clone();
                            try {
                                CloseableBitmap closeableBitmap = closeableReference.get();
                                Bitmap bitmap = closeableBitmap.getUnderlyingBitmap();
                                if (bitmap != null && !bitmap.isRecycled()) {
                                    listener.downloadFinish(bitmap);
                                } else {
                                    listener.downloadFailure();
                                }
                            } catch (Exception e) {
                                listener.downloadFailure();
                            } finally {
                                imageReference.close();
                                closeableReference.close();
                            }
                        } else {
                            listener.downloadFailure();
                        }
                    } else {
                        listener.downloadFailure();
                    }
                }
            }

            @Override
            public void onFailureImpl(DataSource dataSource) {
                if (listener != null) {
                    listener.downloadFailure();
                }
            }
        };

        ImagePipeline imagePipeline = Fresco.getImagePipeline();
        ImageRequest request = ImageRequestBuilder.newBuilderWithSource(Uri.parse(url)).build();
        DataSource<CloseableReference<CloseableImage>> dataSource = imagePipeline.fetchDecodedImage(request, context);
        dataSource.subscribe(dataSubscriber, CallerThreadExecutor.getInstance());
    }

    /**
     * 加载监听
     *
     * 只告诉客户端图片是否加载完成或加载失败，不会返回任何信息
     */
    public interface LoadingListener {
        void loadComplete();
        void loadFailure();
    }

    /**
     * 图片下载监听
     *
     * 返回下载图片的bitmap
     */
    public interface DownloadListener {
        void downloadFinish(Bitmap bitmap);
        void downloadFailure();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (!(this instanceof SimpleDraweeView) && (mShape == SHAPE_CIRCLE || mShape == SHAPE_ROUND)) {
            Drawable drawable = getDrawable();
            if (drawable != null) {
                Bitmap bitmap = drawableToBitmap(drawable);
                BitmapShader bitmapShader = new BitmapShader(bitmap, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP);
                if (drawMatrix != null) {
                    bitmapShader.setLocalMatrix(drawMatrix);
                }
                Paint paint = new Paint();
                paint.setAntiAlias(true);
                paint.setShader(bitmapShader);
                RectF rect = new RectF(0, 0, getWidth(), getHeight());
                if (mShape == SHAPE_ROUND) {
                    canvas.drawRoundRect(rect, mRadius, mRadius, paint);
                } else {
                    int radius = getWidth() > getHeight() ? getHeight()/2 : getWidth()/2;
                    canvas.drawCircle(getWidth()/2, getHeight()/2, radius, paint);
                }
            }
        } else {
            super.onDraw(canvas);
        }
    }

    private Bitmap drawableToBitmap(Drawable drawable) {
        configureBounds(drawable);
        if (drawable instanceof BitmapDrawable) {
            return ((BitmapDrawable) drawable).getBitmap();
        }

        Bitmap bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        drawable.draw(canvas);

        return bitmap;
    }

    public void setRadius(float radius) {
        this.mRadius = (int) radius;
    }

    public void setShape(int shape) {
        this.mShape = shape;
    }

    public void setBorderWidth(int width) {
        this.mBorderWidth = width;
    }

    public void setBorderColor(int color) {
        this.mBorderColor = color;
    }

    private void configureBounds(Drawable drawable) {
        drawMatrix = getMatrix();
        int drawableWidth = drawable.getIntrinsicWidth();
        int drawableHeight = drawable.getIntrinsicHeight();
        int viewWidth = getWidth() - getPaddingLeft() - getPaddingRight();
        int viewHeight = getHeight() - getPaddingTop() - getPaddingBottom();

        if (drawableWidth <= 0 || drawableHeight <= 0 || (drawableWidth == viewWidth &&
            drawableHeight == viewHeight) && getScaleType() == ImageView.ScaleType.FIT_XY) {
            drawable.setBounds(0, 0, viewWidth, viewHeight);
            drawMatrix = null;
        } else {
            drawable.setBounds(0, 0, drawableWidth, drawableHeight);
            switch (getScaleType()) {
                case CENTER:
                    drawMatrix.setTranslate((int) ((viewWidth - drawableWidth) * 0.5f + 0.5f),
                            (int) ((viewHeight - drawableHeight  ) * 0.5f + 0.5f));
                    break;
                case CENTER_CROP:
                    float scale = 0;
                    float offsetX = 0;
                    float offsetY = 0;
                    if (viewWidth * drawableHeight > drawableWidth * viewHeight) {
                        scale = viewWidth / drawableWidth;
                        offsetY = (viewHeight - drawableHeight * scale) * 0.5f;
                    } else {
                        scale = viewHeight / drawableHeight;
                        offsetX = (drawableWidth - viewWidth * scale) * 0.5f;
                    }
                    drawMatrix.setScale(scale, scale);
                    drawMatrix.postTranslate((int)(offsetX + 0.5f), (int)(offsetY + 0.5f));
                    break;
                case CENTER_INSIDE:
                    if (drawableWidth <= viewWidth && drawableHeight <= viewHeight) {
                        scale = 1.0f;
                    } else {
                        scale = Math.min(viewWidth / drawableWidth, viewHeight / drawableHeight);
                    }
                    offsetX = viewWidth - drawableWidth * scale;
                    offsetY = viewHeight - drawableHeight * scale;
                    drawMatrix.setScale(scale, scale);
                    drawMatrix.postTranslate((int)(offsetX + 0.5f), (int)(offsetY + 0.5f));
                    break;
                default:
                    break;
            }
        }
    }
}
