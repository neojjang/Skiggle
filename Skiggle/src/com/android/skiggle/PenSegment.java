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

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PathMeasure;
import android.graphics.PointF;
import android.graphics.RectF;
import android.util.Log;

// PenStroke is a continuous path (between pen down and pen up) drawn by the pen.
public class PenSegment extends Path {

	// TAG for logging debugging info
	private static final String TAG = "MyPenSegment";

	// Class constants - primitive stroke elements making up a character
	public static final char VLINE_CHAR = '|'; // Vertical line segment
	public static final char HLINE_CHAR = '-'; // Horizontal line segment
	public static final char FSLASH_CHAR = '/'; //Forward slash segment
	public static final char BSLASH_CHAR = '\\'; // Back slash segment
	public static final char FC_CHAR = '('; // Forward "C" or left (or open) parenthesis used to represent the curved stroke that looks like a regular, forward 'C'
	public static final char CIRCLE_CHAR = 'O'; // Circle (closed loop) segment
	public static final char BC_CHAR = ')'; // Backward "C" or right (or closed) parenthesis used to represent the curved stroke that looks like a backward 'C'
	public static final char DOT_CHAR = '.'; // Dot or period segment
	public static final char U_CHAR = 'U'; // U segment

	private static final float MAX_CURVATURE_FOR_STRAIGHTLINE = 0.005F; // Maximum curvature (kappa) for a stroke to be a straight line

	private static final float VLINE_ANGLE = 90.0F; // Line goes from N to S
	private static final float HLINE_ANGLE = 0.0F; // Line goes from W to E
	private static final float BSLASH_ANGLE= 45.0F; // Line goes from NW to SE
	private static final float FSLASH_ANGLE = 135.0F; // Line goes from NE to SW

	private static final float VLINE_MAX_ANGLE_SPREAD = 15.0F; //10.0F;  // Max tilt angle spread from the vertical for vertical line
	private static final float HLINE_MAX_ANGLE_SPREAD = 15.0F; //10.0F;	 // Max tilt angle from spread the horizontal for a horizontal line segment
	private static final float BSLASH_MAX_ANGLE_SPREAD = 30.0F; // Maximum tilt angle from the horizontal for a back slash
	private static final float FSLASH_MAX_ANGLE_SPREAD = 30.0F; // Maximum tilt angle from the horizontal for a forward slash

	private static final float MAX_ABS_KAPPA_DIFF_THRESHOLD = 0.025F; // Max difference between the curvature (kappa) values of a stroke segment

	private static final int NUM_OF_POINTS_ON_STROKE = 20;

	// Members
	public Path mPenSegmentPath;
	public PathMeasure mPenStrokeMeasure;
	public float mPenStrokeLength;
	public RectF mBoundingRectF;
	public float mBoundingRectHeight;
	public float mBoundingRectWidth;
	public float mPosStart[] = {0.0F, 0.0F};
	private float mTanStart[] = {0.0F, 0.0F};
	public float mPosEnd[] = {0.0F, 0.0F};
	private float mTanEnd[] = {0.0F, 0.0F};
	private float mAvgAngle = 0.0F;
	public float mAvgKappa = 0.0F; // a measure of average curvature
	public float mAvgX = 0.0F; // average X-coord of points on the stroke
	public float mAvgY = 0.0F; // average Y-coord of points on the stroke
	private float mMaxAbsKappaX = 0.0F; // x-coord of the max absolute Kappa value
	private float mMaxAbsKappaY = 0.0F; // y-coord of the max absolute Kappa value
	private float mMaxAbsKappa = 0.0F; //temp
	private float mMaxAbsKappaDiffX = 0.0F; // x-coord of the max absolute Kappa diff value
	private float mMaxAbsKappaDiffY = 0.0F; // y-coord of the max absolute Kappa diff value
	public int mMaxAbsKappaDiffIndex = -1; // array index or position of the max absolute Kappa duff point
	public float mMaxAbsKappaDiff = 0.0F; // temp
	private String mHistBucketsStr = ""; // temp
	public Character mPenSegmentCharacter;

