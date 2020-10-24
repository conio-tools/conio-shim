clean:
	mvn clean

compile:
	mvn package -DskipTests

run: compile target/
	java -jar target/conio-1.0-SNAPSHOT-jar-with-dependencies.jar