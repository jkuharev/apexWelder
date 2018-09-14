/** apexWelder, apexWelder, Feb 15, 2017*/
package apexWelder;

import org.apache.commons.cli.Option;

import de.mz.jk.jsix.cli.SimpleCLI;

/**
 * <h3>{@link ApexWelderCLI}</h3>
 * @author jkuharev
 * @version Feb 15, 2017 8:59:02 AM
 */
public class ApexWelderCLI extends SimpleCLI
{
	public ApexWelderCLI()
	{
		super();
		setHelpHeader(
				"\nJoin Apex3D result files containing separately processed \n"
						+ "low energy (function 1) and high energy (function 2) data.\n\n" );
		setHelpFooter( "\napexWelder, build " + apexWelder.build + "\n (c) Dr. Joerg Kuharev" );
	}

	@Override public Option[] getDefaultOptions()
	{
		return new Option[] {
				// Option.builder( "raw" ).argName( "raw file" ).desc( "file system path to raw data" ).hasArg().optionalArg( true ).build(),
				Option.builder( "f1" ).argName( "xml file" ).desc( "file path to Apex3D.xml low energy result file" ).hasArg().required().build(),
				Option.builder( "f2" ).argName( "xml file" ).desc( "file path to Apex3D.xml high energy result file" ).hasArg().required().build(),
				Option.builder( "fo" ).argName( "xml file" ).desc( "file path to the resulting Apex3D.xml file" ).hasArg().required().build(),
				Option.builder( "mStart" ).argName( "number" ).desc( "mass corresponding to drift time value of 0.0, e.g. 404" ).hasArg().required( false ).type( Number.class ).build(),
				Option.builder( "mEnd" ).argName( "number" ).desc( "mass corresponding to drift time value of 200.0, e.g. 885" ).hasArg().required( false ).type( Number.class ).build(),
				Option.builder( "mode" ).argName( "(expand|reduce|xml" ).desc( "operating mode: expand: to predict drift time, reduce: to remove drift time, xml: to drop all data." ).hasArg().required( false ).build(),
				getDefaultHelpOption(),
		};
	}

	@Override public String getExecutableJarFileName()
	{
		return "apexWelder";
	}
}
