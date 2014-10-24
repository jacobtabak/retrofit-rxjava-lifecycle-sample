package me.tabak.rxlifecyclehelper.sample;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;

import rx.Observable;
import rx.android.observables.AndroidObservable;
import rx.subjects.PublishSubject;


public final class RxLifecycleHelper {
  private final FragmentActivity mActivity;
  private final Fragment mFragment;
  private final PublishSubject<Void> mDestroyed = PublishSubject.create();

  public RxLifecycleHelper(FragmentActivity activity) {
    mActivity = activity;
    mFragment = null;
  }

  public RxLifecycleHelper(Fragment object) {
    mFragment = object;
    mActivity = null;
  }

  public <T> Observable<T> bindObservable(Observable<T> in) {
    if (mActivity != null) {
      return AndroidObservable.bindActivity(mActivity, in).takeUntil(mDestroyed);
    } else if (mFragment != null) {
      return AndroidObservable.bindFragment(mFragment, in).takeUntil(mDestroyed);
    }
    throw new IllegalStateException();
  }

  @SuppressWarnings("unchecked")
  public <T extends RetainedState> T getRetainedState(Class<T> cls) {
    FragmentManager fragmentManager;
    if (mActivity != null) {
      fragmentManager = mActivity.getSupportFragmentManager();
    } else if (mFragment != null) {
      fragmentManager = mFragment.getChildFragmentManager();
    } else {
      throw new IllegalStateException();
    }

    Fragment state = fragmentManager.findFragmentByTag(cls.getName());
    if (state == null) {
      state = T.instantiate(mActivity, cls.getName());
      fragmentManager.beginTransaction().add(state, cls.getName()).commit();
    }
    return (T) state;
  }

  public void onDestroy() {
    mDestroyed.onNext(null);
  }

  public static class RetainedState extends Fragment {
    @Override
    public void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);
      setRetainInstance(true);
    }
  }
}
