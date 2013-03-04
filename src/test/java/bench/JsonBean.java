/**
 * Copyright (C) 2013 Barchart, Inc. <http://www.barchart.com/>
 *
 * All rights reserved. Licensed under the OSI BSD License.
 *
 * http://www.opensource.org/licenses/bsd-license.php
 */
package bench;

import hudson.plugins.git.GitSCM;

import java.util.List;

import net.sf.json.JSONObject;

import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.export.Model;
import org.kohsuke.stapler.export.ModelBuilder;
import org.kohsuke.stapler.export.Property;

public class JsonBean {

	void test() throws Exception {

		final GitSCM gitScm = new GitSCM("");

		final Model<? extends GitSCM> model = new ModelBuilder().get(gitScm
				.getClass());

		final List<Property> props = model.getProperties();

		final StaplerRequest req = null;
		final JSONObject formData = null;
		new GitSCM.DescriptorImpl().newInstance(req, formData);

	}

}
