package org.geometerplus.android.fbreader;

import org.LXZKimage.R;
import org.geometerplus.fbreader.fbreader.FBReaderApp;

import android.view.View;
import android.widget.RelativeLayout;

public class ShowMessagePopup extends PopupPanel {
	final static String ID = "ShowMessagePopup";

	ShowMessagePopup(FBReaderApp fbReader) {
		super(fbReader);
	}

	@Override
	public void createControlPanel(FBReader activity, RelativeLayout root) {
		if (myWindow != null && activity == myWindow.getActivity()) {
			return;
		}

		myWindow = new PopupWindow(activity, root, PopupWindow.Location.Floating,
				true);

		final View layout = activity.getLayoutInflater().inflate(
				R.layout.navigate, myWindow, false);
		myWindow.addView(layout);
	}

	@Override
	public String getId() {
		return ID;
	}

	@Override
	protected void update() {

	}

	public void show(String mess) {
		if (myWindow == null || myWindow.getVisibility() == View.GONE) {
			Application.showPopup(ID);
		}
	}

}
