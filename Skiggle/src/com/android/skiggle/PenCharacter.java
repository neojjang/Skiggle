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


import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Iterator;
import java.util.Vector;

import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.LogRecord;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.RectF;
import android.util.Log;

// PenCharacter is a class representing the character written.  It has:
//    - one or more PenSegment representing the strokes that make up the character
//    - one or more PenSegment representing the basic segments (building blocks like '-', 'C', '|') that make up a character
public class PenCharacter {
	
	// TAG for logging debugging info
	private static final String TAG = "MyPenCharacter";
	
	private static int DEFAULT_PENCHARACTER_STROKE_WIDTH = 4;

	private Vector<PenStroke> mPenStrokes;
	public float mPenStrokesMinX = Skiggle.DEFAULT_WRITE_PAD_WIDTH; //x-coord of left edge of rectangle bounding all strokes
	public float mPenStrokesMaxX = 0.0F; //x-coord of right edge of rectangle bounding all strokes
	public float mPenStrokesMinY = Skiggle.DEFAULT_WRITE_PAD_HEIGHT; //y-coord of top edge of rectangle bounding all strokes
	public float mPenStrokesMaxY = 0.0F; //y-coord of bottom edge of rectangle bounding all strokes

	public Vector<PenSegment> mPenSegments;

	public Character mPenCharacter = null;
	public String mPenCharacterCandidates = "";
	private float mFontSize = Skiggle.DEFAULT_FONT_SIZE;

	public PenCharacter () {
		mPenStrokes = new Vector<PenStroke>();
		mPenSegments = new Vector<PenSegment>();
		//		mPenStrokes = new Vector<PenSegment>();
	}

	public void addStroke (PenStroke penStroke) {

		// Update the x, y coordinates of the rectangle bounding all the strokes for the character
		mPenStrokesMinX = Math.min(mPenStrokesMinX, penStroke.mBoundingRectF.left);
		mPenStrokesMaxX = Math.max(mPenStrokesMaxX, penStroke.mBoundingRectF.right);
		mPenStrokesMinY = Math.min(mPenStrokesMinY, penStroke.mBoundingRectF.top);
		mPenStrokesMaxY = Math.max(mPenStrokesMaxY, penStroke.mBoundingRectF.bottom);

		mPenStrokes.add(penStroke);
	}

	// Break up stroke into one or more segments
	public void addSegments (PenStroke penStroke, Canvas canvas, Paint textPaint) {

		mPenSegments.addAll(penStroke.segmentStroke(canvas, textPaint));
		
		printSegmentCharacters(mPenSegments.elementAt(0).mBoundingRectF, canvas, textPaint);
	}


	private void showStrokes (Canvas canvas) {
		PenStroke tempStroke;
		int paintColor = 0x99990000 * mPenStrokes.size();
		Paint mPaint = new Paint();
		mPaint.setAntiAlias(true);
		mPaint.setDither(true);
		mPaint.setStyle(Paint.Style.STROKE);
		mPaint.setStrokeJoin(Paint.Join.ROUND);
		mPaint.setStrokeCap(Paint.Cap.ROUND);
		mPaint.setStrokeWidth(DEFAULT_PENCHARACTER_STROKE_WIDTH);
		mPaint.setColor(paintColor);


		for (Iterator<PenStroke> i = mPenStrokes.iterator(); i.hasNext();) {

			tempStroke = i.next();
			mFontSize = Math.max(mFontSize, Math.max(tempStroke.mBoundingRectHeight, tempStroke.mBoundingRectWidth));
			// Paint over the stroke with the original pen color so that the new color will be visible
			mPaint.setColor(0xFF00FFFF);
			canvas.drawPath(tempStroke, mPaint);
			// Paint the copy of the stroke with the new pen color
			mPaint.setColor(paintColor);
			canvas.drawPath(tempStroke, mPaint);
		}

		switch (mPenStrokes.size()) {
		case 2: checkForCapitalL();
		break;
		case 3: checkForCapitalI();
		break;
		default:
		}

		Paint tempPaint = new Paint();
		tempPaint.setColor(paintColor);
		canvas.drawText(Integer.toString(mPenStrokes.size()), 
				10.0F + (float) (mPenStrokes.size() * 10), 350.0F, tempPaint);
	}

	// Reset mPenStrokes
	public void resetStrokes() {
		for (Iterator<PenStroke> i = mPenStrokes.iterator(); i.hasNext();) {
			i.next().reset();
		}
	}

	// Reset mPenSegments
	public void resetSegments() {
		for (Iterator<PenSegment> i = mPenSegments.iterator(); i.hasNext();) {
			i.next().reset();
		}
	}

	// Method for getting candidate characters
	// Get candidates for 1-stroke character
	private void get1SegmentCharacterCandidates() {
		char strokeChar0 = mPenSegments.elementAt(0).mPenSegmentCharacter;
		SegmentBitSet sBitSet0 = SegmentBitSet.getSegmentBitSetForChar(strokeChar0);
		sBitSet0.mSegmentBitSet.and(SegmentBitSet.ONE_SEGMENT_BITSET.mSegmentBitSet);
		mPenCharacterCandidates = sBitSet0.getCharacters();
		// mPenCharacter = mPenSegments.elementAt(0).mPenSegmentCharacter;
	}

	// Get candidates for 2-stroke character
	private void get2SegmentCharacterCandidates() {
		char strokeChar0 = mPenSegments.elementAt(0).mPenSegmentCharacter;
		char strokeChar1 = mPenSegments.elementAt(1).mPenSegmentCharacter;
		SegmentBitSet sBitSet0 = SegmentBitSet.getSegmentBitSetForChar(strokeChar0);
		SegmentBitSet sBitSet1 = SegmentBitSet.getSegmentBitSetForChar(strokeChar1);
		SegmentBitSet s2SegmentsBitSet = new SegmentBitSet();
		s2SegmentsBitSet.copy(SegmentBitSet.TWO_SEGMENTS_BITSET);

		sBitSet0.mSegmentBitSet.and(sBitSet1.mSegmentBitSet);

		sBitSet0.mSegmentBitSet.and(s2SegmentsBitSet.mSegmentBitSet);

		mPenCharacterCandidates = sBitSet0.getCharacters();

		//		}
	}

	// Get candidates for 3-stroke character
	private void get3SegmentCharacterCandidates() {
		char strokeChar0 = mPenSegments.elementAt(0).mPenSegmentCharacter;
		char strokeChar1 = mPenSegments.elementAt(1).mPenSegmentCharacter;
		char strokeChar2 = mPenSegments.elementAt(2).mPenSegmentCharacter;
		SegmentBitSet sBitSet0 = SegmentBitSet.getSegmentBitSetForChar(strokeChar0);
		SegmentBitSet sBitSet1 = SegmentBitSet.getSegmentBitSetForChar(strokeChar1);
		SegmentBitSet sBitSet2 = SegmentBitSet.getSegmentBitSetForChar(strokeChar2);
		SegmentBitSet s3SegmentsBitSet = new SegmentBitSet();
		s3SegmentsBitSet.copy(SegmentBitSet.THREE_SEGMENTS_BITSET);

		sBitSet0.mSegmentBitSet.and(sBitSet1.mSegmentBitSet);
		sBitSet0.mSegmentBitSet.and(sBitSet2.mSegmentBitSet);
		sBitSet0.mSegmentBitSet.and(s3SegmentsBitSet.mSegmentBitSet);

		mPenCharacterCandidates = sBitSet0.getCharacters();
		//		}
	}

	// Get candidates for 4-stroke character
	private void get4SegmentCharacterCandidates() {
		char strokeChar0 = mPenSegments.elementAt(0).mPenSegmentCharacter;
		char strokeChar1 = mPenSegments.elementAt(1).mPenSegmentCharacter;
		char strokeChar2 = mPenSegments.elementAt(2).mPenSegmentCharacter;
		char strokeChar3 = mPenSegments.elementAt(3).mPenSegmentCharacter;

		/*
		  if ((strokeChar0 != strokeChar1) & (strokeChar0 != strokeChar2) 
				& (strokeChar0 != strokeChar2) & (strokeChar0 != strokeChar3)
				& (strokeChar1 != strokeChar2) & (strokeChar1 != strokeChar3)
				& (strokeChar2 != strokeChar3)) {
		 */
		SegmentBitSet sBitSet0 = SegmentBitSet.getSegmentBitSetForChar(strokeChar0);
		SegmentBitSet sBitSet1 = SegmentBitSet.getSegmentBitSetForChar(strokeChar1);
		SegmentBitSet sBitSet2 = SegmentBitSet.getSegmentBitSetForChar(strokeChar2);
		SegmentBitSet sBitSet3 = SegmentBitSet.getSegmentBitSetForChar(strokeChar3);
		SegmentBitSet s4SegmentsBitSet = new SegmentBitSet();
		s4SegmentsBitSet.copy(SegmentBitSet.FOUR_SEGMENTS_BITSET);

		sBitSet0.mSegmentBitSet.and(sBitSet1.mSegmentBitSet);
		sBitSet0.mSegmentBitSet.and(sBitSet2.mSegmentBitSet);
		sBitSet0.mSegmentBitSet.and(sBitSet3.mSegmentBitSet);
		sBitSet0.mSegmentBitSet.and(s4SegmentsBitSet.mSegmentBitSet);

		mPenCharacterCandidates = sBitSet0.getCharacters();
		//}
	}

	public void getCharacterCandidates(Canvas canvas, Paint textPaint) {

		switch (mPenSegments.size()) {
		case 1: 
			get1SegmentCharacterCandidates();
			break;
		case 2: 
			get2SegmentCharacterCandidates();
			break;
		case 3: 
			get3SegmentCharacterCandidates();
			break;
		case 4: 
			get4SegmentCharacterCandidates();
			break;
		default:
			mPenCharacterCandidates = "???";
		}
	}

	// Check to see if a float is greater than the low and less than high thresholds
	private boolean isBetweenThresholds(double num, double lowThreshold, double highThreshold) {
		return ((lowThreshold < num) & (num < highThreshold));
	}

	// Check to see if the strokes form a small letter (that is the rectangle bounding them is small enough for a small letter)
	private boolean isSmallLetter() {

		return isBetweenThresholds(mPenStrokesMaxY - mPenStrokesMinY, 0, (float) (0.4 * Skiggle.DEFAULT_WRITE_PAD_HEIGHT));

	}

	// Get the x,y coordinates of the left and right of a stroke (like a '-')
	// and return a 4-element array containing the leftX, leftY, rightX and rightY respectively
	private float[] getLeftRightCoordsOfSegment(PenSegment pSegment) {
		// Initially assume the start of the stroke
		float leftX = pSegment.mPosStart[0]; // x-coord of top end of the stroke
		float leftY = pSegment.mPosStart[1]; // y-coord of top end of the stroke
		float rightX = pSegment.mPosEnd[0]; // x-coord of bottom end of the stroke
		float rightY = pSegment.mPosEnd[1]; // y-coord of bottom end of the stroke
		// Swap the left and right points of the stroke if necessary
		if (rightX < leftX) {
			leftX = rightX;
			leftY = rightY;
			rightX = pSegment.mPosStart[0];
			rightY = pSegment.mPosStart[1];
		}
		float coords[] = {leftX, leftY, rightX, rightY};
		return coords;
	}

	// Get the x,y coordinates of the top and bottom of a stroke (like a '/', '\', or '|')
	// and return a 4-element array containing the topX, topY, bottomX and bottomY respectively
	private float[] getTopBottomCoordsOfSegment(PenSegment pSegment) {
		// Initially assume the start of the stroke is the top
		float topX = pSegment.mPosStart[0]; // x-coord of top end of the stroke
		float topY = pSegment.mPosStart[1]; // y-coord of top end of the stroke
		float bottomX = pSegment.mPosEnd[0]; // x-coord of bottom end of the stroke
		float bottomY = pSegment.mPosEnd[1]; // y-coord of bottom end of the stroke
		// Swap the top and bottom ends of the stroke if necessary
		if (bottomY < topY) {
			topX = bottomX;
			topY = bottomY;
			bottomX = pSegment.mPosStart[0];
			bottomY = pSegment.mPosStart[1];
		}
		float coords[] = {topX, topY, bottomX, bottomY};
		return coords;
	}

	// Get the gaps between the tops and bottoms of two strokes
	// and return a 2-element array containing the top gap and bottom gap
	private float[] getTopBottomGapsBetween2Segments(PenSegment pSegment1, PenSegment pSegment2) {
		float coords[] = getTopBottomCoordsOfSegment(pSegment1);
		float stroke1TopX = coords[0]; // x-coord of top end of FSLASH stroke
		float stroke1TopY = coords[1]; // y-coord of top end of FSLASH stroke
		float stroke1BottomX = coords[2]; // x-coord of bottom end of FSLASH stroke
		float stroke1BottomY = coords[3]; // y-coord of bottom end of FSLASH stroke

		coords = getTopBottomCoordsOfSegment(pSegment2);
		float stroke2TopX = coords[0]; // x-coord of top end of FSLASH stroke
		float stroke2TopY = coords[1]; // y-coord of top end of FSLASH stroke
		float stroke2BottomX = coords[2]; // x-coord of bottom end of FSLASH stroke
		float stroke2BottomY = coords[3]; // y-coord of bottom end of FSLASH stroke

		float gaps[] = {
				PenUtil.distanceBetween2Points(stroke1TopX, stroke1TopY, stroke2TopX, stroke2TopY),
				PenUtil.distanceBetween2Points(stroke1BottomX, stroke1BottomY, stroke2BottomX, stroke2BottomY)};
		return gaps;
	}

	// Get the x, y coordinates of points at 1/3 and 2/3 from the left (or top) line between the points (left or top
	// coordinates first
	private float[] getPointsAt1stAnd2ndThirdMarks(float leftOrTopX, float leftOrTopY, float rightOrBottomX, float rightOrBottomY) {
		// Get the x,y coordinates at the one-third and two-third marks of the line between
		// the points (leftOrTopX, leftOrTopY) and  (rightOrBottomX, rightOrBottomY)

		float oneThirdX = (rightOrBottomX - leftOrTopX)/3; // one third the distance between 
		float oneThirdY = (rightOrBottomY - leftOrTopY)/3; // one third the distance between left and right y-ccords

		float coords[] = {
				leftOrTopX + oneThirdX,  // x-coord of first one-third mark
				leftOrTopY + oneThirdY,  // y-coord of first one-third mark
				leftOrTopX + 2 * oneThirdX, // x-coord of second one-third mark
				leftOrTopY + 2 * oneThirdY}; // x-coord of second one-third mark

		return coords;
	}

	// Order a pair of PenSegments into the left and right and return them as an array of 2 with
	// left PenSegment as first element and right PenSegment as the second element.
	private PenSegment[] order2PenSegmentsIntoLeftRight(PenSegment pSegment1, PenSegment pSegment2) {
		PenSegment leftRightPenSegments[] = {pSegment1, pSegment2}; // Assume pSegment1 is on left of pSegment2 initially.

		// Determine which of the 2 HLINE strokes is on the left and which is on the right.

		// Initially assume the first PenSegment found to be the left PenSegment of the 'I'.
		float leftSegmentStartX = pSegment1.mPosStart[0]; // x-coord of left point of the first PenSegment stroke.
		float leftSegmentEndX = pSegment1.mPosEnd[0]; // x-coord of bottom end of the first PenSegment stroke.
		float leftSegmentMidX = (leftSegmentStartX + leftSegmentEndX)/2;

		// Initially assume the second PenSegment found to be the right PenSegment of the 'I'.
		float rightSegmentStartX = pSegment2.mPosStart[0]; // x-coord of left point of second PenSegment stroke.
		float rightSegmentEndX = pSegment2.mPosEnd[0]; // x-coord of right point of second PenSegment stroke.
		float rightSegmentMidX = (rightSegmentStartX + rightSegmentEndX)/2;

		// If the second PenSegment is on left of the first PenSegment, swap them (make the second PenSegment the left
		// and the first PenSegment the right).
		if (rightSegmentMidX < leftSegmentMidX) {
			leftRightPenSegments[0] = pSegment2;
			leftRightPenSegments[1] = pSegment1;
		}
		return leftRightPenSegments;
	}

	// Order a pair of PenSegments into the Right and bottom and return them as an array of 2 with
	// top PenSegment as first element and bottom PenSegment as the second element.
	private PenSegment[] order2PenSegmentsIntoTopBottom(PenSegment pSegment1, PenSegment pSegment2) {
		PenSegment topBottomPenSegments[] = {pSegment1, pSegment2}; // Assume pSegment1 is on top of pSegment2 initially.

		// Determine which of the 2 HLINE strokes is at the top and which is at the bottom.

		// Initially assume the first PenSegment found to be the top PenSegment of the 'I'.
		float topSegmentStartY = pSegment1.mPosStart[1]; // y-coord of top end of the first PenSegment stroke.
		float topSegmentEndY = pSegment1.mPosEnd[1]; // y-coord of bottom end of the first PenSegment stroke.
		float topSegmentMidY = (topSegmentStartY + topSegmentEndY)/2;

		// Initially assume the second PenSegment found to be the bottom PenSegment of the 'I'.
		float bottomSegmentStartY = pSegment2.mPosStart[1]; // y-coord of top end of second PenSegment stroke.
		float bottomSegmentEndY = pSegment2.mPosEnd[1]; // y-coord of bottom end of second PenSegment stroke.
		float bottomSegmentMidY = (bottomSegmentStartY + bottomSegmentEndY)/2;

		// If the second PenSegment is on top of the first PenSegment, swap them (make the second PenSegment the top
		// and the first PenSegment the bottom).
		// Note: y increases downwards (y values of the top stroke are smaller than those of the bottom stroke).
		if (bottomSegmentMidY < topSegmentMidY) {
			topBottomPenSegments[0] = pSegment2;
			topBottomPenSegments[1] = pSegment1;
		}
		return topBottomPenSegments;
	}

	// Check to see if the gap between the top (bottom) of a caret ('/' and '\') or V ('\' or '/') are close enough
	// That is, the gap between the tops for caret (or bottoms for 'V') of FSLASH and BSLASH are less than one
	// quarter the distance between their bases
	private boolean gapCheckForCaretShape(PenSegment pSegment1, PenSegment pSegment2) {
		float gaps[] = getTopBottomGapsBetween2Segments(pSegment1, pSegment2);
		float gapBetweenTops = gaps[0];
		float gapBetweenBottoms = gaps[1];
		return 	(gapBetweenTops < 0.25 * gapBetweenBottoms);
	}

	private boolean gapCheckForVShape(PenSegment pSegment1, PenSegment pSegment2) {
		float gaps[] = getTopBottomGapsBetween2Segments(pSegment1, pSegment2);
		float gapBetweenTops = gaps[0];
		float gapBetweenBottoms = gaps[1];
		return 	(gapBetweenBottoms < 0.25 * gapBetweenTops);
	}

	// '9' or 'q' has a FC and a dot2Index on the right joining the VLINE at the top and middle
	private boolean checkFor9OrSmallQ(float vLineTopYMin, float vLineTopYMax) {
		boolean matchedP = false;
		int numOfSegments = mPenSegments.size();

		// VLINE is the first stroke and BC the second, both written top down.
		if (numOfSegments == 2) {
			int fCIndex = -1;
			int vLineIndex = -1;

			for (int i = 0; i < numOfSegments; i++) {
				// 'q' has only two pen stroke characters
				switch (mPenSegments.elementAt(i).mPenSegmentCharacter) {
				case PenSegment.VLINE_CHAR:
					vLineIndex = i;
					break;
				case PenSegment.FC_CHAR:
					fCIndex = i;
					break;
				default:
					break;
				}
			}

			// Check to make sure that the two component strokes for 'q' are there, i.e.,
			// fCIndex and vLineIndex are both not negative
			if ((fCIndex >= 0) & (vLineIndex >= 0)) {
				// Get the x,y of the top and bottom of FC
				float coords[] = getTopBottomCoordsOfSegment(mPenSegments.elementAt(fCIndex));
				float fCTopX = coords[0]; // x-coord of top end of the right FC stroke
				float fCTopY = coords[1]; // y-coord of top end of the right FC stroke
				float fCBottomX = coords[2]; // x-coord of bottom end of the right FC stroke
				float fCBottomY = coords[3]; // y-coord of bottom end of the right FC stroke

				// Get x,y of the top and bottom of VLINE
				coords = getTopBottomCoordsOfSegment(mPenSegments.elementAt(vLineIndex));
				float vLineTopX = coords[0]; // x-coord of top end of the VLINE stroke
				float vLineTopY = coords[1]; // y-coord of top end of the VLINE stroke
				float vLineBottomX = coords[2]; // x-coord of bottom end of the VLINE stroke
				float vLineBottomY = coords[3]; // y-coord of bottom end of the VLINE stroke
				float vLineMidX = (vLineTopX + vLineBottomX)/2; // average of x-coord (mid-point) of the VLINE stroke
				float vLineMidY = (vLineTopY + vLineBottomY)/2; // average of y-coord (mid-point) of the VLINE stroke

				float vLineHeight = Math.abs(vLineBottomY - vLineTopY);
				double gapThreshold = .25 * vLineHeight;

				// Check to see if the following gaps are less their respective thresholds
				// i.   The gap between the top ends of VLINE and FC
				// ii.  The gap between the bottom of FC and the mid-point of VLINE
				// iii. Distance of the top end of the VLINE from the top of the writing area
				matchedP =
					(PenUtil.distanceBetween2Points(vLineTopX, vLineTopY, fCTopX, fCTopY) < gapThreshold) &
					(PenUtil.distanceBetween2Points(vLineMidX, vLineMidY, fCBottomX, fCBottomY) < gapThreshold) &
					isBetweenThresholds(vLineTopY, vLineTopYMin, vLineTopYMax);
			}
		}
		return matchedP;
	}  // End of checkFor9OrSmallQ()

