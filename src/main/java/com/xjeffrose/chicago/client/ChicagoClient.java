package com.xjeffrose.chicago.client;

import com.google.common.hash.Funnels;
import com.xjeffrose.chicago.DefaultChicagoMessage;
import com.xjeffrose.chicago.Op;
import com.xjeffrose.chicago.ZkClient;
import io.netty.channel.ChannelFuture;
import java.net.InetSocketAddress;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.apache.log4j.Logger;

public class ChicagoClient {
  private static final Logger log = Logger.getLogger(ChicagoClient.class);
  private final static String NODE_LIST_PATH = "/chicago/node-list";
  private static final long TIMEOUT = 1000;
  private static boolean TIMEOUT_ENABLED = true;
  private static int MAX_RETRY = 3;

  private final ExecutorService exe = Executors.newFixedThreadPool(20);

  private final InetSocketAddress single_server;
  private final RendezvousHash rendezvousHash;
  private final ClientNodeWatcher clientNodeWatcher;
  private final ZkClient zkClient;
  private final ConnectionPoolManager connectionPoolMgr;

  /*
   * Happy Path:
   * Delete -> send message to all (3) available nodes wait for all (3) responses to be true.
   * Write -> send message to all (3) available nodes wait for all (3) responses to be true.
   * Read -> send message to all (3) available nodes, wait for 1 node to reply, all other (2) replies are dropped.
   *
   * Fail Path:
   * Delete -> not all responses are true
   * Write -> not all responses are true
   * Read -> no nodes respond
   *
   * Reading from a node that hasn't been able to receive writes
   * Write fails, some nodes think that they have good data until they're told that they don't
   * interleaved writes from two different clients for the same key
   *
   *
   *
   *
   * two phase commit with multiple nodes
   *  write (key, value)
   *  ack x 3 nodes
   *  ok x 3 nodes -> write request
   */

  public ChicagoClient(InetSocketAddress server) {
    this.single_server = server;
    this.zkClient = null;
    ArrayList<String> nodeList = new ArrayList<>();
    nodeList.add(server.getHostName());
    this.rendezvousHash = new RendezvousHash(Funnels.stringFunnel(Charset.defaultCharset()), nodeList);
    this.clientNodeWatcher = null;
    connectionPoolMgr = new ConnectionPoolManager(server.getHostName());
  }

  public ChicagoClient(String zkConnectionString) throws InterruptedException {

    this.single_server = null;
    this.zkClient = new ZkClient(zkConnectionString);
    zkClient.start();

    this.rendezvousHash = new RendezvousHash(Funnels.stringFunnel(Charset.defaultCharset()), buildNodeList());
    this.clientNodeWatcher = new ClientNodeWatcher();
    clientNodeWatcher.refresh(zkClient, rendezvousHash);
    this.connectionPoolMgr = new ConnectionPoolManager(zkClient);
  }


  protected List<String> buildNodeList() {
    return zkClient.list(NODE_LIST_PATH);
  }

  public byte[] read(byte[] key) throws ChicagoClientTimeoutException {
    return read("chicago".getBytes(), key);
  }

  public byte[] read(byte[] colFam, byte[] key) throws ChicagoClientTimeoutException {
    final long startTime = System.currentTimeMillis();
    ConcurrentLinkedDeque<byte[]> responseList = new ConcurrentLinkedDeque<>();
    if (single_server != null) {
    }
    try {
      List<String> hashList = rendezvousHash.get(key);
      for (String node : hashList) {
        if (node == null) {
        } else {
          ChannelFuture cf = connectionPoolMgr.getNode(node);
          if (cf.channel().isWritable()) {
            exe.execute(new Runnable() {
              @Override
              public void run() {
//                try {
                  UUID id = UUID.randomUUID();
                  Listener listener = connectionPoolMgr.getListener(node);
                  cf.channel().writeAndFlush(new DefaultChicagoMessage(id, Op.READ, colFam, key, null));
                  listener.addID(id);
                  exe.execute(new Runnable() {
                    @Override
                    public void run() {
                      try {
                        responseList.add((byte[]) listener.getResponse(id));
                      } catch (ChicagoClientTimeoutException e) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException(e);
                      }
                    }
                  });
                }
            });
          }
        }
      }

    } catch (ChicagoClientTimeoutException e) {
      Thread.currentThread().interrupt();
      log.error("Client Timeout During Read Operation:", e);
      return null;
    }


    while (responseList.isEmpty()) {
      if (TIMEOUT_ENABLED && (System.currentTimeMillis() - startTime) > TIMEOUT) {
        Thread.currentThread().interrupt();
        throw new ChicagoClientTimeoutException();
      }
      try {
        Thread.sleep(1);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }

    return responseList.stream().findFirst().orElse(null);
  }

