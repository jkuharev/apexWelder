/** apexWelder, apexWelder, Feb 15, 2017*/
package apex3d;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.LinkedHashMap;
import java.util.Map;

import org.jdom.Element;

import de.mz.jk.jsix.lang.XStringConverter;
import de.mz.jk.jsix.libs.XFiles;
import de.mz.jk.jsix.libs.XJDOM;
import de.mz.jk.jsix.libs.XJava;

/**
 * <h3>{@link Apex3dXmlReader}</h3>
 * @author jkuharev
 * @version Feb 15, 2017 1:36:25 PM
 */
public class Apex3dXmlReader
{
	private File xmlFile = null;
	private String xmlHeader = "";
	private Element xmlDocRoot = null;

	private Map<String, Integer> dataName2Index = null;
	private Map<Integer, String> dataIndex2Name = null;

	private Map<String, String> parameterMap = null;

	private Map<String, Integer> lmName2Index = null;
	private Map<Integer, String> lmIndex2Name = null;

	private Map<String, String> lockMassTagAttributes = null;
	private Map<String, String> ionSticksTagAttributes = null;

	private int rowCounter = 0;
	private BufferedReader rowReader = null;
	private String rowDataString = null;

	private Apex3dDataReadingMode readingMode = null;

	/**
	 * 
	 * @param xmlFile
	 * @throws Exception
	 */
	public Apex3dXmlReader(File xmlFile) throws Exception
	{
		this.xmlFile = xmlFile;

		xmlHeader = XFiles.readUntilMatch( xmlFile, ".*\\<(IONSTICKS|LOCK_MASS_TABLE).*", false );
		xmlDocRoot = XJDOM.getBadJDOMRootElement( xmlHeader );

		// parse parameter map
		Element param = XJDOM.getFirstChild( xmlDocRoot, "PARAMS" );
		parameterMap = XJDOM.getAttributeMap( param, "PARAM", "NAME", "VALUE", XStringConverter.toString, XStringConverter.toString );
		
		// parse lock mass data columns
		Element lmFormat = XJDOM.getFirstChild( xmlDocRoot, "LMFORMAT" );
		lmName2Index = XJDOM.getAttributeMap( lmFormat, "FIELD", "NAME", "POSITION", XStringConverter.toString, XStringConverter.toInteger );
		lmIndex2Name = XJava.reverseMap( lmName2Index );
		
		// parse LE/HE data columns
		Element dataFormat = XJDOM.getFirstChild( xmlDocRoot, "DATAFORMAT" );
		dataName2Index = XJDOM.getAttributeMap( dataFormat, "FIELD", "NAME", "POSITION", XStringConverter.toString, XStringConverter.toInteger );
		dataIndex2Name = XJava.reverseMap( dataName2Index );
	}

	/**
	 * create a reader and directly set to the specified reading mode 
	 */
	public Apex3dXmlReader(File file, Apex3dDataReadingMode mode) throws Exception
	{
		this( file );
		setReadingMode( mode );
	}

	/**
	 * get all the xml header data as a single unparsed string 
	 * @return
	 */
	public String getXmlHeader()
	{
		return xmlHeader;
	}

	/**
	 * Open the speciefied Apex3D.xml file and skip file content until 
	 * the beginning of a data block specific for the used reading mode.
	 * Please use hasNext() to read next data line and getNext() to get the data of this line,
	 * e.g. while(reader.hasNext()) { data = reader.getNext(); ... } 
	 * @param mode LE/HE/LOCK_MASS
	 * @throws Exception
	 */
	public void setReadingMode(Apex3dDataReadingMode mode) throws Exception
	{
		lockMassTagAttributes = null;
		ionSticksTagAttributes = null;
		closeFile();
		rowCounter = 0;
		this.readingMode = mode;
		rowReader = new BufferedReader( new FileReader( xmlFile ) );
		String dataBlockStartRegEx = ".*\\<" + this.readingMode.xmlTag() + "(\\>|\\s)+.*";
		// read file until the start tag or end of file
		String line = "";
		while (( line = rowReader.readLine() ) != null)
		{
			// <LOCK_MASS CHARGE="2" ACTUAL_MASS="785.8426" APPLIED="1" LOCK_MASS_TOLERANCE_AMU="0.2500">
			if (line.matches( ".*\\<LOCK_MASS(\\>|\\s)+.*" ))
				lockMassTagAttributes = XJDOM.getAttributeMap( line, "LOCK_MASS" );

			// <IONSTICKS NUMFUNCS="1">
			if (line.matches( ".*\\<IONSTICKS(\\>|\\s)+.*" ))
				ionSticksTagAttributes = XJDOM.getAttributeMap( line, "IONSTICKS" );

			// stop right before data
			if (line.matches( dataBlockStartRegEx )) break;
		}
		// raise error if we didnt find the tag
		if (line == null)
			throw new Exception( "missing data block (<" + mode.xmlTag() + ">) for reading mode " + mode );
	}

