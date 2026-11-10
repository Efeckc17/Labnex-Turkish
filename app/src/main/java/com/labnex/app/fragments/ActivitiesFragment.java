package com.labnex.app.fragments;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.labnex.app.R;
import com.labnex.app.activities.CommitsActivity;
import com.labnex.app.activities.IssueDetailActivity;
import com.labnex.app.activities.MergeRequestDetailActivity;
import com.labnex.app.activities.ProjectDetailActivity;
import com.labnex.app.activities.SnippetsActivity;
import com.labnex.app.adapters.ActivitiesAdapter;
import com.labnex.app.clients.RetrofitClient;
import com.labnex.app.contexts.IssueContext;
import com.labnex.app.contexts.MergeRequestContext;
import com.labnex.app.contexts.ProjectsContext;
import com.labnex.app.databinding.BottomsheetActivitiesFilterBinding;
import com.labnex.app.databinding.FragmentActivitiesBinding;
import com.labnex.app.helpers.EndlessRecyclerViewScrollListener;
import com.labnex.app.helpers.Toasty;
import com.labnex.app.helpers.UIHelper;
import com.labnex.app.models.events.Events;
import com.labnex.app.models.events.PushData;
import com.labnex.app.models.projects.Projects;
import com.labnex.app.viewmodels.ActivitiesViewModel;
import java.util.ArrayList;
import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * @author mmarif
 */
public class ActivitiesFragment extends Fragment {

	private FragmentActivitiesBinding binding;
	private Context ctx;
	private ActivitiesViewModel viewModel;
	private ActivitiesAdapter adapter;
	private EndlessRecyclerViewScrollListener scrollListener;
	private boolean isFirstLoad = true;
	private Events event;

