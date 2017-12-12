package edu.wfubmc.riiplqa;

//XNAT imports
import org.apache.turbine.util.RunData;
import org.apache.velocity.context.Context;
import org.nrg.xdat.turbine.utils.TurbineUtils;
import org.nrg.xft.ItemI;
import org.nrg.xft.XFTItem;

import edu.wfubmc.riiplqa.NiftiSnapshot;
import edu.wfubmc.riiplqa.QCUtils;
import edu.wfubmc.riiplqa.RiiplDebug;
import edu.wfubmc.riiplqa.RiiplVbm8Utils;
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
	Searches in the nifti/vbm8/  for a p*_seg8.txt.
	Parses the txt file and creates a jpg image for display and stores values from txt.
 * @author Ben Wagner
 * @author ANSIR Lab
 * @version 0.1 October 2011
 */
public class Upload_riiplqa_SegVbm8Data extends org.nrg.xnat.turbine.modules.screens.EditImageAssessorScreen {
	static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(Upload_riiplqa_SegVbm8Data.class);
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
		return "riiplqa:SegVbm8Data";
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
		logger.debug("Upload_riiplqa_SegVbm8Data::finalProcessing");
		org.nrg.xdat.om.RiiplSegvbm8data om = new org.nrg.xdat.om.RiiplSegvbm8data(item);
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
			label = mr.getProperty("label")+"_SegVbm8";
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
			logger.error("SEG VBM8 session already exists.  Please delete the existing session before trying to re-upload.");
			data.setMessage("SEG VBM8 session already exists.  Please delete the existing session before trying to re-upload.");
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
		File seg8Txt=null;
		String niftiVbm8Dir=null;
		try
		{
			niftiVbm8Dir = RiiplVbm8Utils.getVbm8Dir(om.getProjectData().getDisplayID());
			File dir = new File(subjectDir+"/" + niftiVbm8Dir);
			if (!dir.exists())
			{
				logger.error(subjectDir+"/"+niftiVbm8Dir+" does not exist");
				data.setMessage(subjectDir+"/"+niftiVbm8Dir+" does not exist");
				data.setScreenTemplate("Error.vm");
				return;
			}
			FileFilter fileFilter = new WildcardFileFilter("p*_seg8.txt");
			File[] seg8TxtFiles = dir.listFiles(fileFilter);

			if (seg8TxtFiles.length == 0)
			{
				logger.error("No p*_seg8.txt files found in "+subjectDir+"/"+niftiVbm8Dir);
				data.setMessage("No p*_seg8.txt files found in "+subjectDir+"/"+niftiVbm8Dir);
				data.setScreenTemplate("Error.vm");
				return;
			}
			else if (seg8TxtFiles.length > 1)
			{
				logger.error("Too many p*_seg8.txt files found in "+subjectDir+"/"+niftiVbm8Dir+": Count:"+seg8TxtFiles.length);
				data.setMessage("Too many p*_seg8.txt files found in "+subjectDir+"/"+niftiVbm8Dir+": Count:"+seg8TxtFiles.length);
				data.setScreenTemplate("Error.vm");
				return;
			}

			seg8Txt=seg8TxtFiles[0];
			logger.debug("seg8Txt: " + seg8Txt.getAbsolutePath());

			RiiplDebug.setProperty(item,eName + "/tisData/file",seg8Txt.getAbsolutePath());
			RiiplDebug.setProperty(item,eName + "/tisData/filedate",new Date(seg8Txt.lastModified()));
			RiiplDebug.setProperty(item,eName + "/tisData/filetime",FORMAT_HHMMSS.format(new Date(seg8Txt.lastModified())));
			RiiplDebug.setProperty(item,eName + "/tisData/quality",defaultQuality);

			boolean processedFirstLine=false;
			BufferedReader bufRdr = new BufferedReader(new FileReader(seg8Txt));
			String line=null;

			while ((line=bufRdr.readLine())!=null)
			{
				if (processedFirstLine)
				{
					//Only processes the first line...ignore the rest
					logger.debug("Ignoring extra in seg8.txt: " + line);
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
			if (seg8Txt==null)
			{
				logger.error("No p*_seg8.txt files found in "+subjectDir+"/"+niftiVbm8Dir);
				data.setMessage("No p*_seg8.txt files found in "+subjectDir+"/"+niftiVbm8Dir);
				data.setScreenTemplate("Error.vm");
				return;
			}
			else
			{
				String vbm8Dir=seg8Txt.getParent();
				logger.debug("Nifti dir: " + vbm8Dir);
				String t1Base=seg8Txt.getName();
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

					if (new File(vbm8Dir + "/" + nifti + ".nii.gz").exists())
					{
						nifti=vbm8Dir + "/" + nifti + ".nii.gz";
					}
					else if (new File(vbm8Dir + "/" + nifti + ".nii").exists())
					{
						nifti=vbm8Dir + "/" + nifti + ".nii";
					}
					else
					{
						logger.error("Unable to find image: " + vbm8Dir + "/" + nifti + ".nii[.gz]");
						data.setMessage("Unable to find image: " + vbm8Dir + "/" + nifti + ".nii[.gz]");
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
						//data.setScreenTemplate("Error.vm");
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

/* Only do QC image cleanup in EDIT!!!! */
/*

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
						boolean deleteFile=true;
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
// Don't cause an error (only display message) if files not properly deleted
//				data.setScreenTemplate("Error.vm");
		}
*/
	}
}
