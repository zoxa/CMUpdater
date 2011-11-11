package ca.zoxa.cyanogen.updater;

import java.util.Date;

import android.content.Context;
import android.database.Cursor;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.TextView;

public class CMListAdapter extends BaseExpandableListAdapter
{
	private final Context		context;
	private DownloadsRecord[]	downloads;
	private ChangeLogRecord[][]	changelogs;

	private final String		TAG	= "CMAdapter";

	public CMListAdapter( final Context context )
	{
		super();
		this.context = context;
		fillData();
	}

	public ChangeLogRecord getChild( int downloadPosition, int changelogPostion )
	{
		Log.d( TAG, "getChild" );
		return ( downloadPosition < downloads.length && changelogPostion < changelogs[downloadPosition].length ) ? changelogs[downloadPosition][changelogPostion]
				: new ChangeLogRecord();
	}

	public long getChildId( int downloadPosition, int changelogPostion )
	{
		Log.d( TAG, "getChildId" );
		return downloadPosition * 1000 + changelogPostion;
	}

	public View getChildView( int downloadPosition, int changelogPostion, boolean isLastChild,
			View convertView, ViewGroup parent )
	{
		Log.d( TAG, "getChildView" );
		if ( convertView == null )
		{
			LayoutInflater infalInflater = (LayoutInflater) context
					.getSystemService( Context.LAYOUT_INFLATER_SERVICE );
			convertView = infalInflater.inflate( R.layout.changelog_row, null );
		}

		ChangeLogRecord cl = getChild( downloadPosition, changelogPostion );

		TextView subject = (TextView) convertView.findViewById( R.id.cl_subject );
		subject.setText( cl.subject );

		TextView project = (TextView) convertView.findViewById( R.id.cl_project );
		project.setText( cl.project );

		// XXX: check if this is:
		// device change: {subject+project} contains _{device} {device}: {device}Cap
		// translation : subject ranslat, ocaliz, ussian, hinese, ortug, erman, wedish, typo

		return convertView;
	}

	public int getChildrenCount( int downloadPosition )
	{
		Log.d( TAG, "getChildrenCount" );
		return ( downloadPosition < downloads.length ) ? changelogs[downloadPosition].length : 0;
	}

	public DownloadsRecord getGroup( int downloadPosition )
	{
		Log.d( TAG, "getGroup" );
		return downloadPosition < downloads.length ? downloads[downloadPosition]
				: new DownloadsRecord();
	}

	public int getGroupCount()
	{
		Log.d( TAG, "getGroupCount" );
		return downloads.length;
	}

	public long getGroupId( int downloadPosition )
	{
		Log.d( TAG, "getGroupId" );
		return downloadPosition;
	}

	public View getGroupView( int downloadPosition, boolean isExpanded, View convertView,
			ViewGroup parent )
	{
		Log.d( TAG, "getGroupView" );
		if ( convertView == null )
		{
			LayoutInflater infalInflater = (LayoutInflater) context
					.getSystemService( Context.LAYOUT_INFLATER_SERVICE );
			convertView = infalInflater.inflate( R.layout.downloads_row, null );
		}
		DownloadsRecord dr = getGroup( downloadPosition );

		TextView filename = (TextView) convertView.findViewById( R.id.dl_filename );
		filename.setText( dr.filename );

		TextView size = (TextView) convertView.findViewById( R.id.dl_size );
		size.setText( dr.size );

		if ( dr.date_added != null )
		{
			TextView date = (TextView) convertView.findViewById( R.id.dl_date_added );
			date.setText( dr.date_added.toLocaleString() );
		}

		// XXX: add a check here if this installed / downloaded

		return convertView;
	}

	public boolean hasStableIds()
	{
		Log.d( TAG, "hasStableIds" );
		return false;
	}

	public boolean isChildSelectable( int downloadPosition, int changelogPostion )
	{
		Log.d( TAG, "isChildSelectable" );
		// TODO Auto-generated method stub
		return false;
	}

