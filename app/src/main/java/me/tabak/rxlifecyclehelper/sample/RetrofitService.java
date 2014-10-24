package me.tabak.rxlifecyclehelper.sample;

import android.os.AsyncTask;

import java.util.ArrayList;
import java.util.List;

import retrofit.MockRestAdapter;
import retrofit.RestAdapter;
import retrofit.android.MainThreadExecutor;
import retrofit.client.Header;
import retrofit.client.Response;
import retrofit.http.GET;
import rx.Observable;

public interface RetrofitService {
  public static final String ENDPOINT = "http://foo.bar";

  @GET("/download/video")
  Observable<Response> downloadFile();

  public static class MockService implements RetrofitService {
    public static final List<Header> HEADERS = new ArrayList<Header>();

    public static RetrofitService create() {
      RestAdapter adapter = new RestAdapter.Builder()
          .setExecutors(AsyncTask.THREAD_POOL_EXECUTOR, new MainThreadExecutor())
          .setEndpoint(RetrofitService.ENDPOINT)
          .setLogLevel(RestAdapter.LogLevel.BASIC).build();
      MockRestAdapter mockAdapter = MockRestAdapter.from(adapter);
      mockAdapter.setDelay(3000);
      mockAdapter.setErrorPercentage(50);
      mockAdapter.setVariancePercentage(0);
      mockAdapter.create(RetrofitService.class, new MockService());
      return mockAdapter.create(RetrofitService.class, new MockService());
    }

    @Override
    public Observable<Response> downloadFile() {
      return Observable.just(new Response(ENDPOINT + "/download/file", 200, "OK", HEADERS, null));
    }
  }
}
