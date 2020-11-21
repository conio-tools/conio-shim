compile:
	mvn package -DskipTests

clean:
	mvn clean

run: compile target/
	java -jar target/conio-1.0-SNAPSHOT-jar-with-dependencies.jar