/*
 * RHQ Management Platform
 * Copyright (C) 2005-2008 Red Hat, Inc.
 * All rights reserved.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2, as
 * published by the Free Software Foundation, and/or the GNU Lesser
 * General Public License, version 2.1, also as published by the Free
 * Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License and the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU General Public License
 * and the GNU Lesser General Public License along with this program;
 * if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 */
package org.rhq.core.domain.resource.test;

import java.util.List;
import java.util.Random;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import javax.transaction.HeuristicMixedException;
import javax.transaction.HeuristicRollbackException;
import javax.transaction.NotSupportedException;
import javax.transaction.RollbackException;
import javax.transaction.SystemException;

import org.testng.annotations.Test;

import org.rhq.core.domain.alert.Alert;
import org.rhq.core.domain.alert.AlertDampening;
import org.rhq.core.domain.alert.AlertDampening.Category;
import org.rhq.core.domain.alert.AlertDefinition;
import org.rhq.core.domain.alert.AlertPriority;
import org.rhq.core.domain.alert.BooleanExpression;
import org.rhq.core.domain.measurement.Availability;
import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.domain.measurement.MeasurementDefinition;
import org.rhq.core.domain.measurement.MeasurementSchedule;
import org.rhq.core.domain.measurement.NumericType;
import org.rhq.core.domain.measurement.ResourceAvailability;
import org.rhq.core.domain.resource.InventoryStatus;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceCategory;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.domain.resource.composite.ProblemResourceComposite;
import org.rhq.core.domain.test.AbstractEJB3Test;

@Test(groups = "integration.ejb3")
public class ProblemResourceTest extends AbstractEJB3Test {
    private long now;
    private long fiveMinutesAgo;
    private ResourceType platformType;
    private Resource platform;
    private Resource platform2;
    private MeasurementDefinition measDef;
    private MeasurementSchedule measSched;
    private MeasurementSchedule measSched2;
    private AlertDefinition alertDef;
    private AlertDefinition alertDef2;
    private EntityManager entityManager;

    private List<ProblemResourceComposite> results;

    public void testProblemResourcesAlert() throws Throwable {
        getTransactionManager().begin();
        entityManager = getEntityManager();
        try {
            setupResources(entityManager);
            assert entityManager.find(Resource.class, platform.getId()) != null : "Did not setup platform - cannot test";

            assertResults(entityManager, fiveMinutesAgo, 0);
            assertCount(entityManager, fiveMinutesAgo, 0);

            Alert alert = new Alert(alertDef, now - 10000);
            entityManager.persist(alert);

            //commitAndBegin();
            results = assertResults(entityManager, fiveMinutesAgo, 1);
            assertCount(entityManager, fiveMinutesAgo, 1);
            assertComposite(results.get(0), platform, 1);

            Alert alert2 = new Alert(alertDef2, now - 5000);
            entityManager.persist(alert2);

            //commitAndBegin();
            results = assertResults(entityManager, fiveMinutesAgo, 2);
            assertCount(entityManager, fiveMinutesAgo, 2);
            int platform1Index = (results.get(0).getResourceId() == platform.getId()) ? 0 : 1;
            int platform2Index = 1 - platform1Index;
            assertComposite(results.get(platform1Index), platform, 1);
            assertComposite(results.get(platform2Index), platform2, 1);

            Alert alert3 = new Alert(alertDef2, now);
            entityManager.persist(alert3);

            //commitAndBegin();
            results = assertResults(entityManager, fiveMinutesAgo, 2);
            assertCount(entityManager, fiveMinutesAgo, 2);
            platform1Index = (results.get(0).getResourceId() == platform.getId()) ? 0 : 1;
            platform2Index = 1 - platform1Index;
            assertComposite(results.get(platform1Index), platform, 1);
            assertComposite(results.get(platform2Index), platform2, 2);

            deleteResources();
        } catch (Throwable t) {
            dumpResults("testProblemResourcesAlert");
            throw t;
        } finally {
            getTransactionManager().rollback();
        }
    }

