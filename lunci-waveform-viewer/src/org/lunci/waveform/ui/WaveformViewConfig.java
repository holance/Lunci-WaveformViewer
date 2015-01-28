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

import android.os.Parcel;
import android.os.Parcelable;

public class WaveformViewConfig implements Parcelable, Cloneable{
    public int DataMaxValue = Integer.MAX_VALUE;
    public int DataMinValue = Integer.MIN_VALUE;
    public int LineColor = 0xFF00FF00;
    public int AxisColor = 0xA02196F3;
    public int PlotThreadPriority = Thread.NORM_PRIORITY;
    public float VerticalZoom = 1;
    public float HorizontalZoom = 1;
    public boolean AutoPositionAfterZoom = false;
    public int DefaultDataBufferSize = 1000;
    public int DrawingDeltaX = 8;
    public boolean ShowFPS = true;
    public int LeadingClearWidth = 16;
    public boolean ShowCenterLineY = true;
    public int PaddingLeft = 0;
    public boolean EnableVerticalGestureMove = false;
    public float VerticalGestureMoveRatio = 1;

    public WaveformViewConfig(){}

    @Override
    public WaveformViewConfig clone() {
        final WaveformViewConfig config=new WaveformViewConfig();
        cloneParams(config);
        return config;
    }

    public void cloneParams(WaveformViewConfig config){
        config.VerticalZoom=VerticalZoom;
        config.EnableVerticalGestureMove=EnableVerticalGestureMove;
        config.DataMinValue=DataMinValue;
        config.DataMaxValue=DataMaxValue;
        config.LineColor=LineColor;
        config.AxisColor=AxisColor;
        config.PlotThreadPriority=PlotThreadPriority;
        config.HorizontalZoom=HorizontalZoom;
        config.AutoPositionAfterZoom=AutoPositionAfterZoom;
        config.DefaultDataBufferSize=DefaultDataBufferSize;
        config.DrawingDeltaX=DrawingDeltaX;
        config.ShowFPS=ShowFPS;
        config.LeadingClearWidth=LeadingClearWidth;
        config.ShowCenterLineY=ShowCenterLineY;
        config.PaddingLeft=PaddingLeft;
        config.VerticalGestureMoveRatio=VerticalGestureMoveRatio;
    }


    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(this.DataMaxValue);
        dest.writeInt(this.DataMinValue);
        dest.writeInt(this.LineColor);
        dest.writeInt(this.AxisColor);
        dest.writeInt(this.PlotThreadPriority);
        dest.writeFloat(this.VerticalZoom);
        dest.writeFloat(this.HorizontalZoom);
        dest.writeByte(AutoPositionAfterZoom ? (byte) 1 : (byte) 0);
        dest.writeInt(this.DefaultDataBufferSize);
        dest.writeInt(this.DrawingDeltaX);
        dest.writeByte(ShowFPS ? (byte) 1 : (byte) 0);
        dest.writeInt(this.LeadingClearWidth);
        dest.writeByte(ShowCenterLineY ? (byte) 1 : (byte) 0);
        dest.writeInt(this.PaddingLeft);
        dest.writeByte(EnableVerticalGestureMove ? (byte) 1 : (byte) 0);
        dest.writeFloat(this.VerticalGestureMoveRatio);
    }

    private WaveformViewConfig(Parcel in) {
        this.DataMaxValue = in.readInt();
        this.DataMinValue = in.readInt();
        this.LineColor = in.readInt();
        this.AxisColor = in.readInt();
        this.PlotThreadPriority = in.readInt();
        this.VerticalZoom = in.readFloat();
        this.HorizontalZoom = in.readFloat();
        this.AutoPositionAfterZoom = in.readByte() != 0;
        this.DefaultDataBufferSize = in.readInt();
        this.DrawingDeltaX = in.readInt();
        this.ShowFPS = in.readByte() != 0;
        this.LeadingClearWidth = in.readInt();
        this.ShowCenterLineY = in.readByte() != 0;
        this.PaddingLeft = in.readInt();
        this.EnableVerticalGestureMove = in.readByte() != 0;
        this.VerticalGestureMoveRatio = in.readFloat();
    }

    public static final Creator<WaveformViewConfig> CREATOR = new Creator<WaveformViewConfig>() {
        public WaveformViewConfig createFromParcel(Parcel source) {
            return new WaveformViewConfig(source);
        }

        public WaveformViewConfig[] newArray(int size) {
            return new WaveformViewConfig[size];
        }
    };
}