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
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.labnex.app.R;
import com.labnex.app.databinding.ListJobBinding;
import com.labnex.app.databinding.ListPipelineHeaderBinding;
import com.labnex.app.databinding.ListStageHeaderBinding;
import com.labnex.app.helpers.AvatarGenerator;
import com.labnex.app.helpers.TimeHelper;
import com.labnex.app.helpers.Utils;
import com.labnex.app.models.ci.Job;
import com.labnex.app.models.ci.PipelineDetail;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * @author mmarif
 */
public class PipelineDetailAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

	private static final int TYPE_PIPELINE_HEADER = 0;
	private static final int TYPE_STAGE_HEADER = 1;
	private static final int TYPE_JOB = 2;

	private final Context context;
	private final List<Object> items = new ArrayList<>();
	private final OnJobClickListener jobListener;
	private final OnUserClickListener userListener;
	private final OnCommitClickListener commitListener;

	public interface OnJobClickListener {
		void onJobClick(Job job);
	}

	public interface OnUserClickListener {
		void onUserClick(long userId);
	}

	public interface OnCommitClickListener {
		void onCommitClick(String sha);
	}

	public PipelineDetailAdapter(
			Context context,
			OnJobClickListener jobListener,
			OnUserClickListener userListener,
			OnCommitClickListener commitListener) {
		this.context = context;
		this.jobListener = jobListener;
		this.userListener = userListener;
		this.commitListener = commitListener;
	}

	@SuppressLint("NotifyDataSetChanged")
	public void setData(PipelineDetail detail, List<String> stages, List<Job> jobs) {
		items.clear();

		if (detail != null) {
			items.add(detail);
		}

		if (stages == null) stages = new ArrayList<>();
		if (jobs == null) jobs = new ArrayList<>();

		for (String stage : stages) {
			items.add(stage);
			for (Job job : jobs) {
				if (stage.equals(job.getStage())) {
					items.add(job);
				}
			}
		}
		notifyDataSetChanged();
	}

	@Override
	public int getItemViewType(int position) {
		Object item = items.get(position);
		if (item instanceof PipelineDetail) return TYPE_PIPELINE_HEADER;
		if (item instanceof String) return TYPE_STAGE_HEADER;
		return TYPE_JOB;
	}

	@NonNull @Override
	public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
		LayoutInflater inflater = LayoutInflater.from(context);
		if (viewType == TYPE_PIPELINE_HEADER) {
			return new PipelineHeaderViewHolder(
					ListPipelineHeaderBinding.inflate(inflater, parent, false));
		} else if (viewType == TYPE_STAGE_HEADER) {
			return new StageHeaderViewHolder(
					ListStageHeaderBinding.inflate(inflater, parent, false));
		} else {
			return new JobViewHolder(ListJobBinding.inflate(inflater, parent, false));
		}
	}

	@Override
	public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
		int type = getItemViewType(position);
		if (type == TYPE_PIPELINE_HEADER) {
			((PipelineHeaderViewHolder) holder).bind((PipelineDetail) items.get(position));
		} else if (type == TYPE_STAGE_HEADER) {
			((StageHeaderViewHolder) holder).bind((String) items.get(position));
		} else {
			JobViewHolder jobHolder = (JobViewHolder) holder;

			int stageStart = position;
			while (stageStart > 0 && !(items.get(stageStart - 1) instanceof String)) {
				stageStart--;
			}
			int stageEnd = position;
			while (stageEnd < items.size() - 1 && !(items.get(stageEnd + 1) instanceof String)) {
				stageEnd++;
			}

			int posInStage = position - stageStart;
			int totalInStage = (stageEnd - stageStart) + 1;

			jobHolder.bind((Job) items.get(position), posInStage, totalInStage);
		}
	}

	@Override
	public int getItemCount() {
		return items.size();
	}

	class JobViewHolder extends RecyclerView.ViewHolder {
		private final ListJobBinding binding;

		JobViewHolder(ListJobBinding binding) {
			super(binding.getRoot());
			this.binding = binding;
			itemView.setOnClickListener(v -> {
				int pos = getBindingAdapterPosition();
				if (pos != RecyclerView.NO_POSITION
						&& jobListener != null
						&& items.get(pos) instanceof Job) {
					jobListener.onJobClick((Job) items.get(pos));
				}
			});
		}

		void bind(Job job, int posInStage, int totalInStage) {
			binding.jobName.setText(job.getName());

			String status = job.getStatus();
			int colorResId = Utils.getCiColorResId(status);
			int finalColor = ContextCompat.getColor(context, colorResId);
			String displayStatus = context.getString(Utils.getCiStatusStringRes(status));
			Drawable badge =
					AvatarGenerator.getLabelDrawable(context, displayStatus, finalColor, 20);
			binding.jobStatusBadge.setImageDrawable(badge);

			if (job.getDuration() != null && job.getDuration() > 0) {
				binding.jobDuration.setVisibility(View.VISIBLE);
				long seconds = job.getDuration().longValue();
				long minutes = seconds / 60;
				long secs = seconds % 60;
				String durationText =
						minutes > 0
								? String.format(Locale.getDefault(), "%02d:%02d", minutes, secs)
								: String.format(Locale.getDefault(), "00:%02d", secs);
				binding.jobDuration.setText(durationText);
			} else {
				binding.jobDuration.setVisibility(View.GONE);
			}

			if (job.getRunner() != null) {
				String runnerName = job.getRunner().getName();
				if (runnerName == null && job.getRunner().getDescription() != null) {
					runnerName = job.getRunner().getDescription();
				}
				if (runnerName != null && !runnerName.trim().isEmpty()) {
					binding.jobRunnerName.setVisibility(View.VISIBLE);
					binding.jobRunnerName.setText(runnerName);
				} else {
					binding.jobRunnerName.setVisibility(View.GONE);
				}
			} else {
				binding.jobRunnerName.setVisibility(View.GONE);
			}

			binding.getRoot().updateAppearance(posInStage, totalInStage);
		}
	}

	class PipelineHeaderViewHolder extends RecyclerView.ViewHolder {
		private final ListPipelineHeaderBinding binding;

		PipelineHeaderViewHolder(ListPipelineHeaderBinding binding) {
			super(binding.getRoot());
			this.binding = binding;
		}

		void bind(PipelineDetail detail) {
			binding.headerPipelineId.setText(
					context.getString(R.string.text_with_hash, detail.getId()));

			String status = detail.getStatus();
			int colorResId = Utils.getCiColorResId(status);
			int finalColor = ContextCompat.getColor(context, colorResId);
			String displayStatus = context.getString(Utils.getCiStatusStringRes(status));
			Drawable badge =
					AvatarGenerator.getLabelDrawable(context, displayStatus, finalColor, 20);
			binding.headerStatusBadge.setImageDrawable(badge);

			if (detail.getUser() != null) {
				binding.headerUserName.setText(
						context.getString(
								R.string.username_with_at_sign, detail.getUser().getUsername()));

				Glide.with(context)
						.load(detail.getUser().getAvatarUrl())
						.diskCacheStrategy(DiskCacheStrategy.ALL)
						.placeholder(R.drawable.ic_spinner)
						.centerCrop()
						.into(binding.headerUserAvatar);

				binding.headerUserAvatar.setOnClickListener(v -> {
					if (userListener != null) {
						userListener.onUserClick(detail.getUser().getId());
					}
				});
			}

			if (detail.getSha() != null && detail.getSha().length() >= 8) {
				binding.headerCommitSha.setText(detail.getSha().substring(0, 8));
			} else {
				binding.headerCommitSha.setText(detail.getSha());
			}

			binding.headerCommitRow.setOnClickListener(v -> {
				if (commitListener != null && detail.getSha() != null) {
					commitListener.onCommitClick(detail.getSha());
				}
			});

			binding.headerCommitTitle.setText(detail.getSource());

			if (detail.getCreatedAt() != null) {
				Date date = TimeHelper.parseIso8601(detail.getCreatedAt());
				binding.headerTimeAgo.setText(TimeHelper.formatTime(date));
			}

			if (detail.getDuration() != null && detail.getDuration() > 0) {
				binding.iconDuration.setVisibility(View.VISIBLE);
				binding.headerDuration.setVisibility(View.VISIBLE);

				long seconds = detail.getDuration();
				long hours = seconds / 3600;
				long minutes = (seconds % 3600) / 60;
				long secs = seconds % 60;

				String durationText =
						hours > 0
								? String.format(
										Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, secs)
								: String.format(Locale.getDefault(), "%02d:%02d", minutes, secs);

				binding.headerDuration.setText(durationText);
			} else {
				binding.iconDuration.setVisibility(View.GONE);
				binding.headerDuration.setVisibility(View.GONE);
			}
		}
	}

	static class StageHeaderViewHolder extends RecyclerView.ViewHolder {
		private final ListStageHeaderBinding binding;

		StageHeaderViewHolder(ListStageHeaderBinding binding) {
			super(binding.getRoot());
			this.binding = binding;
		}

		void bind(String stageName) {
			binding.stageName.setText(stageName);
		}
	}
}