	// Check to see if the stroke is a 'C' or 'c'.  Both is made up of a single stroke, FC.
	private boolean checkForCShape() { //float minSize, float maxSize){
		boolean matchedP = false;

		int numOfSegments = mPenSegments.size();

		// 'C' has only one pen stroke character.
		if (numOfSegments == 1) {
			float heightOfC = mPenStrokesMaxY - mPenStrokesMinY;
			float widthOfC = mPenStrokesMaxX - mPenStrokesMinX;
			


			matchedP =
				// Must be a forward character primitive
				(mPenSegments.elementAt(0).mPenSegmentCharacter == PenSegment.FC_CHAR) &
				/*
				// and the ends of the stroke is less than its height
				(PenUtil.distanceBetween2Points(
						mPenSegments.elementAt(0).mPosStart[0], mPenSegments.elementAt(0).mPosStart[1],
						mPenSegments.elementAt(0).mPosEnd[0], mPenSegments.elementAt(0).mPosEnd[1])
						< 0.9 * heightOfC); & */
						// and the width of the stroke is more than .4 its height
						(widthOfC > .4 * heightOfC);


		}
		return matchedP;
	}  // End of checkForCShape()

	// Check for the three strokes that form 'K' or 'k' and return either 'K', 'k' or NUL (Ascii value 0)
	private char checkForCapitalOrSmallK() {
		char c = '\0';
		int numOfSegments = mPenSegments.size();

		if (numOfSegments == 3) {
			int fSlashIndex = -1;
			int bSlashIndex = -1;
			int vLineIndex = -1;

			for (int i = 0; i < numOfSegments; i++) {
				// 'K' has only three pen stroke characters

				switch (mPenSegments.elementAt(i).mPenSegmentCharacter) {
				case PenSegment.VLINE_CHAR:
					vLineIndex = i;
					break;
				case PenSegment.FSLASH_CHAR:
					fSlashIndex = i;
					break;
				case PenSegment.BSLASH_CHAR:
					bSlashIndex = i;
					break;
				default:
					break;
				}
			}

			// Check to make sure that the three component strokes for 'K' are there, i.e.,
			// fSlashIndex, bSlashIndex, and vLineIndex are all not negative
			if ((fSlashIndex >= 0) & (bSlashIndex >= 0) & (vLineIndex >= 0)) {

				float coords[] = getTopBottomCoordsOfSegment(mPenSegments.elementAt(fSlashIndex));
				float fSlashTopY = coords[1]; // y-coord of top end of the FSLASH stroke (above BSLASH)
				float fSlashBottomX = coords[2]; // x-coord of bottom end of the FSLASH stroke (above BSLASH)
				float fSlashBottomY = coords[3]; // y-coord of bottom end of the FSLASH stroke (above BSLASH)

				coords = getTopBottomCoordsOfSegment(mPenSegments.elementAt(bSlashIndex));
				float bSlashTopX = coords[0]; // x-coord of top end of the BSLASH stroke (below FSLASH)
				float bSlashTopY = coords[1]; // y-coord of top end of the BSLASH stroke (below FSLASH)
				float bSlashBottomY = coords[3]; // y-coord of bottom end of the BSLASH stroke (below FSLASH)

				// Get x,y of the top and bottom of VLINE
				coords = getTopBottomCoordsOfSegment(mPenSegments.elementAt(vLineIndex));

				float vLineTopX = coords[0]; // x-coord of top end of the VLINE stroke
				float vLineTopY = coords[1]; // y-coord of top end of the VLINE stroke
				float vLineBottomX = coords[2]; // x-coord of bottom end of the VLINE stroke
				float vLineBottomY = coords[3]; // y-coord of bottom end of the VLINE stroke
				float vLineMidX = (vLineTopX + vLineBottomX)/2; // x-coord of middle of VLINE stroke (avg of top and bottom x-coord)
				float vLineMidY = (vLineTopY + vLineBottomY)/2; // y-coord of middle of VLINE stroke (avg of top and bottom y-coord)

				float vLineHeight = Math.abs(vLineBottomY - vLineTopY);
				double gapThreshold = 0.25 * vLineHeight;

				// Check to see if the following gaps are less than their respective thresholds:
				// i.   The gap between the bottom of FSLASH and top of BSLASH
				// ii.  The gap between the bottom of FSLASH and the VLINE mid-point 
				// iii. The gap between the top of BSLASH and the VLINE mid-point

				if	((PenUtil.distanceBetween2Points(fSlashBottomX, fSlashBottomY, bSlashTopX, bSlashTopY) < gapThreshold) &
						(PenUtil.distanceBetween2Points(fSlashBottomX, fSlashBottomY, vLineMidX, vLineMidY) < gapThreshold) &
						(PenUtil.distanceBetween2Points(bSlashTopX, bSlashTopY, vLineMidX, vLineMidY) < gapThreshold))
					// If the vertical height between FSLASH and BSLASH is more than .75 the VLINE height then it is a 'K'
					if ((bSlashBottomY - fSlashTopY) > .75 * vLineHeight) c = 'K';
				// Otherwise it is a 'k'
					else c = 'k';
			}
		}
		return c;
	} // End of checkForKShape()


	// Check to see if the stroke is a 'O' or 'o'.  Both is made up of a single stroke, FC.
	private boolean checkForOShape(){
		boolean matchedP = false;

		int numOfSegments = mPenSegments.size();

		// 'O'or 'o' has only one pen stroke character.
		if (numOfSegments == 1) {
			matchedP = (mPenSegments.elementAt(0).mPenSegmentCharacter == PenSegment.CIRCLE_CHAR);
		}
		return matchedP;
	}  // End of checkForOShape()

	// Check to see if the two strokes (VLINE and BC) form the shape of a 'P'.   
	// This method is used by the methods checking for capital 'P' and small 'p'.
	// Capital 'P' has a height larger than 2/3 the height of the writing space.
	// The height of a small 'p' is less 2/3 the height of the writing space.
	// 'P' has a VLINE and a BC (backward C or ')') on the right joining the VLINE at the top and mid-point
	private boolean checkForPShape() {
		boolean matchedP = false;
		int numOfSegments = mPenSegments.size();

		// VLINE is the first stroke and BC the second, both written top down.
		if (numOfSegments == 2) {
			int vLineIndex = -1;
			int bCIndex = -1;

			for (int i = 0; i < numOfSegments; i++) {
				// 'P' has only two pen stroke characters
				switch (mPenSegments.elementAt(i).mPenSegmentCharacter) {
				case PenSegment.VLINE_CHAR:
					vLineIndex = i;
					break;
				case PenSegment.BC_CHAR:
					bCIndex = i;
					break;
				default:
					break;
				}
			}

			// Check to make sure that the two component strokes for 'P' are there, i.e.,
			// bCIndex and vLineIndex are both not negative
			if ((bCIndex >= 0) & (vLineIndex >= 0)) {
				// Get x,y of the top and bottom of VLINE
				float coords[] = getTopBottomCoordsOfSegment(mPenSegments.elementAt(vLineIndex));
				float vLineTopX = coords[0]; // x-coord of top end of the VLINE stroke
				float vLineTopY = coords[1]; // y-coord of top end of the VLINE stroke
				float vLineBottomX = coords[2]; // x-coord of bottom end of the VLINE stroke
				float vLineBottomY = coords[3]; // y-coord of bottom end of the VLINE stroke
				float vLineMidX = (vLineTopX + vLineBottomX)/2; // average of x-coord (mid-point) of the VLINE stroke
				float vLineMidY = (vLineTopY + vLineBottomY)/2; // average of y-coord (mid-point) of the VLINE stroke

				// Get the x,y of the top and bottom of BC
				coords = getTopBottomCoordsOfSegment(mPenSegments.elementAt(bCIndex));
				float bCTopX = coords[0]; // x-coord of top end of the right BC stroke
				float bCTopY = coords[1]; // y-coord of top end of the right BC stroke
				float bCBottomX = coords[2]; // x-coord of bottom end of the right BC stroke
				float bCBottomY = coords[3]; // y-coord of bottom end of the right BC stroke

				float vLineHeight = Math.abs(vLineBottomY - vLineTopY);
				double gapThreshold = 0.25 * vLineHeight;

				// Check to see if the following gaps are less their respective thresholds
				// i.   The gap between the top ends VLINE and BC
				matchedP =
					(PenUtil.distanceBetween2Points(vLineTopX, vLineTopY, bCTopX, bCTopY) < gapThreshold) &
					(PenUtil.distanceBetween2Points(vLineMidX, vLineMidY, bCBottomX, bCBottomY) < 2 * gapThreshold);
			}
		}
		return matchedP;
	}  // End of checkForPShape()

	// Check for 'S' for capital 'S' and small 's'
	private boolean checkForSShape() {
		boolean matchedP = false;
		int numOfSegments = mPenSegments.size();

		if (numOfSegments == 2) {
			int fCIndex = -1; // first stroke N to S
			int bCIndex = -1; // second stroke N to S
			for (int i = 0; i < numOfSegments; i++) {
				// 'S' has only two pen stroke characters
				switch (mPenSegments.elementAt(i).mPenSegmentCharacter) {
				case PenSegment.FC_CHAR:
					fCIndex = i;
					break;
				case PenSegment.BC_CHAR:
					bCIndex = i;
					break;
				default:
					break;
				}
			}
			// Check to make sure that the two component strokes for 'S' are there, i.e.,
			// fSlashIndex and bSlashIndex are both not negative
			if ((fCIndex >= 0) & (bCIndex >= 0)) {
				float coords[] = getTopBottomCoordsOfSegment(mPenSegments.elementAt(fCIndex));		
				float fCTopX = coords[0]; // x-coord of top end of the FC stroke
				float fCTopY = coords[1]; // y-coord of top end of the FC stroke
				float fCBottomX = coords[2]; // x-coord of bottom end of the FC stroke
				float fCBottomY = coords[3]; // y-coord of bottom end of the FC stroke

				coords = getTopBottomCoordsOfSegment(mPenSegments.elementAt(bCIndex));		
				float bCTopX = coords[0]; // x-coord of top end of the BC stroke
				float bCTopY = coords[1]; // y-coord of top end of the BC stroke
				float bCBottomX = coords[2]; // x-coord of bottom end of the BC stroke
				float bCBottomY = coords[3]; // y-coord of bottom end of the BC stroke

				float endToEndDistance = PenUtil.distanceBetween2Points(fCTopX, fCTopY, bCBottomX, bCBottomY);
				float minGapBetweenFCAndBC = PenUtil.distanceBetween2Points(fCBottomX, fCBottomY, bCTopX, bCTopY);

				// Check to make sure BC and FC cross sufficiently to form the 'S'
				matchedP = (minGapBetweenFCAndBC < 0.25 * endToEndDistance);
			}
		}
		return matchedP;
	} // End of checkForSShape()


	// Check to see if the stroke is a 'U' or 'u'.  Both is made up of a single stroke, U.
	private boolean checkForUShape(){
		boolean matchedP = false;

		int numOfSegments = mPenSegments.size();

		// 'C' has only one pen stroke character.
		if (numOfSegments == 1) {
			matchedP = (mPenSegments.elementAt(0).mPenSegmentCharacter == PenSegment.U_CHAR);
		}
		return matchedP;
	}  // End of checkForUShape()

	// 'V' or 'v' is made up of a back slash ('\') and a forward slash ('/')
	private boolean checkForVShape(){
		boolean matchedP = false;

		int numOfSegments = mPenSegments.size();

		// 'V' has only two pen stroke characters.
		if (numOfSegments == 2) {
			matchedP =
				(gapCheckForVShape(mPenSegments.elementAt(0), mPenSegments.elementAt(1)));
		}
		return matchedP;
	} // End of checkForVShape()

	// 'W' or 'w' has two pairs of BSLASH's ('\') and a FSLASH's ('/') or V's, side by side
	private boolean checkForWShape() {
		boolean matchedP = false;
		int numOfSegments = mPenSegments.size();

		if (numOfSegments == 4) {
			int leftBSlashIndex = -1; 
			int leftFSlashIndex = -1;
			int rightBSlashIndex = -1;
			int rightFSlashIndex = -1;
			for (int i = 0; i < numOfSegments; i++) {
				// 'W' has only four pen stroke characters
				switch (mPenSegments.elementAt(i).mPenSegmentCharacter) {
				case PenSegment.BSLASH_CHAR:
					if (leftBSlashIndex < 0)
						leftBSlashIndex = i;
					else
						rightBSlashIndex = i;
					break;
				case PenSegment.FSLASH_CHAR:
					if (leftFSlashIndex < 0)
						leftFSlashIndex = i;
					else
						rightFSlashIndex = i;
					break;
				default:
					break;
				}
			}

			// Check to make sure that the four component strokes for 'W' are there, i.e.,
			// leftFSlashIndex, leftBSlashIndex, rightFSlashIndex, and rightVLineIndex are all not negative
			if ((leftBSlashIndex >= 0) & (leftFSlashIndex >= 0) & (rightBSlashIndex >= 0) & (rightFSlashIndex >= 0)) {
				// Get the left and right BSLASH strokes of 'W'
				PenSegment leftRightBSlashSegments[] = 
					order2PenSegmentsIntoLeftRight(mPenSegments.elementAt(leftBSlashIndex),
							mPenSegments.elementAt(rightBSlashIndex));

				PenSegment leftBSlashSegment = leftRightBSlashSegments[0];
				PenSegment rightBSlashSegment = leftRightBSlashSegments[1];	

				// Get the left and right FSLASH strokes of 'W'
				PenSegment leftRightFSlashSegments[] = 
					order2PenSegmentsIntoLeftRight(mPenSegments.elementAt(leftFSlashIndex),
							mPenSegments.elementAt(rightFSlashIndex));

				PenSegment leftFSlashSegment = leftRightFSlashSegments[0];
				PenSegment rightFSlashSegment = leftRightFSlashSegments[1];	

				// Check to see if the following gaps are close enough:
				// i.   Gap between the bottom of the left BSLASH and the bottom of the left FSLASH (bottom of first 'V' of 'W')
				// ii.  Gap between the top of the left FSLASH and the bottom of the right BSLASH (top middle caret of 'W')
				// iii. Gap between the bottom of the right BSLASH and the bottom of the right FSLASH (bottom of second 'V' of 'W')
				matchedP = 
					(gapCheckForVShape(leftBSlashSegment, leftFSlashSegment))  // Check for left '\/' of 'W'
					& (gapCheckForCaretShape(leftFSlashSegment, rightBSlashSegment)) // Check for middle '/\' of 'W'
					& (gapCheckForVShape(rightBSlashSegment, rightFSlashSegment));  // Check for right '\/' of 'W'
			}
		}
		return matchedP;
	} // End of checkForWShape()

	// Check for 'X' for capital 'X' and small 'x'
	private boolean checkForXShape() {
		boolean matchedP = false;
		int numOfSegments = mPenSegments.size();


		if (numOfSegments == 2) {
			int bSlashIndex = -1; // first stroke NW to SE
			int fSlashIndex = -1; // second stroke NE to SW
			for (int i = 0; i < numOfSegments; i++) {
				// 'X' has only two pen stroke characters
				switch (mPenSegments.elementAt(i).mPenSegmentCharacter) {
				case PenSegment.BSLASH_CHAR:
					bSlashIndex = i;
					break;
				case PenSegment.FSLASH_CHAR:
					fSlashIndex = i;
					break;
				default:
					break;
				}
			}
			// Check to make sure that the two component strokes for 'X' are there, i.e.,
			// fSlashIndex and bSlashIndex are both not negative
			if ((bSlashIndex >= 0) & (fSlashIndex >= 0)) {
				float coords[] = getTopBottomCoordsOfSegment(mPenSegments.elementAt(bSlashIndex));		
				float bSlashTopX = coords[0]; // x-coord of top end of the BSLASH stroke
				float bSlashTopY = coords[1]; // y-coord of top end of the BSLASH stroke
				float bSlashBottomX = coords[2]; // x-coord of bottom end of the BSLASH stroke
				float bSlashBottomY = coords[3]; // y-coord of bottom end of the BSLASH stroke
				float bSlashAvgX = (bSlashTopX + bSlashBottomX)/2; // average of x-coord (mid-point) of BSLASH stroke
				float bSlashAvgY = (bSlashTopY + bSlashBottomY)/2; // average of y-coord (mid-point) of BSLASH stroke

				coords = getTopBottomCoordsOfSegment(mPenSegments.elementAt(fSlashIndex));		
				float fSlashTopX = coords[0]; // x-coord of top end of the FSLASH stroke
				float fSlashTopY = coords[1]; // y-coord of top end of the FSLASH stroke
				float fSlashBottomX = coords[2]; // x-coord of bottom end of the FSLASH stroke
				float fSlashBottomY = coords[3]; // y-coord of bottom end of the FSLASH stroke
				float fSlashAvgX = (fSlashTopX + fSlashBottomX)/2; // average of x-coord (mid-point) of FSLASH stroke
				float fSlashAvgY = (fSlashTopY + fSlashBottomY)/2; // average of y-coord (mid-point) of FSLASH stroke

				float topGap = PenUtil.distanceBetween2Points(fSlashTopX, fSlashTopY, bSlashTopX, bSlashTopY);
				float bottomGap = PenUtil.distanceBetween2Points(fSlashBottomX, fSlashBottomY, bSlashBottomX, bSlashBottomY);
				float midGap = PenUtil.distanceBetween2Points(fSlashAvgX, fSlashAvgY, bSlashAvgX, bSlashAvgY);
				float maxGap = Math.max(topGap, bottomGap);
				float minGap = Math.min(topGap, bottomGap);

				// Check to make sure FSLASH and BSLASH cross sufficiently to form the 'X'
				matchedP = 
					( minGap > .25 * maxGap) // min horizontal gap between FSLASH and BSLASH must be at least one quarter that of the max gap
					& (fSlashTopX > bSlashTopX) & (fSlashTopY < bSlashBottomY) // top of FSLASH must to right of the top of BSLASH and above the bottom of BSLASH
					& (fSlashBottomX < bSlashBottomX) & (fSlashBottomY > bSlashTopY) // bottom of FSLASH must to left of the bottom of BSLASH and below the top of BSLASH
					& (midGap < (.25 * maxGap)); // gap between the mid points of BSLASH and FSLASH must be small enough
			}
		}
		return matchedP;
	} // End of checkForXShape()

	// 'Z' or 'z' has an HLINE ('-') at the top and bottom of a FSLASH ('/')
	private boolean checkForZShape() {
		boolean matchedP = false;
		int numOfSegments = mPenSegments.size();

		if (numOfSegments == 3) {
			int topHLineIndex = -1;
			int fSlashIndex = -1;
			int bottomHLineIndex = -1;
			for (int i = 0; i < numOfSegments; i++) {
				// 'Z' has only three pen stroke characters
				switch (mPenSegments.elementAt(i).mPenSegmentCharacter) {
				case PenSegment.HLINE_CHAR:
					if (topHLineIndex < 0)
						topHLineIndex = i;
					else
						bottomHLineIndex = i;
					break;
				case PenSegment.FSLASH_CHAR:
					fSlashIndex = i;
					break;
				default:
					break;
				}
			}

			// Check to make sure that the three component strokes for 'Z' are there, i.e.,
			// topHLineIndex, vLineIndex, and bottomHLineIndex are all not negative
			if ((topHLineIndex >= 0) & (fSlashIndex >= 0) & (bottomHLineIndex >= 0)) {
				// Get the top and bottom HLINE strokes of 'Z'
				PenSegment topBottomHLineSegments[] = 
					order2PenSegmentsIntoTopBottom(mPenSegments.elementAt(topHLineIndex),
							mPenSegments.elementAt(bottomHLineIndex));

				// Get x, y coords of right end of top HLINE
				float coords[] = getLeftRightCoordsOfSegment(topBottomHLineSegments[0]);
				float topHLineRightX = coords[2]; // x-coord of right end of top HLINE;
				float topHLineRightY = coords[3]; // y-coord of right end of top HLINE;

				// Get x, y coords of left end of bottom HLINE
				coords = getLeftRightCoordsOfSegment(topBottomHLineSegments[1]);
				float bottomHLineLeftX = coords[0]; // x-coord of left end of bottom HLINE;
				float bottomHLineLeftY = coords[1]; // y-coord of left end of bottom HLINE;

				// Get x,y of top and bottom of FSLASH
				coords = getTopBottomCoordsOfSegment(mPenSegments.elementAt(fSlashIndex));
				float fSlashTopX = coords[0]; // x-coord of top end of the FSLASH stroke
				float fSlashTopY = coords[1]; // y-coord of top end of the FSLASH stroke
				float fSlashBottomX = coords[2]; // x-coord of bottom end of the FSLASH stroke
				float fSlashBottomY = coords[3]; // y-coord of bottom end of the FSLASH stroke

				float fSlashHeight = Math.abs(fSlashBottomY - fSlashTopY);
				double gapThreshold = 0.25 * fSlashHeight;

				// Check to see if the gaps between the mid-points of the top and bottom HLINE's are less than the gap threshold
				matchedP =
					(PenUtil.distanceBetween2Points(fSlashTopX, fSlashTopY, topHLineRightX, topHLineRightY) < gapThreshold) &
					(PenUtil.distanceBetween2Points(fSlashBottomX, fSlashBottomY, bottomHLineLeftX, bottomHLineLeftY) < gapThreshold);
			}
		}
		return matchedP;
	} // End of checkForZShape()


