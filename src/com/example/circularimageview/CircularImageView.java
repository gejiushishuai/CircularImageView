package com.example.circularimageview;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;

/**
 * 1.��xml�л�ȡ������Ϣ�������ǰ뾶��Բ�� 2.���ݰ뾶��Բ�ģ���onMeasure�����ÿؼ��Ĵ�СΪ�뾶
 * 3.��onSizeChanged�и��ݵõ��Ŀؼ���Сȥ�ü�ͼƬ�����Bitmap 4.��onDraw�л���bitmap
 * */

public class CircularImageView extends View {

	public final static String TAG = "CircularImageView";

	public Bitmap srcBitmap; // δ�ü�ԭͼ
	public Bitmap dstBitmap; // �ü���Բͼ
	public Bitmap scaleBitmap; // ������ʾ������ͼ
	public Canvas myCanvas; // �����ü�scaleBitmap�Ļ���
	public Paint mPaint; // �����ü�scaleBitmap�Ļ���
	public Paint borderPaint; // �����ü��߽�Ļ���
	public int width; // �ؼ����
	public int height; // �ؼ��߶�
	public int srcWidth; // ԭʼBitmap���
	public int srcHeight; // ԭʼBitmap�߶�
	public int radius; // Բ��ͷ��뾶,�����ü�ԭʼBitmap����������Բ��Bitmap�뾶Ҳ�����û�ָ���İ뾶����

	public int borderWidth; // �߿��ܿ��
	public int inBorderWidth = 0; // ��Բ��ȣ�Ĭ��Ϊ0,������
	public int betweenWidth; // ��ȿ
	public int outBorderWidth = 0; // ��Բ��ȣ�Ĭ��Ϊ0��������

	public int borderColor; // �߿���ɫ
	public int inBorderColor = Color.BLACK; // Ĭ����ԲΪ��ɫ
	public int betweenColor = Color.WHITE; // Ĭ�ϻ�ȿΪ��ɫ�����δ������Բ����Բ��ȫΪ��ɫ
	public int outBorderColor = Color.BLACK; // Ĭ����ԲΪ��ɫ

	public float centerScaleX = 0; // ͷ�����ĵ�λ��X�����
	public float centerScaleY = 0; // ͷ�����ĵ�λ��Y�����
	public float centerRadius; // ԭʼͼƬ��ָ���İ뾶(����)

	public final static PorterDuffXfermode modeInside; // Բ��ͷ�����ı����²�
	public final static PorterDuffXfermode modeOutsize; // ���α߿򱣴��ϲ�

	static {
		modeInside = new PorterDuffXfermode(PorterDuff.Mode.DST_IN);
		modeOutsize = new PorterDuffXfermode(PorterDuff.Mode.SRC_IN);
	}

	public CircularImageView(Context context) {
		super(context);
		init();
		initPaint();
	}

	public CircularImageView(Context context, AttributeSet attrs) {
		super(context, attrs);

		// ���������Ϣ
		TypedArray array = context.obtainStyledAttributes(attrs,
				R.styleable.CircularImage);
		int id = array.getResourceId(
				R.styleable.CircularImage_CircularImageSrc, 0);
		if (id != 0) {
			srcBitmap = BitmapFactory.decodeResource(getResources(), id);
			srcWidth = srcBitmap.getWidth();
			srcHeight = srcBitmap.getHeight();
		}
		// Բ����ɫ�ֲ�
		borderColor = array.getColor(R.styleable.CircularImage_BorderColor,
				Color.BLACK);
		inBorderColor = array.getColor(R.styleable.CircularImage_InBorderColor,
				Color.BLACK);
		betweenColor = array.getColor(R.styleable.CircularImage_BetweenColor,
				Color.WHITE);
		outBorderColor = array.getColor(
				R.styleable.CircularImage_OutBorderColor, Color.BLACK);
		// Բ����ȷֲ�
		borderWidth = array.getDimensionPixelSize(
				R.styleable.CircularImage_BorderWidth, 0);
		inBorderWidth = array.getDimensionPixelSize(
				R.styleable.CircularImage_InBorderWidth, 0);
		betweenWidth = array.getDimensionPixelSize(
				R.styleable.CircularImage_BetweenWidth, 0);
		outBorderWidth = array.getDimensionPixelSize(
				R.styleable.CircularImage_OutBorderWidth, 0);
		// Բ�����ĵ�λ��
		centerScaleX = array.getFloat(R.styleable.CircularImage_CenterScaleX,
				(float) -1);
		centerScaleY = array.getFloat(R.styleable.CircularImage_CenterScaleY,
				(float) -1);
		centerRadius = array.getFloat(R.styleable.CircularImage_CenterRadius, 0);
		array.recycle();

		init();
		initPaint();
	}

