.PHONY: build package clean

build:
	mvn compile test

package:
	mvn package

clean:
	mvn clean
