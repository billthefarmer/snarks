// Decompiled by Jad v1.5.8f. Copyright 2001 Pavel Kouznetsov.
// Jad home page: http://www.kpdus.com/jad.html
// Decompiler options: packimports(3) fieldsfirst space 
// Source File Name:   TiledLayer.java

// package javax.microedition.lcdui.game;

// import javax.microedition.lcdui.Graphics;
// import javax.microedition.lcdui.Image;

import java.awt.Graphics;
import java.awt.Image;

// Referenced classes of package javax.microedition.lcdui.game:
//	      Layer

public class TiledLayer extends Layer
{

    private int cellHeight;
    private int cellWidth;
    private int rows;
    private int columns;
    private int cellMatrix[][];
    Image sourceImage;
    private int numberOfTiles;
    int tileSetX[];
    int tileSetY[];
    private int anim_to_static[];
    private int numOfAnimTiles;

    public TiledLayer(int i, int j, Image image, int k, int l)
    {
	super(i >= 1 && k >= 1 ? i * k : -1, j >= 1 && l >= 1 ? j * l : -1);

	if (image.getWidth(null) % k != 0 || image.getHeight(null) % l != 0)
	{
	    throw new IllegalArgumentException();
	}

	else
	{
	    columns = i;
	    rows = j;
	    cellMatrix = new int[j][i];
	    int i1 = (image.getWidth(null) / k) * (image.getHeight(null) / l);
	    createStaticSet(image, i1 + 1, k, l, true);
	    return;
	}
    }

    public int createAnimatedTile(int i)
    {
	if (i < 0 || i >= numberOfTiles)
	    throw new IndexOutOfBoundsException();

	if (anim_to_static == null)
	{
	    anim_to_static = new int[4];
	    numOfAnimTiles = 1;
	}

	else
	    if (numOfAnimTiles == anim_to_static.length)
	    {
		int ai[] = new int[anim_to_static.length * 2];
		System.arraycopy(anim_to_static, 0, ai, 0,
				 anim_to_static.length);
		anim_to_static = ai;
	    }

	anim_to_static[numOfAnimTiles] = i;
	numOfAnimTiles++;
	return -(numOfAnimTiles - 1);
    }

    public void setAnimatedTile(int i, int j)
    {
	if (j < 0 || j >= numberOfTiles)
	    throw new IndexOutOfBoundsException();

	i = -i;
	if (anim_to_static == null || i <= 0 || i >= numOfAnimTiles)
	{
	    throw new IndexOutOfBoundsException();
	}

	else
	{
	    anim_to_static[i] = j;
	    return;
	}
    }

    public int getAnimatedTile(int i)
    {
	i = -i;
	if (anim_to_static == null || i <= 0 || i >= numOfAnimTiles)
	    throw new IndexOutOfBoundsException();

	else
	    return anim_to_static[i];
    }

    public void setCell(int i, int j, int k)
    {
	if (i < 0 || i >= columns || j < 0 || j >= rows)
	    throw new IndexOutOfBoundsException();

	if (k > 0)
	{
	    if (k >= numberOfTiles)
		throw new IndexOutOfBoundsException();
	}

	else
	    if (k < 0 && (anim_to_static == null || -k >= numOfAnimTiles))
		throw new IndexOutOfBoundsException();

	cellMatrix[j][i] = k;
    }

    public int getCell(int i, int j)
    {
	if (i < 0 || i >= columns || j < 0 || j >= rows)
	    throw new IndexOutOfBoundsException();

	else
	    return cellMatrix[j][i];
    }

    public void fillCells(int i, int j, int k, int l, int i1)
    {
	if (k < 0 || l < 0)
	    throw new IllegalArgumentException();

	if (i < 0 || i >= columns || j < 0 || j >= rows ||
	    i + k > columns || j + l > rows)
	    throw new IndexOutOfBoundsException();

	if (i1 > 0)
	{
	    if (i1 >= numberOfTiles)
		throw new IndexOutOfBoundsException();
	}

	else
	    if (i1 < 0 && (anim_to_static == null || -i1 >= numOfAnimTiles))
		throw new IndexOutOfBoundsException();

	for (int j1 = j; j1 < j + l; j1++)
	{
	    for (int k1 = i; k1 < i + k; k1++)
		cellMatrix[j1][k1] = i1;
	}
    }

    public final int getCellWidth()
    {
	return cellWidth;
    }

    public final int getCellHeight()
    {
	return cellHeight;
    }

    public final int getColumns()
    {
	return columns;
    }

    public final int getRows()
    {
	return rows;
    }

    public void setStaticTileSet(Image image, int i, int j)
    {
	if (i < 1 || j < 1 || image.getWidth(null) % i != 0 ||
	    image.getHeight(null) % j != 0)
	    throw new IllegalArgumentException();

	setWidthImpl(columns * i);
	setHeightImpl(rows * j);
	int k = (image.getWidth(null) / i) * (image.getHeight(null) / j);
	if (k >= numberOfTiles - 1)
	    createStaticSet(image, k + 1, i, j, true);

	else
	    createStaticSet(image, k + 1, i, j, false);
    }

    public final void paint(Graphics g)
    {
	if (g == null)
	    throw new NullPointerException();

	if (super.visible)
	{
	    boolean flag = false;
	    int j = super.y;
	    for (int k = 0; k < cellMatrix.length;)
	    {
		int l = super.x;
		int i1 = cellMatrix[k].length;
		for (int j1 = 0; j1 < i1;)
		{
		    int i = cellMatrix[k][j1];
		    if (i != 0)
		    {
			if (i < 0)
			    i = getAnimatedTile(i);

// 			g.drawRegion(sourceImage, tileSetX[i], tileSetY[i],
// 				     cellWidth, cellHeight, 0, l, j, 20);

			g.drawImage(sourceImage, l, j, l + cellWidth,
				    j + cellHeight, tileSetX[i], tileSetY[i],
				    tileSetX[i] + cellWidth,
				    tileSetY[i] + cellHeight, null);
		    }
		    j1++;
		    l += cellWidth;
		}

		k++;
		j += cellHeight;
	    }

	}
    }

    private void createStaticSet(Image image, int i, int j, int k, boolean flag)
    {
	cellWidth = j;
	cellHeight = k;
	int l = image.getWidth(null);
	int i1 = image.getHeight(null);
	sourceImage = image;
	numberOfTiles = i;
	tileSetX = new int[numberOfTiles];
	tileSetY = new int[numberOfTiles];
	if (!flag)
	{
	    for (rows = 0; rows < cellMatrix.length; rows++)
	    {
		int j1 = cellMatrix[rows].length;
		for (columns = 0; columns < j1; columns++)
		    cellMatrix[rows][columns] = 0;

	    }

	    anim_to_static = null;
	}
	int k1 = 1;
	for (int l1 = 0; l1 < i1; l1 += k)
	{
	    for (int i2 = 0; i2 < l; i2 += j)
	    {
		tileSetX[k1] = i2;
		tileSetY[k1] = l1;
		k1++;
	    }
	}
    }
}
