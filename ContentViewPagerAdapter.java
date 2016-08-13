package ...

import java.util.ArrayList;

import com.evados.chinesecalendar.R;

import android.support.v4.view.PagerAdapter;
import android.view.View;
import android.view.ViewGroup;

public class ContentViewPagerAdapter extends PagerAdapter {

	// This holds all the currently displayable mViews, in order from left to
	// right.
	private volatile ArrayList<View> mViews = new ArrayList<View>();

	// -- construction
	public ContentViewPagerAdapter() {
	}

	// -----------------------------------------------------------------------------
	// Used by ViewPager. "Object" represents the page; tell the ViewPager where
	// the
	// page should be displayed, from left-to-right. If the page no longer
	// exists,
	// return POSITION_NONE.
	@Override
	public int getItemPosition(Object object) {
		// System.err.println("onPage getItemPosition " + object.hashCode() + "
		// tag " + ((View) object).getTag(R.id.fragment_tag) + " idx " +
		// mViews.indexOf(object));
		int index = mViews.indexOf(object);
		if (index == -1)
			return POSITION_NONE;
		else
			return index;
	}

	// -----------------------------------------------------------------------------
	// Used by ViewPager. Called when ViewPager needs a page to display; it is
	// our job
	// to add the page to the container, which is normally the ViewPager itself.
	// Since
	// all our pages are persistent, we simply retrieve it from our "mViews"
	// ArrayList.
	@Override
	public Object instantiateItem(ViewGroup container, int position) {
		View v = mViews.get(position);
		System.err.println(
				"onPage instantiateItem " + ((Object) v).hashCode() + " tag " + v.getTag(R.id.fragment_tag) + " pos " +
						position);

		// Prevent crash on "java.lang.IllegalStateException: The specified
		// child already has a parent. You must call removeView() on the child's
		// parent first",
		// which may occur if we slide forward and back too quick
		// try {
		container.addView(v);
		// } catch (IllegalStateException e) {
		// e.printStackTrace();
		// }

		return v;
	}

	// -----------------------------------------------------------------------------
	// Used by ViewPager. Called when ViewPager no longer needs a page to
	// display; it
	// is our job to remove the page from the container, which is normally the
	// ViewPager itself. Since all our pages are persistent, we do nothing to
	// the
	// contents of our "mViews" ArrayList.
	@Override
	public void destroyItem(ViewGroup container, int position, Object object) {
		System.err.println("onPage destroyItem " + object.hashCode() + " tag "
				+ ((View) object).getTag(R.id.fragment_tag) + " pos " + position);
		container.removeView((View) object);
	}

	// -----------------------------------------------------------------------------
	// Used by ViewPager; can be used by app as well.
	// Returns the total number of pages that the ViewPage can display. This
	// must
	// never be 0.
	@Override
	public int getCount() {
		return mViews.size();
	}

	// -----------------------------------------------------------------------------
	// Used by ViewPager.
	@Override
	public boolean isViewFromObject(View view, Object object) {
		return view == object;
	}

	// Add "view" to right end of "mViews".
	// Returns the position of the new view.
	// The app should call this to add pages; not used by ViewPager.
	public int addView(View v) {
		return addView(v, mViews.size());
	}

	// -----------------------------------------------------------------------------
	// Add "view" at "position" to "mViews".
	// Returns position of new view.
	// The app should call this to add pages; not used by ViewPager.
	public int addView(View v, int position) {
		mViews.add(position, v);
		return position;
	}

	/**
	 * @return True if oldPosition and newPosition does not exceed View array
	 *         size minus 1, otherwise false
	 */
	public boolean moveView(int oldPosition, int newPosition) {
		if (oldPosition < mViews.size() || newPosition < mViews.size()) {
			View view = mViews.get(oldPosition);
			mViews.remove(oldPosition);
			mViews.add(newPosition, view);
			return true;
		}

		return false;
	}

	// -----------------------------------------------------------------------------
	// Removes "view" from "mViews".
	// Retuns position of removed view.
	// The app should call this to remove pages; not used by ViewPager.
	public int removeView(View v) {
		return removeView(mViews.indexOf(v));
	}

	// -----------------------------------------------------------------------------
	// Removes the "view" at "position" from "mViews".
	// Retuns position of removed view.
	// The app should call this to remove pages; not used by ViewPager.
	public int removeView(int position) {
		// ViewPager doesn't have a delete method; the closest is to set the
		// adapter
		// again. When doing so, it deletes all its mViews. Then we can delete
		// the view
		// from from the adapter and finally set the adapter to the pager again.
		// Note
		// that we set the adapter to null before removing the view from
		// "mViews"
		// - that's
		// because while ViewPager deletes all its mViews, it will call
		// destroyItem which
		// will in turn cause a null pointer ref.
		// mViewPager.setAdapter(null);
		mViews.remove(position);
		// notifyDataSetChanged();
		// mViewPager.setAdapter(this);

		return position;
	}

	// -----------------------------------------------------------------------------
	// Returns the "view" at "position".
	// The app should call this to retrieve a view; not used by ViewPager.
	public View getView(int position) {
		return mViews.get(position);
	}

	public ArrayList<View> getViews() {
		return mViews;
	}

}
