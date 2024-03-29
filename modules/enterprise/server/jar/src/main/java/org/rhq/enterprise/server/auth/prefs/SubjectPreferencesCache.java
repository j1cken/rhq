package org.rhq.enterprise.server.auth.prefs;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.Property;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.enterprise.server.auth.SubjectManagerLocal;
import org.rhq.enterprise.server.common.EntityManagerFacadeLocal;
import org.rhq.enterprise.server.configuration.ConfigurationManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;

public class SubjectPreferencesCache {

    protected final Log log = LogFactory.getLog(SubjectPreferencesCache.class);

    private Map<Integer, Configuration> subjectPreferences;

    private static final SubjectPreferencesCache instance = new SubjectPreferencesCache();

    private SubjectManagerLocal subjectManager;
    private EntityManagerFacadeLocal entityManagerFacade;
    private ConfigurationManagerLocal configurationManager;

    private SubjectPreferencesCache() {
        subjectPreferences = new HashMap<Integer, Configuration>();
        subjectManager = LookupUtil.getSubjectManager();
        entityManagerFacade = LookupUtil.getEntityManagerFacade();
        configurationManager = LookupUtil.getConfigurationManager();
    }

    public static SubjectPreferencesCache getInstance() {
        return instance;
    }

    private void load(int subjectId) {
        // if subject ID is 0, it probably means this is a new LDAP user that needs to be registered
        if (subjectId != 0 && !subjectPreferences.containsKey(subjectId)) {
            try {
                Subject subject = subjectManager.loadUserConfiguration(subjectId);
                Configuration configuration = subject.getUserConfiguration();
                subjectPreferences.put(subjectId, configuration);
            } catch (Throwable t) {
                log.warn("Can not get preferences for subject[id=" + subjectId + "], subject does not exist yet");
            }
        }
    }

    public synchronized PropertySimple getUserProperty(int subjectId, String propertyName) {
        load(subjectId);

        Configuration config = subjectPreferences.get(subjectId);
        if (config == null) {
            return null;
        }

        PropertySimple prop = config.getSimple(propertyName);
        if (prop == null) {
            return null;
        }

        return new PropertySimple(propertyName, prop.getStringValue());
    }

    public synchronized void setUserProperty(int subjectId, String propertyName, String value) {
        load(subjectId);

        Configuration config = subjectPreferences.get(subjectId);
        if (config == null) {
            return;
        }

        PropertySimple prop = config.getSimple(propertyName);
        if (prop == null) {
            prop = new PropertySimple(propertyName, value);
            config.put(prop); // add new to collection
            mergeProperty(prop);
        } else if (prop.getStringValue() == null || !prop.getStringValue().equals(value)) {
            prop.setStringValue(value);
            mergeProperty(prop);
        }
    }

    private void mergeProperty(PropertySimple prop) {
        // merge will persist if property doesn't exist (i.e., id = 0)
        PropertySimple mergedProperty = entityManagerFacade.merge(prop); // only merge changes
        if (prop.getId() == 0) {
            // so subsequent merges do not continue re-persisting property as new
            prop.setId(mergedProperty.getId());
        }
    }

    public synchronized void unsetUserProperty(int subjectId, String propertyName) {
        load(subjectId);

        Configuration config = subjectPreferences.get(subjectId);
        if (config == null) {
            return;
        }

        Property property = config.remove(propertyName);
        // it's possible property was already removed, and thus this operation becomes a no-op to the backing store
        if (property != null && property.getId() != 0) {
            try {
                configurationManager.deleteProperties(new int[] { property.getId() });
            } catch (Throwable t) {
                log.error("Could not remove " + property, t);
            }
        }
    }

    public synchronized void clearConfiguration(int subjectId) {
        if (log.isTraceEnabled()) {
            log.trace("Removing PreferencesCache For " + subjectId);
        }
        subjectPreferences.remove(subjectId);
    }
}
