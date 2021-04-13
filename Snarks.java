///////////////////////////////////////////////////////////////////////////////
//
//   Snarks.java  1.0  30 April 2006
//
//   Snarks - Recreate Snarks and Dragon game
//
//   Copyright (C) 2006 Bill Farmer
//
///////////////////////////////////////////////////////////////////////////////

import java.io.*;
import java.awt.*;
import javax.imageio.*;
import java.util.Random;
import java.awt.event.*;
import java.awt.image.BufferStrategy;

///////////////////////////////////////////////////////////////////////////////

public class Snarks
{
    Random random;
    SnarkFrame frame;

    int score, highScore;

    public Snarks(GraphicsDevice device)
    {
	random = new Random();

	GraphicsConfiguration gc = device.getDefaultConfiguration();

	frame = new SnarkFrame(gc);
	frame.setUndecorated(true);
	frame.setIgnoreRepaint(true);

	device.setFullScreenWindow(frame);
	if (device.isDisplayChangeSupported())
        {
            for (DisplayMode mode: device.getDisplayModes())
                if (mode.getWidth() == 720 &&
                    mode.getHeight() == 480 &&
                    mode.getRefreshRate() == 60)
                    device.setDisplayMode(mode);
        }
    }

    public void start()
    {
	frame.setup();
	frame.start();
    }

    public static void main(String[] args)
    {
        GraphicsEnvironment env =
	    GraphicsEnvironment.getLocalGraphicsEnvironment();

        GraphicsDevice device = env.getDefaultScreenDevice();

	Snarks snarks = new Snarks(device);
	snarks.start();
    }

///////////////////////////////////////////////////////////////////////////////

    static final int SNARKS = 256;
    static final int LASERS = 4;
    static final int ROCKS  = 128;
    static final int TREES  = 16;
    static final int EGGS   = 48;

    static final int HATCH = 32;
    static final int CULL  = 64;
    static final int LAY   = 2;

    static final int EMPTY  = 0;
    static final int ROCK   = 1;
    static final int TREE   = 2;
    static final int EGG    = 3;
    static final int DRAGON = 4;
    static final int SNARK  = 5;
    static final int PLAYER = 6;
    static final int LASER  = 7;

    static final int SPLAT = 9;

    static final int DEAD = -1;

    static final int N  = 1;
    static final int NE = 2;
    static final int E  = 3;
    static final int SE = 4;
    static final int S  = 5;
    static final int SW = 6;
    static final int W  = 7;
    static final int NW = 8;

    static final int FIRE = 9;

    static final int N_PRESSED  = 1 << N;
    static final int NE_PRESSED = 1 << NE;
    static final int E_PRESSED  = 1 << E;
    static final int SE_PRESSED = 1 << SE;
    static final int S_PRESSED  = 1 << S;
    static final int SW_PRESSED = 1 << SW;
    static final int W_PRESSED  = 1 << W;
    static final int NW_PRESSED = 1 << NW;

    static final int FIRE_PRESSED = 1 << FIRE;

    static final int EGG_POINTS    = 10;
    static final int SNARK_POINTS  = 50;
    static final int DRAGON_POINTS = 1000;

    static final int EGG_TILE    = 9;
    static final int DRAGON_TILE = 9;
    static final int ANIMATED_TILE = -1;

    static final int[] MOVE_X = {0, 0, 1, 1, 1, 0, -1, -1, -1};
    static final int[] MOVE_Y = {0, -1, -1, 0, 1, 1, 1, 0, -1};

///////////////////////////////////////////////////////////////////////////////

