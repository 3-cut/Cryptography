SRC_DIR   := src
OUT_DIR   := out
JAR_NAME  := crypto-demo.jar
MAIN_CLASS:= Main

SOURCES := $(shell find $(SRC_DIR) -name "*.java")

.PHONY: update exec install clean

# Recompile le projet dans out/
update:
	mkdir -p $(OUT_DIR)
	javac -d $(OUT_DIR) $(SOURCES)

# Exécute le projet (recompile d'abord pour être sûr d'être à jour)
exec: update
	java -cp $(OUT_DIR) $(MAIN_CLASS)

# Crée un exécutable .jar autonome (recompile d'abord)
install: update
	cp $(SRC_DIR)/model/primes_100000.txt $(OUT_DIR)/model/primes_100000.txt
	cd $(OUT_DIR) && jar cfe ../$(JAR_NAME) $(MAIN_CLASS) .
	@echo "Cree : $(JAR_NAME) (lancer avec : java -jar $(JAR_NAME))"

clean:
	rm -rf $(OUT_DIR) $(JAR_NAME)
