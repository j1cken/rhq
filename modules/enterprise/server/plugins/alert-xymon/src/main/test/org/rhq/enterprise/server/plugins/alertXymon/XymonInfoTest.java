package org.rhq.enterprise.server.plugins.alertXymon;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Created by IntelliJ IDEA.
 * User: torben
 * Date: 12/10/10
 * Time: 5:14 PM
 * To change this template use File | Settings | File Templates.
 */
public class XymonInfoTest {
    XymonInfo groupInfo;
    XymonInfo info;
    XymonInfo missingBin;
    XymonInfo missingHost;
    XymonInfo missingReq;
    XymonInfo emptyPrefix;
    XymonInfo nullPrefix;

    @Before
    public void setUp() throws Exception {
        groupInfo = new XymonInfo("/usr/local/bin/bb", "myHost", "jboss", true, "30m");
        info = new XymonInfo("/usr/local/bin/bb", "myHost", "jboss", false, "30m");
        missingBin = new XymonInfo(null, "myHost", "jboss", true, "30m");
        missingHost = new XymonInfo("bb", null, "jboss", true, "30m");
        missingReq = new XymonInfo(null, null, "jboss", true, "30m");
        emptyPrefix = new XymonInfo(null, null, "", true, "30m");
        nullPrefix = new XymonInfo(null, null, "", true, "30m");
    }

    @Test
    public void testErrors() {
        assertEquals("getErrors() does not return null.", null, groupInfo.getErrors());
        assertEquals("getErrors() does not return the correct error msg.", "Missing: Binary", missingBin.getErrors());
        assertEquals("getErrors() does not return the correct error msg.", "Missing: Host", missingHost.getErrors());
        assertEquals("getErrors() does not return the correct error msg.", "Missing: Binary, Host", missingReq.getErrors());
    }

    @Test
    public void testMerge() {
        assertEquals("wrong merge!", groupInfo, XymonInfo.merge(
              groupInfo,
                new XymonInfo(null, "", null, true, "30m")));
        assertEquals("wrong merge!", new XymonInfo("/usr/local/bin/bb", "myHost", "jboss", false, "30m"), XymonInfo.merge(
              groupInfo,
                new XymonInfo(null, null, null, false, "30m")));
        assertEquals("wrong merge!", new XymonInfo("foo", "bar", "foobar", false, "30m"), XymonInfo.merge(
              groupInfo,
                new XymonInfo("foo", "bar", "foobar", false, "30m")));
        assertEquals("wrong merge!", new XymonInfo("foo", "bar", "foobar", true, "30m"), XymonInfo.merge(
              info,
                new XymonInfo("foo", "bar", "foobar", true, "30m")));
        assertEquals("wrong merge!", new XymonInfo("foo", "bar", "foobar", false, "30m"), XymonInfo.merge(
              info,
                new XymonInfo("foo", "bar", "foobar", false, "30m")));
        assertEquals("wrong merge!", new XymonInfo("foo", "bar", "foobar", false, "1d"), XymonInfo.merge(
              info,
                new XymonInfo("foo", "bar", "foobar", false, "1d")));
    }

    @Test
    public void testPrefix() {
        assertEquals("wrong null prefix!", "", emptyPrefix.getPrefix() );
        assertEquals("wrong empty prefix!", "", nullPrefix.getPrefix());
        assertNotNull("return value is null!", nullPrefix.getPrefix());
        assertEquals("wrong provided prefix!", "jboss-", groupInfo.getPrefix());
    }
}