    class SnarkFrame extends Frame
	implements Runnable, KeyListener
    {
	int[][] cells;
	Snark[] snarks;
	Laser[] lasers;
	Player player;
	Dragon dragon;
	Thread thread;
	boolean auto;
	long time;

	int columns, rows;
	int width, height;
	int tileSize;
	Color bgColour, fgColour;

	LayerManager lm;

	TiledLayer backLayer;
	TiledLayer laserLayer;
	TiledLayer snarkLayer;
	TiledLayer playerLayer;

	BufferStrategy bufferStrategy;

	public SnarkFrame(GraphicsConfiguration gc)
	{
	    super(gc);
	    addKeyListener(this);
	}

	public void start()
	{
	    if ((thread == null) || (!thread.isAlive()))
	    {
		thread = new Thread(this);
		thread.start();
	    }
	}

	public void stop()
	{
	    Thread t = thread;
	    thread = null;

            try
            {
                if (t != null)
                    t.join();
            }

            catch(Exception e) {}
	}

	public void exit()
	{
	    stop();
	    drawBox("Exit Snarks");
	    System.exit(0);
	}

	public void setup()
	{
	    createBufferStrategy(2);
	    bufferStrategy = getBufferStrategy();

	    Toolkit toolkit = Toolkit.getDefaultToolkit();
	    Image image = toolkit.getImage("none.png");
	    Cursor none =
		toolkit.createCustomCursor(image, new Point(0, 0), "none");

	    setCursor(none);

	    fgColour = Color.green;
	    bgColour = Color.black;
	}

	public void run()
	{
	    setMode();
	    setLayers();
	    play();
	}

	public void setMode()
	{
	    width = getWidth();
	    height = getHeight();
	    tileSize = 16;

	    columns = ((width * 6) / 8) / tileSize;
	    rows = ((height * 6) / 8) / tileSize;
	}

	public void setLayers()
	{
	    cells = new int[columns][rows];

	    snarks = new Snark[SNARKS];
	    lasers = new Laser[LASERS];

	    try
	    {
		File back = new File("images/back.png");
		File laser = new File("images/laser.png");
		File snark = new File("images/snark.png");
		File player = new File("images/player.png");

		Image backImage = ImageIO.read(back);
		Image laserImage = ImageIO.read(laser);
		Image snarkImage = ImageIO.read(snark);
		Image playerImage = ImageIO.read(player);

		playerLayer = new TiledLayer(columns, rows, playerImage,
					     tileSize, tileSize);

		laserLayer = new TiledLayer(columns, rows, laserImage,
					     tileSize, tileSize);

		snarkLayer = new TiledLayer(columns, rows, snarkImage,
					    tileSize, tileSize);

		backLayer = new TiledLayer(columns, rows, backImage,
					   tileSize, tileSize);
	    }

	    catch (Exception e) {}

	    lm = new LayerManager();

	    lm.append(playerLayer);
	    lm.append(laserLayer);
	    lm.append(snarkLayer);
	    lm.append(backLayer);
	}

	public void clear()
	{
	    for (int x = 0; x < columns; x++)
		for (int y = 0; y < rows; y++)
		    cells[x][y] = EMPTY;

	    backLayer.fillCells(0, 0, columns, rows, EMPTY);
	    snarkLayer.fillCells(0, 0, columns, rows, EMPTY);
	    laserLayer.fillCells(0, 0, columns, rows, EMPTY);
	    playerLayer.fillCells(0, 0, columns, rows, EMPTY);

	    for (int i = 0; i < SNARKS; i++)
		snarks[i] = null;

	    dragon = null;

	    for (int i = 0; i < LASERS; i++)
		lasers[i] = null;
	}

	public void setBackground()
	{
	    for (int i = 0; i < ROCKS; i++)
	    {
		while (true)
		{
		    int x = random.nextInt(columns);
		    int y = random.nextInt(rows);

		    if (cells[x][y] == EMPTY)
		    {
			cells[x][y] = ROCK;
			backLayer.setCell(x, y, ROCK);
			break;
		    }
		}
	    }

	    backLayer.createAnimatedTile(TREE);

	    for (int i = 0; i < TREES; i++)
	    {
		while (true)
		{
		    int x = random.nextInt(columns);
		    int y = random.nextInt(rows);

		    if (cells[x][y] == EMPTY)
		    {
			cells[x][y] = TREE;
			backLayer.setCell(x, y, ANIMATED_TILE);
			break;
		    }
		}
	    }
	}

	public void newPlayer()
	{
	    while (true)
	    {
		int x = random.nextInt(columns);
		int y = random.nextInt(rows);

		if (cells[x][y] == EMPTY)
		{
		    cells[x][y] = PLAYER;
		    playerLayer.setCell(x, y, N);
		    player = new Player(this, x, y, N);
		    break;
		}
	    }
	}

	public void moreEggs()
	{
	    for (int i = 0; i < EGGS; i++)
	    {
		while (true)
		{
		    int x = random.nextInt(columns);
		    int y = random.nextInt(rows);

		    if (cells[x][y] == EMPTY)
		    {
			cells[x][y] = EGG;
			snarkLayer.setCell(x, y, EGG_TILE);
			snarks[i] = new Snark(this, x, y, EGG_TILE);
			break;
		    }
		}
	    }
	}

	public void drawInfo()
	{
	    Graphics g = bufferStrategy.getDrawGraphics();
	    Font f = g.getFont();
	    g.setFont(f.deriveFont(Font.BOLD, f.getSize() * 1.4f));
	    FontMetrics fm = g.getFontMetrics();

	    g.setColor(bgColour);
	    g.fillRect(0, 0, width, height);

	    g.setColor(fgColour);

	    String s = "Snarks and Dragon";
	    int w = fm.stringWidth(s);
	    int mw = fm.stringWidth("m");
	    int ah = fm.getAscent();
	    int x = (width / 2) - (w / 2);
	    int y = ((((height / tileSize) - 20) / 2) * tileSize) + ah;

	    g.drawRect(x - (mw * 2), y - (2 * tileSize),
		       w + (mw * 4), 22 * tileSize);
	    g.drawRect(x - (mw * 2) - 2, y - (2 * tileSize) - 2,
		       w + (mw * 4) + 4, (22 * tileSize) + 4);
	    g.drawString(s, x, y);

	    g.setFont(f.deriveFont(f.getSize() * 1.4f));
	    y += tileSize * 2;
	    s = "Dragon";
	    w = fm.stringWidth(s) + mw;
	    g.drawString("Snarks", (width / 2) - w, y);
	    y += tileSize * 2;
	    g.drawString("Dragon", (width / 2) - w, y);
	    y += tileSize * 2;
	    g.drawString("Eggs", (width / 2) - w, y);
	    y += tileSize * 2;
	    g.drawString("Rocks", (width / 2) - w, y);
	    y += tileSize * 2;
	    g.drawString("Trees", (width / 2) - w, y);
	    y += tileSize * 2;
	    g.drawString("Player", (width / 2) - w, y);
	    y += tileSize * 2;
	    g.drawString("Score:", (width / 2) - w, y);
	    g.drawString(score + "", width / 2, y);
	    y += tileSize * 2;
	    g.drawString("High:", (width / 2) - w, y);
	    g.drawString(highScore + "", width / 2, y);
	    s = "Press a key to start";
	    w = fm.stringWidth(s);
	    x = (width / 2) - (w / 2);
	    y += tileSize * 2;
	    g.drawString(s, x, y);

	    g.dispose();
	}

	public void setupTiles()
	{
	    Graphics g = bufferStrategy.getDrawGraphics();
	    FontMetrics fm = g.getFontMetrics();
	    int h = fm.getHeight();

	    g.setColor(fgColour);

	    int x = (columns + 1) / 2;
	    int y = (((height / tileSize) - 20) / 2) + 2;

	    snarkLayer.createAnimatedTile(N);
	    snarkLayer.setCell(x, y, ANIMATED_TILE); 
	    y += 2;
	    playerLayer.setCell(x, y, DRAGON_TILE);
	    y += 2;
	    snarkLayer.setCell(x, y, EGG_TILE);
	    y += 2;
	    backLayer.setCell(x, y, ROCK);
	    y += 2;
	    backLayer.createAnimatedTile(TREE);
	    backLayer.setCell(x, y, ANIMATED_TILE);
	    y += 2;
	    playerLayer.createAnimatedTile(N);
	    playerLayer.setCell(x, y, ANIMATED_TILE);

	    int xOffset = (width - (columns * tileSize)) / 2;
	    int yOffset = h - ((h / tileSize) * tileSize);

	    lm.paint(g, xOffset, yOffset);
	    g.dispose();
	}

	public void drawBox(String s)
	{
	    Graphics g = bufferStrategy.getDrawGraphics();
	    Font f = g.getFont();
	    g.setFont(f.deriveFont(Font.BOLD, f.getSize() * 1.4f));
	    FontMetrics fm = g.getFontMetrics();

	    int w = fm.stringWidth(s);
	    int h = fm.getHeight();
	    int ah = fm.getAscent();
	    int rw = w + (fm.charWidth('m') * 2);
	    int rh = h * 2;

	    g.setColor(bgColour);
	    g.fillRect((width / 2) - (rw / 2) - 2, (height / 2) - h - 2,
		       rw + 4, rh + 4);

	    g.setColor(fgColour);
	    g.drawRect((width / 2) - (rw / 2), (height / 2) - h, rw, rh);
	    g.drawRect((width / 2) - (rw / 2) - 2, (height / 2) - h - 2,
		       rw + 4, rh + 4);

	    g.drawString(s, (width / 2) - (w / 2), (height / 2) + (ah / 2));
	    bufferStrategy.show();

	    g.setColor(bgColour);
	    g.fillRect((width / 2) - (rw / 2) - 2, (height / 2) - h - 2,
		       rw + 4, rh + 4);

	    g.setColor(fgColour);
	    g.drawRect((width / 2) - (rw / 2), (height / 2) - h, rw, rh);
	    g.drawRect((width / 2) - (rw / 2) - 2, (height / 2) - h - 2,
		       rw + 4, rh + 4);

	    g.drawString(s, (width / 2) - (w / 2), (height / 2) + (ah / 2));
	    bufferStrategy.show();
	    g.dispose();

	    delay(0);
	    delay(1000);
	}

	public void playDemo()
	{
	    demo:
	    while (thread != null)
	    {
		clear();
		setupTiles();

		for (int i = 0; i != 50; i++)
		{
		    drawInfo();
		    animateTiles((i % 8) + N);
		    drawInfo();
		    animateTiles((i % 8) + N);
		    delay(250);

		    if ((getKeyStates() != 0) || (thread == null) || (auto))
			break demo;
		}

		fade();
		clear();
		setBackground();
		score = 0;
		moreEggs();
		newPlayer();
		newDragon();
		getKeyStates();

		for (int i = 0; i != 250; i++)
		{
		    if (player != null)
			player.move();

		    else
		    {
			tidy();
			drawBox("New player");
			newPlayer();
		    }

		    playSnarks();
		    backLayer.setAnimatedTile(ANIMATED_TILE, (i % 4) + TREE);

		    if ((getKeyStates() != 0) || (thread == null))
			break demo;
		}

		fade();
	    }
	}

	public void animateTiles(int n)
	{
	    Graphics g = bufferStrategy.getDrawGraphics();
	    FontMetrics fm = g.getFontMetrics();
	    int h = fm.getHeight();

	    int xOffset = (width - (columns * tileSize)) / 2;
	    int yOffset = h - ((h / tileSize) * tileSize);

	    snarkLayer.setAnimatedTile(ANIMATED_TILE, n);
	    playerLayer.setAnimatedTile(ANIMATED_TILE, n);
	    backLayer.setAnimatedTile(ANIMATED_TILE, ((n - 1) % 4) + TREE);

	    lm.paint(g, xOffset, yOffset);
	    bufferStrategy.show();
	    g.dispose();
	}

	public void tidy()
	{
	    for (int x = 0; x < columns; x++)
		for (int y = 0; y < rows; y++)
		    if (laserLayer.getCell(x, y) == SPLAT)
			laserLayer.setCell(x, y, EMPTY);
	}

	public void play()
	{
	    delay(0);

	    while (thread != null)
	    {
		playDemo();

		if (thread == null)
		    break;

		fade();
		clear();
		setBackground();
		newPlayer();
		getKeyStates();

		while ((getKeyStates() == 0) && (thread != null) && (!auto))
		    flashPlayer();

		if (thread == null)
		    break;

		score = 0;
		int n = 0;
		moreEggs();
		getKeyStates();
		while (thread != null)
		{
		    if (player != null)
			movePlayer();

		    else
			break;

		    playSnarks();
		    backLayer.setAnimatedTile(ANIMATED_TILE, (n++ % 4) + TREE);
		}

		if (highScore < score)
		    highScore = score;

		if (thread == null)
		    break;

		delay(1000);
		drawBox("Game over");
		fade();
		getKeyStates();
	    }
	}

	public void fade()
	{
	    Graphics g = bufferStrategy.getDrawGraphics();
	    int d = Math.min(height, width) / 2;

	    for (int i = 0; i < d + 1; i += 2)
	    {
		g.setColor(fgColour);
		g.drawRect(i, i, width - (i * 2), height - (i * 2));
		g.setColor(bgColour);
		g.drawRect(i + 1, i + 1,
			   width - (i * 2) - 2, height - (i * 2) - 2);
		bufferStrategy.show();
		g.setColor(fgColour);
		g.drawRect(i, i, width - (i * 2), height - (i * 2));
		g.setColor(bgColour);
		g.drawRect(i + 1, i + 1,
			   width - (i * 2) - 2, height - (i * 2) - 2);
		bufferStrategy.show();

		if (thread == null)
		    break;
	    }

	    g.dispose();
	}

	public void playSnarks()
	{
	    Graphics g = bufferStrategy.getDrawGraphics();
	    Font f = g.getFont();
	    g.setFont(f.deriveFont(Font.BOLD, f.getSize() * 1.4f));
	    FontMetrics fm = g.getFontMetrics();

	    int xOffset = (width - (columns * tileSize)) / 2;
	    int yOffset = (height - (rows * tileSize)) / 2;

	    int count = 0;

	    for (int i = 0; i != SNARKS; i++)
	    {
		if (snarks[i] != null)
		{
		    snarks[i].move();
		    count++;
		}
	    }

	    if (count == 0)
	    {
		moreEggs();
		drawBox("More eggs");
	    }

	    if ((dragon == null) && (count > CULL))
	    {
		newDragon();
		drawBox("Beware dragon");
	    }

	    if (dragon != null)
		dragon.move();

	    for (int t = 0; t < 6; t++)
	    {
		for (int i = 0; i != LASERS; i++)
		    if (lasers[i] != null)
			lasers[i].move();

		g.setColor(bgColour);
		g.fillRect(0, 0, width, height);

		g.setColor(fgColour);
		String s = "Score: ";
		int w = fm.stringWidth(s);
		int h = fm.getHeight();
		int ah = fm.getAscent();
		int rw = (w + fm.charWidth('m')) * 2;
		int rh = h * 2;

		g.drawRect((width / 2) - (rw / 2), (yOffset / 2) - h, rw, rh);
		g.drawRect((width / 2) - (rw / 2) - 2, (yOffset / 2) - h - 2,
			   rw + 4, rh + 4);
		g.drawString(s + score, (width / 2) - w,
			     (yOffset / 2) + (ah / 2));

		g.drawRect(xOffset - 3, yOffset - 3,
			   (columns * tileSize) + 5, (rows * tileSize) + 5);
		g.drawRect(xOffset - 5, yOffset - 5,
			   (columns * tileSize) + 9, (rows * tileSize) + 9);

		lm.paint(g, xOffset, yOffset);
		bufferStrategy.show();
		delay(50);
	    }

	    g.dispose();
	}

	public void flashPlayer()
	{
	    Graphics g = bufferStrategy.getDrawGraphics();
	    Font f = g.getFont();
	    g.setFont(f.deriveFont(Font.BOLD, f.getSize() * 1.4f));
	    FontMetrics fm = g.getFontMetrics();

	    int xOffset = (width - (columns * tileSize)) / 2;
	    int yOffset = (height - (rows * tileSize)) / 2;

	    g.setColor(bgColour);
	    g.fillRect(0, 0, width, height);

	    g.setColor(fgColour);
	    String s = "Score: ";
	    int w = fm.stringWidth(s);
	    int h = fm.getHeight();
	    int ah = fm.getAscent();
	    int rw = (w + fm.charWidth('m')) * 2;
	    int rh = h * 2;

	    g.drawRect((width / 2) - (rw / 2), (yOffset / 2) - h, rw, rh);
	    g.drawRect((width / 2) - (rw / 2) - 2, (yOffset / 2) - h - 2,
		       rw + 4, rh + 4);
	    g.drawString(s + score, (width / 2) - w, (yOffset / 2) + (ah / 2));

	    g.drawRect(xOffset - 3, yOffset - 3,
		       (columns * tileSize) + 5, (rows * tileSize) + 5);
	    g.drawRect(xOffset - 5, yOffset - 5,
		       (columns * tileSize) + 9, (rows * tileSize) + 9);

	    playerLayer.setCell(player.x, player.y, EMPTY);

	    lm.paint(g, xOffset, yOffset);
	    bufferStrategy.show();
	    delay(500);

	    g.drawRect((width / 2) - (rw / 2), (yOffset / 2) - h, rw, rh);
	    g.drawRect((width / 2) - (rw / 2) - 2, (yOffset / 2) - h - 2,
		       rw + 4, rh + 4);
	    g.drawString(s + score, (width / 2) - w, (yOffset / 2) + (ah / 2));

	    g.drawRect(xOffset - 3, yOffset - 3,
		       (columns * tileSize) + 5, (rows * tileSize) + 5);
	    g.drawRect(xOffset - 5, yOffset - 5,
		       (columns * tileSize) + 9, (rows * tileSize) + 9);

	    playerLayer.setCell(player.x, player.y, player.d);

	    lm.paint(g, xOffset, yOffset);
	    bufferStrategy.show();
	    g.dispose();
	    delay(500);
	}

	public void newDragon()
	{
	    while (true)
	    {
		int x = random.nextInt(columns);
		int y = random.nextInt(rows);

		if (cells[x][y] == EMPTY)
		{
		    cells[x][y] = DRAGON;
		    playerLayer.setCell(x, y, DRAGON_TILE);
		    int d = random.nextInt(8) + 1;
		    dragon = new Dragon(this, x, y, d);
		    break;
		}
	    }
	}

	public void movePlayer()
	{
	    int keyState = getKeyStates();

	    if (auto)
	    {
		player.move();
		return;
	    }

	    if ((keyState & N_PRESSED) == N_PRESSED)
		player.move(N);

	    if ((keyState & NE_PRESSED) == NE_PRESSED)
		player.move(NE);

	    if ((keyState & E_PRESSED) == E_PRESSED)
		player.move(E);

	    if ((keyState & SE_PRESSED) == SE_PRESSED)
		player.move(SE);

	    if ((keyState & S_PRESSED) == S_PRESSED)
		player.move(S);

	    if ((keyState & SW_PRESSED) == SW_PRESSED)
		player.move(SW);

	    if ((keyState & W_PRESSED) == W_PRESSED)
		player.move(W);

	    if ((keyState & NW_PRESSED) == NW_PRESSED)
		player.move(NW);

	    if ((keyState & FIRE_PRESSED) == FIRE_PRESSED)
		player.fire();
	}

///////////////////////////////////////////////////////////////////////////////

	int keysPressed, keysReleased;

///////////////////////////////////////////////////////////////////////////////

	public int getKeyStates()
	{
	    int keyState = keysPressed;
	    keysPressed &= ~keysReleased;
	    keysReleased = 0;

	    return keyState;
	}

	public void keyTyped(KeyEvent e)
	{
	    char c = e.getKeyChar();

	    switch (c)
	    {
	    case '`':
		stop();
		auto = !auto;
		drawBox("Auto mode " + ((auto)? "on": "off"));
		start();
		break;

	    case 27:
		exit();
	    }
	}

	public void keyPressed(KeyEvent e)
	{
	    int c = e.getKeyCode();
	    keysPressed = 1 << getGameAction(c);
	}

	public void keyReleased(KeyEvent e)
	{
	    int c = e.getKeyCode();
	    keysReleased = 1 << getGameAction(c);
	}

	public int getGameAction(int keyCode)
	{
	    switch (keyCode)
	    {
	    case KeyEvent.VK_8:
	    case KeyEvent.VK_UP:
	    case KeyEvent.VK_NUMPAD8:
		return N;

	    case KeyEvent.VK_9:
	    case KeyEvent.VK_PAGE_UP:
	    case KeyEvent.VK_NUMPAD9:
		return NE;

	    case KeyEvent.VK_6:
	    case KeyEvent.VK_RIGHT:
	    case KeyEvent.VK_NUMPAD6:
		return E;

	    case KeyEvent.VK_3:
	    case KeyEvent.VK_PAGE_DOWN:
	    case KeyEvent.VK_NUMPAD3:
		return SE;

	    case KeyEvent.VK_2:
	    case KeyEvent.VK_DOWN:
	    case KeyEvent.VK_NUMPAD2:
		return S;

	    case KeyEvent.VK_1:
	    case KeyEvent.VK_END:
	    case KeyEvent.VK_NUMPAD1:
		return SW;

	    case KeyEvent.VK_4:
	    case KeyEvent.VK_LEFT:
	    case KeyEvent.VK_NUMPAD4:
		return W;

	    case KeyEvent.VK_7:
	    case KeyEvent.VK_HOME:
	    case KeyEvent.VK_NUMPAD7:
		return NW;

	    case KeyEvent.VK_5:
	    case KeyEvent.VK_CLEAR:
	    case KeyEvent.VK_ENTER:
	    case KeyEvent.VK_INSERT:
	    case KeyEvent.VK_NUMPAD5:
		return FIRE;
	    }

	    return 0;
	}

	public void delay(int d)
	{
	    long dt = d - (System.currentTimeMillis() - time);

	    if (dt > 0)
	    {
		try
		{
		    Thread.sleep(dt);
		}

		catch (Exception e) {}
	    }

	    time = System.currentTimeMillis();
	}
    }

///////////////////////////////////////////////////////////////////////////////

