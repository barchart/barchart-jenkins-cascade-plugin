/**
 * Copyright (C) 2013 Barchart, Inc. <http://www.barchart.com/>
 *
 * All rights reserved. Licensed under the OSI BSD License.
 *
 * http://www.opensource.org/licenses/bsd-license.php
 */
package com.barchart.jenkins.cascade;

import hudson.Extension;
import hudson.model.Item;
import hudson.model.AbstractProject;
import hudson.model.listeners.ItemListener;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Manage global project life cycle events.
 * 
 * @author Andrei Pozolotin
 */
@Extension
public class ProjectListener extends ItemListener {

	private final static Logger log = Logger.getLogger(ProjectListener.class
			.getName());

	/**
	 * Provide new identity for layout projects.
	 * <p>
	 * Eraze identity for cascade and member projects.
	 */
	@Override
	public void onCopied(final Item source, final Item target) {
		try {

			/** Interested in cascade projects only. */
			if (!(source instanceof AbstractProject)) {
				return;
			}

			final AbstractProject<?, ?> sourceProject = (AbstractProject<?, ?>) source;
			final AbstractProject<?, ?> targetProject = (AbstractProject<?, ?>) target;

			final ProjectIdentity identity = ProjectIdentity
					.identity(targetProject);

			/** Interested in cascade projects only. */
			if (identity == null) {
				return;
			}

			/** Erase identity of target project. */
			targetProject.removeProperty(ProjectIdentity.class);

			switch (identity.role()) {
			case LAYOUT:
				/** Provide new identity. */
				ProjectIdentity.ensureLayoutIdentity(targetProject);
				return;
			case CASCADE:
			case MEMBER:
				/** No identity, update description. */
				final String sourceDescription = sourceProject.getDescription();
				final String targetDescription = "<p>ORPHAN PROJECT<p>";
				final String description = sourceDescription
						+ targetDescription;
				targetProject.setDescription(description);
				return;
			default:
				log.severe("Unkown project role=" + identity.role());
				return;
			}

		} catch (final Throwable e) {
			log.log(Level.SEVERE, "Copy listener failure.", e);
		}
	}

}
