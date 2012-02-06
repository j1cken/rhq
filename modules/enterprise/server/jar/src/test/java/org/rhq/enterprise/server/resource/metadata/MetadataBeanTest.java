package org.rhq.enterprise.server.resource.metadata;

import static org.rhq.core.clientapi.shared.PluginDescriptorUtil.loadPluginDescriptor;

import java.io.File;
import java.net.URL;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.beanutils.MethodUtils;
import org.apache.commons.beanutils.PropertyUtils;
import org.apache.commons.lang.WordUtils;
import org.apache.maven.artifact.versioning.ComparableVersion;
import org.dbunit.database.DatabaseConfig;
import org.dbunit.database.DatabaseConnection;
import org.dbunit.database.IDatabaseConnection;
import org.dbunit.dataset.DataSetException;
import org.dbunit.dataset.IDataSet;
import org.dbunit.dataset.datatype.IDataTypeFactory;
import org.dbunit.dataset.xml.FlatXmlDataSet;
import org.dbunit.dataset.xml.FlatXmlProducer;
import org.dbunit.operation.DatabaseOperation;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeGroups;
import org.testng.annotations.BeforeMethod;
import org.xml.sax.InputSource;

import org.rhq.core.clientapi.descriptor.plugin.PluginDescriptor;
import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.criteria.ResourceTypeCriteria;
import org.rhq.core.domain.plugin.Plugin;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.util.MessageDigestGenerator;
import org.rhq.enterprise.server.auth.SubjectManagerLocal;
import org.rhq.enterprise.server.bundle.TestBundleServerPluginService;
import org.rhq.enterprise.server.resource.ResourceTypeManagerLocal;
import org.rhq.enterprise.server.scheduler.jobs.PurgePluginsJob;
import org.rhq.enterprise.server.scheduler.jobs.PurgeResourceTypesJob;
import org.rhq.enterprise.server.test.AbstractEJB3Test;
import org.rhq.enterprise.server.util.LookupUtil;

public class MetadataBeanTest extends AbstractEJB3Test {

    private List<Integer> pluginIds = new ArrayList<Integer>();

    @Override
    protected boolean isDBResetNeeded() {
        return false;
    }

    @BeforeGroups(groups = { "plugin.metadata" }, dependsOnGroups = { "integration.ejb3" })
    public void beforeGroups() throws Exception {
        setupDB();
    }

    /**
     * Need to delete rows from RHQ_PLUGINS because subsequent tests in server/jar would otherwise fail. Some tests look
     * at what plugins are in the database, and then look for corresponding plugin files on the file system. MetadataTest
     * however removes the generated plugin files during each test run.
     */
    @AfterClass(alwaysRun = true, groups = { "plugin.metadata" })
    void afterClass() throws Exception {
        PluginManagerLocal pluginMgr = LookupUtil.getPluginManager();
        Subject overlord = LookupUtil.getSubjectManager().getOverlord();
        pluginMgr.deletePlugins(overlord, pluginIds);
        pluginMgr.markPluginsForPurge(overlord, pluginIds);
        new PurgePluginsJob().executeJobCode(null);
        new PurgeResourceTypesJob().executeJobCode(null);
    }

    @BeforeMethod(groups = { "plugin.metadata" }, dependsOnGroups = { "integration.ejb3" })
    public void beforeMethod() throws Exception {
        TestBundleServerPluginService bundleService = new TestBundleServerPluginService();
        prepareCustomServerPluginService(bundleService);
        bundleService.startMasterPluginContainerWithoutSchedulingJobs();
        prepareScheduler();
    }

    /**
     * Need to delete rows from RHQ_PLUGINS because subsequent tests in server/jar would otherwise fail. Some tests look
     * at what plugins are in the database, and then look for corresponding plugin files on the file system. MetadataTest
     * however removes the generated plugin files during each test run.
     */
    @AfterMethod(alwaysRun = true, groups = { "plugin.metadata" })
    void afterMethod() throws Exception {
        unprepareServerPluginService();
        unprepareScheduler();
    }

    protected void setupDB() throws Exception {
        Connection connection = null;

        try {
            connection = getConnection();
            DatabaseConnection dbunitConnection = new DatabaseConnection(connection);
            setDbType(dbunitConnection);
            // note - this info should already be in the db as part of dbsetup, but just in case
            // perform the refresh. Do not DELETE this data set as it may be assumed in other tests.
            DatabaseOperation.REFRESH.execute(dbunitConnection, getDataSet());
        } finally {
            if (connection != null) {
                connection.close();
            }
        }
    }

    private void setDbType(IDatabaseConnection connection) throws Exception {
        DatabaseConfig config = connection.getConfig();
        String name = connection.getConnection().getMetaData().getDatabaseProductName().toLowerCase();
        int major = connection.getConnection().getMetaData().getDatabaseMajorVersion();
        IDataTypeFactory type = null;
        if (name.contains("postgres")) {
            type = new org.dbunit.ext.postgresql.PostgresqlDataTypeFactory();
        } else if (name.contains("oracle")) {
            if (major >= 10) {
                type = new org.dbunit.ext.oracle.Oracle10DataTypeFactory();
            } else {
                type = new org.dbunit.ext.oracle.OracleDataTypeFactory();
            }
        }
        if (type != null) {
            config.setProperty("http://www.dbunit.org/properties/datatypeFactory", type);
        }
    }

