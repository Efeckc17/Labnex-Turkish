package com.labnex.app.models.ci;

import com.google.gson.annotations.SerializedName;
import java.io.Serializable;
import java.util.List;

/**
 * @author mmarif
 */
public class Job implements Serializable {

	@SerializedName("id")
	private long id;

	@SerializedName("status")
	private String status;

	@SerializedName("stage")
	private String stage;

	@SerializedName("name")
	private String name;

	@SerializedName("ref")
	private String ref;

	@SerializedName("created_at")
	private String createdAt;

	@SerializedName("started_at")
	private String startedAt;

	@SerializedName("finished_at")
	private String finishedAt;

	@SerializedName("duration")
	private Float duration;

	@SerializedName("failure_reason")
	private String failureReason;

	@SerializedName("runner")
	private Runner runner;

	@SerializedName("artifacts")
	private List<Artifact> artifacts;

	@SerializedName("commit")
	private Commit commit;

	@SerializedName("pipeline")
	private Pipeline pipeline;

	@SerializedName("user")
	private User user;

	public User getUser() {
		return user;
	}

	public static class User implements Serializable {
		@SerializedName("id")
		private long id;

		public long getId() {
			return id;
		}
	}

	public static class Runner implements Serializable {
		@SerializedName("id")
		private long id;

		@SerializedName("name")
		private String name;

		@SerializedName("online")
		private boolean online;

		@SerializedName("description")
		private String description;

		public String getDescription() {
			return description;
		}

		public long getId() {
			return id;
		}

		public String getName() {
			return name;
		}

		public boolean isOnline() {
			return online;
		}
	}

	public static class Artifact implements Serializable {
		@SerializedName("file_type")
		private String fileType;

		@SerializedName("filename")
		private String filename;

		@SerializedName("size")
		private long size;

		public String getFileType() {
			return fileType;
		}

		public String getFilename() {
			return filename;
		}

		public long getSize() {
			return size;
		}
	}

	public static class Commit implements Serializable {
		@SerializedName("id")
		private String id;

		@SerializedName("short_id")
		private String shortId;

		@SerializedName("title")
		private String title;

		@SerializedName("author_name")
		private String authorName;

		public String getId() {
			return id;
		}

		public String getShortId() {
			return shortId;
		}

		public String getTitle() {
			return title;
		}

		public String getAuthorName() {
			return authorName;
		}
	}

	public static class Pipeline implements Serializable {
		@SerializedName("archived")
		private boolean archived;

		public boolean isArchived() {
			return archived;
		}
	}

	public Pipeline getPipeline() {
		return pipeline;
	}

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public String getStage() {
		return stage;
	}

	public void setStage(String stage) {
		this.stage = stage;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getRef() {
		return ref;
	}

	public void setRef(String ref) {
		this.ref = ref;
	}

	public String getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(String createdAt) {
		this.createdAt = createdAt;
	}

	public String getStartedAt() {
		return startedAt;
	}

	public void setStartedAt(String startedAt) {
		this.startedAt = startedAt;
	}

	public String getFinishedAt() {
		return finishedAt;
	}

	public void setFinishedAt(String finishedAt) {
		this.finishedAt = finishedAt;
	}

	public Float getDuration() {
		return duration;
	}

	public void setDuration(Float duration) {
		this.duration = duration;
	}

	public String getFailureReason() {
		return failureReason;
	}

	public void setFailureReason(String failureReason) {
		this.failureReason = failureReason;
	}

	public Runner getRunner() {
		return runner;
	}

	public void setRunner(Runner runner) {
		this.runner = runner;
	}

	public List<Artifact> getArtifacts() {
		return artifacts;
	}

	public void setArtifacts(List<Artifact> artifacts) {
		this.artifacts = artifacts;
	}

	public Commit getCommit() {
		return commit;
	}

	public void setCommit(Commit commit) {
		this.commit = commit;
	}
}