    abstract class Automaton
    {
	SnarkFrame frame;
	TiledLayer layer;
	int[][] cells;
	int x, y;
	int d;

	public int move(int d)
	{
	    return moveCell(MOVE_X[d], MOVE_Y[d]);
	}

	public int moveCell(int dx, int dy)
	{
	    int columns = frame.columns;
	    int rows = frame.rows;

	    int newX = (x + dx + columns) % columns;
	    int newY = (y + dy + rows) % rows;

	    int newCell = cells[newX][newY];

	    if (newCell == EMPTY)
	    {
		int oldCell = cells[x][y];
		int oldD = layer.getCell(x, y);

		cells[x][y] = EMPTY;
		layer.setCell(x, y, EMPTY);

		cells[newX][newY] = oldCell;
		layer.setCell(newX, newY, oldD);

		x = newX;
		y = newY;
	    }

	    return newCell;
	}
    }

///////////////////////////////////////////////////////////////////////////////

    class Snark extends Automaton
    {
	Snark[] snarks;

	public Snark(SnarkFrame frame, int x, int y, int d)
	{
	    this.frame = frame;
	    this.layer = frame.snarkLayer;
	    this.cells = frame.cells;
	    this.snarks = frame.snarks;
	    this.x = x;
	    this.y = y;
	    this.d = d;
	}

