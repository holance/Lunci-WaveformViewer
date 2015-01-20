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

public class WaveformViewConfig {
    public int DataMaxValue = Integer.MAX_VALUE;
    public int DataMinValue = Integer.MIN_VALUE;
    //	public int BackgroundColor = 0x00FFFFFF;
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
}
