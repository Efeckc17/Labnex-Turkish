package com.labnex.app.adapters;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import com.labnex.app.R;
import com.labnex.app.databinding.ListVariablesBinding;
import com.labnex.app.helpers.AvatarGenerator;
import com.labnex.app.models.ci.Variable;
import java.util.ArrayList;
import java.util.List;

/**
 * @author mmarif
 */
public class VariablesAdapter extends RecyclerView.Adapter<VariablesAdapter.VariableHolder> {

	private final Context context;
	private final List<Variable> list;
	private final OnVariableClickListener listener;

	public interface OnVariableClickListener {
		void onMenuClick(Variable variable);
	}

	public VariablesAdapter(
			Context context, List<Variable> list, OnVariableClickListener listener) {
		this.context = context;
		this.list = new ArrayList<>(list);
		this.listener = listener;
	}

	@SuppressLint("NotifyDataSetChanged")
	public void updateList(List<Variable> newList) {
		this.list.clear();
		if (newList != null) this.list.addAll(newList);
		notifyDataSetChanged();
	}

	@NonNull @Override
	public VariableHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
		ListVariablesBinding binding =
				ListVariablesBinding.inflate(LayoutInflater.from(context), parent, false);
		return new VariableHolder(binding);
	}

	@Override
	public void onBindViewHolder(@NonNull VariableHolder holder, int position) {
		holder.bind(list.get(position));
		holder.binding.getRoot().updateAppearance(position, getItemCount());
	}

	@Override
	public int getItemCount() {
		return list.size();
	}

	public class VariableHolder extends RecyclerView.ViewHolder {
		private final ListVariablesBinding binding;

		VariableHolder(ListVariablesBinding binding) {
			super(binding.getRoot());
			this.binding = binding;

			binding.menuButton.setOnClickListener(v -> {
				int pos = getBindingAdapterPosition();
				if (pos != RecyclerView.NO_POSITION && listener != null) {
					listener.onMenuClick(list.get(pos));
				}
			});
		}

		void bind(Variable variable) {
			binding.variableKey.setText(variable.getKey());

			String scope =
					variable.getEnvironmentScope() != null ? variable.getEnvironmentScope() : "*";
			String type =
					variable.getVariableType() != null ? variable.getVariableType() : "env_var";
			binding.variableScope.setText(
					context.getString(R.string.variable_scope_format, scope, type));

			if (variable.isMasked()) {
				binding.badgeMasked.setVisibility(View.VISIBLE);
				int color = ContextCompat.getColor(context, R.color.alert_important_border);
				String label = context.getString(R.string.variable_masked);
				Drawable badge = AvatarGenerator.getLabelDrawable(context, label, color, 18);
				binding.badgeMasked.setImageDrawable(badge);
			} else {
				binding.badgeMasked.setVisibility(View.GONE);
			}

			binding.badgeProtected.setVisibility(variable.isProtected() ? View.VISIBLE : View.GONE);

			if (variable.getDescription() != null && !variable.getDescription().trim().isEmpty()) {
				binding.variableDescription.setVisibility(View.VISIBLE);
				binding.variableDescription.setText(variable.getDescription());
			} else {
				binding.variableDescription.setVisibility(View.GONE);
			}
		}
	}
}
