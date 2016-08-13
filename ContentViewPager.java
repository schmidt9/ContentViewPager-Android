package ...

import android.content.Context;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.view.MotionEvent;

public class ContentViewPager extends ViewPager {

	private boolean mAllowSwipe = true;

	public ContentViewPager(Context context) {
		super(context);
	}

	public ContentViewPager(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	@Override
	public boolean onInterceptTouchEvent(MotionEvent arg0) {
		return mAllowSwipe ? super.onInterceptTouchEvent(arg0) : false;
	}

	public void setAllowSwipe(boolean allowSwipe) {
		mAllowSwipe = allowSwipe;
	}

}