  public boolean write(byte[] key, byte[] value) throws ChicagoClientTimeoutException, ChicagoClientException {
    return write("chicago".getBytes(), key, value);
  }

  public boolean write(byte[] colFam, byte[] key, byte[] value) throws ChicagoClientTimeoutException, ChicagoClientException {
    return _write(colFam, key, value, 0);
  }


    private boolean _write(byte[] colFam, byte[] key, byte[] value, int retries) throws ChicagoClientTimeoutException, ChicagoClientException {
    ConcurrentLinkedDeque<Boolean> responseList = new ConcurrentLinkedDeque<>();
    final long startTime = System.currentTimeMillis();
    List<String> hashList = null;
    if (single_server != null) {
//      connect(single_server, Op.WRITE, key, value, listener);
    }

    try {

      hashList = rendezvousHash.get(key);

      for (String node : hashList) {
        if (node == null) {

        } else {
          ChannelFuture cf = connectionPoolMgr.getNode(node);
          if (cf.channel().isWritable()) {
            exe.execute(new Runnable() {
              @Override
              public void run() {
//            try {
                  UUID id = UUID.randomUUID();
                  Listener listener = connectionPoolMgr.getListener(node); // Blocking
                  cf.channel().writeAndFlush(new DefaultChicagoMessage(id, Op.WRITE, colFam, key, value));
                  listener.addID(id);
                  exe.execute(new Runnable() {
                    @Override
                    public void run() {
                      try {
                        responseList.add(listener.getStatus(id)); //Blocking
                      } catch (ChicagoClientTimeoutException e) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException(e);
                      }
                    }
                  });
              }
            });
          }
        }
      }

    } catch (ChicagoClientTimeoutException e) {
      log.error("Client Timeout During Write Operation: ", e);
      return false;
    }


    while (responseList.size() < hashList.size()) {
      if (TIMEOUT_ENABLED && (System.currentTimeMillis() - startTime) > TIMEOUT) {
        Thread.currentThread().interrupt();
        throw new ChicagoClientTimeoutException();
      }
      try {
        Thread.sleep(1);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }

    if (responseList.stream().allMatch(b -> b)) {
      return true;
    } else {
      if (MAX_RETRY < retries) {
        return _write(colFam, key, value, retries++);
      } else {
        _delete(colFam, key, 0);
        throw new ChicagoClientException("Could not successfully complete a replicated write. Please retry the operation");
      }
    }
  }

  public boolean delete(byte[] key) throws ChicagoClientTimeoutException, ChicagoClientException {
    return delete("chicago".getBytes(), key);
  }

  public boolean delete(byte[] colFam, byte[] key) throws ChicagoClientTimeoutException, ChicagoClientException {
    return _delete(colFam, key, 0);
  }

    private boolean _delete(byte[] colFam, byte[] key, int retries) throws ChicagoClientTimeoutException, ChicagoClientException {
    ConcurrentLinkedDeque<Boolean> responseList = new ConcurrentLinkedDeque<>();
    final long startTime = System.currentTimeMillis();
    List<String> hashList = null;
    try {

      hashList = rendezvousHash.get(key);

      for (String node : hashList) {
        if (node == null) {

        } else {
          ChannelFuture cf = connectionPoolMgr.getNode(node);
          if (cf.channel().isWritable()) {
            exe.execute(new Runnable() {
              @Override
              public void run() {
//                try {
                UUID id = UUID.randomUUID();
                Listener listener = connectionPoolMgr.getListener(node);
                cf.channel().writeAndFlush(new DefaultChicagoMessage(id, Op.DELETE, colFam, key, null));
                listener.addID(id);
                exe.execute(new Runnable() {
                  @Override
                  public void run() {
                    try {
                      responseList.add(listener.getStatus(id));
                    } catch (ChicagoClientTimeoutException e) {
                      Thread.currentThread().interrupt();
                      throw new RuntimeException(e);
                    }
                  }
                });
              }
            });
          }
        }
      }

    } catch (ChicagoClientTimeoutException e) {
      log.error("Client Timeout During Delete Operation: ", e);
      return false;
    }

        while (responseList.size() < hashList.size()) {
          if (TIMEOUT_ENABLED && (System.currentTimeMillis() - startTime) > TIMEOUT) {
            Thread.currentThread().interrupt();
            throw new ChicagoClientTimeoutException();
          }
          try {
            Thread.sleep(1);
          } catch (InterruptedException e) {
            e.printStackTrace();
          }
        }


    if (responseList.stream().allMatch(b -> b)) {
      return true;
    } else {
      if (MAX_RETRY < retries) {
        return _delete(colFam, key, retries++);
      } else {
        throw new ChicagoClientException("Could not successfully complete a replicated write. Please retry the operation");
      }
    }
    }

  public List<String> getNodeList(byte[] key) {
    return rendezvousHash.get(key);
  }
}