	public void initPaint() {
		mPaint = new Paint();
		mPaint.setAntiAlias(true);
		mPaint.setFilterBitmap(false);
		borderPaint = new Paint();
		borderPaint.setAntiAlias(true);// �����
		borderPaint.setStyle(Paint.Style.STROKE);// ���ÿ���Բ
	}

	public void init() {
		if (centerScaleX == 0 || centerScaleY == 0) {
			// ��̬��ӿؼ�
			centerScaleX = 0.5f;
			centerScaleY = 0.5f;
		}
		if(srcBitmap==null){
			return;
		}else{
			srcWidth = srcBitmap.getWidth();
			srcHeight = srcBitmap.getHeight();
		}
		
		// δ�������ĵ㣬Ĭ����ͼƬ�����ģ��뾶�������̵ľ���
		if (centerScaleX == -1 || centerScaleY == -1) {
			centerScaleX = 0.5F;
			centerScaleY = 0.5F;
			radius = ((srcWidth < srcHeight) ? srcWidth : srcHeight) / 2;
		} else if (centerScaleX != -1 && centerScaleY != -1) {
			// ��Ϊ��ָ�������ĵ㣬Ҫ�Ƚ��к������ж�
			if (centerScaleX <= 0 || centerScaleX >= 1 || centerScaleY <= 0 || centerScaleY >= 1) {
				throw new RuntimeException(
						"centerScaleX and centerScaleY must between 0 and 1");
			}
			if (centerRadius == 0) {
				// ��߽���̾�����Ϊ�뾶
				int w = (int) ((srcWidth * centerScaleX < (srcWidth - srcWidth
						* centerScaleX)) ? srcWidth * centerScaleX
						: (srcWidth - srcWidth * centerScaleX));
				int h = (int) ((srcHeight * centerScaleY < (srcHeight - srcHeight
						* centerScaleY)) ? srcHeight * centerScaleY
						: (srcHeight - srcHeight * centerScaleY));
				radius = (w < h) ? w : h;
			} else {
				// ��Ϊ�����˱����뾶���������߽���̾���ȽϺ�ʹ�ý�С��ֵ
				int w = (int) ((srcWidth * centerScaleX < (srcWidth - srcWidth
						* centerScaleX)) ? srcWidth * centerScaleX
						: (srcWidth - srcWidth * centerScaleX));
				int h = (int) ((srcHeight * centerScaleY < (srcHeight - srcHeight
						* centerScaleY)) ? srcHeight * centerScaleY
						: (srcHeight - srcHeight * centerScaleY));
				int min = (w < h) ? w : h;
				int r = (int) (centerRadius * ((srcWidth < srcHeight) ? srcWidth
						: srcHeight));
				radius = (min < r) ? min : r;
			}
		}
	}

	@Override
	public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		int resultWidth = 0;
		int modeWidth = MeasureSpec.getMode(widthMeasureSpec);
		int sizeWidth = MeasureSpec.getSize(widthMeasureSpec);
		if (modeWidth == MeasureSpec.EXACTLY) {
			resultWidth = sizeWidth;
		} else {
			if(srcBitmap!=null){
				// ���Ϊwrap_content����Ƚ�Բ��ͷ���С�͸������������ֵsizeWidth
				int length = radius * 2;
				if (modeWidth == MeasureSpec.AT_MOST) {
					resultWidth = Math.min(sizeWidth, length);
				}
			}else{
				//��̬���
				resultWidth=sizeWidth;
			}
			
		}

