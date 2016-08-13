package ...

import java.util.ArrayList;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

...

import android.app.Activity;
import android.os.Looper;
import android.support.v4.view.ViewPager;
import android.view.View;

public class ContentViewPagerManager {

	public enum SlideViewDirection {
		UNDEFINED,
		// = date decrement
		SLIDE_RIGHT,
		// = date increment
		SLIDE_LEFT
	}

	// Must be odd number (side pages + 1 middle page)
	public static final int PAGE_COUNT = 5;
	public static final int MIDDLE_PAGE_INDEX = PAGE_COUNT / 2;

	private final int mTagKey;
	private final int mPageResourceId;
	private volatile boolean mPageIsIdle;
	private volatile boolean mPageIsProcessing;

	private final ThreadPoolExecutor mExecutor = new ThreadPoolExecutor(1, 1, 0L, TimeUnit.MILLISECONDS,
			new LinkedBlockingQueue<Runnable>());

	private final ContentViewPager mViewPager;
	private volatile ContentViewPagerAdapter mAdapter;
	private final ContentViewPagerWaitDialog mWaitDialog;
	private ViewPager.OnPageChangeListener mPageChangeListener;
	private final Activity mActivity;
	private final MeApplication mApplication;

	public ContentViewPagerManager(Activity activity, ContentViewPager viewPager, int pageResourceId, int tagKey) {
		mActivity = activity;
		mViewPager = viewPager;
		mPageResourceId = pageResourceId;
		mTagKey = tagKey;
		mWaitDialog = new ContentViewPagerWaitDialog(activity);

		mApplication = MeApplication.getInstance();
	}

	public void initViewPager(boolean async) {

		mAdapter = new ContentViewPagerAdapter();
		mViewPager.setAdapter(mAdapter);

		mPageIsIdle = true;

		addPagesSync();

		mViewPager.setAllowSwipe(true);
		// set some bigger limit to prevent automatic calling destroyItem() in adapter,
		// we want control it thru notifyDataSetChanged()
		mViewPager.setOffscreenPageLimit(PAGE_COUNT);
		// prevent calling onPageSelected on setCurrentItem
		mViewPager.setOnPageChangeListener(null);
		mViewPager.setCurrentItem(MIDDLE_PAGE_INDEX, false);

		// TODO: fix case, when we quickly slide forward and back at once and can reach last page
		mPageChangeListener = new ViewPager.OnPageChangeListener() {

			private SlideViewDirection mDirection;

			@Override
			public void onPageSelected(int position) {
				if (mPageIsIdle) {
					return;
				}

				// set current timestamp
				ArrayList<DateTime> timestamps = MeApplication.getInstance().getPageTimestamps();
				DateTime dateTime = getNextTimestamp();
				System.err.println("onPageSelected pos " + position + " ts " + dateTime);
				mApplication.setTimestamp(dateTime, mDirection);

				DateTime dt;
				if (mDirection == SlideViewDirection.SLIDE_RIGHT) {
					dt = timestamps.get(0);
				} else {
					dt = timestamps.get(timestamps.size() - 1);
				}

				// normal case
				if ((position > 0 && mDirection == SlideViewDirection.SLIDE_RIGHT)
						|| (position < timestamps.size() - 1 && mDirection == SlideViewDirection.SLIDE_LEFT)) {
					addNextPageAsync(mDirection, dt);
				}
				// we slide too fast or period limits reached
				else if ((position == 0 && mDirection == SlideViewDirection.SLIDE_RIGHT)
						|| (position == timestamps.size() - 1 && mDirection == SlideViewDirection.SLIDE_LEFT)) {
					mWaitDialog.show();

					int count = MIDDLE_PAGE_INDEX;

					if (mPageIsProcessing) {
						--count;
					}

					for (int i = 0; i < count; i++) {
						System.out.println("onPage second page");
						addNextPageAsync(mDirection, dt);
					}
				}
			}

			// called after onPageScrollStateChanged SCROLL_STATE_DRAGGING
			@Override
			public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
				mDirection = (mViewPager.getCurrentItem() == position) ? SlideViewDirection.SLIDE_LEFT
						: SlideViewDirection.SLIDE_RIGHT;
			}

			@Override
			public void onPageScrollStateChanged(int state) {
				System.err.println("onPageScrollStateChanged STATE " + state);

				switch (state) {
				case ViewPager.SCROLL_STATE_IDLE:
					mPageIsIdle = true;
					notifyDataSetChanged();
					break;
				case ViewPager.SCROLL_STATE_DRAGGING:
					mPageIsIdle = false;
					break;
				case ViewPager.SCROLL_STATE_SETTLING:
					ContentLayout contentLayout = (ContentLayout) WrapperLayout.getCurrentLayout();
					contentLayout.scrollTo(0, 0);
					break;
				default:
					break;
				}
			}

			private DateTime getNextTimestamp() {
				ArrayList<DateTime> timestamps = MeApplication.getInstance().getPageTimestamps();
				DateTime dateTime = MeApplication.getInstance().getTimestamp();
				int index = -1;

				for (int i = 0; i < timestamps.size(); i++) {
					if (dateTime.equals(timestamps.get(i))) {
						index = i;
						break;
					}
				}

				System.err
						.println("onPage getNextTimestamp curr " + dateTime + " arr " + timestamps + " index " + index);

				if (index == 0 || index == timestamps.size() - 1) {
					return dateTime;
				}

				index = (mDirection == SlideViewDirection.SLIDE_RIGHT) ? --index : ++index;
				return timestamps.get(index);
			}
		};

