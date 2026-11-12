package com.labnex.app.bottomsheets;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.gson.Gson;
import com.labnex.app.R;
import com.labnex.app.databinding.BottomsheetCreateVariableBinding;
import com.labnex.app.helpers.Toasty;
import com.labnex.app.helpers.UIHelper;
import com.labnex.app.models.ci.CrudeVariable;
import com.labnex.app.models.ci.Variable;
import com.labnex.app.viewmodels.VariablesViewModel;

/**
 * @author mmarif
 */
public class CreateVariableBottomSheet extends BottomSheetDialogFragment {

	private BottomsheetCreateVariableBinding binding;
	private VariablesViewModel viewModel;
	private long projectId;
	private Variable editingVariable;
	private boolean isEditMode = false;

	public static CreateVariableBottomSheet newInstance(
			long projectId, @Nullable Variable variable) {
		CreateVariableBottomSheet sheet = new CreateVariableBottomSheet();
		Bundle args = new Bundle();
		args.putLong("projectId", projectId);
		if (variable != null) {
			args.putString("variable_json", new Gson().toJson(variable));
		}
		sheet.setArguments(args);
		return sheet;
	}

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (getArguments() != null) {
			projectId = getArguments().getLong("projectId");
			String json = getArguments().getString("variable_json");
			if (json != null) {
				editingVariable = new Gson().fromJson(json, Variable.class);
				isEditMode = true;
			}
		}
	}

	@Nullable @Override
	public View onCreateView(
			@NonNull LayoutInflater inflater,
			@Nullable ViewGroup container,
			@Nullable Bundle savedInstanceState) {
		binding = BottomsheetCreateVariableBinding.inflate(inflater, container, false);
		viewModel = new ViewModelProvider(this).get(VariablesViewModel.class);

		binding.btnClose.setOnClickListener(v -> dismiss());

		if (isEditMode && editingVariable != null) {
			binding.sheetTitle.setText(R.string.edit_variable);
			binding.btnSubmit.setText(R.string.update);
			binding.keyInput.setText(editingVariable.getKey());
			binding.keyInput.setEnabled(false);
			binding.valueInput.setText(editingVariable.getValue());
			binding.scopeInput.setText(
					editingVariable.getEnvironmentScope() != null
							? editingVariable.getEnvironmentScope()
							: "*");
			binding.switchProtected.setChecked(editingVariable.isProtected());
			binding.switchMasked.setChecked(editingVariable.isMasked());
			binding.descriptionInput.setText(
					editingVariable.getDescription() != null
							? editingVariable.getDescription()
							: "");
		}

		binding.btnSubmit.setOnClickListener(v -> submitVariable());
		observeViewModel();

		return binding.getRoot();
	}

	private void submitVariable() {
		String key =
				binding.keyInput.getText() != null
						? binding.keyInput.getText().toString().trim()
						: "";
		String value =
				binding.valueInput.getText() != null
						? binding.valueInput.getText().toString().trim()
						: "";
		String scope =
				binding.scopeInput.getText() != null
						? binding.scopeInput.getText().toString().trim()
						: "*";
		String description =
				binding.descriptionInput.getText() != null
						? binding.descriptionInput.getText().toString().trim()
						: "";

		if (key.isEmpty()) {
			Toasty.show(requireContext(), R.string.variable_key_required);
			return;
		}
		if (value.isEmpty()) {
			Toasty.show(requireContext(), R.string.variable_value_required);
			return;
		}

		boolean isMasked = binding.switchMasked.isChecked();

		if (isMasked) {
			if (value.length() < 8) {
				Toasty.show(requireContext(), R.string.masked_var_too_short);
				return;
			}
			if (value.contains("\n") || value.contains(" ")) {
				Toasty.show(requireContext(), R.string.masked_var_invalid_chars);
				return;
			}
		}

		CrudeVariable payload = new CrudeVariable();
		payload.setKey(key);
		payload.setValue(value);
		payload.setEnvironmentScope(scope);
		payload.setProtected(binding.switchProtected.isChecked());
		payload.setMasked(binding.switchMasked.isChecked());
		payload.setVariableType("env_var");
		if (!description.isEmpty()) {
			payload.setDescription(description);
		}

		if (isEditMode && editingVariable != null) {
			viewModel.updateVariable(
					requireContext(), projectId, editingVariable.getKey(), payload);
		} else {
			viewModel.createVariable(requireContext(), projectId, payload);
		}
	}

	private void observeViewModel() {
		viewModel
				.getIsActionLoading()
				.observe(
						getViewLifecycleOwner(),
						loading -> {
							if (Boolean.TRUE.equals(loading)) {
								binding.btnSubmit.setText(null);
								binding.btnSubmit.setEnabled(false);
								binding.loadingIndicator.setVisibility(View.VISIBLE);
							} else {
								binding.loadingIndicator.setVisibility(View.GONE);
								binding.btnSubmit.setText(
										isEditMode ? R.string.update : R.string.create);
								binding.btnSubmit.setEnabled(true);
							}
						});

		viewModel
				.getCreateSuccess()
				.observe(
						getViewLifecycleOwner(),
						success -> {
							if (Boolean.TRUE.equals(success) && !isEditMode) {
								Toasty.show(requireContext(), R.string.variable_created);
								viewModel.clearCreateSuccess();
								dismiss();
							}
						});

		viewModel
				.getEditSuccess()
				.observe(
						getViewLifecycleOwner(),
						success -> {
							if (Boolean.TRUE.equals(success) && isEditMode) {
								Toasty.show(requireContext(), R.string.variable_updated);
								viewModel.clearEditSuccess();
								dismiss();
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
			UIHelper.applySheetStyle((BottomSheetDialog) dialog, false);
		}
	}

	@Override
	public void onDestroyView() {
		super.onDestroyView();
		binding = null;
	}
}
