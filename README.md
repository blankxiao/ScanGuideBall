# ScanGuideBall

用于引导采集视角信息的组件化项目，支持传感器驱动球体视角、球面点位收集反馈、进度展示，以及在 Demo 中与相机预览叠加展示。  

---

## 演示

![ScanGuideBall 演示](./docs/demo.gif)
![ScanGuideBall 演示](./docs/demo2.gif)

---

## 技术栈

| 模块 | 技术 |
|------|------|
| 引导球库 (guideball) | Kotlin、OpenGL ES、传感器（Rotation Vector）、Jetpack Compose |
| 示例 App (app) | Jetpack Compose、CameraX（预览）、AndroidX Lifecycle |

---

## 快速开始

1. 进入项目目录：
   ```bash
   cd ScanGuideBall
   ```
2. 用 **Android Studio** 打开项目根目录。
3. 连接设备或启动模拟器，运行 **app** 模块。

> 建议 Android SDK 26+，JDK 11+。

---

## 项目结构

```
ScanGuideBall/
├── guideball/     # 引导球渲染与采集库
├── app/           # 示例应用（相机 + 底部引导组件）
└── docs/          # 需求、概念与演示资源
```

---