		mViewPager.setOnPageChangeListener(mPageChangeListener);

	}

	public synchronized void addNextPage(SlideViewDirection direction, final DateTime dateTime) {
		ContentLayout contentLayout = (ContentLayout) WrapperLayout.getCurrentLayout();
		DateTime dt = contentLayout.getNextTimestamp(direction, dateTime);

		if (dt == null || !dt.isInLimits()) {
			return;
		}

		createNextPage(direction);
	}

	public void addNextPageAsync(final SlideViewDirection direction, final DateTime dateTime) {

		mExecutor.execute(new Runnable() {
			@Override
			public void run() {
				mPageIsProcessing = true;
				System.out
						.println("onPage addNextPageAsync " + dateTime + " thread " + Thread.currentThread().getName());
				addNextPage(direction, dateTime);
				mPageIsProcessing = false;

				if (mExecutor.getQueue().size() == 0) {
					mWaitDialog.dismiss();
				}
			}
		});

	}

	public void notifyDataSetChanged() {

		if ((isMainThread() && mPageIsProcessing) || (!isMainThread() && !mPageIsIdle)
				|| mExecutor.getQueue().size() > 0) {
			return;
		}

		System.out.println("onPage notifyDataSetChanged thread " + Thread.currentThread().getName());

		if (isMainThread()) {
			mAdapter.notifyDataSetChanged();
			mWaitDialog.dismiss();
		} else {
			Callable<Void> callable = new Callable<Void>() {
				@Override
				public Void call() {
					mAdapter.notifyDataSetChanged();
					return null;
				}
			};

			FutureTask<Void> task = new FutureTask<Void>(callable);
			mActivity.runOnUiThread(task);

			try {
				// wait until runOnUiThread returns and block next task beginning
				task.get();
			} catch (InterruptedException e1) {
				e1.printStackTrace();
			} catch (ExecutionException e1) {
				e1.printStackTrace();
			}
		}

	}

	public ContentViewPagerAdapter getAdapter() {
		return mAdapter;
	}

	public ContentViewPager getViewPager() {
		return mViewPager;
	}

	public void setCurrentItem(int item, boolean smoothScroll) {
		// reset listener to prevent calling onPageSelected
		mViewPager.setOnPageChangeListener(null);
		mViewPager.setCurrentItem(item, smoothScroll);
		mViewPager.setOnPageChangeListener(mPageChangeListener);
	}

	public static boolean isMainThread() {
		return Thread.currentThread().equals(Looper.getMainLooper().getThread());
	}

	private void addPagesSync() {
		System.err.println("addPagesSync " + Thread.currentThread().getName());

		for (int i = 0; i < PAGE_COUNT; i++) {
			View view = inflatePage();
			view.setTag(mTagKey, i);
			mAdapter.getViews().add(view);
			mApplication.sendLayoutDidChange(view);
		}

		notifyDataSetChanged();
	}

	private void createNextPage(SlideViewDirection direction) {
		mApplication.setSlideViewDirection(direction);

		switch (direction) {
		case SLIDE_RIGHT:
			mAdapter.removeView(mAdapter.getCount() - 1);
			mAdapter.addView(inflatePage(), 0);
			mAdapter.getView(0).setTag(R.id.fragment_tag, -1);
			mApplication.sendLayoutDidChange(mAdapter.getView(0));
			break;

		case SLIDE_LEFT:
			mAdapter.removeView(0);
			mAdapter.addView(inflatePage());
			int lastIndex = mAdapter.getCount() - 1;
			mAdapter.getView(lastIndex).setTag(R.id.fragment_tag, lastIndex + 1);
			mApplication.sendLayoutDidChange(mAdapter.getView(lastIndex));
			break;

		default:
			break;
		}
	}

	private View inflatePage() {
		return mActivity.getLayoutInflater().inflate(mPageResourceId, mViewPager, false);
	}
}
