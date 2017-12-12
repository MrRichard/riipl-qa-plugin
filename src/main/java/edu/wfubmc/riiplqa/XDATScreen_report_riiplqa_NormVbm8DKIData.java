/*
 * GENERATED FILE
 * Created on Fri Aug 05 14:16:34 EDT 2011
 *
 */
//package org.nrg.xdat.turbine.modules.screens;
package edu.wfubmc.riiplqa;
import org.apache.turbine.util.RunData;
import org.apache.velocity.context.Context;
import org.nrg.xdat.turbine.modules.screens.SecureReport;

import edu.wfubmc.riiplqa.RiiplVbm8Utils;

import java.util.Arrays;

/**
 * @author XDAT
 *
 */
public class XDATScreen_report_riiplqa_NormVbm8DKIData extends SecureReport
{
	public static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(XDATScreen_report_riiplqa_NormVbm8DKIData.class);
	/* (non-Javadoc)
	 * @see org.nrg.xdat.turbine.modules.screens.SecureReport#finalProcessing(org.apache.turbine.util.RunData, org.apache.velocity.context.Context)
	 */

	public void finalProcessing(RunData data, Context context)
	{
		org.nrg.xdat.om.RiiplNormvbm8dkidata om = new org.nrg.xdat.om.RiiplNormvbm8dkidata(item);
		org.nrg.xdat.om.XnatMrsessiondata mr = om.getMrSessionData();
		context.put("om",om);
		context.put("mr",mr);
		context.put("subject",mr.getSubjectData());
		try
		{
			RiiplVbm8Utils.checkFiles(data, item, "riiplqa:NormVbm8DKIData");
		}
		catch (Exception e)
		{
			logger.error("Warning: Trouble checking file times: " + e);
			data.setMessage("Error: Trouble checking file times: " + e + "<br>" + Arrays.toString(e.getStackTrace()));
		}
	}
}
