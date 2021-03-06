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

import android.graphics.Canvas;
import android.graphics.PointF;
import android.graphics.RectF;
import android.graphics.Paint;

public class PenUtil
{

	public PenUtil() {}

	public static double getAbsAngle(double y, double x) {

		double angle = 0.0F;
		// atan2 is positive in the lower right quadrant and negative in upper right quadrant and measured
		// from the horizontal (positive x-axis)
		angle = Math.IEEEremainder(360 + Math.toDegrees(Math.atan2(y, x)), 360);

		return angle;
	}

	// Get the gap between two stroke points (x1, y1) and (x2, y2)
	public static float distanceBetween2Points(float x1, float y1, float x2, float y2) {

		return PointF.length(Math.abs(x1 - x2), Math.abs(y1 - y2));
	}

	public static int[] histogram(float[] dataPoints) {

		int[] buckets = {0, 0, 0, 0, 0}; // 5 buckets
		float minVal = 1000000.0F;
		float maxVal = -minVal;
		int numOfDataPoints = dataPoints.length;

		// Get the min and max values of the data points
		for (int i = 0; i < numOfDataPoints; i++) {

			minVal = Math.min(minVal, Math.abs(dataPoints[i]));
			maxVal = Math.max(maxVal, Math.abs(dataPoints[i]));
		}

		float bucketSize = (maxVal - minVal)/5;
		// float bucketSize = 0.002F;

		// Count the number of points for each bucket
		for (int i = 0; i < numOfDataPoints; i++) {

			float val = Math.abs(dataPoints[i]);
			if (val <= minVal + bucketSize)
				buckets[0] = buckets[0] + 1; // between minVal and minVal + bucketSize
			else if (val <= minVal + 2* bucketSize)
				buckets[1] = buckets[1] + 1; // between minVal and minVal + 2* bucketSize
			else if (val <= minVal + 3* bucketSize)
				buckets[2] = buckets[2] + 1; // between minVal and minVal + 3* bucketSize
			else if (val <= minVal + 4* bucketSize)
				buckets[3] = buckets[3] + 1; // between minVal and minVal + 4* bucketSize
			else
				buckets[4] = buckets[4] + 1; // greater than minVal + 4* bucketSize
		}	
		return buckets;
	}

	public static void getSegmentEndPoints(RectF mBoundingRectF, float x[], float y[], float tanAngle[], float kappa[], Canvas canvas, Paint textPaint) {

		int numOfSegments = x.length;
		for (int i =0; i < numOfSegments; i++) {

			printString(String.format("%1$d, k:%2$2.4f", i, kappa[i]), x[i], y[i], mBoundingRectF, canvas, textPaint);
		}

	}

	// Algorithm HK2003 13 (9. S. Hermann and R. Klette. Multigrid analysis of curvature estimators. In Proc.
	// Image Vision Computing New Zealand, pages 108�112, Massey University, 2003.) from
	// "A Comparative Study on 2D Curvature Estimators", Simon Hermann and Reinhard Klette
	public static float computeCurvatureHK2003(float x0, float y0, float x1, float y1, float x2, float y2) {

		double kappa = 0.0D;

		float lB = distanceBetween2Points(x1, y1, x0, y0);
		float lF = distanceBetween2Points(x1, y1, x2, y2);
		float thetaB = (float) Math.atan2(x0 - x1, y0 - y1);
		float thetaF = (float) Math.atan2(x2 - x1, y2 - y1);

		float delta = Math.abs(thetaB - thetaF)/2;

		kappa = (1/lB + 1/lF) * delta/2;
		return (float) kappa;
	}

	// Algorithm M2003 (13.  M. Marji. On the detection of dominant points on digital planar curves. PhD thesis,
	// Wayne State University, Detroit, Michigan, 2003) from "A Comparative Study on 2D Curvature Estimators",
	// Simon Hermann and Reinhard Klette
	public static float computeCurvatureM2003(float x0, float y0, float x1, float y1, float x2, float y2) {

		double kappa = 0.0D;

		float a1 = (x2 - x0)/2;
		float a2 = (x2 + x0)/2 - x1;
		float b1 = (y2 - y0)/2;
		float b2 = (y2 + y0)/2 - y1;

		// float alpha = 0.0F;
		// alpha = (a1*b2 - a2*b1);
		// float beta  = 0.0F;
		// beta = a1*a1 + b1*b1;
		// float delta = 0.0F;
		// delta = (float) Math.pow((a1*a1 + b1*b1), 1.5);

		kappa = 2*(a1*b2 - a2*b1)/ Math.pow((a1*a1 + b1*b1), 1.5);
		return (float) kappa;
	}

	public static void printString(String s, float x, float y, RectF boundingRectF, Canvas canvas, Paint paint) {


		if (s != null) {

			Paint tempPaint = new Paint();
			tempPaint.set(paint);

			tempPaint.setColor(Skiggle.DEFAULT_CANVAS_COLOR + 2);
			tempPaint.setStrokeWidth(Skiggle.DEFAULT_STROKE_WIDTH);
			// canvas.drawRect(boundingRectF, tempPaint);

			tempPaint.setColor(0xFFFF0000);
			//tempPaint.setTextSize(Math.max(mBoundingRectHeight, mBoundingRectWidth));

			canvas.drawText(s, x, y, tempPaint);
		}
	}


}
