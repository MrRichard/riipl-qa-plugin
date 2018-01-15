package edu.wfubmc.RiiplQAPlugin;

// Java 
import java.io.*;
import java.util.Date;
import java.text.SimpleDateFormat;
import java.net.URI;

// other important
import org.apache.commons.io.IOUtils;
import org.json.JSONObject;

// turbine 
import org.apache.turbine.util.RunData;
import org.nrg.xdat.turbine.utils.TurbineUtils;

// httpcomponents - auth
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
// httpcomponents - client 
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.client.AuthCache;

import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.FileBody;

//httpcomponents - impl 
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.client.BasicAuthCache;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.auth.BasicScheme;

//httpcomponents - other 
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.StatusLine;
import org.apache.http.util.EntityUtils;

// xdat
import org.nrg.xdat.XDAT;
import org.nrg.xdat.security.XDATUser;
import org.nrg.xdat.services.AliasTokenService;
import org.nrg.xdat.entities.AliasToken;
import org.nrg.xft.ItemI;
import org.nrg.xft.security.UserI;

import edu.wfubmc.RiiplQAPlugin.RiiplDebug;


/** A class for QC Utilities common to the somewhat standard image QC framework
* @author Richard Barcus
* @author RIIPL Lab
* @version 0.4 May 2017
*/
public class QCUtils
{
	static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(QCUtils.class);
	static SimpleDateFormat FORMAT_HHMMSS=new SimpleDateFormat("HH:mm:ss");
	static SimpleDateFormat FORMAT_YYYYMMDD=new SimpleDateFormat("yyyy-MM-dd");

	/** Sets the imgData[x] fields file/date/time/quality if the given image is different
	* @param		eName						The element name in the XSD
	* @param		fileName				The image file
	* @param		defaultQuality	The default "quality" for the image...usually "unread"
	* @return										void
	*/
	public static void setQcImageFiles(ItemI item, String eName, String fileName, String defaultQuality)
	{
		try
		{
			if (fileName.length()==0)
				return;

			int sX = 0;
			int sY = 0;
			String sName=eName + "/imgData["+sX+"]";
			Date cDate = new Date(new File(fileName).lastModified());

			while (item.getProperty(sName + "/file") != null && !item.getProperty(sName + "/file").equals(fileName))
			{
				sX++;
				sName=eName + "/imgData["+sX+"]";
			}

			if (item.getProperty(sName + "/file") == null)
			{
				RiiplDebug.setProperty(item,sName+"/file",fileName);
			}

			if (item.getProperty(sName + "/date") == null || QCUtils.compareDates(QCUtils.fixDate(item.getProperty(sName + "/date")),cDate) != 0)
			{
				RiiplDebug.setProperty(item,sName + "/date",cDate);
			}

			if (item.getProperty(sName + "/time") == null || QCUtils.compareTimes(item.getProperty(sName + "/time"),cDate) != 0)
			{
				RiiplDebug.setProperty(item,sName + "/time",FORMAT_HHMMSS.format(cDate));
			}
			if (item.getProperty(sName + "/quality") == null)
			{
				RiiplDebug.setProperty(item,sName + "/quality",defaultQuality);
			}
            if (item.getProperty(sName + "/status") == null)
            {
                RiiplDebug.setProperty(item,sName + "/status",defaultQuality);
            }

		}
		catch (Exception e)
		{
			logger.warn("Unable process filename: "+fileName);
		}
	}

