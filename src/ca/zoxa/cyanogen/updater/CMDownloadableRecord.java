package ca.zoxa.cyanogen.updater;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class CMDownloadableRecord
{
    // List of Type for downloads
    public enum TypeList
    {
        nightly, RC, stable
    }

    // Download type
    public TypeList type;

    // File name of download
    public String filename;

    // File md5 checksum
    public String md5sum;

    // File size as string
    public String size;

    // public Date of addition of the file
    public Date date;

    public CMDownloadableRecord()
    {}

    public CMDownloadableRecord( TypeList type, String filename, String md5sum, String size,
            Date date )
    {
        this.type = type;
        this.filename = filename;
        this.md5sum = md5sum;
        this.size = size;
        this.date = date;
    }

    public CMDownloadableRecord( String type, String filename, String md5sum, String size,
            String date ) throws ParseException
    {
        setType( type );
        this.filename = filename;
        this.md5sum = md5sum;
        this.size = size;
        setDate( date );
    }

    public TypeList setType( String type )
    {
        if ( type.equalsIgnoreCase( "nightly" ) )
        {
            this.type = TypeList.nightly;
        }
        else if ( type.equalsIgnoreCase( "RC" ) )
        {
            this.type = TypeList.RC;
        }
        else if ( type.equalsIgnoreCase( "stable" ) )
        {
            this.type = TypeList.stable;
        }
        return this.type;
    }

    public String setFileName( String filename )
    {
        this.filename = filename.trim();
        return this.filename;
    }

    public String setMd5Sum( String md5sum )
    {
        this.md5sum = md5sum.trim();
        return this.md5sum;
    }

    public String setSize( String size )
    {
        this.size = size.trim();
        return this.size;
    }

    public Date setDate( String date ) throws ParseException
    {
        // 2011-11-06 17:34:50
        SimpleDateFormat format = new SimpleDateFormat( "yyyy-MM-dd HH:mm:ss" );
        this.date = format.parse( date );
        return this.date;
    }
}
