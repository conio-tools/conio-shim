.PHONY: build package clean demo

build:
	mvn compile test

package:
	echo "Building the project..."
	mvn package -DskipTests

clean:
	mvn clean

conio-nano:
	echo "Cloning conio-nano..."
	git clone https://github.com/conio-tools/conio-nano

submit: package conio-nano
	echo "Copying files to the cloned repository..."
	rm -rf conio-nano/conio/conio.jar
	cp client/target/client-1.0-SNAPSHOT-jar-with-dependencies.jar conio-nano/conio/conio.jar
	rm -rf conio-nano/conio/sleep_pod.yaml
	cp container/src/test/resources/sleep_pod.yaml conio-nano/conio/
	echo "Building the conio client container..."
	cd conio-nano && make conio
	echo "Submitting sleep yaml pod to Hadoop..."
	docker run -it -a stdin -a stdout -a stderr --env-file conio-nano/hadoop.env --network conio-nano_default -v $(PWD)/conio-nano/conio:/conio conio/base:master -- sudo -u conio java -jar /conio/conio.jar create -yaml /conio/sleep_pod.yaml -queue default -wait -zookeeper zookeeper:2181

demo: conio-nano
	echo "Starting dockerized Hadoop..."
	cd conio-nano && make run
	echo "Waiting for dockerized Hadoop to come alive..."
	sleep 30
	make submit

stop:
	@echo "Stopping dockerized Hadoop..."
	cd conio-nano && make stop