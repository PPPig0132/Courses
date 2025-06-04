# 学习 CMU 15-445 课程并提交项目指南

## 1. 学习课程内容
- **课程简介**：CMU 15-445 是一门高质量的数据库入门课程，由数据库领域专家 Andy Pavlo 讲授。课程使用 C++ 编程语言，要求学生实现一个关系型数据库的关键组件。
- **先修要求**：建议先修 C++、数据结构与算法，以及 CMU 15-213（CS:APP）。
- **课程资源**：
  - **课程网站**：访问 [课程官网](https://15445.courses.cs.cmu.edu/spring2023/schedule.html) 获取最新的课程安排、讲义、视频等。
  - **课程视频**：可以在 [YouTube](https://www.youtube.com/) 上观看课程视频。
  - **教材**：推荐使用《Database System Concepts》。

## 2. 环境搭建
- **下载代码**：从 [GitHub](https://github.com/cmu-db/bustub) 下载 `bustub` 项目的最新版本。
- **安装依赖**：在 Linux 环境下，运行以下命令安装必要的依赖：
  ```bash
  sudo apt-get update
  sudo apt-get install -y cmake make clang-format clang-tidy
  ```
  
- **编译项目**：
```mkdir build
cd build
cmake ..
make
  ```
  
  
## 3. 完成项目
- **项目内容**：课程包含多个项目，如缓冲池管理器（Buffer Pool Manager）、B+树索引、查询执行器与优化器、并发控制等。

- **开发与测试**：通过shell.cpp编译出bustub-shell，实时测试实现的组件是否正确。
  

## 4. 提交项目
- **代码风格检查**：在提交前，确保代码风格符合要求：
```make format
make check-lint
make check-clang-tidy-p0
  ```

- **打包提交**：将项目代码打包为zip文件，并上传到Gradescope平台：
```
make submit-p0
  ```
  然后按照提示操作，生成GRADESCOPE.md文件。
  - **注册Gradescope**：将项目代码打包为zip文件，并上传到Gradescope平台：