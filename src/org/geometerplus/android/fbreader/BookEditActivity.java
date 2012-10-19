package org.geometerplus.android.fbreader;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import org.LXZKimage.R;
import org.geometerplus.fbreader.bookmodel.BookModel;
import org.geometerplus.fbreader.fbreader.FBReaderApp;
import org.geometerplus.fbreader.library.Book;
import org.geometerplus.zlibrary.core.resources.ZLResource;

import android.R.bool;
import android.app.Activity;
import android.os.Bundle;
import android.text.Editable;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

public class BookEditActivity extends Activity implements OnClickListener {
	private TextView txtTitle;
	private EditText editContent;
	private Book book;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.book_edit);
		txtTitle = (TextView) findViewById(R.id.bookEditTitle);
		editContent = (EditText) findViewById(R.id.bookEditContent);
		// final ZLResource buttonResource = ZLResource.resource("dialog")
		// .getResource("button");
		// final Button button = (Button) findViewById(R.id.book_edit_save);
		// button.setText(buttonResource.getResource("saveBook").getValue());
		// button.setOnClickListener(this);
		txtTitle.setText("文本编辑");
		setResult(FBReader.RESULT_RELOAD_BOOK_ALL);
		LoadBook();
	}

	private void LoadBook() {
		if (FBReaderApp.Instance() instanceof FBReaderApp) {
			BookModel mode = ((FBReaderApp) FBReaderApp.Instance()).Model;
			if (mode != null) {
				if (mode.Book != null) {
					book = mode.Book;
					try {
						InputStream stream = book.File.getInputStream();
						byte[] data = new byte[(int) book.File.size()];
						stream.read(data);
						String str = new String(data, book.getEncoding());
						editContent.setText(str);
						stream.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
		}
	}

	private void SaveBook() {
		try {
			BufferedWriter stream = new BufferedWriter(new FileWriter(
					book.File.getPath()));
			Editable text = editContent.getText();
			// if (text != null) {
			// final char d = 0x0d;
			// final char a = 0x0a;
			// char last = 0;
			// for (int i = 0; i < text.length(); i++) {
			// char ch = text.charAt(i);
			// if (ch == a) {
			// if (last == 0 || last != d) {
			// text.insert(i, "\r");
			// }
			// }
			// last = ch;
			// }
			// }
			stream.write(text.toString());
			stream.flush();
			stream.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void onClick(View v) {
		SaveBook();
	}

	@Override
	public void onBackPressed() {
		super.onBackPressed();
		SaveBook();
	}
}
