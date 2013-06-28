package dentex.youtube.downloader;
/*
 * code adapted from: 
 * https://github.com/survivingwithandroid/Surviving-with-android/tree/master/SimpleList
 * 
 * Copyright (C) 2012 jfrankie (http://www.survivingwithandroid.com)
 * Copyright (C) 2012 Surviving with Android (http://www.survivingwithandroid.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

public class DashboardListItem {
	
	private String id;
	private String status;
	private String path;
	private String filename;
	private String audioFilename;
	private String size;


	public DashboardListItem(String id, String status, String path, String filename, String audioSuffix, String size) {
		this.id = id;
		this.status = status;
		this.path = path;
		this.filename = filename;
		this.audioFilename = audioSuffix;
		this.size = size;

	}
	
	public String getId() {
		return id;
	}
	
	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public String getPath() {
		return path;
	}
	
	public void setPath(String name) {
		this.path = name;
	}
	public String getFilename() {
		return filename;
	}
	
	public void setFilename(String filename) {
		this.filename = filename;
	}
	
	public String getAudioFilename() {
		return audioFilename;
	}
	
	public void setAudioFilename(String audioFilename) {
		this.audioFilename = audioFilename;
	}

	public String getSize() {
		return size;
	}
}

