package com.labnex.app.adapters;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.color.MaterialColors;
import com.labnex.app.databinding.ListGroupsBinding;
import com.labnex.app.helpers.Utils;
import com.labnex.app.models.groups.GroupsItem;
import java.util.ArrayList;
import java.util.List;

/**
 * @author mmarif
 */
public class GroupsAdapter extends RecyclerView.Adapter<GroupsAdapter.GroupsHolder> {

	private final Context context;
	private final List<GroupsItem> groupsList;
	private final OnGroupClickListener listener;

	public interface OnGroupClickListener {
		void onGroupClick(GroupsItem group);

		void onGroupMenuClick(GroupsItem group);
	}

	public GroupsAdapter(
			Context ctx, List<GroupsItem> groupsListMain, OnGroupClickListener listener) {
		this.context = ctx;
		this.groupsList = new ArrayList<>();
		if (groupsListMain != null) this.groupsList.addAll(groupsListMain);
		this.listener = listener;
	}

	@SuppressLint("NotifyDataSetChanged")
	public void updateList(List<GroupsItem> newList) {
		groupsList.clear();
		if (newList != null) groupsList.addAll(newList);
		notifyDataSetChanged();
	}

	@NonNull @Override
	public GroupsHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
		ListGroupsBinding binding =
				ListGroupsBinding.inflate(LayoutInflater.from(context), parent, false);
		return new GroupsHolder(binding);
	}

	@Override
	public void onBindViewHolder(@NonNull GroupsHolder holder, int position) {
		holder.bind(groupsList.get(position));
		holder.binding.getRoot().updateAppearance(position, getItemCount());
	}

	@Override
	public int getItemCount() {
		return groupsList.size();
	}

	public class GroupsHolder extends RecyclerView.ViewHolder {

		final ListGroupsBinding binding;

		GroupsHolder(ListGroupsBinding binding) {
			super(binding.getRoot());
			this.binding = binding;

			itemView.setOnClickListener(v -> {
				int pos = getBindingAdapterPosition();
				if (pos != RecyclerView.NO_POSITION && listener != null) {
					listener.onGroupClick(groupsList.get(pos));
				}
			});

			binding.btnMenu.setOnClickListener(v -> {
				int pos = getBindingAdapterPosition();
				if (pos != RecyclerView.NO_POSITION && listener != null) {
					listener.onGroupMenuClick(groupsList.get(pos));
				}
			});
		}

		void bind(GroupsItem group) {
			int level = group.getLevel();

			int baseColor =
					MaterialColors.getColor(
							binding.card,
							com.google.android.material.R.attr.colorSurfaceContainerLow);
			int primaryColor =
					MaterialColors.getColor(
							binding.card, com.google.android.material.R.attr.colorPrimaryContainer);

			if (level > 0) {
				float overlayAlpha = Math.min(0.2f, 0.05f * level);
				int tieredColor = MaterialColors.layer(baseColor, primaryColor, overlayAlpha);
				binding.card.setCardBackgroundColor(tieredColor);

				binding.pathBadge.setVisibility(View.VISIBLE);
				binding.pathBadge.setText(getParentPath(group.getFullPath()));
			} else {
				binding.card.setCardBackgroundColor(baseColor);
				binding.pathBadge.setVisibility(View.GONE);
			}

			Utils.loadEntityAvatar(
					context,
					"groups",
					group.getId(),
					group.getAvatarUrl(),
					group.getName(),
					binding.groupAvatar,
					40);

			binding.groupName.setText(group.getName());
			binding.groupId.setText(group.getFullPath());

			if (group.getDescription() != null && !group.getDescription().isEmpty()) {
				binding.groupDescription.setVisibility(View.VISIBLE);
				binding.groupDescription.setText(group.getDescription());
			} else {
				binding.groupDescription.setVisibility(View.GONE);
			}
		}

		private String getParentPath(String fullPath) {
			if (fullPath == null || !fullPath.contains("/")) return "";
			int lastSlash = fullPath.lastIndexOf("/");
			return fullPath.substring(0, lastSlash).replace("/", " / ").toUpperCase();
		}
	}
}
