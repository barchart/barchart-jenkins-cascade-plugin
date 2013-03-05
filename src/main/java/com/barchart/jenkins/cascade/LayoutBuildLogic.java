package com.barchart.jenkins.cascade;

import static com.barchart.jenkins.cascade.PluginUtilities.*;
import hudson.maven.ModuleName;
import hudson.maven.MavenModule;
import hudson.maven.MavenModuleSet;
import hudson.model.BuildListener;
import hudson.model.TopLevelItem;
import hudson.model.AbstractBuild;
import hudson.model.Descriptor;
import hudson.plugins.git.GitSCM;
import hudson.scm.SCM;
import hudson.scm.SubversionSCM;
import hudson.tasks.BuildWrapper;
import hudson.util.DescribableList;

import java.io.IOException;
import java.util.Collection;

import jenkins.model.Jenkins;

import org.joda.time.DateTime;

import com.barchart.jenkins.cascade.PluginUtilities.JenkinsTask;

/**
 * Layout build logic.
 */
public class LayoutBuildLogic {

	/**
	 * Provide cascade project name.
	 */
	public static String cascadeName(final MavenModuleSet layoutProject) {

		final LayoutBuildWrapper wrapper = layoutProject.getBuildWrappersList()
				.get(LayoutBuildWrapper.class);

		final String cascadePattern = wrapper.getCascadePattern();

		return layoutProject.getName() + "_CASCADE";
	}

	/** Provide member project name. */
	public static String memberName(final MavenModuleSet layoutProject,
			final MavenModule module) {

		final LayoutBuildWrapper wrapper = layoutProject.getBuildWrappersList()
				.get(LayoutBuildWrapper.class);

		final String memberPattern = wrapper.getMemberPattern();

		return module.getModuleName().artifactId;

	}

	/**
	 * Process layout build action.
	 */
	public static boolean process(//
			final PluginLogger log, //
			final AbstractBuild<?, ?> build, //
			final BuildListener listener //
	) throws IOException {

		final Jenkins jenkins = Jenkins.getInstance();

		final MavenModuleSet layoutProject = mavenProject(build);

		final LayoutArgumentsAction action = build
				.getAction(LayoutArgumentsAction.class);

		processCascade(log, layoutProject, action);

		final Collection<MavenModule> moduleList = layoutProject.getModules();

		for (final MavenModule module : moduleList) {

			final ModuleName moduleName = module.getModuleName();

			/**
			 * Module-to-Project naming convention.
			 * <p>
			 * TODO expose in UI.
			 */
			final String memberName = memberName(layoutProject, module);

			log.text("---");
			log.text("Module name: " + moduleName);
			log.text("Project name: " + memberName);

			if (isSameModuleName(layoutProject.getRootModule(), module)) {
				log.text("This is a root module project, managed by user, skip.");
				continue;
			}

			final JenkinsTask projectCreate = new JenkinsTask() {
				public void run() throws IOException {
					if (isProjectExists(memberName)) {
						log.text("Project exists, create skipped: "
								+ memberName);
					} else {
						log.text("Creating project: " + memberName);

						/** Clone project via XML. */
						final TopLevelItem item = jenkins.copy(
								(TopLevelItem) layoutProject, memberName);

						final MavenModuleSet memberProject = (MavenModuleSet) item;

						processMember(log, module, memberProject, layoutProject);

						log.text("Project created: " + memberName);
					}
				}
			};

			final JenkinsTask projectDelete = new JenkinsTask() {
				public void run() throws IOException {
					if (!isProjectExists(memberName)) {
						log.text("Project not present, delete skipped: "
								+ memberName);
					} else {
						final TopLevelItem item = jenkins.getItem(memberName);
						log.text("Deleting project : " + memberName);
						try {
							item.delete();
						} catch (final InterruptedException e) {
							e.printStackTrace();
						}
						log.text("Project deleted: " + memberName);
					}
				}
			};

			switch (action.getConfigAction()) {
			default:
				log.text("Unexpected config action, ignore: "
						+ action.getConfigAction());
				break;
			case CREATE:
				projectCreate.run();
				break;
			case DELETE:
				projectDelete.run();
				break;
			case UPDATE:
				projectDelete.run();
				projectCreate.run();
				break;
			}

		}

		return true;
	}

