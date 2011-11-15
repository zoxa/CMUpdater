package ca.zoxa.cyanogen.updater;

import java.util.Date;

import android.content.Context;
import android.database.Cursor;
import android.database.CursorIndexOutOfBoundsException;
import android.database.SQLException;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.TextView;

public class CMListAdapter extends BaseExpandableListAdapter
{
	private final Context		context;
	private String				device;

	private DownloadsRecord[]	downloads;
	private ChangeLogRecord[][]	changelogs;

	private final String		TAG				= "CMAdapter";

	// URLS
	public static String		CHANGELOG_URL	= "http://review.cyanogenmod.com/%d";

	public static String		DOWNLOAD_LIST	= "http://download.cyanogenmod.com/?device=%s&type=%s";
	public static String		DOWNLOAD_URL	= "http://download.cyanogenmod.com/get/%s";

	public CMListAdapter( final Context context, String device )
	{
		super();
		this.context = context;
		this.device = device;
		fillData();
	}

	public ChangeLogRecord getChild( int downloadPosition, int changelogPostion )
	{
		return ( downloadPosition < downloads.length && changelogPostion < changelogs[downloadPosition].length ) ? changelogs[downloadPosition][changelogPostion]
				: new ChangeLogRecord();
	}

	public long getChildId( int downloadPosition, int changelogPostion )
	{
		return downloadPosition * 1000 + changelogPostion;
	}

	public View getChildView( int downloadPosition, int changelogPostion, boolean isLastChild,
			View convertView, ViewGroup parent )
	{
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
		project.setText( '(' + cl.project + ')' );

		// XXX: check if this is:
		// device change: project contains {device}
		if ( cl.project.toLowerCase().contains( device ) )
		{
			convertView.setBackgroundColor( this.context.getResources().getColor(
					R.color.cl_device_bg ) );
		}
		// translation : subject ranslat, ocaliz, ussian, hinese, ortug, erman, wedish, typo

		return convertView;
	}

	public int getChildrenCount( int downloadPosition )
	{
		return ( downloadPosition < downloads.length ) ? changelogs[downloadPosition].length : 0;
	}

	public DownloadsRecord getGroup( int downloadPosition )
	{
		return downloadPosition < downloads.length ? downloads[downloadPosition]
				: new DownloadsRecord();
	}

	public int getGroupCount()
	{
		return downloads.length;
	}

	public long getGroupId( int downloadPosition )
	{
		return downloadPosition;
	}

	public View getGroupView( int downloadPosition, boolean isExpanded, View convertView,
			ViewGroup parent )
	{
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

		// XXX: add a check here if this installed / downloaded + include colors for types

		return convertView;
	}

	public boolean hasStableIds()
	{
		// TODO Auto-generated method stub
		Log.d( TAG, "hasStableIds" );
		return true;
	}

	public boolean isChildSelectable( int downloadPosition, int changelogPostion )
	{
		return true;
	}

