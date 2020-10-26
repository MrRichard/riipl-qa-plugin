package org.nrg.xnat.turbine.modules.screens;
import org.apache.turbine.util.RunData;
import org.apache.velocity.context.Context;
import org.nrg.xdat.turbine.utils.TurbineUtils;
import org.nrg.xft.ItemI;
import org.nrg.xft.XFTItem;
import org.apache.velocity.tools.generic.DateTool;



public class XDATScreen_edit_riiplqa_SegCat12Data extends org.nrg.xnat.turbine.modules.screens.EditImageAssessorScreen {
	static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(XDATScreen_edit_riiplqa_SegCat12Data.class);

	public String getElementName() {
	    return "riiplqa:SegCat12Data";
	}
	
	public ItemI getEmptyItem(RunData data) throws Exception
	{
		return super.getEmptyItem(data);
	}

	public void finalProcessing(RunData data, Context context) {
		try{
	 		context.put("date" ,  new DateTool());
	 		System.out.println("Loaded date object");
		} catch(Exception e){
			System.out.println("did not load date object");
		}
	}}
