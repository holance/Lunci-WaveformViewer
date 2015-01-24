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
    public int PlotThreadPriority = Thread.NORM_PRIORITY + 1;
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

    private WaveformViewConfig(Parcel in){
        super();
        DataMaxValue=in.readInt();
        DataMinValue=in.readInt();
        LineColor=in.readInt();
        AxisColor=in.readInt();
        PlotThreadPriority=in.readInt();
        VerticalZoom=in.readFloat();
        HorizontalZoom=in.readFloat();
        DefaultDataBufferSize=in.readInt();
        DrawingDeltaX=in.readInt();
        LeadingClearWidth=in.readInt();
        PaddingLeft=in.readInt();
        VerticalGestureMoveRatio=in.readFloat();
        final boolean[] boolArray=new boolean[4];
        in.readBooleanArray(boolArray);
        AutoPositionAfterZoom=boolArray[0];
        ShowFPS=boolArray[1];
        ShowCenterLineY=boolArray[2];
        EnableVerticalGestureMove=boolArray[3];
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(DataMaxValue);
        dest.writeInt(DataMinValue);
        dest.writeInt(LineColor);
        dest.writeInt(AxisColor);
        dest.writeInt(PlotThreadPriority);
        dest.writeFloat(VerticalZoom);
        dest.writeFloat(HorizontalZoom);
        dest.writeInt(DefaultDataBufferSize);
        dest.writeInt(DrawingDeltaX);
        dest.writeInt(LeadingClearWidth);
        dest.writeInt(PaddingLeft);
        dest.writeFloat(VerticalGestureMoveRatio);
        dest.writeBooleanArray(new boolean[]{AutoPositionAfterZoom, ShowFPS, ShowCenterLineY, EnableVerticalGestureMove});
    }

    public static Parcelable.Creator<WaveformViewConfig> CREATOR
            = new Parcelable.Creator<WaveformViewConfig>(){

        @Override
        public WaveformViewConfig createFromParcel(Parcel in){
            final WaveformViewConfig config=new WaveformViewConfig(in);
            return config;
        }

        @Override
        public WaveformViewConfig[] newArray(int size) {
            return new WaveformViewConfig[size];
        }
    };
}