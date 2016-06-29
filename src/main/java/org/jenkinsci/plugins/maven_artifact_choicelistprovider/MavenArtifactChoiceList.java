package org.jenkinsci.plugins.maven_artifact_choicelistprovider;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.lang.StringUtils;
import org.jenkinsci.plugins.maven_artifact_choicelistprovider.nexus.NexusLuceneSearchService;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

import hudson.Extension;
import hudson.ExtensionPoint;
import hudson.model.AbstractProject;
import hudson.model.Descriptor;
import hudson.util.FormValidation;
import jp.ikedam.jenkins.plugins.extensible_choice_parameter.ChoiceListProvider;
import jp.ikedam.jenkins.plugins.extensible_choice_parameter.ExtensibleChoiceParameterDefinition;

public class MavenArtifactChoiceList extends ChoiceListProvider implements ExtensionPoint {

	private static final Logger LOGGER = Logger.getLogger(MavenArtifactChoiceList.class.getName());

	private final String url;
	private final String groupId;
	private final String artifactId;
	private final String packaging;
	private final String classifier;
	private final boolean reverseOrder;

	@DataBoundConstructor
	public MavenArtifactChoiceList(String url, String groupId, String artifactId, String packaging, String classifier,
			boolean reverseOrder) {
		super();
		this.url = StringUtils.trim(url);
		this.groupId = StringUtils.trim(groupId);
		this.artifactId = StringUtils.trim(artifactId);
		this.packaging = StringUtils.trim(packaging);
		this.classifier = StringUtils.trim(classifier);
		this.reverseOrder = reverseOrder;
	}

	public String getUrl() {
		return url;
	}

	public String getGroupId() {
		return groupId;
	}

	public String getArtifactId() {
		return artifactId;
	}

	public String getPackaging() {
		return packaging;
	}

	public String getClassifier() {
		return classifier;
	}

	public boolean getReverseOrder() {
		return reverseOrder;
	}

	@Override
	public void onBuildTriggeredWithValue(AbstractProject<?, ?> job, ExtensibleChoiceParameterDefinition def,
			String value) {
		// XXX: Feature: Once the SelectBox only contains the "short names" of the maven artifactds (without servername)
		// Transform the short-value to the long value again.

		super.onBuildTriggeredWithValue(job, def, value);
		// LOGGER.log(Level.INFO, value);
	}

	@Override
	public List<String> getChoiceList() {
		return readURL(getUrl(), getGroupId(), getArtifactId(), getPackaging(), getClassifier(), getReverseOrder());
	}

	static List<String> readURL(final String pURL, final String pGroupId, final String pArtifactId,
			final String pPackaging, String pClassifier, final boolean pReverseOrder) {
		List<String> retVal = new ArrayList<String>();
		try {

			ValidAndInvalidClassifier classifierBox = ValidAndInvalidClassifier.fromString(pClassifier);
			IVersionReader mService = new NexusLuceneSearchService(pURL, pGroupId, pArtifactId, pPackaging,
					classifierBox);
			retVal = mService.retrieveVersions();

			if (pReverseOrder)
				Collections.reverse(retVal);
		} catch (Exception e) {
			retVal.add("ERROR: " + e.getMessage());
			LOGGER.log(Level.WARNING, "failed to retrieve versions from nexus for r:" + pURL + ", g:" + pGroupId
					+ ", a:" + pArtifactId + ", p:" + pPackaging + ", c:" + pClassifier, e);
		}
		return retVal;
	}

	@Extension
	public static class DescriptorImpl extends Descriptor<ChoiceListProvider> {
		/**
		 * the display name shown in the dropdown to select a choice provider.
		 * 
		 * @return display name
		 * @see hudson.model.Descriptor#getDisplayName()
		 */
		@Override
		public String getDisplayName() {
			return "Maven Artifact Choice Parameter";
		}

		public FormValidation doCheckUrl(@QueryParameter String url) {
			if (StringUtils.isBlank(url)) {
				return FormValidation.error("The server URL cannot be empty");
			}

			return FormValidation.ok();
		}

		public FormValidation doCheckArtifactId(@QueryParameter String artifactId) {
			if (StringUtils.isBlank(artifactId)) {
				return FormValidation.error("The artifactId cannot be empty");
			}

			return FormValidation.ok();
		}

		public FormValidation doCheckPackaging(@QueryParameter String packaging) {
			if (!StringUtils.isBlank(packaging) && packaging.startsWith(".")) {
				return FormValidation.error("packaging must not start with a .");
			}

			return FormValidation.ok();
		}

		public FormValidation doCheckClassifier(@QueryParameter String classifier) {
			if (StringUtils.isBlank(classifier)) {
				FormValidation.ok("OK, will not filter for any classifier");
			}
			return FormValidation.ok();
		}

		/**
		 * Test what files will be listed.
		 * 
		 * @param baseDirPath
		 * @param includePattern
		 * @param excludePattern
		 * @param scanType
		 * @return
		 */
		public FormValidation doTest(@QueryParameter String url, @QueryParameter String groupId,
				@QueryParameter String artifactId, @QueryParameter String packaging, @QueryParameter String classifier,
				@QueryParameter boolean reverseOrder) {
			if (StringUtils.isEmpty(packaging) && !StringUtils.isEmpty(classifier)) {
				return FormValidation.error(
						"You have choosen an empty Packaging configuration but have configured a Classifier. Please either define a Packaging value or remove the Classifier");
			}
			
			try {
				final List<String> entriesFromURL = readURL(url, groupId, artifactId, packaging, classifier,
						reverseOrder);

				if (entriesFromURL.isEmpty()) {
					return FormValidation.ok("(Working, but no Entries found)");
				}
				return FormValidation.ok(StringUtils.join(entriesFromURL, '\n'));
			} catch (Exception e) {
				return FormValidation.error("error reading versions from url:" + e.getMessage());
			}
		}
	}
}