	// Check for specific characters

	// Caret character (or inverted V) is made up of a forward slash ('/') and a back slash ('\')
	private boolean checkForCaret(){
		boolean matchedP = false;

		int numOfSegments = mPenSegments.size();

		// Caret has only two pen stroke characters.
		if (numOfSegments == 2) {
			matchedP = (gapCheckForCaretShape(mPenSegments.elementAt(0), mPenSegments.elementAt(1)));
		}

		return matchedP;
	}  // End of checkForCaret()

	/*****************
	 *               *
	 * Digits 0 to 9 *
	 *               *
	 *****************/

	// ???
	private void checkForNumber1() {
		if (mPenSegments.size() == 1) {
			PenSegment firstSegment = mPenSegments.elementAt(0);	

			if (firstSegment.mPenSegmentCharacter == PenSegment.VLINE_CHAR) {

				mPenCharacter = '1';

			}
		}
	}

	// 3 has two strokes - two BC (backward C or ')') strokes stacked on top of one another.
	private boolean checkFor3() {
		boolean matchedP = false;
		int numOfSegments = mPenSegments.size();

		// 3 has only two pen stroke characters
		if (numOfSegments == 2) {
			int topBCIndex = -1;
			int bottomBCIndex = -1;

			for (int i = 0; i < numOfSegments; i++) {
				switch (mPenSegments.elementAt(i).mPenSegmentCharacter) {
				case PenSegment.BC_CHAR:
					if (topBCIndex < 0) topBCIndex = i;
					else bottomBCIndex = i;
					break;
				default:
					break;
				}
			}

			// Check to make sure that the two component strokes for '3' are there, i.e.,
			// topBCIndex and bottomBCIndex are both not negative
			if ((topBCIndex >= 0) & (bottomBCIndex >= 0)) {
				// Get the top and bottom BC strokes
				PenSegment topBottomBCSegments[] = 
					order2PenSegmentsIntoTopBottom(mPenSegments.elementAt(topBCIndex), mPenSegments.elementAt(bottomBCIndex));

				// Get x,y coords of the top BC stroke
				float coords[] = getTopBottomCoordsOfSegment(topBottomBCSegments[0]);
				float topBCTopY = coords[1]; // y-coord of top end of the top BC stroke
				float topBCBottomX = coords[2]; // x-coord of bottom end of the top BC stroke
				float topBCBottomY = coords[3]; // y-coord of bottom end of the top BC stroke

				// Get x,y coords of the bottom BC stroke
				coords = getTopBottomCoordsOfSegment(topBottomBCSegments[1]);
				float bottomBCTopX = coords[0]; // x-coord of top end of the bottom BC stroke
				float bottomBCTopY = coords[1]; // y-coord of top end of the bottom BC stroke
				float bottomBCBottomY = coords[3]; // y-coord of bottom end of the bottom BC stroke

				float height = Math.abs(bottomBCBottomY - topBCTopY);
				double gapThreshold = 0.25 * height;

				// Check to see if the gap between the bottom of the top BC and the top of the bottom BC are close enough
				matchedP =
					(PenUtil.distanceBetween2Points(topBCBottomX, topBCBottomY, bottomBCTopX, bottomBCTopY) < gapThreshold);
			}
		}
		return matchedP;
	}  // End of checkFor3()

	// 4 has three strokes - a HLINE, a VLINE, and a FSLASH.
	private boolean checkFor4() {
		boolean matchedP = false;
		int numOfSegments = mPenSegments.size();

		// 5 has only three pen stroke characters
		if (numOfSegments == 3) {
			int hLineIndex = -1;
			int vLineIndex = -1;
			int fSlashIndex = -1;

			for (int i = 0; i < numOfSegments; i++) {
				switch (mPenSegments.elementAt(i).mPenSegmentCharacter) {
				case PenSegment.HLINE_CHAR:
					hLineIndex = i;
					break;
				case PenSegment.VLINE_CHAR:
					vLineIndex = i;
					break;
				case PenSegment.FSLASH_CHAR:
					fSlashIndex = i;
					break;
				default:
					break;
				}
			}

			// Check to make sure that the two component strokes for '5' are there, i.e.,
			// hLineIndex, vLineIndex, and fSlashIndex are all not negative
			if ((hLineIndex >= 0) & (vLineIndex >= 0) & (fSlashIndex >= 0)) {
				// Get the left, right x,y coords of the HLINE stroke
				float coords[] = getLeftRightCoordsOfSegment(mPenSegments.elementAt(hLineIndex));
				float hLineLeftX = coords[0]; // x-coord of left end of the HLINE stroke
				float hLineLeftY = coords[1]; // y-coord of left end of the HLINE stroke
				float hLineRightX = coords[2]; // x-coord of right end of the HLINE stroke
				float hLineRightY = coords[3]; // y-coord of right end of the HLINE stroke
				float hLineMidX = (hLineLeftX + hLineRightX)/2; // x-ccord of mid-point of the HLINE stroke
				float hLineMidY = (hLineLeftY + hLineRightY)/2; // y-coord of mid-point of the HLINE stroke

				// Get the top, bottom x,y coords of the VLINE stroke
				coords = getTopBottomCoordsOfSegment(mPenSegments.elementAt(vLineIndex));
				float vLineTopX = coords[0]; // x-coord of top end of the VLINE stroke
				float vLineTopY = coords[1]; // y-coord of top end of the VLINE stroke
				float vLineBottomX = coords[2]; // x-coord of bottom end of the VLINE stroke
				float vLineBottomY = coords[3]; // y-coord of bottom end of the VLINE stroke
				float vLineMidX = (vLineTopX + vLineBottomX)/2; // x-ccord of mid-point of the VLINE stroke
				float vLineMidY = (vLineTopY + vLineBottomY)/2; // y-coord of mid-point of the VLINE stroke

				// Get the top, bottom x,y coords of the BC stroke
				coords = getTopBottomCoordsOfSegment(mPenSegments.elementAt(fSlashIndex));
				float fSlashBottomX = coords[2]; // x-coord of bottom end of the bottom BC stroke
				float fSlashBottomY = coords[3]; // y-coord of bottom end of the bottom BC stroke

				float height = Math.abs(vLineBottomY - vLineTopY);
				double gapThreshold = 0.25 * height;

				// Check to see if the following gaps are less than their respective thresholds
				// i.   Gap between the left end of the HLINE and the bottom end of the FSLASH
				// ii.  Gap between the mid-points of HLINE and VLINE
				matchedP =
					(PenUtil.distanceBetween2Points(hLineLeftX, hLineLeftY, fSlashBottomX, fSlashBottomY) < gapThreshold) &
					(PenUtil.distanceBetween2Points(vLineMidX, vLineMidY, hLineMidX, hLineMidY) < gapThreshold);
			}
		}
		return matchedP;
	}  // End of checkFor4()

	// 5 has three strokes - a HLINE, a VLINE, and a BC (backward C or ')') strokes stacked on top of one another.
	private boolean checkFor5() {
		boolean matchedP = false;
		int numOfSegments = mPenSegments.size();

		// 5 has only three pen stroke characters
		if (numOfSegments == 3) {
			int hLineIndex = -1;
			int vLineIndex = -1;
			int bCIndex = -1;

			for (int i = 0; i < numOfSegments; i++) {
				switch (mPenSegments.elementAt(i).mPenSegmentCharacter) {
				case PenSegment.HLINE_CHAR:
					hLineIndex = i;
					break;
				case PenSegment.VLINE_CHAR:
					vLineIndex = i;
					break;
				case PenSegment.BC_CHAR:
					bCIndex = i;
					break;
				default:
					break;
				}
			}

			// Check to make sure that the two component strokes for '5' are there, i.e.,
			// hLineIndex, vLineIndex, and bCIndex are all not negative
			if ((hLineIndex >= 0) & (vLineIndex >= 0) & (bCIndex >= 0)) {
				// Get the left, right x,y coords of the HLINE stroke
				float coords[] = getLeftRightCoordsOfSegment(mPenSegments.elementAt(hLineIndex));
				float hLineLeftX = coords[0]; // x-coord of left end of the HLINE stroke
				float hLineLeftY = coords[1]; // y-coord of left end of the HLINE stroke

				// Get the top, bottom x,y coords of the VLINE stroke
				coords = getTopBottomCoordsOfSegment(mPenSegments.elementAt(vLineIndex));
				float vLineTopX = coords[0]; // x-coord of top end of the VLINE stroke
				float vLineTopY = coords[1]; // y-coord of top end of the VLINE stroke
				float vLineBottomX = coords[2]; // x-coord of bottom end of the VLINE stroke
				float vLineBottomY = coords[3]; // y-coord of bottom end of the VLINE stroke

				// Get the top, bottom x,y coords of the BC stroke
				coords = getTopBottomCoordsOfSegment(mPenSegments.elementAt(bCIndex));
				float bCTopX = coords[0]; // x-coord of top end of the bottom BC stroke
				float bCTopY = coords[1]; // y-coord of top end of the bottom BC stroke
				float bCBottomY = coords[3]; // y-coord of bottom end of the bottom BC stroke

				float height = Math.abs(bCBottomY - hLineLeftY);
				double gapThreshold = 0.25 * height;

				// Check to see if the following gaps are less than their respective thresholds
				// i.   Gap between the left end of the HLINE and the top end of the VLINE
				// ii.  Gap between the bottom end of the HLINE and the top of the BC
				matchedP =
					(PenUtil.distanceBetween2Points(hLineLeftX, hLineLeftY, vLineTopX, vLineTopY) < gapThreshold) &
					(PenUtil.distanceBetween2Points(vLineBottomX, vLineBottomY, bCTopX, bCTopY) < gapThreshold);
			}
		}
		return matchedP;
	}  // End of checkFor5()

	// 7 has two strokes - a HLINE above a FSLASH.
	private boolean checkFor7() {
		boolean matchedP = false;
		int numOfSegments = mPenSegments.size();

		// 7 has only two pen stroke characters
		if (numOfSegments == 2) {
			int hLineIndex = -1;
			int fSlashIndex = -1;

			for (int i = 0; i < numOfSegments; i++) {
				switch (mPenSegments.elementAt(i).mPenSegmentCharacter) {
				case PenSegment.HLINE_CHAR:
					hLineIndex = i;
					break;
				case PenSegment.FSLASH_CHAR:
					fSlashIndex = i;
					break;
				default:
					break;
				}
			}

			// Check to make sure that the two component strokes for '7' are there, i.e.,
			// hLineIndex and fSlashIndex both all not negative
			if ((hLineIndex >= 0) & (fSlashIndex >= 0)) {
				// Get the left, right x,y coords of the HLINE stroke
				float coords[] = getLeftRightCoordsOfSegment(mPenSegments.elementAt(hLineIndex));
				//				float hLineLeftX = coords[0]; // x-coord of left end of the HLINE stroke
				//				float hLineLeftY = coords[1]; // y-coord of left end of the HLINE stroke
				float hLineRightX = coords[2]; // x-coord of right end of the HLINE stroke
				float hLineRightY = coords[3]; // y-coord of right end of the HLINE stroke

				// Get the top, bottom x,y coords of the VLINE stroke
				coords = getTopBottomCoordsOfSegment(mPenSegments.elementAt(fSlashIndex));
				float fSlashTopX = coords[0]; // x-coord of top end of the VLINE stroke
				float fSlashTopY = coords[1]; // y-coord of top end of the VLINE stroke
				//				float fSlashBottomX = coords[2]; // x-coord of bottom end of the VLINE stroke
				float fSlashBottomY = coords[3]; // y-coord of bottom end of the VLINE stroke


				float height = Math.abs(fSlashBottomY - fSlashTopY);
				double gapThreshold = 0.25 * height;

				// Check to see if the following gaps are less than their respective thresholds
				// i.   Gap between the left end of the HLINE and the top end of the VLINE
				// ii.  Gap between the bottom end of the HLINE and the top of the BC
				matchedP =
					(PenUtil.distanceBetween2Points(hLineRightX, hLineRightY, fSlashTopX, fSlashTopY) < gapThreshold);
			}
		}
		return matchedP;
	}  // End of checkFor7()

	// '9' has a VLINE and a FC on the right joining the VLINE at the top and middle
	private boolean checkFor9() {
		boolean matchedP = false;

		double vLineTopYMin = -1.0;
		double vLineTopYMax = .4 * Skiggle.DEFAULT_WRITE_PAD_HEIGHT;

		// Check to see if the following are true
		// i.   VLINE and FC form a q or 9 shape
		// ii.  The top of the VLINE is less than .4 the height of the writing area from the top edge of the writing area
		matchedP =
			checkFor9OrSmallQ((float)vLineTopYMin, (float)vLineTopYMax);

		return matchedP;
	}  // End of checkFor9()

	/******************
	 *                *
	 * Letters A to Z *
	 *                *
	 ******************/

	// 'A' is made up of a forward slash ('/'), a back slash ('\'), and a horizontal line ('-')
	private boolean checkForCapitalA(){
		boolean matchedP = false;
		int numOfSegments = mPenSegments.size();

		if (numOfSegments == 3) {
			int fSlashIndex = -1;
			int bSlashIndex = -1;
			int hLineIndex = -1;
			for (int i = 0; i < numOfSegments; i++) {
				// 'A' has only three pen stroke characters
				switch (mPenSegments.elementAt(i).mPenSegmentCharacter) {
				case PenSegment.FSLASH_CHAR:
					fSlashIndex = i;
					break;
				case PenSegment.BSLASH_CHAR:
					bSlashIndex = i;
					break;
				case PenSegment.HLINE_CHAR:
					hLineIndex = i;
					break;
				default:
					break;
				}
			}

			// Check to make sure that the three component strokes for 'A' are there, i.e.,
			// fSlashIndex, bSlashIndex, and hLineIndex are all not negative
			if ((fSlashIndex >= 0) & (bSlashIndex >= 0) & (hLineIndex >= 0)) {

				// Get x,y of top and bottom of FSLASH
				float coords[] = getTopBottomCoordsOfSegment(mPenSegments.elementAt(fSlashIndex));
				float fSlashTopX = coords[0]; // x-coord of start point of FSLASH stroke
				float fSlashTopY = coords[1]; // y-coord of start point of FSLASH stroke
				float fSlashBottomX = coords[2]; // x-coord of end point of FSLASH stroke
				float fSlashBottomY = coords[3]; // y-coord of end point of FSLASH stroke

				// Get x,y of top and bottom of BSLASH
				coords = getTopBottomCoordsOfSegment(mPenSegments.elementAt(bSlashIndex));
				float bSlashTopX = coords[0]; // x-coord of start point of BSLASH stroke
				float bSlashTopY = coords[1]; // y-coord of start point of BSLASH stroke
				float bSlashBottomX = coords[2]; // x-coord of end point of BSLASH stroke
				float bSlashBottomY = coords[3]; // y-coord of end point of BSLASH stroke

				// Check to see if the mid-point of horizontal line (dash) is between
				// .25 and .75 of the maximum height of BSLASH and FSLASH
				// and between .25 and .75 of the maximum width of BSLASH and FSLASH
				float maxY = Math.max(fSlashBottomY, bSlashBottomY); // y-coord values increase downwards
				float minY = Math.min(fSlashTopY, bSlashTopY);
				float height = maxY - minY;

				float maxX = Math.max(Math.max(fSlashTopX, fSlashBottomX), Math.max(bSlashTopX, bSlashBottomX));
				float minX = Math.min(Math.min(fSlashTopX, fSlashBottomX), Math.min(bSlashTopX, bSlashBottomX));
				float width = maxX - minX;

				float hLineAvgX = (mPenSegments.elementAt(hLineIndex).mPosStart[0] + mPenSegments.elementAt(hLineIndex).mPosEnd[0])/2;
				float hLineAvgY = (mPenSegments.elementAt(hLineIndex).mPosStart[1] + mPenSegments.elementAt(hLineIndex).mPosEnd[1])/2;

				float hLineAvgXDist = hLineAvgX - minX;
				float hLineAvgYDist = hLineAvgY - minY;

				// Check for caret (or '/' and '\') forming the top of A and position of horizontal line (dash) wrt to caret
				matchedP = 
					gapCheckForCaretShape(mPenSegments.elementAt(fSlashIndex), mPenSegments.elementAt(bSlashIndex)) &
					isBetweenThresholds(hLineAvgXDist, .25 * width, .75 * width) &
					isBetweenThresholds(hLineAvgYDist, .25 * height, .75 * height);
			}
		}
		return matchedP;
	}  // End of checkForCapitalA()

	// 'B' has three strokes - a VLINE and two BC (backward C or ')') strokes on the right.
	// The top BC joins the VLINE at the top and around mid-point and the bottom BC joins the VLINE
	// at around the mid-point and the bottom
	private boolean checkForCapitalB() {
		boolean matchedP = false;
		int numOfSegments = mPenSegments.size();

		if (numOfSegments == 3) {
			int topBCIndex = -1;
			int bottomBCIndex = -1;
			int vLineIndex = -1;

			for (int i = 0; i < numOfSegments; i++) {
				// 'B' has only three pen stroke characters
				switch (mPenSegments.elementAt(i).mPenSegmentCharacter) {
				case PenSegment.BC_CHAR:
					if (topBCIndex < 0) topBCIndex = i;
					else bottomBCIndex = i;
					break;
				case PenSegment.VLINE_CHAR:
					vLineIndex = i;
					break;
				default:
					break;
				}
			}

			// Check to make sure that the three component strokes for 'B' are there, i.e.,
			// topBCIndex, bottomBCIndex, and vLineIndex are all not negative
			if ((topBCIndex >= 0) & (bottomBCIndex >= 0) & (vLineIndex >= 0)) {
				// Get the top and bottom BC strokes
				PenSegment topBottomBCSegments[] = 
					order2PenSegmentsIntoTopBottom(mPenSegments.elementAt(topBCIndex), mPenSegments.elementAt(bottomBCIndex));

				// Get x,y coords of the top BC stroke
				float coords[] = getTopBottomCoordsOfSegment(topBottomBCSegments[0]);
				float topBCTopX = coords[0]; // x-coord of top end of the top BC stroke
				float topBCTopY = coords[1]; // y-coord of top end of the top BC stroke
				float topBCBottomX = coords[2]; // x-coord of bottom end of the top BC stroke
				float topBCBottomY = coords[3]; // y-coord of bottom end of the top BC stroke

				// Get x,y coords of the bottom BC stroke
				coords = getTopBottomCoordsOfSegment(topBottomBCSegments[1]);
				float bottomBCTopX = coords[0]; // x-coord of top end of the bottom BC stroke
				float bottomBCTopY = coords[1]; // y-coord of top end of the bottom BC stroke
				float bottomBCBottomX = coords[2]; // x-coord of bottom end of the bottom BC stroke
				float bottomBCBottomY = coords[3]; // y-coord of bottom end of the bottom BC stroke

				// Get x,y of the top and bottom of VLINE
				coords = getTopBottomCoordsOfSegment(mPenSegments.elementAt(vLineIndex));
				float vLineTopX = coords[0]; // x-coord of top end of the VLINE stroke
				float vLineTopY = coords[1]; // y-coord of top end of the VLINE stroke
				float vLineBottomX = coords[2]; // x-coord of bottom end of the VLINE stroke
				float vLineBottomY = coords[3]; // y-coord of bottom end of the VLINE stroke
				float vLineMidX = (vLineTopX + vLineBottomX)/2; // average of x-coord (mid-point) of the VLINE stroke
				float vLineMidY = (vLineTopY + vLineBottomY)/2; // average of y-coord (mid-point) of the VLINE stroke

				float vLineHeight = Math.abs(vLineBottomY - vLineTopY);
				double gapThreshold = 0.25 * vLineHeight;

				// Check to see if the following gaps are less their respective thresholds
				// i.   The gap between the top ends VLINE and top BC
				// ii.  The gap between the bottom of the top BC and the mid-point of VLINE
				// iii. The gap between the top of the bottom BC and the mid-point of VLINE
				// iv.  The gap between the bottom ends VLINE and bottom BC
				matchedP =
					(PenUtil.distanceBetween2Points(vLineTopX, vLineTopY, topBCTopX, topBCTopY) < gapThreshold) &
					(PenUtil.distanceBetween2Points(vLineMidX, vLineMidY, topBCBottomX, topBCBottomY) < 2 * gapThreshold) &
					(PenUtil.distanceBetween2Points(vLineMidX, vLineMidY, bottomBCTopX, bottomBCTopY) < 2 * gapThreshold) &
					(PenUtil.distanceBetween2Points(vLineBottomX, vLineBottomY, bottomBCBottomX, bottomBCBottomY) < gapThreshold);
			}
		}
		return matchedP;
	}  // End of checkForCapitalB()

	// 'C' is checked using the checkForCShape() and the isSmallLetter() methods

