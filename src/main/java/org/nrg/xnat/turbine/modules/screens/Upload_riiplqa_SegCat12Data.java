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
import edu.wfubmc.RiiplQAPlugin.RiiplCat12Utils;
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
import java.util.Iterator;
import java.util.StringTokenizer;
import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.json.JSONArray;
import org.json.JSONObject;


/** 
	Searches in the nifti/cat12/  for a p*_seg12.txt.
	Parses the txt file and creates a jpg image for display and stores values from txt.
 * @author Richard Barcus
 * @author RIIPL
 * @version 0.1 January 2018
 */
public class Upload_riiplqa_SegCat12Data extends org.nrg.xnat.turbine.modules.screens.EditImageAssessorScreen {
	static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(Upload_riiplqa_SegCat12Data.class);
	/* (non-Javadoc)
	 * @see org.nrg.xdat.turbine.modules.screens.EditScreenA#getElementName()
	 */

	private String defaultQuality=null;
	private Date now=null;
	private String subjectDir=null;
	private String dataLabel;
	private Boolean useXtk=true;

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
		logger.debug("Upload_riiplqa_SegCat12Data::finalProcessing");
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

		//create label
		String label = null;
		try
		{
			label = mr.getProperty("label")+"_SegCat12";
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
			logger.error("SEG CAT12 session already exists.  Please delete the existing session before trying to re-upload.");
			data.setMessage("SEG CAT12  session already exists.  Please delete the existing session before trying to re-upload.");
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
			eName=sName;
			logger.debug("qcIndexed eName="+eName);

			RiiplDebug.setProperty(item,eName + "/username",TurbineUtils.getUser(data).getUsername());
			RiiplDebug.setProperty(item,eName + "/date",now);
			RiiplDebug.setProperty(item,eName + "/time",FORMAT_HHMMSS.format(now));
			RiiplDebug.setProperty(item,eName + "/quality",defaultQuality);
			RiiplDebug.setProperty(item,eName + "/comments","");
			if (!useXtk)
			{
				RiiplDebug.setProperty(item,eName + "/qcImage",dataLabel+"_"+new SimpleDateFormat("yyyy_MM_dd_HH_mm_ss").format(now) + ".jpg");
			}
			else
			{
				RiiplDebug.setProperty(item,eName + "/qcImage","");
			}
		}
		catch (Exception e)
		{
			logger.error("Error finding QCData Index details: " + e);
			data.setMessage("Error finding QCData Index details: " + e + "<br>" + Arrays.toString(e.getStackTrace()));
			data.setScreenTemplate("Error.vm");
			return;
		}

		//Read CSV file and set QC Numbers/images
		File seg12Txt=null;
		String niftiCat12Dir=null;
		try
		{
			niftiCat12Dir = RiiplCat12Utils.getCat12Dir(om.getProjectData().getDisplayID());
			File dir = new File(subjectDir+"/" + niftiCat12Dir);
			if (!dir.exists())
			{
				logger.error(subjectDir+"/"+niftiCat12Dir+" does not exist");
				data.setMessage(subjectDir+"/"+niftiCat12Dir+" does not exist");
				data.setScreenTemplate("Error.vm");
				return;
			}
			FileFilter fileFilter = new WildcardFileFilter("p*_seg12.txt");
			File[] seg12TxtFiles = dir.listFiles(fileFilter);

			if (seg12TxtFiles.length == 0)
			{
				logger.error("No p*_seg12.txt files found in "+subjectDir+"/"+niftiCat12Dir);
				data.setMessage("No p*_seg12.txt files found in "+subjectDir+"/"+niftiCat12Dir);
				data.setScreenTemplate("Error.vm");
				return;
			}
			else if (seg12TxtFiles.length > 1)
			{
				logger.error("Too many p*_seg12.txt files found in "+subjectDir+"/"+niftiCat12Dir+": Count:"+seg12TxtFiles.length);
				data.setMessage("Too many p*_seg12.txt files found in "+subjectDir+"/"+niftiCat12Dir+": Count:"+seg12TxtFiles.length);
				data.setScreenTemplate("Error.vm");
				return;
			}

			seg12Txt=seg12TxtFiles[0];
			logger.debug("seg12Txt: " + seg12Txt.getAbsolutePath());

			RiiplDebug.setProperty(item,eName + "/tisData/file",seg12Txt.getAbsolutePath());
			RiiplDebug.setProperty(item,eName + "/tisData/filedate",new Date(seg12Txt.lastModified()));
			RiiplDebug.setProperty(item,eName + "/tisData/filetime",FORMAT_HHMMSS.format(new Date(seg12Txt.lastModified())));
			RiiplDebug.setProperty(item,eName + "/tisData/quality",defaultQuality);

			boolean processedFirstLine=false;
			BufferedReader bufRdr = new BufferedReader(new FileReader(seg12Txt));
			String line=null;

			while ((line=bufRdr.readLine())!=null)
			{
				if (processedFirstLine)
				{
					logger.debug("Ignoring extra in seg12.txt: " + line);
					continue;
				}
				else
				{
					StringTokenizer st = new StringTokenizer(line,"\t");
					RiiplDebug.setProperty(item,eName + "/tisData/tissue[0]/gm",st.nextToken());
					RiiplDebug.setProperty(item,eName + "/tisData/tissue[0]/wm",st.nextToken());
					RiiplDebug.setProperty(item,eName + "/tisData/tissue[0]/csf",st.nextToken());
				}
			}
		}
		catch (Exception e)
		{
			logger.error("Error setting QC numbers: " + e);
			data.setMessage("Error setting QC numbers: " + e + "<br>" + Arrays.toString(e.getStackTrace()));
			data.setScreenTemplate("Error.vm");
			return;
		}

