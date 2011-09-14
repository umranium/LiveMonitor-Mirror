package com.google.android.apps.mytracks.services.sensors.ant;

public class AntException extends Exception {

  public AntException() {
  }

  public AntException(String detailMessage) {
    super(detailMessage);
  }

  public AntException(Throwable throwable) {
    super(throwable);
  }

  public AntException(String detailMessage, Throwable throwable) {
    super(detailMessage, throwable);
  }

}
