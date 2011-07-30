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

import com.android.skiggle.Skiggle.BoxView;

import java.util.Vector;

import android.graphics.Path;
import android.graphics.PathMeasure;
import android.graphics.RectF;
import android.graphics.Canvas;
import android.graphics.Paint;

public class PenStroke extends Path {
	
	// TAG for logging debugging info
	//private static final String TAG = "MyPenStroke";

	// Members
	public Path mPenStrokePath;
	public PathMeasure mPenStrokeMeasure;
	public float mPenStrokeLength;
	public RectF mBoundingRectF;
	public float mBoundingRectHeight;
	public float mBoundingRectWidth;
	public float mPosStart[] = {0.0F, 0.0F};
	private float mTanStart[] = {0.0F, 0.0F};
	public float mPosEnd[] = {0.0F, 0.0F};
	private float mTanEnd[] = {0.0F, 0.0F};
	//	private float mAvgAngle = 0.0F;
	//	private float mAvgKappa = 0.0F; // a measure of average curvature
	public float mAvgX = 0.0F; // average X-coord of points on the stroke
	public float mAvgY = 0.0F; // average Y-coord of points on the stroke
	//	private float mMaxAbsKappaX = 0.0F; // x-coord of the max absolute Kappa value
	//	private float mMaxAbsKappaY = 0.0F; // y-coord of the max absolute Kappa value
	//	private float mMaxAbsKappa = 0.0F; //temp
	//	private float mMaxAbsKappaDiffX = 0.0F; // x-coord of the max absolute Kappa diff value
	//	private float mMaxAbsKappaDiffY = 0.0F; // y-coord of the max absolute Kappa diff value
	//	private float mMaxAbsKappaDiff = 0.0F; // temp
	//	private String mHistBucketsStr = ""; // temp
	//	public Character mPenStrokeCharacter;

	public PenStroke(Path path) {
		mPenStrokePath = new Path(path);
	}
	
	public void addPath(Path srcPath) {
		super.addPath(srcPath);
		
		mPenStrokePath = new Path(srcPath);
		mPenStrokeMeasure = new PathMeasure(this, false);
		mPenStrokeLength = mPenStrokeMeasure.getLength();
		mPenStrokeMeasure.getPosTan(0, mPosStart, mTanStart);
		mPenStrokeMeasure.getPosTan(mPenStrokeLength, mPosEnd, mTanEnd);
		mBoundingRectF = new RectF();	
		this.computeBounds(mBoundingRectF, false);
		mBoundingRectHeight = Math.abs(mBoundingRectF.top - mBoundingRectF.bottom);
		mBoundingRectWidth = Math.abs(mBoundingRectF.left - mBoundingRectF.right);

	}

	public Vector<PenSegment> segmentStroke(Canvas canvas, Paint textPaint) {
		// Vector<PenSegment> pSegments = new Vector<PenSegment>();
		PenSegment pSegment1 = new PenSegment(this.mPenStrokePath);
		return pSegment1.getStrokeSegments(canvas, textPaint);
	}
	

	
	/*	
	public void printPenStrokeStatsOnScreen(BoxView boxView, Canvas canvas, Paint strokePaint, Paint textPaint) {
		double angle = 0.0;

		PenUtil.printString(mPenStrokeCharacter.toString(), mPosEnd[0], mPosEnd[1], mBoundingRectF, canvas, textPaint);
		PenUtil.printString(boxView.mPenCharacter.mPenCharacter.toString(), mPosEnd[0], mPosEnd[1], mBoundingRectF, canvas, textPaint);


	}
	 */

}
