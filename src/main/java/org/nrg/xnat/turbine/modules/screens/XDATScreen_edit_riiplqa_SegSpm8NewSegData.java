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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Hashtable;
import java.util.StringTokenizer;
import org.json.JSONArray;
import org.json.JSONObject;

/** 
	Searches in the nifti/sgs/ folder for csv file named "seg_report.csv".
	Parses this file and creates a jpg image for display and stores values from csv.
 * @author Ben Wagner
 * @author ANSIR Lab
 * @version 0.1 August 2011
 */
public class XDATScreen_edit_riiplqa_SegSpm8NewSegData extends org.nrg.xnat.turbine.modules.screens.EditImageAssessorScreen {
	static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(XDATScreen_edit_riiplqa_SegSpm8NewSegData.class);
	/* (non-Javadoc)
	 * @see org.nrg.xdat.turbine.modules.screens.EditScreenA#getElementName()
	 */

	private String defaultQuality=null;
	private Date now=null;
	private String subjectDir=null;
	private String dataLabel;

	public String getElementName()
	{
		return "riiplqa:SegSpm8NewSegData";
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
		logger.debug("XDATScreen_edit_riiplqa_SegSpm8NewSegData::finalProcessing");
		org.nrg.xdat.om.RiiplqaSegspm8newsegdata om = new org.nrg.xdat.om.RiiplqaSegspm8newsegdata(item);
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
			RiiplDebug.setProperty(item,eName + "/time",new SimpleDateFormat("HH:mm:ss").format(now));
			RiiplDebug.setProperty(item,eName + "/label",mr.getProperty("label")+"_SegSpm8NewSeg");
		}
		catch (Exception e)
		{
			logger.error("Error setting basic details:" +  e);
			data.setMessage("Error setting basic details:" + e);
			data.setScreenTemplate("Error.vm");
		}


		//Find the QCData Index and set some basic fields
		try
		{
			String sName=eName + "/qcSession["+qcDataIndex+"]";

			while (item.getProperty(sName + "/qcImage") != null)
			{
//				logger.info("Has username in index: " +qcDataIndex);
				qcDataIndex++;
				sName=eName + "/qcSession["+qcDataIndex+"]";
			}
			eName=sName;
			logger.debug("qcIndexed eName="+eName);

			RiiplDebug.setProperty(item,eName + "/username",TurbineUtils.getUser(data).getUsername());
			RiiplDebug.setProperty(item,eName + "/date",now);
			RiiplDebug.setProperty(item,eName + "/time",FORMAT_HHMMSS.format(now));
			RiiplDebug.setProperty(item,eName + "/quality",defaultQuality);
			RiiplDebug.setProperty(item,eName + "/comments","");
			RiiplDebug.setProperty(item,eName + "/qcImage",dataLabel+"_"+new SimpleDateFormat("yyyy_MM_dd_HH_mm_ss").format(now) + ".jpg");
		}
		catch (Exception e)
		{
			logger.error("Error finding QCData Index details: " + e);
			data.setMessage("Error finding QCData Index details: " + e);
			data.setScreenTemplate("Error.vm");
		}

		//Read CSV file and set QC Numbers/images
		try
		{
			File csvFile=new File(subjectDir+"/nifti/sgs/seg_report.csv");
			logger.debug("csvFile: " + csvFile.getAbsolutePath());
			if (!csvFile.exists())
			{
				logger.error("Unable to find segmentation csv file: " + csvFile.getAbsolutePath());
				data.setMessage("Unable to find segmentation csv file: " + csvFile.getAbsolutePath());
				data.setScreenTemplate("Error.vm");
			}
			
			RiiplDebug.setProperty(item,eName + "/tisData/file",csvFile.getAbsolutePath());
			RiiplDebug.setProperty(item,eName + "/tisData/filedate",now);
			RiiplDebug.setProperty(item,eName + "/tisData/filetime",FORMAT_HHMMSS.format(now));
			RiiplDebug.setProperty(item,eName + "/tisData/quality",defaultQuality);
			
			boolean haveSkippedFirst=false;
			BufferedReader bufRdr = new BufferedReader(new FileReader(csvFile));
			String line=null;
			int lX=0;
		
			while ((line=bufRdr.readLine())!=null)
			{
				if (!haveSkippedFirst)
				{
					//Skip the first line of the file (header)
					haveSkippedFirst=true;
					continue;
				}
				setTissueDataRow(eName, line, lX);
				lX++;
			}
		}
		catch (Exception e)
		{
			logger.error("Error setting QC numbers: " + e);
			data.setMessage("Error setting QC numbers: " + e);
			data.setScreenTemplate("Error.vm");
		}

