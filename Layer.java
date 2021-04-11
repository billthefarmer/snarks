// Decompiled by Jad v1.5.8f. Copyright 2001 Pavel Kouznetsov.
// Jad home page: http://www.kpdus.com/jad.html
// Decompiler options: packimports(3) fieldsfirst space 
// Source File Name:   Layer.java

// package javax.microedition.lcdui.game;

// import javax.microedition.lcdui.Graphics;

import java.awt.Graphics;

public abstract class Layer
{

    int x;
    int y;
    int width;
    int height;
    boolean visible;

    Layer(int width, int height)
    {
        visible = true;
        setWidthImpl(width);
        setHeightImpl(height);
    }

    public void setPosition(int x, int y)
    {
        this.x = x;
        this.y = y;
    }

    public void move(int dx, int dy)
    {
        x += dx;
        y += dy;
    }

    public final int getX()
    {
        return x;
    }

    public final int getY()
    {
        return y;
    }

    public final int getWidth()
    {
        return width;
    }

    public final int getHeight()
    {
        return height;
    }

    public void setVisible(boolean visible)
    {
        this.visible = visible;
    }

    public final boolean isVisible()
    {
        return visible;
    }

    public abstract void paint(Graphics g);

    void setWidthImpl(int width)
    {
        if (width < 0)
        {
            throw new IllegalArgumentException();
        } else
        {
            this.width = width;
            return;
        }
    }

    void setHeightImpl(int height)
    {
        if (height < 0)
        {
            throw new IllegalArgumentException();
        } else
        {
            this.height = height;
            return;
        }
    }
}