    /**
     * The Alert resource queries will also return resources that are currently down.
     * 
     * @throws Throwable
     */
    public void testProblemResourcesAvailability() throws Throwable {
        getTransactionManager().begin();
        entityManager = getEntityManager();
        try {
            setupResources(entityManager);
            assert entityManager.find(Resource.class, platform.getId()) != null : "Did not setup platform - cannot test";

            assertResults(entityManager, fiveMinutesAgo, 0);
            assertCount(entityManager, fiveMinutesAgo, 0);

            ResourceAvailability resourceAvail = new ResourceAvailability(platform, AvailabilityType.DOWN);
            entityManager.persist(resourceAvail);
            Availability avail = new Availability(platform, AvailabilityType.DOWN);
            entityManager.persist(avail);

            //commitAndBegin();
            results = assertResults(entityManager, fiveMinutesAgo, 1);
            assertCount(entityManager, fiveMinutesAgo, 1);
            assertComposite(results.get(0), platform, 0);
            assert results.get(0).getAvailabilityType() == AvailabilityType.DOWN;
            assertComposite(results.get(0), platform, 0);
            assert results.get(0).getAvailabilityType() == AvailabilityType.DOWN;

            ResourceAvailability resourceAvail2 = new ResourceAvailability(platform2, AvailabilityType.DOWN);
            entityManager.persist(resourceAvail2);
            Availability avail2 = new Availability(platform2, null, AvailabilityType.DOWN);
            entityManager.persist(avail2);

            //commitAndBegin();
            results = assertResults(entityManager, fiveMinutesAgo, 2);
            assertCount(entityManager, fiveMinutesAgo, 2);
            int platform1Index = (results.get(0).getResourceId() == platform.getId()) ? 0 : 1;
            int platform2Index = 1 - platform1Index;
            assertComposite(results.get(platform1Index), platform, 0);
            assert results.get(platform1Index).getAvailabilityType() == AvailabilityType.DOWN;
            assertComposite(results.get(platform2Index), platform2, 0);
            assert results.get(platform2Index).getAvailabilityType() == AvailabilityType.DOWN;

            platform1Index = (results.get(0).getResourceId() == platform.getId()) ? 0 : 1;
            platform2Index = 1 - platform1Index;
            assertComposite(results.get(platform1Index), platform, 0);
            assert results.get(platform1Index).getAvailabilityType() == AvailabilityType.DOWN;
            assertComposite(results.get(platform2Index), platform2, 0);
            assert results.get(platform2Index).getAvailabilityType() == AvailabilityType.DOWN;

            deleteResources();
        } catch (Throwable t) {
            dumpResults("testProblemResourcesAvailability");
            throw t;
        } finally {
            getTransactionManager().rollback();
        }
    }

    @SuppressWarnings("unchecked")
    private List<ProblemResourceComposite> assertResults(EntityManager em, long oldest, long expectedSize) {
        Query q = em.createNamedQuery(Resource.QUERY_FIND_PROBLEM_RESOURCES_ALERT_ADMIN);
        q.setParameter("oldest", oldest);
        List<ProblemResourceComposite> resultList = q.getResultList();
        assert resultList.size() == expectedSize : "List was to be size=" + expectedSize + " but was " + resultList;
        return resultList;
    }

    private int assertCount(EntityManager em, long oldest, long expectedCount) {
        Query q = em.createNamedQuery(Resource.QUERY_FIND_PROBLEM_RESOURCES_ALERT_COUNT_ADMIN);
        q.setParameter("oldest", oldest);
        int count = ((Number) q.getSingleResult()).intValue();
        assert count == expectedCount : "Expected the count to be " + expectedCount + " but was " + count;
        return count;
    }

    private void assertComposite(ProblemResourceComposite composite, Resource plat, int alertCount) {
        assert composite.getResourceId() == plat.getId() : "Expected id " + plat.getId() + ", got "
            + composite.getResourceId();
        assert composite.getResourceName().equals(plat.getName()) : "Expected name " + plat.getName() + ", got "
            + composite.getResourceName();
        assert composite.getNumAlerts() == alertCount : "Expected " + alertCount + " alerts, got "
            + composite.getNumAlerts();
    }

    private void setupResources(EntityManager em) {
        now = System.currentTimeMillis();
        fiveMinutesAgo = now - (5 * 60 * 1000);

        platformType = new ResourceType("testplatPR", "p", ResourceCategory.PLATFORM, null);
        em.persist(platformType);
        platform = new Resource("platform1", "testProblemResources Platform One", platformType);
        platform.setUuid("" + new Random().nextInt());
        platform.setInventoryStatus(InventoryStatus.COMMITTED);
        em.persist(platform);
        platform2 = new Resource("platform2", "testProblemResources Platform Two", platformType);
        platform2.setUuid("" + new Random().nextInt());
        platform2.setInventoryStatus(InventoryStatus.COMMITTED);
        em.persist(platform2);

        measDef = new MeasurementDefinition(platformType, "testProblemResourcesMeasurement");
        measDef.setDefaultOn(true);
        measDef.setDisplayName("testProblemResources Measurement Display Name");
        measDef.setMeasurementType(NumericType.DYNAMIC);
        em.persist(measDef);

        measSched = new MeasurementSchedule(measDef, platform);
        measSched.setEnabled(true);
        platform.addSchedule(measSched);
        measDef.addSchedule(measSched);
        em.persist(measSched);

        measSched2 = new MeasurementSchedule(measDef, platform2);
        measSched2.setEnabled(true);
        platform2.addSchedule(measSched2);
        measDef.addSchedule(measSched2);
        em.persist(measSched2);

        alertDef = new AlertDefinition();
        alertDef.setName("PRAlertDef1");
        alertDef.setResource(platform);
        alertDef.setPriority(AlertPriority.MEDIUM);
        alertDef.setAlertDampening(new AlertDampening(Category.NONE));
        alertDef.setConditionExpression(BooleanExpression.ALL);
        alertDef.setRecoveryId(0);
        em.persist(alertDef);

        alertDef2 = new AlertDefinition();
        alertDef2.setName("PRAlertDef2");
        alertDef2.setResource(platform2);
        alertDef2.setPriority(AlertPriority.MEDIUM);
        alertDef2.setAlertDampening(new AlertDampening(Category.NONE));
        alertDef2.setConditionExpression(BooleanExpression.ALL);
        alertDef2.setRecoveryId(0);
        em.persist(alertDef2);
    }

