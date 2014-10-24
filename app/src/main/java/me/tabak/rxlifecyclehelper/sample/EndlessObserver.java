package me.tabak.rxlifecyclehelper.sample;

import rx.Observer;

public abstract class EndlessObserver<T> implements Observer<T> {
  @Override
  public void onCompleted() {

  }
}
