# 需求实施计划

- [x] 1. 修改项目构建配置和清单文件
  - 修改 `app/build.gradle.kts`：移除 `ndkVersion`、`externalNativeBuild`、`ndk { abiFilters }` 配置块，移除 ONNX Runtime 依赖，新增 Retrofit、OkHttp、Gson、EncryptedSharedPreferences、MockWebServer(test) 依赖，versionCode 递增为 2、versionName 改为 "2.0"
  - 修改 `AndroidManifest.xml`：新增 `INTERNET` 和 `ACCESS_NETWORK_STATE` 权限声明，引用 `@xml/network_security_config`
  - 新增 `res/xml/network_security_config.xml`：配置仅允许 HTTPS 访问 `aip.baidubce.com`
  - 参考：需求-非功能性需求/安全性，设计-构建配置（修改后）、网络安全配置

- [x] 2. 移除旧的本地 OCR 推理相关代码和资源
  - 删除 `data/ocr/RapidOcrEngine.kt`、`data/ocr/impl/OnnxOcrEngine.kt`、`data/ocr/impl/ImagePreprocessor.kt`、`data/ocr/impl/ImageUtils.kt`、`data/ocr/OcrConfig.kt`
  - 删除 `data/local/model/ModelManager.kt`、`util/OnnxModelHelper.kt`
  - 删除 `src/main/cpp/` 目录（如存在）和 `assets/models/` 目录
  - 更新 `di/AppModule.kt`：移除 `ModelManager` 相关 provider（如有）
  - 更新 `di/ViewModelModule.kt`：移除对 `ModelManager` 的依赖绑定
  - 参考：设计-重构范围（删除项）

- [x] 3. 修改领域模型层
  - 修改 `domain/model/Models.kt`：将 `ModelType` 枚举替换为 `OcrMode` 枚举（STANDARD / HIGH_ACCURACY / HIGH_ACCURACY_WITH_LOCATION）
  - 修改 `HistoryRecord` 数据类：新增 `ocrMode: String` 字段，默认值为 `OcrMode.STANDARD.name`
  - 修改 `data/local/entity/HistoryEntity.kt`：新增 `ocrMode` 字段并设置默认值，用于 Room 增量迁移
  - 修改 `data/local/database/AppDatabase.kt`：将数据库版本号递增，配置 fallbackToDestructiveMigration 或 Migration 方案以增量迁移保留旧历史记录
  - 参考：需求-需求7（历史记录含识别模式），设计-数据库实体（修改后）

- [x] 4. 创建网络层数据传输对象（DTO）
  - 新增 `data/remote/dto/TokenResponseDto.kt`：包含 access_token、expires_in、error、error_description 字段
  - 新增 `data/remote/dto/OcrResponseDto.kt`：标准/高精度响应（words_result_num、words_result、error_code、error_msg、log_id）
  - 新增 `data/remote/dto/OcrResponseWithLocationDto.kt`：含位置响应（words_result 含 location 字段）
  - 新增 DTO 到领域模型的扩展函数 `toDomainModel(elapsedMs)`，处理位置坐标到 PointF 四边形的转换
  - 参考：设计-数据模型（DTO 数据传输对象、DTO 到领域模型转换）

- [x] 5. 实现 Retrofit API 接口定义
  - 新增 `data/remote/BaiduAuthApi.kt`：定义 `getAccessToken` 接口（POST `oauth/2.0/token`，FormUrlEncoded）
  - 新增 `data/remote/BaiduOcrApi.kt`：定义 `recognizeGeneralBasic`、`recognizeAccurateBasic`、`recognizeAccurate` 三个接口（均使用 access_token 查询参数 + image Base64 表单字段 + language_type）
  - 参考：设计-组件和接口（BaiduOcrApi、BaiduAuthApi）

- [x] 6. 实现 NetworkModule 依赖注入
  - 新增 `di/NetworkModule.kt`：提供 OkHttpClient（含 15 秒连接超时、30 秒读取超时、LoggingInterceptor）、GsonConverterFactory、Retrofit 实例（base URL: `https://aip.baidubce.com/`）
  - 提供 `BaiduOcrApi` 和 `BaiduAuthApi` 的 Retrofit 实现绑定
  - 参考：设计-正确性属性（网络超时）、依赖配置（Retrofit + OkHttp）

- [x] 7. 实现 CredentialManager 凭证管理
  - 新增 `data/remote/CredentialManager.kt`：基于 `EncryptedSharedPreferences` 存储 API Key 和 Secret Key
  - 实现 `saveCredentials`、`getApiKey`、`getSecretKey`、`hasCredentials`、`clearCredentials`、`testCredentials` 方法
  - 使用 MasterKey (AES256-GCM) + Android Keystore 保证加密存储
  - 参考：需求-需求5（API 凭证管理），设计-组件和接口（CredentialManager）

- [x] 8. 实现 TokenManager 令牌管理
  - 新增 `data/remote/TokenManager.kt`：access_token 缓存（SharedPreferences）、过期检查（提前 5 分钟刷新）、并发刷新（Mutex）
  - 实现 `getValidToken`：检查有效性，必要时调用 `refreshToken`，使用 Mutex 保证并发只刷新一次
  - 实现 `refreshToken`：调用 BaiduAuthApi 获取新 token，记录过期时间戳，凭证无效时返回 Result.failure
  - 实现 `isTokenValid`、`clearToken`
  - 参考：需求-需求5（验收标准3-4），设计-组件和接口（TokenManager）、正确性属性2-3

