package org.nrg.xnat.turbine.modules.screens;
import org.apache.turbine.util.RunData;
import org.apache.velocity.context.Context;
import org.nrg.xdat.turbine.modules.screens.SecureReport;

import edu.wfubmc.RiiplQAPlugin.RiiplCat12Utils;


/**
 * @author XDAT
 *
 */
public class XDATScreen_report_riiplqa_SegCat12Data extends SecureReport
{
	public static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(XDATScreen_report_riiplqa_SegCat12Data.class);
	/* (non-Javadoc)
	 * @see org.nrg.xdat.turbine.modules.screens.SecureReport#finalProcessing(org.apache.turbine.util.RunData, org.apache.velocity.context.Context)
	 */

	public void finalProcessing(RunData data, Context context)
	{
		try
		{
			org.nrg.xdat.om.RiiplqaSegcat12data om = new org.nrg.xdat.om.RiiplqaSegcat12data(item);
			org.nrg.xdat.om.XnatMrsessiondata mr = om.getMrSessionData();
			context.put("om",om);
			System.out.println("Loaded om object (org.nrg.xdat.om.RiiplSegcat12data) as context parameter 'om'.");
			context.put("mr",mr);
			System.out.println("Loaded mr session object (org.nrg.xdat.om.XnatMrsessiondata) as context parameter 'mr'.");
			context.put("subject",mr.getSubjectData());
			System.out.println("Loaded subject object (org.nrg.xdat.om.XnatSubjectdata) as context parameter 'subject'.");
		}
		catch(Exception e){}
	}
}