    private IDataSet getDataSet() throws DataSetException {
        FlatXmlProducer xmlProducer = new FlatXmlProducer(new InputSource(getClass().getResourceAsStream(
            getDataSetFile())));
        xmlProducer.setColumnSensing(true);
        return new FlatXmlDataSet(xmlProducer);
    }

    protected String getDataSetFile() {
        return "MetadataTest.xml";
    }

    protected void createPlugin(String pluginFileName, String version, String descriptorFileName) throws Exception {
        URL descriptorURL = getDescriptorURL(descriptorFileName);
        PluginDescriptor pluginDescriptor = loadPluginDescriptor(descriptorURL);
        String pluginFilePath = getCurrentWorkingDir() + "/" + pluginFileName + ".jar";
        File pluginFile = new File(pluginFilePath);

        Plugin plugin = new Plugin(pluginDescriptor.getName(), pluginFilePath);
        plugin.setDisplayName(pluginDescriptor.getName());
        plugin.setEnabled(true);
        plugin.setDescription(pluginDescriptor.getDescription());
        plugin.setAmpsVersion(getAmpsVersion(pluginDescriptor));
        plugin.setVersion(pluginDescriptor.getVersion());
        plugin.setMD5(MessageDigestGenerator.getDigestString(descriptorURL));

        SubjectManagerLocal subjectMgr = LookupUtil.getSubjectManager();
        PluginManagerLocal pluginMgr = LookupUtil.getPluginManager();

        pluginMgr.registerPlugin(subjectMgr.getOverlord(), plugin, pluginDescriptor, null, true);

        pluginIds.add(plugin.getId());
    }

    private URL getDescriptorURL(String descriptor) {
        String dir = getClass().getSimpleName();
        return getClass().getResource(dir + "/" + descriptor);
    }

    @SuppressWarnings("unused")
    private String getPluginWorkDir() throws Exception {
        return getCurrentWorkingDir() + "/work";
    }

    private String getCurrentWorkingDir() throws Exception {
        return getClass().getResource(".").toURI().getPath();
    }

    String getAmpsVersion(PluginDescriptor pluginDescriptor) {
        if (pluginDescriptor.getAmpsVersion() == null) {
            return "2.0";
        }

        ComparableVersion version = new ComparableVersion(pluginDescriptor.getAmpsVersion());
        ComparableVersion version2 = new ComparableVersion("2.0");

        if (version.compareTo(version2) <= 0) {
            return "2.0";
        }

        return pluginDescriptor.getAmpsVersion();
    }

    protected ResourceType assertResourceTypeAssociationEquals(String resourceTypeName, String plugin,
        String propertyName, List<String> expected) throws Exception {
        SubjectManagerLocal subjectMgr = LookupUtil.getSubjectManager();
        ResourceTypeManagerLocal resourceTypeMgr = LookupUtil.getResourceTypeManager();

        String fetch = "fetch" + WordUtils.capitalize(propertyName);
        ResourceTypeCriteria criteria = new ResourceTypeCriteria();
        criteria.addFilterName(resourceTypeName);
        criteria.addFilterPluginName(plugin);
        criteria.setStrict(true);
        MethodUtils.invokeMethod(criteria, fetch, true);

        List<ResourceType> resourceTypes = resourceTypeMgr.findResourceTypesByCriteria(subjectMgr.getOverlord(),
            criteria);
        ResourceType resourceType = resourceTypes.get(0);
        Set<String> expectedSet = new HashSet<String>(expected);
        List<String> missing = new ArrayList<String>();
        List<String> unexpected = new ArrayList<String>();

        for (String expectedProperty : expectedSet) {
            if (!contains(resourceType, propertyName, expectedProperty)) {
                missing.add(expectedProperty);
            }
        }

        Collection<?> actualPropertyValues = (Collection<?>) PropertyUtils.getProperty(resourceType, propertyName);
        for (Object actualPropertyValue : actualPropertyValues) {
            String actualName = (String) PropertyUtils.getProperty(actualPropertyValue, "name");
            if (!expectedSet.contains(actualName)) {
                unexpected.add(actualName);
            }
        }

        String errors = "";
        if (!missing.isEmpty()) {
            errors = "Failed to find the following " + propertyName + "(s) for type " + resourceTypeName + ": "
                + missing;
        }

        if (unexpected.size() > 0) {
            errors += "\nFound unexpected " + propertyName + "(s) for type " + resourceTypeName + ": " + unexpected;
        }

        assert errors.isEmpty() : errors;

        return resourceType;
    }

    private boolean contains(ResourceType type, String propertyName, String expected) throws Exception {
        Collection<?> actualPropertyValues = (Collection<?>) PropertyUtils.getProperty(type, propertyName);
        for (Object actualPropertyValue : actualPropertyValues) {
            String actualName = (String) PropertyUtils.getProperty(actualPropertyValue, "name");
            if (actualName.equals(expected)) {
                return true;
            }
        }
        return false;
    }
}
