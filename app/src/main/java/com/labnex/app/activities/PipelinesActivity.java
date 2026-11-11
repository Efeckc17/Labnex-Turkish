package com.labnex.app.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.labnex.app.adapters.PipelinesAdapter;
import com.labnex.app.databinding.ActivityPipelinesBinding;
import com.labnex.app.helpers.EndlessRecyclerViewScrollListener;
import com.labnex.app.helpers.Toasty;
import com.labnex.app.helpers.UIHelper;
import com.labnex.app.viewmodels.PipelinesViewModel;
import java.util.ArrayList;

/**
 * @author mmarif
 */
public class PipelinesActivity extends BaseActivity {

	private ActivityPipelinesBinding binding;
	private PipelinesViewModel viewModel;
	private PipelinesAdapter adapter;
	private long projectId;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		binding = ActivityPipelinesBinding.inflate(getLayoutInflater());
		setContentView(binding.getRoot());

		UIHelper.applyEdgeToEdge(
				this, binding.dockedToolbar, binding.recyclerView, binding.pullToRefresh, null);

		viewModel = new ViewModelProvider(this).get(PipelinesViewModel.class);
		projectId = getIntent().getLongExtra("projectId", 0);

		binding.btnBack.setOnClickListener(v -> finish());

		setupRecyclerView();
		setupPullToRefresh();
		observeViewModel();

		viewModel.loadPipelines(ctx, projectId, null);
	}

	private void setupRecyclerView() {
		adapter =
				new PipelinesAdapter(
						ctx,
						new ArrayList<>(),
						pipeline -> {
							Intent intent = new Intent(ctx, PipelineDetailActivity.class);
							intent.putExtra("projectId", projectId);
							intent.putExtra("pipelineId", pipeline.getId());
							startActivity(intent);
						});

		LinearLayoutManager layoutManager = new LinearLayoutManager(ctx);
		binding.recyclerView.setLayoutManager(layoutManager);
		binding.recyclerView.setAdapter(adapter);

		EndlessRecyclerViewScrollListener scrollListener =
				new EndlessRecyclerViewScrollListener(layoutManager) {
					@Override
					public void onLoadMore(int page, int totalItemsCount, RecyclerView view) {
						viewModel.loadNextPage(ctx);
					}
				};
		binding.recyclerView.addOnScrollListener(scrollListener);
	}

	private void setupPullToRefresh() {
		binding.pullToRefresh.setOnRefreshListener(
				() -> viewModel.loadPipelines(ctx, projectId, null));
	}

	private void observeViewModel() {
		viewModel
				.getIsLoading()
				.observe(
						this,
						loading -> {
							if (Boolean.TRUE.equals(loading)) {
								binding.progressBar.setVisibility(View.VISIBLE);
								binding.recyclerView.setVisibility(View.GONE);
								binding.nothingFoundFrame.getRoot().setVisibility(View.GONE);
							} else {
								binding.progressBar.setVisibility(View.GONE);
								binding.pullToRefresh.setRefreshing(false);
							}
						});

		viewModel
				.getPipelinesList()
				.observe(
						this,
						list -> {
							if (Boolean.TRUE.equals(viewModel.getIsLoading().getValue())) return;
							if (list == null || list.isEmpty()) {
								binding.nothingFoundFrame.getRoot().setVisibility(View.VISIBLE);
								binding.recyclerView.setVisibility(View.GONE);
							} else {
								binding.nothingFoundFrame.getRoot().setVisibility(View.GONE);
								binding.recyclerView.setVisibility(View.VISIBLE);
								adapter.updateList(list);
							}
						});

		viewModel
				.getError()
				.observe(
						this,
						errorMsg -> {
							if (errorMsg != null) {
								Toasty.show(ctx, errorMsg);
								viewModel.clearError();
							}
						});
	}
}
