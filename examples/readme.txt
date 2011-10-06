Seam Remoting Examples
======================

Running the functional tests

- set JBOSS_HOME enviroment property to point to JBoss AS 7 installation
- in the example folder, run mvn clean verify -Darquillian=jbossas-managed-7

Besides, the following configurations are supported:
mvn clean verify -Darquillian=jbossas-managed-6
mvn clean verify -Darquillian=jbossas-managed-7
mvn clean verify -Darquillian=glassfish-remote-3.1

mvn clean verify -Pjbossas6 -Darquillian=jbossas-remote-6
mvn clean verify -Pjbossas7 -Darquillian=jbossas-remote-7
