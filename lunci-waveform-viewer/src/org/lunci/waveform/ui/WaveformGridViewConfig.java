/*
 *  Copyright (C) 2015 Lunci Hua
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *     http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 */
package org.lunci.waveform.ui;

import android.graphics.Color;
import android.os.Parcel;
import android.os.Parcelable;

public class WaveformGridViewConfig implements Parcelable, Cloneable{
	public int DataMaxValue = Integer.MAX_VALUE;
	public int DataMinValue = Integer.MIN_VALUE;
	public int NumColumns = 5;
	public int NumRows = 5;
	public int AxisXColor = 0xFF03A9F4;
	public int AxisYColor = 0xFF03A9F4;
	public int BackgroundColor = Color.BLACK;
    public boolean ShowGridBorderLeft=true;
    public boolean ShowGridBorderRight=true;
    public boolean ShowGridBorderTop=true;
    public boolean ShowGridBorderBottom=true;

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(this.DataMaxValue);
        dest.writeInt(this.DataMinValue);
        dest.writeInt(this.NumColumns);
        dest.writeInt(this.NumRows);
        dest.writeInt(this.AxisXColor);
        dest.writeInt(this.AxisYColor);
        dest.writeInt(this.BackgroundColor);
        dest.writeByte(ShowGridBorderLeft ? (byte) 1 : (byte) 0);
        dest.writeByte(ShowGridBorderRight ? (byte) 1 : (byte) 0);
        dest.writeByte(ShowGridBorderTop ? (byte) 1 : (byte) 0);
        dest.writeByte(ShowGridBorderBottom ? (byte) 1 : (byte) 0);
    }

    public WaveformGridViewConfig() {
    }

    @Override
    public WaveformGridViewConfig clone() {
        final WaveformGridViewConfig config=new WaveformGridViewConfig();
        cloneParams(config);
        return config;
    }

    public void cloneParams(WaveformGridViewConfig config){
        config.ShowGridBorderBottom=this.ShowGridBorderBottom;
        config.ShowGridBorderTop=this.ShowGridBorderTop;
        config.ShowGridBorderRight=this.ShowGridBorderRight;
        config.ShowGridBorderLeft=this.ShowGridBorderLeft;
        config.AxisXColor=this.AxisXColor;
        config.AxisYColor=this.AxisYColor;
        config.DataMaxValue=this.DataMaxValue;
        config.DataMinValue=this.DataMinValue;
        config.NumColumns=this.NumColumns;
        config.NumRows=this.NumRows;
        config.BackgroundColor=this.BackgroundColor;
    }

    private WaveformGridViewConfig(Parcel in) {
        this.DataMaxValue = in.readInt();
        this.DataMinValue = in.readInt();
        this.NumColumns = in.readInt();
        this.NumRows = in.readInt();
        this.AxisXColor = in.readInt();
        this.AxisYColor = in.readInt();
        this.BackgroundColor = in.readInt();
        this.ShowGridBorderLeft = in.readByte() != 0;
        this.ShowGridBorderRight = in.readByte() != 0;
        this.ShowGridBorderTop = in.readByte() != 0;
        this.ShowGridBorderBottom = in.readByte() != 0;
    }

    public static final Creator<WaveformGridViewConfig> CREATOR = new Creator<WaveformGridViewConfig>() {
        public WaveformGridViewConfig createFromParcel(Parcel source) {
            return new WaveformGridViewConfig(source);
        }

        public WaveformGridViewConfig[] newArray(int size) {
            return new WaveformGridViewConfig[size];
        }
    };
}
