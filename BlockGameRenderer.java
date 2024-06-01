package pipira.MIDIPlayer.render.renderer;

import java.awt.Color;
import java.awt.event.KeyEvent;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import javax.sound.midi.Receiver;
import pipira.MIDIPlayer.app.MIDIPlayerApp;
import pipira.MIDIPlayer.midi.MIDISequence;
import pipira.MIDIPlayer.render.renderer.AbstractDefaultMIDIRenderer;
import pipira.MIDIPlayer.render.renderer.BlockGameRenderer;
import pipira.MIDIPlayer.resourcepack.ResourcePack;

public class BlockGameRenderer extends AbstractDefaultMIDIRenderer {
    private final BlockGameRenderer.BlockColor[][] blocks = new BlockGameRenderer.BlockColor[40][10];
    private final Object inputLock = new Object();
    private final HashSet<Integer> keysDown = new HashSet();
    private final ArrayList<Integer> keysPressed = new ArrayList();
    private boolean isGameOver;
    private double time;
    private byte offset = 0;
    private final short delay = 250;
    private long nextmove = delay;
    private long score;
    private final byte UP = 20;
    private final byte DOWN = 10;
    private final byte LEFT = 25;
    private final byte RIGHT = 15;
    private ArrayDeque<byte[]> Snake = new ArrayDeque<byte[]>();
    private byte[] FoodPosition = new byte[2];
    private byte moving_direction = RIGHT;

    public BlockGameRenderer(MIDIPlayerApp app, int width, int height, boolean transparent, Receiver r) {
        super(app, width, height, transparent, r);
        this.assets = new BlockGameRenderer.Assets();
        this.setLoop(true);
    }

    private void reset() {
        for(BlockGameRenderer.BlockColor[] block : this.blocks) {
            Arrays.fill(block, (Object)null);
        }
        this.isGameOver = false;
        this.score = 0L;
        this.time = 0.0D;
        this.moving_direction = RIGHT;
        this.nextmove = delay;
        this.Snake = new ArrayDeque<byte[]>();
        this.Snake.add(this.MakeList((byte)(75/2-1),(byte)(30/2)));
        this.Snake.add(this.MakeList((byte)(75/2),(byte)(30/2)));
        this.Snake.add(this.MakeList((byte)(75/2+1),(byte)(30/2)));
        this.FoodPosition = NewFood(Snake);
    }
    
    private byte[] NewFood(ArrayDeque<byte[]> Snake) {
    	boolean Verified = false;
    	byte X = -1;
    	byte Y = -1;
    	while (!Verified) {
	    	X = (byte)(Math.random()*73+1); //generate new position
	    	Y = (byte)(Math.random()*33+1);
	    	Verified = true;
	    	for (byte[] SnakeBody : Snake) {
	    		if (SnakeBody[0] == X && SnakeBody[1] == Y) {
	    			Verified = false;
	    		}
	    	}
    	}
    	return MakeList(X, Y);
    }
    
    private byte[] MakeList(byte...Values) {
		return Values;
    }
    public synchronized void start() {
        super.start();
        this.reset();
    }

