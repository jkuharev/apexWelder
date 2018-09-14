/** apexWelder, apexWelder, Mar 10, 2017*/
package apexWelder;

/**
 * <h3>{@link Apex3dWeldingMode}</h3>
 * @author jkuharev
 * @version Mar 10, 2017 9:35:21 AM
 */
public enum Apex3dWeldingMode
{
		/** predict missing mobility values for Low Energy data */
		predictMobility,
		/** drop ion mobility values from High Energy data */
		reduceMobility,
		/** keep only XML data */
	emptyData;

	/**
	 * try to guess the mode from a string, in case of doubt it's reduceMobility!
	 * @param modeString
	 * @return
	 */
	public static Apex3dWeldingMode fromString(String modeString)
	{
		modeString = modeString.trim().toLowerCase();
		if (modeString.matches( ".*(predict|expand).*" ))
			return predictMobility;
		if (modeString.matches( ".*(xml|empty).*" ))
			return emptyData;
		return reduceMobility;
	}
}
