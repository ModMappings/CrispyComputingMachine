package org.modmappings.crispycomputingmachine.model.launcher;

import javax.annotation.Generated;
import com.google.gson.annotations.SerializedName;

@Generated("com.robohorse.robopojogenerator")
public class Latest{

	@SerializedName("release")
	private String release;

	@SerializedName("snapshot")
	private String snapshot;

	public void setRelease(String release){
		this.release = release;
	}

	public String getRelease(){
		return release;
	}

	public void setSnapshot(String snapshot){
		this.snapshot = snapshot;
	}

	public String getSnapshot(){
		return snapshot;
	}

	@Override
 	public String toString(){
		return 
			"Latest{" + 
			"release = '" + release + '\'' + 
			",snapshot = '" + snapshot + '\'' + 
			"}";
		}
}