	public int move()
	{
	    switch (d)
	    {
	    case EGG_TILE:
		if (random.nextInt(HATCH) == 0)
		{
		    d = random.nextInt(8) + 1;
		    cells[x][y] = SNARK;
		    layer.setCell(x, y, d);
		}
		return EMPTY;

	    default:
		int newCell = super.move(d);

		switch (newCell)
		{
		case DRAGON:
		case LASER:
		case ROCK:
		case TREE:
		case EGG:
		    d = random.nextInt(8) + 1;
		    newCell = super.move(d);
		    layer.setCell(x, y, d);
		    return EMPTY;

		case SNARK:
		    int oldX = x;
		    int oldY = y;
		    d = random.nextInt(8) + 1;
		    newCell = super.move(d);
		    layer.setCell(x, y, d);

		    if (random.nextInt(LAY) == 0)
			layEgg(oldX, oldY);

		    return EMPTY;

		case PLAYER:
		    int newX = frame.player.x;
		    int newY = frame.player.y;

		    cells[newX][newY] = EMPTY;
		    frame.playerLayer.setCell(newX, newY, EMPTY);
		    frame.laserLayer.setCell(newX, newY, SPLAT);
		    frame.player = null;
		    return EMPTY;

		default:
		    return newCell;
		}
	    }
	}

