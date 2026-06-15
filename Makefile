.PHONY: clean build debug release setup

# 初始化發布目錄
setup:
	mkdir -p release

# 清理環境
clean:
	@echo "--- Cleaning project ---"
	./gradlew clean
	rm -rf .gradle
	rm -rf release/*.apk

# 本地編譯 (如果不使用 Docker)
build:
	./gradlew assembleDebug

# 使用 Docker 編譯 Debug APK
debug: setup
	@echo "--- Building Debug APK in Docker ---"
	docker-compose run --rm builder bash -c "./gradlew assembleDebug && cp app/build/outputs/apk/debug/*.apk ./release/"


# 使用 Docker 編譯 Release APK
release: setup
	@echo "--- Building Release APK in Docker ---"
	docker-compose run --rm builder bash -c "./gradlew assembleRelease && cp app/build/outputs/apk/release/*.apk ./release/"

