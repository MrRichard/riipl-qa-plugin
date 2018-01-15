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
				value= "riiplqa:SegVbm8Data",
				singular = "QC VBM8 Seg Report",
				plural = "QC VBM8 Seg Reports",
				code = "QA"
			),
			@XnatDataModel(
				value= "riiplqa:NormVbm8PASLData",
				singular = "QC VBM8 PASL Report",
				plural = "QC VBM8 PASL Reports",
				code = "QA"
			),
			@XnatDataModel(
				value= "riiplqa:NormVbm8PCASLData",
				singular = "QC VBM8 PCASL Report",
				plural = "QC VBM8 PCASL Reports",
				code = "QA"
			),
			@XnatDataModel(
				value= "riiplqa:NormVbm8vPCASLData",
				singular = "QC VBM8 vPCASL Report",
				plural = "QC VBM8 vPCASL Reports",
				code = "QA"
			),
			@XnatDataModel(
					value= "riiplqa:NormVbm8PCASL2Data",
					singular = "QC VBM8 PCASL2 Report",
					plural = "QC VBM8 PCASL2 Reports",
					code = "QA"
			),
			@XnatDataModel(
					value= "riiplqa:NormVbm8PCASL8Data",
					singular = "QC VBM8 PCASL8 Report",
					plural = "QC VBM8 PCASL8 Reports",
					code = "QA"
			),
			@XnatDataModel(
					value= "riiplqa:NormVbm8PCASLbhData",
					singular = "QC VBM8 PCASL BH Report",
					plural = "QC VBM8 PCASL BH Reports",
					code = "QA"
			),
			@XnatDataModel(
					value= "riiplqa:NormVbm8DTIData",
					singular = "QC VBM8 DTI Data Report",
					plural = "QC VBM8 DTI Reports",
					code = "QA"
			),
			@XnatDataModel(
					value= "riiplqa:NormVbm8DKIData",
					singular = "QC VBM8 DKI Report",
					plural = "QC VBM8 DKI Reports",
					code = "QA"
			),
			@XnatDataModel(
					value= "riiplqa:NormVbm8DKEData",
					singular = "QC VBM8 DKE Report",
					plural = "QC VBM8 DKE Reports",
					code = "QA"
			),
			@XnatDataModel(
					value= "riiplqa:NormVbm8MTRData",
					singular = "QC VBM8 MTR Report",
					plural = "QC VBM8 MTR Reports",
					code = "QA"
			),
			@XnatDataModel(
					value= "riiplqa:NormVbm8LST8Data",
					singular = "QC VBM8 LST Report",
					plural = "QC VBM8 LST Reports",
					code = "QA"
			),
			@XnatDataModel(
					value= "riiplqa:vbm8_covData",
					singular = "QC VBM8 Covariance Report",
					plural = "QC VBM8 Covariance Reports",
					code = "QA"
			),
	}
)

public class RiiplQAPlugin{	
}