	// Twenty element arrays
	public float mPointsX[] = {0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F,
			0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F};
	public float mPointsY[] = {0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F,
			0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F};
	private float mTanAngle[] = {0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F,
			0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F}; // tangent angle in degrees
	private float mKappa[] = {0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F,
			0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F}; // array of curvature
	private float mKappaDiff[] = {0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F,
			0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F}; //


	public PenSegment() {

		super();
	}

	public PenSegment(Path path) {
		mPenSegmentPath = new Path(path);
		mPenStrokeMeasure = new PathMeasure(mPenSegmentPath, false);
		mPenStrokeLength = mPenStrokeMeasure.getLength();
		mPenStrokeMeasure.getPosTan(0, mPosStart, mTanStart);
		mPenStrokeMeasure.getPosTan(mPenStrokeLength, mPosEnd, mTanEnd);
		mBoundingRectF = new RectF();
		path.computeBounds(mBoundingRectF, false);
		mBoundingRectHeight = Math.abs(mBoundingRectF.top - mBoundingRectF.bottom);
		mBoundingRectWidth = Math.abs(mBoundingRectF.left - mBoundingRectF.right);
	}

	public void addPath(Path srcPath) {

		super.addPath(srcPath);		
		mPenStrokeMeasure = new PathMeasure(srcPath, false);
		mPenStrokeLength = mPenStrokeMeasure.getLength();
		mPenStrokeMeasure.getPosTan(0, mPosStart, mTanStart);
		mPenStrokeMeasure.getPosTan(mPenStrokeLength, mPosEnd, mTanEnd);
		mBoundingRectF = new RectF();	
		srcPath.computeBounds(mBoundingRectF, false);
		mBoundingRectHeight = Math.abs(mBoundingRectF.top - mBoundingRectF.bottom);
		mBoundingRectWidth = Math.abs(mBoundingRectF.left - mBoundingRectF.right);
	}

	/*
	private double getAbsAngle(double y, double x) {

		double angle = 0.0F;
		// atan2 is positive in the lower right quadrant and negative in upper right quadrant and measured
		// from the horizontal (positive x-axis)
		angle = Math.IEEEremainder(360 + Math.toDegrees(Math.atan2(y, x)), 360);

		return angle;
	}
	 */

	// If the maximum kappa difference is more than 10 times the kappa average then the stroke has more than one segment
	public boolean hasMultipleSegments() {

		return (mMaxAbsKappaDiff > MAX_ABS_KAPPA_DIFF_THRESHOLD) & (mMaxAbsKappaDiff > 5.0 * Math.abs(mAvgKappa));
	}

	// Check to see if the segment length is at least .1 the total length
	private boolean minSegmentLengthCheck(float segLength, float pathLength)
	{
		return (Math.min(pathLength - segLength, segLength) > .1 * pathLength);
	}

	private boolean checkLineAngle (double lineAngle, double angleThreshold) {

		return 		
		(Math.abs(lineAngle) < angleThreshold) |   // line is in one direction, e.g., left to right, W to E, or NE to SW.
		(Math.abs(180 - Math.abs(lineAngle)) < angleThreshold);  // line is in the other direction, e.g. right to left, E to W, or SW to NE.
	}

	private boolean isStraight(double kappa) {

		return (Math.abs(kappa) < MAX_CURVATURE_FOR_STRAIGHTLINE);
	}

	private boolean isCurved(double kappa) {

//		return !isStraight(kappa) & (mMaxAbsKappaDiff < MAX_ABS_KAPPA_DIFF_THRESHOLD);
		return !isStraight(kappa);
	}

	// Check to see if the curve's center of gravity (average x and y) is to the left of the line joining its end.
	// That is, it opens to the right, like a regular 'C' or open parenthesis '('.
	private boolean isCOGLeftOfEndLine() {

		float endLineMidX = (mPosStart[0] + mPosEnd[0])/2;
		float endLineMidY = (mPosStart[1] + mPosEnd[1])/2;
		float gapX = Math.abs(endLineMidX - mAvgX);

		return ((mAvgX < endLineMidX) & (Math.abs(mAvgY - endLineMidY) < .25 * gapX));
	}

