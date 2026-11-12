package com.labnex.app.viewmodels;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.labnex.app.clients.RetrofitClient;
import com.labnex.app.helpers.ApiResponseHandler;
import com.labnex.app.helpers.Constants;
import com.labnex.app.models.ci.CrudeVariable;
import com.labnex.app.models.ci.Variable;
import java.util.ArrayList;
import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * @author mmarif
 */
public class VariablesViewModel extends ViewModel {

	private final MutableLiveData<List<Variable>> variableList = new MutableLiveData<>(null);
	private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);

	private long currentProjectId;
	private int currentPage = 1;
	private final int resultLimit = Constants.getResultLimit();
	private boolean isLastPage = false;
	private boolean isLoadingMore = false;

	private final MutableLiveData<Boolean> isActionLoading = new MutableLiveData<>(false);
	private final MutableLiveData<Boolean> createSuccess = new MutableLiveData<>(false);
	private final MutableLiveData<Boolean> editSuccess = new MutableLiveData<>(false);
	private final MutableLiveData<Boolean> deleteSuccess = new MutableLiveData<>(false);
	private final MutableLiveData<String> error = new MutableLiveData<>();

	public LiveData<List<Variable>> getVariableList() {
		return variableList;
	}

	public LiveData<Boolean> getIsLoading() {
		return isLoading;
	}

	public LiveData<Boolean> getIsActionLoading() {
		return isActionLoading;
	}

	public LiveData<Boolean> getCreateSuccess() {
		return createSuccess;
	}

	public LiveData<Boolean> getEditSuccess() {
		return editSuccess;
	}

	public LiveData<Boolean> getDeleteSuccess() {
		return deleteSuccess;
	}

	public LiveData<String> getError() {
		return error;
	}

	public void clearCreateSuccess() {
		createSuccess.setValue(false);
	}

	public void clearEditSuccess() {
		editSuccess.setValue(false);
	}

	public void clearDeleteSuccess() {
		deleteSuccess.setValue(false);
	}

	public void clearError() {
		error.setValue(null);
	}

	public void loadVariables(Context ctx, long projectId) {
		this.currentProjectId = projectId;
		currentPage = 1;
		isLastPage = false;
		isLoadingMore = false;
		variableList.setValue(null);
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
				.getProjectVariables(currentProjectId, resultLimit, page)
				.enqueue(
						new Callback<>() {
							@Override
							public void onResponse(
									@NonNull Call<List<Variable>> call,
									@NonNull Response<List<Variable>> response) {
								ApiResponseHandler.handleFetch(
										response,
										isLoading,
										() -> {
											String totalHeader = response.headers().get("x-total");
											List<Variable> body = response.body();
											List<Variable> current =
													(page == 1)
															? new ArrayList<>()
															: (variableList.getValue() != null
																	? new ArrayList<>(
																			variableList.getValue())
																	: new ArrayList<>());

											if (body != null) current.addAll(body);
											variableList.setValue(current);

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
									@NonNull Call<List<Variable>> call, @NonNull Throwable t) {
								isLoading.setValue(false);
								isLoadingMore = false;
								if (page == 1) variableList.setValue(new ArrayList<>());
								error.setValue(t.getMessage());
							}
						});
	}

	public void createVariable(Context ctx, long projectId, CrudeVariable payload) {
		isActionLoading.setValue(true);
		RetrofitClient.getApiInterface(ctx)
				.createProjectVariable(projectId, payload)
				.enqueue(
						new Callback<>() {
							@Override
							public void onResponse(
									@NonNull Call<Variable> call,
									@NonNull Response<Variable> response) {
								isActionLoading.setValue(false);
								if (response.isSuccessful()) {
									createSuccess.setValue(true);
									loadVariables(ctx, projectId);
								} else {
									error.setValue(
											ApiResponseHandler.getErrorMessageStatic(response));
								}
							}

							@Override
							public void onFailure(
									@NonNull Call<Variable> call, @NonNull Throwable t) {
								isActionLoading.setValue(false);
								error.setValue(t.getMessage());
							}
						});
	}

	public void updateVariable(Context ctx, long projectId, String key, CrudeVariable payload) {
		isActionLoading.setValue(true);
		RetrofitClient.getApiInterface(ctx)
				.updateProjectVariable(projectId, key, payload)
				.enqueue(
						new Callback<>() {
							@Override
							public void onResponse(
									@NonNull Call<Variable> call,
									@NonNull Response<Variable> response) {
								isActionLoading.setValue(false);
								if (response.isSuccessful()) {
									editSuccess.setValue(true);
									loadVariables(ctx, projectId);
								} else {
									error.setValue(
											ApiResponseHandler.getErrorMessageStatic(response));
								}
							}

							@Override
							public void onFailure(
									@NonNull Call<Variable> call, @NonNull Throwable t) {
								isActionLoading.setValue(false);
								error.setValue(t.getMessage());
							}
						});
	}

	public void deleteVariable(Context ctx, long projectId, String key) {
		isActionLoading.setValue(true);
		RetrofitClient.getApiInterface(ctx)
				.deleteProjectVariable(projectId, key)
				.enqueue(
						new Callback<>() {
							@Override
							public void onResponse(
									@NonNull Call<Void> call, @NonNull Response<Void> response) {
								isActionLoading.setValue(false);
								if (response.isSuccessful()) {
									deleteSuccess.setValue(true);
									loadVariables(ctx, projectId);
								} else {
									error.setValue(
											ApiResponseHandler.getErrorMessageStatic(response));
								}
							}

							@Override
							public void onFailure(@NonNull Call<Void> call, @NonNull Throwable t) {
								isActionLoading.setValue(false);
								error.setValue(t.getMessage());
							}
						});
	}
}
