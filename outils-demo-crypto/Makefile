SRC_DIR   := src
OUT_DIR   := out
JAR_NAME  := crypto-demo.jar
MAIN_CLASS:= Main
WEB_MAIN  := web.WebServer
WEB_PORT  := 8080

SOURCES := $(shell find $(SRC_DIR) -name "*.java")

.PHONY: update exec install web clean

# Recompile le projet dans out/
update:
	mkdir -p $(OUT_DIR)
	javac -d $(OUT_DIR) $(SOURCES)

# Exécute la version bureau Swing (recompile d'abord pour être sûr d'être à jour)
exec: update
	java -cp $(OUT_DIR) $(MAIN_CLASS)

# Lance le serveur web (http://localhost:8080) -- ouvrir cette adresse dans un navigateur
web: update
	java -cp $(OUT_DIR) $(WEB_MAIN) $(WEB_PORT)

# Crée un exécutable .jar autonome (Swing + serveur web, recompile d'abord)
install: update
	cp $(SRC_DIR)/model/primes_100000.txt $(OUT_DIR)/model/primes_100000.txt
	mkdir -p $(OUT_DIR)/webapp
	cp -r webapp/* $(OUT_DIR)/webapp/
	cd $(OUT_DIR) && jar cfe ../$(JAR_NAME) $(MAIN_CLASS) .
	@echo "Cree : $(JAR_NAME)"
	@echo "  - version bureau : java -jar $(JAR_NAME)"
	@echo "  - version web    : java -cp $(JAR_NAME) $(WEB_MAIN) $(WEB_PORT)"

clean:
	rm -rf $(OUT_DIR) $(JAR_NAME)
