package org.jenkinsci.plugins.maven_artifact_choicelistprovider;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;

/**
 * 
 * Creates URL Parameters for a NEXUS and Artifactory Repository.
 *
 * @author stephan.watermeyer, Diebold Nixdorf
 */
public class RESTfulParameterBuilder {

    public static final String PARAMETER_REPOSITORYID = "repositoryId";

    public static final String PARAMETER_CLASSIFIER = "c";

    public static final String PARAMETER_PACKAGING = "p";

    public static final String PARAMETER_ARTIFACTID = "a";

    public static final String PARAMETER_GROUPID = "g";

    public static final String PACKAGING_ALL = "*";

    private static final Logger LOGGER = Logger.getLogger(RESTfulParameterBuilder.class.getName());

    /**
     * Creates the parameter list for the RESTful service.
     * 
     * @param pRepositoryId
     *            the repositoryId.
     * @param pGroupId
     *            the GroupId
     * @param pArtifactId
     *            the ArtifactId
     * @param pPackaging
     *            the Packaging
     * @param pClassifier
     *            the Classifier
     * @return the parameters to be used for the request.
     */
    public static MultivaluedMap<String, String> create(final String pRepositoryId, final String pGroupId, final String pArtifactId, final String pPackaging,
            final ValidAndInvalidClassifier pClassifier) {
        if (LOGGER.isLoggable(Level.FINE)) {
            LOGGER.fine("create parameters for: repositoryId: " + pRepositoryId + " g:" + pGroupId + ", a:" + pArtifactId + ", p:" + pPackaging + ", c: " + pClassifier.toString());
        }

        MultivaluedMap<String, String> requestParams = new MultivaluedHashMap<String, String>();
        if (pRepositoryId != "")
            requestParams.putSingle(PARAMETER_REPOSITORYID, pRepositoryId);
        if (pGroupId != "")
            requestParams.putSingle(PARAMETER_GROUPID, pGroupId);
        if (pArtifactId != "")
            requestParams.putSingle(PARAMETER_ARTIFACTID, pArtifactId);
        if (pPackaging != "" && !PACKAGING_ALL.equals(pPackaging))
            requestParams.putSingle(PARAMETER_PACKAGING, pPackaging);
        if (pClassifier != null) {
            // FIXME: There is of course a better way how to do it...
            final List<String> query = new ArrayList<String>();
            for (String current : pClassifier.getValid())
                query.add(current);

            if (!query.isEmpty())
                requestParams.put(PARAMETER_CLASSIFIER, query);
        }
        return requestParams;
    }

}