	// 'D' has a VLINE and a BC (backward C or ')') on the right joining the VLINE at the top and bottom
	private boolean checkForCapitalD() {
		boolean matchedP = false;
		int numOfSegments = mPenSegments.size();

		if (numOfSegments == 2) {
			int bCIndex = -1;
			int vLineIndex = -1;

			for (int i = 0; i < numOfSegments; i++) {
				// 'D' has only two pen stroke characters
				switch (mPenSegments.elementAt(i).mPenSegmentCharacter) {
				case PenSegment.BC_CHAR:
					bCIndex = i;
					break;
				case PenSegment.VLINE_CHAR:
					vLineIndex = i;
					break;
				default:
					break;
				}
			}

			// Check to make sure that the two component strokes for 'D' are there, i.e.,
			// bCIndex and vLineIndex are both not negative
			if ((bCIndex >= 0) & (vLineIndex >= 0)) {
				// Get the x,y of the top and bottom of BC
				float coords[] = getTopBottomCoordsOfSegment(mPenSegments.elementAt(bCIndex));
				float bCTopX = coords[0]; // x-coord of top end of the BC stroke
				float bCTopY = coords[1]; // y-coord of top end of the BC stroke
				float bCBottomX = coords[2]; // x-coord of bottom end of the BC stroke
				float bCBottomY = coords[3]; // y-coord of bottom end of the BC stroke

				// Get x,y of the top and bottom of VLINE
				coords = getTopBottomCoordsOfSegment(mPenSegments.elementAt(vLineIndex));
				float vLineTopX = coords[0]; // x-coord of top end of the VLINE stroke
				float vLineTopY = coords[1]; // y-coord of top end of the VLINE stroke
				float vLineBottomX = coords[2]; // x-coord of bottom end of the VLINE stroke
				float vLineBottomY = coords[3]; // y-coord of bottom end of the VLINE stroke

				float vLineHeight = Math.abs(vLineBottomY - vLineTopY);
				double gapThreshold = 0.25 * vLineHeight;

				// Check to see if the gap between the top ends VLINE and BC that between
				// the bottom ends of VLINE and BC are less than the gap threshold
				matchedP =
					(PenUtil.distanceBetween2Points(vLineTopX, vLineTopY, bCTopX, bCTopY) < gapThreshold) &
					(PenUtil.distanceBetween2Points(vLineBottomX, vLineBottomY, bCBottomX, bCBottomY) < gapThreshold);
			}
		}

		return matchedP;
	}  // End of checkForCapitalD()

	// Check for 'E'
	private boolean checkForCapitalE() {
		boolean matchedP = false;
		int numOfSegments = mPenSegments.size();

		if (numOfSegments == 4) {
			int topHLineIndex = -1;
			int midHLineIndex = -1;
			int bottomHLineIndex = -1;
			int vLineIndex = -1;

			for (int i = 0; i < numOfSegments; i++) {
				// 'E' has only four pen stroke characters - 1 long vertical and 3 short horizontal segments
				switch (mPenSegments.elementAt(i).mPenSegmentCharacter) {
				case PenSegment.HLINE_CHAR:
					if (topHLineIndex == -1)
						topHLineIndex = i;
					else if (midHLineIndex == -1)
						midHLineIndex = i;
					else
						bottomHLineIndex = i;
					break;
				case PenSegment.VLINE_CHAR:
					vLineIndex = i;
					break;
				default:
					break;
				}
			}

			// Check to make sure that the four component strokes for 'E' are there, i.e.,
			// topHLineIndex, midHLineIndex, bottomHLineIndex, and vLineIndex are all not negative
			if ((topHLineIndex >= 0) & (midHLineIndex >= 0) & (bottomHLineIndex >= 0) & (vLineIndex >= 0)) {
				float coords[] = getLeftRightCoordsOfSegment(mPenSegments.elementAt(topHLineIndex));
				float topHLineLeftX = coords[0]; // x-coord of left point of the top HLINE stroke
				float topHLineLeftY = coords[1]; // y-coord of left point of the top HLINE stroke

				coords = getLeftRightCoordsOfSegment(mPenSegments.elementAt(midHLineIndex));
				float midHLineLeftX = coords[0]; // x-coord of left point of the mid HLINE stroke
				float midHLineLeftY = coords[1]; // y-coord of left point of the mid HLINE stroke

				coords = getLeftRightCoordsOfSegment(mPenSegments.elementAt(bottomHLineIndex));
				float bottomHLineLeftX = coords[0]; // x-coord of left point of the bottom HLINE stroke
				float bottomHLineLeftY = coords[1]; // y-coord of left point of the bottom HLINE stroke

				// Check to see which of the two HLINE is the top one (the one with lower y-coord value)
				if (midHLineLeftY < topHLineLeftY) {
					// swap x,y coords of top and mid HLINE's
					float temp = topHLineLeftX;
					topHLineLeftX = midHLineLeftX;
					midHLineLeftX = temp;

					temp = topHLineLeftY;
					topHLineLeftY = midHLineLeftY;
					midHLineLeftY = temp;
				}

				// Check if the third HLINE is above the top HLINE, between the top and mid HLINE's or below mid HLINE
				// revise topHLineIndex, midHLineIndex, and bottomHLineIndex accordingly
				if (bottomHLineLeftY < topHLineLeftY) {
					// Rotate the x,y coords of the top HLINE down to the mid HLINE, from the mid HLINE
					// down to the bottom HLINE, and from the bottom HLINE back to the top HLINE.
					float temp = midHLineLeftX;
					midHLineLeftX = topHLineLeftX;
					topHLineLeftX = bottomHLineLeftX;
					bottomHLineLeftX = temp;

					temp = midHLineLeftY;
					midHLineLeftY = topHLineLeftY;
					topHLineLeftY = bottomHLineLeftY;
					bottomHLineLeftY = temp;
				}
				else if (bottomHLineLeftY < midHLineLeftY) {
					// Swap the x,y coords of the mid and bottom HLINE's
					float temp = midHLineLeftX;
					midHLineLeftX = bottomHLineLeftX;
					bottomHLineLeftX = temp;

					temp = midHLineLeftY;
					midHLineLeftY = bottomHLineLeftY;
					bottomHLineLeftY = temp;
				}	

				// Get x,y of the top and bottom of VLINE
				coords = getTopBottomCoordsOfSegment(mPenSegments.elementAt(vLineIndex));

				float vLineTopX = coords[0]; // x-coord of top end of the VLINE stroke
				float vLineTopY = coords[1]; // y-coord of top end of the VLINE stroke
				float vLineBottomX = coords[2]; // x-coord of bottom end of the VLINE stroke
				float vLineBottomY = coords[3]; // y-coord of bottom end of the VLINE stroke
				float vLineMidX = (vLineTopX + vLineBottomX)/2; // x-coord of the mid point of the VLINE stroke
				float vLineMidY = (vLineTopY + vLineBottomY)/2; // x-coord of the mid point of the VLINE stroke

				float vLineHeight = Math.abs(vLineBottomY - vLineTopY);
				double gapThreshold = 0.25 * vLineHeight;

				// Check to see if:
				// i.   The gap between the top of the VLINE and left of the top HLINE is less than the gap threshold
				// ii.  The gap between the middle of the VLINE and left of middle HLINE is less than the gap threshold
				// iii. The gap between the top of the VLINE and left of the bottom HLINE is less than the gap threshold

				matchedP =
					(PenUtil.distanceBetween2Points(vLineTopX, vLineTopY, topHLineLeftX, topHLineLeftY) < gapThreshold) &
					(PenUtil.distanceBetween2Points(vLineBottomX, vLineBottomY, bottomHLineLeftX, bottomHLineLeftY) < gapThreshold) &
					(PenUtil.distanceBetween2Points(vLineMidX, vLineMidY, midHLineLeftX, midHLineLeftY) < gapThreshold);
			}
		}
		return matchedP;
	}  // End of checkForCapitalE()

	// Check for 'F'
	private boolean checkForCapitalF() {
		boolean matchedP = false;
		int numOfSegments = mPenSegments.size();

		if (numOfSegments == 3) {
			int topHLineIndex = -1;
			int midHLineIndex = -1;
			int vLineIndex = -1;

			for (int i = 0; i < numOfSegments; i++) {
				// 'F' has only three pen stroke characters
				switch (mPenSegments.elementAt(i).mPenSegmentCharacter) {
				case PenSegment.HLINE_CHAR:
					if (topHLineIndex == -1) {
						topHLineIndex = i;
					}
					else
						midHLineIndex = i;
					break;
				case PenSegment.VLINE_CHAR:
					vLineIndex = i;
					break;
				default:
					break;
				}
			}

			// Check to make sure that the three component strokes for 'F' are there, i.e.,
			// topHLineIndex, midHLineIndex, and vLineIndex are all not negative
			if ((topHLineIndex >= 0) & (midHLineIndex >= 0) & (vLineIndex >= 0)) {
				// Check to see which of the two HLINE is the top one (the one with lower y-coord value)
				if (mPenSegments.elementAt(midHLineIndex).mPosStart[1] < mPenSegments.elementAt(topHLineIndex).mPosStart[1]) {
					int temp = topHLineIndex;
					topHLineIndex = midHLineIndex;
					midHLineIndex = temp;
				}

				float coords[] = getLeftRightCoordsOfSegment(mPenSegments.elementAt(topHLineIndex));
				float topHLineLeftX = coords[0]; // x-coord of left point of the top HLINE stroke
				float topHLineLeftY = coords[1]; // y-coord of left point of the top HLINE stroke

				coords = getLeftRightCoordsOfSegment(mPenSegments.elementAt(midHLineIndex));
				float midHLineLeftY = coords[1]; // y-coord of left point of the mid HLINE stroke

				// Get x,y of the top and bottom of VLINE
				coords = getTopBottomCoordsOfSegment(mPenSegments.elementAt(vLineIndex));

				float vLineTopX = coords[0]; // x-coord of top end of the VLINE stroke
				float vLineTopY = coords[1]; // y-coord of top end of the VLINE stroke
				float vLineBottomY = coords[3]; // y-coord of bottom end of the VLINE stroke

				float vLineHeight = Math.abs(vLineBottomY - vLineTopY);
				float gapBetweenTopAndMidHLine = Math.abs(topHLineLeftY - midHLineLeftY);
				double gapThreshold = 0.25 * vLineHeight;

				// Check to see if the gaps between the leftmost end the top HLINE and top of the VLINE
				// is less than the gap threshold and the gap between the top and mid HLINE's
				// are less than the gap threshold
				matchedP =
					(isBetweenThresholds(gapBetweenTopAndMidHLine, gapThreshold, .75 * vLineHeight )) &
					(PenUtil.distanceBetween2Points(vLineTopX, vLineTopY, topHLineLeftX, topHLineLeftY) < gapThreshold);
			}
		}
		return matchedP;
	}  // End of checkForCapitalF()

	// 'G' has a FC (forward C or '('), a HLINE and a VLINE.  The HLINE touches the top of the VLINE which
	// in turn is touched by the lower right end of the FC.
	private boolean checkForCapitalG() {
		boolean matchedP = false;
		int numOfSegments = mPenSegments.size();
		// G can have 3 strokes - FC and HLINE and an optional VLINE
		if (numOfSegments == 3) {
			int fCIndex = -1;
			int hLineIndex = -1;
			int vLineIndex = -1;

			for (int i = 0; i < numOfSegments; i++) {
				// 'G' has only two pen stroke characters.
				switch (mPenSegments.elementAt(i).mPenSegmentCharacter) {
				case PenSegment.FC_CHAR:
					fCIndex = i;
					break;
				case PenSegment.HLINE_CHAR:
					hLineIndex = i;
					break;
				case PenSegment.VLINE_CHAR:
					vLineIndex = i;
					break;
				default:
					break;
				}
			}

			// Check to make sure that the three component strokes for 'G' are there, i.e.,
			// fCIndex, vLineIndex, and hLineIndex are all not negative.
			if ((fCIndex >= 0) & (vLineIndex >= 0) & (hLineIndex >= 0)) {
				// Get the x,y of the top and bottom of FC.
				float coords[] = getTopBottomCoordsOfSegment(mPenSegments.elementAt(fCIndex));
				float fCBottomX = coords[2]; // x-coord of bottom end of the BC stroke.
				float fCBottomY = coords[3]; // y-coord of bottom end of the BC stroke.

				// Get x,y of the top and bottom of HLINE
				coords = getLeftRightCoordsOfSegment(mPenSegments.elementAt(hLineIndex));
				float hLineLeftX = coords[0]; // x-coord of left end of the HLINE stroke.
				float hLineLeftY = coords[1]; // y-coord of left end of the HLINE stroke.
				float hLineRightX = coords[2]; // x-coord of right end of the HLINE stroke.
				float hLineRightY = coords[3]; // y-coord of right end of the HLINE stroke.
				float hLineMidX = (hLineLeftX + hLineRightX)/2; // x-coord of the mid-point of the HLINE stroke.
				float hLineMidY = (hLineLeftY + hLineRightY)/2; // x-coord of the mid-point of the HLINE stroke.				
				float hLineWidth = Math.abs(hLineLeftX - hLineRightX);

				// Get x,y of the top and bottom of VLINE
				coords = getTopBottomCoordsOfSegment(mPenSegments.elementAt(vLineIndex));
				float vLineTopX = coords[0]; // x-coord of top end of the VLINE stroke.
				float vLineTopY = coords[1]; // y-coord of top end of the VLINE stroke.
				float vLineBottomX = coords[2]; // x-coord of bottom end of the VLINE stroke.
				float vLineBottomY = coords[3]; // y-coord of bottom end of the VLINE stroke.
				float vLineMidX = (vLineTopX + vLineBottomX)/2; // x-coord of the mid-point of the VLINE stroke.
				float vLineMidY = (vLineTopY + vLineBottomY)/2; // x-coord of the mid-point of the VLINE stroke.				
				float vLineHeight = Math.abs(vLineTopY - vLineBottomY);
				
				float refLineLength = Math.min(hLineWidth, vLineHeight);
				
				// Check to see if the following gaps are less than their respective gap thresholds.
				matchedP =
					// i.  Gap between the y-coord of the mid-point of HLINE and the y-coord of the top end of VLINE.
					(Math.abs(hLineMidY - vLineTopY) <= .25 * refLineLength) &
					
					// ii. Gap between the x-coord of the mid-point of HLINE and the x-coord of the top end of FC
					Math.abs(hLineMidX - vLineTopX) <= .5 * refLineLength &
					
					// iii.  Gap between the y-coord of the mid-point of VLINE and the y-coord of the bottom end of FC.
					(Math.abs(vLineMidY - fCBottomY) <= .5 * refLineLength) &
					
					// ii. Gap between the x-coord of the mid-point of VLINE and the x-coord of the bottom end of FC
					Math.abs(vLineMidX - fCBottomX) <= .25 * refLineLength;


			}
		}

		return matchedP;
	}  // End of checkForCapitalG()

	// 'H' has a HLINE ('-') between two VLINE's ('|')
	private boolean checkForCapitalH() {
		boolean matchedP = false;
		int numOfSegments = mPenSegments.size();

		if (numOfSegments == 3) {
			int leftVLineIndex = -1, hLineIndex = -1, rightVLineIndex = -1;
			for (int i = 0; i < numOfSegments; i++) {
				// 'H' has only three pen stroke characters
				switch (mPenSegments.elementAt(i).mPenSegmentCharacter) {
				case PenSegment.VLINE_CHAR:
					if (leftVLineIndex < 0)
						leftVLineIndex = i;
					else
						rightVLineIndex = i;
					break;
				case PenSegment.HLINE_CHAR:
					hLineIndex = i;
					break;
				default:
					break;
				}
			}

			// Check to make sure that the three component strokes for 'H' are there, i.e.,
			// leftVLineIndex, hLineIndex, and rightVLineIndex are all not negative
			if ((leftVLineIndex >= 0) & (hLineIndex >= 0) & (rightVLineIndex >= 0)) {
				// Get the left and left VLINE strokes of 'I'
				PenSegment leftRightVLineSegments[] = 
					order2PenSegmentsIntoLeftRight(mPenSegments.elementAt(leftVLineIndex),
							mPenSegments.elementAt(rightVLineIndex));
				PenSegment leftVLineSegment = leftRightVLineSegments[0];
				PenSegment rightVLineSegment = leftRightVLineSegments[1];	
				// Get the average x,y coords of each of the left and right VLINE's
				float leftVLineMidX = (leftVLineSegment.mPosStart[0] + leftVLineSegment.mPosEnd[0])/2;
				float leftVLineMidY = (leftVLineSegment.mPosStart[1] + leftVLineSegment.mPosEnd[1])/2;				
				float rightVLineMidX = (rightVLineSegment.mPosStart[0] + rightVLineSegment.mPosEnd[0])/2;
				float rightVLineMidY = (rightVLineSegment.mPosStart[1] + rightVLineSegment.mPosEnd[1])/2;

				// Get x,y of the left and right of HLINE
				float coords[] = getLeftRightCoordsOfSegment(mPenSegments.elementAt(hLineIndex));

				float hLineLeftX = coords[0]; // x-coord of left point of the HLINE stroke
				float hLineLeftY = coords[1]; // y-coord of left point of the HLINE stroke
				float hLineRightX = coords[2]; // x-coord of bottom end of the HLINE stroke
				float hLineRightY = coords[3]; // y-coord of bottom end of the HLINE stroke

				float hLineWidth = Math.abs(hLineLeftX - hLineRightX);
				double gapThreshold = 0.25 * hLineWidth;

				// Check to see if the gaps between the mid-points of the left and right VLINE's and
				// the left and right, respectively, of HLINE are less than the gap threshold
				matchedP =
					(PenUtil.distanceBetween2Points(hLineLeftX, hLineLeftY, leftVLineMidX, leftVLineMidY) < gapThreshold) &
					(PenUtil.distanceBetween2Points(hLineRightX, hLineRightY, rightVLineMidX, rightVLineMidY) < gapThreshold);
			}
		}
		return matchedP;
	}  // End of checkForCapitalH()

	// 'I' has an HLINE ('-') at the top and bottom of a VLINE ('|')
	private boolean checkForCapitalI() {
		boolean matchedP = false;
		int numOfSegments = mPenSegments.size();

		if (numOfSegments == 3) {
			int topHLineIndex = -1;
			int vLineIndex = -1;
			int bottomHLineIndex = -1;
			for (int i = 0; i < numOfSegments; i++) {
				// 'I' has only three pen stroke characters
				switch (mPenSegments.elementAt(i).mPenSegmentCharacter) {
				case PenSegment.HLINE_CHAR:
					if (topHLineIndex < 0)
						topHLineIndex = i;
					else
						bottomHLineIndex = i;
					break;
				case PenSegment.VLINE_CHAR:
					vLineIndex = i;
					break;
				default:
					break;
				}
			}

			// Check to make sure that the three component strokes for 'I' are there, i.e.,
			// topHLineIndex, vLineIndex, and bottomHLineIndex are all not negative
			if ((topHLineIndex >= 0) & (vLineIndex >= 0) & (bottomHLineIndex >= 0)) {
				// Get the top and bottom HLINE strokes of 'I'
				PenSegment topBottomHLineSegments[] = 
					order2PenSegmentsIntoTopBottom(mPenSegments.elementAt(topHLineIndex),
							mPenSegments.elementAt(bottomHLineIndex));
				float topHLineMidX = (topBottomHLineSegments[0].mPosStart[0] + topBottomHLineSegments[0].mPosEnd[0])/2;
				float topHLineMidY = (topBottomHLineSegments[0].mPosStart[1] + topBottomHLineSegments[0].mPosEnd[1])/2;				
				float bottomHLineMidX = (topBottomHLineSegments[1].mPosStart[0] + topBottomHLineSegments[1].mPosEnd[0])/2;
				float bottomHLineMidY = (topBottomHLineSegments[1].mPosStart[1] + topBottomHLineSegments[1].mPosEnd[1])/2;

				// Get x,y of top and bottom of VLINE
				float coords[] = getTopBottomCoordsOfSegment(mPenSegments.elementAt(vLineIndex));

				float vLineTopX = coords[0]; // x-coord of top end of the VLINE stroke
				float vLineTopY = coords[1]; // y-coord of top end of the VLINE stroke
				float vLineBottomX = coords[2]; // x-coord of bottom end of the VLINE stroke
				float vLineBottomY = coords[3]; // y-coord of bottom end of the VLINE stroke

				float vLineHeight = Math.abs(vLineBottomY - vLineTopY);
				double gapThreshold = 0.1 * vLineHeight;

				// Check to see if the gaps between the mid-points of the top and bottom HLINE's 
				// and top and bottom, respectively, of VLINE are less than the gap threshold
				matchedP =
					(PenUtil.distanceBetween2Points(vLineTopX, vLineTopY, topHLineMidX, topHLineMidY) < gapThreshold) &
					(PenUtil.distanceBetween2Points(vLineBottomX, vLineBottomY, bottomHLineMidX, bottomHLineMidY) < gapThreshold);
			}
		}
		return matchedP;
	}  // End of checkForCapitalI()

