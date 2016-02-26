/*
 * Copyright (c) 2005,2006,2007 The Regents of the University of California.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *     * Neither the name of the University of California, nor the
 *       names of its contributors may be used to endorse or promote products
 *       derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE UNIVERSITY OF CALIFORNIA ``AS IS'' AND ANY
 * EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE UNIVERSITY OF CALIFORNIA BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package edu.brown.cs.systems.xtrace.server;

import org.apache.log4j.Logger;

import edu.brown.cs.systems.pubsub.PubSub;
import edu.brown.cs.systems.pubsub.PubSubServer;
import edu.brown.cs.systems.xtrace.XTraceSettings;
import edu.brown.cs.systems.xtrace.server.api.DataStore;
import edu.brown.cs.systems.xtrace.server.api.MetadataStore;
import edu.brown.cs.systems.xtrace.server.impl.DerbyMetadataStore;
import edu.brown.cs.systems.xtrace.server.impl.FileTreeDataStore;
import edu.brown.cs.systems.xtrace.server.impl.PubSubSource;

/**
 * @author George Porter
 * @author Jonathan Mace
 */
public class XTraceServer {
    private static final Logger LOG = Logger.getLogger(XTraceServer.class);

    // Storage
    private final MetadataStore metadata;
    private final DataStore data;

    // Servers
    private final WebServer webserver;
    private final PubSubServer pubsubserver;

    // Report sources
    private final PubSubSource pubsubsource;

    /**
     * Only allow a single XTrace server to run
     */
    private XTraceServer() throws Exception {
        // Create the data stores
        metadata = DerbyMetadataStore.getInstance();
        data = new FileTreeDataStore(XTraceSettings.DATASTORE_DIRECTORY + "/reports/");

        // Create the static servers
        webserver = new WebServer(XTraceSettings.WEBUI_PORT, data, metadata);
        webserver.start();
        pubsubserver = PubSub.startServer();

        // Start the report sources
        pubsubsource = new PubSubSource(data, metadata);

        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                try {
                    metadata.shutdown();
                } catch (Exception e) {
                    LOG.warn("Unable to shutdown metadata store", e);
                }
                try {
                    data.shutdown();
                } catch (Exception e) {
                    LOG.warn("Unable to shutdown data store", e);
                }
                try {
                    pubsubserver.shutdown();
                } catch (Exception e) {
                    LOG.warn("Unable to shutdown pubsub server", e);
                }
                try {
                    pubsubsource.shutdown();
                } catch (Exception e) {
                    LOG.warn("Unable to shutdown pubsub server", e);
                }
                LOG.info("XTraceServer shut down");
            }
        });
    }

    private static XTraceServer INSTANCE;

    public static XTraceServer getInstance() throws Exception {
        if (INSTANCE == null)
            INSTANCE = new XTraceServer();
        return INSTANCE;
    }

    public static void main(String[] args) {
        try {
            XTraceServer.getInstance();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
