package org.geometerplus.android.fbreader;

import org.geometerplus.fbreader.bookmodel.BookModel;
import org.geometerplus.fbreader.fbreader.FBReaderApp;
import org.geometerplus.zlibrary.core.resources.ZLResource;

import android.app.AlertDialog;
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
		BookModel mode = ((FBReaderApp) FBReaderApp.Instance()).Model;
		if (mode != null) {
			if (mode.Book != null) {
				long size = mode.Book.File.size();
				if (size > 1024 * 1024) {
					// BaseActivity.showMessage("文件太大无法编辑，请在PC上编辑");
					final ZLResource resource = ZLResource
							.resource("messageBoxStr");
					new AlertDialog.Builder(BaseActivity)
							.setTitle(resource.getResource("mess").getValue())
							.setMessage(
									resource.getResource("fileTooBig")
											.getValue()).show();
					return;
				}
			}
		}
		Intent intent = new Intent(BaseActivity.getApplicationContext(),
				BookEditActivity.class);
		BaseActivity.startActivityForResult(intent, FBReader.REQUEST_BOOK_INFO);
	}
}
