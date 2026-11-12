package com.labnex.app.bottomsheets;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.labnex.app.R;
import com.labnex.app.adapters.VariablesAdapter;
import com.labnex.app.databinding.BottomsheetVariablesBinding;
import com.labnex.app.helpers.EndlessRecyclerViewScrollListener;
import com.labnex.app.helpers.Toasty;
import com.labnex.app.helpers.UIHelper;
import com.labnex.app.helpers.Utils;
import com.labnex.app.models.app.GenericMenuItemModel;
import com.labnex.app.models.ci.Variable;
import com.labnex.app.viewmodels.VariablesViewModel;
import java.util.ArrayList;
import java.util.List;

/**
 * @author mmarif
 */
public class VariablesBottomSheet extends BottomSheetDialogFragment {

	private BottomsheetVariablesBinding binding;
	private VariablesViewModel viewModel;
	private VariablesAdapter adapter;
	private long projectId;

	public static VariablesBottomSheet newInstance(long projectId) {
		VariablesBottomSheet sheet = new VariablesBottomSheet();
		Bundle args = new Bundle();
		args.putLong("projectId", projectId);
		sheet.setArguments(args);
		return sheet;
	}

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (getArguments() != null) {
			projectId = getArguments().getLong("projectId");
		}
	}

	@Nullable @Override
	public View onCreateView(
			@NonNull LayoutInflater inflater,
			@Nullable ViewGroup container,
			@Nullable Bundle savedInstanceState) {
		binding = BottomsheetVariablesBinding.inflate(inflater, container, false);
		viewModel = new ViewModelProvider(this).get(VariablesViewModel.class);

		setupRecyclerView();
		observeViewModel();

		viewModel.loadVariables(requireContext(), projectId);

		return binding.getRoot();
	}

	private void setupRecyclerView() {
		adapter = new VariablesAdapter(requireContext(), new ArrayList<>(), this::showItemMenu);
		LinearLayoutManager layoutManager = new LinearLayoutManager(requireContext());
		binding.recyclerView.setLayoutManager(layoutManager);
		binding.recyclerView.setAdapter(adapter);

		EndlessRecyclerViewScrollListener scrollListener =
				new EndlessRecyclerViewScrollListener(layoutManager) {
					@Override
					public void onLoadMore(int page, int totalItemsCount, RecyclerView view) {
						viewModel.loadNextPage(requireContext());
					}
				};
		binding.recyclerView.addOnScrollListener(scrollListener);
	}

	private void showItemMenu(Variable variable) {
		List<GenericMenuItemModel> items = new ArrayList<>();

		if (variable.getValue() != null && !variable.getValue().isEmpty()) {
			items.add(
					new GenericMenuItemModel(
							"copy_value",
							R.string.copy_value,
							R.drawable.ic_copy,
							com.google.android.material.R.attr.colorSecondaryContainer,
							com.google.android.material.R.attr.colorOnSecondaryContainer));
		}

		items.add(
				new GenericMenuItemModel(
						"edit",
						R.string.edit,
						R.drawable.ic_edit,
						com.google.android.material.R.attr.colorPrimaryContainer,
						com.google.android.material.R.attr.colorOnPrimaryContainer));

		items.add(
				new GenericMenuItemModel(
						"delete",
						R.string.delete,
						R.drawable.ic_trash,
						com.google.android.material.R.attr.colorErrorContainer,
						com.google.android.material.R.attr.colorOnErrorContainer));

		GenericMenuBottomSheet sheet =
				GenericMenuBottomSheet.newInstance(variable.getKey(), null, items);
		sheet.setOnMenuItemClickListener(id -> {
			if ("copy_value".equals(id)) {
				Utils.copyToClipboard(
						requireContext(),
						variable.getValue(),
						getString(R.string.copied_to_clipboard));
			} else if ("edit".equals(id)) {
				CreateVariableBottomSheet.newInstance(projectId, variable)
						.show(getParentFragmentManager(), "editVariableSheet");
			} else if ("delete".equals(id)) {
				showDeleteConfirmation(variable);
			}
		});
		sheet.show(getParentFragmentManager(), "variableMenuSheet");
	}

	private void showDeleteConfirmation(Variable variable) {
		new MaterialAlertDialogBuilder(requireContext())
				.setTitle(R.string.delete_variable)
				.setMessage(getString(R.string.delete_variable_confirmation, variable.getKey()))
				.setPositiveButton(
						R.string.delete,
						(dialog, which) ->
								viewModel.deleteVariable(
										requireContext(), projectId, variable.getKey()))
				.setNeutralButton(R.string.cancel, null)
				.show();
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
							}
						});

		viewModel
				.getVariableList()
				.observe(
						getViewLifecycleOwner(),
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
				.getDeleteSuccess()
				.observe(
						getViewLifecycleOwner(),
						success -> {
							if (Boolean.TRUE.equals(success)) {
								Toasty.show(requireContext(), R.string.variable_deleted);
								viewModel.clearDeleteSuccess();
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
									Toasty.show(
											requireContext(), getString(R.string.not_authorized));
									break;
								case "access_forbidden_403":
									Toasty.show(
											requireContext(),
											getString(R.string.access_forbidden_403));
									break;
								case "not_found":
									Toasty.show(requireContext(), getString(R.string.not_found));
									break;
								case "generic_error":
									Toasty.show(
											requireContext(), getString(R.string.generic_error));
									break;
								default:
									Toasty.show(requireContext(), errorMsg);
									break;
							}
							viewModel.clearError();
						});
	}

	@Override
	public void onStart() {
		super.onStart();
		Dialog dialog = getDialog();
		if (dialog instanceof BottomSheetDialog) {
			UIHelper.applyFullScreenSheetStyle((BottomSheetDialog) dialog, true);
		}
	}

	@Override
	public void onDestroyView() {
		super.onDestroyView();
		binding = null;
	}
}
