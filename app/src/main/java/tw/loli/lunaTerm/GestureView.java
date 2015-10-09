package tw.loli.lunaTerm;


import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.Paint.Style;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Display;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;

public class GestureView extends View implements View.OnLongClickListener{

	private final String TAG = "GestureView";
	
	private Point lastTouchedPoint;
	private Point startPoint;
	
	private Bitmap footprintBitmap;
	private final Paint footprintPaint;
	private Canvas footprintCanvas;
	private int footprintColor = Color.RED;
	private int footprintWidth = 5;

	private Bitmap textBitmap;
	private final Paint textPaint;
	private Canvas textCanvas;
	private int textBgColor = Color.BLUE;
	private int textColor = Color.WHITE;
	
	private final Rect mRect = new Rect();

	private OnGestureListener mOnGestureListener;
	
	private boolean magnifierOn = false;	
	private boolean fingerOnScreen = false;
	
	private int MAGNIFIER_HEIGHT = 100;
	private int MAGNIFIER_WIDTH = 200;
	private int MAGNIFIER_MARGIN = 50;
	private int MAGNIFIER_FOCUS_HEIGHT = 50;
	private int MAGNIFIER_FOCUS_WIDTH = 100;
	
	private TerminalActivity terminalActivity;

	public void setTerminalActivity(TerminalActivity terminalActivity) {
		this.terminalActivity = terminalActivity;
	}

	public OnGestureListener getOnGestureListener() {
		return mOnGestureListener;
	}

	public void setOnGestureListener(OnGestureListener onGestureListener) {
		this.mOnGestureListener = onGestureListener;
	}

	public GestureView(Context c, AttributeSet attrs) {
		super(c, attrs);

		footprintPaint = new Paint();
		footprintPaint.setAntiAlias(true);
		footprintPaint.setColor(footprintColor);
		footprintPaint.setStyle(Paint.Style.STROKE);
		footprintPaint.setStrokeWidth(footprintWidth);

		textPaint = new Paint();
		textPaint.setAntiAlias(true);
		textPaint.setTextSize(15);
		textPaint.setTypeface(Typeface.MONOSPACE);
		setOnLongClickListener(this);  
	}

	@Override
	protected void onSizeChanged(int w, int h, int oldw, int oldh) {
		super.onSizeChanged(w, h, oldw, oldh);

		footprintBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_4444);
		footprintCanvas = new Canvas();
		footprintCanvas.setBitmap(footprintBitmap);

