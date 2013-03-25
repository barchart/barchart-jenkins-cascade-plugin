/**
 * Copyright (C) 2013 Barchart, Inc. <http://www.barchart.com/>
 *
 * All rights reserved. Licensed under the OSI BSD License.
 *
 * http://www.opensource.org/licenses/bsd-license.php
 */
package com.barchart.jenkins.cascade;

/**
 * Plug-in constants.
 * 
 * @author Andrei Pozolotin
 */
public interface PluginConstants {

	/* Plugin */

	String PLUGIN_NAME = "Maven Cascade Release Plugin";

	String PLUGIN_ID = "maven-release-cascade";

	String PLUGIN_URL = "/plugin/" + PLUGIN_ID;

	String PLUGIN_HELP = PLUGIN_URL + "/help";

	String PLUGIN_IMAGES = PLUGIN_URL + "/images";
	String PLUGIN_IMAGES_16 = PLUGIN_URL + "/images/16x16";
	String PLUGIN_IMAGES_24 = PLUGIN_URL + "/images/24x24";
	String PLUGIN_IMAGES_32 = PLUGIN_URL + "/images/32x32";
	String PLUGIN_IMAGES_48 = PLUGIN_URL + "/images/48x48";

	String PLUGIN_ICON = PLUGIN_IMAGES_48 + "/red-matreshka-head.png";

	/* Cascade project. */

	String CASCADE_PROJECT_PRONOUN = "Cascade";
	String CASCADE_PROJECT_NAME = "Cascade Project";
	String CASCADE_PROJECT_ICON = "dragon.png";

	/* Layout project. */

	String LAYOUT_ACTION_NAME = "Cascade Layout";
	String LAYOUT_ACTION_ICON = "monkey.png";
	String LAYOUT_ACTION_URL = "cascade-layout";

	/* Member projects. */

	String MEMBER_ACTION_NAME = "Cascade Release";
	String MEMBER_ACTION_ICON = "dragon.png";
	String MEMBER_ACTION_URL = "cascade-release";

	/* Member view. */

	String MEMBER_VIEW_NAME = "Cascade View";
	String MEMBER_VIEW_ICON = "monkey.png";
	String MEMBER_VIEW_URL = "cascade-view";

	/* Member graph. */

	String MEMBER_GRAPH_NAME = "Cascade Graph";

	/* Cascade Logger. */

	String LOGGER_PREFIX = "[CASCADE]";

}
