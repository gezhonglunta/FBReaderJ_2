package org.geometerplus.android.fbreader;

import org.geometerplus.fbreader.fbreader.FBReaderApp;

import android.content.Intent;

public class ShowBookEditAction extends FBAndroidAction {

	ShowBookEditAction(FBReader baseActivity, FBReaderApp fbreader) {
		super(baseActivity, fbreader);
	}

	@Override
	public boolean isEnabled() {
		return true;
	}

	@Override
	public boolean isVisible() {
		return true;
	}

	@Override
	protected void run(Object... params) {
		Intent intent = new Intent(BaseActivity.getApplicationContext(),
				BookEditActivity.class);
		BaseActivity.startActivityForResult(intent, FBReader.REQUEST_BOOK_INFO);
	}

}
