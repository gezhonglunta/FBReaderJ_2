package org.geometerplus.zlibrary.ui.android.view;

public class SmoothShiftAnimationProvider extends ShiftAnimationProvider {
	private float mySpeedFactor;

	SmoothShiftAnimationProvider(BitmapManager bitmapManager) {
		super(bitmapManager);
	}

	@Override
	protected void startAnimatedScrollingInternal(int speed) {
		mySpeedFactor = 5;
		mySpeed = 20;
		doStep();
	}

	@Override
	void doStep() {
		if (!getMode().Auto) {
			return;
		}

		switch (myDirection) {
		case leftToRight:
			myEndX -= (int) mySpeed;
			break;
		case rightToLeft:
			myEndX += (int) mySpeed;
			break;
		case up:
			myEndY += (int) mySpeed;
			break;
		case down:
			myEndY -= (int) mySpeed;
			break;
		}
		final int bound;
		if (getMode() == Mode.AnimatedScrollingForward) {
			bound = myDirection.IsHorizontal ? myWidth : myHeight;
		} else {
			bound = 0;
		}
		if (mySpeed > 0) {
			if (getScrollingShift() >= bound) {
				if (myDirection.IsHorizontal) {
					myEndX = myStartX + bound;
				} else {
					myEndY = myStartY + bound;
				}
				terminate();
				return;
			}
		} else {
			if (getScrollingShift() <= -bound) {
				if (myDirection.IsHorizontal) {
					myEndX = myStartX - bound;
				} else {
					myEndY = myStartY - bound;
				}
				terminate();
				return;
			}
		}
		mySpeed *= mySpeedFactor;
	}
}
