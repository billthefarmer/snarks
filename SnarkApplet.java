///////////////////////////////////////////////////////////////////////////////
//
//   SnarkApplet.java  1.0  31 July 2006
//
//   SnarkApplet - Recreate Snarks and Dragon game. Retrofitted with
//   Java version 1.0 methods and event model for compatability.
//
//   Copyright (C) 2006 Bill Farmer
//
///////////////////////////////////////////////////////////////////////////////

import java.awt.*;
import java.applet.*;
import java.util.Random;

///////////////////////////////////////////////////////////////////////////////

public class SnarkApplet extends Applet implements Runnable
{
    Snark[] snarks;
    Laser[] lasers;
    Random random;
    int[][] cells;
    Player player;
    Dragon dragon;
    Thread thread;
    boolean auto;
    Image image;
    long time;

    Color bgColour, fgColour;
    int score, highScore;
    int columns, rows;
    int width, height;
    String images;
    int tileSize;

    LayerManager lm;

    TiledLayer backLayer;
    TiledLayer laserLayer;
    TiledLayer snarkLayer;
    TiledLayer playerLayer;

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

    static final String[] COLOUR_NAMES = {"black", "white", "red", "green",
					  "blue", "yellow", "cyan", "magenta"};
    static final int[] COLOURS = {0x000000, 0xffffff, 0xff0000, 0x00ff00,
				  0x0000ff, 0xffff00, 0x00ffff, 0xff00ff};

///////////////////////////////////////////////////////////////////////////////

    public void init()
    {
	random = new Random();

	width = getWidth();
	height = getHeight();

	image = createImage(width, height);

	fgColour = Color.green;
	bgColour = Color.black;

	tileSize = 16;
	images = "";

	try
	{
	    String s = getParameter("Foreground");
	    if (s != null)
		fgColour = getColour(s);

	    s = getParameter("Background");
	    if (s != null)
		bgColour = getColour(s);

	    s = getParameter("TileSize");
	    if (s != null)
		tileSize = Integer.parseInt(s);

	    s = getParameter("Images");
	    if (s != null)
		images = s;
	}

	catch (Exception e) {}
    }

    public Color getColour(String s)
    {
	int c = 0;

	if (s != null)
	{
	    if (s.startsWith("#"))
		c = Integer.parseInt(s.substring(1), 16);

	    else
		for (int i = 0; i < COLOUR_NAMES.length; i++)
		    if (s.equals(COLOUR_NAMES[i]))
			c = COLOURS[i];
	}

	return new Color(c);
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

	if (t != null)
	    while (t.isAlive())
		Thread.yield();
    }

    public String getAppletInfo()
    {
	return "SnarkApplet - Copyright (C) 2006 Bill Farmer";
    }

    public String[][] getParameterInfo()
    {
	String[][] parameterInfo =
	    {{"Background", "string", "Background colour"},
	     {"Foreground", "string", "Foreground colour"},
	     {"Images", "string", "Images path"},
	     {"TileSize", "integer", "Tile size"}};

	return parameterInfo;
    }