	/** Handle cascade project. */
	public static void processCascade(final PluginLogger log,
			final MavenModuleSet layoutProject,
			final LayoutArgumentsAction action) throws IOException {

		final Jenkins jenkins = Jenkins.getInstance();

		final String layoutName = layoutProject.getName();
		final String cascadeName = cascadeName(layoutProject);

		log.text("---");
		log.text("Root project: " + layoutName);
		log.text("Cascade project: " + cascadeName);

		final MemberProjectProperty layoutProperty = new MemberProjectProperty(
				ProjectRole.LAYOUT.code(), cascadeName, layoutName);

		final JenkinsTask projectCreate = new JenkinsTask() {
			public void run() throws IOException {
				if (isProjectExists(cascadeName)) {
					log.text("Cascade project exist, skip create.");
				} else {
					log.text("Creating cascade project.");
					final CascadeProject cascadeProject = jenkins
							.createProject(CascadeProject.class, cascadeName);
					ensureProperty(layoutProject, layoutProperty);
					/** Provide description */
					{
						final StringBuilder text = new StringBuilder();
						text.append("Generated on:");
						text.append("<br>\n");
						text.append("<b>");
						text.append(new DateTime());
						text.append("</b>");
						text.append("<p>\n");
						text.append("Cascade layout project:");
						text.append("<br>\n");
						text.append("<b>");
						text.append(layoutProject.getName());
						text.append("</b>");
						text.append("<p>\n");
						cascadeProject.setDescription(text.toString());
					}
					/** Persist project. */
					{
						cascadeProject.save();
					}
					log.text("Cascade project created.");
				}
			}
		};
		final JenkinsTask projectDelete = new JenkinsTask() {
			public void run() throws IOException {
				if (!isProjectExists(cascadeName)) {
					log.text("Cascade project missing, skip delete.");
				} else {
					log.text("Deleting cascade project.");
					final TopLevelItem cascadeProject = jenkins
							.getItem(cascadeName);
					try {
						cascadeProject.delete();
					} catch (final InterruptedException e) {
						e.printStackTrace();
					}
					log.text("Cascade project deleted.");
				}
			}
		};

		switch (action.getConfigAction()) {
		default:
			log.text("Unexpected config action, ignore: "
					+ action.getConfigAction());
			break;
		case CREATE:
			projectCreate.run();
			break;
		case DELETE:
			projectDelete.run();
			break;
		case UPDATE:
			projectDelete.run();
			projectCreate.run();
			break;
		}

	}

	/**
	 * Update details of created member project.
	 */
	public static void processMember(final PluginLogger log,
			final MavenModule module, final MavenModuleSet memberProject,
			final MavenModuleSet layoutProject) throws IOException {

		/** Update SCM paths. */
		SCM: {

			final SCM scm = memberProject.getScm();

			if (scm instanceof GitSCM) {

				final GitSCM gitScm = (GitSCM) scm;

				final String includedRegions = module.getRelativePath() + "/.*";

				changeField(gitScm, "includedRegions", includedRegions);

				break SCM;

			}

			if (scm instanceof SubversionSCM) {

				final SubversionSCM svnScm = (SubversionSCM) scm;

				/** TODO */

			}

			log.text("###################################");
			log.text("WARNING: YOU ARE USING UNTESTED SCM");
			log.text("###################################");
		}

		/** Update Maven paths. */
		{
			final String rootPOM = module.getRelativePath() + "/pom.xml";

			memberProject.setRootPOM(rootPOM);

		}

		/** Disable cascade layout action. */
		{
			final DescribableList<BuildWrapper, Descriptor<BuildWrapper>> buildWrapperList = memberProject
					.getBuildWrappersList();

			buildWrapperList.remove(LayoutBuildWrapper.class);
		}

		/** Enable cascade release action. */
		{

			final String cascadeName = cascadeName(layoutProject);
			final String layoutName = layoutProject.getName();

			final MemberProjectProperty memberProperty = new MemberProjectProperty(
					ProjectRole.MEMBER.code(), cascadeName, layoutName);

			ensureProperty(memberProject, memberProperty);

		}

		/** Provide description. */
		{
			final StringBuilder text = new StringBuilder();
			text.append("Generated on:");
			text.append("<br>\n");
			text.append("<b>");
			text.append(new DateTime());
			text.append("</b>");
			text.append("<p>\n");
			text.append("Cascade layout project:");
			text.append("<br>\n");
			text.append("<b>");
			text.append(layoutProject.getName());
			text.append("</b>");
			text.append("<p>\n");
			text.append("Cascade Member project:");
			text.append("<br>\n");
			text.append("<b>");
			text.append(memberProject.getName());
			text.append("</b>");
			text.append("<p>\n");
			memberProject.setDescription(text.toString());
		}

		/** Persist changes. */
		{
			memberProject.save();
		}

	}

	private LayoutBuildLogic() {

	}

}
