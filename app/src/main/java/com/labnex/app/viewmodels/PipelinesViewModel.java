package com.labnex.app.viewmodels;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.labnex.app.clients.RetrofitClient;
import com.labnex.app.helpers.ApiResponseHandler;
import com.labnex.app.helpers.Constants;
import com.labnex.app.models.ci.Pipeline;
import java.util.ArrayList;
import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * @author mmarif
 */
public class PipelinesViewModel extends ViewModel {
	private final MutableLiveData<List<Pipeline>> pipelinesList =
			new MutableLiveData<>(new ArrayList<>());
	private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
	private final MutableLiveData<String> error = new MutableLiveData<>();

	private long projectId;
	private String ref;
	private int currentPage = 1;
	private final int resultLimit = Constants.getResultLimit();
	private boolean isLastPage = false;
	private boolean isLoadingMore = false;

	public LiveData<List<Pipeline>> getPipelinesList() {
		return pipelinesList;
	}

	public LiveData<Boolean> getIsLoading() {
		return isLoading;
	}

	public LiveData<String> getError() {
		return error;
	}

	public void clearError() {
		error.setValue(null);
	}

	public void loadPipelines(Context ctx, long projectId, String ref) {
		this.projectId = projectId;
		this.ref = ref;
		currentPage = 1;
		isLastPage = false;
		isLoadingMore = false;
		isLoading.setValue(true);
		fetch(ctx, 1);
	}

	public void loadNextPage(Context ctx) {
		if (isLoadingMore || isLastPage) return;
		isLoadingMore = true;
		currentPage++;
		fetch(ctx, currentPage);
	}

	private void fetch(Context ctx, int page) {
		RetrofitClient.getApiInterface(ctx)
				.getProjectPipelines(projectId, ref, resultLimit, page)
				.enqueue(
						new Callback<>() {
							@Override
							public void onResponse(
									@NonNull Call<List<Pipeline>> call,
									@NonNull Response<List<Pipeline>> response) {
								ApiResponseHandler.handleFetch(
										response,
										isLoading,
										() -> {
											String totalHeader = response.headers().get("x-total");
											List<Pipeline> body = response.body();
											List<Pipeline> current =
													(page == 1)
															? new ArrayList<>()
															: (pipelinesList.getValue() != null
																	? new ArrayList<>(
																			pipelinesList
																					.getValue())
																	: new ArrayList<>());
											if (body != null) current.addAll(body);
											pipelinesList.setValue(current);

											if (body != null && body.size() < resultLimit)
												isLastPage = true;
											else if (totalHeader != null) {
												try {
													if (current.size()
															>= Integer.parseInt(totalHeader))
														isLastPage = true;
												} catch (NumberFormatException ignored) {
												}
											}
										},
										error);
								isLoadingMore = false;
							}

							@Override
							public void onFailure(
									@NonNull Call<List<Pipeline>> call, @NonNull Throwable t) {
								isLoading.setValue(false);
								isLoadingMore = false;
								if (page == 1) pipelinesList.setValue(new ArrayList<>());
								error.setValue(t.getMessage());
							}
						});
	}
}
