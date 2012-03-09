/*
 * RHQ Management Platform
 * Copyright (C) 2005-2008 Red Hat, Inc.
 * All rights reserved.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */
package org.rhq.enterprise.server.auth.test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import javax.security.auth.login.LoginException;

import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.authz.Permission;
import org.rhq.core.domain.authz.Role;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.criteria.SubjectCriteria;
import org.rhq.core.domain.util.PageList;
import org.rhq.core.domain.util.PageOrdering;
import org.rhq.enterprise.server.auth.SessionManager;
import org.rhq.enterprise.server.auth.SessionNotFoundException;
import org.rhq.enterprise.server.auth.SubjectManagerLocal;
import org.rhq.enterprise.server.authz.AuthorizationManagerLocal;
import org.rhq.enterprise.server.authz.PermissionException;
import org.rhq.enterprise.server.authz.RoleManagerLocal;
import org.rhq.enterprise.server.test.AbstractEJB3Test;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * Tests the subject manager.
 */
@Test
public class SubjectManagerBeanTest extends AbstractEJB3Test {
    
    private SubjectManagerLocal subjectManager;
    private AuthorizationManagerLocal authorizationManager;
    private RoleManagerLocal roleManager;

    /**
     * Prepares things for the entire test class.
     */
    @BeforeClass
    public void beforeClass() {
        subjectManager = LookupUtil.getSubjectManager();
        authorizationManager = LookupUtil.getAuthorizationManager();
        roleManager = LookupUtil.getRoleManager();
    }

    /**
     * This runs after each test method. It clears out all sessions.
     */
    @AfterMethod
    public void purgeAllSessions() {
        // create a list of all users we know our tests have used
        List<String> usernames = new ArrayList<String>();
        usernames.add("admin");
        usernames.add("rhqadmin");
        usernames.add("new_user");

        SessionManager session_manager = SessionManager.getInstance();

        // clean out all users' sessions (a user can have more than one session)
        while (usernames.size() > 0) {
            try {
                String doomed_user = usernames.get(0);
                session_manager.invalidate(doomed_user);
            } finally {
                usernames.remove(0);
            }
        }
    }

    /**
     * Tests persisting and retrieving user configuration.
     *
     * @throws Exception
     */
    public void testUserConfiguration() throws Exception {
        Subject superuser;

        getTransactionManager().begin();

        try {
            superuser = subjectManager.getOverlord();

            // create the user
            Subject new_user = new Subject("dummy-user", true, false);
            new_user = subjectManager.createSubject(superuser, new_user);
            assert new_user.getUserConfiguration() == null : "There should not be any configuration yet";

            // set and persist an empty configuration
            Configuration config = new Configuration();
            new_user.setUserConfiguration(config);
            assert new_user.getUserConfiguration() != null : "An empty configuration should have been set";
            assert new_user.getUserConfiguration().getProperties().size() == 0 : "An empty config should have been set";
            assert new_user.getUserConfiguration().getId() == 0 : "Configuration wasn't persisted - should not have an ID";

            new_user = subjectManager.updateSubject(superuser, new_user); // let superuser change it
            config = new_user.getUserConfiguration();
            assert config != null : "An empty configuration should have been persisted";
            assert config.getProperties().size() == 0 : "An empty config should have been persisted";
            assert config.getId() != 0 : "Configuration was persisted - should have an ID";

            // set and persist a non-empty configuration
            config.put(new PropertySimple("firstname", "firstvalue"));
            config.put(new PropertySimple("secondname", "secondvalue"));
            assert new_user.getUserConfiguration() != null : "A full configuration should have been set";
            assert new_user.getUserConfiguration().getProperties().size() == 2 : "A full config should have been set";

            new_user = createSession(new_user);
            new_user = subjectManager.updateSubject(new_user, new_user); // let the user itself change it
            config = new_user.getUserConfiguration();
            assert config != null : "A full configuration should have been persisted";
            assert config.getProperties().size() == 2 : "A full config should have been persisted";
            assert config.getId() != 0 : "Configuration was persisted - should have an ID";
            assert config.getSimple("firstname").getStringValue().equals("firstvalue") : "Configuration wasn't persisted properly";
            assert config.getSimple("secondname").getStringValue().equals("secondvalue") : "Configuration wasn't persisted properly!";

            // let's try the load method to make sure it can eagerly load the configuration
            Subject loaded = subjectManager.loadUserConfiguration(new_user.getId());
            config = loaded.getUserConfiguration();
            assert config != null : "A full configuration should have been persisted";
            assert config.getProperties().size() == 2 : "A full config should have been persisted";
            assert config.getId() != 0 : "Configuration was persisted - should have an ID";
            assert config.getSimple("firstname").getStringValue().equals("firstvalue") : "Configuration wasn't persisted properly";
            assert config.getSimple("secondname").getStringValue().equals("secondvalue") : "Configuration wasn't persisted properly!";

            // set and persist a null configuration
            new_user.setUserConfiguration(null);
            assert new_user.getUserConfiguration() == null : "There should not be any configuration anymore";

            new_user = subjectManager.updateSubject(superuser, new_user); // let superuser change it
            assert new_user.getUserConfiguration() == null : "An empty configuration should have been persisted";
        } finally {
            getTransactionManager().rollback();
        }
    }