	// Check to see if the curve's center of gravity (average x and y) is to the right of the line joining its end.
	// That is, it opens to the left like a backward C or closed parenthesis ')'
	private boolean isCOGRightOfEndLine() {

		float endLineMidX = (mPosStart[0] + mPosEnd[0])/2;
		float endLineMidY = (mPosStart[1] + mPosEnd[1])/2;
		float gapX = Math.abs(endLineMidX - mAvgX);

		return ((endLineMidX < mAvgX) & (Math.abs(mAvgY - endLineMidY) < .5 * gapX));
//		return ((endLineMidX < mAvgX));
	}

	private boolean isCOGBelowEndLine() {

		float endLineMidX = (mPosStart[0] + mPosEnd[0])/2;
		float endLineMidY = (mPosStart[1] + mPosEnd[1])/2;
		float gapY = Math.abs(endLineMidY - mAvgY);

		return ((mAvgY > endLineMidY) & (Math.abs(mAvgX - endLineMidX) < .25 * gapY));
	}

	/*
	// Get the gap between two stroke points (x1, y1) and (x2, y2)
	public static float distanceBetween2Points(float x1, float y1, float x2, float y2) {

		return PointF.length(Math.abs(x1 - x2), Math.abs(y1 - y2));
	}
	 */

	// Check to see if the stroke is a closed one, that is, the gap between the two ends of the stroke
	// is less than one tenth the length of the stroke
	private boolean isClosedStroke() {

		return (PenUtil.distanceBetween2Points(mPosStart[0], mPosStart[1], mPosEnd[0], mPosEnd[1]) <
				(.1 * mPenStrokeLength));
	}
	private boolean isHLine() {

		// line can be left to right (W to E) or right to left (E to W)
		return (isStraight(mAvgKappa) &
				checkLineAngle(mAvgAngle - HLINE_ANGLE, HLINE_MAX_ANGLE_SPREAD));
	}

	private boolean isBSlash() {

		// line can be NW to SE or SE to NW
		return (isStraight(mAvgKappa) &
				checkLineAngle(mAvgAngle - BSLASH_ANGLE, BSLASH_MAX_ANGLE_SPREAD));
	}

	private boolean isVLine() {

		// line can be top to bottom (N to S) or bottom to top (S to N)
		return (isStraight(mAvgKappa) &
				checkLineAngle(mAvgAngle - VLINE_ANGLE, VLINE_MAX_ANGLE_SPREAD));
	}

	private boolean isFSlash() {

		// line can be NE to SW or SW to NE
		return (isStraight(mAvgKappa) &
				checkLineAngle(mAvgAngle - FSLASH_ANGLE, FSLASH_MAX_ANGLE_SPREAD));
	}

	// Check to see if stroke is a backward C (looks like a more curved version of the left parenthesis ')' )
	private boolean isBC() {

		return (isCurved(mAvgKappa) & isCOGRightOfEndLine());
	}

	// Check to see if stroke is a regular forward C
	private boolean isFC() {

//		return (isCurved(mAvgKappa) & isCOGLeftOfEndLine());
		return (isCurved(mAvgKappa) & isCOGLeftOfEndLine());
	}

	private boolean isCircle() {

		return (isClosedStroke() & isCurved(mAvgKappa));
	}

	private boolean isU() {

		return (isCurved(mAvgKappa) & isCOGBelowEndLine());
	}

	private boolean isDot() {

		return (mBoundingRectWidth < 2) & (mBoundingRectHeight < 2);
	}

	/*
	public int[] histogram(float[] dataPoints) {

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
	 */


	/*
// 	private float tanAngle1stDiff(double tanStart, double tanEnd, float segmentLength) {


		// curvature is the rate of change of the tangent vector
//		return (float) (tanStart - tanEnd)/segmentLength;
//	}


	// Algorithm HK2003 13 (9. S. Hermann and R. Klette. Multigrid analysis of curvature estimators. In Proc.
	// Image Vision Computing New Zealand, pages 108–112, Massey University, 2003.) from
	// "A Comparative Study on 2D Curvature Estimators", Simon Hermann and Reinhard Klette
	private float computeCurvatureHK2003(float x0, float y0, float x1, float y1, float x2, float y2) {

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
	private float computeCurvatureM2003(float x0, float y0, float x1, float y1, float x2, float y2) {

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
	 */

