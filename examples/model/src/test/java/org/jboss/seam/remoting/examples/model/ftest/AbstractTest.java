package org.jboss.seam.remoting.examples.model.ftest;

import java.io.File;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.importer.ZipImporter;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.runner.RunWith;

@RunWith(Arquillian.class)
public abstract class AbstractTest {

    public static final String ARCHIVE_NAME = "remoting-model.war";
    public static final String BUILD_DIRECTORY = "target";
    public static final String MAIN_PAGE = "/remoting-model/model.html";

    @Deployment(testable = false)
    public static WebArchive createDeployment() {
        return ShrinkWrap.create(ZipImporter.class, ARCHIVE_NAME).importFrom(new File(BUILD_DIRECTORY + '/' + ARCHIVE_NAME))
                .as(WebArchive.class);
    }

}
