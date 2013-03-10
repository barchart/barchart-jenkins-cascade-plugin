/**
 * Copyright (C) 2013 Barchart, Inc. <http://www.barchart.com/>
 *
 * All rights reserved. Licensed under the OSI BSD License.
 *
 * http://www.opensource.org/licenses/bsd-license.php
 */
package com.barchart.jenkins.cascade;

import hudson.Extension;
import hudson.maven.MavenModuleSet;
import hudson.model.Item;
import hudson.model.listeners.ItemListener;

import java.util.UUID;
import java.util.logging.Logger;

/**
 * Manage global project life cycle events.
 * 
 * @author Andrei Pozolotin
 */
@Extension
public class PluginProjectListener extends ItemListener {

	private final static Logger log = Logger
			.getLogger(PluginProjectListener.class.getName());

	@Override
	public void onCreated(final Item item) {
	}

	@Override
	public void onCopied(final Item source, final Item target) {
	}

	/**
	 * Generate unique identity for layout project.
	 */
	@Override
	public void onUpdated(final Item item) {

		try {

			if (!(item instanceof MavenModuleSet)) {
				return;
			}

			final MavenModuleSet project = (MavenModuleSet) item;

			log.info("project: " + project);

			/** Not a layout project. */
			if (!LayoutBuildWrapper.hasWrapper(project)) {
				return;
			}

			/** Layout already configured. */
			if (MemberProjectProperty.hasProperty(project)) {
				return;
			}

			final String layoutCode = ProjectRole.LAYOUT.code();
			final String cascadeUUID = UUID.randomUUID().toString();
			final String projectUUID = UUID.randomUUID().toString();

			final MemberProjectProperty property = new MemberProjectProperty(
					layoutCode, cascadeUUID, projectUUID);

			PluginUtilities.ensureProperty(project, property);

			project.save();

			log.info("Property added:" + project.getName() + " = " + property);

		} catch (final Exception e) {
			log.severe("Failed to add member property to project.");
		}

	}

}