	public void getExtremaPoint() {


	}

	// Compute the curvature at various points of the stroke
	public void getCurvaturePoints(Canvas canvas, Paint textPaint) {

		int numOfSegments = NUM_OF_POINTS_ON_STROKE;
		float posStart[] = {0.0F, 0.0F};
		float tanStart[] = {0.0F, 0.0F};
		float posEnd[] = {0.0F, 0.0F};
		float posX[] = {0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F,
				0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F};
		float posY[] = {0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F,
				0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F};
		float tanAngle[] = {0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F,
				0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F}; // tangent angle in degrees
		float tanEnd[] = {0.0F, 0.0F};
		float kappa[] = {0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F,
				0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F}; // array of curvature
		float kappaDiff[] = {0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F,
				0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F}; // array of difference of curvature
		float sumAngle = 0.0F;
		float sumKappa = 0.0F;
		float sumX = 0.0F;
		float sumY = 0.0F;
		float maxAbsKappa = 0.0F;
		float maxAbsKappaDiff = 0.0F;
		float segmentLength = mPenStrokeLength/numOfSegments;
		for (int i = 0; i < numOfSegments; i++) {

			mPenStrokeMeasure.getPosTan(i * segmentLength, posStart, tanStart);
			posX[i] = posStart[0];
			posY[i] = posStart[1];
			sumX = sumX + posX[i];
			sumY = sumY + posY[i];
			tanAngle[i] = (float) PenUtil.getAbsAngle(tanStart[1], tanStart[0]);

			if (i > 0) {

				// mPenStrokeMeasure.getPosTan((i + 1) * segmentLength, posEnd, tanEnd);
				// posX[i + 1] = posEnd[0];
				// posY[i + 1] = posEnd[1];
				// tanAngle[i + 1] = (float) getAbsAngle(tanEnd[1], tanEnd[0]);
				sumAngle = sumAngle + tanAngle[i]; //tanAngle[i+1]; //Math.abs(tanAngle[i+1]);

				if ((i > 1) & (i < numOfSegments - 1)) {
					// need 3 points to compute kappa so ignore start and end points
					kappa[i-1] = PenUtil.computeCurvatureM2003(posX[i-2], posY[i-2], posX[i-1], posY[i-1], posX[i], posY[i]);
					// kappa[i-1] = computeCurvatureHK2003(posX[i-2], posY[i-2], posX[i-1], posY[i-1], posX[i], posY[i]);
					sumKappa = sumKappa + kappa[i-1];
					if (Math.abs(kappa[i-1])> maxAbsKappa) {

						// Update the x,y coordinates and max absolute kappa value
						mMaxAbsKappaX = posX[i-1];
						mMaxAbsKappaY = posY[i-1];
						maxAbsKappa = Math.abs(kappa[i-1]);
					}

					kappaDiff[i-2] = kappa[i-1] - kappa[i-2]; // get difference in kappa of point and its next neighbor
					if (Math.abs(kappaDiff[i-2])> maxAbsKappaDiff) {

						// Update the x,y coordinates, index, and max absolute kappa difference accordingly
						mMaxAbsKappaDiffX = posX[i-2];
						mMaxAbsKappaDiffY = posY[i-2];
						mMaxAbsKappaDiffIndex = i-2;
						maxAbsKappaDiff = Math.abs(kappaDiff[i-2]);
					}
				}
			}

			mAvgX = sumX/numOfSegments;
			mAvgY = sumY/numOfSegments;
			mAvgAngle = sumAngle/(numOfSegments - 1);
			mAvgKappa = sumKappa/(numOfSegments - 2);
			mMaxAbsKappa = maxAbsKappa;
			mMaxAbsKappaDiff = maxAbsKappaDiff;

			mPointsX = posX;
			mPointsY = posY;
			mTanAngle = tanAngle;
			mKappa = kappa;
			mKappaDiff = kappaDiff; 
		}
		//printSegmentEndPoints(mBoundingRectF, posX, posY, tanAngle, kappa, canvas, textPaint);

		int histBuckets[] = PenUtil.histogram(kappaDiff);
		for (int i = 0; i < histBuckets.length; i++) {
			mHistBucketsStr = mHistBucketsStr + ", "  + histBuckets[i];
		}		

	}

