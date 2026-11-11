package com.labnex.app.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.labnex.app.R;
import com.labnex.app.adapters.PipelineDetailAdapter;
import com.labnex.app.bottomsheets.CommitDiffsBottomSheet;
import com.labnex.app.bottomsheets.GenericMenuBottomSheet;
import com.labnex.app.bottomsheets.JobDetailBottomSheet;
import com.labnex.app.databinding.ActivityPipelineDetailBinding;
import com.labnex.app.helpers.AccessLevel;
import com.labnex.app.helpers.Toasty;
import com.labnex.app.helpers.UIHelper;
import com.labnex.app.models.app.GenericMenuItemModel;
import com.labnex.app.models.ci.PipelineDetail;
import com.labnex.app.viewmodels.PipelineDetailViewModel;
import java.util.ArrayList;
import java.util.List;

/**
 * @author mmarif
 */
public class PipelineDetailActivity extends BaseActivity {

	private ActivityPipelineDetailBinding binding;
	private PipelineDetailViewModel viewModel;
	private PipelineDetailAdapter adapter;
	private long projectId;
	private long pipelineId;
	private boolean pendingDelete = false;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		binding = ActivityPipelineDetailBinding.inflate(getLayoutInflater());
		setContentView(binding.getRoot());

		UIHelper.applyEdgeToEdge(
				this, binding.dockedToolbar, binding.recyclerView, binding.pullToRefresh, null);

		viewModel = new ViewModelProvider(this).get(PipelineDetailViewModel.class);
		projectId = getIntent().getLongExtra("projectId", 0);
		pipelineId = getIntent().getLongExtra("pipelineId", 0);

		viewModel.loadProjectAccess(ctx, projectId);

		binding.btnBack.setOnClickListener(v -> finish());

		setupRecyclerView();
		observeViewModel();

		viewModel.loadPipelineDetail(ctx, projectId, pipelineId);

