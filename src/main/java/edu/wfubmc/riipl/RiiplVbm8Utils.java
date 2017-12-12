package edu.wfubmc.riipl;

//XNAT imports
import org.apache.turbine.util.RunData;
import org.nrg.xdat.turbine.utils.TurbineUtils;
import org.nrg.xft.ItemI;

//other stuff
import edu.wfubmc.riipl.RiiplDebug;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.StringTokenizer;
import java.text.SimpleDateFormat;
import org.apache.commons.io.comparator.NameFileComparator;
import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.FileUtils;
import org.json.JSONArray;
import org.json.JSONObject;

//For Properties...
import java.io.FileInputStream;
import java.util.Properties;
import org.nrg.xft.XFT;
import java.util.Hashtable;
import java.util.Enumeration;



/** A class of utilities dealing with VBM8 processing
* @author Ben Wagner
* @author ANSIR Lab
* @version 0.1 January 2012
*/
public class RiiplVbm8Utils
{
	static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(RiiplVbm8Utils.class);

	/** Sets reads the tissue segmentation report (wfu_vbm8_tissue_report.m) and places in vbm8TissueReport
	* @param		data					The "RunData" or Template session information
	* @param		item					The Datatype's DATA
	* @param		eName					The element name in the XSD
	* @param		csvFile				The tissue report (csv file)
	* @return										void
	*/
	public static void processTissueReport(RunData data, ItemI item, String eName, File csvFile) throws Exception, IOException
	{
		String defaultQuality="unread";
		SimpleDateFormat FORMAT_HHMMSS=new SimpleDateFormat("HH:mm:ss");
		SimpleDateFormat FORMAT_YYYYMMDD=new SimpleDateFormat("yyyy-MM-dd");
		
		if (!csvFile.exists())
		{
			logger.error(csvFile.getAbsolutePath() + " does not exist");
			data.setMessage(csvFile.getAbsolutePath() + " does not exist");
			data.setScreenTemplate("Error.vm");
			throw new IOException(csvFile.getAbsolutePath() + " does not exist");
		}

		try
		{
			RiiplDebug.setProperty(item,eName + "/tisData/file",csvFile.getAbsolutePath());
			RiiplDebug.setProperty(item,eName + "/tisData/filedate",new Date(csvFile.lastModified()));
			RiiplDebug.setProperty(item,eName + "/tisData/filetime",FORMAT_HHMMSS.format(new Date(csvFile.lastModified())));
			RiiplDebug.setProperty(item,eName + "/tisData/quality",defaultQuality);
		}
		catch (Exception e)
		{
			throw e;
		}
		
		boolean processedFirstLine=false;
		BufferedReader bufRdr = new BufferedReader(new FileReader(csvFile));
		String line=null;
	
		int sX=0;
	
		while ((line=bufRdr.readLine())!=null)
		{
			if (!processedFirstLine)
			{
				logger.debug("Ignoring first line: " + line);
				processedFirstLine=true;
				continue;
			}
			else
			{
				StringTokenizer st = new StringTokenizer(line,",");
				try
				{
					RiiplDebug.setProperty(item,eName + "/tisData/tissue["+sX+"]/imageFile",st.nextToken());
					RiiplDebug.setProperty(item,eName + "/tisData/tissue["+sX+"]/imageType",st.nextToken());
					RiiplDebug.setProperty(item,eName + "/tisData/tissue["+sX+"]/gm",st.nextToken());
					RiiplDebug.setProperty(item,eName + "/tisData/tissue["+sX+"]/wm",st.nextToken());
					RiiplDebug.setProperty(item,eName + "/tisData/tissue["+sX+"]/csf",st.nextToken());
					RiiplDebug.setProperty(item,eName + "/tisData/tissue["+sX+"]/gmThresh",st.nextToken());
					RiiplDebug.setProperty(item,eName + "/tisData/tissue["+sX+"]/wmThresh",st.nextToken());
					RiiplDebug.setProperty(item,eName + "/tisData/tissue["+sX+"]/csfThresh",st.nextToken());
					sX=sX+1;
				}
				catch (Exception e)
				{
					throw e;
				}
			}
		}
	}

