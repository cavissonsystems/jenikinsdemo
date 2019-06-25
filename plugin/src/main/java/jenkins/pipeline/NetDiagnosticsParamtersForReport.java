/*
 * The MIT License
 *
 * Copyright 2016 compass.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package jenkins.pipeline;

public class NetDiagnosticsParamtersForReport {
  
	
  private String curStartTime;
  private String curEndTime;
  private String baseStartTime;
  private String baseEndTime;
  private String initStartTime;
  private String initEndTime;
  private boolean prevDuration;
  private String checkProfilePath;
  private String critiThreshold;
  private String warThreshold;
  private String failThreshold;

  
public String getCritiThreshold() {
	return critiThreshold;
}
public void setCritiThreshold(String critiThreshold) {
	this.critiThreshold = critiThreshold;
}
public String getWarThreshold() {
	return warThreshold;
}
public void setWarThreshold(String warThreshold) {
	this.warThreshold = warThreshold;
}
public String getFailThreshold() {
	return failThreshold;
}
public void setFailThreshold(String failThreshold) {
	this.failThreshold = failThreshold;
}
public String getCurStartTime() {
	return curStartTime;
}
public void setCurStartTime(String curStartTime) {
	this.curStartTime = curStartTime;
}
public String getCurEndTime() {
	return curEndTime;
}
public void setCurEndTime(String curEndTime) {
	this.curEndTime = curEndTime;
}
public String getBaseStartTime() {
	return baseStartTime;
}
public void setBaseStartTime(String baseStartTime) {
	this.baseStartTime = baseStartTime;
}
public String getBaseEndTime() {
	return baseEndTime;
}
public void setBaseEndTime(String baseEndTime) {
	this.baseEndTime = baseEndTime;
}
public String getInitStartTime() {
	return initStartTime;
}
public void setInitStartTime(String initStartTime) {
	this.initStartTime = initStartTime;
}
public String getInitEndTime() {
	return initEndTime;
}
public void setInitEndTime(String initEndTime) {
	this.initEndTime = initEndTime;
}
public boolean isPrevDuration() {
	return prevDuration;
}
public void setPrevDuration(boolean prevDuration) {
	this.prevDuration = prevDuration;
}
public String getCheckProfilePath() {
	return checkProfilePath;
}
public void setCheckProfilePath(String checkProfilePath) {
	this.checkProfilePath = checkProfilePath;
}


@Override
public String toString() {
	return "curStartTime=" + curStartTime + ",curEndTime=" + curEndTime+ ",baseStartTime=" + baseStartTime + ",baseEndTime=" + baseEndTime + ",initStartTime=" + initStartTime+ ",initEndTime=" + initEndTime + ",prevDuration=" + prevDuration + ",checkProfilePath="+ checkProfilePath +",criThreshold="+critiThreshold +",warThreshold="+warThreshold +",failThreshold="+failThreshold;
}
  
  

	
}
