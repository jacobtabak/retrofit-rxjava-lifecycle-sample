package me.tabak.rxlifecyclehelper.sample;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;

import rx.Observable;
import rx.android.observables.AndroidObservable;
import rx.subjects.PublishSubject;


/**
 * RxJava lifecycle helper that manages state across activity or fragment
 * re-creation and provides activities and fragments with a simple way to unsubscribe
 * from all bound observables.
 */
public final class RxLifecycleHelper {
  private final FragmentActivity mActivity;
  private final Fragment mFragment;
  private final PublishSubject<Void> mDestroyed = PublishSubject.create();

  /**
   * Constructor for activities
   * @param activity Support FragmentActivity
   */
  public RxLifecycleHelper(FragmentActivity activity) {
    mActivity = activity;
    mFragment = null;
  }

  /**
   * Constructor for fragments
   * @param fragment Support fragment
   */
  public RxLifecycleHelper(Fragment fragment) {
    mFragment = fragment;
    mActivity = null;
  }

  /**
   * Binds an observable to the current activity or fragment.
   * @param in Observable to bind to this activity or fragment.
   * @return A wrapped observable that is lifecycle-aware
   * @see rx.android.observables.AndroidObservable#bindActivity(android.app.Activity, rx.Observable)
   * @see rx.android.observables.AndroidObservable#bindFragment(Object, rx.Observable)
   */
  public <T> Observable<T> bindObservable(Observable<T> in) {
    if (mActivity != null) {
      return AndroidObservable.bindActivity(mActivity, in).takeUntil(mDestroyed);
    } else if (mFragment != null) {
      return AndroidObservable.bindFragment(mFragment, in).takeUntil(mDestroyed);
    }
    throw new IllegalStateException();
  }

  /**
   * Creates or retrieves a {@link me.tabak.rxlifecyclehelper.sample.RxLifecycleHelper.RetainedState}
   * object that will persist across activity or fragment re-creation.
   * @param cls The class to create or retrieve.
   * @return An instance will be associated with this activity and persisted across re-creation.
   */
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
    Context context = mActivity != null ? mActivity : mFragment.getActivity();
    if (state == null) {
      state = T.instantiate(context, cls.getName());
      fragmentManager.beginTransaction().add(state, cls.getName()).commit();
    }

    return (T) state;
  }

  /**
   * You should call onDestroy from your Activity or Fragment to unsubscribe all bound observables.
   */
  public void onDestroy() {
    mDestroyed.onNext(null);
  }

  /**
   * Base class for retained state.
   */
  public static class RetainedState extends Fragment {
    @Override
    public void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);
      setRetainInstance(true);
    }
  }
}
