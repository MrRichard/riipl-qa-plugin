/*
 * xnat-template: org.nrg.xnat.plugins.template.plugin.XnatTemplatePlugin
 * XNAT http://www.xnat.org
 * Copyright (c) 2017, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 */
package edu.wfubmc.RiiplQAPlugin;
import org.nrg.framework.annotations.XnatPlugin;
import org.nrg.framework.annotations.XnatDataModel;

/**
 * @author rbarcus
 */
@XnatPlugin(
	value = "riipl-qa-plugin",
	name = "RIIPL XNAT QA Plugin Ver 0.0.5",
	description = "Now featuring PETPVC",
	dataModels = {
			@XnatDataModel(
				value= "riiplqa:Cat12Data",
				singular = "QC CAT12 Seg Report",
				plural = "QC CAT12 Seg Reports",
				code = "QA"
			),
			@XnatDataModel(
					value= "riiplqa:NormCat12PCASL8Data",
					singular = "QC PCASL8 CAT12 Seg Report",
					plural = "QC PCASL8 CAT12 Seg Reports",
					code = "QA"
				),
			@XnatDataModel(
					value= "riiplqa:NormCat12PCASLbhData",
					singular = "QC PCASL BH CAT12 Seg Report",
					plural = "QC PCASL BH CAT12 Seg Reports",
					code = "QA"
				),
			@XnatDataModel(
					value= "riiplqa:NormCat12DTIData",
					singular = "QC DTI CAT12 Seg Report",
					plural = "QC DTI CAT12 Seg Reports",
					code = "QA"
				),
			@XnatDataModel(
					value= "riiplqa:NormCat12NODDIData",
					singular = "QC NODDI CAT12 Seg Report",
					plural = "QC NODDI CAT12 Seg Reports",
					code = "QA"
				),
	}
)

public class RiiplQAPlugin{	
}