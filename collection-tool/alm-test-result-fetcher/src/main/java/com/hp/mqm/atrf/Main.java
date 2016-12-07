package com.hp.mqm.atrf;

import com.hp.mqm.atrf.alm.services.AlmWrapperService;
import com.hp.mqm.atrf.core.*;
import com.hp.mqm.atrf.octane.services.OctaneWrapperService;
import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LoggerContext;

import java.net.URI;
import java.net.URISyntaxException;

/**
 *
 */
public class Main {
    static final Logger logger = LogManager.getLogger();
    private static FetchConfiguration configuration;
    private static AlmWrapperService almWrapper;
    private static OctaneWrapperService octaneWrapper;

    public static void main(String[] args)  {

        configureLog4J();
        setUncaughtExceptionHandler();

        logger.info("Command line args are : " + ((args.length > 0) ? StringUtils.join(args, ",") : " none "));

        //fetch settings from xml file

        configuration = FetchConfiguration.loadFromArguments(args);

        loginToAlm();
        loginToOctane();
        almWrapper.init(configuration);
    }

    private static void loginToAlm()  {
        almWrapper = new AlmWrapperService(configuration.getAlmServerUrl(), configuration.getAlmDomain(), configuration.getAlmProject());
        if (almWrapper.login(configuration.getAlmUser(), configuration.getAlmPassword())) {

            logger.info("ALM : Login successful");
            if (almWrapper.validateConnectionToProject()) {
                logger.info("ALM : Connected to ALM project successfully");
            } else {
                throw new RuntimeException("ALM : Failed to connect to ALM Project.");
            }
        } else {
            throw new RuntimeException("ALM : Failed to login");
        }
    }

    private static void loginToOctane()  {
        long sharedSpaceId = Long.parseLong(configuration.getOctaneSharedSpaceId());
        long workspaceId = Long.parseLong(configuration.getOctaneWorkspaceId());

        octaneWrapper = new OctaneWrapperService(configuration.getOctaneServerUrl(), sharedSpaceId, workspaceId);
        if (octaneWrapper.login(configuration.getOctaneUser(), configuration.getOctanePassword())) {

            logger.info("Octane : Login successful");
            if (octaneWrapper.validateConnectionToWorkspace()) {
                logger.info("Octane : Connected to Octane project successfully");
            } else {
                throw new RuntimeException("Octane : Failed to connect to Octane Workspace.");
            }
        } else {
            throw new RuntimeException("Octane : Failed to login");
        }
    }

    private static void setUncaughtExceptionHandler() {
        Thread.currentThread().setUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            public void uncaughtException(Thread t, Throwable e) {
                logger.error(e.getMessage(), e);
            }
        });
    }

    private static void configureLog4J() {
        String log4jConfiguration = System.getProperty("log4j.configuration");
        if (StringUtils.isEmpty(log4jConfiguration)) {
            //take it from resources
            URI uri = null;
            try {
                uri = Main.class.getClassLoader().getResource("log4j2.xml").toURI();
                LoggerContext context = (LoggerContext) LogManager.getContext(false);
                context.setConfigLocation(uri);
                logger.warn("Log4j configuration loaded from resource file configuration");
            } catch (URISyntaxException e) {
                throw new RuntimeException("Failed to load Log4j configuration from resource file configuration");
            }

        } else {
            logger.error("Log4j configuration loaded from external configuration");
        }
    }
}