		//QC Image
		try
		{
			int sX = 0;
			ArrayList<File> files=new ArrayList<File>();
			String sName=eName + "/imgData["+sX+"]";
			while (item.getProperty(sName + "/file") != null)
			{
				files.add(new File((String)item.getProperty(sName + "/file")));
				//Prep for next look
				sX++;
				sName=eName + "/imgData["+sX+"]";
			}
			
/*
			File outDir=new File(mr.getCurrentSessionFolder(true)+"/"+label);
			logger.debug("Ouput directory is: "+ outDir.getAbsolutePath());
			if (!outDir.exists())
			{
				logger.debug("Creating that directory");
				outDir.mkdirs();
			}
*/
			File tempOutFile=File.createTempFile(dataLabel,".jpg");
			
			String outFile=tempOutFile.getAbsolutePath();
			logger.debug("Temp Ouput montage file is: "+ outFile);
			NiftiSnapshot.createMontage(outFile, files, true, true);

			QCUtils.postQCMosaic(data,item,this.getElementName(),tempOutFile,qcDataIndex);

		}
		catch (Exception e)
		{
			logger.error("Error creating/setting QC image: " + e);
			data.setMessage("Error creating/setting QC image: " + e);
//			data.setScreenTemplate("Error.vm");
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
				data.setMessage("Warning: Trouble cleaning QC image listings: " + e);
//				data.setScreenTemplate("Error.vm");
		}
	}
	
	/** Sets the information from the csv
	* @param	line		A line from the CSV file
	* @param	lX			The line number (excluding header)
	* @return 				void
	*/
	private void setTissueDataRow(String eName, String line, int lX)
	{
		StringTokenizer st = new StringTokenizer(line,",");
		setTissueDataItem(eName,"patId",lX,st); //is discarded
		setTissueDataItem(eName,"tissueType",lX,st);
		setTissueDataItem(eName,"parameter",lX,st);
		setTissueDataItem(eName,"mean",lX,st);
		setTissueDataItem(eName,"stdDev",lX,st);
		setTissueDataItem(eName,"min",lX,st);
		setTissueDataItem(eName,"max",lX,st);
		setTissueDataItem(eName,"nVoxels",lX,st);
		setTissueDataItem(eName,"file",lX,st); //comination of fileDir and fileName
		setTissueDataItem(eName,"t1 segment",lX,st);
		setTissueDataItem(eName,"gmThreshold",lX,st);
		setTissueDataItem(eName,"wmThreshold",lX,st);
		setTissueDataItem(eName,"csfThreshold",lX,st);
		setTissueDataItem(eName,"gmMin",lX,st);
		setTissueDataItem(eName,"gmMax",lX,st);
		setTissueDataItem(eName,"wmMin",lX,st);
		setTissueDataItem(eName,"wmMax",lX,st);
		setTissueDataItem(eName,"csfMin",lX,st);
		setTissueDataItem(eName,"csfMax",lX,st);
		setTissueDataItem(eName,"snr",lX,st);
		setTissueDataItem(eName,"segImg",lX,st);
		return;
	}

	private void setTissueDataItem(String eName,String key, int lX, StringTokenizer st)
	{
		try
		{
			if (key.equals("patId"))
			{
				//no need to save this
				logger.debug("skip padId field, value: " + st.nextToken());
			}
			else if (key.equals("file"))
			{
				String fileName=st.nextToken()+"/"+st.nextToken();
				QCUtils.setQcImageFiles(item, eName,fileName,defaultQuality);
			}
			else if (key.equals("segImg"))
			{
				String fileName=st.nextToken();
				QCUtils.setQcImageFiles(item, eName,fileName,defaultQuality);
			}
			else if (key.equals("parameter"))
			{
				String dName=eName + "/tisData";
				String value=st.nextToken();
				RiiplDebug.setProperty(item,dName + "/" + key,value);
			}
			else
			{
				String dName=eName + "/tisData/tissue["+lX+"]";
				String value=st.nextToken();
				RiiplDebug.setProperty(item,dName + "/" + key,value);
			}
		}
		catch (Exception e)
		{
			logger.warn("Unable to set line "+lX+" key: " +key);
			logger.debug("Unable to set "+key+" error: " + e);
		}
	}
}
