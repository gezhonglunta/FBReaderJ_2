package org.geometerplus.fbreader.fbreader;

import org.geometerplus.fbreader.fbreader.FBAction;
import org.geometerplus.fbreader.fbreader.FBReaderApp;
import org.geometerplus.fbreader.fbreader.ScrollingPreferences.AutoBrowseInterval;
import org.geometerplus.fbreader.fbreader.ScrollingPreferences.AutoBrowseUnit;

public class AutoBrowseAction extends FBAction {
	private boolean actionOn = false;

	AutoBrowseAction(FBReaderApp fbreader, boolean actionOn) {
		super(fbreader);
		this.actionOn = actionOn;
	}

	@Override
	public boolean isVisible() {
		if (actionOn) {
			if (thread == null) {
				return true;
			}
		} else {
			if (thread != null) {
				return true;
			}
		}
		return false;
	}

	private static AutoBrowseThread thread;

	public boolean isAutoBrowsing() {
		return !(thread == null);
	}

	@Override
	protected void run(Object... params) {
		if (thread == null) {
			thread = new AutoBrowseThread();
			AutoBrowseInterval intervalEnum = ScrollingPreferences.Instance().AutoBrowseIntervalOption
					.getValue();
			int interval = Integer.parseInt(intervalEnum.name()
					.replace("S", "")) * 1000;
			FBReaderApp.Instance().addTimerTask(thread, interval);
		} else {
			FBReaderApp.Instance().removeTimerTask(thread);
			thread = null;
		}
	}

	static class AutoBrowseThread implements Runnable {
		public void run() {
			boolean result = FBReaderApp
					.Instance()
					.StartAutoBrowse(
							ScrollingPreferences.Instance().AutoBrowseUnitOption
									.getValue() == AutoBrowseUnit.Page ? FBView.ScrollingMode.SCROLL_PERCENTAGE
									: FBView.ScrollingMode.SCROLL_LINES);
			if (!result) {
				FBReaderApp.Instance().StopAutoBrowse();
			}
		}
	}
}
