/*
 * xnat-template: org.nrg.xnat.plugins.template.plugin.XnatTemplatePlugin
 * XNAT http://www.xnat.org
 * Copyright (c) 2017, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 */

package org.nrg.xnat.plugins.RiiplQAPlugin.plugin;
import org.nrg.framework.annotations.XnatPlugin;
import org.nrg.framework.annotations.XnatDataModel;

/**
 * @author rbarcus
 */
@XnatPlugin(
	value = "riipl-qa-plugin",
	name = "RIIPL XNAT QA Plugin Ver 0.1",
	description = "Plugin for RIIPL Pipeline-specific QA",
	dataModels = {
			@XnatDataModel(
				value= "riiplqa:SegVbm8Data",
				singular = "QC VBM8 Seg Report",
				plural = "QC VBM8 Seg Reports",
				code = "QA"
			),
			@XnatDataModel(
				value= "riiplqa:NormVbm8PASLData",
				singular = "QC PASL Report",
				plural = "QC PASL Reports",
				code = "QA"
			),
			@XnatDataModel(
				value= "riiplqa:NormVbm8PCASLData",
				singular = "QC PCASL Report",
				plural = "QC PCASL Reports",
				code = "QA"
			),
			@XnatDataModel(
				value= "riiplqa:NormVbm8vPCASLData",
				singular = "QC vPCASL Report",
				plural = "QC vPCASL Reports",
				code = "QA"
			)
	}
)

public class RiiplQAPlugin{	
}