	/** post a qcImage (Mosaic) to the RESOURCE catalog
	* Method from PlexiViewer / src / org / nrg / plexiViewer / converter / WebBasedQCImageCreator.java 
	*
	* @param		data					The "RunData" or Template session information
	* @param		item					The Datatype's DATA
	* @param		elementName				The element name (datatype) associated with image
	* @param		file					The image file to upload
	* @param		qcDataIndex				The QC Session the this image is tagged to
	* @return								void
	*/
	public static void postQCMosaic(RunData data, ItemI item, String elementName, File file,int qcDataIndex) throws IOException, Exception {
		org.nrg.xdat.om.XnatMrassessordata om = new org.nrg.xdat.om.XnatMrassessordata(item);
		org.nrg.xdat.om.XnatMrsessiondata mr = om.getMrSessionData();

		String label=elementName.replaceFirst(".*:","");
		String server = TurbineUtils.GetFullServerPath();
		if (!server.endsWith("/"))
		{
			server +="/";
		}

		String uri=server + "data/archive/projects/"+om.getProjectData().getDisplayID()+"/subjects/"+mr.getSubjectData().getId();
		try
		{
			logger.debug("Base URI:        " + uri);
			logger.debug("Experiemnts:     " + mr.getId());
			logger.debug("ElementName:     " + elementName);
			logger.debug("qcSession Index: " + qcDataIndex);
			uri=uri+"/experiments/"+mr.getId()+"/resources/QC_IMAGES/files/"+item.getProperty(elementName + "/qcSession["+qcDataIndex+"]/qcImage");
		}
		catch (Exception e)
		{
			logger.error("Cannot create REST URL to save image montage", e);
			data.setMessage("Cannot create REST URL to save image montage: " + e);

			return;
		}
		postFile(data,uri,file);
	}

	/** post a mvmtImage (movement/rotation) image to the RESOURCE catalog
	*
	* Method from PlexiViewer / src / org / nrg / plexiViewer / converter / WebBasedQCImageCreator.java 
	*
	* @param		data					The "RunData" or Template session information
	* @param		item					The Datatype's DATA
	* @param		elementName				The element name (datatype) associated with image
	* @param		file					The image file to upload
	* @param		qcDataIndex				The QC Session the this image is tagged to
	* @return								void
	*/
	public static void postMvmtGraph(RunData data, ItemI item, String elementName, File file,int qcDataIndex) throws IOException, Exception {
		org.nrg.xdat.om.XnatMrassessordata om = new org.nrg.xdat.om.XnatMrassessordata(item);
		org.nrg.xdat.om.XnatMrsessiondata mr = om.getMrSessionData();

		String label=elementName.replaceFirst(".*:","");
		String server = TurbineUtils.GetFullServerPath();
		if (!server.endsWith("/"))
		{
			server +="/";
		}

		String uri=server + "data/archive/projects/"+om.getProjectData().getDisplayID()+"/subjects/"+mr.getSubjectData().getId();
		try
		{
			logger.debug("Base URI:        " + uri);
			logger.debug("Experiments:     " + mr.getId());
			logger.debug("ElementName:     " + elementName);
			logger.debug("qcSession Index: " + qcDataIndex);
			uri=uri+"/experiments/"+mr.getId()+"/resources/QC_IMAGES/files/"+item.getProperty(elementName + "/qcSession["+qcDataIndex+"]/mvmtImage");
		} 
		catch (Exception e)
		{
			logger.error("Cannot create REST URL to save mvmt graph: ", e);
			data.setMessage("Cannot create REST URL to save mvmt graph: " + e);
			data.setScreenTemplate("Error.vm");
			return;
		}

		postFile(data,uri,file);
	}

	/** post a file to the RESOURCE catalog
	* 
	* Posts a file named: NormVbm8PCASLData_identifier_2012_01_18_16_07_25.jpg (element_identifier_date_time.jpg) to
	* mrSession's root resource.
	*
	* Method from PlexiViewer / src / org / nrg / plexiViewer / converter / WebBasedQCImageCreator.java 
	*
	* @param		data					The "RunData" or Template session information
	* @param		uriString			    URI to upload to
	* @param		file					The image File to upload
	* @return								oid
	*/

