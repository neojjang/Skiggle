/*
 *     This file is part of Skiggle, an online handwriting recognition
 *     Java application.
 *     Copyright (C) 2009-2011 Willie Lim <wlim650@gmail.com>
 *
 *     Skiggle is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     Skiggle is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with Skiggle.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package com.android.skiggle;


import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PathMeasure;
import android.graphics.RectF;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;

public class Skiggle extends Activity {
	//	 implements ColorPickerDialog.OnColorChangedListener {

	public static int DEFAULT_PEN_COLOR = 0xFF00FFFF;   //;
	public static int DEFAULT_CANVAS_COLOR = 0xFFFFFFFF;  //0xFFAAAAAA);
	public static float DEFAULT_STROKE_WIDTH = 12.0F;	
	public static float DEFAULT_FONT_SIZE = 14.0F; //12.0F;

	public static int DEFAULT_WRITE_PAD_WIDTH = 320;
	public static int DEFAULT_WRITE_PAD_HEIGHT = 480;

	private Paint mPaint;
	private Paint mTextPaint;


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(new BoxView(this));

		mPaint = new Paint();
		mPaint.setAntiAlias(true);
		mPaint.setDither(true);
		mPaint.setColor(DEFAULT_PEN_COLOR);
		mPaint.setStyle(Paint.Style.STROKE);
		mPaint.setStrokeJoin(Paint.Join.ROUND);
		mPaint.setStrokeCap(Paint.Cap.ROUND);
		mPaint.setStrokeWidth(DEFAULT_STROKE_WIDTH);

		mTextPaint = new Paint();
		mTextPaint.setTextSize(DEFAULT_FONT_SIZE);



	}

	public class BoxView extends View {

		private Bitmap mBitmap;

		private Canvas mCanvas;
		private Path mPath;
		private Paint mBitmapPaint;
		private float mX, mY;
		private static final float TOUCH_TOLERANCE = 4;
		private PenStroke mPenStroke;
		private PenSegment mPenSegment;
		//		private int mStrokeNumber = 0;
		//		private int mSegmentNumber = 0;
		public PenCharacter mPenCharacter = new PenCharacter();

		public BoxView(Context c) {
			super(c);

			mBitmap = Bitmap.createBitmap(DEFAULT_WRITE_PAD_WIDTH, DEFAULT_WRITE_PAD_HEIGHT, Bitmap.Config.ARGB_8888);
			mCanvas = new Canvas(mBitmap);
			mBitmapPaint = new Paint(Paint.DITHER_FLAG);
			mPath = new Path();
		}

		@Override
		protected void onDraw(Canvas canvas) {
			canvas.drawColor(DEFAULT_CANVAS_COLOR);

			canvas.drawBitmap(mBitmap, 0, 0, mBitmapPaint);

			canvas.drawPath(mPath, mPaint);
		}

		private void touch_start(float x, float y) {
			mPath.reset();
			mPath.moveTo(x, y);
			mX = x;
			mY = y;
		}

		private void touch_move(float x, float y) {
			float dx = Math.abs(x - mX);
			float dy = Math.abs(y - mY);
			if (dx >= TOUCH_TOLERANCE || dy >= TOUCH_TOLERANCE) {
				mPath.quadTo(mX, mY, (x + mX) / 2, (y + mY) / 2);
				mX = x;
				mY = y;
			}
		}

		private void touch_up() {
			mPath.lineTo(mX, mY);

			// commit the path to our off screen
			mCanvas.drawPath(mPath, mPaint);

			// If the stroke is a point of zero length , make it a filled circle of
			// diameter Skiggle.DEFAULT_STROKE_WIDTH and add it to the path
			PathMeasure pMeasure = new PathMeasure(mPath, false);
			if (pMeasure.getLength() == 0) {
				RectF boundingRectF = new RectF(); 
				mPath.computeBounds(boundingRectF, false);

				// Create a line of 1 pixel length
				mPath.lineTo(boundingRectF.centerX(), boundingRectF.centerY() + 1);

			}

			// Set pen stroke to a copy of the stroke
			// mPenStroke = new PenStroke();
			mPenStroke = new PenStroke(mPath);
			mPenStroke.addPath(mPath);
			mPenCharacter.addStroke(mPenStroke);
			//			mStrokeNumber = mStrokeNumber + 1;
			// Set pen color to a different color for the copy of the stroke
			//  mPenStrokeMeasure = new PathMeasure( mPenStroke, false);
			// if ( mPenStrokeMeasure.isClosed())
			//	mPaint.setColor(0xFFFF0000);
			// Paint the copy of the stroke with the new pen color
			mCanvas.drawPath( mPenStroke, mPaint);
			
			// Check to see if the stroke is a jagged "clear screen" stroke
			if ((mPenStroke.mPenStrokeLength/(mPenStroke.mBoundingRectWidth + mPenStroke.mBoundingRectHeight)) > 2) {
				this.clear();
				// Clear the Pen Character object
				mPenCharacter = new PenCharacter();
			}
			else {

				// Add segment(s) for the PenStroke
				// mPenSegment = new PenSegment(mPenStroke);
				mPenCharacter.addSegments(mPenStroke, mCanvas, mTextPaint);
				//				mSegmentNumber = mSegmentNumber + 1;
				
				mPenCharacter.findMatchingCharacter(mCanvas, mTextPaint);

			}

			// kill this so we don't double draw
			mPath.reset();
		}

		@Override
		public boolean onTouchEvent(MotionEvent event) {

			float x = event.getX();
			float y = event.getY();
			switch (event.getAction()) {
			case MotionEvent.ACTION_DOWN:
				touch_start(x, y);
				invalidate();
				break;
			case MotionEvent.ACTION_MOVE:
				touch_move(x, y);
				invalidate();
				break;
			case MotionEvent.ACTION_UP:
				touch_up();
				invalidate();
				break;
			}
			return true;
		}

		public void clear() {
			mBitmap.eraseColor(DEFAULT_CANVAS_COLOR);
			mPath.reset();
			mPenCharacter.resetStrokes();
			mPenCharacter.resetSegments();
			invalidate();
		}

	}
}