	// 'J' has an HLINE ('-') at the top of a VLINE ('|') and a U below the VLINE
	private boolean checkForCapitalJ() {
		boolean matchedP = false;
		int numOfSegments = mPenSegments.size();

		if (numOfSegments == 3) {
			int topHLineIndex = -1;
			int vLineIndex = -1;
			int bottomUIndex = -1;
			for (int i = 0; i < numOfSegments; i++) {
				// 'J' has only three pen stroke characters
				switch (mPenSegments.elementAt(i).mPenSegmentCharacter) {
				case PenSegment.HLINE_CHAR:
					topHLineIndex = i;
					break;
				case PenSegment.VLINE_CHAR:
					vLineIndex = i;
					break;
				case PenSegment.U_CHAR:
					bottomUIndex = i;
					break;
				default:
					break;
				}
			}

			// Check to make sure that the three component strokes for 'J' are there, i.e.,
			// topHLineIndex, vLineIndex, and bottomUIndex are all not negative
			if ((topHLineIndex >= 0) & (vLineIndex >= 0) & (bottomUIndex >= 0)) {
				// Get the top HLINE stroke of 'J'
				PenSegment topHLineSegment = mPenSegments.elementAt(topHLineIndex);
				float topHLineMidX = (topHLineSegment.mPosStart[0] + topHLineSegment.mPosEnd[0])/2;
				float topHLineMidY = (topHLineSegment.mPosStart[1] + topHLineSegment.mPosEnd[1])/2;				

				// Get x,y of top and bottom of VLINE
				float coords[] = getTopBottomCoordsOfSegment(mPenSegments.elementAt(vLineIndex));

				float vLineTopX = coords[0]; // x-coord of top end of the VLINE stroke
				float vLineTopY = coords[1]; // y-coord of top end of the VLINE stroke
				float vLineBottomX = coords[2]; // x-coord of bottom end of the VLINE stroke
				float vLineBottomY = coords[3]; // y-coord of bottom end of the VLINE stroke

				float vLineHeight = Math.abs(vLineBottomY - vLineTopY);
				double gapThreshold = 0.1 * vLineHeight;

				// Get the left, right x,y coordinates of the bottom U stroke of 'J'
				coords = getLeftRightCoordsOfSegment(mPenSegments.elementAt(bottomUIndex));
				float rightUX = coords[2];
				float rightUY = coords[3];				

				// Check to see if the gaps between the mid-points of the top and bottom HLINE's 
				// and top and bottom, respectively, of VLINE are less than the gap threshold
				matchedP =
					(PenUtil.distanceBetween2Points(vLineTopX, vLineTopY, topHLineMidX, topHLineMidY) < gapThreshold) &
					(PenUtil.distanceBetween2Points(vLineBottomX, vLineBottomY, rightUX, rightUY) < gapThreshold);
			}
		}
		return matchedP;
	}  // End of checkForCapitalJ()


	// 'K' is checked using checkForCapitalOrSmallK() which returns a 'k', 'K', or '\0' and not a boolean

	// 'L' has a VLINE and an HLINE at the bottom
	private boolean checkForCapitalL() {
		boolean matchedP = false;
		int numOfSegments = mPenSegments.size();

		if (numOfSegments == 2) {
			int bottomHLineIndex = -1;
			int vLineIndex = -1;

			for (int i = 0; i < numOfSegments; i++) {
				// 'L' has only two pen stroke characters
				switch (mPenSegments.elementAt(i).mPenSegmentCharacter) {
				case PenSegment.HLINE_CHAR:
					bottomHLineIndex = i;
					break;
				case PenSegment.VLINE_CHAR:
					vLineIndex = i;
					break;
				default:
					break;
				}
			}

			// Check to make sure that the two component strokes for 'L' are there, i.e.,
			// bottomHLineIndex and vLineIndex are both not negative
			if ((bottomHLineIndex >= 0) & (vLineIndex >= 0)) {
				float coords[] = getLeftRightCoordsOfSegment(mPenSegments.elementAt(bottomHLineIndex));
				float bottomHLineLeftX = coords[0]; // x-coord of left point of the bottom HLINE stroke
				float bottomHLineLeftY = coords[1]; // y-coord of left point of the bottom HLINE stroke

				// Get x,y of the top and bottom of VLINE
				coords = getTopBottomCoordsOfSegment(mPenSegments.elementAt(vLineIndex));

				float vLineTopY = coords[1]; // y-coord of top end of the VLINE stroke
				float vLineBottomX = coords[2]; // x-coord of bottom end of the VLINE stroke
				float vLineBottomY = coords[3]; // y-coord of bottom end of the VLINE stroke

				float vLineHeight = Math.abs(vLineBottomY - vLineTopY);
				double gapThreshold = 0.25 * vLineHeight;

				// Check to see if the gaps between the leftmost end the bottom HLINE and bottom of the VLINE
				// is less than the gap threshold
				matchedP =
					(PenUtil.distanceBetween2Points(vLineBottomX, vLineBottomY, bottomHLineLeftX, bottomHLineLeftY) < gapThreshold);
			}
		}

		return matchedP;
	}  // End of checkForCapitalL()

	// 'M' has a BSLASH ('\') and a FSLASH ('/') between two VLINE's ('|')
	private boolean checkForCapitalM() {
		boolean matchedP = false;
		int numOfSegments = mPenSegments.size();

		if (numOfSegments == 4) {
			int leftVLineIndex = -1;
			int bSlashIndex = -1;
			int fSlashIndex = -1;
			int rightVLineIndex = -1;
			for (int i = 0; i < numOfSegments; i++) {
				// 'M' has only four pen stroke characters
				switch (mPenSegments.elementAt(i).mPenSegmentCharacter) {
				case PenSegment.VLINE_CHAR:
					if (leftVLineIndex < 0)
						leftVLineIndex = i;
					else
						rightVLineIndex = i;
					break;
				case PenSegment.BSLASH_CHAR:
					bSlashIndex = i;
					break;
				case PenSegment.FSLASH_CHAR:
					fSlashIndex = i;
					break;
				default:
					break;
				}
			}

			// Check to make sure that the four component strokes for 'M' are there, i.e.,
			// leftVLineIndex, bSlashIndex, fSlashIndex, and rightVLineIndex are all not negative
			if ((leftVLineIndex >= 0) & (bSlashIndex >= 0) & (fSlashIndex >= 0) & (rightVLineIndex >= 0)) {
				// Get the left and left VLINE strokes of 'M'
				PenSegment leftRightVLineSegments[] = 
					order2PenSegmentsIntoLeftRight(mPenSegments.elementAt(leftVLineIndex),
							mPenSegments.elementAt(rightVLineIndex));

				PenSegment leftVLineSegment = leftRightVLineSegments[0];
				PenSegment rightVLineSegment = leftRightVLineSegments[1];				
				// Get x,y of the top of the left VLINE
				float coords[] = getTopBottomCoordsOfSegment(leftVLineSegment);
				float leftVLineTopX = coords[0]; // x-coord of top of the left VLINE
				float leftVLineTopY = coords[1]; // y-coord of top of the left VLINE	

				coords = getTopBottomCoordsOfSegment(rightVLineSegment);
				float rightVLineTopX = coords[0]; // x-coord of top of the right VLINE
				float rightVLineTopY = coords[1]; // y-coord of top of the right VLINE	

				// Get x,y of the top and bottom of BSLASH
				coords = getTopBottomCoordsOfSegment(mPenSegments.elementAt(bSlashIndex));
				float bSlashTopX = coords[0]; // x-coord of top end of the BSLASH stroke
				float bSlashTopY = coords[1]; // y-coord of top end of the BSLASH stroke
				float bSlashBottomY = coords[3]; // y-coord of bottom end of the BSLASH stroke
				float bSlashHeight = Math.abs(bSlashBottomY - bSlashTopY);
				double bSlashGapThreshold = 0.25 * bSlashHeight;

				// Get x,y of the top and bottom of FSLASH
				coords = getTopBottomCoordsOfSegment(mPenSegments.elementAt(fSlashIndex));
				float fSlashTopX = coords[0]; // x-coord of top end of the FSLASH stroke
				float fSlashTopY = coords[1]; // y-coord of top end of the FSLASH stroke
				float fSlashBottomY = coords[3]; // y-coord of bottom end of the FSLASH stroke
				float fSlashHeight = Math.abs(fSlashBottomY - fSlashTopY);
				double fSlashGapThreshold = 0.25 * fSlashHeight;

				// Check to see if the following gaps are close enough:
				// i.   Gap between the top of the left HLINE and the top of the BSLASH (left top of 'M')
				// ii.  Gap between the bottom of the BSLASH and the bottom of FSLASH (bottom of middle 'V' of 'M')
				// iii. Gap between the top of the right HLINE and the top of the FSLASH (right top of 'M')
				matchedP =
					(PenUtil.distanceBetween2Points(bSlashTopX, bSlashTopY, leftVLineTopX, leftVLineTopY) < bSlashGapThreshold) &
					gapCheckForVShape(mPenSegments.elementAt(fSlashIndex), mPenSegments.elementAt(bSlashIndex)) &					
					(PenUtil.distanceBetween2Points(fSlashTopX, fSlashTopY, rightVLineTopX, rightVLineTopY) < fSlashGapThreshold);
			}
		}
		return matchedP;
	} // End of checkForCapitalM()

	// 'N' has a BSLASH ('\') between two VLINE's ('|')
	private boolean checkForCapitalN() {
		boolean matchedP = false;
		int numOfSegments = mPenSegments.size();

		if (numOfSegments == 3) {
			int leftVLineIndex = -1;
			int bSlashIndex = -1;
			int rightVLineIndex = -1;
			for (int i = 0; i < numOfSegments; i++) {
				// 'N' has only three pen stroke characters
				switch (mPenSegments.elementAt(i).mPenSegmentCharacter) {
				case PenSegment.VLINE_CHAR:
					if (leftVLineIndex < 0)
						leftVLineIndex = i;
					else
						rightVLineIndex = i;
					break;
				case PenSegment.BSLASH_CHAR:
					bSlashIndex = i;
					break;
				default:
					break;
				}
			}

			// Check to make sure that the three component strokes for 'N' are there, i.e.,
			// leftVLineIndex, bSlashIndex, and rightVLineIndex are all not negative
			if ((leftVLineIndex >= 0) & (bSlashIndex >= 0) & (rightVLineIndex >= 0)) {
				// Get the left and right VLINE strokes of 'N'
				PenSegment leftRightVLineSegments[] = 
					order2PenSegmentsIntoLeftRight(mPenSegments.elementAt(leftVLineIndex),
							mPenSegments.elementAt(rightVLineIndex));

				PenSegment leftVLineSegment = leftRightVLineSegments[0];
				PenSegment rightVLineSegment = leftRightVLineSegments[1];				
				// Get x,y of the top of the left VLINE
				float coords[] = getTopBottomCoordsOfSegment(leftVLineSegment);
				float leftVLineTopX = coords[0]; // x-coord of top of the left VLINE
				float leftVLineTopY = coords[1]; // y-coord of top of the left VLINE	

				// Get x,y of the bottom of the right VLINE
				coords = getTopBottomCoordsOfSegment(rightVLineSegment);
				float rightVLineBottomX = coords[2]; // x-coord of bottom of the right VLINE
				float rightVLineBottomY = coords[3]; // y-coord of bottom of the right VLINE

				// Get x,y of the top and bottom of BSLASH
				coords = getTopBottomCoordsOfSegment(mPenSegments.elementAt(bSlashIndex));
				float bSlashTopX = coords[0]; // x-coord of top end of the BSLASH stroke
				float bSlashTopY = coords[1]; // y-coord of top end of the BSLASH stroke
				float bSlashBottomX = coords[2]; // x-coord of bottom end of the BSLASH stroke
				float bSlashBottomY = coords[3]; // y-coord of bottom end of the BSLASH stroke

				float bSlashHeight = Math.abs(bSlashBottomY - bSlashTopY);
				double gapThreshold = 0.25 * bSlashHeight;

				// Check to see if the gaps between the top of the left VLINE and the top of the BSLASH and
				// between the bottom of the right VLINE and the bottom of the BSLASH are less than
				// the gap threshold
				matchedP =
					(PenUtil.distanceBetween2Points(bSlashTopX, bSlashTopY, leftVLineTopX, leftVLineTopY) < gapThreshold) &
					(PenUtil.distanceBetween2Points(bSlashBottomX, bSlashBottomY, rightVLineBottomX, rightVLineBottomY) < gapThreshold);
			}
		}
		return matchedP;
	}  // End of checkForCapitalN()

	// 'O' is checked using the checkForCShape() and the isSmallLetter() methods
	// 'P' is checked using checkForPShape() and letter size check

	// 'Q' has a CIRCLE and a BSLASH cutting the circle at the bottom right
	private boolean checkForCapitalQ() {
		boolean matchedP = false;
		int numOfSegments = mPenSegments.size();

		if (numOfSegments == 2) {
			int circleIndex = -1;
			int bSlashIndex = -1;

			for (int i = 0; i < numOfSegments; i++) {
				// 'Q' has only two pen stroke characters
				switch (mPenSegments.elementAt(i).mPenSegmentCharacter) {
				case PenSegment.CIRCLE_CHAR:
					circleIndex = i;
					break;
				case PenSegment.BSLASH_CHAR:
					bSlashIndex = i;
					break;
				default:
					break;
				}
			}

			// Check to make sure that the two component strokes for 'Q' are there, i.e.,
			// circleIndex and vLineIndex are both not negative
			if ((circleIndex >= 0) & (bSlashIndex >= 0)) {
				// Get the x,y of the top and bottom of BC
				float coords[] = getTopBottomCoordsOfSegment(mPenSegments.elementAt(bSlashIndex));
				float bSlashTopX = coords[0]; // x-coord of top end of the BSLASH stroke
				float bSlashTopY = coords[1]; // y-coord of top end of the BSLASH stroke

				// Get average x,y of the CIRCLE stroke
				float hLineAvgX = mPenSegments.elementAt(circleIndex).mAvgX; // average of x-coord (mid-point) of the CIRCLE stroke
				float hLineAvgY = mPenSegments.elementAt(circleIndex).mAvgY; // average of y-coord (mid-point) of the CIRCLE stroke

				float circleHeight = mPenSegments.elementAt(circleIndex).mBoundingRectHeight;

				// Check to see if the following gaps are less their respective thresholds
				// i.   The gap between the top end BSLASH and the mid x,y coords of CIRCLE

				matchedP = (PenUtil.distanceBetween2Points(bSlashTopX, bSlashTopY, hLineAvgX, hLineAvgY) < 0.5 * circleHeight);
			}
		}

		return matchedP;
	}  // End of checkForCapitalQ()

	// 'R' has three strokes - a VLINE, a BC (backward C or ')') and a BSLASH on the right.
	// The BC joins the VLINE at the top and around mid-point and the BSLASH joins the VLINE
	// at around the mid-point and the bottom.
	private boolean checkForCapitalR() {
		boolean matchedP = false;
		int numOfSegments = mPenSegments.size();

		if (numOfSegments == 3) {
			int bCIndex = -1;
			int bSlashIndex = -1;
			int vLineIndex = -1;

			for (int i = 0; i < numOfSegments; i++) {
				// 'R' has only three pen stroke characters.
				switch (mPenSegments.elementAt(i).mPenSegmentCharacter) {
				case PenSegment.BC_CHAR:
					bCIndex = i;
					break;
				case PenSegment.BSLASH_CHAR:
					bSlashIndex = i;
					break;
				case PenSegment.VLINE_CHAR:
					vLineIndex = i;
					break;
				default:
					break;
				}
			}

			// Check to make sure that the two component strokes for 'P' are there, i.e.,
			// bCIndex, bSlashIndex and vLineIndex are both not negative.
			if ((bCIndex >= 0) & (bSlashIndex >= 0) & (vLineIndex >= 0)) {
				// Get x,y coords of the BC stroke
				float coords[] = getTopBottomCoordsOfSegment(mPenSegments.elementAt(bCIndex));
				float bCTopX = coords[0]; // x-coord of top end of the BC stroke
				float bCTopY = coords[1]; // y-coord of top end of the BC stroke
				float bCBottomX = coords[2]; // x-coord of bottom end of the BC stroke
				float bCBottomY = coords[3]; // y-coord of bottom end of the BC stroke

				// Get x,y coords of the BSLASH stroke
				coords = getTopBottomCoordsOfSegment(mPenSegments.elementAt(bSlashIndex));
				float bSlashTopX = coords[0]; // x-coord of top end of the BSLASH stroke
				float bSlashTopY = coords[1]; // y-coord of top end of the BSLASH stroke

				// Get x,y of the top and bottom of VLINE
				coords = getTopBottomCoordsOfSegment(mPenSegments.elementAt(vLineIndex));
				float vLineTopX = coords[0]; // x-coord of top end of the VLINE stroke
				float vLineTopY = coords[1]; // y-coord of top end of the VLINE stroke
				float vLineBottomX = coords[2]; // x-coord of bottom end of the VLINE stroke
				float vLineBottomY = coords[3]; // y-coord of bottom end of the VLINE stroke
				float vLineMidX = (vLineTopX + vLineBottomX)/2; // average of x-coord (mid-point) of the VLINE stroke
				float vLineMidY = (vLineTopY + vLineBottomY)/2; // average of y-coord (mid-point) of the VLINE stroke

				float vLineHeight = Math.abs(vLineBottomY - vLineTopY);
				double gapThreshold = 0.25 * vLineHeight;

				// Check to see if the following gaps are less their respective thresholds
				// i.   The gap between the top ends VLINE and BC
				// ii.  The gap between the bottom of the BC and the mid-point of VLINE
				// iii. The gap between the top of the BSLASH and the mid-point of VLINE
				matchedP =
					(PenUtil.distanceBetween2Points(vLineTopX, vLineTopY, bCTopX, bCTopY) < gapThreshold) &
					(PenUtil.distanceBetween2Points(vLineMidX, vLineMidY, bCBottomX, bCBottomY) < 2 * gapThreshold) &
					(PenUtil.distanceBetween2Points(vLineMidX, vLineMidY, bSlashTopX, bSlashTopY) < 2 * gapThreshold);
			}
		}
		return matchedP;
	}  // End of checkForCapitalR()

	// 'T' has an HLINE ('-') at the top of a VLINE ('|')
	private boolean checkForCapitalT() {
		boolean matchedP = false;
		int numOfSegments = mPenSegments.size();

		if (numOfSegments == 2) {
			int topHLineIndex = -1;
			int vLineIndex = -1;

			for (int i = 0; i < numOfSegments; i++) {
				// 'T' has only two pen stroke characters
				switch (mPenSegments.elementAt(i).mPenSegmentCharacter) {
				case PenSegment.HLINE_CHAR:
					topHLineIndex = i;
					break;
				case PenSegment.VLINE_CHAR:
					vLineIndex = i;
					break;
				default:
					break;
				}
			}

			// Check to make sure that the two component strokes for 'T' are there, i.e.,
			// topHLineIndex and vLineIndex are both not negative
			if ((topHLineIndex >= 0) & (vLineIndex >= 0)) {
				// Assume the HLINE found to be top HLINE of the 'T'
				float topHLineStartX = mPenSegments.elementAt(topHLineIndex).mPosStart[0]; // x-coord of top end of the HLINE stroke
				float topHLineStartY = mPenSegments.elementAt(topHLineIndex).mPosStart[1]; // y-coord of top end of the HLINE stroke
				float topHLineEndX = mPenSegments.elementAt(topHLineIndex).mPosEnd[0]; // x-coord of bottom end of the HLINE stroke
				float topHLineEndY = mPenSegments.elementAt(topHLineIndex).mPosEnd[1]; // y-coord of bottom end of the HLINE stroke
				float topHLineMidX = (topHLineStartX + topHLineEndX)/2;
				float topHLineMidY = (topHLineStartY + topHLineEndY)/2;

				// Get x,y of top and bottom of VLINE
				float coords[] = getTopBottomCoordsOfSegment(mPenSegments.elementAt(vLineIndex));		
				float vLineTopX = coords[0]; // x-coord of top end of the VLINE stroke
				float vLineTopY = coords[1]; // y-coord of top end of the VLINE stroke
				float vLineBottomY = coords[3]; // y-coord of bottom end of the VLINE stroke

				float vLineHeight = Math.abs(vLineBottomY - vLineTopY);
				double gapThreshold = 0.15 * vLineHeight;

				// Check to see if the gap between the mid-point of the top HLINE and the top of
				// the VLINE is less than the gap threshold
				matchedP =
					(PenUtil.distanceBetween2Points(vLineTopX, vLineTopY, topHLineMidX, topHLineMidY) < gapThreshold);
			}
		}
		return matchedP;
	} // End of checkForCapitalT()

	// 'U' is checked using the checkForCShape() and the isSmallLetter() methods
	// 'V' is checked using the checkForCShape() and the isSmallLetter() methods
	// 'W' is checked using the checkForCShape() and the isSmallLetter() methods
	// 'X' is checked using the checkForCShape() and the isSmallLetter() methods

