package org.nrg.xnat.turbine.modules.screens;

//XNAT imports
import org.apache.turbine.util.RunData;
import org.apache.velocity.context.Context;
import org.nrg.xdat.turbine.utils.TurbineUtils;
import org.nrg.xft.ItemI;
import org.nrg.xft.XFTItem;

import edu.wfubmc.RiiplQAPlugin.NiftiSnapshot;
import edu.wfubmc.RiiplQAPlugin.QCUtils;
import edu.wfubmc.RiiplQAPlugin.RiiplDebug;
import ij.ImagePlus;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileReader;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Date;
import java.util.Hashtable;
import java.util.StringTokenizer;
import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.json.JSONArray;
import org.json.JSONObject;


/** 
	Searches in the nifti/cat12/  for a p*_seg8.txt.
	Parses the txt file and creates a jpg image for display and stores values from txt.
 * @author Ben Wagner
 * @author ANSIR Lab
 * @version 0.1 October 2011
 */
public class XDATScreen_edit_riiplqa_SegCat12Data extends org.nrg.xnat.turbine.modules.screens.EditImageAssessorScreen {
	static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(XDATScreen_edit_riiplqa_SegCat12Data.class);
	/* (non-Javadoc)
	 * @see org.nrg.xdat.turbine.modules.screens.EditScreenA#getElementName()
	 */

	private String defaultQuality=null;
	private Date now=null;
	private String subjectDir=null;
	private String dataLabel;

	public String getElementName()
	{
		return "riiplqa:SegCat12Data";
	}
	
	public ItemI getEmptyItem(RunData data) throws Exception
	{
		return super.getEmptyItem(data);
	}
	/* (non-Javadoc)
	 * @see org.nrg.xdat.turbine.modules.screens.SecureReport#finalProcessing(org.apache.turbine.util.RunData, org.apache.velocity.context.Context)
	 */
	public void finalProcessing(RunData data, Context context)
	{
		logger.debug("XDATScreen_edit_riiplqa_SegCat12Data::finalProcessing");
		org.nrg.xdat.om.RiiplqaSegcat12data om = new org.nrg.xdat.om.RiiplqaSegcat12data(item);
		org.nrg.xdat.om.XnatMrsessiondata mr = om.getMrSessionData();
		
		//field needs to be all lower case, even though defined as subjectDir
		defaultQuality="unread";
		now = new Date();
		subjectDir=(java.lang.String)mr.getFieldByName("subjectdir"); 
		dataLabel=this.getElementName().replaceFirst(".*:","");
		logger.debug("subjectDir: " + subjectDir);

		String eName=this.getElementName();
		int qcDataIndex = 0;
		
		SimpleDateFormat FORMAT_HHMMSS=new SimpleDateFormat("HH:mm:ss");
		SimpleDateFormat FORMAT_YYYYMMDD=new SimpleDateFormat("yyyy-MM-dd");
		
		if (subjectDir.length()==0)
		{
			data.setMessage("Error: No subjectDir found.  Unable to procceed");
			data.setScreenTemplate("Error.vm");
			return;
		}

		
		//Basic Details
		try
		{
			RiiplDebug.setProperty(item,eName + "/date",now);
			RiiplDebug.setProperty(item,eName + "/time",FORMAT_HHMMSS.format(now));
			RiiplDebug.setProperty(item,eName + "/label",mr.getProperty("label")+"_SegCat12");
			RiiplDebug.setProperty(item,eName + "/subject",mr.getSubjectData().getLabel());
		}
		catch (Exception e)
		{
			logger.error("Error setting basic details:" +  e);
			data.setMessage("Error setting basic details:" + e + "<br>" + Arrays.toString(e.getStackTrace()));
			data.setScreenTemplate("Error.vm");
			return;
		}


		//Find the QCData Index and set some basic fields
		try
		{
			String sName=eName + "/qcSession["+qcDataIndex+"]";

			while (item.getProperty(sName + "/quality") != null)
			{
//				logger.info("Has username in index: " +qcDataIndex);
				qcDataIndex++;
				sName=eName + "/qcSession["+qcDataIndex+"]";
			}

			//We don't want a new qcDataIndex for an EDIT
			if (qcDataIndex >= 1)
			{
				qcDataIndex--;
				sName=eName + "/qcSession["+qcDataIndex+"]";
			}
			else
			{
				logger.error("qcDataIndex too low for an EDIT");
				data.setMessage("qcDataIndex too low for an EDIT");
				data.setScreenTemplate("Error.vm");
				return;
			}

			eName=sName;
			logger.debug("qcIndexed eName="+eName);

			RiiplDebug.setProperty(item,eName + "/username",TurbineUtils.getUser(data).getUsername());
			RiiplDebug.setProperty(item,eName + "/date",now);
			RiiplDebug.setProperty(item,eName + "/time",FORMAT_HHMMSS.format(now));
//			RiiplDebug.setProperty(item,eName + "/quality",defaultQuality);
//			RiiplDebug.setProperty(item,eName + "/comments","");
//			RiiplDebug.setProperty(item,eName + "/qcImage",dataLabel+"_"+new SimpleDateFormat("yyyy_MM_dd_HH_mm_ss").format(now) + ".jpg");
		}
		catch (Exception e)
		{
			logger.error("Error finding QCData Index details: " + e);
			data.setMessage("Error finding QCData Index details: " + e + "<br>" + Arrays.toString(e.getStackTrace()));
			data.setScreenTemplate("Error.vm");
			return;
		}


		//QC Images cleanup on server....seems that that this code generates images when called and when saved
		try
		{
			JSONObject jsonResp = QCUtils.listFiles(data,item);
			if (jsonResp != null)
			{
				JSONObject resultSet = jsonResp.getJSONObject("ResultSet");
				JSONArray fileList = resultSet.getJSONArray("Result");
				logger.debug("Number of enteries in RESOURCE CATALOG: " + fileList.length());
				for (int i=0; i < fileList.length(); i++)
				{
					String fileI=(fileList.getJSONObject(i).getString("Name"));
					if (fileI.startsWith(dataLabel))
					{
						int sX=0;
						String sName=this.getElementName() + "/qcSession["+sX+"]";
//						boolean deleteFile=true;
						boolean deleteFile=false; //don't delete any files
						while (item.getProperty(sName + "/qcImage") != null)
						{
							if (item.getProperty(sName + "/qcImage").equals(fileI))
							{
								deleteFile=false;
								break;
							}
							else
							{
								sX++;
								sName=this.getElementName() + "/qcSession["+sX+"]";
							}
						}
						if (deleteFile)
						{
							logger.debug("Deleting file: " + fileI);
							QCUtils.deleteFile(data, fileList.getJSONObject(i).getString("URI"));
						}
					}
				}
			}
		}
		catch (Exception e)
		{
				logger.error("Warning: Trouble cleaning QC image listings: " + e);
				data.setMessage("Warning: Trouble cleaning QC image listings: " + e + "<br>" + Arrays.toString(e.getStackTrace()));
		}
	}
}
