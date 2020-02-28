package org.modmappings.crispycomputingmachine.model.launcher;

import javax.annotation.Generated;
import com.google.gson.annotations.SerializedName;

@Generated("com.robohorse.robopojogenerator")
public class VersionsItem{

	@SerializedName("releaseTime")
	private String releaseTime;

	@SerializedName("id")
	private String id;

	@SerializedName("time")
	private String time;

	@SerializedName("type")
	private String type;

	@SerializedName("url")
	private String url;

	public void setReleaseTime(String releaseTime){
		this.releaseTime = releaseTime;
	}

	public String getReleaseTime(){
		return releaseTime;
	}

	public void setId(String id){
		this.id = id;
	}

	public String getId(){
		return id;
	}

	public void setTime(String time){
		this.time = time;
	}

	public String getTime(){
		return time;
	}

	public void setType(String type){
		this.type = type;
	}

	public String getType(){
		return type;
	}

	public void setUrl(String url){
		this.url = url;
	}

	public String getUrl(){
		return url;
	}

	@Override
 	public String toString(){
		return 
			"VersionsItem{" + 
			"releaseTime = '" + releaseTime + '\'' + 
			",id = '" + id + '\'' + 
			",time = '" + time + '\'' + 
			",type = '" + type + '\'' + 
			",url = '" + url + '\'' + 
			"}";
		}
}