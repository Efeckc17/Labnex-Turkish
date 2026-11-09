package com.labnex.app.activities;

import android.os.Bundle;
import android.view.View;
import androidx.lifecycle.ViewModelProvider;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.labnex.app.R;
import com.labnex.app.bottomsheets.FollowersBottomSheet;
import com.labnex.app.bottomsheets.GenericMenuBottomSheet;
import com.labnex.app.database.api.BaseApi;
import com.labnex.app.database.api.UserAccountsApi;
import com.labnex.app.database.models.UserAccount;
import com.labnex.app.databinding.ActivityProfileBinding;
import com.labnex.app.databinding.ItemProfileDetailBinding;
import com.labnex.app.helpers.Markdown;
import com.labnex.app.helpers.TimeHelper;
import com.labnex.app.helpers.Toasty;
import com.labnex.app.helpers.UIHelper;
import com.labnex.app.helpers.Utils;
import com.labnex.app.models.app.GenericMenuItemModel;
import com.labnex.app.models.user.User;
import com.labnex.app.viewmodels.ProfileViewModel;
import com.vdurmont.emoji.EmojiParser;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * @author mmarif
 */
public class ProfileActivity extends BaseActivity {

	private ActivityProfileBinding binding;
	private ProfileViewModel viewModel;
	private UserAccountsApi userAccountsApi;
	private long profileUserId;
	private boolean isOwnProfile;
	private boolean currentlyFollowed;
	private long myUserId;
	private User currentUser;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		binding = ActivityProfileBinding.inflate(getLayoutInflater());
		setContentView(binding.getRoot());

		UIHelper.applyEdgeToEdge(this, binding.dockedToolbar, binding.mainView, null, null);

		viewModel = new ViewModelProvider(this).get(ProfileViewModel.class);
		userAccountsApi = BaseApi.getInstance(ctx, UserAccountsApi.class);

		binding.btnBack.setOnClickListener(v -> finish());
		observeViewModel();

		int activeAccountId = sharedPrefDB.getInt("currentActiveAccountId", 0);
		UserAccount activeAccount = userAccountsApi.getAccountById(activeAccountId);
		myUserId = (activeAccount != null) ? activeAccount.getUserId() : 0;

		String userIdStr = getIntent().getStringExtra("userId");
		String username = getIntent().getStringExtra("username");

		if (userIdStr != null) {
			profileUserId = Long.parseLong(userIdStr);
			initLoading(profileUserId);
		} else if (username != null && !username.isEmpty()) {
			viewModel.loadUserByUsername(ctx, username);
		} else if (myUserId > 0) {
			profileUserId = myUserId;
			initLoading(profileUserId);
		} else {
			finish();
			return;
		}

		binding.btnFollow.setOnClickListener(v -> {
			if (isOwnProfile) return;
			viewModel.toggleFollow(ctx, profileUserId, myUserId, currentlyFollowed);
		});

		binding.followers.setOnClickListener(v -> {
			if (profileUserId <= 0) return;
			FollowersBottomSheet.newInstance("followers", profileUserId)
					.show(getSupportFragmentManager(), "followersSheet");
		});

