package com.evados.chinesecalendar.ui.view.contentviewpager;

import com.evados.chinesecalendar.R;

import android.app.AlertDialog;
import android.content.Context;
import android.os.Bundle;
import android.view.Window;

public class ContentViewPagerWaitDialog extends AlertDialog {

	public ContentViewPagerWaitDialog(Context context) {
		super(context);
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		super.onCreate(savedInstanceState);
		setCancelable(false);
		setCanceledOnTouchOutside(false);
		setContentView(R.layout.layout_wait_dialog);
	}

}
