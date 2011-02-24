
Seam Remoting ${project.version}
===================================

Seam Remoting is an AJAX remoting API for Java EE6 applications.


Contents of distribution
========================

 artifacts/
 
   Seam Remoting libraries

 doc/

   API Docs and reference guide.
  
 examples/

   Seam Remoting Examples
  
 lib/

   Libraries for dependencies
   
 source/
 
   Source code for this module
  
Licensing
=========

 This distribution, as a whole, is licensed under the terms of the Apache
 Software License, Version 2.0 (ASL).

Seam Remoting URLs
==================

Seam Framework Home Page:      http://www.seamframework.org
Downloads:                     http://www.seamframework.org/Download/SeamDownloads
Forums:                        http://www.seamframework.org/Community/SeamUsers
Source Code:                   git://github.com/seam/remoting.git
Issue Tracking:                http://jira.jboss.org/jira/browse/SEAMREMOTING

Release Notes
=============
Version 3.0.0 Beta 2
--------------------
* Added new feature - bean validation
* Updated to use Seam Solder 

Version 3.0.0 Beta 1
--------------------
First beta release of Seam Remoting 3.x, ported from Seam 2.x to CDI.

* Added new feature - Model API
* Simplified JavaScript stubs for server-side beans
* Added option to compress remote.js - add ?compress=true to URL
* Experimental JMS support (that was present in Seam 2.x) has been removed - this feature will be
  provided at a later date by a unified AJAX event bus.
* Support for batch requests has been removed

* If using Maven, some artifacts may only be available in the JBoss Repository. To allow Seam Remoting to correctly function, add the JBoss Repository to Maven. Edit your ~/.m2/settings.xml, and add the following entry:

      <profile>
         <id>jboss.repository</id>
         <activation>
            <activeByDefault>true</activeByDefault>
         </activation>
         <repositories>
            <repository>
               <id>repository.jboss.org</id>
               <url>http://repository.jboss.org/maven2</url>
               <releases>
                  <enabled>true</enabled>
               </releases>
               <snapshots>
                  <enabled>false</enabled>
               </snapshots>
            </repository>
         </repositories>
      </profile>