	public Vector<PenSegment> getStrokeSegments(Canvas canvas, Paint textPaint) {

		Vector<PenSegment> pSegments = new Vector<PenSegment>();

		getCurvaturePoints(canvas, textPaint);


		if (hasMultipleSegments()) {

			int offSet = 5;

			float pathLength1 = mPenStrokeMeasure.getLength();
			float headLength1 = Math.max(0, mMaxAbsKappaDiffIndex + 1 - offSet) * (pathLength1/NUM_OF_POINTS_ON_STROKE);
			float tailLength1 = Math.min(pathLength1, mMaxAbsKappaDiffIndex + offSet) * (pathLength1/NUM_OF_POINTS_ON_STROKE);
			Path path2 = new Path();

			if (mPenStrokeMeasure.getSegment(headLength1, tailLength1, path2, true)) {
				PenSegment pSegment2 = new PenSegment(path2);
				pSegment2.getCurvaturePoints(canvas, textPaint);

				float pathLength2 = pSegment2.mPenStrokeMeasure.getLength();
				// Set length of the head (first segment) to a default value - start of path to the first max abs kappa diff point
				float headLength2 = Math.max(0, mMaxAbsKappaDiffIndex) * (pathLength1/NUM_OF_POINTS_ON_STROKE);

				// See if the max abs kappa diff of the new shorter segment is more than that of the longer segment
				if (pSegment2.mMaxAbsKappaDiff > mMaxAbsKappaDiff)
					headLength2 = Math.max(0, pSegment2.mMaxAbsKappaDiffIndex) * (pathLength2/NUM_OF_POINTS_ON_STROKE);
				// float tailLength2 = Math.min(pathLength1, pSegment2.mMaxAbsKappaDiffIndex + offSet) * (pathLength1/NUM_OF_POINTS_ON_STROKE);

				if (minSegmentLengthCheck(headLength2, pathLength1)) {
					Path path3 = new Path();
					Path path4 = new Path();
					if (mPenStrokeMeasure.getSegment(0, headLength1 + headLength2, path3, true)
							& mPenStrokeMeasure.getSegment(headLength1 + headLength2 + 1, tailLength1, path4, true)) {
						PenSegment pSegment3 = new PenSegment(path3);
						PenSegment pSegment4 = new PenSegment(path4);

						PenUtil.printString(String.format(".(%1$3.1f,%2$3.1f), k:%2$3.1f", pSegment3.mPosEnd[0], pSegment3.mPosEnd[1], pSegment3.mKappa),
								pSegment3.mPosEnd[0], pSegment3.mPosEnd[1], mBoundingRectF, canvas, textPaint);

						//				PenUtil.printString(String.format("!!%1$3.1f, %2$3.1f, %3$3.1f, %4$3.1f, %5$3.1f", mMaxAbsKappaDiff, pSegment2.mMaxAbsKappa, tailLength1 - headLength1, pSegment2.mPenStrokeLength, mPenStrokeLength), 100, 420, mBoundingRectF, canvas, textPaint);
						pSegment2.printSegmentStats(canvas, textPaint);
						pSegments.addAll(pSegment3.getStrokeSegments(canvas, textPaint));
						pSegments.addAll(pSegment4.getStrokeSegments(canvas, textPaint));						
						return pSegments;
					}

				}

			}

			getExtremaPoint();


		}
		findMatchingCharacter();
		pSegments.add(this);



		return pSegments;
	}

	public void findMatchingCharacter() {

		// Check for the DOT stroke first as it has the length of one pixel
		if (isDot())
			mPenSegmentCharacter = DOT_CHAR;
		else if (isHLine())
			mPenSegmentCharacter = HLINE_CHAR;
		else if (isBSlash())
			mPenSegmentCharacter = BSLASH_CHAR;
		else if (isVLine())
			mPenSegmentCharacter = VLINE_CHAR;
		else if (isFSlash())
			mPenSegmentCharacter = FSLASH_CHAR;
		else if (isBC())
			mPenSegmentCharacter = BC_CHAR;
		else if (isFC())
			mPenSegmentCharacter = FC_CHAR;
		else if (isCircle())
			mPenSegmentCharacter = CIRCLE_CHAR;
		else if (isU())
			mPenSegmentCharacter = U_CHAR;
		else 
			mPenSegmentCharacter = new Character('?');
	}

