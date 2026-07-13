# 用户指令记忆

本文件记录了用户的指令、偏好和教导，用于在未来的交互中提供参考。

## 格式

### 用户指令条目
[用户指令摘要]
- Date: YYYY-MM-DD
- Context: 提及的场景或时间
- Instructions:
  - 用户教导或指示的内容，逐行描述

### 项目知识条目
[项目知识摘要]
- Date: YYYY-MM-DD
- Context: Agent 在执行具体任务描述时发现
- Category: 运维部署|构建方法|测试方法|排错调试|工作流协作|环境配置
- Instructions:
  - 具体的知识点，逐行描述

## 条目

[Android 项目构建环境]
- Date: 2026-07-13
- Context: Agent 在执行 assembleDebug / testDebugUnitTest 构建时发现
- Category: 构建方法
- Instructions:
  - 本仓库为 Android 应用，构建前必须设置环境：ANDROID_HOME=/opt/android-sdk，JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64，PATH 含 $JAVA_HOME/bin
  - Android SDK 已安装于 /opt/android-sdk（cmdline-tools、platform-tools、platforms;android-34、build-tools;34.0.0）
  - Gradle 8.4 内置 HTTP client 与 dl.google.com 协商 TLS 失败（报 "Remote host terminated the handshake"），必须强制 export JAVA_TOOL_OPTIONS="-Djdk.tls.client.protocols=TLSv1.2" 才能解析依赖
  - 标准构建命令：export ANDROID_HOME=/opt/android-sdk; export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64; export PATH=$JAVA_HOME/bin:$PATH; export JAVA_TOOL_OPTIONS="-Djdk.tls.client.protocols=TLSv1.2"; ./gradlew assembleDebug --no-daemon
  - 单元测试命令：./gradlew testDebugUnitTest --no-daemon（同样需上述 TLS 环境变量）
