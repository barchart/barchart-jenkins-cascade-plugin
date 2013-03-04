/**
 * Copyright (C) 2013 Barchart, Inc. <http://www.barchart.com/>
 *
 * All rights reserved. Licensed under the OSI BSD License.
 *
 * http://www.opensource.org/licenses/bsd-license.php
 */
package design;

import hudson.model.Action;
import hudson.model.AbstractProject;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import jenkins.util.ProgressiveRendering;
import net.sf.json.JSON;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import org.kohsuke.stapler.bind.JavaScriptMethod;

/**
 * Shows the connected component of the project
 */
public class ProjectAction implements Action {

	final private AbstractProject<?, ?> project;

	public ProjectAction(final AbstractProject<?, ?> project) {
		this.project = project;
	}

	public String getIconFileName() {
		return "plugin.png";
	}

	public String getDisplayName() {
		return "Project Action";
	}

	public String getUrlName() {
		return "barchart-cascade-action";
	}

	public List<String> getList() {
		final List<String> list = new ArrayList<String>();
		list.add("1");
		list.add("2");
		list.add("3");
		return list;
	}

	@JavaScriptMethod
	public String getValue(final String index) throws Exception {
		Thread.sleep(3 * 1000);
		return "-" + index;
	}

	public ProgressiveRendering factor(final String numberS) {

		return new ProgressiveRendering() {

			final List<Integer> newFactors = new LinkedList<Integer>();

			@Override
			protected void compute() throws Exception {

				final int number = Integer.parseInt(numberS);

				// try entering a
				// nonnumeric value!
				// Deliberately inefficient:

				for (int i = 1; i <= number; i++) {
					if (canceled()) {
						return;
					}
					if (i % 1000000 == 0) {
						Thread.sleep(10); // take a breather
					}
					if (number % i == 0) {
						synchronized (this) {
							newFactors.add(i);
						}
					}
					progress(((double) i) / number);
				}
			}

			@Override
			protected synchronized JSON data() {
				final JSONArray r = new JSONArray();
				for (final int i : newFactors) {
					r.add(i);
				}
				newFactors.clear();
				return new JSONObject().accumulate("newfactors", r);
			}
		};

	}

}
