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
	name = "RIIPL XNAT QA Plugin Ver 0.1",
	description = "Plugin for RIIPL Pipeline-specific QA",
	dataModels = {
			@XnatDataModel(
				value= "riiplqa:Cat12Data",
				singular = "QC CAT12 Seg Report",
				plural = "QC CAT12 Seg Reports",
				code = "QA"
			)
	}
)

public class RiiplQAPlugin{	
}