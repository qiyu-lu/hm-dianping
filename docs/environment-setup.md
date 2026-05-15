# 本地环境与常见问题

## 后端接口检查

可以访问下面的接口检查后端是否正常启动：

```text
http://localhost:8083/shop-type/list
```

如果通过前端和 Nginx 访问，打开浏览器开发者模式，切换到手机模式后访问：

```text
http://localhost:8080/
```

![手机模式图标](../figure/手机模式.png)

## JDK 版本问题

如果运行或编译时报错：

```text
java: java.lang.NoSuchFieldError:
Class com.sun.tools.javac.tree.JCTree$JCImport does not have member field 'com.sun.tools.javac.tree.JCTree qualid'
```

通常是 JDK 版本和项目依赖不匹配。该项目建议使用 JDK 8。

### IDEA Project SDK

路径：

```text
File -> Project Structure -> Project
```

设置：

```text
Project SDK: JDK 1.8
Project language level: 8
```

### IDEA Module SDK

路径：

```text
File -> Project Structure -> Modules -> hmdp -> Dependencies
```

设置：

```text
Module SDK: JDK 1.8
```

### Maven Runner

路径：

```text
Settings -> Build Tools -> Maven -> Runner
```

设置：

```text
JRE: Project JDK
```