    /**
     * Use this mainly when debugging and using {@link #commitAndBegin()} - this will help clean out things.
     */
    private void deleteResources() {
        Object doomed;

        try {
            if ((doomed = entityManager.find(AlertDefinition.class, alertDef.getId())) != null) {
                entityManager.remove(doomed);
            }

            if ((doomed = entityManager.find(AlertDefinition.class, alertDef2.getId())) != null) {
                entityManager.remove(doomed);
            }

            if ((doomed = entityManager.find(MeasurementSchedule.class, measSched.getId())) != null) {
                entityManager.remove(doomed);
            }

            if ((doomed = entityManager.find(MeasurementSchedule.class, measSched2.getId())) != null) {
                entityManager.remove(doomed);
            }

            if ((doomed = entityManager.find(MeasurementDefinition.class, measDef.getId())) != null) {
                entityManager.remove(doomed);
            }

            if ((doomed = entityManager.find(Resource.class, platform.getId())) != null) {
                entityManager.remove(doomed);
            }

            if ((doomed = entityManager.find(Resource.class, platform2.getId())) != null) {
                entityManager.remove(doomed);
            }

            if ((doomed = entityManager.find(ResourceType.class, platformType.getId())) != null) {
                entityManager.remove(doomed);
            }
        } catch (Exception e) {
            System.out.println("Cannot delete test resources, database still has test data in it");
            e.printStackTrace();
        }
    }

    private void dumpResults(String msg) {
        //      System.out.println( "-----" + msg + "-----" );
        //      if ( results != null )
        //      {
        //         System.out.println( "LAST [" + results.size() + "] RESULTS WERE:" );
        //         for ( ProblemResourceComposite problem : results )
        //         {
        //            System.out.println( "PROBLEM-->" + problem );
        //            Resource r = entityManager.find( Resource.class, problem.getResourceId() );
        //            if ( r == null )
        //            {
        //               continue;
        //            }
        //
        //            System.out.println( "RESOURCE--> name=" + r.getName() );
        //            System.out.println( "RESOURCE--> availability=" + r.getAvailability() );
        //            if ( r.getAlertDefinitions() != null )
        //            {
        //               for ( AlertDefinition def : r.getAlertDefinitions() )
        //               {
        //                  System.out.println( "RESOURCE--> alerts: " + def.getAlerts() );
        //               }
        //            }
        //         }
        //      }
        //      else
        //      {
        //         System.out.println( "NO RESULTS AVAILABLE" );
        //      }
        printHierarchies();
    }

    // commitAndBegin, commit, begin are used mainly when testing in a debugger
    // so you can commit the data, and examine it in an external SQL tool without
    // worrying about transactions timing out within the paused test thread.
    private void commitAndBegin() throws Exception {
        commit();
        begin(); // put a breakpoint here and call commitAndBegin at a place where you want to pause the test
    }

    private void commit() throws RollbackException, HeuristicMixedException, HeuristicRollbackException,
        SystemException {
        entityManager.flush();
        getTransactionManager().commit();
        entityManager.close();
    }

    private void begin() throws NotSupportedException, SystemException {
        getTransactionManager().begin();
        entityManager = getEntityManager();
    }

    private void printHierarchies() {
        System.out.println("---hierarchy START---");
        printPlatform(platform);
        printPlatform(platform2);
        System.out.println("---hierarchy END---");
    }

    private void printPlatform(Resource plat) {
        plat = entityManager.merge(plat);
        System.out.println(plat);
        for (AlertDefinition def : plat.getAlertDefinitions()) {
            System.out.println("   " + def);
            for (Alert a : def.getAlerts()) {
                System.out.println("      " + a);
            }
        }
    }
}