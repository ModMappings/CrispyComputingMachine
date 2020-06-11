package org.modmappings.crispycomputingmachine.model.launcher;

import java.util.List;
import javax.annotation.Generated;
import com.google.gson.annotations.SerializedName;

@Generated("com.robohorse.robopojogenerator")
public class LauncherMetadata{

	@SerializedName("versions")
	private List<VersionsItem> versions;

	@SerializedName("latest")
	private Latest latest;

	public void setVersions(List<VersionsItem> versions){
		this.versions = versions;
	}

	public List<VersionsItem> getVersions(){
		return versions;
	}

	public void setLatest(Latest latest){
		this.latest = latest;
	}

	public Latest getLatest(){
		return latest;
	}

	@Override
 	public String toString(){
		return 
			"LauncherMetadata{" + 
			"versions = '" + versions + '\'' + 
			",latest = '" + latest + '\'' + 
			"}";
		}
}