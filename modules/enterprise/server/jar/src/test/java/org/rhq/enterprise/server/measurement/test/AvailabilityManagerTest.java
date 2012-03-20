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
package org.rhq.enterprise.server.measurement.test;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Random;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import javax.transaction.TransactionManager;

import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.criteria.AvailabilityCriteria;
import org.rhq.core.domain.discovery.AvailabilityReport;
import org.rhq.core.domain.measurement.Availability;
import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.domain.measurement.ResourceAvailability;
import org.rhq.core.domain.resource.Agent;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceCategory;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.domain.util.PageOrdering;
import org.rhq.enterprise.server.measurement.AvailabilityManagerLocal;
import org.rhq.enterprise.server.measurement.AvailabilityPoint;
import org.rhq.enterprise.server.resource.ResourceAvailabilityManagerLocal;
import org.rhq.enterprise.server.resource.ResourceManagerLocal;
import org.rhq.enterprise.server.test.AbstractEJB3Test;
import org.rhq.enterprise.server.test.TestServerPluginService;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * Test the functionality of the AvailabilityManager
 *
 * @author Heiko W. Rupp
 * @author John Mazzitelli
 */
public class AvailabilityManagerTest extends AbstractEJB3Test {
    private static final boolean ENABLE_TESTS = true;

    private static final AvailabilityType UP = AvailabilityType.UP;
    private static final AvailabilityType DOWN = AvailabilityType.DOWN;
    private static final AvailabilityType DISABLED = AvailabilityType.DISABLED;
    private static final AvailabilityType UNKNOWN = AvailabilityType.UNKNOWN;

    private AvailabilityManagerLocal availabilityManager;
    private ResourceAvailabilityManagerLocal resourceAvailabilityManager;
    private ResourceManagerLocal resourceManager;

    private Subject overlord;
    private Agent theAgent;
    private Resource theResource;
    private ResourceType theResourceType;
    private List<Resource> additionalResources;
    private Availability availability1;
    private Availability availability2;
    private Availability availability3;

    private TestServerPluginService testServerPluginService;

    @BeforeMethod
    public void beforeMethod() {
        try {
            prepareScheduler();

            this.availabilityManager = LookupUtil.getAvailabilityManager();
            this.resourceAvailabilityManager = LookupUtil.getResourceAvailabilityManager();
            this.resourceManager = LookupUtil.getResourceManager();
            this.overlord = LookupUtil.getSubjectManager().getOverlord();
            additionalResources = new ArrayList<Resource>();

            testServerPluginService = new TestServerPluginService();
            prepareCustomServerPluginService(testServerPluginService);
            testServerPluginService.masterConfig.getPluginDirectory().mkdirs();
            testServerPluginService.startMasterPluginContainer();
        } catch (Throwable t) {
            // Catch RuntimeExceptions and Errors and dump their stack trace, because Surefire will completely swallow them
            // and throw a cryptic NPE (see http://jira.codehaus.org/browse/SUREFIRE-157)!
            t.printStackTrace();
            throw new RuntimeException(t);
        }
    }

