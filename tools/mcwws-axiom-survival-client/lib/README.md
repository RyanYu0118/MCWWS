# 编译依赖（请复制到本目录）

本模组需 **compileOnly** 以下 jar 才能编译（不会打包进产物）：

| 文件 | 来源 |
|------|------|
| `Axiom-5.5.0-for-MC26.2.jar` | 客户端 `.minecraft/mods/` |
| `minecraft-client-26.2.jar` | 官方客户端或 `bluemap/minecraft-client-26.2.jar` |
| `fabric-loader-*.jar` | `.minecraft/libraries/net/fabricmc/fabric-loader/` |
| `fabric-api-0.156.0+26.2.jar` | `.minecraft/mods/` |
| `mixin-0.8.7.jar` | Fabric loader 依赖 |

`build.ps1` 会自动在常见路径搜索；若失败请手动放入 `lib/`。
