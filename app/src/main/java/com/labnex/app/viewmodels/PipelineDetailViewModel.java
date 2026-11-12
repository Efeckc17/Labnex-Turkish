package com.labnex.app.viewmodels;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.labnex.app.clients.RetrofitClient;
import com.labnex.app.helpers.AccessLevel;
import com.labnex.app.helpers.ApiResponseHandler;
import com.labnex.app.models.ci.Job;
import com.labnex.app.models.ci.PipelineDetail;
import com.labnex.app.models.projects.Projects;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * @author mmarif
 */
public class PipelineDetailViewModel extends ViewModel {

	public static class PipelineData {
		public PipelineDetail detail;
		public List<String> stages;
		public List<Job> jobs;

		public PipelineData(PipelineDetail detail, List<String> stages, List<Job> jobs) {
			this.detail = detail;
			this.stages = stages;
			this.jobs = jobs;
		}
	}

	private final MutableLiveData<PipelineData> pipelineData = new MutableLiveData<>();
	private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
	private final MutableLiveData<String> error = new MutableLiveData<>();
	private final MutableLiveData<Integer> userAccessLevel = new MutableLiveData<>(0);
	private final MutableLiveData<Boolean> isActionLoading = new MutableLiveData<>(false);
	private final MutableLiveData<Boolean> actionSuccess = new MutableLiveData<>(false);
	private final MutableLiveData<String> actionError = new MutableLiveData<>();

	public LiveData<Boolean> getIsActionLoading() {
		return isActionLoading;
	}

	public LiveData<Boolean> getActionSuccess() {
		return actionSuccess;
	}

	public LiveData<String> getActionError() {
		return actionError;
	}

	public LiveData<PipelineData> getPipelineData() {
		return pipelineData;
	}

	public LiveData<Boolean> getIsLoading() {
		return isLoading;
	}

	public LiveData<String> getError() {
		return error;
	}

	public LiveData<Integer> getUserAccessLevel() {
		return userAccessLevel;
	}

	public void clearError() {
		error.setValue(null);
	}

	public void clearActionSuccess() {
		actionSuccess.setValue(false);
	}

	public void clearActionError() {
		actionError.setValue(null);
	}

	public void loadProjectAccess(Context ctx, long projectId) {
		RetrofitClient.getApiInterface(ctx)
				.getProjectInfo(projectId)
				.enqueue(
						new Callback<>() {
							@Override
							public void onResponse(
									@NonNull Call<Projects> call,
									@NonNull Response<Projects> response) {
								if (response.isSuccessful() && response.body() != null) {
									int level = AccessLevel.getUserAccessLevel(response.body());
									userAccessLevel.setValue(level);
								} else {
									userAccessLevel.setValue(0);
								}
							}

							@Override
							public void onFailure(
									@NonNull Call<Projects> call, @NonNull Throwable t) {
								userAccessLevel.setValue(0);
							}
						});
	}

	public void loadPipelineDetail(Context ctx, long projectId, long pipelineId) {
		isLoading.setValue(true);

		RetrofitClient.getApiInterface(ctx)
				.getPipelineJobs(projectId, pipelineId, 100, 1)
				.enqueue(
						new Callback<>() {
							@Override
							public void onResponse(
									@NonNull Call<List<Job>> call,
									@NonNull Response<List<Job>> response) {
								if (response.isSuccessful() && response.body() != null) {
									List<Job> jobs = response.body();
									Set<String> stageSet = new LinkedHashSet<>();
									for (Job job : jobs) {
										if (job.getStage() != null) stageSet.add(job.getStage());
									}
									List<String> stages = new ArrayList<>(stageSet);

									fetchPipelineDetails(ctx, projectId, pipelineId, stages, jobs);
								} else {
									isLoading.setValue(false);
									error.setValue("Failed to load jobs");
								}
							}

							@Override
							public void onFailure(
									@NonNull Call<List<Job>> call, @NonNull Throwable t) {
								isLoading.setValue(false);
								error.setValue(t.getMessage());
							}
						});
	}

	private void fetchPipelineDetails(
			Context ctx, long projectId, long pipelineId, List<String> stages, List<Job> jobs) {
		RetrofitClient.getApiInterface(ctx)
				.getPipelineDetail(projectId, pipelineId)
				.enqueue(
						new Callback<>() {
							@Override
							public void onResponse(
									@NonNull Call<PipelineDetail> call,
									@NonNull Response<PipelineDetail> response) {
								isLoading.setValue(false);
								if (response.isSuccessful() && response.body() != null) {
									pipelineData.setValue(
											new PipelineData(response.body(), stages, jobs));
								} else {
									pipelineData.setValue(new PipelineData(null, stages, jobs));
								}
							}

							@Override
							public void onFailure(
									@NonNull Call<PipelineDetail> call, @NonNull Throwable t) {
								isLoading.setValue(false);
								pipelineData.setValue(new PipelineData(null, stages, jobs));
							}
						});
	}

	public void retryPipeline(Context ctx, long projectId, long pipelineId) {
		if (Boolean.TRUE.equals(isActionLoading.getValue())) return;
		isActionLoading.setValue(true);

		RetrofitClient.getApiInterface(ctx)
				.retryPipeline(projectId, pipelineId)
				.enqueue(
						new Callback<>() {
							@Override
							public void onResponse(
									@NonNull Call<PipelineDetail> call,
									@NonNull Response<PipelineDetail> response) {
								isActionLoading.setValue(false);
								if (response.isSuccessful()) {
									actionSuccess.setValue(true);
								} else {
									actionError.setValue(
											ApiResponseHandler.getErrorMessageStatic(response));
								}
							}

							@Override
							public void onFailure(
									@NonNull Call<PipelineDetail> call, @NonNull Throwable t) {
								isActionLoading.setValue(false);
								actionError.setValue(t.getMessage());
							}
						});
	}

	public void cancelPipeline(Context ctx, long projectId, long pipelineId) {
		if (Boolean.TRUE.equals(isActionLoading.getValue())) return;
		isActionLoading.setValue(true);

		RetrofitClient.getApiInterface(ctx)
				.cancelPipeline(projectId, pipelineId)
				.enqueue(
						new Callback<>() {
							@Override
							public void onResponse(
									@NonNull Call<PipelineDetail> call,
									@NonNull Response<PipelineDetail> response) {
								isActionLoading.setValue(false);
								if (response.isSuccessful()) {
									actionSuccess.setValue(true);
								} else {
									actionError.setValue(
											ApiResponseHandler.getErrorMessageStatic(response));
								}
							}

							@Override
							public void onFailure(
									@NonNull Call<PipelineDetail> call, @NonNull Throwable t) {
								isActionLoading.setValue(false);
								actionError.setValue(t.getMessage());
							}
						});
	}

	public void deletePipeline(Context ctx, long projectId, long pipelineId) {
		if (Boolean.TRUE.equals(isActionLoading.getValue())) return;
		isActionLoading.setValue(true);

		RetrofitClient.getApiInterface(ctx)
				.deletePipeline(projectId, pipelineId)
				.enqueue(
						new Callback<>() {
							@Override
							public void onResponse(
									@NonNull Call<Void> call, @NonNull Response<Void> response) {
								isActionLoading.setValue(false);
								if (response.isSuccessful()) {
									actionSuccess.setValue(true);
								} else {
									actionError.setValue(
											ApiResponseHandler.getErrorMessageStatic(response));
								}
							}

							@Override
							public void onFailure(@NonNull Call<Void> call, @NonNull Throwable t) {
								isActionLoading.setValue(false);
								actionError.setValue(t.getMessage());
							}
						});
	}
}
