/*
 * RHQ Management Platform
 * Copyright (C) 2005-2009 Red Hat, Inc.
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

package org.rhq.plugins.apache.util;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;

import org.rhq.augeas.node.AugeasNode;
import org.rhq.augeas.tree.AugeasTree;

/**
 * Utility class to extract various HTTP addresses from Augeas loaded Apache configuration.
 * 
 * @author Lukas Krejci
 */
public class HttpdAddressUtility {

    private HttpdAddressUtility() {
        
    }
    
    public static class Address {
        public String host;
        public int port;
        
        public Address(String host, int port) {
            this.host = host;
            this.port = port;
        }
        
        @Override
        public int hashCode() {
            int hash = port;
            if (host != null) hash *= host.hashCode();
            return hash;
        }
        
        @Override
        public boolean equals(Object other) {
            if (!(other instanceof Address)) return false;
            
            Address o = (Address)other;
            
            if (this.host == null) {
                return o.host == null && this.port == o.port;
            } else {
                return this.host.equals(o.host) && this.port == o.port;
            }
        }
    }    
    
    /**
     * This just constructs a first available address under which the server or one of its virtual hosts can be reached.
     * 
     * @param ag the tree of the httpd configuration
     * @return the address
     */
    public static Address getMainServerSampleAddress(AugeasTree ag) {
        //there has to be at least one Listen directive
        AugeasNode listen = ag.matchRelative(ag.getRootNode(), "Listen").get(0);
        
        List<AugeasNode> params = listen.getChildByLabel("param");
        String address = params.get(0).getValue();
        
        String host = null;
        String port = null;
        if (address.startsWith("[")) {
            int bracketIdx = address.indexOf(']');
            host = address.substring(1, bracketIdx);
            port = address.substring(bracketIdx + 2);
        } else {
            String[] hostPort = address.split(":");
            if (hostPort.length == 1) {
                port = hostPort[0];
            } else {
                host = hostPort[0];
                port = hostPort[1];
            }
        }
        if (host == null) {
            try {
                host = InetAddress.getLocalHost().getHostAddress();
            } catch (UnknownHostException e) {
                throw new IllegalStateException("Unable to get the localhost address.", e);
            }
        }
        return new Address(host, Integer.parseInt(port));
    }    
    
    /**
     * This constructs an address on which given virtual host can be accessed.
     * 
     * @param ag the augeas tree of the httpd configuration
     * @param virtualHost the port or address:port of the virtual host
     * @param serverName the server name for the namebased virtual hosts (or null if the virtual host is ip based)
     * 
     * @return the address on which the virtual host can be accessed.
     */
    public static Address getVirtualHostSampleAddress(AugeasTree ag, String virtualHost, String serverName) {
        Address addr = new Address(null, -1);
        if (virtualHost.startsWith("[")) {
            //TODO IPv6 address
            return null;
        } else {
            String[] hostPort = virtualHost.split(":");
            if (hostPort.length == 1) {
                //just address specified
                Address serverAddr = getMainServerSampleAddress(ag);
                
                addr.port = Integer.parseInt(hostPort[0]);
                addr.host = serverAddr.host;
            } else {
                String host = hostPort[0];
                if ("*".equals(host)) {
                    host = getMainServerSampleAddress(ag).host;
                }
                String port = hostPort[1];
                addr.host = host;
                addr.port = Integer.parseInt(port);
            }
        }
        
        if (serverName != null) {
            addr.host = serverName.replaceAll("\\*", "replaced-wildcard");
        }
        
        return addr;
    }    
}