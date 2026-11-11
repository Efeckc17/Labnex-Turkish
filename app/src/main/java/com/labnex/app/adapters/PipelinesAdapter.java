package com.labnex.app.adapters;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import com.labnex.app.R;
import com.labnex.app.databinding.ListPipelinesBinding;
import com.labnex.app.helpers.AvatarGenerator;
import com.labnex.app.helpers.TimeHelper;
import com.labnex.app.helpers.Utils;
import com.labnex.app.models.ci.Pipeline;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * @author mmarif
 */
public class PipelinesAdapter extends RecyclerView.Adapter<PipelinesAdapter.PipelinesHolder> {

	private final Context context;
	private List<Pipeline> list;
	private final OnPipelineClickListener listener;

	public interface OnPipelineClickListener {
		void onPipelineClick(Pipeline pipeline);
	}

	public PipelinesAdapter(
			Context context, List<Pipeline> list, OnPipelineClickListener listener) {
		this.context = context;
		this.list = new ArrayList<>(list);
		this.listener = listener;
	}

	@SuppressLint("NotifyDataSetChanged")
	public void updateList(List<Pipeline> newList) {
		this.list = new ArrayList<>(newList);
		notifyDataSetChanged();
	}

	@NonNull @Override
	public PipelinesHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
		ListPipelinesBinding binding =
				ListPipelinesBinding.inflate(LayoutInflater.from(context), parent, false);
		return new PipelinesHolder(binding);
	}

	@Override
	public void onBindViewHolder(@NonNull PipelinesHolder holder, int position) {
		holder.bind(list.get(position));
		holder.binding.getRoot().updateAppearance(position, getItemCount());
	}

	@Override
	public int getItemCount() {
		return list.size();
	}

	public class PipelinesHolder extends RecyclerView.ViewHolder {
		final ListPipelinesBinding binding;

		PipelinesHolder(ListPipelinesBinding binding) {
			super(binding.getRoot());
			this.binding = binding;
			itemView.setOnClickListener(v -> {
				int pos = getBindingAdapterPosition();
				if (pos != RecyclerView.NO_POSITION && listener != null) {
					listener.onPipelineClick(list.get(pos));
				}
			});
		}

		void bind(Pipeline pipeline) {
			binding.pipelineId.setText(
					context.getString(R.string.text_with_hash, pipeline.getId()));

			String title = pipeline.getSource() != null ? pipeline.getSource() : "Pipeline Run";
			binding.commitTitle.setText(title);

			String ref = pipeline.getRef();
			if (ref != null) {
				if (ref.startsWith("refs/merge-requests/")) {
					ref = "MR " + ref.replace("refs/merge-requests/", "").replace("/head", "");
				} else {
					ref = ref.replace("refs/heads/", "");
				}
			}
			binding.branchName.setText(ref != null ? ref : "main");

			String shortSha =
					pipeline.getSha() != null && pipeline.getSha().length() >= 8
							? pipeline.getSha().substring(0, 8)
							: "";
			binding.commitSha.setText(shortSha);

			if (pipeline.getCreatedAt() != null) {
				Date date = TimeHelper.parseIso8601(pipeline.getCreatedAt());
				binding.timeAgo.setText(TimeHelper.formatTime(date));
			}

			String status = pipeline.getStatus();
			int colorResId = Utils.getCiColorResId(status);
			int finalColor = ContextCompat.getColor(context, colorResId);
			String displayStatus = context.getString(Utils.getCiStatusStringRes(status));

			Drawable statusBadge =
					AvatarGenerator.getLabelDrawable(context, displayStatus, finalColor, 20);
			binding.statusBadge.setImageDrawable(statusBadge);
		}
	}
}
