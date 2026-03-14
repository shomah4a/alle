.PHONY: build test lint fmt run

build:
	./gradlew :alle-app:shadowJar

test:
	./gradlew test

lint:
	./gradlew spotlessCheck

fmt:
	./gradlew spotlessApply

run: build
	java -jar alle-app/build/libs/alle-app-*-all.jar
