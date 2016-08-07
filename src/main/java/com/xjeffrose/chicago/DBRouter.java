package com.xjeffrose.chicago;

import com.xjeffrose.chicago.server.ChicagoServerPipeline;
import com.xjeffrose.xio.application.Application;
import com.xjeffrose.xio.bootstrap.ApplicationBootstrap;

import com.xjeffrose.xio.pipeline.XioSslHttp1_1Pipeline;
import io.netty.channel.ChannelHandler;
import java.io.Closeable;
import java.io.IOException;
import java.net.InetSocketAddress;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DBRouter implements Closeable {
  private static final Logger log = LoggerFactory.getLogger(DBRouter.class);

  //TODO(JR): Make this concurrent to applow for parallel streams
  private final ChiConfig config;
  private final RocksDBImpl rocksDbImpl;
  private final DBLog dbLog;
  //private ChicagoMasterManager masterManager;
  private final ChannelHandler handler;
  private Application application;

  public DBRouter(ChiConfig config, RocksDBImpl rocksDbImpl, DBLog dbLog) {
    this.config = config;
    this.rocksDbImpl = rocksDbImpl;
    this.dbLog = dbLog;
    this.handler = new ChicagoDBHandler(rocksDbImpl, dbLog);
    //config.setDbRouter(this);
  }
  /*
  private void configureDBServer() {
    XioServerDef dbServer = new XioServerDefBuilder()
        .name("Chicago DB Server")
        .listen(new InetSocketAddress(config.getDBBindIP(), config.getDBPort()))
//        .withSecurityFactory(new XioNoOpSecurityFactory())
        .withSecurityFactory(new XioSecurityFactory() {
          @Override
          public XioSecurityHandlers getSecurityHandlers(XioServerDef xioServerDef, XioServerConfig xioServerConfig) {
            return new XioSecurityHandlerImpl(config.getCert(), config.getKey());
          }

          @Override
          public XioSecurityHandlers getSecurityHandlers() {
            return new XioSecurityHandlerImpl(config.getCert(), config.getKey());
          }
        })
        .withProcessorFactory(new XioProcessorFactory() {
          @Override
          public XioProcessor getProcessor() {
            return new ChicagoProcessor();
          }
        })
        .withCodecFactory(new XioCodecFactory() {
          @Override
          public ChannelHandler getCodec() {
            return new ChicagoCodec();
          }
        })
        .withAggregator(new XioAggregatorFactory() {
          @Override
          public ChannelHandler getAggregator() {
            return new XioNoOpHandler();
          }
        })
        .withRoutingFilter(new XioRoutingFilterFactory() {
          @Override
          public ChannelInboundHandler getRoutingFilter() {
            return new ChicagoDBHandler(rocksDbImpl, dbLog);
          }
        })
        .build();

    serverDefSet.add(dbServer);
  }

  */
//  private void configureElectionServer() {
//    XioServerDef eServer = new XioServerDefBuilder()
//        .name("Chicago Election Server")
//        .listen(new InetSocketAddress(config.getEBindIP(), config.getEPort()))
////        .withSecurityFactory(new XioNoOpSecurityFactory())
//        .withSecurityFactory(new XioSecurityFactory() {
//          @Override
//          public XioSecurityHandlers getSecurityHandlers(XioServerDef xioServerDef, XioServerConfig xioServerConfig) {
//            return new XioSecurityHandlerImpl(config.getCert(), config.getKey());
//          }
//
//          @Override
//          public XioSecurityHandlers getSecurityHandlers() {
//            return new XioSecurityHandlerImpl(config.getCert(), config.getKey());
//          }
//        })
//        .withProcessorFactory(new XioProcessorFactory() {
//          @Override
//          public XioProcessor getProcessor() {
//            return new ChicagoProcessor();
//          }
//        })
//        .withCodecFactory(new XioCodecFactory() {
//          @Override
//          public ChannelHandler getCodec() {
//            return new XioNoOpHandler();
//          }
//        })
//        .withAggregator(new XioAggregatorFactory() {
//          @Override
//          public ChannelHandler getAggregator() {
//            return new XioNoOpHandler();
//          }
//        })
//        .withRoutingFilter(new XioRoutingFilterFactory() {
//          @Override
//          public ChannelInboundHandler getRoutingFilter() {
//            return new ChicagoElectionHandler(config, masterManager);
//          }
//        })
//        .build();
//
//    serverDefSet.add(eServer);
//  }

//  private void configureRPCServer() {
//    XioServerDef rpcServer = new XioServerDefBuilder()
//        .name("Chicago RPC Server")
//        .listen(new InetSocketAddress(config.getRPCBindIP(), config.getRPCPort()))
////        .withSecurityFactory(new XioNoOpSecurityFactory())
//        .withSecurityFactory(new XioSecurityFactory() {
//          @Override
//          public XioSecurityHandlers getSecurityHandlers(XioServerDef xioServerDef, XioServerConfig xioServerConfig) {
//            return new XioSecurityHandlerImpl(config.getCert(), config.getKey());
//          }
//
//          @Override
//          public XioSecurityHandlers getSecurityHandlers() {
//            return new XioSecurityHandlerImpl(config.getCert(), config.getKey());
//          }
//        })
//        .withProcessorFactory(new XioProcessorFactory() {
//          @Override
//          public XioProcessor getProcessor() {
//            return new ChicagoProcessor();
//          }
//        })
//        .withCodecFactory(new XioCodecFactory() {
//          @Override
//          public ChannelHandler getCodec() {
//            return new ChicagoCodec();
//          }
//        })
//        .withAggregator(new XioAggregatorFactory() {
//          @Override
//          public ChannelHandler getAggregator() {
//            return new XioNoOpHandler();
//          }
//        })
//        .withRoutingFilter(new XioRoutingFilterFactory() {
//          @Override
//          public ChannelInboundHandler getRoutingFilter() {
//            return new ChicagoRPCHandler(config, masterManager);
//          }
//        })
//        .build();
//
//    serverDefSet.add(rpcServer);
//  }


  /*
  private ChicagoServerPipeline buildElectionPipeline() {
    return new ChicagoServerPipeline("election") {
      @Override
      public ChannelHandler getCodecHandler() {
        return new XioNoOpHandler();
      }

      @Override
      public ChannelHandler getApplicationHandler() {
        return new ChicagoElectionHandler(config, masterManager);
      }
    };
  }

  private ChicagoServerPipeline buildRpcPipeline() {
    return new ChicagoServerPipeline("rpc") {
      @Override
      public ChannelHandler getApplicationHandler() {
        return new ChicagoRPCHandler(config, masterManager);
      }
    };
  }
  */

  private ChicagoServerPipeline buildDbPipeline() {
    return new ChicagoServerPipeline("db") {
      @Override
      public ChannelHandler getApplicationHandler() {
//        return new ChicagoDBHandler(rocksDbImpl, dbLog);
        return handler;
      }
    };
  }


  public void run() {
    application = new ApplicationBootstrap("chicago.application")
      .addServer("admin", (bs) -> bs.addToPipeline(new XioSslHttp1_1Pipeline()))
      .addServer("stats", (bs) -> bs.addToPipeline(new XioSslHttp1_1Pipeline()))
      .addServer("db", (bs) -> bs.addToPipeline(buildDbPipeline()))
//      .addServer("election", (bs) -> bs.addToPipeline(buildElectionPipeline()))
//      .addServer("rpc", (bs) -> bs.addToPipeline(buildRpcPipeline()))
      .build();

    //this.masterManager = new ChicagoMasterManager(config, application.instrumentation("election").boundAddress());

    /*
    configureAdminServer();
    configureStatsServer();
    //configureElectionServer();
    // configureRPCServer();
    configureDBServer();

    XioServerConfig serverConfig = XioServerConfig.newBuilder()
        .setBossThreadCount(config.getBossCount())
        .setBossThreadExecutor(Executors.newCachedThreadPool())
        .setWorkerThreadCount(config.getWorkers())
        .setWorkerThreadExecutor(Executors.newCachedThreadPool())
//        .setBootstrapOptions((Map<ChannelOption<Object>, Object>) new HashMap<>().put(ChannelOption.SO_KEEPALIVE, true))
        .build();

    ChannelGroup channels = new DefaultChannelGroup(new NioEventLoopGroup(config.getWorkers()).next());
    x = new XioBootstrap(serverDefSet, serverConfig, channels);

    try {
      x.start();
      config.setChannelStats(x.getXioMetrics());
      // For debug, leave commented out (or not, your choice if you like it)
      String msg = "--------------- Chicago Server Started!!! ----------------------";
      //System.out.println(msg);
      log.info(msg);
    } catch (Exception e) {
      e.printStackTrace();
      log.error("There was an error starting Chicago: ", e);
      x.stop();
      throw new RuntimeException(e);
    }
    */
  }

  @Override
  public void close() throws IOException {
    application.close();
    /*
    x.stop();
    serverDefSet.clear();
    */
    rocksDbImpl.destroy();
  }

  public void stop() {
    try {
      close();
    } catch (IOException e) {
      //TODO(JR): Should we just force close here?
      log.error("Error while attempting to close", e);
      throw new RuntimeException(e);
    }
  }

  public InetSocketAddress getDBBoundInetAddress() {
    return application.instrumentation("db").boundAddress();
  }
}
