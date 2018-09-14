/** apexWelder, apexWelder, Feb 15, 2017*/
package apexWelder;

import java.io.File;
import java.io.PrintStream;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.cli.CommandLine;
import org.apache.log4j.Logger;

import apex3d.Apex3dDataReadingMode;
import apex3d.Apex3dXmlReader;
import apex3d.SonarMath;
import apexWelder.log.Log;
import de.mz.jk.jsix.libs.XJava;

/**
 * <h3>{@link apexWelder}</h3>
 * @author jkuharev
 * @version Feb 15, 2017 11:17:25 AM
 */
public class apexWelder implements Runnable
{
	public static final String build = "20170410";
	private static Logger log = Log.getLogger( apexWelder.class );

	private final static DecimalFormat df4 = new DecimalFormat( "0.0000" );
	private final static DecimalFormat df3 = new DecimalFormat( "0.000" );

	private Apex3dWeldingMode weldingMode = Apex3dWeldingMode.predictMobility;

	/**
	* @param args
	*/
	public static void main(String[] args)
	{
		Log.init();
		initCli( args );
	}

	public static void mainTest(String... args)
	{
		//main( "-mStart 404 -mEnd 885 -f1 /Volumes/DAT/Users/jkuharev/Desktop/SONAR/F1F2/F1_Apex3D.xml -f2 /Volumes/DAT/Users/jkuharev/Desktop/SONAR/F1F2/F2_Apex3D.xml -fo /Volumes/DAT/Users/jkuharev/Desktop/SONAR/F1F2/Welded_Apex3D.xml"
		//		.split( "\\s" ) );
		main( "-mode reduce -f1 /Volumes/DAT/Users/jkuharev/Desktop/SONAR/F1F2/F1_Apex3D.xml -f2 /Volumes/DAT/Users/jkuharev/Desktop/SONAR/F1F2/F2_Apex3D.xml -fo /Volumes/DAT/Users/jkuharev/Desktop/SONAR/F1F2/Welded_Apex3D.xml"
				.split( "\\s" ) );
	}

	private static void initCli(String[] args)
	{
		ApexWelderCLI welderCLI = new ApexWelderCLI();
		try{
			if (args.length < 1) throw new Exception( "please specify appropriate parameters!" );
		
			log.info( "parsing command line options:\n\t" + XJava.joinArray( args, " " ) );
			CommandLine cli = welderCLI.parseCommandLine( args );

			File f1 = new File( cli.getOptionValue( "f1" ) );
			File f2 = new File( cli.getOptionValue( "f2" ) );
			File fo = new File( cli.getOptionValue( "fo" ) );
			
			if (!f1.exists()) throw new Exception( "File ''" + f1.toString() + "'' not found." );
			if (!f2.exists()) throw new Exception( "File ''" + f2.toString() + "'' not found." );
			
			Apex3dWeldingMode mode = Apex3dWeldingMode.fromString( cli.getOptionValue( "mode", "reduce" ) );

			log.info( "operating mode: " + mode.toString() );
			
			// here we should have everything we need
			apexWelder welder = new apexWelder( f1, f2, fo, mode );
			
			// did you set mass range?
			if (cli.hasOption( "mStart" ) && cli.hasOption( "mEnd" ))
			{
				double startMass = ( (Number)cli.getParsedOptionValue( "mStart" ) ).doubleValue();
				double endMass = ( (Number)cli.getParsedOptionValue( "mEnd" ) ).doubleValue();
				/** TODO interpret start / end masses from raw file */
				// only set mass range for predicting mobilities
				if (mode == Apex3dWeldingMode.predictMobility)
					welder.setMassRange( startMass, endMass );
			}

			// TODO encapsulate in a thread?
			welder.run();
		}
		catch (Exception e)
		{
			log.error( "execution stopped: " + e.getMessage() );
			log.debug( "execution stopped: ", e );
			welderCLI.showHelp();
		}
	}

	private File f1 = null, f2 = null, fo = null;
	private SonarMath math = new SonarMath();

	private double minMass = 0.0;
	private double maxMass = 0.0;

	public apexWelder(File f1, File f2, File fo, Apex3dWeldingMode weldingMode)
	{
		this.f1 = f1;
		this.f2 = f2;
		this.fo = fo;
		this.weldingMode = weldingMode;
	}

	/**
	 * @param startMass
	 * @param endMass
	 */
	public void setMassRange(double startMass, double endMass)
	{
		log.info( "setting mass range from " + startMass + " to " + endMass );
		double range = endMass - startMass;
		this.minMass = startMass - range * 0.05;
		this.maxMass = endMass + range * 0.05;
		math.setMassRange( startMass, endMass );
	}

