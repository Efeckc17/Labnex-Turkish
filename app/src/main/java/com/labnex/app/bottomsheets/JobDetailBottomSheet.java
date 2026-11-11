package com.labnex.app.bottomsheets;

import android.app.Dialog;
import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.widget.NestedScrollView;
import androidx.lifecycle.ViewModelProvider;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.gson.Gson;
import com.labnex.app.R;
import com.labnex.app.databinding.BottomsheetJobDetailBinding;
import com.labnex.app.helpers.AccessLevel;
import com.labnex.app.helpers.AvatarGenerator;
import com.labnex.app.helpers.Toasty;
import com.labnex.app.helpers.UIHelper;
import com.labnex.app.helpers.Utils;
import com.labnex.app.models.ci.Job;
import com.labnex.app.viewmodels.JobDetailViewModel;

/**
 * @author mmarif
 */
public class JobDetailBottomSheet extends BottomSheetDialogFragment {

	private BottomsheetJobDetailBinding binding;
	private JobDetailViewModel viewModel;
	private Job job;
	private long projectId;
	private boolean isScrolledToBottom = false;
	private int userAccessLevel;
	private long currentUserId;

	public static JobDetailBottomSheet newInstance(
			Job job, long projectId, int accessLevel, long userId) {
		JobDetailBottomSheet sheet = new JobDetailBottomSheet();
		Bundle args = new Bundle();
		args.putString("job_json", new Gson().toJson(job));
		args.putLong("projectId", projectId);
		args.putInt("accessLevel", accessLevel);
		args.putLong("userId", userId);
		sheet.setArguments(args);
		return sheet;
	}

	@Nullable @Override
	public View onCreateView(
			@NonNull LayoutInflater inflater,
			@Nullable ViewGroup container,
			@Nullable Bundle savedInstanceState) {
		binding = BottomsheetJobDetailBinding.inflate(inflater, container, false);
		return binding.getRoot();
	}

