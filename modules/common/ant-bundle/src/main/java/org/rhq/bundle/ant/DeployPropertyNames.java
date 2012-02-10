package org.rhq.bundle.ant;

/**
 * @author Ian Springer
 */
public class DeployPropertyNames {
    /** System property that should always be available to Ant scripts - it's the location where the deployment should be installed */
    public static final String DEPLOY_DIR = "rhq.deploy.dir";

    /** System property that should always be available to Ant scripts - it's the ID of the bundle deployment */
    public static final String DEPLOY_ID = "rhq.deploy.id";

    /** System property that should always be available to Ant scripts - it's the name of the bundle deployment */
    public static final String DEPLOY_NAME = "rhq.deploy.name";

    /** System property that should always be available to Ant scripts - it's the bundle deployment phase being executed */
    public static final String DEPLOY_PHASE = "rhq.deploy.phase";

    /**
     * Optional deploy property - if true, this is a revert of an installed bundle to its state prior to the last
     * deployment; if not specified, the default is false
     */
    public static final String DEPLOY_REVERT = "rhq.deploy.revert";

    /**
     * Optional deploy property - if true, this is a fresh install (that is, the deploy directory should be wiped clean
     * prior to deployment of the specified version; if not specified, the default is false
     */
    public static final String DEPLOY_CLEAN = "rhq.deploy.clean";

    /**
     * Optional deploy property - if true, this is a dry run or preview (that is, it should log what steps would occur
     * but not actually perform any of the steps); if not specified, the default is false
     */
    public static final String DEPLOY_DRY_RUN = "rhq.deploy.dryRun";

   /**
    * Prefix which is appended to Ant properties.
    * Tags of a resource are available as Ant properties during content bundle deployment. A "rhq
    * .tag:[<namespace>:]<semantic>" will be substituted by "<name>".
    */
   public static final String DEPLOY_TAG_PREFIX = "rhq.tag.";

    private DeployPropertyNames() {
    }
}
