package com.labnex.app.viewmodels;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.labnex.app.clients.RetrofitClient;
import com.labnex.app.helpers.ApiResponseHandler;
import com.labnex.app.models.ci.Job;
import java.io.IOException;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * @author mmarif
 */
public class JobDetailViewModel extends ViewModel {

	private final MutableLiveData<Boolean> isActionLoading = new MutableLiveData<>(false);
	private final MutableLiveData<Boolean> actionSuccess = new MutableLiveData<>(false);
	private final MutableLiveData<String> error = new MutableLiveData<>();
	private final MutableLiveData<String> traceLog = new MutableLiveData<>();
	private final MutableLiveData<Boolean> isTraceLoading = new MutableLiveData<>(false);

	public LiveData<Boolean> getIsActionLoading() {
		return isActionLoading;
	}

	public LiveData<Boolean> getActionSuccess() {
		return actionSuccess;
	}

	public LiveData<String> getError() {
		return error;
	}

	public LiveData<String> getTraceLog() {
		return traceLog;
	}

	public LiveData<Boolean> getIsTraceLoading() {
		return isTraceLoading;
	}

	public void clearActionSuccess() {
		actionSuccess.setValue(false);
	}

	public void clearError() {
		error.setValue(null);
	}

	public void loadTraceLog(Context ctx, long projectId, long jobId) {
		isTraceLoading.setValue(true);

		RetrofitClient.getApiInterface(ctx)
				.getJobTrace(projectId, jobId)
				.enqueue(
						new Callback<>() {
							@Override
							public void onResponse(
									@NonNull Call<ResponseBody> call,
									@NonNull Response<ResponseBody> response) {
								isTraceLoading.setValue(false);
								ResponseBody body = response.body();

								if (response.isSuccessful() && body != null) {
									try (ResponseBody closable = body) {
										traceLog.setValue(closable.string());
									} catch (IOException e) {
										traceLog.setValue("ERROR_READING_LOG");
									}
								} else {
									traceLog.setValue("");
								}
							}

							@Override
							public void onFailure(
									@NonNull Call<ResponseBody> call, @NonNull Throwable t) {
								isTraceLoading.setValue(false);
								traceLog.setValue("ERROR: " + t.getMessage());
							}
						});
	}

	public void performAction(Context ctx, long projectId, long jobId, String action) {
		if (Boolean.TRUE.equals(isActionLoading.getValue())) return;
		isActionLoading.setValue(true);

		Call<Job> call;
		switch (action) {
			case "retry":
				call = RetrofitClient.getApiInterface(ctx).retryJob(projectId, jobId);
				break;
			case "cancel":
				call = RetrofitClient.getApiInterface(ctx).cancelJob(projectId, jobId);
				break;
			case "play":
				call = RetrofitClient.getApiInterface(ctx).playJob(projectId, jobId);
				break;
			default:
				isActionLoading.setValue(false);
				return;
		}

		call.enqueue(
				new Callback<>() {
					@Override
					public void onResponse(
							@NonNull Call<Job> call, @NonNull Response<Job> response) {
						isActionLoading.setValue(false);
						if (response.isSuccessful()) {
							actionSuccess.setValue(true);
						} else {
							error.setValue(ApiResponseHandler.getErrorMessageStatic(response));
						}
					}

					@Override
					public void onFailure(@NonNull Call<Job> call, @NonNull Throwable t) {
						isActionLoading.setValue(false);
						error.setValue(t.getMessage());
					}
				});
	}
}
