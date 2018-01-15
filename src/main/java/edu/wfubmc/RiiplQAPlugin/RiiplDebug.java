package edu.wfubmc.RiiplQAPlugin;

//XNAT imports
import org.nrg.xft.ItemI;

/** A class for QC Utilities common to the somewhat standard image QC framework
* @author Ben Wagner
* @author ANSIR Lab
* @version 0.1 October 2011
*/
public class RiiplDebug
{
	static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(RiiplDebug.class);

	public static void setProperty(ItemI item, String eName, java.lang.Object value) throws java.lang.Exception
	{
		try
		{
			logger.debug("Setting: "+eName+" = "+value);
			item.setProperty(eName,value);
		}
		catch (Exception e)
		{
			logger.debug("Failed Setting: "+eName+" to " +value);
			throw e;
		}
	}

	public static void setPropertyIfNull(ItemI item, String eName, java.lang.Object value) throws java.lang.Exception
	{
		try
		{
			if (item.getProperty(eName)==null)
			{
				RiiplDebug.setProperty(item, eName, value);
			}
		}
		catch (Exception e)
		{
			logger.debug("Unknown NULL status for: "+eName);
			throw e;
		}
	}
	public static java.lang.Object getProperty(ItemI item, String eName, java.lang.Object value) throws java.lang.Exception
	{
		java.lang.Object retItem=null;
		try
		{
			logger.debug("Getting: "+eName);
			retItem = item.getProperty(eName);
			logger.debug("Reviewed: "+eName+ " Value:" + retItem);
		}
		catch (Exception e)
		{
			logger.debug("Failed Getting: "+eName);
			throw e;
		}
		return retItem;
	}
}