	public int layEgg(int x, int y)
	{
	    if (cells[x][y] == EMPTY)
	    {
		for (int i = 0; i < SNARKS; i++)
		{
		    if (snarks[i] == null)
		    {
			cells[x][y] = EGG;
			layer.setCell(x, y, EGG_TILE);
			snarks[i] = new Snark(frame, x, y, EGG_TILE);
			break;
		    }
		}

		return EMPTY;
	    }

	    return cells[x][y];
	}
    }

///////////////////////////////////////////////////////////////////////////////

    abstract class Aggressor extends Automaton
    {
	Laser[] lasers;
	int[] prey;

	public int move()
	{
	    search();

	    boolean shooting = false;

	    for (int i = 0; i < LASERS; i++)
	    {
		if ((lasers[i] != null) &&
		    (lasers[i].owner == this))
		{
		    shooting = true;
		    break;
		}
	    }

	    if (!shooting)
	    {
		int newCell = super.move(d);

		switch (newCell)
		{
		case EGG:
		case ROCK:
		case TREE:
		case SNARK:
		    d = random.nextInt(8) + 1;
		    newCell = super.move(d);
		    break;
		}
	    }

	    return EMPTY;
	}

	public int search()
	{
	    int columns = frame.columns;
	    int rows = frame.rows;

	    for (int i = -1; i < 7; i++)
	    {
		int dir = ((d + i) % 8) + 1;
		int newX = x;
		int newY = y;

		while (true)
		{
		    newX = (newX + MOVE_X[dir] + columns) % columns;
		    newY = (newY + MOVE_Y[dir] + rows) % rows;

		    int newCell = cells[newX][newY];

		    if (newCell != EMPTY)
		    {
			for (int j = 0; j < prey.length; j++)
			    if (newCell == prey[j])
				return fire(dir);

			break;
		    }
		}
	    }

	    return EMPTY;
	}