		binding.following.setOnClickListener(v -> {
			if (profileUserId <= 0) return;
			FollowersBottomSheet.newInstance("following", profileUserId)
					.show(getSupportFragmentManager(), "followingSheet");
		});
	}

	private void initLoading(long profileUserId) {
		int activeAccountId = sharedPrefDB.getInt("currentActiveAccountId", 0);
		UserAccount activeAccount = userAccountsApi.getAccountById(activeAccountId);
		myUserId = (activeAccount != null) ? activeAccount.getUserId() : 0;
		isOwnProfile = (profileUserId == myUserId);
		viewModel.loadUser(ctx, profileUserId, myUserId);
	}

	private void observeViewModel() {
		viewModel
				.getIsLoading()
				.observe(
						this,
						loading -> {
							int showView = Boolean.TRUE.equals(loading) ? View.VISIBLE : View.GONE;
							binding.progressBar.setVisibility(showView);
							binding.mainView.setAlpha(showView);
						});

		viewModel
				.getUserInfo()
				.observe(
						this,
						user -> {
							if (user != null) populateUI(user);
						});

		viewModel
				.getIsFollowed()
				.observe(
						this,
						followed -> {
							if (followed == null) return;
							currentlyFollowed = followed;
							updateFollowButton();
						});

		viewModel
				.getIsFollowActionLoading()
				.observe(
						this,
						loading -> {
							binding.btnFollow.setEnabled(!Boolean.TRUE.equals(loading));
						});

		viewModel
				.getFollowActionMessage()
				.observe(
						this,
						messageKey -> {
							if (messageKey == null) return;
							int resId =
									"follow_success".equals(messageKey)
											? R.string.follow_success
											: "unfollow_success".equals(messageKey)
													? R.string.unfollow_success
													: R.string.follow_failed;
							Toasty.show(ctx, getString(resId));
							viewModel.clearFollowActionMessage();
						});

		viewModel
				.getFollowersCount()
				.observe(
						this,
						count -> {
							if (count != null && count >= 0) updateFollowersText(count);
						});

		viewModel
				.getFollowingCount()
				.observe(
						this,
						count -> {
							if (count != null && count >= 0) updateFollowingText(count);
						});

		viewModel
				.getError()
				.observe(
						this,
						errorMsg -> {
							if (errorMsg != null) {
								Toasty.show(ctx, getString(R.string.generic_server_response_error));
								viewModel.clearError();
							}
						});
	}

	private void populateUI(User user) {
		this.currentUser = user;
		Glide.with(ctx)
				.load(user.getAvatarUrl())
				.diskCacheStrategy(DiskCacheStrategy.ALL)
				.placeholder(R.drawable.ic_spinner)
				.centerCrop()
				.into(binding.userAvatar);

		binding.fullname.setText(user.getFullName());
		binding.username.setText(getString(R.string.username_with_at_sign, user.getUsername()));

		binding.btnFollow.setVisibility(isOwnProfile ? View.GONE : View.VISIBLE);
		currentlyFollowed = user.isFollowed();
		updateFollowButton();

		if (user.getJobTitle() != null && !user.getJobTitle().isEmpty()) {
			binding.chipJobTitle.setVisibility(View.VISIBLE);
			binding.chipJobTitle.setText(user.getJobTitle());
		}

		binding.chipAdmin.setVisibility(user.isIsAdmin() ? View.VISIBLE : View.GONE);

		if (user.getBio() != null && !user.getBio().trim().isEmpty()) {
			binding.userBioInfo.setVisibility(View.VISIBLE);
			Markdown.render(ctx, EmojiParser.parseToUnicode(user.getBio().trim()), binding.userBio);
		} else {
			binding.userBioInfo.setVisibility(View.GONE);
		}

		setDetail(binding.detailLocation, R.drawable.ic_location, user.getLocation());
		setDetail(binding.detailWebsite, R.drawable.ic_browser, user.getWebsiteUrl());
		setDetail(binding.detailEmail, R.drawable.ic_email, user.getPublicEmail());
		setDetail(binding.detailPronouns, R.drawable.ic_person, user.getPronouns());

		boolean hasDetails =
				binding.detailLocation.getRoot().getVisibility() == View.VISIBLE
						|| binding.detailWebsite.getRoot().getVisibility() == View.VISIBLE
						|| binding.detailEmail.getRoot().getVisibility() == View.VISIBLE
						|| binding.detailPronouns.getRoot().getVisibility() == View.VISIBLE
						|| binding.detailJoined.getRoot().getVisibility() == View.VISIBLE;

		binding.detailsCard.setVisibility(hasDetails ? View.VISIBLE : View.GONE);

		binding.btnFollow.setVisibility(isOwnProfile || user.isBot() ? View.GONE : View.VISIBLE);

		if (user.getCreatedAt() != null) {
			Date d = TimeHelper.parseIso8601(user.getCreatedAt());
			setDetail(
					binding.detailJoined,
					R.drawable.ic_calendar,
					getString(
							R.string.member_since,
							TimeHelper.getAbsoluteDate(d, Locale.getDefault())));
		}

		binding.btnMenu.setOnClickListener(v -> showCopyMenu());

		boolean hasSocial = setupSocialLinks(user);
		binding.socialLinksCard.setVisibility(hasSocial ? View.VISIBLE : View.GONE);
	}

	private void showCopyMenu() {
		if (currentUser == null) return;

		List<GenericMenuItemModel> items = new ArrayList<>();
		items.add(
				new GenericMenuItemModel(
						"copy_id",
						R.string.copy_user_id,
						R.drawable.ic_info,
						com.google.android.material.R.attr.colorPrimaryContainer,
						com.google.android.material.R.attr.colorOnPrimaryContainer));
		items.add(
				new GenericMenuItemModel(
						"copy_username",
						R.string.copy_username,
						R.drawable.ic_person,
						com.google.android.material.R.attr.colorPrimaryContainer,
						com.google.android.material.R.attr.colorOnPrimaryContainer));
		items.add(
				new GenericMenuItemModel(
						"copy_profile_url",
						R.string.copy_profile_url,
						R.drawable.ic_link,
						com.google.android.material.R.attr.colorPrimaryContainer,
						com.google.android.material.R.attr.colorOnPrimaryContainer));

		GenericMenuBottomSheet sheet =
				GenericMenuBottomSheet.newInstance(getString(R.string.copy_info), null, items);
		sheet.setOnMenuItemClickListener(id -> {
			switch (id) {
				case "copy_id":
					Utils.copyToClipboard(
							ctx,
							String.valueOf(currentUser.getId()),
							getString(R.string.copied_to_clipboard));
					break;
				case "copy_username":
					Utils.copyToClipboard(
							ctx,
							currentUser.getUsername(),
							getString(R.string.copied_to_clipboard));
					break;
				case "copy_profile_url":
					Utils.copyToClipboard(
							ctx, currentUser.getWebUrl(), getString(R.string.copied_to_clipboard));
					break;
			}
		});
		sheet.show(getSupportFragmentManager(), "copyInfoSheet");
	}

	private boolean setupSocialLinks(User user) {
		boolean hasDiscord = false;
		boolean hasLinkedin = false;
		boolean hasTwitter = false;

		if (user.getDiscord() != null && !user.getDiscord().isEmpty()) {
			setDetail(
					binding.detailDiscord,
					R.drawable.ic_discord,
					getString(R.string.discord_user_profile_link, user.getDiscord()));
			hasDiscord = true;
		}

		if (user.getLinkedin() != null && !user.getLinkedin().isEmpty()) {
			setDetail(binding.detailLinkedin, R.drawable.ic_linkedin, user.getLinkedin());
			hasLinkedin = true;
		}

		if (user.getTwitter() != null && !user.getTwitter().isEmpty()) {
			setDetail(binding.detailTwitter, R.drawable.ic_x, user.getTwitter());
			hasTwitter = true;
		}

		return hasDiscord || hasLinkedin || hasTwitter;
	}

	private void setDetail(ItemProfileDetailBinding b, int icon, String text) {
		if (text == null || text.isEmpty()) {
			b.getRoot().setVisibility(View.GONE);
			return;
		}
		b.getRoot().setVisibility(View.VISIBLE);
		b.detailIcon.setImageResource(icon);
		b.detailText.setText(text);
	}

	private void updateFollowButton() {
		binding.btnFollow.setText(currentlyFollowed ? R.string.unfollow : R.string.follow);
	}

	private void updateFollowersText(int count) {
		binding.followers.setText(getString(R.string.user_followers, Utils.numberFormatter(count)));
		updateSocialCardVisibility();
	}

	private void updateFollowingText(int count) {
		binding.following.setText(getString(R.string.user_following, Utils.numberFormatter(count)));
		updateSocialCardVisibility();
	}

	private void updateSocialCardVisibility() {
		Integer f = viewModel.getFollowersCount().getValue();
		Integer g = viewModel.getFollowingCount().getValue();
		int followers = f != null ? f : 0;
		int following = g != null ? g : 0;
		binding.socialMetricsCard.setVisibility(
				(followers > 0 || following > 0) ? View.VISIBLE : View.GONE);
	}
}
