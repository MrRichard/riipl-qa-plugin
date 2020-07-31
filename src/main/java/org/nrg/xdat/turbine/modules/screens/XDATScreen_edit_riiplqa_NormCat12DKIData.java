//package org.nrg.xdat.turbine.modules.screens;
/*
 * GENERATED FILE
 * Created on Mon Jul 23 13:17:02 EDT 2018
 *
 */
import org.apache.turbine.util.RunData;
import org.apache.velocity.context.Context;
import org.nrg.xdat.turbine.utils.TurbineUtils;
import org.nrg.xft.ItemI;
import org.nrg.xft.XFTItem;
import org.apache.velocity.tools.generic.DateTool;

/**
 * @author XDAT
 *
 */
public class XDATScreen_edit_riiplqa_NormCat12DKIData extends org.nrg.xnat.turbine.modules.screens.EditImageAssessorScreen {
	static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(XDATScreen_edit_riiplqa_NormCat12DKIData.class);
	/* (non-Javadoc)
	 * @see org.nrg.xdat.turbine.modules.screens.EditScreenA#getElementName()
	 */
	public String getElementName() {
	    return "riiplqa:NormCat12DKIData";
	}
	
	public ItemI getEmptyItem(RunData data) throws Exception
	{
		return super.getEmptyItem(data);
	}
	/* (non-Javadoc)
	 * @see org.nrg.xdat.turbine.modules.screens.SecureReport#finalProcessing(org.apache.turbine.util.RunData, org.apache.velocity.context.Context)
	 */
	public void finalProcessing(RunData data, Context context) {
		try{
		 		context.put("date" ,  new DateTool());
		 		System.out.println("Loaded date object");
			} catch(Exception e){}
	}}
