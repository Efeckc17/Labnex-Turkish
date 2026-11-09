package com.labnex.app.viewmodels;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.labnex.app.clients.RetrofitClient;
import com.labnex.app.helpers.ApiResponseHandler;
import com.labnex.app.models.user.User;
import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * @author mmarif
 */
public class ProfileViewModel extends ViewModel {

	private final MutableLiveData<User> userInfo = new MutableLiveData<>();
	private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
	private final MutableLiveData<String> error = new MutableLiveData<>();
	private final MutableLiveData<Boolean> isFollowActionLoading = new MutableLiveData<>(false);
	private final MutableLiveData<Boolean> isFollowed = new MutableLiveData<>(null);
	private final MutableLiveData<String> followActionMessage = new MutableLiveData<>();

	public LiveData<Boolean> getIsFollowActionLoading() {
		return isFollowActionLoading;
	}

	public LiveData<Boolean> getIsFollowed() {
		return isFollowed;
	}

	public LiveData<String> getFollowActionMessage() {
		return followActionMessage;
	}

	public void clearFollowActionMessage() {
		followActionMessage.setValue(null);
	}

	private final MutableLiveData<Integer> followersCount = new MutableLiveData<>(-1);
	private final MutableLiveData<Integer> followingCount = new MutableLiveData<>(-1);

	public LiveData<Integer> getFollowersCount() {
		return followersCount;
	}

	public LiveData<Integer> getFollowingCount() {
		return followingCount;
	}

	public LiveData<User> getUserInfo() {
		return userInfo;
	}

	public LiveData<Boolean> getIsLoading() {
		return isLoading;
	}

	public LiveData<String> getError() {
		return error;
	}

	public void loadUser(Context ctx, long userId, long currentLoggedInId) {
		isLoading.setValue(true);
		fetchFollowersCount(ctx, userId);
		fetchFollowingCount(ctx, userId);

		if (userId == currentLoggedInId) {
			fetchPrivateProfile(ctx, userId);
		} else {
			fetchPublicProfile(ctx, userId);
		}
	}

	private void fetchPrivateProfile(Context ctx, long userId) {
		RetrofitClient.getApiInterface(ctx)
				.getCurrentUser()
				.enqueue(
						new Callback<>() {
							@Override
							public void onResponse(
									@NonNull Call<User> c, @NonNull Response<User> r) {
								if (r.isSuccessful() && r.body() != null) {
									userInfo.setValue(r.body());
									fetchPublicProfile(ctx, userId);
								} else {
									isLoading.setValue(false);
									error.setValue(ApiResponseHandler.getErrorMessageStatic(r));
								}
							}

							@Override
							public void onFailure(@NonNull Call<User> c, @NonNull Throwable t) {
								isLoading.setValue(false);
								error.setValue(t.getMessage());
							}
						});
	}

	private void fetchPublicProfile(Context ctx, long userId) {
		RetrofitClient.getApiInterface(ctx)
				.getSingleUser(userId)
				.enqueue(
						new Callback<>() {
							@Override
							public void onResponse(
									@NonNull Call<User> c, @NonNull Response<User> r) {
								isLoading.setValue(false);
								if (r.isSuccessful() && r.body() != null) {
									userInfo.setValue(r.body());
								} else {
									error.setValue(ApiResponseHandler.getErrorMessageStatic(r));
								}
							}

							@Override
							public void onFailure(@NonNull Call<User> c, @NonNull Throwable t) {
								isLoading.setValue(false);
								error.setValue(t.getMessage());
							}
						});
	}

	public void clearError() {
		error.setValue(null);
	}

	public void loadUserByUsername(Context ctx, String username) {
		if (ctx == null) return;
		isLoading.setValue(true);

		RetrofitClient.getApiInterface(ctx)
				.getUsersByUsername(username)
				.enqueue(
						new Callback<>() {
							@Override
							public void onResponse(
									@NonNull Call<List<User>> c, @NonNull Response<List<User>> r) {
								if (r.isSuccessful() && r.body() != null && !r.body().isEmpty()) {
									User user = r.body().get(0);
									loadUser(ctx, user.getId(), -1);
								} else {
									isLoading.setValue(false);
									error.setValue("not_found");
								}
							}

							@Override
							public void onFailure(
									@NonNull Call<List<User>> c, @NonNull Throwable t) {
								isLoading.setValue(false);
								error.setValue(t.getMessage());
							}
						});
	}

	public void toggleFollow(
			Context ctx, long userId, long currentLoggedInId, boolean currentlyFollowed) {
		if (Boolean.TRUE.equals(isFollowActionLoading.getValue())) return;
		isFollowActionLoading.setValue(true);

		Call<Void> call =
				currentlyFollowed
						? RetrofitClient.getApiInterface(ctx).unfollowUser(userId)
						: RetrofitClient.getApiInterface(ctx).followUser(userId);

		call.enqueue(
				new Callback<>() {
					@Override
					public void onResponse(@NonNull Call<Void> c, @NonNull Response<Void> r) {
						isFollowActionLoading.setValue(false);
						if (r.isSuccessful()) {
							isFollowed.setValue(!currentlyFollowed);
							followActionMessage.setValue(
									!currentlyFollowed ? "follow_success" : "unfollow_success");
							loadUser(ctx, userId, currentLoggedInId);
						} else {
							followActionMessage.setValue("follow_failed");
						}
					}

					@Override
					public void onFailure(@NonNull Call<Void> c, @NonNull Throwable t) {
						isFollowActionLoading.setValue(false);
						followActionMessage.setValue("follow_failed");
					}
				});
	}

	private void fetchFollowersCount(Context ctx, long userId) {
		RetrofitClient.getApiInterface(ctx)
				.getFollowers(userId, 1, 1)
				.enqueue(
						new Callback<>() {
							@Override
							public void onResponse(
									@NonNull Call<List<User>> c, @NonNull Response<List<User>> r) {
								followersCount.setValue(
										r.isSuccessful()
												? parseTotalHeader(r.headers().get("x-total"))
												: 0);
							}

							@Override
							public void onFailure(
									@NonNull Call<List<User>> c, @NonNull Throwable t) {
								followersCount.setValue(0);
							}
						});
	}

	private void fetchFollowingCount(Context ctx, long userId) {
		RetrofitClient.getApiInterface(ctx)
				.getFollowing(userId, 1, 1)
				.enqueue(
						new Callback<>() {
							@Override
							public void onResponse(
									@NonNull Call<List<User>> c, @NonNull Response<List<User>> r) {
								followingCount.setValue(
										r.isSuccessful()
												? parseTotalHeader(r.headers().get("x-total"))
												: 0);
							}

							@Override
							public void onFailure(
									@NonNull Call<List<User>> c, @NonNull Throwable t) {
								followingCount.setValue(0);
							}
						});
	}

	private int parseTotalHeader(String header) {
		if (header != null) {
			try {
				return Integer.parseInt(header);
			} catch (NumberFormatException e) {
				return 0;
			}
		}
		return 0;
	}
}
