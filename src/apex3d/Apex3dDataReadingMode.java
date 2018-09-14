package apex3d;
/**
 * target reading mode
 * <h3>{@link Apex3DResultReadingMode}</h3>
 * @author jkuharev
 * @version Feb 16, 2017 2:22:57 PM
 */
public enum Apex3dDataReadingMode
{
	/** parent ion peaks, function 1, MS1 or low energy */
	low_energy,
	/** product ion peaks, function 2, MS2 or high energy */
	heigh_energy,
	/** lock mass ion peaks, typically function 3 */
	lock_mass;

	public String xmlTag()
	{
		switch (this)
		{
			case heigh_energy:
				return "HE";
			case lock_mass:
				return "LOCK_MASS";
			default:
			case low_energy:
				return "LE";
		}
	}

	/**
	 * try to guess the mode from a string, in case of doubt it's low energy!
	 * @param modeString
	 * @return
	 */
	public static Apex3dDataReadingMode fromString(String modeString)
	{
		modeString = modeString.trim().toLowerCase();
		if (modeString.matches( ".*lock\\s+mass.*" ) || modeString.matches( ".*lm.*" )) return lock_mass;
		if (modeString.matches( ".*(hi|he|product).*" )) return heigh_energy;
		return low_energy;
	}
}