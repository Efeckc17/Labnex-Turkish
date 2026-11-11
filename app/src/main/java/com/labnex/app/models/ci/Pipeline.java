package com.labnex.app.models.ci;

import com.google.gson.annotations.SerializedName;
import java.io.Serializable;

/**
 * @author mmarif
 */
public class Pipeline implements Serializable {

	@SerializedName("id")
	private long id;

	@SerializedName("iid")
	private long iid;

	@SerializedName("project_id")
	private long projectId;

	@SerializedName("status")
	private String status;

	@SerializedName("source")
	private String source;

	@SerializedName("ref")
	private String ref;

	@SerializedName("sha")
	private String sha;

	@SerializedName("web_url")
	private String webUrl;

	@SerializedName("created_at")
	private String createdAt;

	@SerializedName("updated_at")
	private String updatedAt;

	@SerializedName("user")
	private User user;

	@SerializedName("commit")
	private Commit commit;

	@SerializedName("duration")
	private Long duration;

	@SerializedName("queued_duration")
	private Long queuedDuration;

	public Pipeline() {}

	public static class User implements Serializable {
		@SerializedName("id")
		private long id;

		@SerializedName("name")
		private String name;

		@SerializedName("username")
		private String username;

		@SerializedName("avatar_url")
		private String avatarUrl;

		public long getId() {
			return id;
		}

		public String getName() {
			return name;
		}

		public String getUsername() {
			return username;
		}

		public String getAvatarUrl() {
			return avatarUrl;
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

		@SerializedName("author_email")
		private String authorEmail;

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

		public String getAuthorEmail() {
			return authorEmail;
		}
	}

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public long getIid() {
		return iid;
	}

	public void setIid(long iid) {
		this.iid = iid;
	}

	public long getProjectId() {
		return projectId;
	}

	public void setProjectId(long projectId) {
		this.projectId = projectId;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public String getSource() {
		return source;
	}

	public void setSource(String source) {
		this.source = source;
	}

	public String getRef() {
		return ref;
	}

	public void setRef(String ref) {
		this.ref = ref;
	}

	public String getSha() {
		return sha;
	}

	public void setSha(String sha) {
		this.sha = sha;
	}

	public String getWebUrl() {
		return webUrl;
	}

	public void setWebUrl(String webUrl) {
		this.webUrl = webUrl;
	}

	public String getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(String createdAt) {
		this.createdAt = createdAt;
	}

	public String getUpdatedAt() {
		return updatedAt;
	}

	public void setUpdatedAt(String updatedAt) {
		this.updatedAt = updatedAt;
	}

	public User getUser() {
		return user;
	}

	public void setUser(User user) {
		this.user = user;
	}

	public Commit getCommit() {
		return commit;
	}

	public Long getDuration() {
		return duration;
	}

	public Long getQueuedDuration() {
		return queuedDuration;
	}
}