	// 'Y' is made up of a back slash ('\'), a forward slash ('/') and a vertical line ('|')
	private boolean checkForCapitalY(){
		boolean matchedP = false;
		int numOfSegments = mPenSegments.size();

		if (numOfSegments == 3) {
			int fSlashIndex = -1;
			int bSlashIndex = -1;
			int vLineIndex = -1;
			for (int i = 0; i < numOfSegments; i++) {
				// 'Y' has only three pen stroke characters
				switch (mPenSegments.elementAt(i).mPenSegmentCharacter) {
				case PenSegment.FSLASH_CHAR:
					fSlashIndex = i;
					break;
				case PenSegment.BSLASH_CHAR:
					bSlashIndex = i;
					break;
				case PenSegment.VLINE_CHAR:
					vLineIndex = i;
					break;
				default:
					break;
				}
			}

			// Check to make sure that the three component strokes for 'Y' are there, i.e.,
			// fSlashIndex, bSlashIndex, and vLineIndex are all not negative
			if ((fSlashIndex >= 0) & (bSlashIndex >= 0) & (vLineIndex >= 0)) {

				// Get the bottom coords of FSLASH and BSLASH
				float coords[] = getTopBottomCoordsOfSegment(mPenSegments.elementAt(fSlashIndex));		
				float fSlashBottomX = coords[2]; // x-coord of bottom end of the FSLASH stroke
				float fSlashBottomY = coords[3]; // y-coord of bottom end of the FSLASH stroke

				coords = getTopBottomCoordsOfSegment(mPenSegments.elementAt(bSlashIndex));		
				float bSlashBottomX = coords[2]; // x-coord of bottom end of the BSLASH stroke
				float bSlashBottomY = coords[3]; // y-coord of bottom end of the BSLASH stroke

				float avgSlashBottomX = (fSlashBottomX + bSlashBottomX)/2;
				float avgSlashBottomY = (fSlashBottomY + bSlashBottomY)/2;

				coords = getTopBottomCoordsOfSegment(mPenSegments.elementAt(vLineIndex));
				float hLineTopX = coords[0]; // x-coord of top end of the VLINE stroke
				float hLineTopY = coords[1]; // y-coord of top end of the VLINE stroke
				float hLineBottomY = coords[3]; // y-coord of bottom end of the VLINE stroke
				float hLineHeight = Math.abs(hLineBottomY - hLineTopY);

				// Check to see if the gap between the bottom of FSLASH and BSLASH is less than one quarter the distance
				// between their tops and the bottom of FSLASH and BSLASH is less than half the HLINE height from the
				// top of HLINE
				matchedP = 
					(gapCheckForVShape(mPenSegments.elementAt(fSlashIndex), mPenSegments.elementAt(bSlashIndex))) &
					(PenUtil.distanceBetween2Points(avgSlashBottomX, avgSlashBottomY, hLineTopX, hLineTopY) < .5 * hLineHeight);

			}
		}
		return matchedP;
	} // End of checkForCapitalY()

	// 'Z' is checked using the checkForCShape() and the isSmallLetter() methods

	/************************
	 *                      *
	 * Small Letters a to z *
	 *                      *
	 ************************/

	// 'a' has a FC and VLINE on the right joining the VLINE almost at the top and bottom
	private boolean checkForSmallA() {
		boolean matchedP = false;
		int numOfSegments = mPenSegments.size();

		if (numOfSegments == 2) {
			// Indices listed in writing order, i.e., FC first and then VLINE (both strokes should be written top to bottom)
			int fCIndex = -1;
			int vLineIndex = -1;

			for (int i = 0; i < numOfSegments; i++) {
				// 'a' has only two pen stroke characters
				switch (mPenSegments.elementAt(i).mPenSegmentCharacter) {
				case PenSegment.FC_CHAR:
					fCIndex = i;
					break;
				case PenSegment.VLINE_CHAR:
					vLineIndex = i;
					break;
				default:
					break;
				}
			}

			// Check to make sure that the two component strokes for 'a' are there, i.e.,
			// fCIndex and vLineIndex are both not negative
			if ((fCIndex >= 0) & (vLineIndex >= 0)) {
				// Get the x,y of the top and bottom of FC
				float coords[] = getTopBottomCoordsOfSegment(mPenSegments.elementAt(fCIndex));
				float fCTopX = coords[0]; // x-coord of top end of the FC stroke
				float fCTopY = coords[1]; // y-coord of top end of the FC stroke
				float fCBottomX = coords[2]; // x-coord of bottom end of the FC stroke
				float fCBottomY = coords[3]; // y-coord of bottom end of the FC stroke

				// Get x,y of the top and bottom of VLINE
				coords = getTopBottomCoordsOfSegment(mPenSegments.elementAt(vLineIndex));
				float vLineTopX = coords[0]; // x-coord of top end of the VLINE stroke
				float vLineTopY = coords[1]; // y-coord of top end of the VLINE stroke
				float vLineBottomX = coords[2]; // x-coord of bottom end of the VLINE stroke
				float vLineBottomY = coords[3]; // y-coord of bottom end of the VLINE stroke

				float vLineHeight = Math.abs(vLineBottomY - vLineTopY);
				double gapThreshold = 0.2 * vLineHeight;

				// Check to see if the gap between the top ends of VLINE and FC and that between
				// the bottom ends of VLINE and FC are less than the gap threshold
				matchedP =
					(PenUtil.distanceBetween2Points(vLineTopX, vLineTopY, fCTopX, fCTopY) < gapThreshold) &
					(PenUtil.distanceBetween2Points(vLineBottomX, vLineBottomY, fCBottomX, fCBottomY) < gapThreshold);
			}
		}

		return matchedP;
	}  // End of checkForSmallA()

	// 'b' has a VLINE and a BC on the right joining the VLINE at the middle and bottom
	private boolean checkForSmallB() {
		boolean matchedP = false;
		int numOfSegments = mPenSegments.size();

		if (numOfSegments == 2) {
			// Indices listed in writing order, i.e., VLINE first and then BC (both strokes should be written top to bottom)
			int vLineIndex = -1;
			int bCIndex = -1;


			for (int i = 0; i < numOfSegments; i++) {
				// 'b' has only two pen stroke characters
				switch (mPenSegments.elementAt(i).mPenSegmentCharacter) {
				case PenSegment.VLINE_CHAR:
					vLineIndex = i;
					break;
				case PenSegment.BC_CHAR:
					bCIndex = i;
					break;
				default:
					break;
				}
			}

			// Check to make sure that the two component strokes for 'b' are there, i.e.,
			// bCIndex and vLineIndex are both not negative
			if ((bCIndex >= 0) & (vLineIndex >= 0)) {

				// Get x,y of the top and bottom of VLINE
				float coords[] = getTopBottomCoordsOfSegment(mPenSegments.elementAt(vLineIndex));
				float vLineTopX = coords[0]; // x-coord of top end of the VLINE stroke
				float vLineTopY = coords[1]; // y-coord of top end of the VLINE stroke
				float vLineBottomX = coords[2]; // x-coord of bottom end of the VLINE stroke
				float vLineBottomY = coords[3]; // y-coord of bottom end of the VLINE stroke
				float vLineMidX = (vLineTopX + vLineBottomX)/2; // x-coordinate of the mid point of the VLINE stroke
				float vLineMidY = (vLineTopY + vLineBottomY)/2; // y-coordinate of the mid point of the VLINE stroke

				// Get the x,y of the top and bottom of BC
				coords = getTopBottomCoordsOfSegment(mPenSegments.elementAt(bCIndex));
				float bCTopX = coords[0]; // x-coord of top end of the BC stroke
				float bCTopY = coords[1]; // y-coord of top end of the BC stroke
				float bCBottomX = coords[2]; // x-coord of bottom end of the BC stroke
				float bCBottomY = coords[3]; // y-coord of bottom end of the BC stroke


				float vLineHeight = Math.abs(vLineBottomY - vLineTopY);
				double gapThreshold = 0.2 * vLineHeight;

				// Check to see if the following gaps are within their respective threshold
				// i.  Gap between the middle of VLINE and top of BC
				// ii. The gap between the bottom ends of VLINE and BC
				matchedP =
					(PenUtil.distanceBetween2Points(vLineMidX, vLineMidY, bCTopX, bCTopY) < gapThreshold) &
					(PenUtil.distanceBetween2Points(vLineBottomX, vLineBottomY, bCBottomX, bCBottomY) < gapThreshold);
			}
		}

		return matchedP;
	}  // End of checkForSmallB()

	// 'c' is checked using the checkForCShape() and the isSmallLetter() methods

	// 'd' has a FC and VLINE on the right joining the VLINE at the middle and bottom
	private boolean checkForSmallD() {
		boolean matchedP = false;
		int numOfSegments = mPenSegments.size();

		if (numOfSegments == 2) {
			// Indices listed in writing order, i.e., FC first and then VLINE (both strokes should be written top to bottom)
			int fCIndex = -1;
			int vLineIndex = -1;

			for (int i = 0; i < numOfSegments; i++) {
				// 'd' has only two pen stroke characters
				switch (mPenSegments.elementAt(i).mPenSegmentCharacter) {
				case PenSegment.FC_CHAR:
					fCIndex = i;
					break;
				case PenSegment.VLINE_CHAR:
					vLineIndex = i;
					break;
				default:
					break;
				}
			}

			// Check to make sure that the two component strokes for 'd' are there, i.e.,
			// fCIndex and vLineIndex are both not negative
			if ((fCIndex >= 0) & (vLineIndex >= 0)) {
				// Get the x,y of the top and bottom of FC
				float coords[] = getTopBottomCoordsOfSegment(mPenSegments.elementAt(fCIndex));
				float fCTopX = coords[0]; // x-coord of top end of the FC stroke
				float fCTopY = coords[1]; // y-coord of top end of the FC stroke
				float fCBottomX = coords[2]; // x-coord of bottom end of the FC stroke
				float fCBottomY = coords[3]; // y-coord of bottom end of the FC stroke

				// Get x,y of the top and bottom of VLINE
				coords = getTopBottomCoordsOfSegment(mPenSegments.elementAt(vLineIndex));
				float vLineTopX = coords[0]; // x-coord of top end of the VLINE stroke
				float vLineTopY = coords[1]; // y-coord of top end of the VLINE stroke
				float vLineBottomX = coords[2]; // x-coord of bottom end of the VLINE stroke
				float vLineBottomY = coords[3]; // y-coord of bottom end of the VLINE stroke
				float vLineMidX = (vLineTopX + vLineBottomX)/2; // x-coordinate of the mid point of the VLINE stroke
				float vLineMidY = (vLineTopY + vLineBottomY)/2; // y-coordinate of the mid point of the VLINE stroke		

				float vLineHeight = Math.abs(vLineBottomY - vLineTopY);
				double gapThreshold = 0.2 * vLineHeight;

				// Check to see if the following gaps are within their respective threshold
				// i.  Gap between the middle of VLINE and top of FC
				// ii. The gap between the bottom ends of VLINE and FC
				matchedP =
					(PenUtil.distanceBetween2Points(vLineMidX, vLineMidY, fCTopX, fCTopY) < gapThreshold) &
					(PenUtil.distanceBetween2Points(vLineBottomX, vLineBottomY, fCBottomX, fCBottomY) < gapThreshold);
			}
		}

		return matchedP;
	}  // End of checkForSmallD()

	// 'i' has a VLINE and a DOT above it
	private boolean checkForSmallI() {
		boolean matchedP = false;
		int numOfSegments = mPenSegments.size();

		if (numOfSegments == 2) {
			// Indices listed in writing order, i.e., VLINE (top down)and then a DOT above the VLINE
			int vLineIndex = -1;
			int dotIndex = -1;

			for (int i = 0; i < numOfSegments; i++) {
				// 'i' has only two pen stroke characters
				switch (mPenSegments.elementAt(i).mPenSegmentCharacter) {
				case PenSegment.VLINE_CHAR:
					vLineIndex = i;
					break;
				case PenSegment.DOT_CHAR:
					dotIndex = i;
					break;
				default:
					break;
				}
			}

			// Check to make sure that the two component strokes for 'i' are there, i.e.,
			// vLineIndex and dotIndex are both not negative
			if ((vLineIndex >= 0) & (dotIndex >= 0)) {
				// Get x,y of the top and bottom of VLINE
				float coords[] = getTopBottomCoordsOfSegment(mPenSegments.elementAt(vLineIndex));
				float vLineTopX = coords[0]; // x-coord of top end of the VLINE stroke
				float vLineTopY = coords[1]; // y-coord of top end of the VLINE stroke
				float vLineBottomY = coords[3]; // y-coord of bottom end of the VLINE stroke	

				float vLineHeight = vLineBottomY - vLineTopY;
				double verticalGapThreshold = 0.5 * vLineHeight;
				double horizontalGapThreshold = 0.1 * vLineHeight;

				// Get the x,y of the top and bottom of DOT
				coords = getTopBottomCoordsOfSegment(mPenSegments.elementAt(dotIndex));
				float dotBottomX = coords[2]; // x-coord of bottom end of the DOT stroke
				float dotBottomY = coords[3]; // y-coord of bottom end of the DOT stroke

				// Check to see if the DOT is above the VLINE
				matchedP =
					isBetweenThresholds((vLineTopY - dotBottomY),  0, verticalGapThreshold) &
					(Math.abs(vLineTopX - dotBottomX) < horizontalGapThreshold);
			}
		}

		return matchedP;
	}  // End of checkForSmallI()

	// 'o' is checked using the method checkForOShape() and the method isSmallLetter()
	// 'p' is checked using checkForPShape() and letter size check

	// 'q' has a VLINE and a FC on the right joining the VLINE at the top and middle
	private boolean checkForSmallQ() {
		boolean matchedP = false;

		double vLineTopYMin = .4 * Skiggle.DEFAULT_WRITE_PAD_HEIGHT;
		double vLineTopYMax = Skiggle.DEFAULT_WRITE_PAD_HEIGHT + 1.0;

		// Check to see if the following are true
		// i.   VLINE and FC form a q or 9 shape
		// ii.  The top of the VLINE is more than .4 the height of the writing area from the top edge of the writing area
		matchedP =
			checkFor9OrSmallQ((float)vLineTopYMin, (float)vLineTopYMax);

		return matchedP;
	}  // End of checkForSmallQ()

	// 't' has a VLINE and a HLINE cutting the VLINE somewhere between .2 and .4 of the VLINE's height from the top of VLINE
	private boolean checkForSmallT() {
		boolean matchedP = false;
		int numOfSegments = mPenSegments.size();

		if (numOfSegments == 2) {
			int vLineIndex = -1;
			int hLineIndex = -1;

			for (int i = 0; i < numOfSegments; i++) {
				// 't' has only two pen stroke characters
				switch (mPenSegments.elementAt(i).mPenSegmentCharacter) {
				case PenSegment.VLINE_CHAR:
					vLineIndex = i;
					break;
				case PenSegment.HLINE_CHAR:
					hLineIndex = i;
					break;
				default:
					break;
				}
			}

			// Check to make sure that the two component strokes for 't' are there, i.e.,
			// vLineIndex and hLineIndex are both not negative
			if ((vLineIndex >= 0) & (hLineIndex >= 0)) {
				// Get x,y of the top and bottom of VLINE
				float coords[] = getTopBottomCoordsOfSegment(mPenSegments.elementAt(vLineIndex));
				float vLineTopX = coords[0]; // x-coord of top end of the VLINE stroke
				float vLineTopY = coords[1]; // y-coord of top end of the VLINE stroke
				float vLineBottomY = coords[3]; // y-coord of bottom end of the VLINE stroke

				float vLineHeight = Math.abs(vLineBottomY - vLineTopY);
				double minDistFromTop = 0.2 * vLineHeight;
				double maxDistFromTop = 0.5 * vLineHeight;

				coords = getLeftRightCoordsOfSegment(mPenSegments.elementAt(hLineIndex));
				float hLineLeftX = coords[0]; // x-coord of left point of the HLINE stroke
				float hLineLeftY = coords[1]; // y-coord of left point of the HLINE stroke
				float hLineRightX = coords[2]; // x-coord of right point of the HLINE stroke
				float hLineRightY = coords[3]; // y-coord of right point of the HLINE stroke
				float hLineMidX = (hLineLeftX + hLineRightX)/2; // x-coord of the mid point of the HLINE stroke
				float hLineMidY = (hLineLeftY + hLineRightY)/2; // y-coord of the mid point of the HLINE stroke

				// Check to see if the gaps between the leftmost end the bottom HLINE and bottom of the VLINE
				// is less than the gap threshold
				matchedP =
					(isBetweenThresholds(PenUtil.distanceBetween2Points(vLineTopX, vLineTopY, hLineMidX, hLineMidY),
							minDistFromTop, maxDistFromTop));
			}
		}

		return matchedP;
	}  // End of checkForSmallT()


	// 'u' is checked using the checkForCShape() and the isSmallLetter() methods
	// 'v' is checked using the checkForCShape() and the isSmallLetter() methods
	// 'w' is checked using the checkForCShape() and the isSmallLetter() methods
	// 'x' is checked using the checkForCShape() and the isSmallLetter() methods


	// 'y' has a BSLASH and a FSLASH on the right with the bottom of the BSLASH touching the middle of the FSLASH
	private boolean checkForSmallY() {
		boolean matchedP = false;
		int numOfSegments = mPenSegments.size();

		if (numOfSegments == 2) {
			int bSlashIndex = -1; // First stroke
			int fSlashIndex = -1; // Second stroke

			for (int i = 0; i < numOfSegments; i++) {
				// 'y' has only two pen stroke characters
				switch (mPenSegments.elementAt(i).mPenSegmentCharacter) {
				case PenSegment.BSLASH_CHAR:
					bSlashIndex = i;
					break;
				case PenSegment.FSLASH_CHAR:
					fSlashIndex = i;
					break;
				default:
					break;
				}
			}
			// Check to make sure that the two component strokes for 'y' are there, i.e.,
			// fSlashIndex and bSlashIndex are both not negative
			if ((bSlashIndex >= 0) & (fSlashIndex >= 0)) {

				float coords[] = getTopBottomCoordsOfSegment(mPenSegments.elementAt(bSlashIndex));		
				float bSlashBottomX = coords[2]; // x-coord of bottom end of the BSLASH stroke
				float bSlashBottomY = coords[3]; // y-coord of bottom end of the BSLASH stroke

				coords = getTopBottomCoordsOfSegment(mPenSegments.elementAt(fSlashIndex));		
				float fSlashTopX = coords[0]; // x-coord of top end of the FSLASH stroke
				float fSlashTopY = coords[1]; // y-coord of top end of the FSLASH stroke
				float fSlashBottomX = coords[2]; // x-coord of bottom end of the FSLASH stroke
				float fSlashBottomY = coords[3]; // y-coord of bottom end of the FSLASH stroke
				float fSlashAvgX = (fSlashTopX + fSlashBottomX)/2; // average of the x-coord of FSLASH
				float fSlashAvgY = (fSlashTopY + fSlashBottomY)/2; // average of the y-coord of FSLASH
				double gapThreshold = .25 * Math.abs(fSlashBottomY - fSlashTopY); 

				// Check to make sure that the bottom of BSLASH touches the middle of FSLASH to form the 'y'
				matchedP = 
					PenUtil.distanceBetween2Points(bSlashBottomX, bSlashBottomY, fSlashAvgX, fSlashAvgY) < gapThreshold;
			}
		}

		return matchedP;
	}  // End of checkForSmallY()

	// 'z' is checked using the checkForCShape() and the isSmallLetter() methods

	/******************************************************************************************************************************
	 *                                                                                                                            *
	 * Special Characters <space>, !, ", #, $, %, &, ', (, ), *, +, ,, -, ., /, :, ;, <, =, >, ?, @, [, \, ], ^, _, `, {, |, }, ~ *
	 *                                                                                                                            *
	 ******************************************************************************************************************************/

	// Check for  ' '

	// Check for  '!'
	// '!' has a VLINE and a DOT below it
	private boolean checkForExclamationMark() {
		boolean matchedP = false;
		int numOfSegments = mPenSegments.size();

		if (numOfSegments == 2) {
			// Indices listed in writing order, i.e., VLINE (top down)and then a DOT above the VLINE
			int vLineIndex = -1;
			int dotIndex = -1;

			for (int i = 0; i < numOfSegments; i++) {
				// '!' has only two pen stroke characters
				switch (mPenSegments.elementAt(i).mPenSegmentCharacter) {
				case PenSegment.VLINE_CHAR:
					vLineIndex = i;
					break;
				case PenSegment.DOT_CHAR:
					dotIndex = i;
					break;
				default:
					break;
				}
			}

			// Check to make sure that the two component strokes for '!' are there, i.e.,
			// vLineIndex and dotIndex are both not negative
			if ((vLineIndex >= 0) & (dotIndex >= 0)) {
				// Get x,y of the top and bottom of VLINE
				float coords[] = getTopBottomCoordsOfSegment(mPenSegments.elementAt(vLineIndex));
				float vLineTopY = coords[1]; // y-coord of top end of the VLINE stroke
				float vLineBottomX = coords[2]; // x-coord of top end of the VLINE stroke
				float vLineBottomY = coords[3]; // y-coord of bottom end of the VLINE stroke	

				float vLineHeight = vLineBottomY - vLineTopY;
				double verticalGapThreshold = 0.5 * vLineHeight;
				double horizontalGapThreshold = 0.1 * vLineHeight;

				// Get the x,y of the top and bottom of DOT
				coords = getTopBottomCoordsOfSegment(mPenSegments.elementAt(dotIndex));
				float dotTopX = coords[0]; // x-coord of top end of the DOT stroke
				float dotTopY = coords[1]; // y-coord of top end of the DOT stroke

				// Check to see if the DOT is below the VLINE
				matchedP =
					isBetweenThresholds((dotTopY - vLineBottomY), 0, verticalGapThreshold) &
					(Math.abs(dotTopX - vLineBottomX) < horizontalGapThreshold);
			}
		}

		return matchedP;
	}  // End of checkForExclamationMark()

	// Check for  '\"'