    /**
     * Makes sure you cannot disable super users.
     *
     * @throws Exception
     */
    public void testTryToDisableSuperUsers() throws Exception {
        Subject superuser;
        Subject rhqadmin;

        getTransactionManager().begin();
        try {
            superuser = subjectManager.getOverlord();
            rhqadmin = subjectManager.getSubjectByName("rhqadmin");
            rhqadmin = createSession(rhqadmin);

            try {
                superuser.setFactive(false);
                subjectManager.updateSubject(rhqadmin, superuser);
                assert false : "You should not be able to disable the superuser";
            } catch (PermissionException se) {
                superuser.setFactive(true);
            }
        } finally {
            getTransactionManager().rollback();
        }

        getTransactionManager().begin();
        try {
            try {
                rhqadmin.setFactive(false);
                subjectManager.updateSubject(superuser, rhqadmin);
                assert false : "You should not be able to delete the rhqadmin user";
            } catch (PermissionException se) {
                rhqadmin.setFactive(true);
            }
        } finally {
            getTransactionManager().rollback();
        }
    }

    /**
     * Makes sure you cannot delete the super users.
     *
     * @throws Exception
     */
    public void testTryToDeleteSuperUsers() throws Exception {
        Subject superuser;
        Subject rhqadmin;

        getTransactionManager().begin();
        try {
            superuser = subjectManager.getOverlord();
            superuser = createSession(superuser);
            rhqadmin = subjectManager.getSubjectByName("rhqadmin");
            rhqadmin = createSession(rhqadmin);

            try {
                subjectManager.deleteUsers(superuser, new int[] { rhqadmin.getId() });
                assert false : "You should not be able to delete the rhqadmin user";
            } catch (PermissionException se) {
            }
        } finally {
            getTransactionManager().rollback();
        }

        getTransactionManager().begin();
        try {
            try {
                subjectManager.deleteUsers(rhqadmin, new int[] { superuser.getId() });
                assert false : "You should not be able to delete the superuser";
            } catch (PermissionException se) {
            }
        } finally {
            getTransactionManager().rollback();
        }

        // but you should be allowed to change the rhqadmin's principal password
        getTransactionManager().begin();
        try {
            subjectManager.changePassword(rhqadmin, rhqadmin.getName(), "change-me");
        } finally {
            getTransactionManager().rollback();
        }
    }

