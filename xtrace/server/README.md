## X-Trace - Report Server

The X-Trace server is a standalone component the receives reports from clients and persists them into a database.  It also runs a webserver for accessing and visualizing reports.  
The following command will run the X-Trace server:

    xtrace/server/target/appassembler/bin/backend

The X-Trace backend runs both the web server and the pub sub server.  To change any of the pubsub configuration values, see [PubSub](tracingplane/pubsub/index.html)



The X-Trace server uses the following default config values:

	// X-Trace Server Config
	xtrace {

	    server {
	        bind-hostname = 0.0.0.0 # hostname to bind the webserver to

	        database-update-interval-ms = 1000

	        webui {
	            port                    = 4080
	        }

	        datastore {
	            dir                     = "./xtrace-data"   # location of xtrace storage
	            buffer-size             = 65536             # buffer size for each task writer
	            cache-size              = 1000              # number of file handles to cache for writing tasks
	            cache-timeout           = 30000             # cache eviction timeout for file handles
	        }
	    }

	}