	/**
	 * Load downloads and changelogs with data
	 */
	private void fillData()
	{
		NightliesAdapter na = new NightliesAdapter( context );
		na.read();

		// get Downloads
		Cursor cur_dl = na.getDownloadsCursor();
		cur_dl.moveToFirst();

		// we will start db records from 1: 0 reserved for our custom record
		int count_dl = cur_dl.getCount() + 1;
		downloads = new DownloadsRecord[count_dl];
		changelogs = new ChangeLogRecord[count_dl][];

		Log.d( TAG, "downloads count " + count_dl );

		DownloadsRecord dr;
		ChangeLogRecord cl;
		Cursor cur_cl;
		int count_cl;
		long dateTo = ( new Date() ).getTime();
		long dateFrom = cur_dl.getLong( cur_dl.getColumnIndex( NightliesAdapter.CM_DATE ) );

		// empty first record
		dr = new DownloadsRecord();
		dr.filename = "next nightly";
		dr.type = "nightly";
		dr.size = "";
		dr.md5sum = "";
		dr.date_added = new Date();
		downloads[0] = dr;

		// get change log data
		cur_cl = na.getChangeLogCursor( dateFrom, dateTo );
		count_cl = cur_cl.getCount();

		Log.d( TAG, "changelog count " + count_cl );

		changelogs[0] = new ChangeLogRecord[count_cl];
		for ( int k = 0; k < count_cl && cur_cl.moveToNext(); k++ )
		{
			cl = new ChangeLogRecord();
			cl.id = cur_cl.getInt( cur_cl.getColumnIndex( NightliesAdapter.CL_ID ) );
			cl.project = cur_cl.getString( cur_cl.getColumnIndex( NightliesAdapter.CL_PROJECT ) );
			cl.subject = cur_cl.getString( cur_cl.getColumnIndex( NightliesAdapter.CL_SUBJECT ) );
			cl.last_updated = new Date( cur_cl.getLong( cur_cl
					.getColumnIndex( NightliesAdapter.CL_LAST_UPDATED ) ) );
			changelogs[0][k] = cl;
		}

		Log.d( TAG, "cPos:" + cur_dl.getPosition() );
		for ( int i = 1; i < count_dl && !cur_dl.isAfterLast(); i++ )
		{
			Log.i( "CAdapter", "i: " + i + " cPos:" + cur_dl.getPosition() );
			dr = new DownloadsRecord();
			dr.filename = cur_dl.getString( cur_dl.getColumnIndex( NightliesAdapter.CM_FILENAME ) );
			dr.type = cur_dl.getString( cur_dl.getColumnIndex( NightliesAdapter.CM_TYPE ) );
			dr.md5sum = cur_dl.getString( cur_dl.getColumnIndex( NightliesAdapter.CM_MD5SUM ) );
			dr.size = cur_dl.getString( cur_dl.getColumnIndex( NightliesAdapter.CM_SIZE ) );
			dateFrom = cur_dl.getLong( cur_dl.getColumnIndex( NightliesAdapter.CM_DATE ) );
			dr.date_added = new Date( dateFrom );
			downloads[i] = dr;

			// get change log data
			cur_cl = na.getChangeLogCursor( dateFrom, dateTo );
			count_cl = cur_cl.getCount();
			cur_cl.moveToFirst();

			Log.d( TAG, "changelog count " + count_cl );

			changelogs[i] = new ChangeLogRecord[count_cl];
			for ( int k = 0; k < count_cl && !cur_cl.isAfterLast(); k++ )
			{
				cl = new ChangeLogRecord();
				cl.id = cur_cl.getInt( cur_cl.getColumnIndex( NightliesAdapter.CL_ID ) );
				cl.project = cur_cl
						.getString( cur_cl.getColumnIndex( NightliesAdapter.CL_PROJECT ) );
				cl.subject = cur_cl
						.getString( cur_cl.getColumnIndex( NightliesAdapter.CL_SUBJECT ) );
				cl.last_updated = new Date( cur_cl.getLong( cur_cl
						.getColumnIndex( NightliesAdapter.CL_LAST_UPDATED ) ) );
				changelogs[i][k] = cl;
				cur_cl.moveToNext();
			}
			dateTo = dateFrom;
			cur_dl.moveToNext();
		}

		na.close();
	}

	/**
	 * Set for Downloads Record
	 */
	@SuppressWarnings("unused")
	private class DownloadsRecord
	{
		public String	type;
		public String	filename;
		public String	md5sum;
		public String	size;
		public Date		date_added;
	}

	/**
	 * Set for Change Log Record
	 */
	@SuppressWarnings("unused")
	private class ChangeLogRecord
	{
		public int		id;
		public String	project;
		public String	subject;
		public Date		last_updated;
	}
}
