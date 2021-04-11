// Decompiled by Jad v1.5.8f. Copyright 2001 Pavel Kouznetsov.
// Jad home page: http://www.kpdus.com/jad.html
// Decompiler options: packimports(3) fieldsfirst space 
// Source File Name:   LayerManager.java

// package javax.microedition.lcdui.game;

// import javax.microedition.lcdui.Graphics;

import java.awt.Graphics;
import java.awt.Shape;

// Referenced classes of package javax.microedition.lcdui.game:
//            Layer

public class LayerManager
{

    private int nlayers;
    private Layer component[];
    private int viewX;
    private int viewY;
    private int viewWidth;
    private int viewHeight;

    public LayerManager()
    {
        component = new Layer[4];
        setViewWindow(0, 0, 0x7fffffff, 0x7fffffff);
    }

    public void append(Layer l)
    {
        removeImpl(l);
        addImpl(l, nlayers);
    }

    public void insert(Layer l, int index)
    {
        if (index < 0 || index > nlayers)
        {
            throw new IndexOutOfBoundsException();
        } else
        {
            removeImpl(l);
            addImpl(l, index);
            return;
        }
    }

    public Layer getLayerAt(int index)
    {
        if (index < 0 || index >= nlayers)
            throw new IndexOutOfBoundsException();
        else
            return component[index];
    }

    public int getSize()
    {
        return nlayers;
    }

    public void remove(Layer l)
    {
        removeImpl(l);
    }

    public void paint(Graphics g, int x, int y)
    {
	Shape shape = g.getClip();

//         int clipX = g.getClipX();
//         int clipY = g.getClipY();
//         int clipW = g.getClipWidth();
//         int clipH = g.getClipHeight();

        g.translate(x - viewX, y - viewY);
        g.clipRect(viewX, viewY, viewWidth, viewHeight);

        int i = nlayers;
        while (true)
        {
            if (--i < 0)
                break;

            Layer comp = component[i];
            if (comp.visible)
                comp.paint(g);
        }

        g.translate(-x + viewX, -y + viewY);
	g.setClip(shape);

//         g.setClip(clipX, clipY, clipW, clipH);
    }

    public void setViewWindow(int x, int y, int width, int height)
    {
        if (width < 0 || height < 0)
        {
            throw new IllegalArgumentException();
        } else
        {
            viewX = x;
            viewY = y;
            viewWidth = width;
            viewHeight = height;
            return;
        }
    }

    private void addImpl(Layer layer, int index)
    {
        if (nlayers == component.length)
        {
            Layer newcomponents[] = new Layer[nlayers + 4];
            System.arraycopy(component, 0, newcomponents, 0, nlayers);
            System.arraycopy(component, index, newcomponents,
			     index + 1, nlayers - index);
            component = newcomponents;
        }

	else
        {
            System.arraycopy(component, index, component,
			     index + 1, nlayers - index);
        }

        component[index] = layer;
        nlayers++;
    }

    private void removeImpl(Layer l)
    {
        if (l == null)
            throw new NullPointerException();

        int i = nlayers;
        do
        {
            if (--i < 0)
                break;

            if (component[i] == l)
                remove(i);

        } while (true);
    }

    private void remove(int index)
    {
        System.arraycopy(component, index + 1, component,
			 index, nlayers - index - 1);
        component[--nlayers] = null;
    }
}