    public void setLayers()
    {
	columns = width / tileSize;
	rows = (height / tileSize);

	cells = new int[columns][rows];

	snarks = new Snark[SNARKS];
	lasers = new Laser[LASERS];

	MediaTracker tracker = new MediaTracker(this);

	try
	{
	    Image backImage = getImage(getCodeBase(), images + "back.png");
	    Image laserImage = getImage(getCodeBase(), images + "laser.png");
	    Image snarkImage = getImage(getCodeBase(), images + "snark.png");
	    Image playerImage = getImage(getCodeBase(), images + "player.png");

	    tracker.addImage(backImage, 0);
	    tracker.addImage(laserImage, 0);
	    tracker.addImage(snarkImage, 0);
	    tracker.addImage(playerImage, 0);

	    tracker.waitForAll();

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
	Graphics g = image.getGraphics();
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
	Graphics g = image.getGraphics();
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
	Graphics g = image.getGraphics();
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
	repaint();

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
	    showStatus("Click on the image to select");

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
	Graphics g = image.getGraphics();
	FontMetrics fm = g.getFontMetrics();
	int h = fm.getHeight();

	int xOffset = (width - (columns * tileSize)) / 2;
	int yOffset = h - ((h / tileSize) * tileSize);

	snarkLayer.setAnimatedTile(ANIMATED_TILE, n);
	playerLayer.setAnimatedTile(ANIMATED_TILE, n);
	backLayer.setAnimatedTile(ANIMATED_TILE, ((n - 1) % 4) + TREE);

	lm.paint(g, xOffset, yOffset);
	repaint();
	g.dispose();
    }

    public void tidy()
    {
	for (int x = 0; x < columns; x++)
	    for (int y = 0; y < rows; y++)
		if (laserLayer.getCell(x, y) == SPLAT)
		    laserLayer.setCell(x, y, EMPTY);
    }

    public void run()
    {
	setLayers();

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
	Graphics g = image.getGraphics();
	int d = Math.min(height, width) / 2;
	g.setColor(bgColour);

	for (int i = 0; i < d + 1; i++)
	{
	    g.drawRect(i, i, width - (i * 2), height - (i * 2));
	    repaint();

	    delay(10);

	    if (thread == null)
		break;
	}

	g.dispose();
    }

    public void playSnarks()
    {
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

	showStatus("Score: " + score);

	for (int t = 0; t < 6; t++)
	{
	    for (int i = 0; i != LASERS; i++)
		if (lasers[i] != null)
		    lasers[i].move();

	    Graphics g = image.getGraphics();

	    g.setColor(bgColour);
	    g.fillRect(0, 0, width, height);

	    g.setColor(fgColour);
	    lm.paint(g, xOffset, yOffset);
	    repaint();
	    delay(50);
	}
    }

    public void flashPlayer()
    {
	Graphics g = image.getGraphics();

	int xOffset = (width - (columns * tileSize)) / 2;
	int yOffset = (height - (rows * tileSize)) / 2;

	g.setColor(bgColour);
	g.fillRect(0, 0, width, height);

	g.setColor(fgColour);

	playerLayer.setCell(player.x, player.y, EMPTY);

	lm.paint(g, xOffset, yOffset);
	repaint();
	delay(500);

	playerLayer.setCell(player.x, player.y, player.d);

	lm.paint(g, xOffset, yOffset);
	repaint();
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

    public boolean keyDown(Event e, int key)
    {
	switch (key)
	{
	case Event.F2:
	    stop();
	    auto = !auto;
	    drawBox("Auto mode " + ((auto)? "on": "off"));
	    start();
	    break;

	case Event.ESCAPE:
	    stop();
	    drawBox("Stop Snarks");
	    break;

	case Event.INSERT:
	case Event.DELETE:
	    drawBox("Start Snarks");
	    start();
	    break;

	default:
	    keysPressed = 1 << getGameAction(key);
	    break;
	}

	return true;
    }

    public boolean keyUp(Event e, int key)
    {
	keysReleased = 1 << getGameAction(key);
	return true;
    }

    public int getGameAction(int key)
    {
	switch (key)
	{
	case Event.UP:
	    return N;

	case Event.PGUP:
	    return NE;

	case Event.RIGHT:
	    return E;

	case Event.PGDN:
	    return SE;

	case Event.DOWN:
	    return S;

	case Event.END:
	    return SW;

	case Event.LEFT:
	    return W;

	case Event.HOME:
	    return NW;

	case Event.ENTER:
	case Event.INSERT:
	case 65535:
	    return FIRE;
	}

	return 0;
    }

    public void update(Graphics g)
    {
	paint(g);
    }

    public void paint(Graphics g)
    {
	if (image != null)
	    g.drawImage(image, 0, 0, this);
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

///////////////////////////////////////////////////////////////////////////////

    abstract class Automaton
    {
	SnarkApplet applet;
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
	    int columns = applet.columns;
	    int rows = applet.rows;

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

	public Snark(SnarkApplet applet, int x, int y, int d)
	{
	    this.applet = applet;
	    this.layer = applet.snarkLayer;
	    this.cells = applet.cells;
	    this.snarks = applet.snarks;
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
		    int newX = applet.player.x;
		    int newY = applet.player.y;

		    cells[newX][newY] = EMPTY;
		    applet.playerLayer.setCell(newX, newY, EMPTY);
		    applet.laserLayer.setCell(newX, newY, SPLAT);
		    applet.player = null;
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
			snarks[i] = new Snark(applet, x, y, EGG_TILE);
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
	    int columns = applet.columns;
	    int rows = applet.rows;

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
		    lasers[i] = new Laser(applet, x, y, dir, this);
		    break;
		}
	    }

	    return EMPTY;
	}
    }

///////////////////////////////////////////////////////////////////////////////

    class Dragon extends Aggressor
    {
	public Dragon(SnarkApplet applet, int x, int y, int d)
	{
	    this.applet = applet;
	    this.layer = applet.playerLayer;
	    this.lasers = applet.lasers;
	    this.cells = applet.cells;
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
	public Player(SnarkApplet applet, int x, int y, int d)
	{
	    this.applet = applet;
	    this.layer = applet.playerLayer;
	    this.lasers = applet.lasers;
	    this.cells = applet.cells;
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

	public Laser(SnarkApplet applet, int x, int y, int d, Aggressor owner)
	{
	    this.applet = applet;
	    this.layer = applet.laserLayer;
	    this.snarks = applet.snarks;
	    this.lasers = applet.lasers;
	    this.cells = applet.cells;
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
		    applet.playerLayer.setCell(x, y, EMPTY);
		    layer.setCell(x, y, SPLAT);
		    applet.player = null;
		    break;

		case DRAGON:
		    cells[x][y] = EMPTY;
		    applet.playerLayer.setCell(x, y, EMPTY);
		    layer.setCell(x, y, SPLAT);
		    applet.dragon = null;
		    break;

		case EGG:
		case SNARK:
		    cells[x][y] = EMPTY;
		    applet.snarkLayer.setCell(x, y, EMPTY);
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
		    applet.backLayer.setCell(x, y, EMPTY);
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
			applet.backLayer.setCell(x, y, ROCK);

			cells[oldX][oldY] = LASER;
			layer.setCell(oldX, oldY, d);
			applet.backLayer.setCell(oldX, oldY, EMPTY);
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

		if (owner == applet.player)
		{
		    switch (newCell)
		    {
		    case DRAGON:
			score += DRAGON_POINTS;
			applet.drawBox(DRAGON_POINTS + " points");
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
	    int columns = applet.columns;
	    int rows = applet.rows;

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