- [x] 9. 实现 ImageEncoder 图片编码工具
  - 新增 `data/util/ImageEncoder.kt`：将 Bitmap 编码为 Base64 字符串，自动压缩至不超过 4MB
  - 实现压缩循环：从 quality=100 递减至 10，每次降低 10，直到字节数达标
  - 使用 `Base64.NO_WRAP` 避免换行符干扰 API 解析
  - 参考：需求-非功能性需求/性能（图片压缩），设计-组件和接口（ImageEncoder）、百度 API 图片大小限制

- [x] 10. 实现 BaiduOcrService OCR 服务
  - 新增 `data/remote/BaiduOcrService.kt`：封装 OCR 调用全流程
  - 实现 `recognize(bitmap, mode)`：图片编码 → 获取 token → 根据 OcrMode 分发到对应 API → 处理 error_code（110/111 刷新重试、18 延迟重试、17 提示额度、216201/216202 处理）→ 转换为领域模型
  - 实现分层重试策略：token 失效自动刷新后重试 1 次、QPS 超限延迟 500ms 重试 2 次、其他错误不自动重试
  - 参考：需求-需求6（识别模式切换），设计-组件和接口（BaiduOcrService）、错误处理表、正确性属性4

- [x] 11. 修改 OcrRepository 数据仓库
  - 修改 `data/repository/OcrRepository.kt`：移除 `RapidOcrEngine` 和 `ModelManager` 依赖，注入 `BaiduOcrService` 和 `CredentialManager`
  - 修改 `recognize` 方法签名：新增 `mode: OcrMode` 参数，调用前检查凭证是否存在
  - 移除 `initialize` 方法和 `initialized` 状态（在线模式无需初始化模型）
  - 参考：设计-组件和接口（OcrRepository 修改后）、重构范围

- [x] 12. 修改 ViewModel 层
  - 修改 `viewmodel/OcrViewModel.kt`：`recognize` 方法增加 `OcrMode` 参数传递，调用 `RecognizeTextUseCase` 时传入模式
  - 修改 `domain/usecase/RecognizeTextUseCase.kt`：增加 `OcrMode` 参数，转发至 OcrRepository
  - 修改 `viewmodel/SettingsViewModel.kt`：移除 `ModelManager` 依赖，注入 `CredentialManager` 和 `TokenManager`；提供凭证保存、测试连接、识别模式切换的 StateFlow
  - 修改 `viewmodel/InitializationViewModel.kt`：检查凭证是否已配置，决定首次启动引导跳转
  - 参考：需求-需求5/需求6，设计-模块划分（ViewModel 层）

- [x] 13. 适配和优化 UI 层
  - 修改 `ui/settings/SettingsScreen.kt`：将"模型切换"替换为"识别模式"选择（STANDARD / HIGH_ACCURACY / HIGH_ACCURACY_WITH_LOCATION），新增 API 凭证配置区（API Key、Secret Key 输入框 + 保存按钮 + 测试连接按钮）
  - 修改 `ui/setup/FirstLaunchScreen.kt`：引导用户配置 API 凭证，保存后验证并跳转主页
  - 修改 `ui/home/HomeScreen.kt`：增加网络状态检测，无网络时禁用识别按钮并显示"网络不可用"提示
  - 复用保留 `ui/result/ResultScreen.kt`、`ui/history/HistoryScreen.kt`、`ui/camera/CameraScreen.kt` 的现有交互逻辑
  - 参考：需求-需求5/需求6，设计-模块划分（UI 层复用但优化）、用户确认决策（无网络禁用+提示）

- [x] 14. 检查点 - 确保所有测试通过
  - 确保所有测试通过,如有疑问请询问用户
  - 运行 `./gradlew assembleDebug` 验证编译通过
  - 运行 `./gradlew test` 验证单元测试通过

- [x] 15. 更新测试代码
  - [ ]* 15.1 修改现有测试以适配新架构
    - 删除 `OnnxOcrEngineTest.kt`、`RapidOcrEngineTest.kt`、`ModelManagerTest.kt`、`OcrConfigTest.kt`（对应已删除的类）
    - 修改 `OcrViewModelTest.kt`：适配新增的 OcrMode 参数
    - 修改 `HistoryRepositoryTest.kt`：适配新增的 ocrMode 字段
  - [ ]* 15.2 为 CredentialManager 编写单元测试
    - 验证凭证保存、读取、清除、存在性检查逻辑
  - [ ]* 15.3 为 TokenManager 编写单元测试
    - 模拟 BaiduAuthApi，验证 token 缓存、过期刷新、并发刷新（Mutex）行为
  - [ ]* 15.4 为 BaiduOcrService 编写单元测试
    - 使用 MockWebServer 模拟 API 响应，验证各模式调用、error_code 处理、分层重试逻辑
  - [ ]* 15.5 为 ImageEncoder 编写单元测试
    - 验证图片压缩至目标大小、Base64 编码正确性
  - [ ]* 15.6 为 DTO 转换编写单元测试
    - 验证 OcrResponseDto、OcrResponseWithLocationDto 到 OcrResult 的转换正确性
    - 验证 LocationDto 四边形坐标转换逻辑
