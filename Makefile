.PHONY: build test lint fmt package run

build:
	./gradlew build

test:
	./gradlew test

lint:
	./gradlew spotlessCheck

fmt:
	./gradlew spotlessApply

package:
	./gradlew :alle-app:shadowJar

run: package
	java -jar alle-app/build/libs/alle-app-*-all.jar
