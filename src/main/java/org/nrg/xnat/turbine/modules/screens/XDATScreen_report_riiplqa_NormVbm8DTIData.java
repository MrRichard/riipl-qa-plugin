/*
 * GENERATED FILE
 * Created on Fri Aug 05 14:16:34 EDT 2011
 *
 */
//package org.nrg.xdat.turbine.modules.screens;
package org.nrg.xnat.turbine.modules.screens;
import org.apache.turbine.util.RunData;
import org.apache.velocity.context.Context;
import org.nrg.xdat.turbine.modules.screens.SecureReport;

import edu.wfubmc.RiiplQAPlugin.RiiplVbm8Utils;

import java.util.Arrays;

/**
 * @author XDAT
 *
 */
public class XDATScreen_report_riiplqa_NormVbm8DTIData extends SecureReport
{
	public static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(XDATScreen_report_riiplqa_NormVbm8DTIData.class);
	/* (non-Javadoc)
	 * @see org.nrg.xdat.turbine.modules.screens.SecureReport#finalProcessing(org.apache.turbine.util.RunData, org.apache.velocity.context.Context)
	 */

	public void finalProcessing(RunData data, Context context)
	{
		org.nrg.xdat.om.RiiplqaNormvbm8dtidata om = new org.nrg.xdat.om.RiiplqaNormvbm8dtidata(item);
		org.nrg.xdat.om.XnatMrsessiondata mr = om.getMrSessionData();
		context.put("om",om);
		context.put("mr",mr);
		context.put("subject",mr.getSubjectData());
		try
		{
			RiiplVbm8Utils.checkFiles(data, item, "riiplqa:NormVbm8DTIData");
		}
		catch (Exception e)
		{
			logger.error("Warning: Trouble checking file times: " + e);
			data.setMessage("Error: Trouble checking file times: " + e + "<br>" + Arrays.toString(e.getStackTrace()));
		}
	}
}
