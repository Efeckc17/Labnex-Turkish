package com.labnex.app.models.ci;

import com.google.gson.annotations.SerializedName;

/**
 * @author mmarif
 */
public class CrudeVariable {
	@SerializedName("key")
	private String key;

	@SerializedName("value")
	private String value;

	@SerializedName("variable_type")
	private String variableType;

	@SerializedName("protected")
	private Boolean isProtected;

	@SerializedName("masked")
	private Boolean isMasked;

	@SerializedName("environment_scope")
	private String environmentScope;

	@SerializedName("description")
	private String description;

	public void setKey(String key) {
		this.key = key;
	}

	public void setValue(String value) {
		this.value = value;
	}

	public void setVariableType(String variableType) {
		this.variableType = variableType;
	}

	public void setProtected(Boolean isProtected) {
		this.isProtected = isProtected;
	}

	public void setMasked(Boolean isMasked) {
		this.isMasked = isMasked;
	}

	public void setEnvironmentScope(String environmentScope) {
		this.environmentScope = environmentScope;
	}

	public void setDescription(String description) {
		this.description = description;
	}
}
