# 编译依赖（请复制到本目录）

本模组需 **compileOnly** 以下 jar 才能编译（不会打包进产物）：

| 文件 | 来源 |
|------|------|
| `Axiom-*-for-MC26.3.jar` | 客户端 `.minecraft/mods/`（`build.ps1` 也会自动搜） |
| `minecraft-client-26.3.jar` | 官方客户端 `versions/26.3/26.3.jar` 复制并改名 |
| `fabric-loader-*.jar` | `.minecraft/libraries/net/fabricmc/fabric-loader/` |
| `fabric-api-*-26.3.jar` | `.minecraft/mods/` |
| `sponge-mixin-*.jar` | Fabric loader 依赖 |

`build.ps1` 会自动在常见路径搜索 Axiom；其余请放入 `lib/`。