		int resultHeight = 0;
		int modeHeight = MeasureSpec.getMode(heightMeasureSpec);
		int sizeHeight = MeasureSpec.getSize(heightMeasureSpec);
		if (modeHeight == MeasureSpec.EXACTLY) {
			resultHeight = sizeHeight;
		} else {
			if(srcBitmap!=null){
				// ���Ϊwrap_content����Ƚ�Բ��ͷ���С�͸������������ֵsizeHeight
				int length = radius * 2;
				if (modeHeight == MeasureSpec.AT_MOST) {
					resultHeight = Math.min(sizeHeight, length);
				}
			}else{
				//��̬���
				resultHeight=sizeHeight;
			}
		}

		setMeasuredDimension(resultWidth, resultHeight);
	}

	@Override
	public void onSizeChanged(int w, int h, int oldw, int oldh) {
		super.onSizeChanged(w, h, oldw, oldh);
		width = w;
		height = h;
		Log.i(TAG, "width:" + width);
		Log.i(TAG, "height:" + height);
		create();
	}

	public void create() {
		// �ڴ˴���òü����Bitmap
		dstBitmap = createDstBitmap();
		// �����scaleBitmap������Բ�εģ���������createScaledBitmapִ��ʱ��width,height����任
		if(dstBitmap!=null){
			scaleBitmap = Bitmap.createScaledBitmap(dstBitmap, width, height, true);
		}
	}

	/**
	 * ���ݿؼ��Ĵ�С�ü�Bitmap���裺 ����Բ�ļ��뾶�ü�Բ�����������µ�Bitmap
	 * */
	public Bitmap createDstBitmap() {
		if(srcBitmap==null){
			return null;
		}
		int centerX = (int) (srcBitmap.getWidth() * centerScaleX);
		int centerY = (int) (srcBitmap.getHeight() * centerScaleY);
		dstBitmap = Bitmap.createBitmap(radius * 2, radius * 2,
				Bitmap.Config.ARGB_8888);
		myCanvas = new Canvas(dstBitmap);

		// ��û�������Canvas���вü�
		int j = myCanvas.saveLayer(0, 0, radius * 2, radius * 2, null,
				Canvas.ALL_SAVE_FLAG);
		int x = centerX - radius;
		int y = centerY - radius;
		Bitmap bm = Bitmap
				.createBitmap(srcBitmap, x, y, radius * 2, radius * 2);
		myCanvas.drawBitmap(bm, 0, 0, mPaint);
		mPaint.setXfermode(modeInside);
		myCanvas.drawBitmap(createMask(), 0, 0, mPaint);
		mPaint.setXfermode(null);
		myCanvas.restoreToCount(j);

		if (betweenWidth != 0 || borderWidth != 0) {
			mPaint.setStyle(Paint.Style.STROKE);
			myCanvas.drawBitmap(createBorder(), 0, 0, mPaint);
		}
		return dstBitmap;
	}

	@Override
	public void onDraw(Canvas canvas) {
		super.onDraw(canvas);
		if(scaleBitmap!=null){
			canvas.drawBitmap(scaleBitmap, 0, 0, null);
		}
	}

	// ͨ���ڽӾ���������Բ������,
	public Bitmap createMask() {
		Bitmap bm = Bitmap.createBitmap(radius * 2, radius * 2,
				Bitmap.Config.ARGB_8888);
		Canvas canvas = new Canvas(bm);
		Paint paint = new Paint(1);
		paint.setColor(getResources().getColor(android.R.color.holo_blue_light));
		// Բ�����еľ���
		RectF rectF = new RectF(0, 0, dstBitmap.getWidth(),
				dstBitmap.getHeight());
		canvas.drawArc(rectF, 0, 360, true, paint);

		return bm;
	}

	// ���α߽縲��ԭͼ��ı߽�:���������򡪡���Բ����ȿ����Բ,Ĭ��ֻ���ƻ�ȿ(������࿪ʼ����)
	public Bitmap createBorder() {

		Bitmap bm = Bitmap.createBitmap(radius * 2, radius * 2,
				Bitmap.Config.ARGB_8888);
		Canvas canvas = new Canvas(bm);
		// δ������Բ����Բ��ֻ���ƻ�ȿ���Ҹú����ı����õ�ǰ����betweenWidth��borderWidth��ȫΪ0
		if (outBorderWidth == 0 && inBorderWidth == 0) {

			if (betweenWidth != 0 && borderWidth == 0) {
				borderWidth = betweenWidth;
			}
			// ʹ��borderWidth���������뾶radius=realRadius-borderWidth/2
			borderPaint.setColor(borderColor);
			borderPaint.setStrokeWidth(borderWidth);
			canvas.drawCircle(radius, radius, radius - borderWidth / 2,
					borderPaint);
			return bm;
		}

		// ��������Բ����Բ���Ҹú����ı����õ�ǰ����betweenWidth��borderWidth��ȫΪ0
		if (outBorderWidth != 0 || inBorderWidth != 0) {

			// ��Բ����ȿ����Բ��Ҫ����
			if (outBorderWidth != 0 && inBorderWidth != 0) {
				// �����ȿ�Ŀ��δ���ã������ܿ��borderWidth��ȥ��Բ����Բ�Ŀ��
				// �������˻�ȿ�Ŀ�ȣ���ô�ܿ��borderWidth�����ƾͲ����������ˡ�
				if (betweenWidth == 0) {
					betweenWidth = borderWidth - inBorderWidth - outBorderWidth;
				}
				// ��Բ�Ļ���
				borderPaint.setColor(outBorderColor);
				borderPaint.setStrokeWidth(outBorderWidth);
				canvas.drawCircle(radius, radius, radius - outBorderWidth / 2,
						borderPaint);
				// ��ȿ�Ļ���
				borderPaint.setColor(betweenColor);
				borderPaint.setStrokeWidth(betweenWidth);
				canvas.drawCircle(radius, radius, radius - outBorderWidth
						- betweenWidth / 2, borderPaint);
				// ��Բ�Ļ���
				borderPaint.setColor(inBorderColor);
				borderPaint.setStrokeWidth(inBorderWidth);// ��Ϊ2
				canvas.drawCircle(radius, radius, radius - outBorderWidth
						- betweenWidth - inBorderWidth / 2, borderPaint);
				return bm;
			}

			// ֻ���ƻ�ȿ����Բ
			if (outBorderWidth != 0 && inBorderWidth == 0) {
				if (betweenWidth == 0) {
					betweenWidth = borderWidth - outBorderWidth;
				}
				// ��Բ�Ļ���
				borderPaint.setColor(outBorderColor);
				borderPaint.setStrokeWidth(outBorderWidth);
				canvas.drawCircle(radius, radius, radius - outBorderWidth / 2,
						borderPaint);
				// ��ȿ�Ļ���
				borderPaint.setColor(betweenColor);
				borderPaint.setStrokeWidth(betweenWidth);
				canvas.drawCircle(radius, radius, radius - outBorderWidth
						- betweenWidth / 2, borderPaint);

				return bm;
			}
			// ֻ���ƻ�ȿ����Բ
			if (outBorderWidth == 0 && inBorderWidth != 0) {
				if (betweenWidth == 0) {
					betweenWidth = borderWidth - inBorderWidth;
				}
				// ��ȿ�Ļ���
				borderPaint.setColor(betweenColor);
				borderPaint.setStrokeWidth(betweenWidth);
				canvas.drawCircle(radius, radius, radius - betweenWidth / 2,
						borderPaint);
				// ��Բ�Ļ���
				borderPaint.setColor(inBorderColor);
				borderPaint.setStrokeWidth(inBorderWidth);// ��Ϊ2
				canvas.drawCircle(radius, radius, radius - betweenWidth
						- inBorderWidth / 2, borderPaint);
				return bm;
			}
		}
		return null;
	}

	public void setImageBitmap(Bitmap bm){
		if(bm!=null){
			//���²����ͻ���
			srcBitmap=bm;
			init();
			super.requestLayout();
			invalidate();
		}
	}
	
}