	// Check for  '#'
	// '#' has a pair of FSLASH's ('/') next to each other and a pair of HLINE's ('-') one on top of the other
	//  The pairs of FSLASH's and HLINE's cross each other.
	private boolean checkForHashSymbol() {
		boolean matchedP = false;
		int numOfSegments = mPenSegments.size();

		if (numOfSegments == 4) {
			int hLine1Index = -1;
			int hLine2Index = -1;
			int fSlash1Index = -1; 
			int fSlash2Index = -1;
			for (int i = 0; i < numOfSegments; i++) {
				// '#' has only four pen stroke characters
				switch (mPenSegments.elementAt(i).mPenSegmentCharacter) {
				case PenSegment.HLINE_CHAR:
					if (hLine1Index < 0)
						hLine1Index = i;
					else
						hLine2Index = i;
					break;
				case PenSegment.FSLASH_CHAR:
					if (fSlash1Index < 0)
						fSlash1Index = i;
					else
						fSlash2Index = i;
					break;
				default:
					break;
				}
			}

			// Check to make sure that the four component strokes for '#' are there, i.e.,
			// hLine1Index, hLine2Index, fSlash1Index, and fSlash2Index are all not negative
			if ((hLine1Index >= 0) & (hLine2Index >= 0) & (fSlash1Index >= 0) & (fSlash2Index >= 0)) {
				// Get the top and bottom HLINE strokes of '#'
				PenSegment topBottomHLineSegments[] = 
					order2PenSegmentsIntoTopBottom(mPenSegments.elementAt(hLine1Index),
							mPenSegments.elementAt(hLine2Index));

				// Get the x,y coordinates at the one-third point of the top HLINE stroke
				PenSegment topHLineSegment = topBottomHLineSegments[0];
				float coords[] = getLeftRightCoordsOfSegment(topHLineSegment);
				float coordsOf1stAnd2ndThirdMarks[] = getPointsAt1stAnd2ndThirdMarks(coords[0], coords[1], coords[2], coords[3]);
				float topHLine1st3rdX = coordsOf1stAnd2ndThirdMarks[0];
				float topHLine1st3rdY = coordsOf1stAnd2ndThirdMarks[1];
				float topHLine2nd3rdX = coordsOf1stAnd2ndThirdMarks[2];
				float topHLine2nd3rdY = coordsOf1stAnd2ndThirdMarks[3];

				// Get the x,y coordinates at the two-third point of the bottom HLINE stroke
				PenSegment bottomHLineSegment = topBottomHLineSegments[1];
				coords = getLeftRightCoordsOfSegment(bottomHLineSegment);
				coordsOf1stAnd2ndThirdMarks = getPointsAt1stAnd2ndThirdMarks(coords[0], coords[1], coords[2], coords[3]);
				float bottomHLine1st3rdX = coordsOf1stAnd2ndThirdMarks[0];
				float bottomHLine1st3rdY = coordsOf1stAnd2ndThirdMarks[1];
				float bottomHLine2nd3rdX = coordsOf1stAnd2ndThirdMarks[2];
				float bottomHLine2nd3rdY = coordsOf1stAnd2ndThirdMarks[3];

				// Get the left and right FSLASH strokes of '#'
				PenSegment leftRightFSlashSegments[] = 
					order2PenSegmentsIntoTopBottom(mPenSegments.elementAt(fSlash1Index),
							mPenSegments.elementAt(fSlash2Index));

				// Get the x,y coordinates at the one-third point of the top FSLASH stroke
				PenSegment leftFSlashSegment = leftRightFSlashSegments[0];
				coords = getTopBottomCoordsOfSegment(leftFSlashSegment);
				coordsOf1stAnd2ndThirdMarks = getPointsAt1stAnd2ndThirdMarks(coords[0], coords[1], coords[2], coords[3]);
				float leftFSlash1st3rdX = coordsOf1stAnd2ndThirdMarks[0];
				float leftFSlash1st3rdY = coordsOf1stAnd2ndThirdMarks[1];
				float leftFSlash2nd3rdX = coordsOf1stAnd2ndThirdMarks[2];
				float leftFSlash2nd3rdY = coordsOf1stAnd2ndThirdMarks[3];

				// Get the x,y coordinates at the one-third point of the top FSLASH stroke
				PenSegment rightFSlashSegment = leftRightFSlashSegments[1];
				coords = getTopBottomCoordsOfSegment(rightFSlashSegment);
				coordsOf1stAnd2ndThirdMarks = getPointsAt1stAnd2ndThirdMarks(coords[0], coords[1], coords[2], coords[3]);
				float rightFSlash1st3rdX = coordsOf1stAnd2ndThirdMarks[0];
				float rightFSlash1st3rdY = coordsOf1stAnd2ndThirdMarks[1];
				float rightFSlash2nd3rdX = coordsOf1stAnd2ndThirdMarks[2];
				float rightFSlash2nd3rdY = coordsOf1stAnd2ndThirdMarks[3];

				float gapThreshold = 
					// (float) 0.25 *
					Math.min(Math.min(PenUtil.distanceBetween2Points(topHLine1st3rdX, topHLine1st3rdY, topHLine2nd3rdX, topHLine2nd3rdY),
							PenUtil.distanceBetween2Points(bottomHLine1st3rdX, bottomHLine1st3rdY, bottomHLine2nd3rdX, bottomHLine2nd3rdY)),
							Math.min(PenUtil.distanceBetween2Points(leftFSlash1st3rdX, leftFSlash1st3rdY, leftFSlash2nd3rdX, leftFSlash2nd3rdY),
									PenUtil.distanceBetween2Points(rightFSlash1st3rdX, rightFSlash1st3rdY, rightFSlash2nd3rdX, rightFSlash2nd3rdY)));

				// Check to see if the following gaps are close enough:
				matchedP = 
					(PenUtil.distanceBetween2Points(topHLine1st3rdX, topHLine1st3rdY, leftFSlash1st3rdX, leftFSlash1st3rdY) < gapThreshold) &
					(PenUtil.distanceBetween2Points(topHLine2nd3rdX, topHLine2nd3rdY, rightFSlash1st3rdX, rightFSlash1st3rdY) < gapThreshold) &
					(PenUtil.distanceBetween2Points(bottomHLine1st3rdX, bottomHLine1st3rdY, leftFSlash2nd3rdX, leftFSlash2nd3rdY) < gapThreshold) &
					(PenUtil.distanceBetween2Points(bottomHLine2nd3rdX, bottomHLine2nd3rdY, rightFSlash2nd3rdX, rightFSlash2nd3rdY) < gapThreshold);

			}
		}
		return matchedP;
	} // End of checkForHashSymbol()



	// Check for  '$'

	// Check for  '%'
	// '%' CIRCLE ('o') at the top and bottom of a FSLASH ('/')
	private boolean checkForPercentSign() {
		boolean matchedP = false;
		int numOfSegments = mPenSegments.size();

		if (numOfSegments == 3) {
			int topCircleIndex = -1;
			int fSlashIndex = -1;
			int bottomCircleIndex = -1;
			for (int i = 0; i < numOfSegments; i++) {
				// '%' has only three pen stroke characters
				switch (mPenSegments.elementAt(i).mPenSegmentCharacter) {
				case PenSegment.CIRCLE_CHAR:
					if (topCircleIndex < 0)
						topCircleIndex = i;
					else
						bottomCircleIndex = i;
					break;
				case PenSegment.FSLASH_CHAR:
					fSlashIndex = i;
					break;
				default:
					break;
				}
			}

			// Check to make sure that the three component strokes for '%' are there, i.e.,
			// topCircleIndex, vLineIndex, and bottomCircleIndex are all not negative
			if ((topCircleIndex >= 0) & (fSlashIndex >= 0) & (bottomCircleIndex >= 0)) {
				// Get the top and bottom CIRCLE strokes of '%'
				PenSegment topBottomCircleSegments[] = 
					order2PenSegmentsIntoTopBottom(mPenSegments.elementAt(topCircleIndex),
							mPenSegments.elementAt(bottomCircleIndex));

				// Get x, y coords of the middle of the top CIRCLE
				float topCircleMidX = topBottomCircleSegments[0].mAvgX; // x-coord of mid point of the top CIRCLE;
				float topCircleMidY = topBottomCircleSegments[0].mAvgY; // y-coord of mid point of the top CIRCLE;

				// Get x, y coords of the middle of the bottom CIRCLE
				float bottomCircleMidX = topBottomCircleSegments[1].mAvgX; // x-coord of mid point of the bottom CIRCLE;
				float bottomCircleMidY = topBottomCircleSegments[1].mAvgY; // y-coord of mid point of the bottom CIRCLE;

				// Get x,y of top and bottom of FSLASH
				float coords[] = getTopBottomCoordsOfSegment(mPenSegments.elementAt(fSlashIndex));
				float fSlashTopX = coords[0]; // x-coord of top end of the FSLASH stroke
				float fSlashTopY = coords[1]; // y-coord of top end of the FSLASH stroke
				float fSlashBottomX = coords[2]; // x-coord of bottom end of the FSLASH stroke
				float fSlashBottomY = coords[3]; // y-coord of bottom end of the FSLASH stroke
				float fSlashMidX = (fSlashTopX + fSlashBottomX)/2; // x-ccord of the mid point of the FSLASH stroke
				float fSlashMidY = (fSlashTopY + fSlashBottomY)/2; // y-ccord of the mid point of the FSLASH stroke

				float fSlashHeight = fSlashBottomY - fSlashTopY;
				double gapThreshold = 0.5 * fSlashHeight;

				// Check to see if the gaps between the mid-points of the top and bottom HLINE's are less than the gap threshold
				matchedP =
					(PenUtil.distanceBetween2Points(fSlashMidX, fSlashMidY, topCircleMidX, topCircleMidY) < gapThreshold) &
					(PenUtil.distanceBetween2Points(fSlashMidX, fSlashMidY, bottomCircleMidX, bottomCircleMidY) < gapThreshold);
			}
		}
		return matchedP;
	} // End of checkForPercentSign()

	// Check for  '&'
	// Check for  '\''

	// Check for  '('
	private boolean checkForLeftParenthesis() {
		boolean matchedP = false;
		// In checkForCShape, a test is done to see if the C shape is curved enough.  If so it is a "c" or "C".
		// If not it is assumed to be a left parenthesis after confirming that the it is a FC_CHAR stroke primitive.
		if (mPenSegments.size() == 1) // Left prenthesis has only one stroke
			matchedP = (mPenSegments.elementAt(0).mPenSegmentCharacter == PenSegment.FC_CHAR);
		
		return matchedP;
	} // checkForLeftParenthesis()

	// Check for  ')' or ','
	// Check for the BC stroke that form ')' or ',' and return either ')', ',' or NUL (Ascii value 0)
	private char checkForRightParenthesisOrComma() {
		char c = '\0';

		if (mPenSegments.size() == 1) // ')' or ',' has only one stroke
			if (mPenSegments.elementAt(0).mPenSegmentCharacter == PenSegment.BC_CHAR) {
				// Check to see the BC is no more than one third the height of the screen from the bottom of the screen
				if (mPenSegments.elementAt(0).mAvgY > Skiggle.DEFAULT_WRITE_PAD_HEIGHT * (2.0/3))
					c = ',';   // If so then it is a comma ','
				else c = ')';  // otherwise it is a right parenthesis ')'
			}

		return c;
	} // End of checkForRightParenthesisOrComma()

	// Check for  '*'

	// Check for  '+'
	private boolean checkForPlusSign() {
		boolean matchedP = false;
		int numOfSegments = mPenSegments.size();


		if (numOfSegments == 2) {
			int hLineIndex = -1; // first stroke E to W
			int vLineIndex = -1; // second stroke N to S

			for (int i = 0; i < numOfSegments; i++) {
				// '+' has only two pen stroke characters
				switch (mPenSegments.elementAt(i).mPenSegmentCharacter) {
				case PenSegment.HLINE_CHAR:
					hLineIndex = i;
					break;
				case PenSegment.VLINE_CHAR:
					vLineIndex = i;
					break;
				default:
					break;
				}
			}
			// Check to make sure that the two component strokes for '+' are there, i.e.,
			// vLineIndex and hLineIndex are both not negative
			if ((hLineIndex >= 0) & (vLineIndex >= 0)) {
				float coords[] = getTopBottomCoordsOfSegment(mPenSegments.elementAt(hLineIndex));		
				float hLineTopX = coords[0]; // x-coord of top end of the HLINE stroke
				float hLineTopY = coords[1]; // y-coord of top end of the HLINE stroke
				float hLineBottomX = coords[2]; // x-coord of bottom end of the HLINE stroke
				float hLineBottomY = coords[3]; // y-coord of bottom end of the HLINE stroke
				float hLineAvgX = (hLineTopX + hLineBottomX)/2; // average of x-coord (mid-point) of HLINE stroke
				float hLineAvgY = (hLineTopY + hLineBottomY)/2; // average of y-coord (mid-point) of HLINE stroke

				coords = getTopBottomCoordsOfSegment(mPenSegments.elementAt(vLineIndex));		
				float vLineTopX = coords[0]; // x-coord of top end of the VLINE stroke
				float vLineTopY = coords[1]; // y-coord of top end of the VLINE stroke
				float vLineBottomX = coords[2]; // x-coord of bottom end of the VLINE stroke
				float vLineBottomY = coords[3]; // y-coord of bottom end of the VLLINE stroke
				float vLineAvgX = (vLineTopX + vLineBottomX)/2; // average of x-coord (mid-point) of VLINE stroke
				float vLineAvgY = (vLineTopY + vLineBottomY)/2; // average of y-coord (mid-point) of VLINE stroke

				float topGap = PenUtil.distanceBetween2Points(vLineTopX, vLineTopY, hLineTopX, hLineTopY);
				float bottomGap = PenUtil.distanceBetween2Points(vLineBottomX, vLineBottomY, hLineBottomX, hLineBottomY);
				float midGap = PenUtil.distanceBetween2Points(vLineAvgX, vLineAvgY, hLineAvgX, hLineAvgY);
				float maxGap = Math.max(topGap, bottomGap);

				// Check to make sure HLINE and VLINE cross sufficiently to form the '+'
				matchedP = (midGap < (.25 * maxGap)); // gap between the mid points of HLINE and VLINE must be small enough
			}
		}
		return matchedP;
	} // End of checkForPlusSign()



	// Check for ',' is done in the same check for ')'

	// Check for  '-' or '_'
	// Check for the HLINE stroke that form '-' or '_' and return either '-', '_' or NUL (Ascii value 0)
	private char checkForDashOrUnderscore() {
		char c = '\0';

		if (mPenSegments.size() == 1) // '-' or '_' has only one stroke
			if (mPenSegments.elementAt(0).mPenSegmentCharacter == PenSegment.FSLASH_CHAR) {
				// Check to see the HLINE is no more than one third the height of the screen from the bottom of the screen
				if (mPenSegments.elementAt(0).mAvgY > Skiggle.DEFAULT_WRITE_PAD_HEIGHT * (2.0/3))
					c = '_';   // If so then it is an underscore '_'
				else c = '-';  // otherwise it is a dash '-'
			}

		return c;
	} // End of checkForDashOrUnderscore()

	// Check for  '.'
	private boolean checkForPeriod() {
		boolean matchedP = false;
		if (mPenSegments.size() == 1) // Back slash has only one stroke
			matchedP = (mPenSegments.elementAt(0).mPenSegmentCharacter == PenSegment.DOT_CHAR);

		return matchedP;
	} // checkForPeriod()

	// Check for  '/'
	private boolean checkForForwardSlash() {
		boolean matchedP = false;
		if (mPenSegments.size() == 1) // Forward slash has only one stroke
			matchedP = (mPenSegments.elementAt(0).mPenSegmentCharacter == PenSegment.FSLASH_CHAR);

		return matchedP;
	} // checkForForwardSlash()

	// Check for  ':'
	// ':' has a two DOT's, one on top of the other
	private boolean checkForColon() {
		boolean matchedP = false;
		int numOfSegments = mPenSegments.size();

		if (numOfSegments == 2) {
			int dot1Index = -1;
			int dot2Index = -1;

			for (int i = 0; i < numOfSegments; i++) {
				// ':' has only two pen stroke characters
				if (mPenSegments.elementAt(i).mPenSegmentCharacter == PenSegment.DOT_CHAR)
					if (dot1Index < 0) dot1Index = i;
					else dot2Index = i;
			}

			// Check to make sure that the two component strokes for ':' are there, i.e.,
			// dot1Index and dot2Index are both not negative
			if ((dot1Index >= 0) & (dot2Index >= 0)) {
				PenSegment pSegments[] = order2PenSegmentsIntoTopBottom(mPenSegments.elementAt(dot1Index), mPenSegments.elementAt(dot2Index));
				// Get x,y of the top DOT
				float topDotX = pSegments[0].mAvgX; // x-coord of the top DOT

				// Get x,y of the bottom DOT
				float bottomDotX = pSegments[0].mAvgX; // x-coord of the bottom DOT

				// Check to see if the DOT is below the dot1
				matchedP = (Math.abs(topDotX - bottomDotX) < 20);
			}
		}

		return matchedP;
	}  // End of checkForColon()

	// Check for  ';'
	// ';' has a  DOT and a BC below it
	private boolean checkForSemiColon() {
		boolean matchedP = false;
		int numOfSegments = mPenSegments.size();

		if (numOfSegments == 2) {
			int dotIndex = -1;
			int bCIndex = -1;

			for (int i = 0; i < numOfSegments; i++) {
				// ';' has only two pen stroke characters
				switch (mPenSegments.elementAt(i).mPenSegmentCharacter) {
				case PenSegment.DOT_CHAR:
					dotIndex = i;
					break;
				case PenSegment.BC_CHAR:
					bCIndex = i;
					break;
				default:
					break;
				}
			}

			// Check to make sure that the two component strokes for ';' are there, i.e.,
			// dotIndex and bCIndex are both not negative
			if ((dotIndex >= 0) & (bCIndex >= 0)) {
				// Get x,y of the top DOT

				float dotX = mPenSegments.elementAt(dotIndex).mAvgX; // x-coord of the top DOT
				//				float dotY = mPenSegments.elementAt(dotIndex).mAvgY; // y-coord of the top DOT

				// Get x,y of the bottom DOT
				float coord[] = getTopBottomCoordsOfSegment(mPenSegments.elementAt(bCIndex));
				float bCTopX = coord[0]; // x-coord of top end of BC
				//				float bCTopY = coord[1]; // y-coord of the top end of BC

				// Check to see if the BC is below the DOT
				matchedP = (Math.abs(dotX - bCTopX) < 20);
			}
		}

		return matchedP;
	}  // End of checkForSemiColon()

	// Check for  '<' or '>'
	// Check for the two strokes that form '<' or '>' and return either '<', '>' or NUL (Ascii value 0)
	private char checkForLessOrGreaterThanSign() {
		char c = '\0';
		int numOfSegments = mPenSegments.size();


		if (numOfSegments == 2) {
			int bSlashIndex = -1; // first stroke NW to SE
			int fSlashIndex = -1; // second stroke NE to SW
			for (int i = 0; i < numOfSegments; i++) {
				// '>' has only two pen stroke characters
				switch (mPenSegments.elementAt(i).mPenSegmentCharacter) {
				case PenSegment.BSLASH_CHAR:
					bSlashIndex = i;
					break;
				case PenSegment.FSLASH_CHAR:
					fSlashIndex = i;
					break;
				default:
					break;
				}
			}
			// Check to make sure that the two component strokes for '<' or '>' are there, i.e.,
			// fSlashIndex and bSlashIndex are both not negative
			if ((bSlashIndex >= 0) & (fSlashIndex >= 0)) {
				PenSegment pSegments[] = order2PenSegmentsIntoTopBottom(mPenSegments.elementAt(bSlashIndex), mPenSegments.elementAt(fSlashIndex));

				// Get x,y of the top slash (could be an FSLASH or BSLASH)
				float coords[] = getTopBottomCoordsOfSegment(pSegments[0]);
				float topSlashTopY = coords[1]; // y-coord of the top of the top slash
				float topSlashBottomX = coords[2]; // x-coord of the bottom of the top slash
				float topSlashBottomY = coords[3]; // y-coord of the bottom of the top slash

				// Get x,y of the bottom slash (could be an FSLASH or BSLASH)
				coords = getTopBottomCoordsOfSegment(pSegments[1]);
				float bottomSlashTopX = coords[0]; // x-coord of the top of the bottom slash
				float bottomSlashTopY = coords[1]; // y-coord of the top of the bottom slash
				float bottomSlashBottomY = coords[3]; // y-coord of the bottom of the bottom slash

				float strokesHeight = bottomSlashBottomY - topSlashTopY;

				// Check to see the bottom of the top slash and the top of the bottom slash are closed enough
				if (PenUtil.distanceBetween2Points(topSlashBottomX, topSlashBottomY, bottomSlashTopX, bottomSlashTopY) < .1 * strokesHeight)
					if (pSegments[0].mPenSegmentCharacter == PenSegment.BSLASH_CHAR) // If the top slash is a BSLASH
						c = '>';                                                  // then it is a '>'
					else c = '<';                                                 // otherwise it is a '<'
			}
		}
		return c;
	} // End of checkForLessOrGreaterThanSign()

