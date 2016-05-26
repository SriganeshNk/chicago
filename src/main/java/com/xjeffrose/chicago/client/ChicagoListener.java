package com.xjeffrose.chicago.client;

import com.xjeffrose.chicago.ChicagoMessage;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import org.apache.log4j.Logger;

class ChicagoListener implements Listener<byte[]> {
  private static final Logger log = Logger.getLogger(ChicagoListener.class);
  private static final long TIMEOUT = 1000;
  private static final boolean TIMEOUT_ENABLED = true;

  private static final ConcurrentLinkedDeque<UUID> reqIds = new ConcurrentLinkedDeque<>();
  private static final ConcurrentLinkedDeque<UUID> messageIds = new ConcurrentLinkedDeque<>();
  private static final Map<UUID, ChicagoMessage> responseMap = new ConcurrentHashMap<>();

  public ChicagoListener() {

  }


  @Override
  public void onRequestSent() {
  }

  @Override
  public void onResponseReceived(byte[] message, boolean success) {
  }

  @Override
  public void onResponseReceived(ChicagoMessage chicagoMessage) {
    messageIds.add(chicagoMessage.getId());
    responseMap.put(chicagoMessage.getId(), chicagoMessage);
  }

  @Override
  public void onChannelError(Exception requestException) throws ChicagoClientException {
    log.error("Error Reading Response: ", requestException);
    throw new ChicagoClientException(requestException);
  }


  @Override
  public byte[] getResponse(UUID id) throws ChicagoClientTimeoutException {
    return _getResponse(id, System.currentTimeMillis());
  }

  private byte[] _getResponse(UUID id, long startTime) throws ChicagoClientTimeoutException {
    while (Collections.disjoint(reqIds, messageIds)) {
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

    while (!responseMap.containsKey(id)) {
      try {
        if (TIMEOUT_ENABLED && (System.currentTimeMillis() - startTime) > TIMEOUT) {
        Thread.currentThread().interrupt();
          throw new ChicagoClientTimeoutException();
        }
        Thread.sleep(1);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }

    ChicagoMessage _resp = responseMap.remove(id);

    if (_resp.getSuccess()) {
      return _resp.getVal();
    } else {
      log.error("Invalid Response returned");
      return null;
    }
  }

  @Override
  public boolean getStatus(UUID id) throws ChicagoClientTimeoutException {
    boolean resp = _getStatus(id, System.currentTimeMillis());
    return resp;
  }

  private boolean _getStatus(UUID id, long startTime) throws ChicagoClientTimeoutException {
    while (Collections.disjoint(reqIds, messageIds)) {
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

    while (!responseMap.containsKey(id)) {
      try {
        if (TIMEOUT_ENABLED && (System.currentTimeMillis() - startTime) > TIMEOUT) {
        Thread.currentThread().interrupt();
          throw new ChicagoClientTimeoutException();
        }
        Thread.sleep(1);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }
    if (responseMap.remove(id).getKey().length == 4) {
      return true;
    } else {
      return false;
    }

  }


  @Override
  public void onChannelReadComplete() {

  }

  @Override
  public void addID(UUID id) {
    reqIds.add(id);
  }

  @Override
  public ConcurrentLinkedDeque<UUID> getReqIds() {
    return reqIds;
  }

  @Override
  public byte[] getResponse(ConcurrentLinkedDeque<UUID> idList) throws ChicagoClientTimeoutException {
    return _getResponse(idList, System.currentTimeMillis());
  }

  private byte[] _getResponse(ConcurrentLinkedDeque<UUID> idList, long startTime) throws ChicagoClientTimeoutException {
    while (Collections.disjoint(reqIds, messageIds)) {
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

    while (Collections.disjoint(responseMap.keySet(), idList)) {
      try {
        if (TIMEOUT_ENABLED && (System.currentTimeMillis() - startTime) > TIMEOUT) {
          Thread.currentThread().interrupt();
          throw new ChicagoClientTimeoutException();
        }
        Thread.sleep(1);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }


    while (!responseMap.containsKey(idList.getFirst())) {
      idList.removeFirst();
    }

    ChicagoMessage _resp = responseMap.remove(idList.getFirst());

    if (_resp.getSuccess()) {
      return _resp.getVal();
    } else {
      log.error("Invalid Response returned");
      return null;
    }
  }


}
