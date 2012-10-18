package org.geometerplus.android.fbreader;

import org.geometerplus.fbreader.fbreader.FBReaderApp;

import android.content.Intent;

public class ShowBookEditAction extends FBAndroidAction {

	ShowBookEditAction(FBReader baseActivity, FBReaderApp fbreader) {
		super(baseActivity, fbreader);
	}

	@Override
	public boolean isEnabled() {
		if (Reader.Model != null && Reader.Model.Book != null) {
			String extension = Reader.Model.Book.File.getExtension();
			if (extension != null && extension.toUpperCase().equals("TXT")) {
				return true;
			}
		}
		return false;
	}

	@Override
	protected void run(Object... params) {
		Intent intent = new Intent(BaseActivity.getApplicationContext(),
				BookEditActivity.class);
		BaseActivity.startActivityForResult(intent, FBReader.REQUEST_BOOK_INFO);
	}

}