	@Override
	public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);

		viewModel = new ViewModelProvider(this).get(JobDetailViewModel.class);

		if (getArguments() != null) {
			String jobJson = getArguments().getString("job_json");
			if (jobJson != null) {
				job = new Gson().fromJson(jobJson, Job.class);
			}
			projectId = getArguments().getLong("projectId");
			userAccessLevel = getArguments().getInt("accessLevel", 0);
			currentUserId = getArguments().getLong("userId", 0);
		}

		if (job == null) {
			dismiss();
			return;
		}

		setupUI();
		setupScrollToggle();
		loadTraceLog();
		setupActions();
		observeViewModel();
	}

	private void setupUI() {
		binding.jobNameTitle.setText(job.getName());

		String status = job.getStatus();
		int colorResId = Utils.getCiColorResId(status);
		int finalColor = ContextCompat.getColor(requireContext(), colorResId);
		String displayStatus = requireContext().getString(Utils.getCiStatusStringRes(status));
		binding.jobStatusBadge.setImageDrawable(
				AvatarGenerator.getLabelDrawable(requireContext(), displayStatus, finalColor, 24));

		String shortSha =
				job.getCommit() != null && job.getCommit().getShortId() != null
						? job.getCommit().getShortId()
						: "";
		String metadata =
				requireContext()
						.getString(
								R.string.job_metadata_format,
								job.getStage(),
								shortSha,
								job.getRef());
		binding.jobMetadata.setText(metadata);
	}

	private void loadTraceLog() {
		viewModel.loadTraceLog(requireContext(), projectId, job.getId());
	}

	private void applyLogHighlighting(String rawLog) {
		SpannableStringBuilder builder = new SpannableStringBuilder(rawLog);

		int errorColor = ContextCompat.getColor(requireContext(), R.color.ci_red);
		int successColor = ContextCompat.getColor(requireContext(), R.color.ci_green);
		int warningColor = ContextCompat.getColor(requireContext(), R.color.ci_orange);

		String[] lines = rawLog.split("\n");
		int currentIndex = 0;

		for (String line : lines) {
			if (line.contains("ERROR") || line.contains("Failed") || line.contains("FAILED")) {
				builder.setSpan(
						new ForegroundColorSpan(errorColor),
						currentIndex,
						currentIndex + line.length(),
						Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
			} else if (line.contains("WARNING") || line.contains("WARN")) {
				builder.setSpan(
						new ForegroundColorSpan(warningColor),
						currentIndex,
						currentIndex + line.length(),
						Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
			} else if (line.contains("Job succeeded") || line.contains("SUCCESS")) {
				builder.setSpan(
						new ForegroundColorSpan(successColor),
						currentIndex,
						currentIndex + line.length(),
						Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
			}

			currentIndex += line.length() + 1;
		}

		binding.traceLog.setText(builder);
	}

	private void setupActions() {
		binding.btnClose.setOnClickListener(v -> dismiss());

		binding.dockContainer.removeView(binding.btnRetry);
		binding.dockContainer.removeView(binding.btnCancel);
		binding.dockContainer.removeView(binding.btnPlay);
		binding.dockContainer.removeView(binding.dockDivider);

		boolean isArchived = job.getPipeline() != null && job.getPipeline().isArchived();
		if (isArchived) return;

		boolean isMaintainer = userAccessLevel >= AccessLevel.MAINTAINER;
		boolean isDeveloper = userAccessLevel == AccessLevel.DEVELOPER;
		boolean isJobOwner = job.getUser() != null && job.getUser().getId() == currentUserId;

		boolean canModify = isMaintainer || (isDeveloper && isJobOwner);
		if (!canModify) return;

		String status = job.getStatus();
		if (status == null) return;

		boolean showCancel =
				"running".equalsIgnoreCase(status)
						|| "pending".equalsIgnoreCase(status)
						|| "canceling".equalsIgnoreCase(status);
		boolean showRetry =
				"failed".equalsIgnoreCase(status)
						|| "canceled".equalsIgnoreCase(status)
						|| "success".equalsIgnoreCase(status);
		boolean showPlay = "manual".equalsIgnoreCase(status);

		if (showCancel || showRetry || showPlay) {
			binding.dockContainer.addView(binding.dockDivider);
		}

		if (showCancel) {
			binding.dockContainer.addView(binding.btnCancel);
			binding.btnCancel.setOnClickListener(v ->
					viewModel.performAction(requireContext(), projectId, job.getId(), "cancel"));
		}

		if (showRetry) {
			binding.dockContainer.addView(binding.btnRetry);
			if ("success".equalsIgnoreCase(status)) {
				binding.btnRetry.setText(R.string.run_again);
			} else {
				binding.btnRetry.setText(R.string.retry);
			}
			binding.btnRetry.setOnClickListener(v ->
					viewModel.performAction(requireContext(), projectId, job.getId(), "retry"));
		}

		if (showPlay) {
			binding.dockContainer.addView(binding.btnPlay);
			binding.btnPlay.setOnClickListener(
					v -> viewModel.performAction(requireContext(), projectId, job.getId(), "play"));
		}
	}

	private void observeViewModel() {
		viewModel
				.getIsActionLoading()
				.observe(
						getViewLifecycleOwner(),
						isLoading -> {
							boolean enabled = !Boolean.TRUE.equals(isLoading);
							binding.btnRetry.setEnabled(enabled);
							binding.btnCancel.setEnabled(enabled);
							binding.btnPlay.setEnabled(enabled);
						});

		viewModel
				.getActionSuccess()
				.observe(
						getViewLifecycleOwner(),
						success -> {
							if (Boolean.TRUE.equals(success)) {
								Toasty.show(requireContext(), R.string.action_success);
								viewModel.clearActionSuccess();
								dismiss();
							}
						});

		viewModel
				.getIsTraceLoading()
				.observe(
						getViewLifecycleOwner(),
						isLoading -> {
							if (Boolean.TRUE.equals(isLoading)) {
								binding.traceLog.setText(R.string.loading_log);
							}
						});

		viewModel
				.getTraceLog()
				.observe(
						getViewLifecycleOwner(),
						rawLog -> {
							if (rawLog == null) return;

							if (rawLog.isEmpty()) {
								binding.traceLog.setText(R.string.log_empty);
							} else if ("ERROR_READING_LOG".equals(rawLog)) {
								binding.traceLog.setText(R.string.log_failed_to_read);
							} else if (rawLog.startsWith("ERROR: ")) {
								binding.traceLog.setText(
										requireContext()
												.getString(
														R.string.log_failed_to_load,
														rawLog.substring(7)));
							} else {
								applyLogHighlighting(rawLog);
							}
						});

		viewModel
				.getError()
				.observe(
						getViewLifecycleOwner(),
						errorMsg -> {
							if (errorMsg != null) {
								Toasty.show(requireContext(), errorMsg);
								viewModel.clearError();
							}
						});
	}

	private void setupScrollToggle() {
		binding.fabScrollToggle.setOnClickListener(v -> {
			if (isScrolledToBottom) {
				binding.scrollView.smoothScrollTo(0, 0);
				isScrolledToBottom = false;
				binding.fabScrollToggle.setImageResource(R.drawable.ic_arrow_down);
			} else {
				binding.scrollView.post(() -> {
					binding.scrollView.fullScroll(View.FOCUS_DOWN);
					isScrolledToBottom = true;
					binding.fabScrollToggle.setImageResource(R.drawable.ic_arrow_up);
				});
			}
		});

		binding.scrollView.setOnScrollChangeListener(
				(NestedScrollView.OnScrollChangeListener)
						(v, scrollX, scrollY, oldScrollX, oldScrollY) -> {
							int contentHeight = v.getChildAt(0).getHeight();
							int viewHeight = v.getHeight();

							if (contentHeight <= viewHeight) {
								binding.fabScrollToggle.setVisibility(View.GONE);
								return;
							}
							binding.fabScrollToggle.setVisibility(View.VISIBLE);

							int maxScroll = contentHeight - viewHeight;

							if (scrollY >= maxScroll - 50) {
								isScrolledToBottom = true;
								binding.fabScrollToggle.setImageResource(R.drawable.ic_arrow_up);
							} else if (scrollY <= 50) {
								isScrolledToBottom = false;
								binding.fabScrollToggle.setImageResource(R.drawable.ic_arrow_down);
							}
						});
	}

	@Override
	public void onStart() {
		super.onStart();
		Dialog dialog = getDialog();
		if (dialog instanceof BottomSheetDialog) {
			UIHelper.applyFullScreenSheetStyle((BottomSheetDialog) dialog, false);
		}
	}

	@Override
	public void onDestroyView() {
		super.onDestroyView();
		binding = null;
	}
}