	@Override
	public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		View dock = requireActivity().findViewById(R.id.docked_toolbar);
		UIHelper.applyInsets(view, dock, binding.recyclerView, binding.pullToRefresh, null);
	}

	@Nullable @Override
	public View onCreateView(
			@NonNull LayoutInflater inflater,
			@Nullable ViewGroup container,
			@Nullable Bundle savedInstanceState) {
		binding = FragmentActivitiesBinding.inflate(inflater, container, false);
		ctx = requireContext();
		viewModel = new ViewModelProvider(this).get(ActivitiesViewModel.class);

		setupRecyclerView();
		setupPullToRefresh();
		observeViewModel();

		return binding.getRoot();
	}

	@Override
	public void onResume() {
		super.onResume();
		if (viewModel == null) {
			viewModel = new ViewModelProvider(this).get(ActivitiesViewModel.class);
		}
		if (!isHidden()) {
			if (isFirstLoad || viewModel.needsDataLoad()) {
				lazyLoad();
			} else {
				restoreUIState();
			}
		}
	}

	@Override
	public void onHiddenChanged(boolean hidden) {
		super.onHiddenChanged(hidden);
		if (!hidden) {
			if (viewModel == null) {
				viewModel = new ViewModelProvider(this).get(ActivitiesViewModel.class);
			}
			if (isFirstLoad || viewModel.needsDataLoad()) {
				lazyLoad();
			} else {
				restoreUIState();
			}
		}
	}

	private void lazyLoad() {
		isFirstLoad = false;
		if (viewModel != null) {
			viewModel.loadEvents(ctx);
		}
	}

	private void restoreUIState() {
		if (viewModel == null) return;

		List<Events> events = viewModel.getEvents().getValue();
		if (events != null && !events.isEmpty()) {
			binding.nothingFoundFrame.getRoot().setVisibility(View.GONE);
			binding.recyclerView.setVisibility(View.VISIBLE);
			binding.progressBar.setVisibility(View.GONE);
		} else {
			binding.nothingFoundFrame.getRoot().setVisibility(View.VISIBLE);
			binding.recyclerView.setVisibility(View.GONE);
			binding.progressBar.setVisibility(View.GONE);
		}
	}

	public void openContextMenu() {
		showFilterBottomSheet();
	}

	private void setupRecyclerView() {
		adapter = new ActivitiesAdapter(ctx, new ArrayList<>(), this::handleEventClick);
		LinearLayoutManager layoutManager = new LinearLayoutManager(ctx);
		binding.recyclerView.setLayoutManager(layoutManager);
		binding.recyclerView.setAdapter(adapter);

		scrollListener =
				new EndlessRecyclerViewScrollListener(layoutManager) {
					@Override
					public void onLoadMore(int page, int totalItemsCount, RecyclerView view) {
						viewModel.loadNextPage(ctx);
					}
				};
		binding.recyclerView.addOnScrollListener(scrollListener);
	}

	private void handleEventClick(Events event) {
		String action = event.getActionName();
		if ("destroyed".equalsIgnoreCase(action)
				|| "deleted".equalsIgnoreCase(action)
				|| "removed".equalsIgnoreCase(action)) {
			Toasty.show(ctx, getString(R.string.activity_deleted_no_action));
			return;
		}

		String type = event.getTargetType();
		if (type == null) {
			openProject(event.getProjectId());
			return;
		}

		switch (type) {
			case "Issue", "WorkItem":
				openIssueDetail(event);
				break;
			case "MergeRequest":
				openMrDetail(event);
				break;
			case "Note":
				openNote(event);
				break;
			case "Milestone":
				openProjectMilestone(event);
				break;
			case "Tag":
				openProjectTag(event);
				break;
			case "Label":
				openProjectLabel(event);
				break;
			case "Member":
				openProjectMember(event);
				break;
			case "Release":
				openProjectRelease(event);
				break;
			case "Wiki", "WikiPage::Meta":
				openProjectWiki(event);
				break;
			case "Snippet":
				openSnippet(event);
				break;
			case "User":
				openUserProfile(event);
				break;
			case "Project":
				if (event.getPushData() != null) {
					handleProjectEvent(event);
				} else {
					openProject(event.getProjectId());
				}
				break;
			default:
				openProject(event.getProjectId());
				break;
		}
	}

	private void openNote(Events event) {
		if (event.getNote() == null) {
			openProject(event.getProjectId());
			return;
		}
		String noteable = event.getNote().getNoteableType();
		if ("Issue".equalsIgnoreCase(noteable)) {
			openIssueDetail(event);
		} else if ("MergeRequest".equalsIgnoreCase(noteable)) {
			openMrDetail(event);
		} else {
			openProject(event.getProjectId());
		}
	}

	private void openIssueDetail(Events event) {
		long projectId = event.getProjectId();
		String iidRaw = event.getTargetIid();

		if ("Note".equalsIgnoreCase(event.getTargetType()) && event.getNote() != null) {
			long noteableIid = event.getNote().getNoteableIid();
			if (noteableIid > 0) {
				iidRaw = String.valueOf(noteableIid);
			}
		}

		if (projectId <= 0 || iidRaw == null || iidRaw.isEmpty()) {
			openProject(projectId);
			return;
		}

		long issueIid;
		try {
			issueIid = Long.parseLong(iidRaw);
		} catch (NumberFormatException e) {
			openProject(projectId);
			return;
		}

		RetrofitClient.getApiInterface(ctx)
				.getProjectInfo(projectId)
				.enqueue(
						new Callback<>() {
							@Override
							public void onResponse(
									@NonNull Call<Projects> c, @NonNull Response<Projects> r) {
								if (r.isSuccessful() && r.body() != null) {
									ProjectsContext pc = new ProjectsContext(r.body(), ctx);
									pc.saveToDB(ctx);

									IssueContext issueContext =
											new IssueContext(pc, (int) issueIid, "ISSUE");
									startActivity(
											issueContext.getIntent(ctx, IssueDetailActivity.class));
								} else {
									openProject(projectId);
								}
							}

							@Override
							public void onFailure(@NonNull Call<Projects> c, @NonNull Throwable t) {
								openProject(projectId);
							}
						});
	}

	private void openMrDetail(Events event) {
		long projectId = event.getProjectId();
		String iidRaw = event.getTargetIid();

		if ("Note".equalsIgnoreCase(event.getTargetType()) && event.getNote() != null) {
			long noteableIid = event.getNote().getNoteableIid();
			if (noteableIid > 0) {
				iidRaw = String.valueOf(noteableIid);
			}
		}

		if (projectId <= 0 || iidRaw == null || iidRaw.isEmpty()) {
			openProject(projectId);
			return;
		}

		long mrIid;
		try {
			mrIid = Long.parseLong(iidRaw);
		} catch (NumberFormatException e) {
			openProject(projectId);
			return;
		}

		RetrofitClient.getApiInterface(ctx)
				.getProjectInfo(projectId)
				.enqueue(
						new Callback<>() {
							@Override
							public void onResponse(
									@NonNull Call<Projects> c, @NonNull Response<Projects> r) {
								if (r.isSuccessful() && r.body() != null) {
									ProjectsContext pc = new ProjectsContext(r.body(), ctx);
									pc.saveToDB(ctx);

									MergeRequestContext mrContext =
											new MergeRequestContext(pc, (int) mrIid);
									startActivity(
											mrContext.getIntent(
													ctx, MergeRequestDetailActivity.class));
								} else {
									openProject(projectId);
								}
							}

							@Override
							public void onFailure(@NonNull Call<Projects> c, @NonNull Throwable t) {
								openProject(projectId);
							}
						});
	}

	private void openProjectMilestone(Events event) {
		openProject(event.getProjectId(), "milestones");
	}

	private void openProjectWiki(Events event) {
		String action = event.getActionName();
		String title = event.getTargetTitle();
		boolean autoOpen =
				("created".equalsIgnoreCase(action) || "updated".equalsIgnoreCase(action))
						&& title != null
						&& !title.isEmpty();

		openProject(event.getProjectId(), "wikis", autoOpen ? title : null);
	}

	private void openProjectTag(Events event) {
		openProject(event.getProjectId(), "tags");
	}

	private void openProjectLabel(Events event) {
		openProject(event.getProjectId(), "labels");
	}

	private void openProjectMember(Events event) {
		openProject(event.getProjectId(), "members");
	}

	private void openProjectRelease(Events event) {
		openProject(event.getProjectId(), "releases");
	}

	private void handleProjectEvent(Events event) {
		PushData push = event.getPushData();
		if (push == null) {
			openProject(event.getProjectId());
			return;
		}

		String refType = push.getRefType();
		if ("tag".equalsIgnoreCase(refType)) {
			openProject(event.getProjectId(), "tags");
			return;
		}

		if (!"branch".equalsIgnoreCase(refType)) {
			openProject(event.getProjectId());
			return;
		}

		String branch = push.getRef();
		if (branch == null || branch.isEmpty()) {
			openProject(event.getProjectId());
			return;
		}

		String sha = null;
		if (push.getCommitCount() == 1) {
			String to = push.getCommitTo();
			if (to != null && !to.isEmpty()) {
				sha = to;
			}
		}

		openCommits(event.getProjectId(), branch, sha);
	}

	private void openCommits(long projectId, String branch, String sha) {
		if (projectId <= 0) return;
		Intent intent = new Intent(ctx, CommitsActivity.class);
		intent.putExtra("source", "project");
		intent.putExtra("projectId", projectId);
		intent.putExtra("branch", branch);
		if (sha != null) {
			intent.putExtra("pending_sha", sha);
		}
		ctx.startActivity(intent);
	}

	private void openSnippet(Events event) {
		String idRaw = event.getTargetId();
		if (idRaw == null || idRaw.isEmpty()) {
			ctx.startActivity(new Intent(ctx, SnippetsActivity.class));
			return;
		}
		long snippetId;
		try {
			snippetId = Long.parseLong(idRaw);
		} catch (NumberFormatException e) {
			ctx.startActivity(new Intent(ctx, SnippetsActivity.class));
			return;
		}
		Intent intent = new Intent(ctx, SnippetsActivity.class);
		intent.putExtra("pending_snippet_id", snippetId);
		ctx.startActivity(intent);
	}

	private void openUserProfile(Events event) {
		openProject(event.getProjectId());
	}

	private void openProject(long projectId) {
		openProject(projectId, null, null);
	}

	private void openProject(long projectId, String openSheet) {
		openProject(projectId, openSheet, null);
	}

	private void openProject(long projectId, String openSheet, String openWikiTitle) {
		if (projectId <= 0) return;

		RetrofitClient.getApiInterface(ctx)
				.getProjectInfo(projectId)
				.enqueue(
						new Callback<>() {
							@Override
							public void onResponse(
									@NonNull Call<Projects> c, @NonNull Response<Projects> r) {
								if (r.isSuccessful() && r.body() != null) {
									ProjectsContext pc = new ProjectsContext(r.body(), ctx);
									pc.saveToDB(ctx);
									Intent intent = pc.getIntent(ctx, ProjectDetailActivity.class);
									if (openSheet != null) {
										intent.putExtra("open_sheet", openSheet);
									}
									if (openWikiTitle != null) {
										intent.putExtra("open_wiki_title", openWikiTitle);
									}
									ctx.startActivity(intent);
								}
							}

							@Override
							public void onFailure(
									@NonNull Call<Projects> c, @NonNull Throwable t) {}
						});
	}

	private void setupPullToRefresh() {
		binding.pullToRefresh.setOnRefreshListener(() -> viewModel.loadEvents(ctx));
	}

	private void showFilterBottomSheet() {
		BottomSheetDialog dialog = new BottomSheetDialog(ctx);
		BottomsheetActivitiesFilterBinding fb =
				BottomsheetActivitiesFilterBinding.inflate(getLayoutInflater());
		dialog.setContentView(fb.getRoot());
		UIHelper.applySheetStyle(dialog, true);

		String current = viewModel.getFilter();
		fb.chipAll.setChecked(ActivitiesViewModel.FILTER_ALL.equals(current));
		fb.chipIssue.setChecked(ActivitiesViewModel.FILTER_ISSUE.equals(current));
		fb.chipNote.setChecked(ActivitiesViewModel.FILTER_NOTE.equals(current));
		fb.chipProject.setChecked(ActivitiesViewModel.FILTER_PROJECT.equals(current));
		fb.chipMr.setChecked(ActivitiesViewModel.FILTER_MERGE_REQUEST.equals(current));
		fb.chipMilestone.setChecked(ActivitiesViewModel.FILTER_MILESTONE.equals(current));
		fb.chipSnippet.setChecked(ActivitiesViewModel.FILTER_SNIPPET.equals(current));
		fb.chipEpic.setChecked(ActivitiesViewModel.FILTER_EPIC.equals(current));
		fb.chipUser.setChecked(ActivitiesViewModel.FILTER_USER.equals(current));

		fb.chipAll.setOnClickListener(v -> {
			viewModel.setFilter(ActivitiesViewModel.FILTER_ALL);
			viewModel.loadEvents(ctx);
			dialog.dismiss();
		});
		fb.chipIssue.setOnClickListener(v -> {
			viewModel.setFilter(ActivitiesViewModel.FILTER_ISSUE);
			viewModel.loadEvents(ctx);
			dialog.dismiss();
		});
		fb.chipNote.setOnClickListener(v -> {
			viewModel.setFilter(ActivitiesViewModel.FILTER_NOTE);
			viewModel.loadEvents(ctx);
			dialog.dismiss();
		});
		fb.chipProject.setOnClickListener(v -> {
			viewModel.setFilter(ActivitiesViewModel.FILTER_PROJECT);
			viewModel.loadEvents(ctx);
			dialog.dismiss();
		});
		fb.chipMr.setOnClickListener(v -> {
			viewModel.setFilter(ActivitiesViewModel.FILTER_MERGE_REQUEST);
			viewModel.loadEvents(ctx);
			dialog.dismiss();
		});
		fb.chipMilestone.setOnClickListener(v -> {
			viewModel.setFilter(ActivitiesViewModel.FILTER_MILESTONE);
			viewModel.loadEvents(ctx);
			dialog.dismiss();
		});
		fb.chipSnippet.setOnClickListener(v -> {
			viewModel.setFilter(ActivitiesViewModel.FILTER_SNIPPET);
			viewModel.loadEvents(ctx);
			dialog.dismiss();
		});
		fb.chipEpic.setOnClickListener(v -> {
			viewModel.setFilter(ActivitiesViewModel.FILTER_EPIC);
			viewModel.loadEvents(ctx);
			dialog.dismiss();
		});
		fb.chipUser.setOnClickListener(v -> {
			viewModel.setFilter(ActivitiesViewModel.FILTER_USER);
			viewModel.loadEvents(ctx);
			dialog.dismiss();
		});

		dialog.show();
	}

	private void observeViewModel() {
		viewModel
				.getIsLoading()
				.observe(
						getViewLifecycleOwner(),
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
				.getEvents()
				.observe(
						getViewLifecycleOwner(),
						events -> {
							if (Boolean.TRUE.equals(viewModel.getIsLoading().getValue())) return;

							if (events == null) return;

							if (events.isEmpty()) {
								binding.nothingFoundFrame.getRoot().setVisibility(View.VISIBLE);
								binding.recyclerView.setVisibility(View.GONE);
							} else {
								binding.nothingFoundFrame.getRoot().setVisibility(View.GONE);
								binding.recyclerView.setVisibility(View.VISIBLE);
								adapter.updateList(events);
								scrollListener.resetState();
							}
						});

		viewModel
				.getError()
				.observe(
						getViewLifecycleOwner(),
						errorMsg -> {
							if (errorMsg == null) return;
							switch (errorMsg) {
								case "auth_error":
									Toasty.show(ctx, getString(R.string.not_authorized));
									break;
								case "access_forbidden_403":
									Toasty.show(ctx, getString(R.string.access_forbidden_403));
									break;
								case "not_found":
									Toasty.show(ctx, getString(R.string.not_found));
									break;
								case "generic_error":
									Toasty.show(ctx, getString(R.string.generic_error));
									break;
								default:
									Toasty.show(ctx, errorMsg);
									break;
							}
							viewModel.clearError();
						});
	}

	@Override
	public void onDestroyView() {
		super.onDestroyView();
		binding = null;
	}
}
