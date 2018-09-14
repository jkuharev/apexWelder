/** apexWelder, apexWelder.log, Feb 20, 2017*/
package apexWelder.log;

import java.io.File;

import org.apache.log4j.Logger;
import org.apache.log4j.xml.DOMConfigurator;

/**
 * <h3>{@link Log}</h3>
 * @author jkuharev
 * @version Feb 20, 2017 1:05:18 PM
 */
public class Log
{
	private final static File log_file = new File( "log4j.xml" );
	public final static String version = "2017-02-20";

	public static void init()
	{
		try
		{
			if (log_file.exists())
				DOMConfigurator.configure( log_file.getPath() );
			else
				DOMConfigurator.configure( ClassLoader.getSystemResource( log_file.getName() ) );
		}
		catch (Exception e)
		{
			System.err.println( "failed to configure log4j logger!" );
			e.printStackTrace();
		}
	}

	public static Logger getLogger(Class parent)
	{
		return Logger.getLogger( parent );
	}
}
