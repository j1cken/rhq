<%@ page contentType="text/html" %>

<%@ taglib uri="http://java.sun.com/jsf/html" prefix="h" %>
<%@ taglib uri="http://java.sun.com/jsf/core" prefix="f" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>


<f:view>
<f:loadBundle var="bundle" basename="InstallerMessages" />

<html>
    <head>
      <script type="text/javascript">
         function hideDiv( _levelId) {
            var thisDiv = document.getElementById( _levelId );
            thisDiv.style.display = "none";
         }

         function showDiv( _levelId) {
            var thisDiv = document.getElementById( _levelId );
            thisDiv.style.display = "block";
          }
	 function checkForPassword() {
            if ( document.getElementById('propForm:databasepassword').value  == '' ) {
               alert("Please enter database password.");
               return false;
            }
            return true;
         }
      </script>
   </head>

   <body>
   <f:subview id="header">
      <jsp:include page="/header.jsp" flush="true" />
   </f:subview>

   <p align="left">
      <h:outputText value="#{bundle.setPropertiesInstructions}" />
   </p>

   <h:form>
      <p align="left">
         <h4 align="left">
            <h:outputText value="#{bundle.advancedSettingsInstructions}" />
         </h4>
         <h:selectBooleanCheckbox onclick="submit()"
                                  id="showAdvancedSettings"
                                  value="#{configurationBean.showAdvancedSettings}" />
         <h:outputLabel for="showAdvancedSettings"
                        value="#{bundle.showAdvancedSettings}" />
      </p>
   </h:form>

   <h:form id="propForm">
      <br/>
      <h4 align="left">
         <h:outputText value="#{bundle.databaseSettingsInstructions}" />
      </h4>
      <div id="experimentalDbMsg" style="display: none;">
        <table>
            <tr>
                <td><img alt="warning" src="images/warning.gif"/></td>
                <td><h:outputText value="#{bundle.experimentalDb}" styleClass="javascriptWarning"/></td>
            </tr>
        </table>

      </div>

      <h:panelGrid columns="1">
         <h:panelGrid columns="2"
                      rowClasses="evenRow,oddRow">
            <h:outputLink value="javascript:popUp('#{bundle.helpDocRHQServerPropParentPage}#{configurationBean.databaseConfiguration[0].itemDefinition.help}', 'propertyHelp')">
               <h:outputText value="#{configurationBean.databaseConfiguration[0].itemDefinition.propertyLabel}" />
            </h:outputLink>
            <h:selectOneMenu id="databasetype"
                             value="#{configurationBean.databaseConfiguration[0].value}"
                             onchange="document.getElementById('propForm:databaseusername').value = 'rhqadmin';
                                       document.getElementById('propForm:databasepassword').value = 'rhqadmin';

                                       if (this.options[this.selectedIndex].value == 'PostgreSQL'){
                                          document.getElementById('propForm:databaseconnectionurl').value = 'jdbc:postgresql://127.0.0.1:5432/rhq';
                                          document.getElementById('propForm:databasedriverclass').value = 'org.postgresql.Driver';
                                          document.getElementById('propForm:databasexadatasourceclass').value = 'org.postgresql.xa.PGXADataSource';
                                          hideDiv('experimentalDbMsg');
                                       } else if (this.options[this.selectedIndex].value == 'Oracle') {
                                          document.getElementById('propForm:databaseconnectionurl').value = 'jdbc:oracle:thin:@127.0.0.1:1521:rhq';
                                          document.getElementById('propForm:databasedriverclass').value = 'oracle.jdbc.driver.OracleDriver';
                                          document.getElementById('propForm:databasexadatasourceclass').value = 'oracle.jdbc.xa.client.OracleXADataSource';
                                          hideDiv('experimentalDbMsg');
                                       } else if (this.options[this.selectedIndex].value == 'H2') {
                                          document.getElementById('propForm:databaseconnectionurl').value = 'jdbc:h2:#{configurationBean.dataDirectory}/rhq;MVCC=TRUE;DB_CLOSE_ON_EXIT=FALSE;LOG=2';
                                          document.getElementById('propForm:databasedriverclass').value = 'org.h2.Driver';
                                          document.getElementById('propForm:databasexadatasourceclass').value = 'org.h2.jdbcx.JdbcDataSource';
                                          alert('#{bundle.experimentalDb}');
                                          showDiv('experimentalDbMsg');
                                       } else if (this.options[this.selectedIndex].value == 'sqlserver') {
                                          document.getElementById('propForm:databaseconnectionurl').value = 'jdbc:jtds:sqlserver://localhost:1433;databaseName=rhq';
                                          document.getElementById('propForm:databasedriverclass').value = 'net.sourceforge.jtds.jdbc.Driver';
                                          document.getElementById('propForm:databasexadatasourceclass').value = 'net.sourceforge.jtds.jdbcx.JtdsDataSource';
                                          alert('#{bundle.experimentalDb}');
                                          showDiv('experimentalDbMsg');
                                       } else if (this.options[this.selectedIndex].value == 'MySQL') {
                                          document.getElementById('propForm:databaseconnectionurl').value = 'jdbc:mysql://127.0.0.1/rhq';
                                          document.getElementById('propForm:databasedriverclass').value = 'com.mysql.jdbc.Driver';
                                          document.getElementById('propForm:databasexadatasourceclass').value = 'com.mysql.jdbc.jdbc2.optional.MysqlXADataSource';
                                          alert('#{bundle.experimentalDb}');
                                          showDiv('experimentalDbMsg');
                                       }
                                      ">
               <f:selectItems value="#{configurationBean.databaseConfiguration[0].itemDefinition.options}" />
            </h:selectOneMenu>

            <h:outputLink value="javascript:popUp('#{bundle.helpDocRHQServerPropParentPage}#{configurationBean.databaseConfiguration[1].itemDefinition.help}', 'propertyHelp')">
               <h:outputText value="#{configurationBean.databaseConfiguration[1].itemDefinition.propertyLabel}" />
            </h:outputLink>
            <h:inputText id="databaseconnectionurl"
                         size="#{configurationBean.databaseConfiguration[1].itemDefinition.fieldSize}"
                         value="#{configurationBean.databaseConfiguration[1].value}" />

            <h:outputLink value="javascript:popUp('#{bundle.helpDocRHQServerPropParentPage}#{configurationBean.databaseConfiguration[2].itemDefinition.help}', 'propertyHelp')">
               <h:outputText value="#{configurationBean.databaseConfiguration[2].itemDefinition.propertyLabel}" />
            </h:outputLink>
            <h:inputText id="databasedriverclass"
                         size="#{configurationBean.databaseConfiguration[2].itemDefinition.fieldSize}"
                         value="#{configurationBean.databaseConfiguration[2].value}" />

            <h:outputLink value="javascript:popUp('#{bundle.helpDocRHQServerPropParentPage}#{configurationBean.databaseConfiguration[3].itemDefinition.help}', 'propertyHelp')">
               <h:outputText value="#{configurationBean.databaseConfiguration[3].itemDefinition.propertyLabel}" />
            </h:outputLink>
            <h:inputText id="databasexadatasourceclass"
                         size="#{configurationBean.databaseConfiguration[3].itemDefinition.fieldSize}"
                         value="#{configurationBean.databaseConfiguration[3].value}" />

            <h:outputLink value="javascript:popUp('#{bundle.helpDocRHQServerPropParentPage}#{configurationBean.databaseConfiguration[4].itemDefinition.help}', 'propertyHelp')">
               <h:outputText value="#{configurationBean.databaseConfiguration[4].itemDefinition.propertyLabel}" />
            </h:outputLink>
            <h:inputText id="databaseusername"
                         size="#{configurationBean.databaseConfiguration[4].itemDefinition.fieldSize}"
                         value="#{configurationBean.databaseConfiguration[4].value}" />

            <h:outputLink value="javascript:popUp('#{bundle.helpDocRHQServerPropParentPage}#{configurationBean.databaseConfiguration[5].itemDefinition.help}', 'propertyHelp')">
               <h:outputText value="#{configurationBean.databaseConfiguration[5].itemDefinition.propertyLabel}" />
            </h:outputLink>
            <h:inputSecret id="databasepassword"
                           size="#{configurationBean.databaseConfiguration[5].itemDefinition.fieldSize}"
                           value="#{configurationBean.databaseConfiguration[5].value}" />
         </h:panelGrid>

         <h:panelGrid columns="2" border="1">
            <h:panelGrid columns="1">
               <h:outputText value="#{bundle.testDatabaseNote}" style="font-size: 100%"/>
               <h:panelGrid columns="2">
                  <h:commandButton id="testDatabaseButton"
                                   onclick="if (!checkForPassword()) return false"
                                   action="#{configurationBean.testConnection}"
                                   value="#{bundle.testDatabaseButton}"/>
                  <h:panelGroup rendered="#{configurationBean.lastTest != null && configurationBean.lastTest != 'OK'}">
                     <h:graphicImage value="/images/warning.gif" alt="Error" onclick="alert('#{configurationBean.lastTest}')" onmouseover="this.style.cursor='pointer'" onmouseout="this.style.cursor='default'"/>
                  </h:panelGroup>
                  <h:panelGroup rendered="#{configurationBean.lastTest != null && configurationBean.lastTest == 'OK'}">
                     <h:graphicImage value="/images/ok.gif" alt="OK"/>
                  </h:panelGroup>
               </h:panelGrid>
            </h:panelGrid>