	public static void postFile(RunData data, String uriString, File file) throws IOException, Exception 
	{
		logger.debug("postFile:File:             " + file.getAbsolutePath());
		logger.debug("postFile:URI:              " + uriString);

		int stsCode = -1;
		
		CloseableHttpClient httpclient = HttpClients.createDefault();
		
        try {
            HttpPost httppost = new HttpPost(uriString);
            FileBody bin = new FileBody(file);
            HttpEntity reqEntity = MultipartEntityBuilder.create()
                    .addPart("bin", bin)
                    .build();
            httppost.setEntity(reqEntity);

            URI uri=new URI(uriString);
            HttpClientContext context = getContext( uri , data);
            
            CloseableHttpResponse response = httpclient.execute(httppost, context);
            
            try {
            	logger.debug("----------------------------------------");
            	logger.debug(response.getStatusLine());
            	
            	StatusLine sl = response.getStatusLine();
    			logger.debug(sl);
    			stsCode = sl.getStatusCode();
            	
                HttpEntity resEntity = response.getEntity();
                if (resEntity != null) {
                	logger.debug("----------------------------------------");
    				logger.debug("Response content length: " + resEntity.getContentLength());
    				logger.debug("Chunked?: " + resEntity.isChunked());
                }
                EntityUtils.consume(resEntity);
            } finally {
                response.close();
            }
        } finally {
        	httpclient.close();
        }

		switch (stsCode)
		{
			case -1:
				data.setMessage("QC Image upload error (" + stsCode +"): try/catch error");
				break;
			case 200:
                break;
			case 400:
				data.setMessage("QC Image upload error (" + stsCode +"): Bad Request/Syntax");
				break;
			case 401:
				data.setMessage("QC Image upload error (" + stsCode +"): Unauthorized");
				break;
			default:
				data.setMessage("QC Image upload error (" + stsCode +"): Unknown");
				break;
		}
	}

	/** delete a file from the RESOURCE catalog.  BE CAREFUL USING THIS DIRECT METHOD
	*
	* @param		data					The "RunData" or Template session information
	* @param		uriString				The URI of the file to be deleted.
	* @return									void
	*/
	public static void deleteFile(RunData data,String uriString)
	{
		
	}

	/** get a file 
	* 
	* Gets a file named
	*
	* @param		data					The "RunData" or Template session information
	* @param		uriString				URI to get from to
	* @param		file					The File to retrieve into
	* @return									void
	*/
	public static void getFile(RunData data, String uriString, File file) throws IOException, Exception
	{
		logger.debug("getFile:File:             " + file.getAbsolutePath());
		logger.debug("getFile:URI:              " + uriString);

		FileOutputStream fop = new FileOutputStream(file);
		int stsCode = -1;

		URI uri=new URI(uriString);
		CloseableHttpClient httpclient = HttpClients.createDefault();
		HttpGet httpget = new HttpGet(uri);
		HttpClientContext context = getContext(uri , data);
		CloseableHttpResponse response = httpclient.execute(httpget , context);
		HttpEntity resEntity = response.getEntity();

		StatusLine sl = response.getStatusLine();
		logger.debug(sl);
		stsCode = sl.getStatusCode();

		if (resEntity != null) 
		{
			logger.debug("----------------------------------------");
			logger.debug("Response content length: " + resEntity.getContentLength());
			logger.debug("Chunked?: " + resEntity.isChunked());

			IOUtils.copy(resEntity.getContent(), fop);
			logger.debug("File Cleanup");
			fop.flush();
			if (fop != null) 
			{
				fop.close();
			}
		}
		
		httpclient.close();
		
		switch (stsCode)
		{
			case -1:
				data.setMessage("QCUtils download error (" + stsCode +"): try/catch error");
				break;
			case 200:
				//OK
				break;
			case 400:
				data.setMessage("QCUtils download error (" + stsCode +"): Bad Request/Syntax");
				break;
			case 401:
				data.setMessage("QCUtils download error (" + stsCode +"): Unauthorized");
				break;
			default:
				data.setMessage("QCUtils upload error (" + stsCode +"): Unknown");
				break;
		}
	}