    @AfterMethod
    public void afterMethod() throws Exception {
        try {
            if (theResource != null) {
                // perform in-band and out-of-band work in quick succession
                // this also deletes our attached agent
                resourceManager.uninventoryResource(overlord, theResource.getId());
                resourceManager.uninventoryResourceAsyncWork(overlord, theResource.getId());
                theResource = null;
            }

            if (additionalResources != null) {
                getTransactionManager().begin();
                EntityManager em = getEntityManager();

                for (Resource res : additionalResources) {
                    Resource res2 = em.find(Resource.class, res.getId());
                    resourceManager.uninventoryResource(overlord, res2.getId());
                    resourceManager.uninventoryResourceAsyncWork(overlord, res2.getId());

                }
                getTransactionManager().commit();
            }

            if (theResourceType != null) {
                getTransactionManager().begin();
                EntityManager em = getEntityManager();

                em.remove(em.find(ResourceType.class, theResourceType.getId()));
                theResourceType = null;

                getTransactionManager().commit();
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }

        finally {
            unprepareScheduler();
            unprepareServerPluginService();
            testServerPluginService.stopMasterPluginContainer();
        }
    }

    @SuppressWarnings("unchecked")
    @Test(enabled = ENABLE_TESTS)
    public void testInsertPastAvailabilities() throws Exception {
        Long now = System.currentTimeMillis();
        Long middle = now - 30000; // 30s ago
        Long then = now - 60000; // 60s ago

        EntityManager em = beginTx();

        try {
            setupResource(em);
            commitAndClose(em);

            Availability aThen = new Availability(theResource, then, UP);
            aThen.setEndTime(middle);

            Availability aMiddle = new Availability(theResource, middle, DOWN);
            aMiddle.setEndTime(now);

            Availability aNow = new Availability(theResource, now, UP);

            /*
             * Simulate a report (aMiddle) that came in late (e.g. because of sorting
             * issues on the agent or because of a network blip anyway. Expectation is
             * that it gets just inserted in the middle.
             */
            // UNKNOWN(0) -->
            persistAvailability(aThen);
            // UNKNOWN(0) --> UP(-60000) -->            
            persistAvailability(aNow);
            // no change, already at UP
            // UNKNOWN(0) --> UP(-60000) -->            
            persistAvailability(aMiddle);
            // UNKNOWN(0) --> UP(-60000) --> DOWN(-30000)

            em = beginTx();
            Query q = em.createNamedQuery(Availability.FIND_BY_RESOURCE);
            q.setParameter("resourceId", theResource.getId());
            List<Availability> avails = q.getResultList();

            assert avails.size() == 3 : "Did not get 3 availabilities but " + avails.size();

        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        } finally {
            if (em != null) {
                getTransactionManager().rollback();
                em.close();
            }
        }
    }

    @SuppressWarnings("unchecked")
    @Test(enabled = ENABLE_TESTS)
    public void testPurgeAvailabilities() throws Exception {
        Long now = System.currentTimeMillis();
        Long middle = now - 30000; // 30s ago
        Long then = now - 60000; // 60s ago

        EntityManager em = beginTx();

        try {
            setupResource(em);
            commitAndClose(em);

            Availability aThen = new Availability(theResource, then, UP);

            Availability aMiddle = new Availability(theResource, middle, DOWN);
            aMiddle.setEndTime(now);

            Availability aNow = new Availability(theResource, now, UP);

            // UNKNOWN(0) -->
            persistAvailability(aThen);
            // UNKNOWN(0) --> UP(-60000) -->            
            persistAvailability(aMiddle);
            // UNKNOWN(0) --> UP(-60000) --> DOWN(-30000) -->        
            persistAvailability(aNow);
            // UNKNOWN(0) --> UP(-60000) --> DOWN(-30000) --> UP(NOW) -->            

            em = beginTx();

            int purged = availabilityManager.purgeAvailabilities(now - 29999); // keeps aMiddle and aNow
            assert purged == 2 : "Didn't purge 2 --> " + purged;

            Query q = em.createNamedQuery(Availability.FIND_BY_RESOURCE);
            q.setParameter("resourceId", theResource.getId());
            List<Availability> avails = q.getResultList();

            assert avails.size() == 2;
            assert avails.get(0).getAvailabilityType() == DOWN;
            assert avails.get(0).getStartTime().equals(middle);
            assert avails.get(0).getEndTime().equals(now);
            assert avails.get(1).getAvailabilityType() == UP;
            assert avails.get(1).getStartTime().equals(now);
            assert avails.get(1).getEndTime() == null;

            // try to delete them all - but we never should delete the latest
            purged = availabilityManager.purgeAvailabilities(now + 12345);
            assert purged == 1 : "Didn't purge 1 --> " + purged;
            purged = availabilityManager.purgeAvailabilities(now + 12345);
            assert purged == 0 : "Didn't purge 0 --> " + purged;

            q = em.createNamedQuery(Availability.FIND_BY_RESOURCE);
            q.setParameter("resourceId", theResource.getId());
            avails = q.getResultList();

            assert avails.size() == 1;
            assert avails.get(0).getAvailabilityType() == UP;
            assert avails.get(0).getStartTime().equals(now);
            assert avails.get(0).getEndTime() == null;
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        } finally {
            if (em != null) {
                getTransactionManager().rollback();
                em.close();
            }
        }
    }

    @Test(enabled = ENABLE_TESTS)
    public void testGetAvailabilities() throws Exception {
        EntityManager em = beginTx();

        try {
            List<AvailabilityPoint> availPoints;
            Availability avail;

            setupResource(em);
            // platform: UNKNOWN(0) -->             
            commitAndClose(em);
            em = null;

            AvailabilityReport report = new AvailabilityReport(false, theAgent.getName());

            availPoints = availabilityManager.findAvailabilitiesForResource(overlord, theResource.getId(), 1,
                System.currentTimeMillis(), 3, false);
            assert availPoints.size() == 3 : "There is no avail data, but should still get 3 availability points";
            assert availPoints.get(0).getAvailabilityType() == UNKNOWN;
            assert availPoints.get(1).getAvailabilityType() == UNKNOWN;
            assert availPoints.get(2).getAvailabilityType() == UNKNOWN;

            Long startMillis = 60000L;
            avail = new Availability(theResource, startMillis, UP);
            report.addAvailability(avail);
            availabilityManager.mergeAvailabilityReport(report);
            // platform: UNKNOWN(0) --> UP(60000) -->            

            // our avail data point is right on the start edge
            availPoints = availabilityManager.findAvailabilitiesForResource(overlord, theResource.getId(), startMillis,
                startMillis + 10000, 3, false);
            assert availPoints.size() == 3 : "There is 1 avail data, but should still get 3 availability points";
            assert availPoints.get(0).getAvailabilityType() == UP;
            assert availPoints.get(1).getAvailabilityType() == UP;
            assert availPoints.get(2).getAvailabilityType() == UP;

            // our avail data point is right on the end edge
            availPoints = availabilityManager.findAvailabilitiesForResource(overlord, theResource.getId(),
                startMillis - 3, startMillis, 3, false);
            assert availPoints.size() == 3 : "There is 1 avail data, but should still get 3 availability points";
            assert availPoints.get(0).getAvailabilityType() == UNKNOWN;
            assert !availPoints.get(0).isKnown() : availPoints;
            assert availPoints.get(1).getAvailabilityType() == UNKNOWN;
            assert !availPoints.get(1).isKnown() : availPoints;
            assert availPoints.get(2).getAvailabilityType() == UNKNOWN;
            assert !availPoints.get(2).isKnown() : availPoints;

            availPoints = availabilityManager.findAvailabilitiesForResource(overlord, theResource.getId(),
                startMillis - 20000, startMillis + 10000, 3, false);
            assert availPoints.size() == 3 : "There is 1 avail data, but should still get 3 availability points";
            assert availPoints.get(0).getAvailabilityType() == UNKNOWN;
            assert !availPoints.get(0).isKnown() : availPoints;
            assert availPoints.get(1).getAvailabilityType() == UNKNOWN;
            assert !availPoints.get(1).isKnown() : availPoints;
            assert availPoints.get(2).getAvailabilityType() == UP : availPoints;

            availPoints = availabilityManager.findAvailabilitiesForResource(overlord, theResource.getId(),
                startMillis - 10000, startMillis + 20000, 3, false);
            assert availPoints.size() == 3 : "There is 1 avail data, but should still get 3 availability points";
            assert availPoints.get(0).getAvailabilityType() == UNKNOWN;
            assert !availPoints.get(0).isKnown() : availPoints;
            assert availPoints.get(1).getAvailabilityType() == UP;
            assert availPoints.get(1).isKnown() : availPoints;
            assert availPoints.get(2).getAvailabilityType() == UP;

            availPoints = availabilityManager.findAvailabilitiesForResource(overlord, theResource.getId(),
                startMillis - 20000, startMillis + 20000, 10, false);
            assert availPoints.size() == 10 : "There is 1 avail data, but should still get 10 availability points";
            assert availPoints.get(0).getAvailabilityType() == UNKNOWN : availPoints;
            assert !availPoints.get(0).isKnown() : availPoints;
            assert availPoints.get(1).getAvailabilityType() == UNKNOWN : availPoints;
            assert !availPoints.get(1).isKnown() : availPoints;
            assert availPoints.get(2).getAvailabilityType() == UNKNOWN : availPoints;
            assert !availPoints.get(2).isKnown() : availPoints;
            assert availPoints.get(3).getAvailabilityType() == UNKNOWN : availPoints;
            assert !availPoints.get(3).isKnown() : availPoints;
            assert availPoints.get(4).getAvailabilityType() == UNKNOWN : availPoints;
            assert !availPoints.get(4).isKnown() : availPoints;
            assert availPoints.get(5).getAvailabilityType() == UP : availPoints;
            assert availPoints.get(5).isKnown() : availPoints;
            assert availPoints.get(6).getAvailabilityType() == UP : availPoints;
            assert availPoints.get(7).getAvailabilityType() == UP : availPoints;
            assert availPoints.get(8).getAvailabilityType() == UP : availPoints;
            assert availPoints.get(9).getAvailabilityType() == UP : availPoints;

            report = new AvailabilityReport(false, theAgent.getName()); // 70000
            report.setEnablementReport(true); // simulate a real disable
            report.addAvailability(new Availability(theResource, (startMillis + 10000L), DISABLED));
            availabilityManager.mergeAvailabilityReport(report);
            // UNKNOWN(0) --> UP(60000) --> DISABLED(70000) -->            

            // before setting other avails, must end disable with enablement report to unknown
            report = new AvailabilityReport(false, theAgent.getName()); // 75000
            report.setEnablementReport(true);
            report.addAvailability(new Availability(theResource, (startMillis + 15000L), UNKNOWN));
            availabilityManager.mergeAvailabilityReport(report);
            // UNKNOWN(0) --> UP(60000) --> DISABLED(70000) --> UNKNOWN(75000) -->            

            report = new AvailabilityReport(false, theAgent.getName()); // 80000
            report.addAvailability(new Availability(theResource, (startMillis + 20000L), UP));
            availabilityManager.mergeAvailabilityReport(report);
            // UNKNOWN(0) --> UP(60000) --> DISABLED(70000) --> UNKNOWN(75000) --> UP(80000)            

            report = new AvailabilityReport(false, theAgent.getName()); // 90000
            report.addAvailability(new Availability(theResource, (startMillis + 30000L), DOWN));
            availabilityManager.mergeAvailabilityReport(report);
            // UNKNOWN(0) --> UP(60000) --> DISABLED(70000) --> UNKNOWN(75000) --> UP(80000) --> DOWN(90000) -->            

            availPoints = availabilityManager.findAvailabilitiesForResource(overlord, theResource.getId(),
                startMillis - 15000, startMillis + 35000, 5, false); // 45000 - 95000

            // 45-55 == unknown
            assert availPoints.size() == 5 : "should get 5 availability points";
            assert availPoints.get(0).getAvailabilityType() == UNKNOWN : availPoints;
            assert !availPoints.get(0).isKnown() : availPoints;

            // 55-65 == 55-60=unknown, 60-65=up
            // because part of it was UNKNOWN, we consider the data point UNKNOWN
            assert availPoints.get(1).getAvailabilityType() == UNKNOWN : availPoints;
            assert !availPoints.get(1).isKnown() : availPoints;

            // 65-75 == 65-70=up, 70-75=disabled
            assert availPoints.get(2).getAvailabilityType() == DISABLED : availPoints;

            // 75-85 == 75-80=unknown, 80-85=up
            assert availPoints.get(3).getAvailabilityType() == UNKNOWN : availPoints;

            // 85-95,  == 85-90=up, 90-95=down
            assert availPoints.get(4).getAvailabilityType() == DOWN : availPoints;

            // 30000 - 90000
            availPoints = availabilityManager.findAvailabilitiesForResource(overlord, theResource.getId(),
                startMillis - 30000, startMillis + 30000, 10, false);

            assert availPoints.size() == 10 : "should get 10 availability points";
            assert availPoints.get(0).getAvailabilityType() == UNKNOWN : availPoints; // 30-36
            assert availPoints.get(1).getAvailabilityType() == UNKNOWN : availPoints; // 36-42
            assert availPoints.get(2).getAvailabilityType() == UNKNOWN : availPoints; // 42-48
            assert availPoints.get(3).getAvailabilityType() == UNKNOWN : availPoints; // 58-54
            assert availPoints.get(4).getAvailabilityType() == UNKNOWN : availPoints; // 54-60
            assert availPoints.get(5).getAvailabilityType() == UP : availPoints; // 60-66
            assert availPoints.get(6).getAvailabilityType() == DISABLED : availPoints; // 66-72
            assert availPoints.get(7).getAvailabilityType() == DISABLED : availPoints; // 72-78
            assert availPoints.get(8).getAvailabilityType() == UNKNOWN : availPoints; // 78-84
            assert availPoints.get(9).getAvailabilityType() == UP : availPoints; // 84-90
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        } finally {
            if (em != null) {
                getTransactionManager().rollback();
                em.close();
            }
        }
    }

    @Test(enabled = ENABLE_TESTS)
    public void testSetAllAgentResourceAvailabilities() throws Exception {
        EntityManager em = beginTx();

        try {
            setupResource(em);
            commitAndClose(em);
            em = null;

            // setAllAgentResourceAvails will only operate on those that have at least 1 avail row
            Availability avail = new Availability(theResource, UNKNOWN);
            persistAvailability(avail);

            assert availabilityManager.getCurrentAvailabilityTypeForResource(overlord, theResource.getId()) == UNKNOWN;
            availabilityManager.updateAgentResourceAvailabilities(theAgent.getId(), UP, UP);
            assert availabilityManager.getCurrentAvailabilityTypeForResource(overlord, theResource.getId()) == UP;
            availabilityManager.updateAgentResourceAvailabilities(theAgent.getId(), DOWN, DOWN);
            assert availabilityManager.getCurrentAvailabilityTypeForResource(overlord, theResource.getId()) == DOWN;
            availabilityManager.updateAgentResourceAvailabilities(theAgent.getId(), DOWN, DOWN); // extend it
            assert availabilityManager.getCurrentAvailabilityTypeForResource(overlord, theResource.getId()) == DOWN;
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        } finally {
            if (em != null) {
                getTransactionManager().rollback();
                em.close();
            }
        }
    }

    @Test(enabled = ENABLE_TESTS)
    public void testAgentBackfillNewResource() throws Exception {
        EntityManager em = beginTx();

        try {
            setupResource(em);
            commitAndClose(em);
            em = null;

            // have never heard from the new agent yet - we do not backfill anything in this case
            LookupUtil.getAgentManager().checkForSuspectAgents();
            assert availabilityManager.getCurrentAvailabilityTypeForResource(overlord, theResource.getId()) == UNKNOWN;

            em = beginTx();
            Resource resource = em.find(Resource.class, theResource.getId());
            List<Availability> avails = resource.getAvailability();
            assert avails != null;
            assert avails.size() == 1; // the initial avail on resource persist
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        } finally {
            if (em != null) {
                getTransactionManager().rollback();
                em.close();
            }
        }
    }

    @Test(enabled = ENABLE_TESTS)
    public void testAgentBackfill() throws Exception {
        EntityManager em = beginTx();

        try {
            prepareForTestAgents();

            setupResource(em);
            commitAndClose(em);
            em = null;

            // add a report that says the resource was up 20 minutes ago
            Availability avail = new Availability(theResource, (System.currentTimeMillis() - 12000000), UP);
            AvailabilityReport report = new AvailabilityReport(false, theAgent.getName());
            report.addAvailability(avail);
            // UNKNOWN(0) -->             
            availabilityManager.mergeAvailabilityReport(report);
            // UNKNOWN(0) --> UP(-12000000) -->            
            assert availabilityManager.getCurrentAvailabilityTypeForResource(overlord, theResource.getId()) == UP;

            // let's pretend we haven't heard from the agent in a few minutes
            em = beginTx();
            Agent agent = em.find(Agent.class, theAgent.getId());
            agent.setLastAvailabilityPing(System.currentTimeMillis() - (1000 * 60 * 18)); // 18 mins
            commitAndClose(em);
            em = null;

            // the agent should be suspect and will be considered down, the platform resource should be down
            // (although children should be UNKNOWN)
            LookupUtil.getAgentManager().checkForSuspectAgents(); // checks for 5 mins !!
            // UNKNOWN(0) --> UP(-12000000) -->DOWN(now) -->            
            AvailabilityType curAvail;
            curAvail = availabilityManager.getCurrentAvailabilityTypeForResource(overlord, theResource.getId());
            assert curAvail == DOWN : curAvail;

            // make sure our resource's new availabilities are consistent (first (UNKNOWN) , second (UP), third (DOWN))
            em = beginTx();
            Resource resource = em.find(Resource.class, theResource.getId());
            List<Availability> allAvails = resource.getAvailability();
            assert allAvails.size() == 3;
            commitAndClose(em);
            em = null;

            Availability a1 = allAvails.get(0);
            Availability a2 = allAvails.get(1);
            Availability a3 = allAvails.get(2);
            assert a1.getAvailabilityType() == UNKNOWN;
            assert a2.getAvailabilityType() == UP : a2.getAvailabilityType();
            assert a3.getAvailabilityType() == DOWN : a3.getAvailabilityType();
            assert a1.getEndTime() != null;
            assert a2.getEndTime() != null;
            assert a3.getEndTime() == null;
            assert a1.getEndTime() > a1.getStartTime();
            assert a2.getEndTime() > a2.getStartTime();
            assert a2.getStartTime().equals(a1.getEndTime());
            assert a3.getStartTime().equals(a2.getEndTime());

        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        } finally {
            unprepareForTestAgents();

            if (em != null) {
                getTransactionManager().rollback();
                em.close();
            }
        }
    }

    @Test(enabled = true)
    //ENABLE_TESTS)
    public void testAgentBackfillPerformance() throws Exception {
        EntityManager em = beginTx();
        List<Resource> allResources = new ArrayList<Resource>();

        try {
            prepareForTestAgents();

            setupResource(em); // setup theResource

            allResources.add(theResource);

            // now create a bunch more resources
            for (int i = 0; i < 99; i++) {
                allResources.add(setupAnotherResource(em, i, theResource));
            }
            em.flush();

            commitAndClose(em);
            em = null;

            for (Resource res : allResources) {
                int resId = res.getId();
                Availability currentAvailability = availabilityManager.getCurrentAvailabilityForResource(overlord,
                    resId);
                assert currentAvailability != null : "Current Availability was null for " + resId;
                assert currentAvailability.getAvailabilityType() == UNKNOWN : "Current AvailabilityType should have been UNKNOWN for "
                    + resId;

                AvailabilityCriteria c = new AvailabilityCriteria();
                c.addFilterResourceId(resId);
                c.addFilterInterval(0L, Long.MAX_VALUE);
                c.addSortStartTime(PageOrdering.ASC);
                List<Availability> allData = availabilityManager.findAvailabilityByCriteria(overlord, c);
                assert allData != null : "All availabilities was null for " + resId;
                assert allData.size() == 1 : "All availabilities size was " + allData.size() + " for " + resId;

                ResourceAvailability currentResAvail = resourceAvailabilityManager.getLatestAvailability(resId);
                assert currentResAvail != null : "Current ResourceAvailability was null for " + resId;
                assert currentResAvail.getAvailabilityType() == UNKNOWN : "Current ResourceAvailabilityType should have been UNKNOWN for "
                    + resId;
            }

            // let's pretend we haven't heard from the agent in a few minutes
            em = beginTx();
            Agent agent = em.find(Agent.class, theAgent.getId());
            agent.setLastAvailabilityPing(System.currentTimeMillis() - (1000 * 60 * 18)); // 18 mins
            commitAndClose(em);
            em = null;

            // the agent should be suspect and will be considered down. the resources have their initial
            // UNKNOWN avails.  The platform should get a new DOWN Availability row. The rest should remain
            // as is since they are already UNKNOWN.
            long start = System.currentTimeMillis();
            LookupUtil.getAgentManager().checkForSuspectAgents();

            System.out.println("testAgentBackfillPerformance: checkForSuspectAgents run 1 took "
                + (System.currentTimeMillis() - start) + "ms");

            // add a report that says the resources are now up or disabled- the report will add one avail for each
            // resource
            Thread.sleep(500);
            AvailabilityReport upReport = new AvailabilityReport(false, theAgent.getName());
            AvailabilityReport disabledReport = new AvailabilityReport(false, theAgent.getName());
            disabledReport.setEnablementReport(true);
            int resNum = 0;
            for (Resource resource : allResources) {
                if (resNum++ <= 80) {
                    upReport.addAvailability(new Availability(resource, UP));
                } else {
                    disabledReport.addAvailability(new Availability(resource, DISABLED));
                }
            }

            start = System.currentTimeMillis();
            availabilityManager.mergeAvailabilityReport(upReport);
            availabilityManager.mergeAvailabilityReport(disabledReport);

            System.out.println("testAgentBackfillPerformance: mergeAvailabilityReport run took "
                + (System.currentTimeMillis() - start) + "ms");

            // sanity check - make sure the merge at least appeared to work
            assert availabilityManager.getCurrentAvailabilityTypeForResource(overlord, theResource.getId()) == UP;

            // let's again pretend we haven't heard from the agent in a few minutes
            em = beginTx();
            agent = em.find(Agent.class, theAgent.getId());
            agent.setLastAvailabilityPing(System.currentTimeMillis() - (1000 * 60 * 18));
            commitAndClose(em);
            em = null;

            // the agent should be suspect and will be considered down
            // all of the resources have availabilities now, so another row will be added to them if they are not disabled
            start = System.currentTimeMillis();
            LookupUtil.getAgentManager().checkForSuspectAgents();

            System.out.println("testAgentBackfillPerformance: checkForSuspectAgents run 2 took "
                + (System.currentTimeMillis() - start) + "ms");

            AvailabilityType curAvail;
            start = System.currentTimeMillis();
            resNum = 0;

            for (Resource resource : allResources) {
                curAvail = availabilityManager.getCurrentAvailabilityTypeForResource(overlord, resource.getId());
                AvailabilityType expected = (0 == resNum) ? DOWN : ((resNum > 80) ? DISABLED : UNKNOWN);
                ++resNum;
                assert curAvail == expected : "Expected " + expected.name() + " but got " + curAvail.name() + " for "
                    + resource;

                // make sure our resources' new availabilities are consistent
                // the first time we backfilled everything was unknown, only the platform was updated. 
                // later we went UP/DISABLED then DOWN so we'll have 2, 3 or 4 rows)
                em = beginTx();
                resource = em.find(Resource.class, resource.getId());
                List<Availability> allAvails = resource.getAvailability();
                assert allAvails.size() == ((expected == DOWN) ? 4 : ((expected == DISABLED) ? 2 : 3)) : allAvails;
                commitAndClose(em);
                em = null;

                Availability a0 = allAvails.get(0);
                Availability a1 = allAvails.get(1);
                Availability a2 = null;
                Availability a3 = null;
                assert a0.getAvailabilityType() == UNKNOWN : allAvails;
                switch (expected) {
                case DOWN:
                    // platform
                    a2 = allAvails.get(2);
                    a3 = allAvails.get(3);
                    assert a1.getAvailabilityType() == DOWN : allAvails;
                    assert a2.getAvailabilityType() == UP : allAvails;
                    assert a3.getAvailabilityType() == DOWN : allAvails;
                    assert a0.getEndTime() != null : allAvails;
                    assert a1.getEndTime() != null : allAvails;
                    assert a2.getEndTime() != null : allAvails;
                    assert a3.getEndTime() == null : allAvails;
                    assert a0.getEndTime() > a0.getStartTime() : allAvails;
                    assert a0.getEndTime().equals(a1.getStartTime()) : allAvails;
                    assert a1.getEndTime().equals(a2.getStartTime()) : allAvails;
                    assert a2.getEndTime().equals(a3.getStartTime()) : allAvails;
                    break;
                case DISABLED:
                    assert a1.getAvailabilityType() == DISABLED : allAvails;
                    assert a0.getEndTime() != null : allAvails;
                    assert a1.getEndTime() == null : allAvails;
                    assert a0.getEndTime() > a0.getStartTime() : allAvails;
                    assert a0.getEndTime().equals(a1.getStartTime()) : allAvails;
                    break;
                default:
                    a2 = allAvails.get(2);
                    assert a1.getAvailabilityType() == UP : allAvails;
                    assert allAvails.get(2).getAvailabilityType() == UNKNOWN : allAvails;
                    assert a0.getEndTime() != null : allAvails;
                    assert a1.getEndTime() != null : allAvails;
                    assert a2.getEndTime() == null : allAvails;
                    assert a0.getEndTime() > a0.getStartTime() : allAvails;
                    assert a0.getEndTime().equals(a1.getStartTime()) : allAvails;
                    assert a1.getEndTime().equals(a2.getStartTime()) : allAvails;
                }
            }

            System.out.println("testAgentBackfillPerformance: checking validity of data took "
                + (System.currentTimeMillis() - start) + "ms");

            em = null;
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        } finally {
            unprepareForTestAgents();

            if (em != null) {
                getTransactionManager().rollback();
                em.close();
            }
        }
    }

    @Test(enabled = ENABLE_TESTS)
    public void testAgentOldReport() throws Exception {
        EntityManager em = beginTx();

        try {
            setupResource(em); // inserts initial UNKNOWN Availability at epoch
            commitAndClose(em);
            em = null;

            Availability avail;
            long now = System.currentTimeMillis();

            // add a report that says the resource is down
            avail = new Availability(theResource, DOWN);
            AvailabilityReport report = new AvailabilityReport(false, theAgent.getName());
            report.addAvailability(avail);
            availabilityManager.mergeAvailabilityReport(report);

            // now pretend the agent sent us a report from a previous time period - should insert this in the past
            avail = new Availability(theResource, (now - 600000), UP);
            report = new AvailabilityReport(false, theAgent.getName());
            report.addAvailability(avail);
            availabilityManager.mergeAvailabilityReport(report);
            assert getPointInTime(new Date(avail.getStartTime() - 2)) == UNKNOWN;
            assert getPointInTime(new Date(avail.getStartTime())) == UP;
            assert getPointInTime(new Date(avail.getStartTime() + 2)) == UP;

            // it's still down though - since we've received a more recent report saying it was down
            assert availabilityManager.getCurrentAvailabilityTypeForResource(overlord, theResource.getId()) == DOWN;

            // now pretend the agent sent us reports from inbetween our existing time periods
            // this UP record combines with the UP we added previously
            avail = new Availability(theResource, (now - 300000), UP);
            report = new AvailabilityReport(false, theAgent.getName());
            report.addAvailability(avail);
            availabilityManager.mergeAvailabilityReport(report);
            assert getPointInTime(new Date(avail.getStartTime() - 2)) == UP;
            assert getPointInTime(new Date(avail.getStartTime())) == UP;
            assert getPointInTime(new Date(avail.getStartTime() + 2)) == UP;

            // its still down though - since we've received a more recent report saying it was down
            assert availabilityManager.getCurrentAvailabilityTypeForResource(overlord, theResource.getId()) == DOWN;

            // this DOWN record combines with the current DOWN
            avail = new Availability(theResource, (now - 100000), DOWN);
            report = new AvailabilityReport(false, theAgent.getName());
            report.addAvailability(avail);
            availabilityManager.mergeAvailabilityReport(report);
            assert getPointInTime(new Date(avail.getStartTime() - 2)) == UP;
            assert getPointInTime(new Date(avail.getStartTime())) == DOWN;
            assert getPointInTime(new Date(avail.getStartTime() + 2)) == DOWN;

            // this DOWN record is between the two UPs we added earlier. However, because we are RLE,
            // we actually lost the information that we had an UP at both -60000 and -30000.  We just
            // have a RLE interval of UP starting at -60000.  This new DOWN record will add a new row
            // that will indicate we were only UP from -60000 to -45000 and DOWN thereafter.  This is
            // an odd test and probably will never occur in the wild (why would an agent tell us
            // we were one status in the past but another status further back in the past?)
            avail = new Availability(theResource, (now - 450000), DOWN);
            report = new AvailabilityReport(false, theAgent.getName());
            report.addAvailability(avail);
            availabilityManager.mergeAvailabilityReport(report);
            assert getPointInTime(new Date(avail.getStartTime() - 2)) == UP;
            assert getPointInTime(new Date(avail.getStartTime())) == DOWN;
            assert getPointInTime(new Date(avail.getStartTime() + 2)) == DOWN;

            // its still down
            assert availabilityManager.getCurrentAvailabilityTypeForResource(overlord, theResource.getId()) == DOWN;

            // let's insert one in the very beginning that is the same type as the current first interval
            avail = new Availability(theResource, (now - 700000), UP);
            report = new AvailabilityReport(false, theAgent.getName());
            report.addAvailability(avail);
            availabilityManager.mergeAvailabilityReport(report);
            assert getPointInTime(new Date(avail.getStartTime() - 2)) == UNKNOWN;
            assert getPointInTime(new Date(avail.getStartTime())) == UP;
            assert getPointInTime(new Date(avail.getStartTime() + 2)) == UP;
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        } finally {
            if (em != null) {
                getTransactionManager().rollback();
                em.close();
            }
        }
    }

    @Test(enabled = ENABLE_TESTS)
    public void testAgentOldReport2() throws Exception {
        EntityManager em = beginTx();

        try {
            setupResource(em);
            commitAndClose(em);
            em = null;

            long now = System.currentTimeMillis();

            // add a report that says the resource is down - reports can't have the same resource in it twice,
            // so just create a size=1 report multiple times
            persistAvailability(new Availability(theResource, (now - 1000000), UP));
            persistAvailability(new Availability(theResource, (now - 900000), DOWN));
            persistAvailability(new Availability(theResource, (now - 800000), UP));
            persistAvailability(new Availability(theResource, (now - 50000), DOWN));
            persistAvailability(new Availability(theResource, (now - 30000), UP));
            persistAvailability(new Availability(theResource, (now), DOWN));

            // now pretend the agent sent us a report from a previous time period - should insert this in the past
            persistAvailability(new Availability(theResource, (now - 600000), UP));

            // its still down though - since we've received a more recent report saying it was down
            assert availabilityManager.getCurrentAvailabilityTypeForResource(overlord, theResource.getId()) == DOWN;

            // now pretend the agent sent us reports from in between our existing time periods
            // this UP record combines with the UP we added previously
            persistAvailability(new Availability(theResource, (now - 300000), UP));

            // its still down though - since we've received a more recent report saying it was down
            assert availabilityManager.getCurrentAvailabilityTypeForResource(overlord, theResource.getId()) == DOWN;

            // this DOWN record combines with the current DOWN
            persistAvailability(new Availability(theResource, (now - 100000), DOWN));

            // this DOWN record is between the two UPs we added earlier. However, because we are RLE,
            // we actually lost the information that we had an UP at both -60000 and -30000.  We just
            // have a RLE interval of UP starting at -60000.  This new DOWN record will add a new row
            // that will indicate we were only UP fro -60000 to -45000 and DOWN thereafter.  This is
            // an odd test and probably will never occur in the wild (why would an agent tell us
            // we were one status in the past but another status further back in the past?)
            persistAvailability(new Availability(theResource, (now - 450000), DOWN));

            // its still down
            assert availabilityManager.getCurrentAvailabilityTypeForResource(overlord, theResource.getId()) == DOWN;

            // let's insert one in the very beginning that is the same type as the current first interval
            persistAvailability(new Availability(theResource, (now - 700000), UP));
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        } finally {
            if (em != null) {
                getTransactionManager().rollback();
                em.close();
            }
        }
    }

    @Test(enabled = ENABLE_TESTS)
    public void testGetAvailabilities2() throws Exception {
        EntityManager em = beginTx();

        try {
            Availability avail;

            setupResource(em);
            commitAndClose(em);
            em = null;

            Calendar cal = Calendar.getInstance();
            cal.set(Calendar.YEAR, 2000);
            cal.set(Calendar.MONTH, 0);
            cal.set(Calendar.DAY_OF_MONTH, 1);
            cal.set(Calendar.HOUR, 1);
            cal.set(Calendar.MINUTE, 0);
            cal.set(Calendar.SECOND, 0);
            cal.set(Calendar.MILLISECOND, 0);
            Date date1 = cal.getTime();

            cal.set(Calendar.HOUR, 1);
            cal.set(Calendar.MINUTE, 30);
            Date date2 = cal.getTime();

            cal.set(Calendar.HOUR, 2);
            cal.set(Calendar.MINUTE, 0);
            Date date3 = cal.getTime();

            cal.set(Calendar.HOUR, 2);
            cal.set(Calendar.MINUTE, 30);
            Date date4 = cal.getTime();

            cal.set(Calendar.HOUR, 3);
            cal.set(Calendar.MINUTE, 0);
            Date date5 = cal.getTime();

            cal.set(Calendar.HOUR, 3);
            cal.set(Calendar.MINUTE, 30);
            Date date6 = cal.getTime();

            avail = new Availability(theResource, date1.getTime(), UP);
            avail.setEndTime(date2.getTime());
            persistAvailability(avail);

            avail = new Availability(theResource, date2.getTime(), DOWN);
            avail.setEndTime(date3.getTime());
            persistAvailability(avail);

            avail = new Availability(theResource, date3.getTime(), UP);
            avail.setEndTime(date4.getTime());
            persistAvailability(avail);

            avail = new Availability(theResource, date4.getTime(), DOWN);
            avail.setEndTime(date5.getTime());
            persistAvailability(avail);

            avail = new Availability(theResource, date5.getTime(), UP);
            avail.setEndTime(date6.getTime());
            persistAvailability(avail);

            avail = new Availability(theResource, date6.getTime(), DOWN);
            persistAvailability(avail);

            List<AvailabilityPoint> points = availabilityManager.findAvailabilitiesForResource(overlord,
                theResource.getId(), date1.getTime(), date6.getTime(), 5, false);
            assert points.size() == 5;
            assert points.get(0).getAvailabilityType() == UP;
            assert points.get(1).getAvailabilityType() == DOWN;
            assert points.get(2).getAvailabilityType() == UP;
            assert points.get(3).getAvailabilityType() == DOWN;
            assert points.get(4).getAvailabilityType() == UP;
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        } finally {
            if (em != null) {
                getTransactionManager().rollback();
                em.close();
            }
        }
    }

    /**
     * See if merging in AvailabilityReports from the agent work.
     *
     * @throws Exception in case of error
     */
    @Test(enabled = ENABLE_TESTS)
    public void testMergeReport() throws Exception {
        EntityManager em = beginTx();

        try {
            Availability avail;
            AvailabilityReport report;

            setupResource(em);
            commitAndClose(em);
            em = null;

            long allAvailCount = setUpAvailabilities(em);

            // we now have 1:00 UP, 1:20 DOWN, 1:40 UP
            Subject overlord = LookupUtil.getSubjectManager().getOverlord();
            avail = availabilityManager.getCurrentAvailabilityForResource(overlord, theResource.getId());
            assert avail.getAvailabilityType() == UP;
            assert availabilityManager.getCurrentAvailabilityTypeForResource(overlord, theResource.getId()) == UP;

            // add something after the start of last, but still be UP (result: nothing added)
            Long currentStartTime = avail.getStartTime();
            avail = new Availability(theResource, (currentStartTime + 3600000), UP);
            report = new AvailabilityReport(false, theAgent.getName());
            report.addAvailability(avail);
            Thread.sleep(1000);
            availabilityManager.mergeAvailabilityReport(report);

            // the agent should have been updated, but no new rows in availability were added
            Agent agent = LookupUtil.getAgentManager().getAgentByName(theAgent.getName());
            Date lastReport = new Date(agent.getLastAvailabilityReport());
            assert lastReport != null;
            assert countAvailabilitiesInDB(null) == allAvailCount;
            avail = availabilityManager.getCurrentAvailabilityForResource(overlord, theResource.getId());

            // should have returned availability3
            // NOTE: availability3 never got an ID assigned, so we can't compare by id
            //       assert avail.getId() == availability3.getId();
            assert avail.getStartTime().equals(availability3.getStartTime());
            assert avail.getAvailabilityType() == availability3.getAvailabilityType();
            assert Math.abs(avail.getStartTime() - availability3.getStartTime()) < 1000;
            assert avail.getEndTime() == null;
            assert availability3.getEndTime() == null;

            // change start after the start of last (result: add new avail row)
            avail = new Availability(theResource, (currentStartTime + 7200000), DOWN);
            report = new AvailabilityReport(false, theAgent.getName());
            report.addAvailability(avail);
            Thread.sleep(1000);
            availabilityManager.mergeAvailabilityReport(report);

            // the agent should have been updated and a new row in availability was added (resource is now DOWN)
            agent = LookupUtil.getAgentManager().getAgentByName(theAgent.getName());
            assert new Date(agent.getLastAvailabilityReport()).after(lastReport);
            assert countAvailabilitiesInDB(null) == (allAvailCount + 1);
            assert availabilityManager.getCurrentAvailabilityTypeForResource(overlord, theResource.getId()) == DOWN;
            Availability queriedAvail = availabilityManager.getCurrentAvailabilityForResource(overlord,
                theResource.getId());
            assert queriedAvail.getId() > 0;
            assert queriedAvail.getAvailabilityType() == avail.getAvailabilityType();
            assert Math.abs(queriedAvail.getStartTime() - avail.getStartTime()) < 1000;
            assert queriedAvail.getEndTime() == null;
            assert avail.getEndTime() == null;
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        } finally {
            if (em != null) {
                getTransactionManager().rollback();
                em.close();
            }
        }
    }

    @Test(enabled = ENABLE_TESTS)
    public void testMergeReportPerformance() throws Exception {
        EntityManager em = beginTx();
        List<Resource> allResources = new ArrayList<Resource>();

        try {
            setupResource(em); // setup theResource

            allResources.add(theResource);

            // now create a bunch more resources
            for (int i = 0; i < 100; i++) {
                allResources.add(setupAnotherResource(em, i, theResource));
            }
            em.flush();

            commitAndClose(em);
            em = null;

            // add a report that says the resources are now up - the report will add one avail for each resource
            // at this point, the resources do not yet have a row in availability - after the merge they will have 1
            AvailabilityReport report = new AvailabilityReport(false, theAgent.getName());
            for (Resource resource : allResources) {
                Availability avail = new Availability(resource, UP);
                report.addAvailability(avail);
            }

            long start = System.currentTimeMillis();
            availabilityManager.mergeAvailabilityReport(report);

            System.out.println("testMergeReportPerformance: mergeAvailabilityReport run 1 took "
                + (System.currentTimeMillis() - start) + "ms");

            AvailabilityType curAvail;
            start = System.currentTimeMillis();

            for (Resource resource : allResources) {
                curAvail = availabilityManager.getCurrentAvailabilityTypeForResource(overlord, resource.getId());
                assert curAvail == UP : curAvail;
            }

            System.out.println("testMergeReportPerformance: checking validity of data 1 took "
                + (System.currentTimeMillis() - start) + "ms");

            // add a report that says the resources are now down - the report will add one avail for each resource
            // at this point, the resources have 1 row in availability - after the merge they will have 2
            report = new AvailabilityReport(false, theAgent.getName());
            for (Resource resource : allResources) {
                Availability avail = new Availability(resource, DOWN);
                report.addAvailability(avail);
            }

            start = System.currentTimeMillis();
            availabilityManager.mergeAvailabilityReport(report);

            System.out.println("testMergeReportPerformance: mergeAvailabilityReport run 2 took "
                + (System.currentTimeMillis() - start) + "ms");

            start = System.currentTimeMillis();

            for (Resource resource : allResources) {
                curAvail = availabilityManager.getCurrentAvailabilityTypeForResource(overlord, resource.getId());
                assert curAvail == DOWN : curAvail;
            }

            System.out.println("testMergeReportPerformance: checking validity of data 2 took "
                + (System.currentTimeMillis() - start) + "ms");

            // add a report that says the resources are now unknown - the report will add one avail for each resource
            // at this point, the resources have 2 rows in availability - after the merge they will have 3
            report = new AvailabilityReport(false, theAgent.getName());
            for (Resource resource : allResources) {
                Availability avail = new Availability(resource, UNKNOWN);
                report.addAvailability(avail);
            }

            start = System.currentTimeMillis();
            availabilityManager.mergeAvailabilityReport(report);

            System.out.println("testMergeReportPerformance: mergeAvailabilityReport run 3 took "
                + (System.currentTimeMillis() - start) + "ms");

            start = System.currentTimeMillis();

            for (Resource resource : allResources) {
                curAvail = availabilityManager.getCurrentAvailabilityTypeForResource(overlord, resource.getId());
                assert curAvail == UNKNOWN : curAvail;
            }

            System.out.println("testMergeReportPerformance: checking validity of data 3 took "
                + (System.currentTimeMillis() - start) + "ms");

            em = null;
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        } finally {
            if (em != null) {
                getTransactionManager().rollback();
                em.close();
            }
        }
    }

    private EntityManager beginTx() throws Exception {
        getTransactionManager().begin();
        EntityManager em = getEntityManager();
        return em;
    }

    private void commitAndClose(EntityManager em) throws Exception {
        getTransactionManager().commit();
        em.close();
    }

    /**
     * @param time The point in time will span 1 millisecond from the time provided to time+1
     * @return
     */
    private AvailabilityType getPointInTime(Date time) {
        List<AvailabilityPoint> list = availabilityManager.findAvailabilitiesForResource(overlord, theResource.getId(),
            time.getTime(), time.getTime() + 1, 1, false);
        assert list != null;
        assert list.size() == 1 : "Should have returned a single point";
        AvailabilityType type = list.get(0).getAvailabilityType();

        switch (type) {
        case UP:
        case DOWN:
        case DISABLED:
        case UNKNOWN:
            return type;

        default:
            assert false : "AvailabilityType enum has some additional values not known to this test: " + type;
        }

        return null;
    }

    /**
     * See how many rows we have in the availability table
     *
     * @param  em EntityManager to use
     *
     * @return the rowcount
     *
     * @throws Exception
     */
    private long countAvailabilitiesInDB(EntityManager em) throws Exception {
        TransactionManager tx = null;

        if (em == null) {
            tx = getTransactionManager();
            tx.begin();
            em = getEntityManager();
        }

        try {
            Query q = em.createQuery("SELECT count(a) FROM Availability a");
            long count = (Long) q.getSingleResult();
            return count;
        } finally {
            if (tx != null) {
                tx.rollback();
                em.close();
            }
        }
    }

    /**
     * Just set up a resource where we can attach the availabilities to
     *
     * @param  em The EntityManager to use
     *
     * @return A Resource ready to use
     */
    private Resource setupResource(EntityManager em) {
        theAgent = new Agent("testagent", "localhost", 1234, "", "randomToken");
        em.persist(theAgent);

        theResourceType = new ResourceType("test-plat", "test-plugin", ResourceCategory.PLATFORM, null);
        em.persist(theResourceType);

        theResource = new Resource("test-platform-key", "test-platform-name", theResourceType);
        theResource.setUuid("" + new Random().nextInt());
        theResource.setAgent(theAgent);
        em.persist(theResource);

        em.flush();
        return theResource;
    }

    /**
     * Set up another unique resource that will be related to <code>theAgent</code>. The resource will be of type <code>
     * theResourceType</code>.
     *
     * @param  em           The EntityManager to use
     * @param  uniqueNumber used to define a unique key for the resource
     *
     * @return A Resource ready to use
     */
    private Resource setupAnotherResource(EntityManager em, int uniqueNumber, Resource parentResource) {
        Resource newResource;

        newResource = new Resource("test-platform-key-" + uniqueNumber, "test-platform-name-" + uniqueNumber,
            theResourceType);
        newResource.setUuid("" + new Random().nextInt());
        newResource.setAgent(theAgent);
        parentResource.addChildResource(newResource);
        em.persist(newResource);
        additionalResources.add(newResource);

        return newResource;
    }

    /**
     * Set up an availability scenario where we set up availability for one hour, split it in the middle and have 20min
     * up, 20min down, 20min up starting at 1:00am.
     *
     * @param  em An EntityManager to use
     *
     * @return total number of availability records in the DB after we've added ours
     *
     * @throws Exception
     */
    private long setUpAvailabilities(EntityManager em) throws Exception {
        Calendar cal = Calendar.getInstance();
        cal.setTime(new Date());
        cal.set(Calendar.HOUR, 1);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        Date start = cal.getTime();

        // split time
        cal.set(Calendar.HOUR, 1);
        cal.set(Calendar.MINUTE, 20);
        Date splitStart = cal.getTime();
        cal.set(Calendar.MINUTE, 40);
        Date splitEnd = cal.getTime();

        long count = countAvailabilitiesInDB(em);

        availability1 = new Availability(theResource, start.getTime(), UP);
        availability1.setEndTime(splitStart.getTime());
        persistAvailability(availability1);

        availability2 = new Availability(theResource, splitStart.getTime(), DOWN);
        availability2.setEndTime(splitEnd.getTime());
        persistAvailability(availability2);

        availability3 = new Availability(theResource, splitEnd.getTime(), UP);
        persistAvailability(availability3);

        long countNow = countAvailabilitiesInDB(em);

        assert countNow == (count + 3) : "Did not find three availabilities - instead found: " + countNow;

        return countNow;
    }

    /**
     * Convenience method for persisting availability.  Availability data can no longer be directly merged
     * by the EntityManager because it does not update the corresponding currentAvailability data on the
     * Resource entity.  This method will update the necessary objects for you.
     */
    private void persistAvailability(Availability... availabilities) {
        AvailabilityReport report = new AvailabilityReport(false, theAgent.getName());
        for (Availability avail : availabilities) {
            report.addAvailability(avail);
        }
        availabilityManager.mergeAvailabilityReport(report);
    }
}