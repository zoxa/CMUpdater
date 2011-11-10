package ca.zoxa.cyanogen.updater;

import android.content.Context;
import android.database.Cursor;
import android.widget.SimpleCursorTreeAdapter;

public class CMListAdapter extends SimpleCursorTreeAdapter
{
	private final NightliesAdapter	na;

	public CMListAdapter( Context context, Cursor cursor, int groupLayout, String[] groupFrom,
			int[] groupTo, int childLayout, String[] childFrom, int[] childTo, NightliesAdapter na )
	{
		super( context, cursor, groupLayout, groupFrom, groupTo, childLayout, childFrom, childTo );
		this.na = na;
	}

	@Override
	protected Cursor getChildrenCursor( Cursor dCursor )
	{
		// TODO Auto-generated method stub
		return null;
	}

	// protected View getGroupView(int groupPosition, boolean isExpanded, View convertView,
	// ViewGroup parent)

	// getChildView

}