    /**
     * Tests CRUD on subjects.
     *
     * @throws Exception
     */
    public void testSubjects() throws Exception {
        Subject new_user = null;

        getTransactionManager().begin();

        List<Permission> all_global_perms = new ArrayList<Permission>();
        all_global_perms.add(Permission.MANAGE_SECURITY);
        all_global_perms.add(Permission.MANAGE_INVENTORY);
        all_global_perms.add(Permission.MANAGE_SETTINGS);

        // get the system super subject
        Subject superuser = subjectManager.getSubjectById(1);
        assert superuser.getId() == 1;
        assert superuser.getName().equals("admin");
        assert authorizationManager.getExplicitGlobalPermissions(superuser).containsAll(all_global_perms);

        // get the rhqadmin subject
        Subject rhqadmin = subjectManager.getSubjectByName("rhqadmin");
        assert rhqadmin.getId() == 2;
        assert rhqadmin.getName().equals("rhqadmin");
        assert authorizationManager.getExplicitGlobalPermissions(rhqadmin).containsAll(all_global_perms);

        rhqadmin = createSession(rhqadmin); // our test needs to ensure the rhqadmin user has a session

        // check the subjects that do and do not have principals
        Collection<String> all_users_with_principals = subjectManager.findAllUsersWithPrincipals();
        assert !all_users_with_principals.contains(superuser.getName());
        assert all_users_with_principals.contains(rhqadmin.getName());
        assert !subjectManager.isUserWithPrincipal(superuser.getName());
        assert subjectManager.isUserWithPrincipal(rhqadmin.getName());

        // get all subjects, whether or not they have a principal
        SubjectManagerLocal subjectManager = LookupUtil.getSubjectManager();
        Subject subject = null;
        try {
            subject = subjectManager.loginUnauthenticated("rhqadmin");
        } catch (Exception e) {
            assert false : "There must be at least rhqadmin user";
        }

        SubjectCriteria c = new SubjectCriteria();
        c.addFilterFsystem(false);
        c.addSortName(PageOrdering.ASC);
        PageList<Subject> all_subjects = subjectManager.findSubjectsByCriteria(subject, c);

        assert all_subjects.size() >= 1 : "There must be at least rhqadmin user";
        assert !all_subjects.contains(superuser) : "The superuser should not have been returned in the list";
        assert all_subjects.contains(rhqadmin) : "Missing user [" + rhqadmin + "] from: " + all_subjects;

        // create, modify then delete the user
        new_user = new Subject();
        new_user.setFsystem(false);
        new_user.setFactive(true);
        new_user.setName("dummy-user");
        new_user.setFirstName("my-firstname");
        new_user.setLastName("my-lastname");
        new_user.setEmailAddress("email@address.com");
        new_user.setPhoneNumber("1-800-555-1212");
        new_user.setSmsAddress("sms address");
        new_user.setDepartment("my-department");

        new_user = subjectManager.createSubject(rhqadmin, new_user);
        new_user = createSession(new_user);
        assert !subjectManager.isUserWithPrincipal(new_user.getName());
        subjectManager.createPrincipal(subjectManager.getOverlord(), new_user.getName(), "my-password");
        assert subjectManager.isUserWithPrincipal(new_user.getName());

        // make sure it was persisted and you can actually login with it
        assert new_user.getId() != 0;
        Subject login_new_user = subjectManager.loginUnauthenticated(new_user.getName());
        assert login_new_user.equals(new_user);
        new_user = login_new_user; // login_new_user was given a new session ID

        subjectManager.changePassword(new_user, new_user.getName(), "my-new-password");
        subjectManager.changePassword(rhqadmin, new_user.getName(), "my-new-password"); // see that rhqadmin can change it too

        getTransactionManager().commit();
        getTransactionManager().begin();

        try {
            subjectManager.changePassword(new_user, rhqadmin.getName(), "not-allowed");
            assert false : "The new user does not have permission to change another's password";
        } catch (PermissionException se) {
        } finally {
            // PermissionException causes a rollback, let's start a new tx
            getTransactionManager().rollback();
        }

        getTransactionManager().begin();

        // this new user has no roles - therefore, no global permissions
        assert authorizationManager.getExplicitGlobalPermissions(new_user).size() == 0;

        try {
            subjectManager.deleteUsers(new_user, new int[] { new_user.getId() });
            assert false : "The new user should not have had the permission to delete itself";
        } catch (PermissionException e) {
        } finally {
            // PermissionException causes a rollback, let's start a new tx
            getTransactionManager().rollback();
        }

        getTransactionManager().begin();

        // delete the new user
        assert subjectManager.getSubjectByName(new_user.getName()).equals(new_user);
        assert subjectManager.isUserWithPrincipal(new_user.getName());
        subjectManager.deleteUsers(rhqadmin, new int[] { new_user.getId() });
        assert subjectManager.getSubjectByName(new_user.getName()) == null;
        assert !subjectManager.isUserWithPrincipal(new_user.getName());

        getTransactionManager().commit();
    }