    public synchronized void update() {
        if (this.isGameOver) {
            this.close();
        } else {
            this.time = this.calcTargetTime();
            if (!this.isPaused() && this.isRunning() && !(this.time < 0.0D)) {
                boolean softDrop;
                synchronized(this.inputLock) {
                    softDrop = this.keysDown.contains(83) || this.keysDown.contains(40) || this.keysDown.contains(98);

                    for(Integer key : this.keysPressed) {
                        try {
							switch(key) {
							//Handle keyboard events: 
							case KeyEvent.VK_UP:
								if (!Check180(moving_direction, UP)) {
									this.moving_direction = UP;
								}
								break;
							case KeyEvent.VK_DOWN:
								if (!Check180(moving_direction, DOWN)) {
									this.moving_direction = DOWN;
								}
								break;
							case KeyEvent.VK_LEFT:
								if (!Check180(moving_direction, LEFT)) {
									this.moving_direction = LEFT;
								}
								break;
							case KeyEvent.VK_RIGHT:
								if (!Check180(moving_direction, RIGHT)) {
									this.moving_direction = RIGHT;
								}
								break;
							}
						} catch (Exception e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
                    }
                    this.keysPressed.clear();
                }
            } else {
                synchronized(this.inputLock) {
                    this.keysPressed.clear();
                }
            }

            super.update();
        }
    }

    private void drawBlock(int keyX, int keyY, BlockGameRenderer.BlockColor color) {
        if (keyX >= 0 && keyX < 75 && keyY >= 0) {
            int blockHeight = this.frames.length/35;
            keyX = renderOrder[keyX];
            int i = 0;
            for(int fi = keyY * blockHeight; i < blockHeight; ++fi) {
                this.frames[fi][keyX] = color.ordinal();
                ++i;
            }

        }
    }

    protected void generateFrames(double time) {
        this.framesGeneratedIdx = this.frames.length - 1;
        this.framesGenerated = this.frames.length;
        //Clear screen before drawing new content
        for(Integer[] integers : this.frames) {
            Arrays.fill(integers, (Object)null);
        }
        
        if (time >= this.nextmove && !this.isGameOver) {
        	this.nextmove += delay;
        	byte[] HeadLocation = this.Snake.getLast();
        	switch (this.moving_direction) { // Move the snake in selected direction
        	case UP:
        		this.Snake.add(MakeList((byte)(HeadLocation[0]), (byte)(HeadLocation[1]+1)));
        		break;
        	case DOWN:
        		this.Snake.add(MakeList((byte)(HeadLocation[0]), (byte)(HeadLocation[1]-1)));
        		break;
        	case LEFT:
        		this.Snake.add(MakeList((byte)(HeadLocation[0]-1), (byte)(HeadLocation[1])));
        		break;
        	case RIGHT:
        		this.Snake.add(MakeList((byte)(HeadLocation[0]+1), (byte)(HeadLocation[1])));
        		break;
        	}
            if (this.Snake.getLast()[0] == this.FoodPosition[0] && this.Snake.getLast()[1] == this.FoodPosition[1]) { // Got food
        		this.FoodPosition = NewFood(Snake);
        		this.score++;
        	} else { // No food
        		this.Snake.removeFirst();
        	}
        }
        
        // Border collision
    	if (this.Snake.getLast()[0] >= 74 || this.Snake.getLast()[0] <= 0 || this.Snake.getLast()[1] >= 34 || this.Snake.getLast()[1] <= 0) {
    		this.isGameOver = true;
    	}
    	
        //Draw snake
        for(byte[] SnakeBody : this.Snake) {
        	this.drawBlock(SnakeBody[0], SnakeBody[1], this.isGameOver?BlockColor.GRAY:BlockColor.GREEN);
        }
        
        //Draw food
        this.drawBlock(this.FoodPosition[0], this.FoodPosition[1], this.isGameOver?BlockColor.GRAY:BlockColor.RED);

    	//Draw border
        for (short x = 0; x < 75; x++) {
        	this.drawBlock(x, 35, this.isGameOver&&this.moving_direction==UP?BlockColor.RED:BlockColor.GRAY);
        }
        for (short x = 0; x < 75; x++) {
        	this.drawBlock(x, 0, this.isGameOver&&this.moving_direction==DOWN?BlockColor.RED:BlockColor.GRAY);
        }
        for (short y = 0; y <= 35; y++) {
        	this.drawBlock(0, y, this.isGameOver&&this.moving_direction==LEFT?BlockColor.RED:BlockColor.GRAY);
        }
        for (short y = 0; y <= 35; y++) {
        	this.drawBlock(74, y, this.isGameOver&&this.moving_direction==RIGHT?BlockColor.RED:BlockColor.GRAY);
        }
    }

    protected boolean isTimeInSequence() {
        return true;
    }

    public long getNotes() {
        return this.score;
    }

    public int calcPolyphony() {
        return 0;
    }

    public long calcNps() {
        return 0;
    }

    public void onKeyPress(KeyEvent e) {
        if (this.isRunning()) {
            synchronized(this.inputLock) {
                this.keysDown.add(e.getExtendedKeyCode());
                this.keysPressed.add(e.getExtendedKeyCode());
            }
        }
    }

    public void onKeyRelease(KeyEvent e) {
        if (this.isRunning()) {
            synchronized(this.inputLock) {
                this.keysDown.remove(e.getExtendedKeyCode());
            }
        }
    }

    public class Assets extends AbstractDefaultMIDIRenderer.Assets {
        public void generateColoredTextures(int colorsToGenerate) {
            super.generateColoredTextures(Math.max(BlockGameRenderer.BlockColor.values().length * 2, colorsToGenerate));
        }

        protected Color[] generateColors(MIDISequence seq, ResourcePack pack, int colorCount) {
            Color background = this.app.getConfig().render.getBackground();
            Color[] colors = new Color[colorCount];
            BlockGameRenderer.BlockColor[] blockColors = BlockGameRenderer.BlockColor.values();

            for(int i = 0; i < blockColors.length; ++i) {
                colors[i] = blockColors[i].color;
                colors[blockColors.length + i] = new Color((int)((float)blockColors[i].color.getRed() * 0.2F + (float)background.getRed() * 0.8F), (int)((float)blockColors[i].color.getGreen() * 0.2F + (float)background.getGreen() * 0.8F), (int)((float)blockColors[i].color.getBlue() * 0.2F + (float)background.getBlue() * 0.8F));
            }

            return colors;
        }
    }

    private static enum BlockColor {
        AQUA(65535),
        YELLOW(16776960),
        GREEN(65280),
        RED(16711680),
        BLUE(255),
        ORANGE(16744192),
        PINK(16711807),
        GRAY(4210752);

        final Color color;

        private BlockColor(int rgb) {
            this.color = new Color(rgb);
        }
    }
    
    private boolean Check180(byte OriginalDIR, byte NewDIR) throws Exception {
    	//Prevent the snake from doing a 180deg turn. 
    	return OriginalDIR % 10 == NewDIR % 10 ? true : false;
    }
}
