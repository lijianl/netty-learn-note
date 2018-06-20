
# 修改git
+ git commit -m "Change repo." # 先把所有为保存的修改打包为一个commit
+ git remote remove origin # 删掉原来git源
+ git remote add origin [YOUR NEW .GIT URL] # 将新源地址写入本地版本库配置文件
+ git push -u origin master # 提交所有代码