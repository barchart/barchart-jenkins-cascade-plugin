package com.barchart.jenkins.cascade;

import hudson.model.Action;

/**
 * Empty base action.
 */
public abstract class AdapterAction implements Action {

	public String getIconFileName() {
		/** Show no icon. */
		return null;
	}

	public String getDisplayName() {
		/** Show no name. */
		return null;
	}

	public String getUrlName() {
		/** Show no link. */
		return null;
	}

}
