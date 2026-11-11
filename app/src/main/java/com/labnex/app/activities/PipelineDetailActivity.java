package com.labnex.app.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.labnex.app.R;
import com.labnex.app.adapters.PipelineDetailAdapter;
import com.labnex.app.bottomsheets.CommitDiffsBottomSheet;
import com.labnex.app.bottomsheets.JobDetailBottomSheet;
import com.labnex.app.databinding.ActivityPipelineDetailBinding;
import com.labnex.app.helpers.Toasty;
import com.labnex.app.helpers.UIHelper;
import com.labnex.app.viewmodels.PipelineDetailViewModel;

/**
 * @author mmarif
 */
public class PipelineDetailActivity extends BaseActivity {

	private ActivityPipelineDetailBinding binding;
	private PipelineDetailViewModel viewModel;
	private PipelineDetailAdapter adapter;
	private long projectId;
	private long pipelineId;

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
		binding.pipelineTitle.setText(getString(R.string.text_with_hash, pipelineId));

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
}