	/** Populates the normalization QC for NormVbm8[DTI|PASL|PCASL|MTR|...]Data 
	* @param		mr						org.nrg.xdat.om.XnatMrsessiondata
	* @param		data					The "RunData" or Template session information
	* @param		item					The Datatype's DATA
	* @param		elementName		element name in the XSD
	* @param		expLabel			The experiment labeL suffix (will be appended to mr.getProperty("label"))
	* @param		csvFile				The tissue report (csv file)
	* @param		imageFilters	An array of image filters (see WildcardFileFilter)
	* @param		rpProcess		Find and process an rp_NNN.txt file to match first imageFilter
	* @return									void
	*/
	public static void normalizationQC(org.nrg.xdat.om.XnatMrsessiondata mr, RunData data, ItemI item, String elementName, String expLabel, String csvFileName, ArrayList<WildcardFileFilter> imageFilters, boolean rpProcess)
	{
		Boolean useXtk=true;
		org.nrg.xdat.om.XnatMrassessordata om = new org.nrg.xdat.om.XnatMrassessordata(item);

		//field needs to be all lower case, even though defined as subjectDir
		String defaultQuality="unread";
		Date now = new Date();
		String subjectDir=(java.lang.String)mr.getFieldByName("subjectdir"); 
		String dataLabel=elementName.replaceFirst(".*:","");
		String eName=elementName;  //This gets used/modified in the script.
		logger.debug("subjectDir: " + subjectDir);

		int qcDataIndex = 0;
		
		SimpleDateFormat FORMAT_HHMMSS=new SimpleDateFormat("HH:mm:ss");
		SimpleDateFormat FORMAT_YYYYMMDD=new SimpleDateFormat("yyyy-MM-dd");

		File vbm8Dir = new File(subjectDir+"/nifti/vbm8");
		File imageDir = vbm8Dir;
		
		if (subjectDir.length()==0)
		{
			data.setMessage("Error: No subjectDir found.  Unable to procceed");
			data.setScreenTemplate("Error.vm");
			return;
		}

		//create label
		String label = null;
		try
		{
			label = mr.getProperty("label")+"_"+expLabel;
		}
		catch (Exception e)
		{
			logger.error("Error setting label:" +  e);
			data.setMessage("Error setting label:" + e + "<br>" + Arrays.toString(e.getStackTrace()));
			data.setScreenTemplate("Error.vm");
			return;
		}

		//checking for prior existance
		if (QCUtils.isExistingAssessor(data,item,label))
		{
			logger.error(elementName + " session already exists.  Please delete the existing session before trying to re-upload.");
			data.setMessage(elementName + " session already exists.  Please delete the existing session before trying to re-upload.");
			data.setScreenTemplate("Error.vm");
			return;
		}

		//Basic Details
		try
		{
			RiiplDebug.setProperty(item,eName + "/date",now);
			RiiplDebug.setProperty(item,eName + "/time",FORMAT_HHMMSS.format(now));
			RiiplDebug.setProperty(item,eName + "/label",label);
			RiiplDebug.setProperty(item,eName + "/subject",mr.getSubjectData().getLabel());
		}
		catch (Exception e)
		{
			logger.error("Error setting basic details",e);
			data.setMessage("Error setting basic details:" + e);
			data.setScreenTemplate("Error.vm");
			return;
		}


		//Find the QCData Index and set some basic fields
		try
		{
			String sName=eName + "/qcSession["+qcDataIndex+"]";

			while (item.getProperty(sName + "/quality") != null)
			{
				logger.debug(sName + "/qcImage has data: " + item.getProperty(sName + "/quality"));
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
			if (!useXtk)
			{
				RiiplDebug.setProperty(item,eName + "/qcImage",dataLabel+"_MOSAIC_"+new SimpleDateFormat("yyyy_MM_dd_HH_mm_ss").format(now) + ".jpg");
			}
			else
			{
				RiiplDebug.setProperty(item,eName + "/qcImage","");
			}
		}
		catch (Exception e)
		{
			logger.error("Error finding QCData Index details: ",e);
			data.setMessage("Error finding QCData Index details: " + e);
			data.setScreenTemplate("Error.vm");
			return;
		}

		//seg8Txt is used to get the T1 image name
		File seg8Txt=null;
		try
		{
			if (!vbm8Dir.exists())
			{
				logger.error(subjectDir+"/nifti/vbm8 does not exist");
				data.setMessage(subjectDir+"/nifti/vbm8 does not exist");
				data.setScreenTemplate("Error.vm");
				return;
			}
			FileFilter fileFilter = new WildcardFileFilter("p*_seg8.txt");
			File[] seg8TxtFiles = vbm8Dir.listFiles(fileFilter);
			
			if (seg8TxtFiles.length == 0)
			{
				logger.error("No p*_seg8.txt files found in "+subjectDir+"/nifti/vbm8");
				data.setMessage("No p*_seg8.txt files found in "+subjectDir+"/nifti/vbm8");
				data.setScreenTemplate("Error.vm");
				return;
			}
			else if (seg8TxtFiles.length > 1)
			{
				logger.error("Too many p*_seg8.txt files found in "+subjectDir+"/nifti/vbm8: Count:"+seg8TxtFiles.length);
				data.setMessage("Too many p*_seg8.txt files found in "+subjectDir+"/nifti/vbm8: Count:"+seg8TxtFiles.length);
				data.setScreenTemplate("Error.vm");
				return;
			}
			
			seg8Txt=seg8TxtFiles[0];
			logger.debug("seg8Txt: " + seg8Txt.getAbsolutePath());
		}
		catch (Exception e)
		{
			logger.error("Error getting T1", e);
			data.setMessage("Error getting T1: " + e);
			data.setScreenTemplate("Error.vm");
			return;
		}


		//Read CSV file and set QC Numbers/images
		try
		{
			Boolean processCsvFileWholeBrain = false;
			Boolean processCsvFileTissue = true;
			File csvFile = null;
			
			if (elementName.equals("riipl:NormVbm8DTIData"))
			{
				//look for DTI_SNNNN directories first.
				//Also for DTI_SNNN directories which come in normalized from the older DTI pipeline
				File[] DTIdirs = vbm8Dir.listFiles((FileFilter) new WildcardFileFilter((new String[] {"DTI_S????","DTI_S???"})));
				logger.debug("number of nifti/vbm8/DTI_Sxxx directories: "+DTIdirs.length);
				if (DTIdirs.length == 1)
				{
					String DTIdir=DTIdirs[0].getAbsolutePath();
					csvFile = new File(DTIdir+'/'+csvFileName);
					if (csvFile.exists())
					{
						imageDir=DTIdirs[0];
					}
					else
					{
						csvFile = null;
					}
				}
				else if (DTIdirs.length > 1)
				{
					logger.error("More than one VBM8/DTI_Snnnn directory in "+subjectDir+"/nifti/vbm8");
					data.setMessage("More than one VBM8/DTI_Snnnn directory in "+subjectDir+"/nifti/vbm8");
					data.setScreenTemplate("Error.vm");
					return;
				}
			}
			if (elementName.equals("riipl:NormVbm8DKIData"))
			{
				//look for DKI_SNNNN directories first.
				//Also for DKI_SNNN directories which come in normalized from the older DKI pipeline
				File[] DKIdirs = vbm8Dir.listFiles((FileFilter) new WildcardFileFilter((new String[] {"DKI_S????","DKI_S???"})));
				logger.debug("number of nifti/vbm8/DKI_Sxxx directories: "+DKIdirs.length);
				if (DKIdirs.length == 1)
				{
					String DKIdir=DKIdirs[0].getAbsolutePath();
					csvFile = new File(DKIdir+'/'+csvFileName);
					if (csvFile.exists())
					{
						imageDir=DKIdirs[0];
					}
					else
					{
						csvFile = null;
					}
				}
				else if (DKIdirs.length > 1)
				{
					logger.error("More than one VBM8/DKI_Snnnn directory in "+subjectDir+"/nifti/vbm8");
					data.setMessage("More than one VBM8/DKI_Snnnn directory in "+subjectDir+"/nifti/vbm8");
					data.setScreenTemplate("Error.vm");
					return;
				}
			}
			if (elementName.equals("riipl:NormVbm8DKEData"))
			{
				//look for DKE_SNNNN directories first.
				//Also for DKE_SNNN directories which come in normalized from the older DKE pipeline
				File[] DKEdirs = vbm8Dir.listFiles((FileFilter) new WildcardFileFilter((new String[] {"DKE_S????","DKE_S???"})));
				logger.debug("number of nifti/vbm8/DKE_Sxxx directories: "+DKEdirs.length);
				if (DKEdirs.length == 1)
				{
					String DKEdir=DKEdirs[0].getAbsolutePath();
					csvFile = new File(DKEdir+'/'+csvFileName);
					if (csvFile.exists())
					{
						imageDir=DKEdirs[0];
					}
					else
					{
						csvFile = null;
					}
				}
				else if (DKEdirs.length > 1)
				{
					logger.error("More than one VBM8/DKE_Snnnn directory in "+subjectDir+"/nifti/vbm8");
					data.setMessage("More than one VBM8/DKE_Snnnn directory in "+subjectDir+"/nifti/vbm8");
					data.setScreenTemplate("Error.vm");
					return;
				}
			}
			if (elementName.equals("riipl:NormVbm8MTRData"))
			{
				//look for MTR_SNNNN directories first.
				File[] MTRdirs = vbm8Dir.listFiles((FileFilter) new WildcardFileFilter((new String[] {"MTR_S????","MTR_S???"})));
				logger.debug("number of nifti/vbm8/MTR_Sxxxx directories: "+MTRdirs.length);
				if (MTRdirs.length == 1)
				{
					String MTRdir=MTRdirs[0].getAbsolutePath();
					csvFile = new File(MTRdir+'/'+csvFileName);
					if (csvFile.exists())
					{
						imageDir=MTRdirs[0];
					}
					else
					{
						csvFile = null;
					}
				}
				else if (MTRdirs.length > 1)
				{
					logger.error("More than one VBM8/MTR_Snnnn directory in "+subjectDir+"/nifti/vbm8");
					data.setMessage("More than one VBM8/MTR_Snnnn directory in "+subjectDir+"/nifti/vbm8");
					data.setScreenTemplate("Error.vm");
					return;
				}
			}
			if (elementName.equals("riipl:NormVbm8LST8Data"))
			{
				processCsvFileTissue = false; //No processing of TissueData
				//look for MTR_SNNNN directories first.
				File[] LSTdirs = vbm8Dir.listFiles((FileFilter) new WildcardFileFilter((new String[] {"lst8"})));
				logger.debug("number of nifti/vbm8/lst8 directories: "+LSTdirs.length);
				if (LSTdirs.length == 1)
				{
					String LSTdir=LSTdirs[0].getAbsolutePath();
					csvFile = new File(LSTdir+'/'+csvFileName);
					String flair= null;
					
					if (csvFile.exists())
					{
						imageDir=LSTdirs[0];
						//Read an process CSV

//*********************************************************************************

						try
						{
							RiiplDebug.setProperty(item,eName + "/wbData/file",csvFile.getAbsolutePath());
							RiiplDebug.setProperty(item,eName + "/wbData/filedate",new Date(csvFile.lastModified()));
							RiiplDebug.setProperty(item,eName + "/wbData/filetime",FORMAT_HHMMSS.format(new Date(csvFile.lastModified())));
							RiiplDebug.setProperty(item,eName + "/wbData/quality",defaultQuality);
						}
						catch (Exception e)
						{
							throw e;
						}
		
						boolean processedFirstLine=false;
						BufferedReader bufRdr = new BufferedReader(new FileReader(csvFile));
						String line=null;
	
						int sX=0;
	
						while ((line=bufRdr.readLine())!=null)
						{
							StringTokenizer st = new StringTokenizer(line,",");
							try
							{
								String fileNameBase=st.nextToken();
								String value=st.nextToken();

								RiiplDebug.setProperty(item,eName + "/wbData/whlBrn["+sX+"]/imageFile",fileNameBase);

								if (fileNameBase.contains("wb_000_lesion_lbm0_005") || fileNameBase.contains("wb_000_lesion_005"))
								{
									RiiplDebug.setProperty(item,eName + "/wbData/whlBrn["+sX+"]/imageType","LST 0.05");
								}
								else if (fileNameBase.contains("wb_000_lesion_lbm0_010") || fileNameBase.contains("wb_000_lesion_010"))
								{
									RiiplDebug.setProperty(item,eName + "/wbData/whlBrn["+sX+"]/imageType","LST 0.10");
								}
								else if (fileNameBase.contains("wb_000_lesion_lbm0_015") || fileNameBase.contains("wb_000_lesion_015"))
								{
									RiiplDebug.setProperty(item,eName + "/wbData/whlBrn["+sX+"]/imageType","LST 0.15");
								}
								else if (fileNameBase.contains("wb_000_lesion_lbm0_020") || fileNameBase.contains("wb_000_lesion_020"))
								{
									RiiplDebug.setProperty(item,eName + "/wbData/whlBrn["+sX+"]/imageType","LST 0.20");
								}
								else if (fileNameBase.contains("wb_000_lesion_lbm0_025") || fileNameBase.contains("wb_000_lesion_025"))
								{
									RiiplDebug.setProperty(item,eName + "/wbData/whlBrn["+sX+"]/imageType","LST 0.25");
								}
								else if (fileNameBase.contains("wb_000_lesion_lbm0_030") || fileNameBase.contains("wb_000_lesion_030"))
								{
									RiiplDebug.setProperty(item,eName + "/wbData/whlBrn["+sX+"]/imageType","LST 0.30");
								}
								else if (fileNameBase.contains("wb_000_lesion_lbm0_035") || fileNameBase.contains("wb_000_lesion_035"))
								{
									RiiplDebug.setProperty(item,eName + "/wbData/whlBrn["+sX+"]/imageType","LST 0.35");
								}
								else if (fileNameBase.contains("wb_000_lesion_lbm0_040") || fileNameBase.contains("wb_000_lesion_040"))
								{
									RiiplDebug.setProperty(item,eName + "/wbData/whlBrn["+sX+"]/imageType","LST 0.40");
								}
								else if (fileNameBase.contains("wb_000_lesion_lbm0_045") || fileNameBase.contains("wb_000_lesion_045"))
								{
									RiiplDebug.setProperty(item,eName + "/wbData/whlBrn["+sX+"]/imageType","LST 0.45");
								}
								else if (fileNameBase.contains("wb_000_lesion_lbm0_050") || fileNameBase.contains("wb_000_lesion_050"))
								{
									RiiplDebug.setProperty(item,eName + "/wbData/whlBrn["+sX+"]/imageType","LST 0.50");
								}
								else if (fileNameBase.contains("wb_000_lesion_lbm0_055") || fileNameBase.contains("wb_000_lesion_055"))
								{
									RiiplDebug.setProperty(item,eName + "/wbData/whlBrn["+sX+"]/imageType","LST 0.55");
								}
								else if (fileNameBase.contains("wb_000_lesion_lbm0_060") || fileNameBase.contains("wb_000_lesion_060"))
								{
									RiiplDebug.setProperty(item,eName + "/wbData/whlBrn["+sX+"]/imageType","LST 0.60");
								}
								else if (fileNameBase.contains("wb_000_lesion_lbm0_065") || fileNameBase.contains("wb_000_lesion_065"))
								{
									RiiplDebug.setProperty(item,eName + "/wbData/whlBrn["+sX+"]/imageType","LST 0.65");
								}
								else if (fileNameBase.contains("wb_000_lesion_lbm0_070") || fileNameBase.contains("wb_000_lesion_070"))
								{
									RiiplDebug.setProperty(item,eName + "/wbData/whlBrn["+sX+"]/imageType","LST 0.70");
								}
								else if (fileNameBase.contains("wb_000_lesion_lbm0_075") || fileNameBase.contains("wb_000_lesion_075"))
								{
									RiiplDebug.setProperty(item,eName + "/wbData/whlBrn["+sX+"]/imageType","LST 0.75");
								}
								else if (fileNameBase.contains("wb_000_lesion_lbm0_080") || fileNameBase.contains("wb_000_lesion_080"))
								{
									RiiplDebug.setProperty(item,eName + "/wbData/whlBrn["+sX+"]/imageType","LST 0.80");
								}
								else if (fileNameBase.contains("wb_000_lesion_lbm0_085") || fileNameBase.contains("wb_000_lesion_085"))
								{
									RiiplDebug.setProperty(item,eName + "/wbData/whlBrn["+sX+"]/imageType","LST 0.85");
								}
								else if (fileNameBase.contains("wb_000_lesion_lbm0_090") || fileNameBase.contains("wb_000_lesion_090"))
								{
									RiiplDebug.setProperty(item,eName + "/wbData/whlBrn["+sX+"]/imageType","LST 0.90");
								}
								else if (fileNameBase.contains("wb_000_lesion_lbm0_095") || fileNameBase.contains("wb_000_lesion_095"))
								{
									RiiplDebug.setProperty(item,eName + "/wbData/whlBrn["+sX+"]/imageType","LST 0.95");
								}
								else if (fileNameBase.contains("wb_000_lesion_lbm0_100") || fileNameBase.contains("wb_000_lesion_100"))
								{
									RiiplDebug.setProperty(item,eName + "/wbData/whlBrn["+sX+"]/imageType","LST 1.00");
								}
								else
								{
									RiiplDebug.setProperty(item,eName + "/wbData/whlBrn["+sX+"]/imageType","Unknown");
								}
								RiiplDebug.setProperty(item,eName + "/wbData/whlBrn["+sX+"]/wb",value);
								sX=sX+1;
								
								if (flair==null)
								{
									for (int fX=fileNameBase.length(); fX >= 0; fX--)
									{
										logger.debug("Flair search: "+fileNameBase.substring(fX));
										String[] searchString  = {"wr" + fileNameBase.substring(fX) + ".nii","wr" + fileNameBase.substring(fX) + ".nii.gz"};
										File[] FlairFiles = LSTdirs[0].listFiles((FileFilter) new WildcardFileFilter(searchString));
										if (FlairFiles.length == 1)
										{
											flair=LSTdirs[0].getAbsolutePath();
											imageFilters.add(new WildcardFileFilter(searchString));
											break;
										}
									}
								}
								imageFilters.add(new WildcardFileFilter(new String[] {fileNameBase + ".nii*"}));
							}
							catch (Exception e)
							{
								throw e;
							}
						}


//*********************************************************************************
					}
					else
					{
						csvFile = null;
					}
				}
				else if (LSTdirs.length > 1)
				{
					logger.error("More than one VBM8/lst8 directory in "+subjectDir+"/nifti/vbm8");
					data.setMessage("More than one VBM8/lst8 directory in "+subjectDir+"/nifti/vbm8");
					data.setScreenTemplate("Error.vm");
					return;
				}
			}

			if (processCsvFileTissue)
			{
				if (csvFile==null)
				{
					csvFile = new File(subjectDir+"/nifti/vbm8/"+csvFileName);
				}
				RiiplVbm8Utils.processTissueReport(data,item,eName,csvFile);
			}

		}
		catch (Exception e)
		{
			logger.error("Error setting QC numbers: ", e);
			data.setMessage("Error setting QC numbers: " + e);
			data.setScreenTemplate("Error.vm");
			return;
		}

		//QC Image
		try
		{
			if (seg8Txt==null)
			{
				logger.error("No p*_seg8.txt files found in "+subjectDir+"/nifti/vbm8");
				data.setMessage("No p*_seg8.txt files found in "+subjectDir+"/nifti/vbm8");
				data.setScreenTemplate("Error.vm");
				return;
			}
			else
			{
				logger.debug("Nifti dir: " + vbm8Dir);
				String t1Base=seg8Txt.getName();
				t1Base=t1Base.substring(1,t1Base.length()-9);
				logger.debug("T1 Base: " + t1Base);
				imageFilters.add(0,new WildcardFileFilter("w"+t1Base+".nii*"));

				ArrayList<File> files=new ArrayList<File>();
				for (int sX=0; sX < imageFilters.size(); sX++)
				{
					File[] niftis;
					if (sX==0)
					{
						niftis = vbm8Dir.listFiles((FileFilter)(imageFilters.get(sX)));
					}
					else
					{
						niftis = imageDir.listFiles((FileFilter)(imageFilters.get(sX)));
					}
					
					String nifti = null;
					if (niftis.length == 1)
					{
						nifti=niftis[0].getAbsolutePath();
						logger.debug("Image " +sX+ " is: "+ nifti);
						QCUtils.setQcImageFiles(item, eName, nifti,defaultQuality);
						files.add(new File(nifti));
					}
					else if (niftis.length > 1)
					{
						if (elementName.equals("riipl:NormVbm8vPCASLData"))
						{
							boolean hasMultipleCBF=false;
							boolean hasMultipleATT=false;
							Arrays.sort(niftis,NameFileComparator.NAME_INSENSITIVE_COMPARATOR);
							for (int nNifti=0; nNifti < niftis.length; nNifti++)
							{
								nifti=niftis[nNifti].getAbsolutePath();
								if (nifti.toLowerCase().contains("._cbf"))
								{
									if (hasMultipleCBF)
										data.setMessage(data.getMessage() + "Mutliple CBF nifti images! <br>");
									hasMultipleCBF=true;
								}
								if (nifti.toLowerCase().contains("._att"))
								{
									if (hasMultipleATT)
										data.setMessage(data.getMessage() + "Mutliple ATT nifti images! <br>");
									hasMultipleATT=true;
								}
								logger.debug("Image " +sX+ ":" +nNifti+" is: "+ nifti);
								QCUtils.setQcImageFiles(item, eName, nifti,defaultQuality);
								files.add(new File(nifti));
							}
						}
						else
						{
							data.setMessage(data.getMessage() + "Mutliple nifties image for filter "+ imageFilters.get(sX) + ":<br><ul>");
							for (int nNifti=0; nNifti < niftis.length; nNifti++)
							{
								data.setMessage(data.getMessage() + "<li>" + niftis[nNifti] +"</li>");
							}
							data.setMessage(data.getMessage() + "</ul>Only the first in the list above is shown.");

							nifti=niftis[0].getAbsolutePath();
							QCUtils.setQcImageFiles(item, eName, nifti,defaultQuality);
							files.add(new File(nifti));
						}
					}
					else
					{
						logger.error("Unable to find image: " + vbm8Dir + "/" + imageFilters.get(sX));
						data.setMessage("Unable to find image: " + vbm8Dir + "/" + imageFilters.get(sX));
						data.setScreenTemplate("Error.vm");
						return;
					}
				
				} 
			
				if (!useXtk)  //STATIC JPG QC IMAGE VIEWER
				{
					File tempOutFile=File.createTempFile(dataLabel,".jpg");
			
					String outFile=tempOutFile.getAbsolutePath();
					logger.debug("Temp Ouput montage file is: "+ outFile);
					NiftiSnapshot.createMontage(outFile, files,true,true);
					QCUtils.postQCMosaic(data,item,elementName,tempOutFile,qcDataIndex);
				}
				else //XTK INTERACTIVE QC IMAGE VIEWER
				{
					String server = TurbineUtils.GetFullServerPath();
					if (!server.endsWith("/"))
					{
						server +="/";
					}

					String uriBase=server + "REST/projects/"+om.getProjectData().getDisplayID()+"/subjects/"+mr.getSubjectData().getId();
					try
					{
						logger.debug("Base URI:        " + uriBase);
						logger.debug("Experiemnts:     " + mr.getId());
						logger.debug("qcSession Index: " + qcDataIndex);
						uriBase=uriBase+"/experiments/"+mr.getId()+"/resources/QC_IMAGES/files/";
					} 
					catch (Exception e)
					{
						logger.error("Cannot create REST URL to save mvmt graph: ", e);
						data.setMessage("Cannot create REST URL to save mvmt graph: " + e);
			//			data.setScreenTemplate("Error.vm");
						return;
					}

					Iterator it=files.iterator();
					while(it.hasNext())
					{
						int thisOrient=-1;
						File f=(File)it.next();
						Date cDate = new Date(f.lastModified());
						String cDateString = new SimpleDateFormat("yyyyMMdd_HHmmss").format(cDate);
						String baseFileName = f.getName();
						String uri=uriBase+cDateString+"/"+baseFileName+"?overwrite=false";
						QCUtils.postFile(data,uri,f);
					}
				}

			}
		}
		catch (Exception e)
		{
			logger.error("Error creating/setting QC image: ",e);
			data.setMessage("Error creating/setting QC image: " + e);
			data.setScreenTemplate("Error.vm");
			return;
		}

		//Movement Image
		try
		{
			if (rpProcess)
			{
				String fileBase=FilenameUtils.getBaseName((String)item.getProperty(eName+"/imgData[1]/file"));
				logger.debug("fileBase: " + fileBase);
				/* fileBase is usually something like: wrraadhs107003-0014-120-0001-1440-epiflexdhs_paslFilter_CBF
				* or wraadhs107003.05_56_19.20111118.P15360_CBF
				* Remove several of the first and last characters to make it look for the correct RP file
				*/
				fileBase=fileBase.substring(5);
				if (fileBase.lastIndexOf("_CBF") > 0)
				{
					fileBase=fileBase.substring(0,fileBase.lastIndexOf("_CBF"));
				}
				if (fileBase.lastIndexOf("_paslFilter") > 0)
				{
					fileBase=fileBase.substring(0,fileBase.lastIndexOf("_paslFilter"));
				}
				if (fileBase.lastIndexOf("_ATT") > 0)
				{
					fileBase=fileBase.substring(0,fileBase.lastIndexOf("_ATT"));
				}
				if (fileBase.lastIndexOf("_MTR") > 0)
				{
					fileBase=fileBase.substring(0,fileBase.lastIndexOf("_MTR"));
				}
				logger.debug("fileBase for rpProcess: " + fileBase);
				if (fileBase.endsWith(".") == true)
				{
					fileBase=fileBase.substring(0,fileBase.lastIndexOf("."));
				}

				//Could just listFiles method, except for ther may be multiple spep diectories.  
				//Using the FileUtils.listFiles option on all to be consistant.  Although, speps SHOULD
				//be in the nifti directory.  Keeping just in case that changes.

				String searchDirs[] = {"spep*","ASL_*","nifti","pasl*"};
				Collection possibleRpFiles = FileUtils.listFiles(vbm8Dir, new WildcardFileFilter("rp_*"+fileBase+"*.txt"), new WildcardFileFilter(searchDirs));
				if (possibleRpFiles.isEmpty())
				{
					possibleRpFiles = FileUtils.listFiles(new File(subjectDir), new WildcardFileFilter("rp_*"+fileBase+"*.txt"), new WildcardFileFilter(searchDirs));
				}
				if (possibleRpFiles.isEmpty())
				{
					logger.error("Unable to find RP file");
					data.setMessage("Unable to find RP file");
					data.setScreenTemplate("Error.vm");
					return;
				}

				File rpFile = (File)possibleRpFiles.iterator().next();
				String rpFileName = rpFile.getAbsolutePath();
				logger.debug("rpFile: " + rpFileName);
				Double maxV[] = new Double[6];

				File tempOutFile=File.createTempFile(dataLabel,".jpg");
				String outFile=tempOutFile.getAbsolutePath();
				logger.debug("Temp Ouput rpGraph file is: "+ outFile);

				rpSnapshot.createRPGraphic(outFile,rpFile, maxV);
			
				RiiplDebug.setProperty(item,eName + "/mvmtFile",rpFileName);
				RiiplDebug.setProperty(item,eName + "/mvmtImage",dataLabel+"_MVMT_"+new SimpleDateFormat("yyyy_MM_dd_HH_mm_ss").format(now) + ".jpg");
				RiiplDebug.setProperty(item,eName + "/mvmtQuality",defaultQuality);
				RiiplDebug.setProperty(item,eName + "/mvmtMaxX",maxV[0]);
				RiiplDebug.setProperty(item,eName + "/mvmtMaxY",maxV[1]);
				RiiplDebug.setProperty(item,eName + "/mvmtMaxZ",maxV[2]);
				RiiplDebug.setProperty(item,eName + "/mvmtMaxPitch",maxV[3]);
				RiiplDebug.setProperty(item,eName + "/mvmtMaxRoll",maxV[4]);
				RiiplDebug.setProperty(item,eName + "/mvmtMaxYaw",maxV[5]);
				RiiplDebug.setProperty(item,eName + "/mvmtDate",new Date(rpFile.lastModified()));
				RiiplDebug.setProperty(item,eName + "/mvmtTime",FORMAT_HHMMSS.format(new Date (rpFile.lastModified())));
				QCUtils.postMvmtGraph(data,item,elementName,tempOutFile,qcDataIndex);
			}
		}
		catch (Exception e)
		{
			logger.error("Error creating/setting QC RP image: ", e);
			data.setMessage("Error creating/setting QC RP image: " + e);
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
//						boolean deleteFile=true;
						boolean deleteFile=false; //don't delete any files
						for (int sX=0; sX <= qcDataIndex; sX++)
						{
							String sName=elementName + "/qcSession["+sX+"]";
							if (item.getProperty(sName + "/qcImage") != null)
							{
								try
								{
									if (item.getProperty(sName + "/qcImage").equals(fileI))
									{
										deleteFile=false;
										break;
									}
								}
								catch (Exception e) {}
							}
							if (item.getProperty(sName + "/mvmtImage") != null)
							{
								try
								{
									if (item.getProperty(sName + "/mvmtImage").equals(fileI))
									{
										deleteFile=false;
										break;
									}
								}
								catch (Exception e) {}
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
				logger.error("Warning: Trouble cleaning QC image listings: ",e);
				data.setMessage("Warning: Trouble cleaning QC image listings: " + e);
// Don't cause an error (only display message) if files not properly deleted
//				data.setScreenTemplate("Error.vm");
		}
	}

	/** Populates the normalization QC for NormVbm8[DTI|PASL|PCASL|MTR|...]Data 
	* @param		data					The "RunData" or Template session information
	* @param		item					The Datatype's DATA
	* @param		elementName		element name in the XSD
	* @param		expLabel			The experiment labeL suffix (will be appended to mr.getProperty("label"))
	* @param		csvFile				The tissue report (csv file)
	* @param		imageFilters	An array of image filters (see WildcardFileFilter)
	* @param		rpProcess		Find and process an rp_NNN.txt file to match first imageFilter
	* @return									void
	*/
	public static void normalizationQCedit(org.nrg.xdat.om.XnatMrsessiondata mr, RunData data, ItemI item, String elementName, String expLabel, String csvFileName, ArrayList<WildcardFileFilter> imageFilters, boolean rpProcess)
	{
		Date now = new Date();
		String eName=elementName;  //This gets used/modified in the script.

		int qcDataIndex = 0;
		
		SimpleDateFormat FORMAT_HHMMSS=new SimpleDateFormat("HH:mm:ss");
		SimpleDateFormat FORMAT_YYYYMMDD=new SimpleDateFormat("yyyy-MM-dd");

		//Find the QCData Index and set some basic fields
		try
		{
			String sName=eName + "/qcSession["+qcDataIndex+"]";

			
			do {
				logger.debug(sName + "/qcImage has data: " + item.getProperty(sName + "/quality"));
//				logger.info("Has username in index: " +qcDataIndex);
				qcDataIndex++;
				sName=eName + "/qcSession["+qcDataIndex+"]";
			} while (item.getProperty(sName + "/quality") != null);
			qcDataIndex--; //undo the last increment.
			sName=eName + "/qcSession["+qcDataIndex+"]"; //reset the sName

			eName=sName;
			logger.debug("qcIndexed eName="+eName);

			RiiplDebug.setProperty(item,eName + "/username",TurbineUtils.getUser(data).getUsername());
			RiiplDebug.setProperty(item,eName + "/date",now);
			RiiplDebug.setProperty(item,eName + "/time",FORMAT_HHMMSS.format(now));
		}
		catch (Exception e)
		{
			logger.error("Error finding QCData Index details: ",e);
			data.setMessage("Error finding QCData Index details: " + e);
			data.setScreenTemplate("Error.vm");
		}
		return;
	}



	/** Checks the VBM8 data type for file changes.
	* @param		data					The "RunData" or Template session information
	* @param		item					The Datatype's DATA
	* @param		eName					The element name in the XSD
	* @return									boolean (true=files OK, false=file have changed time or are missing)
	*/
	public static boolean checkFiles(RunData data, ItemI item, String eName) throws Exception, IOException
	{
		boolean OK = true;
		String message = new String();
		boolean qcSessionOK = true;
		String qcSessionMessage = new String();

		int qcDataIndex=0;
		try
		{
			String sName=eName + "/qcSession["+qcDataIndex+"]";
			while (item.getProperty(sName + "/qcImage") != null)
			{
				//Reset these items each time....don't worry about the "prior" settings
				qcSessionOK=true;
				qcSessionMessage=new String();
				int idDataIndex=0;
				String idName=sName+"/imgData["+idDataIndex+"]";

				while (item.getProperty(idName + "/file") != null)
				{
					logger.debug("Checking: " + idName + "/file");
					try
					{
						if (QCUtils.compareFileDateTimeExist(item, idName, "file", "date", "time") != 0)
						{
							qcSessionMessage = qcSessionMessage + item.getProperty(idName+"/file") + " has changed.<br>";
							qcSessionOK=false;
						}
					}
					catch (IOException e)
					{
						qcSessionMessage = qcSessionMessage + item.getProperty(idName+"/file") + " no longer exists.<br>";
						qcSessionOK=false;
					}
					catch (Exception e)
					{
						logger.error("Error: ", e);
						qcSessionMessage = qcSessionMessage + "Error checking: " + item.getProperty(idName+"/file") + ".<br>";
						qcSessionOK=false;
					}
					idDataIndex=idDataIndex+1;
					idName=sName+"/imgData["+idDataIndex+"]";
				}
				qcDataIndex=qcDataIndex+1;
				sName=eName + "/qcSession["+qcDataIndex+"]";
			}
			OK = OK & qcSessionOK;
			message = message + qcSessionMessage;
			data.setMessage(message);
		}
		catch (Exception e) 
		{
			logger.error("Error: Trouble checking file times",e);
			data.setMessage("Error: Trouble checking file times: " + e);
			data.setScreenTemplate("Error.vm");
		}
		return OK;
	}

	/** Checks the Vbm8.properies data file for nifti/vbm8 location
	* @param		project				The project ot lookup
	* @return									String for nifti/vbm8 location
	*/
	public static String getVbm8Dir(String project) throws Exception, IOException
	{
		Properties prop = new Properties();
		try
		{
			//load a properties file
			prop.load(new FileInputStream(XFT.GetConfDir()+"Vbm8.properties"));
		}
		catch (IOException ex) 
		{
			ex.printStackTrace();
		}

		logger.debug("Read " + prop.size() + " vbm8 project mappingfrom properties file");


		Enumeration e = prop.propertyNames();
		while (e.hasMoreElements())
		{
			String key = (String) e.nextElement();
			String value = prop.getProperty(key);
			logger.debug("Checking project name '"+ project +"' to project "+ key);

			if (project.length() < key.length())
				continue;
			if (project.substring(0,key.length()).toUpperCase().equals(key.toUpperCase()))
			{
				logger.info("Matched project name '"+ project +"' to project "+ key);
				return (value);
			}
		}
		logger.info("No matches for project name '"+ project +"' in " + prop.size() + " projects");
		return ("nifti/vbm8");
	}


}

