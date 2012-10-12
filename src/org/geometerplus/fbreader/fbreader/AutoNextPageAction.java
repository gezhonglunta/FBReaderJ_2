package org.geometerplus.fbreader.fbreader;

import java.util.Timer;
import java.util.TimerTask;

import org.geometerplus.fbreader.fbreader.FBAction;
import org.geometerplus.fbreader.fbreader.FBReaderApp;

import android.R.bool;

class AutoNextPageAction extends FBAction {
	private boolean isOn = false;

	AutoNextPageAction(FBReaderApp fbreader, boolean isOn) {
		super(fbreader);
		this.isOn = isOn;
	}

	private static Timer timer;

	@Override
	public boolean isVisible() {
		if (isOn) {
			if (timer==null) {
				return false;
			}
		}
		else {
			if (timer!=null) {
				return false;
			}
		}
		return true;
	}

	@Override
	protected void run(Object... params) {
		if (timer == null) {
			timer = new Timer();
			timer.schedule(new TimerTask() {
				@Override
				public void run() {
					((FBReaderApp) FBReaderApp.Instance()).StartAutoNextPage();
				}
			}, 1000, 1000);
		} else {
			timer.cancel();
			timer = null;
		}
	}
}
