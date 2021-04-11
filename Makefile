# Snarks makefile
#

all:	Snarks SnarkApplet

Snarks:	Snarks.jar

Snarks.jar:	Snarks.class Snarks.mf
	jar -cvmf Snarks.mf Snarks.jar Snarks*.class\
		Layer.class LayerManager.class TiledLayer.class images

SnarkApplet:	SnarkApplet.jar SnarkApplet.zip

SnarkApplet.jar:	SnarkApplet.class SnarkApplet.mf
	jar -cvmf SnarkApplet.mf SnarkApplet.jar SnarkApplet*.class\
		Layer.class LayerManager.class TiledLayer.class images

SnarkApplet.zip:	SnarkApplet.jar SnarkApplet.html
	zip SnarkApplet.zip SnarkApplet.jar SnarkApplet*.*\
		Layer.class LayerManager.class TiledLayer.class images/*

%.class:	%.java
	javac $<

%.mf:		%.class
	echo Main-Class: $*>$@
