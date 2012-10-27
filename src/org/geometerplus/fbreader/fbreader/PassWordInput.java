package org.geometerplus.fbreader.fbreader;

import org.LXZKimage.R;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.View;

public class PassWordInput extends Dialog implements
		android.view.View.OnClickListener {

	public PassWordInput(Context context) {
		super(context);
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.pass_word_input);
	}

	@Override
	public void onClick(View v) {

	}

}