    /**
     * Tests getting a super user subject.
     *
     * @throws Exception
     */
    public void testGetSuperUser() throws Exception {
        getTransactionManager().begin();

        try {
            Subject superuser1 = subjectManager.getOverlord();
            assert superuser1.getId() == 1;

            Subject superuser2 = subjectManager.getOverlord();
            assert superuser2.getId() == 1;

            assert superuser1.equals(superuser2);

            // that second call should have used the same session since not enough time elapsed to expire the session
            assert superuser1.getSessionId().equals(superuser2.getSessionId());
        } finally {
            getTransactionManager().rollback();
        }
    }

    /**
     * Tests logging in and out when not requiring passwords.
     *
     * @throws Exception
     */
    public void testLoginUnauthenticated() throws Exception {
        getTransactionManager().begin();

        try {
            Subject subject1 = subjectManager.loginUnauthenticated("rhqadmin");
            int session1 = subject1.getSessionId();

            Thread.sleep(500); // just wait a bit

            Subject subject2 = subjectManager.loginUnauthenticated("rhqadmin");
            int session2 = subject2.getSessionId();

            assert session1 != session2 : "The same sessionId should never be assigned when logging in twice";
            assert subject1.equals(subject2);

            Subject s = subjectManager.getSubjectByNameAndSessionId("rhqadmin", subject1.getSessionId());
            assert s.getSessionId() == session1;
            s = subjectManager.getSubjectByNameAndSessionId("rhqadmin", subject2.getSessionId());
            assert s.getSessionId() == session2;

            subjectManager.logout(session1);
            try {
                s = subjectManager.getSubjectByNameAndSessionId("rhqadmin", subject1.getSessionId());
                assert false : "Session should be invalid";
            } catch (SessionNotFoundException ok) {
            }

            s = subjectManager.getSubjectByNameAndSessionId("rhqadmin", subject2.getSessionId());
            assert s.getSessionId() == session2;

            // this should ne a no-op, no exception
            subjectManager.logout(session1);

            subjectManager.logout(session2);
            try {
                s = subjectManager.getSubjectByNameAndSessionId("rhqadmin", subject2.getSessionId());
                fail("Session should be invalid");
            } catch (SessionNotFoundException e) {
                // expected
            }

            try {
                subjectManager.loginUnauthenticated("rhqadminX");
                assert false : "Should not have logged in - provided a bad username";
            } catch (LoginException ok) {
            }
        } finally {
            getTransactionManager().rollback();
        }
    }

    public void testDeleteUser() throws Exception {
        getTransactionManager().begin();

        try {
            Subject overlord = subjectManager.getOverlord();

            Subject new_user = new Subject("dummy-user", true, false);
            new_user = subjectManager.createSubject(overlord, new_user);

            Role new_role = new Role("dummy-role");
            RoleManagerLocal roleManager = LookupUtil.getRoleManager();
            new_role = roleManager.createRole(overlord, new_role);

            //            new_user.addRole(new_role);
            int[] subjectIds = new int[] { new_user.getId() };
            roleManager.addSubjectsToRole(overlord, new_role.getId(), subjectIds);
            assert new_role.getSubjects().contains(new_user) : "New_role does not contain new_user";
            int count = new_role.getSubjects().size();

            subjectManager.deleteUsers(overlord, subjectIds);

            assert new_role.getSubjects().size() == count - 1 : "User was not deleted from new_role";
        } finally {
            getTransactionManager().rollback();
        }

    }
    
    public void testViewUsersPermission_subjectWithViewUsersRoleCanViewOtherUsers() throws Exception {
        getTransactionManager().begin();

        try {
            Subject overlord = subjectManager.getOverlord();
            Subject rhqadmin = subjectManager.getSubjectByName("rhqadmin");

            Role roleWithViewUsersPerm = new Role("role" + UUID.randomUUID());
            roleWithViewUsersPerm.addPermission(Permission.VIEW_USERS);
            roleWithViewUsersPerm = roleManager.createRole(overlord, roleWithViewUsersPerm);

            Subject subjectWithViewUsersRole = new Subject("subject" + UUID.randomUUID(), true, false);
            subjectWithViewUsersRole.addRole(roleWithViewUsersPerm);
            subjectWithViewUsersRole = subjectManager.createSubject(overlord, subjectWithViewUsersRole, "password");
            subjectWithViewUsersRole = subjectManager.loginUnauthenticated(subjectWithViewUsersRole.getName());

            Subject anotherSubject = new Subject("subject" + UUID.randomUUID(), true, false);
            anotherSubject = subjectManager.createSubject(overlord, anotherSubject, "password");

            PageList<Subject> subjects = subjectManager.findSubjectsByCriteria(subjectWithViewUsersRole, new SubjectCriteria());
            Set<Subject> subjectSet = new HashSet<Subject>(subjects);
            assertTrue(subjectSet.contains(subjectWithViewUsersRole));
            assertTrue(subjectSet.contains(anotherSubject));
            assertTrue(subjectSet.contains(rhqadmin));
        } finally {
            getTransactionManager().rollback();
        }
    }

