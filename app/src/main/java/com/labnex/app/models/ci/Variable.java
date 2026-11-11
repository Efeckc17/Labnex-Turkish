package com.labnex.app.models.ci;

import com.google.gson.annotations.SerializedName;
import java.io.Serializable;

/**
 * @author mmarif
 */
public class Variable implements Serializable {

	@SerializedName("key")
	private String key;

	@SerializedName("value")
	private String value;

	@SerializedName("variable_type")
	private String variableType;

	@SerializedName("protected")
	private boolean isProtected;

	@SerializedName("masked")
	private boolean isMasked;

	@SerializedName("hidden")
	private boolean isHidden;

	@SerializedName("raw")
	private boolean isRaw;

	@SerializedName("environment_scope")
	private String environmentScope;

	public String getKey() {
		return key;
	}

	public void setKey(String key) {
		this.key = key;
	}

	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}

	public String getVariableType() {
		return variableType;
	}

	public void setVariableType(String variableType) {
		this.variableType = variableType;
	}

	public boolean isProtected() {
		return isProtected;
	}

	public void setProtected(boolean aProtected) {
		isProtected = aProtected;
	}

	public boolean isMasked() {
		return isMasked;
	}

	public void setMasked(boolean masked) {
		isMasked = masked;
	}

	public boolean isHidden() {
		return isHidden;
	}

	public void setHidden(boolean hidden) {
		isHidden = hidden;
	}

	public boolean isRaw() {
		return isRaw;
	}

	public void setRaw(boolean raw) {
		isRaw = raw;
	}

	public String getEnvironmentScope() {
		return environmentScope;
	}

	public void setEnvironmentScope(String environmentScope) {
		this.environmentScope = environmentScope;
	}
}