	public void run()
	{
		log.info( "running apexWelder build " + build + "." );

		try
		{
			log.info( "opening target file " + fo );
			PrintStream out = new PrintStream( fo );

			// get xml header from F2 file
			File xmlHeaderFile = weldingMode == Apex3dWeldingMode.reduceMobility ? f1 : f2;
			log.info( "reading xml header from file " + xmlHeaderFile );
			Apex3dXmlReader headerReader = new Apex3dXmlReader( xmlHeaderFile, Apex3dDataReadingMode.lock_mass );
			log.info( "writing xml header" );
			out.println( headerReader.getXmlHeader() );

			// get lock mass data from F2 file
			log.info( "processing lock mass data from file " + xmlHeaderFile );
			processLMData( headerReader, out );
			log.info( "processing lock mass data finished. [" + headerReader.getRowCount() + " peaks]" );
			headerReader.closeFile();
			
			// start <IONSTICKS>
			// write IONSTICKS tag and its attributes, change the number of available functions!
			Apex3dXmlReader f2Reader = new Apex3dXmlReader( f2, Apex3dDataReadingMode.heigh_energy );
			Map<String, String> istAtts = f2Reader.getIonSticksTagAttributes();
			istAtts.put( "numfuncs", "2" );
			out.println( "  " + getTagString( "IONSTICKS", istAtts ) );

			// get LE from F1 file
			log.info( "processing low energy data from file " + f1 );
			Apex3dXmlReader f1Reader = new Apex3dXmlReader( f1, Apex3dDataReadingMode.low_energy );
			processPeaks( f1Reader, out, weldingMode == Apex3dWeldingMode.predictMobility ? f2Reader.getColumnNames() : null );
			f1Reader.closeFile();
			log.info( "processing low energy data finished. [" + f1Reader.getRowCount() + " peaks]" );
			
			// get HE
			log.info( "processing high energy data from file " + f2 );
			processPeaks( f2Reader, out, weldingMode == Apex3dWeldingMode.reduceMobility ? f1Reader.getColumnNames() : null );
			f2Reader.closeFile();
			log.info( "processing high energy data finished. [" + f2Reader.getRowCount() + " peaks]" );

			// finalize xml tags
			out.println( "  </IONSTICKS>" );
			out.println( "</APEX3D>" );
			out.flush();
			out.close();
			log.info( "successfully welded!" );
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}

	private void processPeaks(Apex3dXmlReader in, PrintStream out, String[] cols) throws Exception
	{
		Apex3dDataReadingMode readingMode = in.getReadingMode();
		out.println( "    <" + readingMode.xmlTag() + ">" );
		int rt = 0;
		
		// read data
		if (weldingMode != Apex3dWeldingMode.emptyData)
		{
			if (cols == null)
				cols = in.getColumnNames();
			List<String> validCols = new ArrayList<>( cols.length );
			for ( int i = 0; i < cols.length; i++ )
			{
				// exclude mobility columns in reduction mode
				if (weldingMode == Apex3dWeldingMode.reduceMobility && cols[i].toLowerCase().contains( ".*mobility.*" ))
					continue;
				else
					validCols.add( cols[i] );
			}

			while (in.hasNext())
			{
				Map<String, String> data = in.getNextAsMap();
				double Mass = XJava.parseNumber( data.get( "Mass" ), 0.0 );

				// progress count
				if (data.containsKey( "RT" ))
				{
					int nextRT = (int)XJava.parseNumber( data.get( "RT" ), 0f );
					if (nextRT > rt)
					{
						rt = nextRT;
						if (rt % 10 == 0)
							log.info( "" + in.getRowCount() + " peaks found in first " + rt + "minutes of LC-MS run." );
					}
				}
				
				// forget MS1 ions out of scan range
				if (weldingMode == Apex3dWeldingMode.predictMobility
						&& readingMode == Apex3dDataReadingMode.low_energy
						&& ( Mass < minMass || Mass > maxMass ))

					continue;
				out.print( "     " ); // leading spaces
				
				// process columns in the given order
				for ( String col : validCols )
				{
					out.print( " " ); // column separation
					if (data.containsKey( col ))
					{
						out.print( data.get( col ) );
					}
					else
					{
						switch (col)
						{
							case "Mobility":
								// interprete IMS values by ion mass
								out.print( df3.format( math.getMobility( Mass ) ) );
								break;
							case "MobilityFWHM":
								// get constant FWHM value
								out.print( math.getMobilityFWHM() );
								break;
							case "ErrMobility":
								// calculate mobility error from intensity
								out.print( df4.format( math.getMobilityError( XJava.parseNumber( data.get( "Intensity" ), 0.0 ) ) ) );
								break;
							default:
								// should never happen
								log.warn( "WTF, unknown column name '" + col + "'!" );
								log.warn( "Your data is CRAP according to the rule 'Crap in - Crap out'!" );
						}
					}
				}
				out.println();
			}
		}
		out.println( "    </" + readingMode.xmlTag() + ">" );
	}

	private void processLMData(Apex3dXmlReader in, PrintStream out) throws Exception
	{
		// start LOCK_MASS_TABLE
		out.println( "  <LOCK_MASS_TABLE>" );
		out.println( "    " + getTagString( "LOCK_MASS", in.getLockMassTagAttributes() ) );
		// read and write lock mass data
		if (weldingMode != Apex3dWeldingMode.emptyData)
		{
			while (in.hasNext())
			{
				String line = in.getNextAsString();
				out.println( "      " + line );
			}
		}
		// end LOCK_MASS_TABLE
		out.println( "    </LOCK_MASS>" );
		out.println( "  </LOCK_MASS_TABLE>" );
	}

	/**
	 * construct xml tag from tag name and attrubute map
	 * @param tagName
	 * @param attributes
	 * @return
	 */
	private String getTagString(String tagName, Map<String, String> attributes)
	{
		String tag = "<" + tagName;
		for ( String k : attributes.keySet() )
		{
			tag += " " + k.toUpperCase() + "=\"" + attributes.get( k ) + "\"";
		}
		tag += ">";
		return tag;
	}
}