	public int fire(int dir)
	{
	    for (int i = 0; i < LASERS; i++)
	    {
		if (lasers[i] == null)
		{
		    lasers[i] = new Laser(frame, x, y, dir, this);
		    break;
		}
	    }

	    return EMPTY;
	}
    }

///////////////////////////////////////////////////////////////////////////////

    class Dragon extends Aggressor
    {
	public Dragon(SnarkFrame frame, int x, int y, int d)
	{
	    this.frame = frame;
	    this.layer = frame.playerLayer;
	    this.lasers = frame.lasers;
	    this.cells = frame.cells;
	    this.x = x;
	    this.y = y;
	    this.d = d;

	    int[] prey = {EGG, SNARK, PLAYER};
	    this.prey = prey;
	}
    }

///////////////////////////////////////////////////////////////////////////////

    class Player extends Aggressor
    {
	public Player(SnarkFrame frame, int x, int y, int d)
	{
	    this.frame = frame;
	    this.layer = frame.playerLayer;
	    this.lasers = frame.lasers;
	    this.cells = frame.cells;
	    this.x = x;
	    this.y = y;
	    this.d = d;

	    int[] prey = {EGG, SNARK, DRAGON};
	    this.prey = prey;
	}

	public int move(int dir)
	{
	    if (d == dir)
		return super.move(d);

	    else
		return turn(dir);
	}

