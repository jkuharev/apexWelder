/** apexWelder, apexWelder, Feb 20, 2017*/
package apexWelder;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintStream;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.log4j.Logger;

import apex3d.Apex3dDataReadingMode;
import apex3d.Apex3dXmlReader;
import apexWelder.log.Log;
import de.mz.jk.jsix.cli.SimpleCLI;
import de.mz.jk.jsix.libs.XJava;
import de.mz.jk.jsix.ui.ProgressWindow;

/**
 * <h3>{@link Apex3dXmlToCsvConverter}</h3>
 * @author jkuharev
 * @version Feb 20, 2017 12:49:54 PM
 */
public class Apex3dXmlToCsvConverter implements Runnable
{
	private static Logger log = Log.getLogger( Apex3dXmlToCsvConverter.class );
	
	public static class ConverterCLI extends SimpleCLI
	{
		@Override public Option[] getDefaultOptions()
		{
			return new Option[] {
					Option.builder( "i" ).argName( "xml file" ).desc( "file path to Apex3D.xml result file" ).hasArg().build(),
					Option.builder( "o" ).argName( "csv file" ).desc( "file path to export the data" ).hasArg().build(),
					Option.builder( "f" ).argName( "function" ).desc( "the function type to be exported: LE, HE or LM." ).hasArg().build(),
					getDefaultHelpOption(),
			};
		}

		@Override public String getExecutableJarFileName()
		{
			return Apex3dXmlToCsvConverter.class.getSimpleName();
		}
	}

	public static void main(String[] args) throws Exception
	{
		Log.init();
		ConverterCLI converterCLI = new ConverterCLI();
		try
		{
			if (args.length < 1) throw new Exception( "please specify appropriate parameters!" );

			log.info( "parsing command line options: " + XJava.joinArray( args, " " ) );
			CommandLine cli = converterCLI.parseCommandLine( args );

			File fi = new File( cli.getOptionValue( "i" ) );
			File fo = new File( cli.getOptionValue( "o" ) );
			Apex3dDataReadingMode rm = Apex3dDataReadingMode.fromString( cli.getOptionValue( "f" ) );

			if (!fi.exists()) throw new Exception( "File ''" + fi.toString() + "'' not found." );
			Apex3dXmlToCsvConverter conv = new Apex3dXmlToCsvConverter( fi, fo, rm );

			conv.run();
		}
		catch (Exception e)
		{
			log.error( "execution stopped: " + e.getMessage() );
			log.debug( "execution stopped: ", e );
			converterCLI.showHelp();
		}
	}

	private File xmlFile = null;
	private File csvFile = null;
	private Apex3dDataReadingMode mode = null;

	public Apex3dXmlToCsvConverter(File xmlFile, File csvFile, Apex3dDataReadingMode mode)
	{
		this.xmlFile = xmlFile;
		this.csvFile = csvFile;
		this.mode = mode;
	}

	@Override public void run()
	{
		try
		{
			Apex3dXmlReader reader = new Apex3dXmlReader( xmlFile );
			reader.setReadingMode( mode );

			PrintStream writer = new PrintStream( new BufferedOutputStream( new FileOutputStream( csvFile ) ) );
			Object[] colNames = reader.getColumnNames();
			writer.println( XJava.joinArray( colNames, "," ) );
			ProgressWindow pbar = new ProgressWindow( "exporting data ..." );
			pbar.setProgressMaxValue( 1000000 );
			pbar.startProgress( "parsing data ... " );
			for(int i=0; reader.hasNext(); i++)
			{
				String[] cells = reader.getNextAsArray();
				String row = XJava.joinArray( cells, "," );
				writer.println( row );
				if (i % 10000 == 0) pbar.setProgressValue( i % 1000000 );
			}
			pbar.dispose();
			writer.flush();
			writer.close();
			log.info( reader.getRowCount() + " data rows written to csv file." );
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
}
