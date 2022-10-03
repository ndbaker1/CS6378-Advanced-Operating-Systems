JFLAGS := -Xlint:unchecked
JC := javac
OUT := build
SRC := src

all: classes

start: classes
	./launcher.sh

stop:
	./cleanup.sh

classes: $(OUT)
	$(JC) $(JFLAGS) $(SRC)/* -d $(OUT)

clean:
	rm -rf $(OUT)

$(OUT):
	@mkdir -p $@