	public void printSegmentStats(Canvas canvas, Paint textPaint) {

		/*
		double angle = 0.0;

		// Paint the copy of the stroke with the new pen color
		canvas.drawText(String.format("Len:%1$3.1f, len/(wd + ht):%2$3.1f",
				mPenStrokeLength, mPenStrokeLength/(mBoundingRectWidth + mBoundingRectHeight)), 
				mPosStart[0] + 5.0F, mPosStart[1], textPaint);
		canvas.drawText(String.format("Pos:(%1$3.1f,%2$3.1f); angle:%3$3.1f;", 
				mPosStart[0], mPosStart[1],
				getAbsAngle(mTanStart[1], mTanStart[0])),
				mPosStart[0] + 5.0F, mPosStart[1] + 15.0F, textPaint);
		canvas.drawText(String.format("ht=%1$3.1f, wd=%2$3.1f, ht/wd=%3$3.1f", 
				mBoundingRectHeight,
				mBoundingRectWidth,
				mBoundingRectHeight/mBoundingRectWidth),
				mPosStart[0] + 5.0F, mPosStart[1] + 30.0F, textPaint);

		PenUtil.printString(mPenSegmentCharacter.toString(), mPosEnd[0], mPosEnd[1], mBoundingRectF, canvas, textPaint);

		 */
		String msg = String.format("k:%1$3.3f, maxKD:%2$3.3f, x:%3$3.3f, y:%4$3.3f",
				mAvgKappa, mMaxAbsKappaDiff, mMaxAbsKappaDiffX, mMaxAbsKappaDiffY);
		PenUtil.printString(mHistBucketsStr, 10, 420, mBoundingRectF, canvas, textPaint);
		canvas.drawText(msg, mMaxAbsKappaDiffX , mMaxAbsKappaDiffY, textPaint);
		//Log.i(PenSegment.TAG, msg);
		//printSegmentPointsData();
	}

	public void printSegmentEndPoints(RectF mBoundingRectF, float x[], float y[], float tanAngle[], float kappa[], Canvas canvas, Paint textPaint) {

		int numOfSegments = x.length;
		String msg = "";
		for (int i =0; i < numOfSegments; i++) {
			msg = String.format("%1$d, k:%2$2.4f", i, kappa[i]);
			PenUtil.printString(msg, x[i], y[i], mBoundingRectF, canvas, textPaint);
			//Log.i(PenSegment.TAG, msg);
		}

	}

	public void printSegmentPointsData() {
		String msg = "";
		/*
		Log.i(PenSegment.TAG, String.format(
				"mPosStart = (%1$2.4f, %2$2.4f), mPosEnd = (%3$2.4f, %4$2.4f), " +
				"mBoundingRectF.left = %5$2.4f, mBoundingRectF.top = %6$2.4f, " +
				"mBoundingRectF.right = %7$2.4f, mBoundingRectF.bottom = %8$2.4f, " +
				"mBoundingRectF.center = (%9$2.4f, %10$2.4f)",
				mPosStart[0], mPosStart[1], mPosEnd[0], mPosEnd[1],
				mBoundingRectF.left, mBoundingRectF.top, mBoundingRectF.right, mBoundingRectF.bottom,
				mBoundingRectF.centerX(), mBoundingRectF.centerY()));
				*/
		Log.i(PenSegment.TAG, "i, mPointsX[i], mPointsY[i], mTanAngle[i], mKappa[i], mKappaDiff[i]");
		for (int i =0; i < NUM_OF_POINTS_ON_STROKE; i++) {
			msg = String.format("%1$d, %2$2.4f, %3$2.4f, %4$2.4f, %5$2.4f, %6$2.4f", i,
					mPointsX[i],
					mPointsY[i],
					mTanAngle[i],
					mKappa[i],
					mKappaDiff[i]);
			Log.i(PenSegment.TAG, msg);
		}
	}

}

