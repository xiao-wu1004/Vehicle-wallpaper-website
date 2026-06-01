# Vehicle Wallpaper Backend

这是当前项目的 Java 后端，适合直接用 IntelliJ IDEA 打开 `backend/` 目录进行开发。

## 技术栈

- Java 8
- Spring Boot 2.7
- Spring Web
- Spring Data JPA
- H2 文件数据库
- Maven Wrapper

## 已实现能力

- `GET /` 重定向到现有前端主页 `main.html`
- 直接托管仓库根目录下的前端文件和 `cars/` 壁纸素材
- `GET /api/catalog` 返回完整品牌壁纸目录
- `GET /api/catalog/brands/{brandSlug}` 返回单个品牌的壁纸列表
- `GET /api/catalog/wallpapers` 支持按品牌和关键字搜索
- `GET /api/feedback/highlights` 返回高亮用户反馈
- `POST /api/feedback` 接收反馈表单并写入 H2

## 在 IDEA 中运行

1. 用 IDEA 打开 [`backend/pom.xml`](D:\Codes\vscodecodes\Web前端课程设计\backend\pom.xml)。
2. 等待 Maven 依赖同步完成。
3. 确认项目 SDK 为 Java 8。
4. 运行 `com.vehiclewallpaper.backend.BackendApplication`。
5. 浏览器访问 `http://localhost:8080/`。

## 命令行运行

Windows:

```powershell
cd backend
.\mvnw.cmd spring-boot:run
```

运行测试：

```powershell
cd backend
.\mvnw.cmd test
```

## 本地数据

- H2 数据文件默认写在 `backend/data/vehicle-wallpaper*`
- H2 控制台：`http://localhost:8080/h2-console`
- JDBC URL：`jdbc:h2:file:./data/vehicle-wallpaper`
