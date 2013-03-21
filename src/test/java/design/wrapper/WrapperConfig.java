package design.wrapper;

import hudson.maven.MavenModuleSet;
import hudson.model.Descriptor;
import hudson.tasks.BuildWrapper;
import hudson.util.DescribableList;

import org.jvnet.hudson.plugins.m2release.M2ReleaseBuildWrapper;

import com.barchart.jenkins.cascade.PluginUtilities;
import com.barchart.jenkins.cascade.CascadeOptions;
import com.barchart.jenkins.cascade.LayoutBuildWrapper;
import com.barchart.jenkins.cascade.LayoutOptions;

public class WrapperConfig {

	/**
	 * Perform additional configuration steps.
	 */
	public static void performConfig(final String projectName,
			final LayoutOptions options, final CascadeOptions op) {
	
		SYNC: if (options.getSyncReleasePlugins()) {
			try {
	
				LayoutBuildWrapper.log.info("### projectName=" + projectName);
	
				if (projectName == null) {
					LayoutBuildWrapper.log.info("### Missing project name.");
					break SYNC;
				}
	
				final MavenModuleSet layoutProject = PluginUtilities
						.mavenProject(projectName);
	
				if (layoutProject == null) {
					LayoutBuildWrapper.log.info("### Missing layout project: " + projectName);
					break SYNC;
				}
	
				final DescribableList<BuildWrapper, Descriptor<BuildWrapper>> wrapperList = layoutProject
						.getBuildWrappersList();
	
				final M2ReleaseBuildWrapper m2releaseWrapper = wrapperList
						.get(M2ReleaseBuildWrapper.class);
	
				if (m2releaseWrapper == null) {
					LayoutBuildWrapper.log.info("### Missing m2release wrapper: " + projectName);
					break SYNC;
				}
	
				final String releaseGoals = op.getMavenReleaseGoals();
	
				final String pretendGoals = releaseGoals
						+ " --define dryRun=true ";
	
				PluginUtilities.changeField(m2releaseWrapper, "releaseGoals",
						releaseGoals);
	
				PluginUtilities.changeField(m2releaseWrapper, "dryRunGoals",
						pretendGoals);
	
				wrapperList.removeAll(M2ReleaseBuildWrapper.class);
	
				wrapperList.add(m2releaseWrapper);
	
				layoutProject.save();
	
				LayoutBuildWrapper.log.info("### Updated m2release wrapper: " + projectName);
	
			} catch (final Exception e) {
				LayoutBuildWrapper.log.severe("Failed to sync release plugins.");
				e.printStackTrace();
			}
	
		}
	
	}

}
