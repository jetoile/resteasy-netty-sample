/*
 * Copyright (c) 2011 Khanh Tuong Maudoux <kmx.petals@gmail.com>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. The name of the author may not be used to endorse or promote products
 *    derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
 * NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
 * THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package fr.jetoile.sample;

import com.codahale.metrics.JmxReporter;
import com.codahale.metrics.MetricRegistry;
import fr.jetoile.sample.exception.BootstrapException;
import fr.jetoile.sample.service.SimpleService;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.jboss.resteasy.plugins.server.netty.NettyJaxrsServer;
import org.jboss.resteasy.spi.ResteasyDeployment;
import org.jboss.resteasy.test.TestPortProvider;
import org.jolokia.jvmagent.JolokiaServer;
import org.jolokia.jvmagent.JolokiaServerConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.HashMap;

/**
 * User: khanh
 * To change this template use File | Settings | File Templates.
 */
public class Main {

    private final static Logger LOGGER = LoggerFactory.getLogger(Main.class);

    public static final String CONF_PROPERTIES = "conf.properties";

    public static MetricRegistry metricRegistry;

    private static Configuration config;

    public static void main(String[] args) throws ConfigurationException, UnknownHostException, BootstrapException {

        try {
            config = new PropertiesConfiguration(Main.CONF_PROPERTIES);

        } catch (ConfigurationException e) {
            throw new BootstrapException("bad config", e);
        }

        initJolokiaServer();

        initServer();

        metricRegistry = new MetricRegistry();
        final JmxReporter reporter = JmxReporter.forRegistry(metricRegistry).build();
        reporter.start();

    }

    private static void initJolokiaServer() {
        try {
            JolokiaServerConfig config = new JolokiaServerConfig(new HashMap<String, String>());

            JolokiaServer jolokiaServer = new JolokiaServer(config, true);
            jolokiaServer.start();
        } catch (Exception e) {
            LOGGER.error("unable to start jolokia server", e);
        }
    }

    private static void initServer() {
        SimpleService service = new SimpleService();
        ResteasyDeployment deployment = new ResteasyDeployment();

        int nettyPort = 8081;

        if (config != null) {
            deployment.setAsyncJobServiceEnabled(config.getBoolean("netty.asyncJobServiceEnabled", false));
            deployment.setAsyncJobServiceMaxJobResults(config.getInt("netty.asyncJobServiceMaxJobResults", 100));
            deployment.setAsyncJobServiceMaxWait(config.getLong("netty.asyncJobServiceMaxWait", 300000));
            deployment.setAsyncJobServiceThreadPoolSize(config.getInt("netty.asyncJobServiceThreadPoolSize", 100));

            nettyPort = config.getInt("netty.port", TestPortProvider.getPort());
        } else {
            LOGGER.warn("is going to use default netty config");
        }

        deployment.setResources(Arrays.<Object>asList(service));

        NettyJaxrsServer netty = new NettyJaxrsServer();

        netty.setDeployment(deployment);
        netty.setPort(nettyPort);
        netty.setRootResourcePath("");
        netty.setSecurityDomain(null);
        netty.start();
    }

}


