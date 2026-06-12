.PHONY: clean build rebuild rebuild-no-daemon

# Clean up all build and cache directories aggressively
clean:
	@echo "--- Aggressively cleaning build and cache directories ---"
	./gradlew --stop
	rm -rf .gradle
	rm -rf app/.gradle
	rm -rf build
	rm -rf app/build
	./gradlew clean

# Standard debug build
build:
	@echo "--- Running standard debug build ---"
	./gradlew assembleDebug

# Clean and build
rebuild: clean build

# Clean and build without daemon (useful for avoiding persistent locks)
rebuild-no-daemon:
	@echo "--- Rebuilding without daemon to avoid locks ---"
	./gradlew --stop
	rm -rf .gradle
	rm -rf app/.gradle
	rm -rf build
	rm -rf app/build
	./gradlew clean
	./gradlew assembleDebug --no-daemon
