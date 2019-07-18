# MediaLearn
`MediaLearn`是一个旨在分享音视频开发知识的项目，目前仅仅局限于Android平台，后续会逐步扩展。<br>因为项目是以学习为目的，所以暂时不支持作为开发工具库使用，因为有少部分的代码具有一点实验性质，并没有特别好地进行封装。但大部分的功能我都讲耦合性处理地很低，譬如[Audio模块](https://github.com/JadynAi/MediaLearn/tree/master/mediakit/src/main/java/com/jadyn/mediakit/audio)。而[视频模块中](https://github.com/JadynAi/MediaLearn/tree/master/mediakit/src/main/java/com/jadyn/mediakit/video)，解码相关的功能趋近完整，但有些许功能我仍在持续不断地优化。<br>**本项目仅供学习参考使用**

## Camera2 视频录制

#### Camera2录制视频(一)：音频的录制及编码，[点击传送门](https://juejin.im/post/5d130936e51d45777b1a3dc8)

#### Camera2录制视频(二)：MediaCodeC+OpenGL视频编码，[传送门](https://juejin.im/post/5d2c12fdf265da1bae392b4c)

## MediaCodeC硬编码

#### MediaCodeC硬编码将图片集编码为视频Mp4文件[MediaCodeC编码视频](https://ailoli.me/2019/04/01/2019-04-01-MediaCodeC-encoder1/)

#### MediaCodeC将视频完整解码，并存储为图片文件。使用两种不同的方式，[硬编码解码视频](https://ailoli.me/2019/01/25/2019-01-25-MediaCodeC-Decode-1/)

### MediaCodeC解码视频指定帧[硬编码解码指定帧](https://ailoli.me/2019/02/09/2019-02-09-MediaCodeC-frame/)



## LICENSE

    Copyright [JadynAi]

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
