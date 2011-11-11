/*
 * RHQ Management Platform
 * Copyright (C) 2005-2009 Red Hat, Inc.
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
package org.rhq.enterprise.server.plugins.alertXymon;

import java.io.IOException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.rhq.core.domain.alert.Alert;
import org.rhq.core.domain.alert.notification.SenderResult;
import org.rhq.enterprise.server.plugin.pc.alert.AlertSender;

/**
 * Class to send notifications to a Xymon monitoring server.
 *
 * @author <mailto:torben@redhat.com>Torben Jaeger</mailto>
 */
public class XymonSender extends AlertSender {

   private static final String PIPE = "|";
   private static final String PIPE_ESCAPED = "\\|";
   private static final String REGEX_IP = "\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}";
   private final Log log = LogFactory.getLog(XymonSender.class);

   private static final String SPACE = " ";

   @Override
   public SenderResult send(Alert alert) {

      XymonInfo xymonInfo = getXymonInfo();

      if (xymonInfo.error != null) {
         return SenderResult.getSimpleFailure(xymonInfo.error);
      }

      try {
         return sendMessage(xymonInfo, alert);

      } catch (IOException e) {
         return SenderResult.getSimpleFailure(new StringBuilder().append("IOException invoking Xymon binary: ").append(e.getMessage()).toString());
      } catch (InterruptedException e) {
         return SenderResult.getSimpleFailure(new StringBuilder().append("InterruptedException invoking Xymon binary: ").append(e.getMessage()).toString());
      }
   }

   private SenderResult sendMessage(XymonInfo xymonInfo, Alert alert) throws IOException, InterruptedException {

      SenderResult senderResult = new SenderResult();
      String[] hosts;
      if (xymonInfo.getHost().contains(PIPE)) {
         hosts = xymonInfo.getHost().split(PIPE_ESCAPED);
      } else {
         hosts = new String[]{xymonInfo.getHost()};
      }
      for (int i = 0; i < hosts.length; i++) {

         xymonInfo.setHost(hosts[i]);

         if (log.isInfoEnabled()) {
            log.info("Sending Xymon Alert to: " + xymonInfo.getHost());
         }

         String bbMessage = getBBMessage(xymonInfo, alert);

         if (log.isDebugEnabled()) {
            log.debug(new StringBuilder().append("Executing ")
                            .append(xymonInfo.getBinary())
                            .append(SPACE)
                            .append(xymonInfo.getHost())
                            .append(SPACE)
                            .append(bbMessage).toString());
         }


         Process proc = new ProcessBuilder(xymonInfo.getBinary(), xymonInfo.getHost(), bbMessage).start();
         if (proc.waitFor() != 0) {
            senderResult.addFailureMessage(new StringBuilder().append("Invoking Xymon binary exited with status ").
                  append(String.valueOf(proc.exitValue())).toString());
         } else {
            senderResult.addSuccessMessage(new StringBuilder().append("Status message sent to ")
                                                 .append(xymonInfo.getHost())
                                                 .append(": ")
                                                 .append(bbMessage).toString());
         }
         try {
            proc.getInputStream().close();
            proc.getOutputStream().close();
            proc.getErrorStream().close();
         } catch (Exception e) {
            log.debug("Exception while cleaning up resources: " + e);
         } finally {
            proc.destroy();
         }
      }
      return senderResult;
   }

   private XymonInfo getXymonInfo() {
      // 1st load global, then 2nd per Alert
      XymonInfo xymonInfo1 = XymonInfo.load(preferences);
      XymonInfo xymonInfo2 = XymonInfo.load(alertParameters);
      return XymonInfo.merge(xymonInfo1, xymonInfo2);
   }

   private String getBBMessage(XymonInfo info, Alert alert) {
      /*
     /usr/local/bb/bin/bb 10.207.193.38 "status jon01.jboss-Server green Thu Nov 18 04:05:55 2010 jon01
     <table summary='Status summary' border=1>
     <tr>
         <td valign=top>&green</td>
         <td valign=top>Thu Nov 18 04:05:55 2010</td>
         <td>JON Server Availability<br/>
         Resource: <a href='https://10.204.194.33:9443/rhq/resource/summary/overview.xhtml?id=0'>null</a></td>
     </tr></table>"
      */
      StatusMessage statusMessage = StatusMessageFactory.getInstance(info, alert);

      StringBuilder builder = new StringBuilder();
      builder.append("status");
      builder.append("+");
      builder.append(info.getLifetime());
      builder.append(SPACE);
      builder.append(statusMessage.getHostname());
      builder.append(info.getHostSuffix());
      builder.append(".");
      builder.append(info.getPrefix());
      builder.append(statusMessage.getTestname().
            replaceAll(REGEX_IP, "host").
            replaceAll(" ", "_").
            replaceAll("\\.", "_"));
      builder.append(SPACE);
      builder.append(statusMessage.getColor());
      builder.append(SPACE);
      builder.append(statusMessage.getDate());
      builder.append(SPACE);
      builder.append(statusMessage.getTestname());
      builder.append(SPACE);
      builder.append(statusMessage.getDetails());
      return builder.toString();
   }

}
