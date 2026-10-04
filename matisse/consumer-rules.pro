# Coil 与 Glide 均为 compileOnly 依赖，宿主通常只引入其中之一；
# 未引入的一方在 CoilImageEngine / GlideImageEngine 中的引用不应导致 R8 报缺失类
-dontwarn coil3.**
-dontwarn com.bumptech.glide.**