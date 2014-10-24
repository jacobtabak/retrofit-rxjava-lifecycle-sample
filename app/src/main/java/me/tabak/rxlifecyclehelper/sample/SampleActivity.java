package me.tabak.rxlifecyclehelper.sample;

import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import retrofit.client.Response;
import rx.Observable;


/**
 * Sample activity that demonstrates persisting Retrofit RxJava Observables
 * across activity re-creation.
 * @author Jacob Tabak
 */
public class SampleActivity extends FragmentActivity {
  private RxLifecycleHelper mRxHelper;
  private final RetrofitService mService = RetrofitService.MockService.create();
  private SampleActivityState mRetainedState;
  private Button mButton;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(me.tabak.rxlifecyclehelper.R.layout.activity_sample);
    mButton = (Button) findViewById(me.tabak.rxlifecyclehelper.R.id.button);

    mRxHelper = new RxLifecycleHelper(this);

    // Gets the retained instance state.  If none exists, this will create it.
    mRetainedState = mRxHelper.getRetainedState(SampleActivityState.class);

    // Set the button to the correct state.
    syncButtonState();

    if (mRetainedState.observable != null) {
      // A request is pending, re-subscribe to it.
      mRxHelper
          .bindObservable(mRetainedState.observable)
          .subscribe(new DownloadObserver());
    }

    mButton.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        downloadFile();
      }
    });
  }

  private void downloadFile() {
    mRetainedState.buttonText = "Downloading...";
    mButton.setEnabled(false);
    mRetainedState.buttonEnabled = false;
    syncButtonState();

    // Get a 'cold' observable that will start when it is subscribed to.
    Observable<Response> responseObservable = mService.downloadFile();

    // Apply the cache operator so we can re-subscribe without restarting the request.
    // Then, save it to our retained state.
    mRetainedState.observable = responseObservable.cache();

    // Finally, bind the observable to our activity and subscribe to it.
    // This ensures that the callbacks can safely interact with the UI.
    // Note that we save the observable BEFORE binding it to the activity.
    mRxHelper
        .bindObservable(mRetainedState.observable)
        .subscribe(new DownloadObserver());
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();
    // You must call onDestroy to prevent the context from temporarily leaking.
    mRxHelper.onDestroy();
  }

  private class DownloadObserver extends EndlessObserver<Response> {
    @Override
    public void onError(Throwable e) {
      Log.e("DownloadObserver", "Download failed", e);
      mRetainedState.buttonText = "Failed :( Again?";
      mRetainedState.buttonEnabled = true;
      mRetainedState.observable = null;
      syncButtonState();
    }

    @Override
    public void onNext(Response response) {
      mRetainedState.buttonText = "Success! Again?";
      mRetainedState.buttonEnabled = true;
      mRetainedState.observable = null;
      syncButtonState();
    }
  }

  private void syncButtonState() {
    mButton.setText(mRetainedState.buttonText);
    mButton.setEnabled(mRetainedState.buttonEnabled);
  }

  public static class SampleActivityState extends RxLifecycleHelper.RetainedState {
    Observable<Response> observable;
    private boolean buttonEnabled = true;
    private String buttonText = "Tap to Start";
  }
}
