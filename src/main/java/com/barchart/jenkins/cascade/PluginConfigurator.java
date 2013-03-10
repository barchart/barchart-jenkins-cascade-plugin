/**
 * Copyright (C) 2013 Barchart, Inc. <http://www.barchart.com/>
 *
 * All rights reserved. Licensed under the OSI BSD License.
 *
 * http://www.opensource.org/licenses/bsd-license.php
 */
package com.barchart.jenkins.cascade;

import static com.barchart.jenkins.cascade.PluginConstants.*;
import hudson.Extension;
import hudson.model.ManagementLink;
import net.sf.json.JSONObject;

import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

/**
 * Creates the link on the "Manage Jenkins" page.
 * 
 * @author Andrei Pozolotin
 */
@Extension
public class PluginConfigurator extends ManagementLink {

	/**
	 * Jelly form submit.
	 */
	public void doSubmit(//
			final StaplerRequest request, //
			final StaplerResponse response //
	) throws Exception {

		final JSONObject pluginForm = request.getSubmittedForm();

		final JSONObject layoutOptions = pluginForm
				.getJSONObject(LayoutOptions.NAME);
		LayoutOptions.META.configure(request, layoutOptions);

		final JSONObject cascadeOptions = pluginForm
				.getJSONObject(CascadeOptions.NAME);
		CascadeOptions.META.configure(request, cascadeOptions);

		response.sendRedirect("/manage");

	}

	/**
	 * Global cascade options.
	 */
	public CascadeOptions getCascadeOptions() {
		return CascadeOptions.META.global();
	}

	@Override
	public String getDescription() {
		return "Configure global settings and defaults of the cascade projects.";
	}

	public String getDisplayName() {
		return PLUGIN_NAME;
	}

	@Override
	public String getIconFileName() {
		return MEMBER_ACTION_ICON;
	}

	/**
	 * Global layout options.
	 */
	public LayoutOptions getLayoutOptions() {
		return LayoutOptions.META.global();
	}

	@Override
	public String getUrlName() {
		return PLUGIN_ID;
	}

}