	/**
	 * Load downloads and changelogs with data
	 */
	private void fillData()
	{
		NightliesAdapter na = new NightliesAdapter( context );
		try
		{
			na.read();

			// get Downloads
			Cursor cur_dl = na.getDownloadsCursor();
			cur_dl.moveToFirst();

			// we will start db records from 1: 0 reserved for our custom record
			int count_dl = cur_dl.getCount() + 1;
			if ( count_dl > 1 )
			{
				downloads = new DownloadsRecord[count_dl];
				changelogs = new ChangeLogRecord[count_dl][];

				DownloadsRecord dr;
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
				changelogs[0] = fillChangeLogData( na, dateFrom, dateTo );

				for ( int i = 1; i < count_dl && !cur_dl.isAfterLast(); i++ )
				{
					dr = new DownloadsRecord();
					dr.filename = cur_dl.getString( cur_dl
							.getColumnIndex( NightliesAdapter.CM_FILENAME ) );
					dr.type = cur_dl.getString( cur_dl.getColumnIndex( NightliesAdapter.CM_TYPE ) );
					dr.md5sum = cur_dl.getString( cur_dl
							.getColumnIndex( NightliesAdapter.CM_MD5SUM ) );
					dr.size = cur_dl.getString( cur_dl.getColumnIndex( NightliesAdapter.CM_SIZE ) );
					dateFrom = cur_dl.getLong( cur_dl.getColumnIndex( NightliesAdapter.CM_DATE ) );
					dr.date_added = new Date( dateFrom );
					downloads[i] = dr;

					// get change log data
					changelogs[i] = fillChangeLogData( na, dateFrom, dateTo );

					dateTo = dateFrom;
					cur_dl.moveToNext();
				}
			}
			else
			{
				downloads = new DownloadsRecord[1];
				changelogs = new ChangeLogRecord[1][0];

				// no records found
				DownloadsRecord dr = new DownloadsRecord();
				dr.filename = "No Records found, please refresh";
				dr.type = "nightly";
				dr.size = "";
				dr.md5sum = "";
				dr.date_added = null;
				downloads[0] = dr;
			}
		}
		catch ( SQLException e )
		{
			// data base does not exist or cannot be opened for read
			downloads = new DownloadsRecord[1];
			changelogs = new ChangeLogRecord[1][0];

			// no records found
			DownloadsRecord dr = new DownloadsRecord();
			dr.filename = "Problem with DB, please refresh";
			dr.type = "nightly";
			dr.size = "";
			dr.md5sum = "";
			dr.date_added = null;
			downloads[0] = dr;

			Log.e( TAG, "DB Fail", e );
		}
		catch ( CursorIndexOutOfBoundsException e )
		{
			downloads = new DownloadsRecord[1];
			changelogs = new ChangeLogRecord[1][0];

			// no records found
			DownloadsRecord dr = new DownloadsRecord();
			dr.filename = "Problem with DB, please refresh";
			dr.type = "nightly";
			dr.size = "";
			dr.md5sum = "";
			dr.date_added = null;
			downloads[0] = dr;

			Log.e( TAG, "DB Fail", e );
		}
		finally
		{
			if ( na != null )
			{
				na.close();
			}
		}
	}

	/**
	 * Get Change Log records from DB and form nice array set
	 * 
	 * @param na
	 *            database connection
	 * @param dateFrom
	 *            start date for changes
	 * @param dateTo
	 *            end date for changes
	 * @return array of change log records
	 */
	private ChangeLogRecord[] fillChangeLogData( NightliesAdapter na, long dateFrom, long dateTo )
	{
		Cursor cur_cl = na.getChangeLogCursor( dateFrom, dateTo );
		int count_cl = cur_cl.getCount();
		ChangeLogRecord cl;
		ChangeLogRecord[] changelogs = new ChangeLogRecord[count_cl];
		for ( int k = 0; k < count_cl && cur_cl.moveToNext(); k++ )
		{
			cl = new ChangeLogRecord();
			cl.id = cur_cl.getInt( cur_cl.getColumnIndex( NightliesAdapter.CL_ID ) );
			cl.project = cur_cl.getString( cur_cl.getColumnIndex( NightliesAdapter.CL_PROJECT ) );
			cl.subject = cur_cl.getString( cur_cl.getColumnIndex( NightliesAdapter.CL_SUBJECT ) );
			cl.last_updated = new Date( cur_cl.getLong( cur_cl
					.getColumnIndex( NightliesAdapter.CL_LAST_UPDATED ) ) );
			changelogs[k] = cl;
		}
		return changelogs;
	}

	@Override
	public void notifyDataSetChanged()
	{
		Log.i( TAG, "refresh data set called" );
		fillData();
		super.notifyDataSetChanged();
	}

	public void setDevice( String device )
	{
		this.device = device;
	}

	/**
	 * Set for Downloads Record
	 */
	public class DownloadsRecord
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
	public class ChangeLogRecord
	{
		public int		id;
		public String	project;
		public String	subject;
		public Date		last_updated;
	}
}