	public int turn(int dir)
	{
	    d = dir;
	    layer.setCell(x, y, d);
	    return EMPTY;
	}

	public int fire()
	{
	    return fire(d);
	}
    }

///////////////////////////////////////////////////////////////////////////////

    class Laser extends Automaton
    {
	Snark[] snarks;
	Laser[] lasers;
	Aggressor owner;
	boolean forward;
	int splatX, splatY;

	public Laser(SnarkFrame frame, int x, int y, int d, Aggressor owner)
	{
	    this.frame = frame;
	    this.layer = frame.laserLayer;
	    this.snarks = frame.snarks;
	    this.lasers = frame.lasers;
	    this.cells = frame.cells;
	    this.owner = owner;
	    this.x = x;
	    this.y = y;
	    this.d = d;

	    forward = true;
	}

	public int move()
	{
	    int newCell = super.move(d);

	    if (forward)
	    {
		switch (newCell)
		{
		case PLAYER:
		    cells[x][y] = EMPTY;
		    frame.playerLayer.setCell(x, y, EMPTY);
		    layer.setCell(x, y, SPLAT);
		    frame.player = null;
		    break;

		case DRAGON:
		    cells[x][y] = EMPTY;
		    frame.playerLayer.setCell(x, y, EMPTY);
		    layer.setCell(x, y, SPLAT);
		    frame.dragon = null;
		    break;

		case EGG:
		case SNARK:
		    cells[x][y] = EMPTY;
		    frame.snarkLayer.setCell(x, y, EMPTY);
		    layer.setCell(x, y, SPLAT);

		    for (int i = 0; i < SNARKS; i++)
		    {
			if ((snarks[i] != null) &&
			    (snarks[i].x == x) &&
			    (snarks[i].y == y))
			{
			    snarks[i] = null;
			    break;
			}
		    }
		    break;

		case TREE:
		    cells[x][y] = EMPTY;
		    frame.backLayer.setCell(x, y, EMPTY);
		    layer.setCell(x, y, SPLAT);
		    break;

		case ROCK:
		    int oldX = x;
		    int oldY = y;

		    newCell = super.move(d);

		    if (newCell == EMPTY)
		    {
			cells[x][y] = ROCK;
			layer.setCell(x, y, EMPTY);
			frame.backLayer.setCell(x, y, ROCK);

			cells[oldX][oldY] = LASER;
			layer.setCell(oldX, oldY, d);
			frame.backLayer.setCell(oldX, oldY, EMPTY);
		    }

		    else
		    {
			x = oldX;
			y = oldY;
		    }
		    break;

		case EMPTY:
		    return EMPTY;
		}

		if (owner == frame.player)
		{
		    switch (newCell)
		    {
		    case DRAGON:
			score += DRAGON_POINTS;
			frame.drawBox(DRAGON_POINTS + " points");
			break;

		    case SNARK:
			score += SNARK_POINTS;
			break;

		    case EGG:
			score += EGG_POINTS;
			break;
		    }
		}

		d = ((d + 3) % 8) + 1;
		forward = false;
		splatX = x;
		splatY = y;
		return EMPTY;
	    }

	    return EMPTY;
	}

	public int moveCell(int dx, int dy)
	{
	    int columns = frame.columns;
	    int rows = frame.rows;

	    x = (x + dx + columns) % columns;
	    y = (y + dy + rows) % rows;

	    int newCell = cells[x][y];

	    if (forward)
	    {
		if (newCell == EMPTY)
		{
		    cells[x][y] = LASER;
		    layer.setCell(x, y, d);
		}

		return newCell;
	    }

	    else
	    {
		if (newCell == LASER)
		{
		    cells[x][y] = EMPTY;
		    layer.setCell(x, y, EMPTY);

		    return EMPTY;
		}

		if (layer.getCell(splatX, splatY) == SPLAT)
		    layer.setCell(splatX, splatY, EMPTY);

		for (int i = 0; i < LASERS; i++)
		    if (this == lasers[i])
			lasers[i] = null;

		return EMPTY;
	    }
	}
    }
}

///////////////////////////////////////////////////////////////////////////////
