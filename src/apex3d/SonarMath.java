/** apexWelder, apex3d, Mar 1, 2017*/
package apex3d;

import java.util.Arrays;
import java.util.List;

import de.mz.jk.jsix.math.interpolation.Interpolator;
import de.mz.jk.jsix.math.interpolation.LinearInterpolator;

/**
 * <h3>{@link SonarMath}</h3>
 * @author jkuharev
 * @version Mar 1, 2017 2:22:09 PM
 */
public class SonarMath
{
	public static double defaultMobilityFWHM = 5.563;
	/** default ion mobility drift time range from 0 to 200 */
	public static List<Double> defaultMobilityRange = Arrays.asList( new Double[] { 0.0, 200.0 } );
	/*  default SONAR quadrupole window beginning range from 400 to 880, 
	 *  it looks like the mean collected masses range from 405 to 885 
	 */
	private List<Double> defaultMassRange = Arrays.asList( new Double[] { 405.0, 885.0 } );
	private double mobilityFWHM = defaultMobilityFWHM;

	private List<Double> mobilityRange = defaultMobilityRange;
	private List<Double> massRange = defaultMassRange;
	private Interpolator mz2dt = initMobilityPredictor();

	public void setMobilityRange(double minMobility, double maxMobility)
	{
		this.mobilityRange = Arrays.asList( new Double[] { minMobility, maxMobility } );
		initMobilityPredictor();
	}

	/**
	 * set masses corresponding to min and max mobility values 
	 * @param minMass
	 * @param maxMass
	 */
	public void setMassRange(double minMass, double maxMass)
	{
		this.massRange = Arrays.asList( new Double[] { minMass, maxMass } );
		this.mz2dt = initMobilityPredictor();
	}

	/** 
	 * set constant mobility fwhm value 
	 * @param mobilityFWHM
	 */
	public void setMobilityFWHM(double mobilityFWHM)
	{
		this.mobilityFWHM = mobilityFWHM;
	}

	private Interpolator initMobilityPredictor()
	{
		LinearInterpolator li = new LinearInterpolator( massRange, mobilityRange );
		li.addBounds( 0, mobilityRange.get( 0 ), 100000, mobilityRange.get( 1 ) );
		return li;
	}

	/**
	 * predict mobility value by given mass
	 * @param mass
	 * @return
	 */
	public double getMobility(double mass)
	{
		return ( mz2dt.getY( mass ) );
	}

	/** 
	 * Mobility error as function of intensity.
	 * At least for SONAR data, the mobility error value can be predicted by exp(a + b * log(Intensity))
	 * with coefficients a = 2.2743 and b = -0.4852.
	 * The coeffiecients of the function were modeled in R by using real values as follows:
	 * model = nls( me ~ exp(a + b * li), start=list(a=0, b=0) )
	 * with me = Mobility Error and li = log( Intensity )
	 * @param intensity
	 * @return predicted mobility error = exp(2.2743 - 0.4852 * log(Intensity))
	 */
	public double getMobilityError(double intensity)
	{
		return Math.exp( 2.2743 - 0.4852 * Math.log( intensity ) );
	}

	public double getMobilityFWHM()
	{
		return mobilityFWHM;
	}
}
