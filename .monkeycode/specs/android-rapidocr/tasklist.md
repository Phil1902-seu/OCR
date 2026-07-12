# 需求实施计划

- [x] 1. 初始化 Android 项目与构建配置
  - 创建 Android Studio 项目（Kotlin + Jetpack Compose）
  - 配置 Gradle：compileSdk 34、minSdk 26、targetSdk 34、Kotlin 1.9.22、Compose Compiler 1.5.10
  - 配置 NDK 25.2.9519653 + CMake 3.22.1，ABI 过滤 arm64-v8a / armeabi-v7a
  - 添加依赖：CameraX 1.3.1、Room 2.6.1、Hilt 2.50、ONNX Runtime 1.16.3、iText 7.2.5、Navigation Compose
  - 创建包结构：ui/、viewmodel/、domain/、data/、di/
  - 配置 Hilt（Application 类 + @HiltAndroidApp）
  - 配置 Compose Material3 主题
  - 参考：design.md 架构图、构建配置、依赖配置
  - [ ] 1.1 编写 Gradle 构建配置的单元验证脚本，确保版本号、依赖版本符合设计要求

- [x] 2. 集成 RapidOCR JNI 原生库
  - 创建 `src/main/cpp/CMakeLists.txt`，配置 ONNX Runtime + RapidOCR 编译
  - 创建 JNI 桥接层：`RapidOcrJni.cpp`，封装 Detection → Classification → Recognition 管线
  - 创建 Kotlin `RapidOcrEngine` 类，通过 JNI 调用 C++ 推理逻辑
  - 实现模型初始化（initialize）、图片推理（recognize）、资源释放（release）接口
  - 将 PP-OCRv6 small 模型文件（det/cls/rec）放入 `src/main/assets/models/`
  - 参考：design.md RapidOcrEngine 组件、OcrConfig 数据模型
  - [x] 2.1 编写 JNI 接口单元测试，验证 native 方法绑定正确（mock JNI 调用）
  - [x] 2.2 编写 OcrConfig 属性测试，验证配置参数类型和默认值正确性

- [x] 3. 实现数据模型与领域层
  - 创建领域模型：OcrResult、TextBox、HistoryRecord、OcrConfig、ImageMeta、ExportFormat、ModelType
  - 创建 `RecognizeTextUseCase`：封装 OCR 推理调用和错误处理
  - 创建 `ExportResultUseCase`：实现 TXT / PDF / Markdown / JSON 四种格式导出逻辑
  - 创建 `HistoryUseCase`：封装历史记录增删查操作
  - 参考：design.md 数据模型、ExportResultUseCase、组件和接口
  - [x] 3.1 编写领域模型单元测试，验证数据类序列化和默认值
  - [x] 3.2 编写 ExportResultUseCase 测试，验证四种格式输出内容正确性（Mock 文件写入）
  - [x] 3.3 编写属性测试：JSON 导出结果 SHALL 包含 boxes、fullText、elapsedMs 字段（正确性属性）
  - [x] 3.4 编写属性测试：Markdown 导出结果 SHALL 包含识别表格和原图引用

- [x] 4. 实现数据层与存储
  - 创建 Room 数据库：AppDatabase、HistoryDao、HistoryEntity
  - 实现 `HistoryRepository`：历史记录的 CRUD 和 Flow 查询
  - 实现 `ModelManager`：从 assets 复制模型到私有存储、模型切换（small/standard）
  - 实现 `OcrRepository`：封装 RapidOcrEngine，提供协程接口
  - 创建 Hilt AppModule：提供所有 Repository 和 UseCase 的依赖注入
  - 参考：design.md HistoryRepository、ModelManager、OcrRepository、依赖注入
  - [x] 4.1 编写 Room DAO 测试（Instrumented Test），验证 CRUD 操作和 Flow 查询
  - [x] 4.2 编写 ModelManager 单元测试，模拟 assets 复制流程和切换逻辑
  - [x] 4.3 编写属性测试：模型初始化 SHALL 幂等，重复调用返回成功且不产生文件重复

- [x] 5. 检查点 - 确保核心模块编译通过
  - 确保项目编译通过，如有疑问请询问用户
  - 验证模型从 assets 复制到本地存储
  - 验证 OCR 引擎可用一张测试图片完成端到端推理
  - 确保任务 2.1、2.2、3.1、3.2、3.3、3.4、4.1、4.2、4.3 测试通过