		binding.pullToRefresh.setOnRefreshListener(
				() -> viewModel.loadPipelineDetail(ctx, projectId, pipelineId));
	}

	private void setupRecyclerView() {
		adapter =
				new PipelineDetailAdapter(
						ctx,
						job -> {
							int accessLevel =
									viewModel.getUserAccessLevel().getValue() != null
											? viewModel.getUserAccessLevel().getValue()
											: 0;
							long currentUserId = getAccount().getUserId();

							JobDetailBottomSheet.newInstance(
											job, projectId, accessLevel, currentUserId)
									.show(getSupportFragmentManager(), "jobDetailSheet");
						},
						userId -> {
							Intent intent = new Intent(ctx, ProfileActivity.class);
							intent.putExtra("source", "ci");
							intent.putExtra("userId", String.valueOf(userId));
							startActivity(intent);
						},
						sha -> {
							Bundle b = new Bundle();
							b.putLong("projectId", projectId);
							b.putString("sha", sha);
							CommitDiffsBottomSheet sheet = new CommitDiffsBottomSheet();
							sheet.setArguments(b);
							sheet.show(getSupportFragmentManager(), "commitDiffsSheet");
						});
		binding.recyclerView.setLayoutManager(new LinearLayoutManager(ctx));
		binding.recyclerView.setAdapter(adapter);
	}

	private void observeViewModel() {
		viewModel
				.getIsLoading()
				.observe(
						this,
						loading -> {
							if (Boolean.TRUE.equals(loading)) {
								binding.pullToRefresh.setRefreshing(false);

								binding.progressBar.setVisibility(View.VISIBLE);
								binding.recyclerView.setVisibility(View.GONE);
								binding.nothingFoundFrame.getRoot().setVisibility(View.GONE);
							} else {
								binding.progressBar.setVisibility(View.GONE);
								binding.pullToRefresh.setRefreshing(false);
							}
						});

		viewModel
				.getPipelineData()
				.observe(
						this,
						data -> {
							if (data == null || data.stages == null || data.stages.isEmpty()) {
								binding.nothingFoundFrame.getRoot().setVisibility(View.VISIBLE);
								binding.recyclerView.setVisibility(View.GONE);
							} else {
								binding.nothingFoundFrame.getRoot().setVisibility(View.GONE);
								binding.recyclerView.setVisibility(View.VISIBLE);
								adapter.setData(data.detail, data.stages, data.jobs);
								setupPipelineMenu(data.detail);
							}
						});

		viewModel
				.getActionSuccess()
				.observe(
						this,
						success -> {
							if (Boolean.TRUE.equals(success)) {
								Toasty.show(ctx, R.string.action_success);
								viewModel.clearActionSuccess();

								if (pendingDelete) {
									pendingDelete = false;
									finish();
								} else {
									viewModel.loadPipelineDetail(ctx, projectId, pipelineId);
								}
							}
						});

		viewModel
				.getActionError()
				.observe(
						this,
						errorMsg -> {
							if (errorMsg != null) {
								Toasty.show(ctx, errorMsg);
								viewModel.clearActionError();
							}
						});

		viewModel
				.getError()
				.observe(
						this,
						error -> {
							if (error != null) {
								Toasty.show(ctx, error);
								viewModel.clearError();
							}
						});
	}

	private void setupPipelineMenu(PipelineDetail detail) {
		binding.dockContainer.removeView(binding.btnPipelineMenu);
		binding.dockContainer.removeView(binding.dockDivider);

		if (detail == null) return;

		Integer accessLevel = viewModel.getUserAccessLevel().getValue();
		int level = accessLevel != null ? accessLevel : 0;
		boolean isMaintainer = level >= AccessLevel.MAINTAINER;

		if (!isMaintainer) return;

		String status = detail.getStatus();
		if (status == null) return;

		List<GenericMenuItemModel> items = new ArrayList<>();

		if ("failed".equalsIgnoreCase(status)
				|| "canceled".equalsIgnoreCase(status)
				|| "success".equalsIgnoreCase(status)) {
			items.add(
					new GenericMenuItemModel(
							"retry",
							R.string.retry,
							R.drawable.ic_refresh,
							com.google.android.material.R.attr.colorPrimaryContainer,
							com.google.android.material.R.attr.colorOnPrimaryContainer));
		}

		if ("running".equalsIgnoreCase(status) || "pending".equalsIgnoreCase(status)) {
			items.add(
					new GenericMenuItemModel(
							"cancel",
							R.string.cancel,
							R.drawable.ic_close,
							com.google.android.material.R.attr.colorErrorContainer,
							com.google.android.material.R.attr.colorOnErrorContainer));
		}

		items.add(
				new GenericMenuItemModel(
						"delete",
						R.string.delete,
						R.drawable.ic_trash,
						com.google.android.material.R.attr.colorErrorContainer,
						com.google.android.material.R.attr.colorOnErrorContainer));

		binding.dockContainer.addView(binding.btnPipelineMenu);
		binding.dockContainer.addView(binding.dockDivider);

		binding.btnPipelineMenu.setOnClickListener(v -> {
			GenericMenuBottomSheet sheet =
					GenericMenuBottomSheet.newInstance(
							getString(R.string.pipeline_details), "#" + pipelineId, items);

			sheet.setOnMenuItemClickListener(id -> {
				if ("delete".equals(id)) {
					new MaterialAlertDialogBuilder(this)
							.setTitle(R.string.delete_pipeline)
							.setMessage(R.string.delete_pipeline_confirmation)
							.setPositiveButton(
									R.string.delete,
									(d, w) -> {
										pendingDelete = true;
										viewModel.deletePipeline(ctx, projectId, pipelineId);
									})
							.setNegativeButton(R.string.cancel, null)
							.show();
				} else if ("retry".equals(id)) {
					viewModel.retryPipeline(ctx, projectId, pipelineId);
				} else if ("cancel".equals(id)) {
					viewModel.cancelPipeline(ctx, projectId, pipelineId);
				}
			});
			sheet.show(getSupportFragmentManager(), "pipelineMenuSheet");
		});
	}
}
