package edu.wfubmc.riiplqa;

//XNAT imports
import org.apache.turbine.util.RunData;
import org.apache.velocity.context.Context;
import org.nrg.xdat.turbine.utils.TurbineUtils;
import org.nrg.xft.ItemI;
import org.nrg.xft.XFTItem;

import edu.wfubmc.riiplqa.RiiplDebug;
import edu.wfubmc.riiplqa.RiiplVbm8Utils;

import java.util.ArrayList;
import org.apache.commons.io.filefilter.WildcardFileFilter;


/** 
	Searches in the nifti/vbm8/  for a p*_seg8.txt.
	Parses the txt file and creates a jpg image for display and stores values from txt.
 * @author Ben Wagner
 * @author ANSIR Lab
 * @version 0.1 October 2011
 */
public class Upload_riiplqa_NormVbm8LST8Data extends org.nrg.xnat.turbine.modules.screens.EditImageAssessorScreen {
	static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(Upload_riiplqa_NormVbm8LST8Data.class);
	/* (non-Javadoc)
	 * @see org.nrg.xdat.turbine.modules.screens.EditScreenA#getElementName()
	 */

	public String getElementName()
	{
		return "riiplqa:NormVbm8LST8Data";
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
		logger.debug("Upload_riiplqa_NormVbm8LST8Data::finalProcessing");
		org.nrg.xdat.om.RiiplNormvbm8lst8data om = new org.nrg.xdat.om.RiiplNormvbm8lst8data(item);
		org.nrg.xdat.om.XnatMrsessiondata mr = om.getMrSessionData();
		
		ArrayList<WildcardFileFilter> imageFilters=new ArrayList<WildcardFileFilter>();
// ImageFilters will be added as the CSV is read from tlv_all.csv
//		imageFilters.add(new WildcardFileFilter(new String[] {"wr*P?????_CBF.nii*","wr*._CBF.nii*"}));
		
		RiiplVbm8Utils.normalizationQC(mr, data, item, this.getElementName(),"NormVbm8LST8", "tlv_all.csv", imageFilters,false);
	}
}
