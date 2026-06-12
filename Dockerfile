FROM thyrlian/android-sdk:latest

WORKDIR /app

# 設定環境變數
ENV GRADLE_USER_HOME=/app/.gradle

# 預設執行權限
RUN chmod +x gradlew