	/** list files in an experiment
	*
	* @param		data					The "RunData" or Template session information
	* @param		item					The Datatype's DATA
	* @return									JSONArray of items
	* @throws IOException 
	*/
	public static JSONObject listFiles(RunData data, ItemI item) throws IOException
	{
		JSONObject retValues=null;

		org.nrg.xdat.om.XnatMrassessordata om = new org.nrg.xdat.om.XnatMrassessordata(item);
		org.nrg.xdat.om.XnatMrsessiondata mr = om.getMrSessionData();

		String server = TurbineUtils.GetFullServerPath();

		if (!server.endsWith("/"))
		{
			server +="/";
		}

		String uriString=server + "data/archive/projects/"+om.getProjectData().getDisplayID()+"/subjects/"+mr.getSubjectData().getId();
		try
		{
			uriString=uriString+"/experiments/"+mr.getId()+"/resources/QC_IMAGES/files?format=json";
		} 
		catch (Exception e)
		{
			logger.error("Cannot create REST URL", e);
			data.setMessage("Cannot create REST URL: " + e);
			return retValues;
		}

		logger.debug("listFiles:URI:              " + uriString);

		int stsCode = -1;

		CloseableHttpClient httpclient = HttpClients.createDefault();

		try
		{
			URI uri=new URI(uriString);
			HttpGet httpget = new HttpGet(uri);
			HttpClientContext context = getContext(uri , data);
			CloseableHttpResponse response = httpclient.execute(httpget,context);
			HttpEntity resEntity = response.getEntity();

			StatusLine sl = response.getStatusLine();
			logger.debug(sl);
			stsCode = sl.getStatusCode();

			if (resEntity != null)
			{
				logger.debug("----------------------------------------");
				logger.debug("Response content length: " + resEntity.getContentLength());
				logger.debug("Chunked?: " + resEntity.isChunked());
				retValues=new JSONObject(EntityUtils.toString(resEntity));
			}
		}
		catch (Exception e)
		{
			logger.debug("listFiles GET METHOD error: ", e);
		}
		finally
		{
			if (httpclient != null)
			{
				httpclient.close();
			}
		}

		switch (stsCode)
		{
			case -1:
				data.setMessage("QC List Images error (" + stsCode +"): try/catch error");
				break;
			case 200:
				//OK
				break;
			case 400:
				data.setMessage("QC List Images upload error (" + stsCode +"): Bad Request/Syntax");
				break;
			case 401:
				data.setMessage("QC List Images upload error (" + stsCode +"): Unauthorized");
				break;
			default:
				data.setMessage("QC List Images upload error (" + stsCode +"): Unknown");
				break;
		}
	return retValues;
	}

