.PHONY: all scanner install uninstall compile doc test clean hscc2019

PREFIX=/usr/local

SCANNER=src/main/scala/falstar/parser/Scanner.java
SRC=$(shell find src/main/scala -iname "*.scala") $(SCANNER)
BIN=falstar.jar falstar falstar-session

ARCH2018=$(wildcard src/test/configuration/arch2018/*.cfg)
HSCC2019=$(wildcard src/test/configuration/hscc2019/*.cfg)

all: compile

compile: falstar.jar
doc: README.html
scanner: $(SCANNER)

test: falstar.jar
	./falstar src/test/configuration/test.cfg

install: $(BIN)
	install -m 755 falstar falstar-session $(PREFIX)
	install -m 644 falstar.jar $(PREFIX)

uninstall:
	rm -f $(addprefix $(PREFIX)/,$(BIN))

arch2018: $(ARCH2018:src/test/configuration/arch2018/%.cfg=results/arch2018/%.csv)
hscc2019: $(HSCC2019:src/test/configuration/hscc2019/%.cfg=results/hscc2019/%.csv)

falstar.jar: bin $(SRC)
	scalac -d bin -cp lib/engine.jar $(SRC)
	javac  -d bin -cp bin $(SCANNER)
	jar cf $@ -C bin .

%.html: %.md
	pandoc -s $^ -o $@

%.java: %.flex
	./jflex -nobak $^

bin:
	mkdir -p bin

results/arch2018/%.csv: src/test/configuration/arch2018/%.cfg falstar.jar
	./falstar $<

results/hscc2019/%.csv: src/test/configuration/hscc2019/%.cfg falstar.jar
	./falstar $<

clean:
	rm -fr bin
	rm -fr slprj
	rm -f outcmaes*.dat
	rm -f variablescmaes*.mat
	rm -f falstar.jar
	rm -f *.slxc *.mexa64
