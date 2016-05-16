package com.xjeffrose.chicago.client;

import org.apache.log4j.Logger;

class ChicagoListener implements Listener<byte[]> {
  private static final Logger log = Logger.getLogger(ChicagoListener.class);

  private byte[] response;
  private boolean success;

  @Override
  public void onRequestSent() {

  }

  @Override
  public void onResponseReceived(byte[] message, boolean _success) {
    response = message;
    success = _success;
  }

  @Override
  public void onChannelError(Exception requestException) {
    log.error("Error Reading Response: ", requestException);
  }

  @Override
  public byte[] getResponse() {
    if (response != null) {
      return response;
    } else {
      try {
        Thread.sleep(1);
        getResponse();
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
      return getResponse();
    }
  }

  @Override
  public boolean getStatus() {
    if (response != null) {
      return success;
    } else {
      try {
        Thread.sleep(1);
        getStatus();
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
      return success;
    }
  }
}