	public void closeFile()
	{
		if (rowReader != null)
		{
			try
			{
				rowReader.close();
			}
			catch (Exception e)
			{}
		}
	}

	/**
	 * @return the readingMode
	 */
	public Apex3dDataReadingMode getReadingMode()
	{
		return readingMode;
	}

	/**
	 * @return the rowCounter
	 */
	public int getRowCount()
	{
		return rowCounter;
	}

	/**
	 * get last preread data.
	 * Values are split to an array of strings.
	 * Please use  the appropriate column-index mapping for the used reading mode
	 * @return
	 */
	public String[] getNextAsArray()
	{
		return ( rowDataString == null ) ? null : rowDataString.split( "\\s+" );
	}

	/**
	 * get last preread data as string 
	 * (leading / trailing spaces are already removed)
	 * @return
	 */
	public String getNextAsString()
	{
		return rowDataString;
	}

	/**
	 * wrap result by mapping values to column names as keys 
	 * @return
	 */
	public Map<String, String> getNextAsMap()
	{
		if (rowDataString == null) return null;
		Map<String, String> res = new LinkedHashMap<String, String>();
		Map<Integer, String> i2n = ( readingMode == Apex3dDataReadingMode.lock_mass ) ? lmIndex2Name : dataIndex2Name;
		String[] rowDataArray = getNextAsArray();
		for ( int i = 0; i < rowDataArray.length; i++ )
		{
			res.put( i2n.get( Integer.valueOf( i + 1 ) ), rowDataArray[i] );
		}
		return res;
	}

	/**
	 * column names depending on the current reading mode
	 * @return
	 */
	public String[] getColumnNames()
	{
		Map<String, Integer> map = ( readingMode == Apex3dDataReadingMode.lock_mass ) ? lmName2Index : dataName2Index;
		String[] res = new String[map.size()];
		int i = 0;
		for ( String c : map.keySet() )
			res[i++] = c;
		return res;
	}

	/**
	 * preread next line an parse data if any
	 * @return true in case of success, false in any other case
	 */
	public boolean hasNext()
	{
		try
		{
			String line = rowReader.readLine();
			if (line != null)
			{
				line = line.trim();
				if (!line.startsWith( "<" ))
				{
					rowCounter++;
					rowDataString = line;
					return true;	
				}
			}
			rowReader.close();
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		rowDataString = null;
		return false;
	}

	/**
	 * @return the parameterMap
	 */
	public Map<String, String> getParameterMap()
	{
		return parameterMap;
	}

	/**
	 * @return the dataIndex2Name
	 */
	public Map<Integer, String> getDataIndex2Name()
	{
		return dataIndex2Name;
	}

	/**
	 * @return the dataName2Index
	 */
	public Map<String, Integer> getDataName2Index()
	{
		return dataName2Index;
	}

	/**
	 * @return the lmIndex2Name
	 */
	public Map<Integer, String> getLmIndex2Name()
	{
		return lmIndex2Name;
	}

	/**
	 * @return the lmName2Index
	 */
	public Map<String, Integer> getLmName2Index()
	{
		return lmName2Index;
	}

	/**
	 * @return the lockMassTagAttributes
	 */
	public Map<String, String> getLockMassTagAttributes()
	{
		return lockMassTagAttributes;
	}

	/**
	 * @return the ionSticksTagAttributes
	 */
	public Map<String, String> getIonSticksTagAttributes()
	{
		return ionSticksTagAttributes;
	}
}

