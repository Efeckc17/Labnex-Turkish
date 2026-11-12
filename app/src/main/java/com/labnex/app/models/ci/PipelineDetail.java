package com.labnex.app.models.ci;

import com.google.gson.annotations.SerializedName;
import java.util.List;

/**
 * @author mmarif
 */
public class PipelineDetail extends Pipeline {

	@SerializedName("stages")
	private List<String> stages;

	@SerializedName("detailed_status")
	private DetailedStatus detailedStatus;

	public PipelineDetail() {
		super();
	}

	public static class DetailedStatus {
		@SerializedName("icon")
		private String icon;

		@SerializedName("text")
		private String text;

		@SerializedName("label")
		private String label;

		@SerializedName("group")
		private String group;

		public String getIcon() {
			return icon;
		}

		public String getText() {
			return text;
		}

		public String getLabel() {
			return label;
		}

		public String getGroup() {
			return group;
		}
	}

	public List<String> getStages() {
		return stages;
	}

	public void setStages(List<String> stages) {
		this.stages = stages;
	}

	public DetailedStatus getDetailedStatus() {
		return detailedStatus;
	}

	public void setDetailedStatus(DetailedStatus detailedStatus) {
		this.detailedStatus = detailedStatus;
	}
}
