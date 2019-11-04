#make dev
.PHONY: dev
dev:
	./mvnw compile quarkus:dev

#make debug
.PHONY: debug
debug:
	./mvnw compile quarkus:dev -Ddebug -Dsuspend

#make clean
.PHONY: clean
clean:
	./mvnw clean

#make native
.PHONY: native
native:
	./mvnw package -Pnative -DskipTests

#make ext-add ID=kotlin
.PHONY: ext-add
ext-add:
	./mvnw quarkus:add-extension -Dextensions="$(ID)"

#make ext-list
.PHONY: ext-list
ext-list:
	./mvnw quarkus:list-extensions
