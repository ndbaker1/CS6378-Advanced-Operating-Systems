JFLAGS := -Xlint:unchecked
JC := javac
OUT := build
SRC := src

all: classes

start: classes
	./launcher.sh

stop:
	./cleanup.sh

eval:
	python3 src/metrics.py

classes: $(OUT)
	$(JC) $(JFLAGS) $(SRC)/* -d $(OUT)

clean:
	rm -rf $(OUT)

$(OUT):
	@mkdir -p $@
