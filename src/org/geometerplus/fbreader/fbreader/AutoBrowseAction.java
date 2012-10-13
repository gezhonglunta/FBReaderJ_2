package org.geometerplus.fbreader.fbreader;

import java.util.Timer;
import java.util.TimerTask;

import org.geometerplus.fbreader.fbreader.FBAction;
import org.geometerplus.fbreader.fbreader.FBReaderApp;

class AutoBrowseAction extends FBAction {
	private boolean actionOn = false;

	AutoBrowseAction(FBReaderApp fbreader, boolean actionOn) {
		super(fbreader);
		this.actionOn = actionOn;
	}

	private static Timer timer;

	@Override
	public boolean isVisible() {
		if (actionOn) {
			if (timer==null) {
				return true;
			}
		}
		else {
			if (timer!=null) {
				return true;
			}
		}
		return false;
	}

	@Override
	protected void run(Object... params) {
		if (timer == null) {
			timer = new Timer();
			timer.schedule(new TimerTask() {
				@Override
				public void run() {
					FBReaderApp.Instance().StartAutoBrowse(3);
				}
			}, 1000, 1000);
		} else {
			timer.cancel();
			timer = null;
		}
	}
}