	// Check for  '='
	// '=' has a two HLINE's, one on top of the other
	private boolean checkForEqualSign() {
		boolean matchedP = false;
		int numOfSegments = mPenSegments.size();

		if (numOfSegments == 2) {
			int hLine1Index = -1;
			int hLine2Index = -1;

			for (int i = 0; i < numOfSegments; i++) {
				// ':' has only two pen stroke characters
				if (mPenSegments.elementAt(i).mPenSegmentCharacter == PenSegment.HLINE_CHAR)
					if (hLine1Index < 0) hLine1Index = i;
					else hLine2Index = i;
			}

			// Check to make sure that the two component strokes for ':' are there, i.e.,
			// hLine1Index and hLine2Index are both not negative
			if ((hLine1Index >= 0) & (hLine2Index >= 0)) {
				PenSegment pSegments[] = order2PenSegmentsIntoTopBottom(mPenSegments.elementAt(hLine1Index), mPenSegments.elementAt(hLine2Index));
				// Get x,y of the top HLINE
				float topHLineX = pSegments[0].mAvgX; // x-coord of the top HLINE

				// Get x,y of the bottom HLINE
				float bottomHLineX = pSegments[0].mAvgX; // x-coord of the bottom HLINE

				// Check to see if the HLINE is below the hLine1
				matchedP = (Math.abs(topHLineX - bottomHLineX) < 20);
			}
		}

		return matchedP;
	}  // End of checkForEqualSign()

	// Check for '>' is done in the same check for '<'

	// Check for  '?'
	// Check for  '@'

	// Check for 'I', '[' or ']'
	// Check for the two strokes that form 'I', '[' or ']' and return either '[', ']' or NUL (Ascii value 0)
	private char checkForCapitalIOrLeftOrRightSquareBracket() {
		char c = '\0';
		int numOfSegments = mPenSegments.size();

		if (numOfSegments == 3) {
			int topHLineIndex = -1; // top HLINE
			int bottomHLineIndex = -1; // bottom HLINE
			int vLineIndex = -1; // VLINE

			for (int i = 0; i < numOfSegments; i++) {
				// 'I', '[' or ']'has only three pen stroke characters
				switch (mPenSegments.elementAt(i).mPenSegmentCharacter) {
				case PenSegment.HLINE_CHAR:
					if (topHLineIndex < 0)
						topHLineIndex = i;
					else
						bottomHLineIndex = i;
					break;
				case PenSegment.VLINE_CHAR:
					vLineIndex = i;
					break;
				default:
					break;
				}
			}

			// Check to make sure that the three component strokes for 'I', '[' or ']' are there, i.e.,
			// topHLineIndex, vLineIndex, and bottomHLineIndex are all not negative
			if ((topHLineIndex >= 0) & (vLineIndex >= 0) & (bottomHLineIndex >= 0)) {
				// Get the top and bottom HLINE strokes of '[' or ']'
				PenSegment topBottomHLineSegments[] = 
					order2PenSegmentsIntoTopBottom(mPenSegments.elementAt(topHLineIndex),
							mPenSegments.elementAt(bottomHLineIndex));

				float coords[] = getLeftRightCoordsOfSegment(topBottomHLineSegments[0]);
				float topHLineLeftX = coords[0]; // x-coord of the left point of the top HLINE
				float topHLineLeftY = coords[1]; // y-coord of the left point of the top HLINE
				float topHLineRightX = coords[2]; // x-coord of the right point of the top HLINE
				float topHLineRightY = coords[3]; // y-coord of the right point of the top HLINE
				float topHLineMidX = (topHLineLeftX + topHLineRightX)/2; // x-coord of the mid-point of the top HLINE
				float topHLineMidY = (topHLineLeftY + topHLineRightY)/2; // y-coord of the mid-point of the top HLINE

				coords = getLeftRightCoordsOfSegment(topBottomHLineSegments[1]);
				float bottomHLineLeftX = coords[0]; // x-coord of the left point of the bottom HLINE
				float bottomHLineLeftY = coords[1]; // y-coord of the left point of the bottom HLINE
				float bottomHLineRightX = coords[2]; // x-coord of the right point of the bottom HLINE
				float bottomHLineRightY = coords[3]; // y-coord of the right point of the bottom HLINE
				float bottomHLineMidX = (bottomHLineLeftX + bottomHLineRightX)/2; // x-coord of the mid-point of the bottom HLINE
				float bottomHLineMidY = (bottomHLineLeftY + bottomHLineRightY)/2; // y-coord of the mid-point of the botom HLINE

				// Get x,y of top and bottom of VLINE
				coords = getTopBottomCoordsOfSegment(mPenSegments.elementAt(vLineIndex));

				float vLineTopX = coords[0]; // x-coord of top end of the VLINE stroke
				float vLineTopY = coords[1]; // y-coord of top end of the VLINE stroke
				float vLineBottomX = coords[2]; // x-coord of bottom end of the VLINE stroke
				float vLineBottomY = coords[3]; // y-coord of bottom end of the VLINE stroke

				float vLineHeight = Math.abs(vLineBottomY - vLineTopY);
				double gapThreshold = 0.1 * vLineHeight;

				// Check to see if the gaps between the left points of the top and bottom HLINE's 
				// and top and bottom, respectively, of VLINE are less than the gap threshold
				if
				((PenUtil.distanceBetween2Points(vLineTopX, vLineTopY, topHLineMidX, topHLineMidY) < gapThreshold) &
						(PenUtil.distanceBetween2Points(vLineBottomX, vLineBottomY, bottomHLineMidX, bottomHLineMidY) < gapThreshold))
					c = 'I'; // the three strokes form a capital I
				else if 
				((PenUtil.distanceBetween2Points(vLineTopX, vLineTopY, topHLineRightX, topHLineRightY) < gapThreshold) &
						(PenUtil.distanceBetween2Points(vLineBottomX, vLineBottomY, bottomHLineRightX, bottomHLineRightY) < gapThreshold))
					c = ']'; // the three strokes form a right square bracket
				else if
				((PenUtil.distanceBetween2Points(vLineTopX, vLineTopY, topHLineLeftX, topHLineLeftY) < gapThreshold) &
						(PenUtil.distanceBetween2Points(vLineBottomX, vLineBottomY, bottomHLineLeftX, bottomHLineLeftY) < gapThreshold))
					c = '['; // the three strokes form a left square bracket
			}
		}
		return c;
	} // End of checkForCapitalILeftOrRightSquareBracket()

	// Check for  '\\'
	private boolean checkForBackSlash() {
		boolean matchedP = false;
		if (mPenSegments.size() == 1) // Back slash has only one stroke
			matchedP = (mPenSegments.elementAt(0).mPenSegmentCharacter == PenSegment.BSLASH_CHAR);

		return matchedP;
	} // checkForBackSlash()

	// Check for  ']'
	// Check for  '^'
	// Check for  '_'
	// Check for  '`'
	
	// Check for  '{'
	// *************TO BE COMPLETED*************//
	private boolean checkForLeftCurlyBrace() {
		boolean matchedP = false;
//		if (mPenSegments.size() == 2) // Left curly brace has two strokes - two "c" stacked on top of one another
//			matchedP = (mPenSegments.elementAt(0).mPenSegmentCharacter == PenSegment.VLINE_CHAR);

		return matchedP;
	} // checkForLeftCurlyBrace()
	
	// Check for  '|'
	private boolean checkForVerticalBar() {
		boolean matchedP = false;
		if (mPenSegments.size() == 1) // Vertical bar has only one stroke
			matchedP = (mPenSegments.elementAt(0).mPenSegmentCharacter == PenSegment.VLINE_CHAR);

		return matchedP;
	} // checkForVerticalBar()

	// Check for  '}'
	// Check for  '~'

	// Match the pen strokes to the given character c
	private boolean matchCharacter(char c) {
		boolean foundP = false;
		char penChar = '?';

		char ch = '\0'; // used in check for 'K', 'k', '>' or '<'

		switch (c) {
		/*****************/
		/* Digits 0 to 9 */
		/*****************/
		case '0':
			break;
		case '1':
			break;
		case '2':
			//			foundP = checkFor2();
			//			if (foundP) penChar = '2';
			break;
		case '3':
			foundP = checkFor3();
			if (foundP) penChar = '3';
			break;
		case '4':
			foundP = checkFor4();
			if (foundP) penChar = '4';
			break;
		case '5':
			foundP = checkFor5();
			if (foundP) penChar = '5';
			break;
		case '6':
			break;
		case '7':
			foundP = checkFor7();
			if (foundP) penChar = '7';
			break;
		case '8':
			break;
		case '9':
			foundP = checkFor9();
			if (foundP) penChar = '9';
			break;

			/**********************************************/
			/* Letters A-Z, c, k, o, p, u, v, w, x, and z */
			/**********************************************/
		case 'A':
			foundP = checkForCapitalA();
			if (foundP) penChar = 'A';
			break;
		case 'B': 
			foundP = checkForCapitalB();
			if (foundP) penChar = 'B';
			break;
		case 'C': // Check for capital 'C' and small 'c'; same stroke but 'c' is smaller
		case 'c':
			foundP = checkForCShape(); //checkForCapitalC();
			if (foundP) 
				if (isSmallLetter()) penChar = 'c';
				else penChar = 'C';
			break;
		case 'D':
			foundP = checkForCapitalD();
			if (foundP) penChar = 'D';
			break;
		case 'E':
			foundP = checkForCapitalE();
			if (foundP) penChar = 'E';
			break;
		case 'F':
			foundP = checkForCapitalF();
			if (foundP) penChar = 'F';
			break;
		case 'G':
			foundP = checkForCapitalG();
			if (foundP) penChar = 'G';
			break;
		case 'H':
			foundP = checkForCapitalH();
			if (foundP) penChar = 'H';
			break;
		case 'I': // Check for 'I', '[' or ']'
		case '[':
		case ']':
			ch = checkForCapitalIOrLeftOrRightSquareBracket();
			if (ch != '\0') {
				foundP = true;
				penChar = ch;
			}
			break;
		case 'J':
			foundP = checkForCapitalJ();
			if (foundP) penChar = 'J';
			break;
		case 'K':
		case 'k':
			// Check for 'K' or 'k'
			ch = checkForCapitalOrSmallK();
			if (ch != '\0') {
				foundP = true;
				penChar = ch;
			}
			break;
		case 'L':
			foundP = checkForCapitalL();
			if (foundP) penChar = 'L';
			break;
		case 'M':
			foundP = checkForCapitalM();
			if (foundP) penChar = 'M';
			break;
		case 'N':
			foundP = checkForCapitalN();
			if (foundP) penChar = 'N';
			break;
		case 'O': // Check for capital 'O' and small 'o'; same stroke but 'o' is smaller
		case 'o':
			foundP = checkForOShape();
			if (foundP) 
				if (isSmallLetter()) penChar = 'o';
				else penChar = 'O';
			break;
		case 'P': // Check for capital 'P' and small 'p'; same stroke but 'P' is at least .6 the height of the writing area
		case 'p':
			foundP = checkForPShape();
			if (foundP)
				if ((mPenStrokesMaxY - mPenStrokesMinY) > (Skiggle.DEFAULT_WRITE_PAD_HEIGHT * .6))
					penChar = 'P';
				else penChar = 'p';
			break;
		case 'Q':
			foundP = checkForCapitalQ();
			if (foundP) penChar = 'Q';
			break;
		case 'R':
			foundP = checkForCapitalR();
			if (foundP) penChar = 'R';
			break;
		case 'S':
		case 's':
			foundP = checkForSShape();
			if (foundP) 
				if (isSmallLetter()) penChar = 's';
				else penChar = 'S';
			break;
		case 'T':
			foundP = checkForCapitalT();
			if (foundP) penChar = 'T';
			break;
		case 'U': // Check for capital 'U' and small 'u'; same segment but 'u' is smaller
		case 'u':
			foundP = checkForUShape();
			if (foundP) 
				if (isSmallLetter()) penChar = 'u';
				else penChar = 'U';
			break;
		case 'V': // Check for capital 'V' and small 'u'; same segment but 'v' is smaller
		case 'v':
			foundP = checkForVShape();
			if (foundP) 
				if (isSmallLetter()) penChar = 'v';
				else penChar = 'V';
			break;
		case 'W': // Check for capital 'W' and small 'w'; same segment but 'w' is smaller
		case 'w':
			foundP = checkForWShape();
			if (foundP) 
				if (isSmallLetter()) penChar = 'w';
				else penChar = 'W';
			break;
		case 'X': // Check for capital 'X' and small 'x'; same segment but 'x' is smaller
		case 'x':
			foundP = checkForXShape();
			if (foundP) 
				if (isSmallLetter()) penChar = 'x';
				else penChar = 'X';
			break;
		case 'Y':
			foundP = checkForCapitalY();
			if (foundP) penChar = 'Y';
			break;
		case 'Z': // Check for capital 'Z' and small 'z'; same segment but 'z' is smaller
		case 'z':
			foundP = checkForZShape();
			if (foundP) 
				if (isSmallLetter()) penChar = 'z';
				else penChar = 'Z';
			break;

			/********************************************************/
			/* Letters a-z except for c, k, o, p, u, v, w, x, and z */
			/********************************************************/
		case 'a':
			foundP = checkForSmallA();
			if (foundP) penChar = 'a';
			break;
		case 'b':
			foundP = checkForSmallB();
			if (foundP) penChar = 'b';
			break;
			// Small 'c' is checked in the case for capital 'C'
		case 'd':
			foundP = checkForSmallD();
			if (foundP) penChar = 'd';
			break;
		case 'e':
			break;
		case 'f':
			break;
		case 'g':
			break;
		case 'h':
			break;
		case 'i':
			foundP = checkForSmallI();
			if (foundP) penChar = 'i';
			break;
		case 'j':
			break;
			// Small 'k' is checked in the case for capital 'K'			
		case 'l':
			break;
		case 'm':
			break;
		case 'n':
			break;
			// Small 'o' is checked in the case for capital 'O'
			// Small 'p' is checked in the case for capital 'P'
		case 'q':
			foundP = checkForSmallQ();
			if (foundP) penChar = 'q';
			break;
		case 'r':
			break;
			// Small 's' is checked in the case for capital 'S'
		case 't':
			foundP = checkForSmallT();
			if (foundP) penChar = 't';
			break;
			// Small 'u' is checked in the case for capital 'U'
			// Small 'v' is checked in the case for capital 'V'
			// Small 'w' is checked in the case for capital 'W'
			// Small 'x' is checked in the case for capital 'X'
		case 'y':
			foundP = checkForSmallY();
			if (foundP) penChar = 'y';
			break;
			// Small 'z' is checked in the case for capital 'Z'

			/********************************/
			/* Check for special characters */
			/********************************/
		case ' ':
			break;
		case '!':
			foundP = checkForExclamationMark();
			if (foundP) penChar = '!';
			break;
		case '\"': // Check for quote, "
			break; 
		case '#':
			foundP = checkForHashSymbol();
			if (foundP) penChar = '#';
			break;
		case '$':
			break;
		case '%':
			foundP = checkForPercentSign();
			if (foundP) penChar = '%';
			break;
		case '&':
			break;
		case '\'': // Check for single quote or prime, '
			break;
		case '(':
			foundP = checkForLeftParenthesis();
			if (foundP) penChar = '(';
			break;
		case ')': // Check for right parenthesis ')' and comma ','
		case ',':
			// Check for ')' or ','
			ch = checkForRightParenthesisOrComma();
			if (ch != '\0') {
				foundP = true;
				penChar = ch;
			}
			break;
		case '*':
			break;
		case '+':
			foundP = checkForPlusSign();
			if (foundP) penChar = '+';
			break;		
			// ',' is checked in the case for ')'

		case '-': // Check for dash '-' and underscore '_'
		case '_':
			// Check for '-' or '_'
			ch = checkForDashOrUnderscore();
			if (ch != '\0') {
				foundP = true;
				penChar = ch;
			}
			break;
		case '.':
			foundP = checkForPeriod();
			if (foundP) penChar = '.';
			break;
		case '/':
			foundP = checkForForwardSlash();
			if (foundP) penChar = '/';
			break;
		case ':':
			foundP = checkForColon();
			if (foundP) penChar = ':';
			break;
		case ';':
			foundP = checkForSemiColon();
			if (foundP) penChar = ';';
			break;
		case '<': // Check for '<' or '>'
		case '>':
			// Check for '<' or '>'
			ch = checkForLessOrGreaterThanSign();
			if (ch != '\0') {
				foundP = true;
				penChar = ch;
			}
			break;
		case '=':
			foundP = checkForEqualSign();
			if (foundP) penChar = '=';
			break;
			// '>' is checked in the case for '<'
		case '?':
			break;
		case '@':
			break;
			// '[' is checked in the case for 'I', '[', and ']'
			// ']' is checked in the case for 'I', '[', and ']'
		case '\\':
			foundP = checkForBackSlash();
			if (foundP) penChar = '\\';
			break;
			// '>' is checked in the case for '<'
		case '^':
			foundP = checkForCaret();
			if (foundP) penChar = '^';
			break;
			// '_' is checked in the case for '-'
		case '`':
			break;
		case '{':
			foundP = checkForLeftCurlyBrace();
			if (foundP) penChar = '{';
			break;
		case '|':
			foundP = checkForVerticalBar();
			if (foundP) penChar = '|';
			break;
		case '}':
			break;
		case '~':
			break;
		default:

		}
		mPenCharacter = penChar;
		return foundP;
	}

	public void findMatchingCharacter (Canvas canvas, Paint textPaint) {
		getCharacterCandidates(canvas, textPaint);

		int len = mPenCharacterCandidates.length();
		for (int i = 0; i < len; i++) {
			if (matchCharacter(mPenCharacterCandidates.charAt(i)))
				break;
		}

		// mPenCharacter.showStrokes(mCanvas);
		// Print the stroke statistics on the screen
		// mPenStroke.printPenStrokeStatsOnScreen(this, mCanvas, mPaint, mTextPaint);
		// Reset the pen color back to default
		// mPaint.setColor(DEFAULT_PEN_COLOR);
		// mPenStroke.printMatchedCharacter(mCanvas, mPenCharacter.mPenCharacter, 100.0F, 300.0F, mTextPaint);
		printPenCharacter(canvas, 50.0F, 400.0F, textPaint);
		printPenCharacterCandidates(canvas, 60.0F, 400.0F, textPaint);
		
		printCharacterSegmentsData();

		//		PenUtil.printString(Float.toString(mPenSegments.elementAt(0).mPointsY[0]), 50.0F, 410.F,
		//				mPenSegments.elementAt(0).mBoundingRectF, canvas, textPaint);

		// PenSegment pSegment = mPenSegments.elementAt(0);
		// pSegment.printSegmentStats(canvas, textPaint);
	}


	// Methods for printing PenCharacter
	private void printString(String str, Canvas canvas, float x, float y, Paint paint) {

		if (str != null) {

			Paint tempPaint = new Paint();
			tempPaint.set(paint);

			//tempPaint.setColor(Skiggle.DEFAULT_CANVAS_COLOR + 2);
			//			tempPaint.setStrokeWidth(Skiggle.DEFAULT_STROKE_WIDTH);
			//			canvas.drawRect(mBoundingRectF, tempPaint);

			tempPaint.setColor(0xFFFFFFFF);
			canvas.drawRect(x, y-mFontSize-3, x+200, y+3, tempPaint);

			tempPaint.setTextSize(mFontSize);
			tempPaint.setColor(0xFFFF0000);
			canvas.drawText(str, x, y, tempPaint);
		}
	}

	public void printPenCharacter(Canvas canvas, float x, float y, Paint paint) {

		if (mPenCharacter != null) {
			printString(mPenCharacter.toString(), canvas, x, y, paint);
		}
	}

	public void printPenCharacterCandidates(Canvas canvas, float x, float y, Paint paint) {

		if (mPenCharacterCandidates != null) {
			printString(mPenCharacterCandidates, canvas, x, y, paint);
		}
	}
	
	
	public void printSegmentCharacters(RectF mBoundingRectF, Canvas canvas, Paint textPaint) {

		int numOfSegments = mPenSegments.size();
		PenSegment segment;
		String str = "Len:" + Integer.toString(numOfSegments);
		for (int i =0; i < numOfSegments; i++) {
			segment = mPenSegments.elementAt(i);
			str = str + ", " + segment.mPenSegmentCharacter;
			//Log.i(PenCharacter.TAG, "Segment " + i);
			//segment.printSegmentPointsData();
			
		}
		
		PenUtil.printString(str, 10, 15, mBoundingRectF, canvas, textPaint);
	/*	
		ConsoleHandler cons = new ConsoleHandler();
		cons.publish(new LogRecord(Level.SEVERE, "abcde"));
		cons.flush();
		System.out.print("ABCDEFG");
		Log.i(PenCharacter.TAG, "ABBBB");
		try {
		BufferedWriter f = new BufferedWriter(new FileWriter("c:\\Users\\Willie\\Programming\\android.log"));
		f.write("ABCDEFGH");
		f.close();
		} 
		catch (IOException e) {
			System.out.println("IO Error");

		}
	*/	
		

	}
	
	public void printCharacterSegmentsData() {

		int numOfSegments = mPenSegments.size();
		PenSegment segment;
		//String str = "Len:" + Integer.toString(numOfSegments);
		//Log.i(PenCharacter.TAG, "Character: " + mPenCharacter);
		for (int i =0; i < numOfSegments; i++) {
			segment = mPenSegments.elementAt(i);
			//str = str + ", " + segment.mPenSegmentCharacter;
			//Log.i(PenCharacter.TAG, "Segment " + i);
			//segment.printSegmentPointsData();
			
		}
	}
}