	/** return if a boolean of wether an assessor already exists
	*
	* @param		data					The "RunData" or Template session information
	* @param		item					The Datatype's DATA
	* @param		assessor				A String with the assessor's name or ID
	* @return								false (no existing item) or true.  On failure returns false.
	*/
	public static Boolean isExistingAssessor(RunData data, ItemI item, String assessor)
	{
		org.nrg.xdat.om.XnatMrassessordata om = new org.nrg.xdat.om.XnatMrassessordata(item);
		org.nrg.xdat.om.XnatMrsessiondata mr = om.getMrSessionData();

		String server = TurbineUtils.GetFullServerPath();

		if (!server.endsWith("/"))
		{
			server +="/";
		}

		String uriString=server + "data/archive/projects/"+om.getProjectData().getDisplayID()+"/subjects/"+mr.getSubjectData().getId();
		try
		{
			uriString=uriString+"/experiments/"+mr.getId()+"/assessors/"+assessor;
		} 
		catch (Exception e)
		{
			logger.error("Cannot create REST URL", e);
			data.setMessage("Cannot create REST URL: " + e);
			return false;
		}

		logger.debug("isExistingAssessor:URI:              " + uriString);

		int stsCode = -1;

		// Prepare a request object
		CloseableHttpClient httpclient = HttpClients.createDefault();
		try
		{
			URI uri=new URI(uriString);			
			HttpClientContext context = getContext(uri,data);
			HttpGet request = new HttpGet(uriString);
			CloseableHttpResponse response = null;
			response = httpclient.execute(request, context);
			HttpEntity resEntity = response.getEntity();
		    stsCode = response.getStatusLine().getStatusCode();
			StatusLine s1 = response.getStatusLine();
			stsCode = response.getStatusLine().getStatusCode();
			logger.debug(s1);

			if (resEntity != null)
			{
				logger.debug("----------------------------------------");
				logger.debug("Response content length: " + resEntity.getContentLength());
				logger.debug("Chunked?: " + resEntity.isChunked());
			}
		}
		catch (Exception e)
		{
			logger.debug("listFiles GET METHOD error: ", e);
		}
		finally
		{
			if (httpclient != null)
			{
				try {
					httpclient.close();
				} catch (IOException e) {
					logger.debug("----------------------------------------");
					logger.debug(e.getMessage());
				}
			}
		}

		switch (stsCode)
		{
			case -1:
				logger.debug("isExistingAssessor response (" + stsCode +"): try/catch error");
				return false;
			case 200:
				logger.debug("isExistingAssessor response (" + stsCode +"): OK");
				return true;
			case 400:
				logger.debug("isExistingAssessor response (" + stsCode +"): Bad Request/Syntax");
				return false;
			case 401:
				logger.debug("isExistingAssessor response (" + stsCode +"): Unauthorized");
				return false;
			default:
				logger.debug("isExistingAssessor response (" + stsCode +"): Unknown");
				return false;
		}
	}

	/** Compares 2 strings as DATES
	* @param		date1
	* @param		date2
	* @return		date1 is before (-1), same (0), or after (1) date2
	*/
	public static int compareDates(java.util.Date date1, java.util.Date date2)
	{
		if (date1 == date2)
		{
			return 0;
		}
		else if (date1.before(date2))
		{
			return -1;
		}
		else
		{
			return 1;
		}
	}

	/** Compares 2 strings as TIMES through SimpleDateFormat
	* @param		time1
	* @param		time2
	* @return		time1 is before (-1), same (0), or after (1) time2
	*/
	public static int compareTimes(java.lang.Object t1, java.lang.Object t2) throws java.text.ParseException
	{
		java.util.Date time1 = null;
		java.util.Date time2 = null;

		try
		{
			time1 = FORMAT_HHMMSS.parse(t1.toString());
			time2 = FORMAT_HHMMSS.parse(t1.toString());
		}
		catch (java.text.ParseException e)
		{
			logger.error(String.format("Error: Trouble converting times: %s and %s", t1.toString(), t2.toString()));
			throw e;
		}

		if (time1 == time2)
		{
			return 0;
		}
		else if (time1.before(time2))
		{
			return -1;
		}
		else
		{
			return 1;
		}
	}

	/** Fixes problems of multiple "DATE" types, converting to java.util.Date)
	* @param		a java.util.Date
	* @return		a java.util.Date
	*/
	public static java.util.Date fixDate(java.util.Date in)
	{
		//Ha!  do nothing
		return in;
	}

	/** Fixes problems of multiple "DATE" types, converting to java.util.Date)
	* @param		a java.sql.Date
	* @return		a java.util.Date
	*/
	public static java.util.Date fixDate(java.sql.Date in)
	{
		java.util.Date out = new java.util.Date(in.getTime());
		return out;
	}

	/** Fixes problems of multiple "DATE" types, converting to java.util.Date)
	* @param		a String Date
	* @return		a java.util.Date
	*/
	public static java.util.Date fixDate(String in) throws java.text.ParseException
	{
		java.util.Date out = FORMAT_YYYYMMDD.parse(in);
		return out;
	}

	/** Fixes problems of multiple "DATE" types, converting to java.util.Date)
	* @param		a java.lang.Object Date
	* @return		a java.util.Date
	*/
	public static java.util.Date fixDate(java.lang.Object in) throws java.text.ParseException
	{
		java.util.Date out = FORMAT_YYYYMMDD.parse(in.toString());
		return out;
	}