    public void testViewUsersPermission_rhqadminCanViewOtherUsers() throws Exception {
        getTransactionManager().begin();

        try {
            Subject overlord = subjectManager.getOverlord();

            Subject rhqadmin = subjectManager.getSubjectByName("rhqadmin");
            rhqadmin = subjectManager.loginUnauthenticated(rhqadmin.getName());

            Subject anotherSubject = new Subject("subject" + UUID.randomUUID(), true, false);
            anotherSubject = subjectManager.createSubject(overlord, anotherSubject, "password");

            PageList<Subject> subjects = subjectManager.findSubjectsByCriteria(rhqadmin, new SubjectCriteria());
            Set<Subject> subjectSet = new HashSet<Subject>(subjects);
            assertTrue(subjectSet.contains(anotherSubject));
            assertTrue(subjectSet.contains(rhqadmin));
        } finally {
            getTransactionManager().rollback();
        }
    }

    public void testViewUsersPermission_subjectWithNonViewUsersRoleCannotViewOtherUsers() throws Exception {
        getTransactionManager().begin();

        try {
            Subject overlord = subjectManager.getOverlord();

            Role roleWithoutViewUsersPerm = new Role("role" + UUID.randomUUID());
            roleWithoutViewUsersPerm = roleManager.createRole(overlord, roleWithoutViewUsersPerm);

            Subject subjectWithNonViewUsersRole = new Subject("subject" + UUID.randomUUID(), true, false);
            subjectWithNonViewUsersRole.addRole(roleWithoutViewUsersPerm);
            subjectWithNonViewUsersRole = subjectManager.createSubject(overlord, subjectWithNonViewUsersRole, "password");
            subjectWithNonViewUsersRole = subjectManager.loginUnauthenticated(subjectWithNonViewUsersRole.getName());

            Subject anotherSubject = new Subject("subject" + UUID.randomUUID(), true, false);
            anotherSubject = subjectManager.createSubject(overlord, anotherSubject, "password");

            PageList<Subject> subjects = subjectManager.findSubjectsByCriteria(subjectWithNonViewUsersRole, new SubjectCriteria());
            Set<Subject> subjectSet = new HashSet<Subject>(subjects);
            assertEquals(1, subjectSet.size());
            assertTrue(subjectSet.contains(subjectWithNonViewUsersRole));
        } finally {
            getTransactionManager().rollback();
        }
    }

    public void testViewUsersPermission_subjectWithNoRolesCannotViewOtherUsers() throws Exception {
        getTransactionManager().begin();

        try {
            Subject overlord = subjectManager.getOverlord();

            Subject subjectWithNoRoles = new Subject("subject" + UUID.randomUUID(), true, false);
            subjectWithNoRoles = subjectManager.createSubject(overlord, subjectWithNoRoles, "password");
            subjectWithNoRoles = subjectManager.loginUnauthenticated(subjectWithNoRoles.getName());

            Subject anotherSubject = new Subject("subject" + UUID.randomUUID(), true, false);
            anotherSubject = subjectManager.createSubject(overlord, anotherSubject, "password");

            PageList<Subject> subjects = subjectManager.findSubjectsByCriteria(subjectWithNoRoles, new SubjectCriteria());
            Set<Subject> subjectSet = new HashSet<Subject>(subjects);
            assertEquals(1, subjectSet.size());
            assertTrue(subjectSet.contains(subjectWithNoRoles));
        } finally {
            getTransactionManager().rollback();
        }
    }

}