		//QC Image
		try
		{
			if (seg12Txt==null)
			{
				logger.error("No p*_seg12.txt files found in "+subjectDir+"/"+niftiCat12Dir);
				data.setMessage("No p*_seg12.txt files found in "+subjectDir+"/"+niftiCat12Dir);
				data.setScreenTemplate("Error.vm");
				return;
			}
			else
			{
				String cat12Dir=seg12Txt.getParent();
				logger.debug("Nifti dir: " + cat12Dir);
				String t1Base=seg12Txt.getName();
				t1Base=t1Base.substring(1,t1Base.length()-9);
				logger.debug("T1 Base: " + t1Base);

				ArrayList<File> files=new ArrayList<File>();
				for (int sX=0; sX <= 4; sX++)
				{
					String nifti;
					if (sX==0)
					{
						nifti="wmr"+t1Base;
					}
					else
					{
						nifti="wrp"+(sX-1)+t1Base;
					}

					if (new File(cat12Dir + "/" + nifti + ".nii.gz").exists())
					{
						nifti=cat12Dir + "/" + nifti + ".nii.gz";
					}
					else if (new File(cat12Dir + "/" + nifti + ".nii").exists())
					{
						nifti=cat12Dir + "/" + nifti + ".nii";
					}
					else
					{
						logger.error("Unable to find image: " + cat12Dir + "/" + nifti + ".nii[.gz]");
						data.setMessage("Unable to find image: " + cat12Dir + "/" + nifti + ".nii[.gz]");
						data.setScreenTemplate("Error.vm");
						return;
					}

					QCUtils.setQcImageFiles(item, eName, nifti,defaultQuality);
					files.add(new File(nifti));
				}

				if (!useXtk)  //STATIC JPG QC IMAGE VIEWER
				{
					File tempOutFile=File.createTempFile(dataLabel,".jpg");

					String outFile=tempOutFile.getAbsolutePath();
					logger.debug("Temp Ouput montage file is: "+ outFile);
					NiftiSnapshot.createMontage(outFile, files,true,true);
					QCUtils.postQCMosaic(data,item,this.getElementName(),tempOutFile,qcDataIndex);
				}
				else //XTK INTERACTIVE QC IMAGE VIEWER
				{
					String server = TurbineUtils.GetFullServerPath();
					if (!server.endsWith("/"))
					{
						server +="/";
					}

					String uriBase=server + "data/archive/projects/"+om.getProjectData().getDisplayID()+"/subjects/"+mr.getSubjectData().getId();
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
			logger.error("Error creating/setting QC image: " + e);
			data.setMessage("Error creating/setting QC image: " + e + "<br>" + Arrays.toString(e.getStackTrace()));
			data.setScreenTemplate("Error.vm");
			return;
		}
	}
}