		textBitmap = Bitmap.createBitmap(w, 20, Bitmap.Config.ARGB_4444);
		textCanvas = new Canvas();
		textCanvas.setBitmap(textBitmap);

	}
	
	
	public void setMagnifierParms(int fwidth, int fheight, int zoom){
		// At the time parms set, view is not measured yet
		final Display display = ((WindowManager) getContext().getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay(); 
		
		/* we map zoom value(0~100) to 1x~4x */
		final float rate = 1 + (zoom/100f)*3;
		
		if((TerminalActivity.termActFlags & TerminalActivity.FLAG_MAGNIFIER_FULLSCREEN) !=0){
			MAGNIFIER_WIDTH = display.getWidth();
			MAGNIFIER_HEIGHT = display.getHeight();
			
			MAGNIFIER_FOCUS_WIDTH = Math.round(MAGNIFIER_WIDTH / rate);
			MAGNIFIER_FOCUS_HEIGHT = Math.round(MAGNIFIER_HEIGHT / rate);
		}else{		
			MAGNIFIER_FOCUS_WIDTH =  (int) ((float)fwidth / 100 * display.getWidth());
			MAGNIFIER_FOCUS_HEIGHT = (int) ((float)fheight / 100 * display.getHeight());	
	
			MAGNIFIER_WIDTH = (int)Math.ceil(rate * MAGNIFIER_FOCUS_WIDTH);
			MAGNIFIER_HEIGHT = (int)Math.ceil(rate * MAGNIFIER_FOCUS_HEIGHT);
		}
	}
	
	
	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);
		if(magnifierOn && (fingerOnScreen || (TerminalActivity.termActFlags & TerminalActivity.FLLAG_LONG_PRESS_ACTIVATE) !=0)){
			Rect magnifier = new Rect();
			Paint mPaint = new Paint();
			if((TerminalActivity.termActFlags & TerminalActivity.FLAG_MAGNIFIER_FULLSCREEN) !=0){
				magnifier.set(0, 0, MAGNIFIER_WIDTH, MAGNIFIER_HEIGHT);				
			}else{
				/* Place magnifier on top on finger if possible */
				magnifier.top = lastTouchedPoint.y - MAGNIFIER_HEIGHT - MAGNIFIER_MARGIN;
				magnifier.left = lastTouchedPoint.x - MAGNIFIER_WIDTH/2;
			
				if(magnifier.top<0){
					/* if no space left on top, place it right */
					magnifier.top = lastTouchedPoint.y - MAGNIFIER_HEIGHT/2;
					if(magnifier.top<0)
						magnifier.top = 0;
					magnifier.left = lastTouchedPoint.x + MAGNIFIER_MARGIN;
					/* if no place at right, put it left */
					if(magnifier.left+MAGNIFIER_WIDTH>getWidth())
						magnifier.left = lastTouchedPoint.x - MAGNIFIER_WIDTH - MAGNIFIER_MARGIN;				 
				}
				if(magnifier.left < 0)
					magnifier.left = 0;	
				
				magnifier.right = magnifier.left + MAGNIFIER_WIDTH;
				magnifier.bottom = magnifier.top + MAGNIFIER_HEIGHT;
				
				if(magnifier.right > getWidth())
					magnifier.right = getWidth();			
					
				mPaint.setColor(Color.WHITE);
				canvas.drawRect(magnifier.left - 1, magnifier.top -1, magnifier.right+1, magnifier.bottom +1, mPaint); //Draw border			
			}
			/* clean magnifier area */
			mPaint.setColor(Color.BLACK);
			canvas.drawRect(magnifier, mPaint);
			
			RectF focus = new RectF(	lastTouchedPoint.x -  MAGNIFIER_FOCUS_WIDTH/2,
										lastTouchedPoint.y - MAGNIFIER_FOCUS_HEIGHT/2,0,0);
			focus.right = focus.left + MAGNIFIER_FOCUS_WIDTH;
			focus.bottom = focus.top + MAGNIFIER_FOCUS_HEIGHT;

			//Check Bounds
			if(focus.left<0){
				focus.left =0;
				focus.right = focus.left + MAGNIFIER_FOCUS_WIDTH;
			}else if(focus.right > getWidth()){
				focus.right = getWidth();
				focus.left = focus.right - MAGNIFIER_FOCUS_WIDTH;
			}
			if(focus.top<0){
				focus.top = 0;
				focus.bottom = focus.top + MAGNIFIER_FOCUS_HEIGHT;
			}else if(focus.bottom > getHeight()){
				focus.bottom = getHeight();
				focus.top = focus.bottom - MAGNIFIER_FOCUS_HEIGHT;
			}
				
			///////////////////////////
			// Uncomment these to debug focus area
			// Paint testPaint = new Paint();testPaint.setStyle(Style.STROKE); testPaint.setColor(Color.BLUE);
			// canvas.drawRect(focus, testPaint);
			///////////////////////////
			
			TerminalView view = terminalActivity.getCurrentTerminalView();
			
			//FIXME: User FC at here, but there seems no reason for this to be null....? 
			//		 Needs double check!!
			if(view != null)
				view.renderMagnifier(canvas, magnifier, focus);
		}else{
			canvas.drawBitmap(footprintBitmap, 0, 0, null);
			canvas.drawBitmap(textBitmap, 0, 0, null);
		}
	}
	
	
	private int distance(Point start,Point end){		
		return (int)Math.pow(Math.pow(start.x-end.x, 2) + Math.pow(start.y-end.y,2),0.5);
	}
	
	public boolean onLongClick(View  v){
		if(distance(startPoint,lastTouchedPoint) > minGestureDistance)   /* this is not a long press */
			return false;
				
		/* long press actions:
		 * FLAG_NO_MAGNIFIER:				do nothing
		 * FLLAG_LONG_PRESS_ACTIVATE:		enable/disable magnifier
		 * FLLAG_LONG_PRESS_MODE_SWITCH:	enable/disable magnifier
		 * FLLAG_LONG_PRESS_SHOW:			enable magnifier
		 */
		
		if((TerminalActivity.termActFlags & TerminalActivity.FLAG_NO_MAGNIFIER) != 0)
			return false;

		if((TerminalActivity.termActFlags & 
				(TerminalActivity.FLLAG_LONG_PRESS_MODE_SWITCH | TerminalActivity.FLLAG_LONG_PRESS_ACTIVATE)) != 0 && magnifierOn){
			magnifierOn = false;
			return true;
		}					
		if(currentGesture.length()==0){
			footprintBitmap.eraseColor(0);
			textBitmap.eraseColor(0);
			magnifierOn = true;	
			Log.v(TAG,"magnifier on");
		}
		return true;
	}
	
	public static final char GESTURE_LEFT = 'L';
	public static final char GESTURE_RIGHT = 'R';
	public static final char GESTURE_UP = 'U';
	public static final char GESTURE_DOWN = 'D';
	private int minGestureDistance = 50;

	private String currentGesture = "";

	private void recognize(char e) {
		Log.d("Gesture", "computeGesture:" + currentGesture + "#" + e);

		if (currentGesture.length() > 0) {
			if (currentGesture.charAt(currentGesture.length() - 1) != e) {
				currentGesture = currentGesture + ("," + e);
				Log.d("Gesture", "Gesture:" + currentGesture);
				drawText();
			}
		} else {
			currentGesture = currentGesture + e;
			drawText();
		}
	}

	private void clear() {

		if (currentGesture.length() > 0) {
			mOnGestureListener.onGestureEvent(currentGesture);
		}

		currentGesture = "";
		footprintBitmap.eraseColor(0);
		textBitmap.eraseColor(0);

		invalidate();

	}

	private void drawLine(Point p1, Point p2) {
		if (footprintBitmap == null)
			return;

		footprintCanvas.drawLine(p1.x, p1.y, p2.x, p2.y, footprintPaint);

		int x1 = Math.min(p1.x, p2.x) - 10;
		int y1 = Math.min(p1.y, p2.y) - 10;
		int x2 = Math.max(p1.x, p2.x) + 10;
		int y2 = Math.max(p1.y, p2.y) + 10;

		mRect.set(x1, y1, x2, y2);
		invalidate(mRect);
	}

	private void drawText() {
		if (textBitmap == null)
			return;

		String desc = mOnGestureListener.getGestureText(currentGesture);

		textBitmap.eraseColor(0);
		textPaint.setColor(textBgColor);
		textCanvas.drawRect(0, 0, desc.length() * 9, textBitmap.getHeight(),
				textPaint);
		textPaint.setColor(textColor);
		textCanvas.drawText(desc.toString(), 0, 15, textPaint);
		textBitmap.getWidth();

		mRect.set(0, 0, textBitmap.getWidth(), textBitmap.getWidth());
		invalidate(mRect);
	}

	private float dx = 0;
	private float dy = 0;


	@Override
	public boolean onTouchEvent(MotionEvent ev) {		
		Log.v(TAG, "onTouchEvent...action=" + ev.getAction());
		Point evPoint = new Point((int) ev.getX(),(int) ev.getY());
		
		if(magnifierOn){
			if (ev.getAction() == MotionEvent.ACTION_UP){

				if((TerminalActivity.termActFlags & TerminalActivity.FLLAG_LONG_PRESS_SHOW) != 0){
					/* deactivate magnifier */
					magnifierOn = false;
					Log.v(TAG,"magnifier off");
				}
			}
			invalidate(); //we will paint magnifier in onDraw
		}else {	
			//Perform gesture
			
			if (mOnGestureListener == null)
				Log.e(TAG, "there is no gesture listener");

			mGestureDetector.onTouchEvent(ev);

			if (ev.getAction() != MotionEvent.ACTION_DOWN) 
				drawLine(lastTouchedPoint, evPoint);		
			
			if (ev.getAction() == MotionEvent.ACTION_UP) 
				clear();
		}
		
		if( ev.getAction() == MotionEvent.ACTION_DOWN){
			startPoint = evPoint; 
			fingerOnScreen = true;
		}else if(ev.getAction() == MotionEvent.ACTION_UP || ev.getAction() == MotionEvent.ACTION_CANCEL)
			fingerOnScreen = false;
		
		lastTouchedPoint = evPoint;
		
		super.onTouchEvent(ev);
		return true;
	}

	GestureDetector mGestureDetector = new GestureDetector(
			new GestureDetector.SimpleOnGestureListener() {

				@Override
				public boolean onSingleTapConfirmed(MotionEvent e) {
					TerminalView view = terminalActivity
							.getCurrentTerminalView();
					if (view != null)
						return view.onTouchEvent(e);
					else
						return false;
				}

				@Override
				public boolean onScroll(MotionEvent e1, MotionEvent e2,
						float distanceX, float distanceY) {
					Log.d("Gesture", "onScroll:" + distanceX + "," + distanceY);

					if (Math.max(Math.abs(dx), Math.abs(dy)) >= minGestureDistance) {
						char g = GESTURE_LEFT;
						if (Math.abs(dx) > Math.abs(dy)) {
							if (dx < 0)
								g = GESTURE_RIGHT;
							else
								g = GESTURE_LEFT;
						} else {
							if (dy < 0)
								g = GESTURE_DOWN;
							else
								g = GESTURE_UP;
						}

						recognize(g);
						dx = dy = 0;
					} else {
						dx = dx + distanceX;
						dy = dy + distanceY;

					}
					return true;
				}
			});
}