	/** Checks that eName/fieldField exists and that the time and date matches.  Throws IOException if file does not exists
	* @param		item					The Datatype's DATA
	* @param		eName					The element name in the XSD
	* @param		fileField			The file name field under eName
	* @param		dateField			The date name field under eName
	* @param		timeField			The time name field under eName
	* @return									File is older (-1), same (0), or newer (1) than date/time in XNAT
	*/
	public static int compareFileDateTimeExist(ItemI item, String eName, String fileField, String dateField, String timeField) throws Exception, IOException
	{
		try
		{
			String fName = item.getProperty(eName + "/" + fileField).toString();
			logger.debug("checkFileDateTimeExist File: " + fName);
			String fDate = item.getProperty(eName + "/" + dateField).toString();
			String fTime = item.getProperty(eName + "/" + timeField).toString();

			File   idFile = new File(fName);

			if (!idFile.exists())
			{
				logger.debug("!!! File does not exist");
				throw new IOException(fName + " does not exist");
			}

			Date cDate = new Date(idFile.lastModified());

			if (fDate == null || fTime == null)
			{
				logger.debug("!!! Field Date or Time is null");
				throw new Exception("Field Date or Time is null");
			}
			int dateSts = QCUtils.compareDates(QCUtils.fixDate(fDate),QCUtils.fixDate(cDate));
			int timeSts = QCUtils.compareTimes(fTime,cDate);

			if (dateSts != 0)
			{
				logger.debug("!!! Date does not match: " + fDate + " != " + FORMAT_YYYYMMDD.format(cDate));
				return dateSts;
			}
			else if (timeSts !=0)
			{
				logger.debug("!!! time does not match: " + fTime + " != " + FORMAT_HHMMSS.format(cDate));
				return timeSts;
			}

			logger.debug("OK!");
			return 0;

		}
		catch (IOException io)
		{
			//pass this through...
			throw io;
		}
		catch (Exception e)
		{
			logger.error("Error: Trouble checking file times", e);
			throw e;
		}
	}


	private static HttpClientContext getContext(URI uri , RunData data)
	{
		// XDATuser
		UserI userData = (XDATUser) TurbineUtils.getUser(data);
		//UserI userData = XDAT.getUserDetails(data);
		AliasToken token = XDAT.getContextService().getBean(AliasTokenService.class).issueTokenForUser(userData);
		token.setSingleUse(true);
		String user = token.getAlias();
		String pwd = token.getSecret();
		logger.debug("getClient:userAlias:             " + user);
		logger.debug("getClient:secretAlias:           " + pwd);
		
//		CredentialsProvider credsProvider = new BasicCredentialsProvider();
//		AuthScope authscope=new AuthScope( uri.getHost() , uri.getPort());
//        credsProvider.setCredentials( 
//        		authscope ,
//        		new UsernamePasswordCredentials(user, pwd));
//     
//		// Add AuthCache to execution
//		HttpClientContext context = HttpClientContext.create();
//		context.setCredentialsProvider(credsProvider);
//		Credentials credentials = credsProvider.getCredentials(authscope);
		
		HttpHost targetHost = new HttpHost(uri.getHost(),uri.getPort(), "http");
		CredentialsProvider credsProvider = new BasicCredentialsProvider();
		credsProvider.setCredentials( 
				AuthScope.ANY ,
        		new UsernamePasswordCredentials(user, pwd));
		
		AuthCache authCache = new BasicAuthCache();
		BasicScheme basicAuth = new BasicScheme();
		authCache.put(targetHost, basicAuth);
	
		final HttpClientContext context = HttpClientContext.create();
		context.setCredentialsProvider(credsProvider);
		//AuthScheme authScheme =  (AuthScheme) context.getAttribute("preemptive-auth");
		//authCache.put(targetHost, authScheme);
		context.setAuthCache(authCache);

        return context;
	}

}