<%-- this is confusing people, don't show this
            <h:panelGrid columns="1">
               <h:outputText value="#{bundle.createDatabaseNote}" style="font-size: 100%"/>
               <h:panelGrid columns="2">
                  <h:commandButton id="createDatabaseButton" action="#{configurationBean.showCreateDatabasePage}" value="#{bundle.createDatabaseButton}" />
                  <h:panelGroup rendered="#{configurationBean.lastCreate != null && configurationBean.lastCreate != 'OK'}">
                     <h:graphicImage value="/images/warning.gif" alt="Error" onclick="alert('#{configurationBean.lastCreate}')" onmouseover="this.style.cursor='pointer'" onmouseout="this.style.cursor='default'"/>
                  </h:panelGroup>
                  <h:panelGroup rendered="#{configurationBean.lastCreate != null && configurationBean.lastCreate == 'OK'}">
                     <h:graphicImage value="/images/ok.gif" alt="OK"/>
                  </h:panelGroup>
               </h:panelGrid>
            </h:panelGrid>
--%>
         </h:panelGrid>
      </h:panelGrid>

      <h:panelGrid columns="2" columnClasses="warningColor" rendered="#{configurationBean.databaseSchemaExist == true}">
         <h:outputText value="#{bundle.existingSchemaQuestion}" style="font-size: 80%"/>
         <h:selectOneMenu onchange="submit()" label="#{bundle.existingSchemaQuestion}" value="#{configurationBean.existingSchemaOption}">
            <f:selectItems value="#{configurationBean.existingSchemaOptions}" />
         </h:selectOneMenu>
      </h:panelGrid>


      <br/>
      <h4 align="left">
         <h:outputText value="#{bundle.installSettingsInstructions}" />
      </h4>
      <h:panelGrid columns="1">
         <h:panelGrid columns="1" rendered="#{configurationBean.registeredServers == true}" >

            <h:outputText value="#{bundle.installSettingsNote1}" />
            <br/>

            <h:panelGrid columns="2" >

               <h:outputLink value="javascript:popUp('#{bundle.helpDocRHQServerPropParentPage}-RegisteredServers', 'propertyHelp')" >
                  <h:outputText value="#{bundle.registeredServersLabel}" />
               </h:outputLink>
               <h:selectOneMenu value="#{configurationBean.selectedRegisteredServerName}"
                                onchange="{ document.getElementById('propForm:haservername').value = this.options[this.selectedIndex].value;
                                            document.getElementById('propForm:haendpointaddress').value = '';
                                            document.getElementById('propForm:haendpointport').value = '';
                                            document.getElementById('propForm:haendpointsecureport').value = '';
                                            if (document.getElementById('propForm:haaffinitygroup') != null) {
                                               document.getElementById('propForm:haaffinitygroup').value = ''; }
                                            submit(); }" >
                  <f:selectItems value="#{configurationBean.registeredServerNames}" />
               </h:selectOneMenu>

            </h:panelGrid>

            <br/>
            <h:outputText value="#{bundle.installSettingsNote2}" />

         </h:panelGrid>


         <h:panelGrid columns="3" headerClass="evenRow" rowClasses="evenRow,oddRow">

            <h:outputText value=" " />
            <h:outputText value=" " />
            <h:outputText value="#{bundle.requiresRestart}" style="font-weight:bold" />

            <h:outputLink value="javascript:popUp('#{bundle.helpDocRHQServerPropParentPage}#{configurationBean.propHaServerName.itemDefinition.help}', 'propertyHelp')" >
               <h:outputText value="#{bundle.propertyHighAvailabilityName}" />
            </h:outputLink>
            <h:inputText id="haservername" size="#{configurationBean.propHaServerName.itemDefinition.fieldSize}"
                         value="#{configurationBean.haServerName}" >
            </h:inputText>
            <h:outputText value="#{bundle.yesString}" rendered="#{configurationBean.propHaServerName.itemDefinition.requiresRestart}" />
            <h:outputText value="#{bundle.noString}" rendered="#{!configurationBean.propHaServerName.itemDefinition.requiresRestart}" />

            <h:outputLink value="javascript:popUp('#{bundle.helpDocRHQServerPropParentPage}-ServerPublicAddress', 'propertyHelp')" >
               <h:outputText value="#{bundle.propertyHighAvailabilityEndpointAddress}" />
            </h:outputLink>
            <h:inputText id="haendpointaddress" size="#{configurationBean.propHaServerName.itemDefinition.fieldSize}"
                         value="#{configurationBean.haServer.endpointAddress}" >
            </h:inputText>
            <h:outputText value="#{bundle.noString}" />

            <h:outputLink value="javascript:popUp('#{bundle.helpDocRHQServerPropParentPage}#{configurationBean.propHaEndpointPort.itemDefinition.help}', 'propertyHelp')" >
               <h:outputText value="#{bundle.propertyHttpPort}" />
            </h:outputLink>
            <h:inputText id="haendpointport" size="#{configurationBean.propHaEndpointPort.itemDefinition.fieldSize}"
                         value="#{configurationBean.haServer.endpointPortString}" >
            </h:inputText>
            <h:outputText value="#{bundle.yesString}" rendered="#{configurationBean.propHaEndpointPort.itemDefinition.requiresRestart}" />

            <h:outputLink value="javascript:popUp('#{bundle.helpDocRHQServerPropParentPage}#{configurationBean.propHaEndpointSecurePort.itemDefinition.help}', 'propertyHelp')" >
               <h:outputText value="#{bundle.propertyHttpsPort}" />
            </h:outputLink>
            <h:inputText id="haendpointsecureport" size="#{configurationBean.propHaEndpointSecurePort.itemDefinition.fieldSize}"
                         value="#{configurationBean.haServer.endpointSecurePortString}" >
            </h:inputText>
            <h:outputText value="#{bundle.yesString}" rendered="#{configurationBean.propHaEndpointSecurePort.itemDefinition.requiresRestart}" />

            <h:outputLink value="javascript:popUp('#{bundle.helpDocRHQServerPropParentPage}-ServerAffinityGroup', 'propertyHelp')"
                          rendered="#{configurationBean.showAdvancedSettings == true}" >
               <h:outputText value="#{bundle.propertyHighAvailabilityAffinityGroup}" />
            </h:outputLink>
            <h:inputText id="haaffinitygroup" size="#{configurationBean.propHaServerName.itemDefinition.fieldSize}"
                         value="#{configurationBean.haServer.affinityGroup}"
                         rendered="#{configurationBean.showAdvancedSettings == true}" >
            </h:inputText>
            <h:outputText value="#{bundle.noString}" rendered="#{configurationBean.showAdvancedSettings == true}" />

         </h:panelGrid>
      </h:panelGrid>

      <br/>
      <h4 align="left">
      <h:outputText value="#{bundle.serverSettingsInstructions}" />
      </h4>
      <h:dataTable value="#{configurationBean.nonDatabaseConfiguration}" var="prop"
                   headerClass="evenRow" rowClasses="oddRow,evenRow">
         <h:column>
            <f:facet name="header">
               <h:outputText value="" />
            </f:facet>

            <h:outputLink value="javascript:popUp('#{bundle.helpDocRHQServerPropParentPage}#{prop.itemDefinition.help}', 'propertyHelp')">
               <h:outputText value="#{prop.itemDefinition.propertyLabel}" />
            </h:outputLink>
         </h:column>
         <h:column>
            <f:facet name="header">
               <h:outputText value="" />
            </f:facet>
            <h:inputText size="#{prop.itemDefinition.fieldSize}"
                         value="#{prop.value}"
                         rendered="#{prop.itemDefinition.options == null && (prop.itemDefinition.propertyType.name eq 'java.lang.Integer' || prop.itemDefinition.propertyType.name eq 'java.net.InetAddress')}" />
            <h:inputText size="#{prop.itemDefinition.fieldSize}"
                         value="#{prop.value}"
                         rendered="#{prop.itemDefinition.options == null && (prop.itemDefinition.propertyType.name eq 'java.lang.String' && !prop.itemDefinition.secret)}" />
            <h:inputSecret size="#{prop.itemDefinition.fieldSize}"
                           value="#{prop.value}"
                           rendered="#{prop.itemDefinition.options == null && (prop.itemDefinition.propertyType.name eq 'java.lang.String' && prop.itemDefinition.secret)}" />
            <h:selectOneRadio value="#{prop.value}"
                              rendered="#{prop.itemDefinition.options == null && (prop.itemDefinition.propertyType.name eq 'java.lang.Boolean')}">
               <f:selectItem itemLabel="#{bundle.yesString}" itemValue="true"/>
               <f:selectItem itemLabel="#{bundle.noString}" itemValue="false"/>
            </h:selectOneRadio>
            <h:selectOneMenu value="#{prop.value}"
                             rendered="#{prop.itemDefinition.options != null}">
               <f:selectItems value="#{prop.itemDefinition.options}" />
            </h:selectOneMenu>
         </h:column>
         <h:column>
            <f:facet name="header">
               <h:outputText value="#{bundle.requiresRestart}" />
            </f:facet>
            <h:outputText value="#{bundle.yesString}" rendered="#{prop.itemDefinition.requiresRestart}" />
            <h:outputText value="#{bundle.noString}"  rendered="#{!prop.itemDefinition.requiresRestart}" />
         </h:column>
      </h:dataTable>

      <br/>
      <p align="left">
<%-- this is confusing people, don't show this
         <h4 align="left">
            <h:outputText value="#{bundle.installButtonsText}" />
         </h4>
         <table border="1"><tr>
            <td><h:commandButton id="save" action="#{configurationBean.save}" value="#{bundle.save}" /></td>
            <td><h:outputText value="#{bundle.orText}" /></td>
            <td><h:commandButton id="saveEmbeddedMode" action="#{configurationBean.saveEmbeddedMode}" value="#{bundle.saveEmbeddedMode}" /></td>
         </tr></table>
--%>
         <table border="1"><tr>
            <td><h:commandButton id="save" 
                   onclick="if (!checkForPassword()) return false"
                   action="#{configurationBean.save}"
                   value="#{bundle.save}" />
            </td>
         </tr></table>
      </p>
   </h:form>

   </body>

</html>

</f:view>