- [x] 6. 实现首页与相机拍照
  - 创建 `OcrViewModel`：管理 OCR 推理状态（空闲/加载中→成功/失败）
  - 实现 `HomeScreen`（Compose）：拍照按钮、选图按钮、加载指示器
  - 集成 CameraX：实现相机预览和拍照功能，拍照后跳转结果页
  - 实现相册选图：通过 ActivityResultContracts 获取图片 URI
  - 处理图片预处理：自动旋转校正（EXIF）、超限压缩（最大 4096px）
  - 参考：design.md HomeScreen、需求 1（相机拍照识别）、需求 2（相册选图识别）
  - [x] 6.1 编写 OcrViewModel 单元测试，验证状态流转（空闲→加载→成功/失败）
  - [x] 6.2 编写 Compose UI 测试，验证首页按钮交互和导航跳转
  - [x] 6.3 编写图片压缩函数单元测试，验证超限图片被正确压缩至 4096px

- [x] 7. 实现识别结果展示与编辑
  - 实现 `ResultScreen`（Compose）：展示识别文字列表、图片标注Overlay
  - 实现文字区域标注：在原始图片上绘制检测框，支持点击高亮对应文字
  - 实现编辑模式：允许用户修改识别文字
  - 实现复制操作：复制全部识别文字到系统剪贴板
  - 实现分享操作：调起系统分享面板，分享识别文字
  - 实现导出操作：弹出格式选择面板（TXT/PDF/Markdown/JSON）并调用 ExportResultUseCase
  - 自动保存识别记录到历史
  - 参考：design.md ResultScreen、需求 3（结果展示）、需求 4（编辑操作）、需求 7（导出）
  - [x] 7.1 编写 ResultScreen Compose UI 测试，验证标注绘制和点击交互
  - [x] 7.2 编写导出面板交互测试，验证四种格式选择触发正确的导出逻辑
  - [x] 7.3 编写属性测试：未检测到文字时 SHALL 显示"未检测到文字内容"（需求 3.4）
  - [x] 7.4 编写属性测试：复制成功后 SHALL 显示"已复制到剪贴板"提示（需求 4.4）

- [x] 8. 实现历史记录页
  - 实现 `HistoryViewModel`：加载历史记录流、删除、清空
  - 实现 `HistoryScreen`（Compose）：展示历史列表（缩略图+摘要+时间），按时间倒序
  - 实现列表项点击：跳转结果页展示完整详情
  - 实现滑动删除单条记录（同时删除缩略图文件）
  - 实现清空全部历史：弹出二次确认对话框
  - 参考：design.md HistoryScreen、需求 6（历史记录）
  - [x] 8.1 编写 HistoryViewModel 单元测试，验证列表加载、删除、清空逻辑
  - [x] 8.2 编写 Compose UI 测试，验证列表顺序为时间倒序、滑动删除交互
  - [x] 8.3 编写属性测试：清空历史 SHALL 弹出二次确认对话框（需求 6.4）
  - [x] 8.4 编写属性测试：删除历史记录 SHALL 同时删除对应缩略图文件（正确性属性 4）

- [x] 9. 实现设置页
  - 实现 `SettingsViewModel`：读取/写入模型偏好设置
  - 实现 `SettingsScreen`（Compose）：展示当前模型类型、切换按钮
  - 实现模型切换：复制新模型文件 → 重新初始化 OCR 引擎 → 验证可用
  - 参考：design.md SettingsScreen、需求 5（模型管理）、需求 5.5（模型切换）
  - [x] 9.1 编写 SettingsViewModel 单元测试，验证设置读取/写入和模型切换流程
  - [x] 9.2 编写属性测试：模型切换失败 SHALL 保持原模型可用（健壮性）

- [x] 10. 检查点 - 确保功能完整与测试通过
  - 确保所有功能编译通过，如有疑问请询问用户
  - 验证完整流程：拍照→识别→展示→编辑→导出→历史
  - 验证错误处理：权限拒绝、模型复制失败、OCR 推理异常、图片过大
  - 验证数据一致性：历史记录删除同时删除缩略图文件
  - 验证幂等性：模型初始化重复调用不产生副作用
  - 确保任务 6.1、6.2、6.3、7.1、7.2、7.3、7.4、8.1、8.2、8.3、8.4、9.1、9.2 测试通过
