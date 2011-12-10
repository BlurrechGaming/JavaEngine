package blurrech.co.uk;
import java.awt.*;
import java.lang.System;
import java.awt.event.*;

import blurrech.co.uk.audio.MidiSequence;
import blurrech.co.uk.audio.SoundClip;
import blurrech.co.uk.graphics.AnimatedSprite;
import blurrech.co.uk.graphics.ImageEntity;
import blurrech.co.uk.graphics.Point2D;

public class InsertGameName extends Game {
    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;
    static int FRAMERATE = 60;
    static int SCREENWIDTH = 800;
    static int SCREENHEIGHT = 600;

    final int BULLET_SPEED = 4;
    final double ACCELERATION = 0.05;
    final double SHIPROTATION = 5.0;

    //sprite state values
    final int STATE_NORMAL = 0;
    final int STATE_COLLIDED = 1;
    final int STATE_EXPLODING = 2;
    final int SPRITE_INVISIBLE = 3;
    final int SPRITE_ATTACKING = 4;
    final int SPRITE_WEAK = 5;
    final int SPRITE_RESTORING = 6;

    //sprite types
    final int SPRITE_SHIP = 1;
    final int SPRITE_BULLET = 100;
    final int SPRITE_EXPLOSION = 200;

    //game states
    final int GAME_START_MENU = 0;
    final int GAME_RUNNING = 1;
    final int GAME_OVER = 2;
    final int GAME_LEVEL_SELECT = 3;

    //various toggles
    boolean showBounds = true;
    boolean collisionTesting = true;

    //define the images used in the game
    ImageEntity background;
    ImageEntity bulletImage;
    ImageEntity[] explosions = new ImageEntity[2];
    ImageEntity[] shipImage = new ImageEntity[3];
    ImageEntity[] barImage = new ImageEntity[2];
    ImageEntity barFrame;

    //health/shield meters and score
    int health = 20;
    int shield = 100;
    int score = 0;
    int highscore = 0;
    int gameState = GAME_START_MENU;
    boolean keyShield = false;
    
    long fireTicks;
    long shieldTicks;
    

    //used to make ship temporarily invulnerable
    long collisionTimer = 0;

    //sound eff-ects and music
    MidiSequence music = new MidiSequence();
    SoundClip shoot = new SoundClip();
    SoundClip explosion = new SoundClip();

    public InsertGameName() {
        super(FRAMERATE, SCREENWIDTH, SCREENHEIGHT);
    }

    void gameStartup() {
        //load sounds and music
        music.load("music.mid");
        shoot.load("shoot.au");
        explosion.load("explode.au");

        //load the health/shield bars
        barFrame = new ImageEntity(this);
        barFrame.load("barframe.png");
        barImage[0] = new ImageEntity(this);
        barImage[0].load("bar_health.png");
        barImage[1] = new ImageEntity(this);
        barImage[1].load("bar_shield.png");

        //load the background image
        background = new ImageEntity(this);
        background.load("bluespace.png");

        //create the ship sprite--first in the sprite list
        shipImage[0] = new ImageEntity(this);
        shipImage[0].load("spaceship.png");
        shipImage[1] = new ImageEntity(this);
        shipImage[1].load("ship_thrust.png");
        shipImage[2] = new ImageEntity(this);
        shipImage[2].load("ship_shield.png");

        AnimatedSprite ship = new AnimatedSprite(this, graphics());
        ship.setSpriteType(SPRITE_SHIP);
        ship.setImage(shipImage[0].getImage());
        ship.setFrameWidth(ship.imageWidth());
        ship.setFrameHeight(ship.imageHeight());
        ship.setPosition(new Point2D(SCREENWIDTH/2, SCREENHEIGHT/2));
        ship.setAlive(true);
        //start ship off as invulnerable
        ship.setState(STATE_EXPLODING);
        collisionTimer = System.currentTimeMillis();
        sprites().add(ship);

        //load the bullet sprite image
        bulletImage = new ImageEntity(this);
        bulletImage.load("plasmashot.png");

        //load the explosion sprite image
        explosions[0] = new ImageEntity(this);
        explosions[0].load("explosion.png");
        explosions[1] = new ImageEntity(this);
        explosions[1].load("explosion2.png");

        //start off in pause mode
        pauseGame();
    }

    private void restartGame() {
        //restart the music soundtrack
        music.setLooping(true);
        music.play();

        //save the ship for the restart
        AnimatedSprite ship = (AnimatedSprite) sprites().get(0);

        //wipe out the sprite list to start over!
        sprites().clear();

        //add the saved ship to the sprite list
        ship.setPosition(new Point2D(SCREENWIDTH/2, SCREENHEIGHT/2));
        ship.setAlive(true);
        ship.setState(STATE_EXPLODING);
        collisionTimer = System.currentTimeMillis();
        ship.setVelocity(new Point2D(0, 0));
        sprites().add(ship);

        //reset variables
        health = 20;
        shield = 20;
        score = 0;
    }

    void gameTimedUpdate() {
        checkInput();

        /*if (!gamePaused() && sprites().size() == 1) {
            restartGame();
            gameState = GAME_OVER;
        }*/
    }

    void gameRefreshScreen() {
        Graphics2D g2d = graphics();

        //draw the background
        g2d.drawImage(background.getImage(),0,0,SCREENWIDTH-1,SCREENHEIGHT-1,this);
        switch (gameState) { 
        case GAME_START_MENU:
            g2d.setFont(new Font("Verdana", Font.BOLD, 36));
            g2d.setColor(Color.BLACK);
            int x = 270, y = 15;
            g2d.setFont(new Font("Times New Roman", Font.ITALIC | Font.BOLD, 20));
            g2d.setColor(Color.YELLOW);
            g2d.drawString("CONTROLS:", x, ++y*20);
            g2d.drawString("ROTATE - Left/Right Arrows", x+20, ++y*20);
            g2d.drawString("THRUST - Up Arrow", x+20, ++y*20);
            g2d.drawString("SHIELD - Shift key (no scoring)", x+20, ++y*20);
            g2d.drawString("FIRE - Ctrl key", x+20, ++y*20);

            g2d.setColor(Color.WHITE);
            g2d.drawString("POWERUPS INCREASE FIREPOWER!", 240, 480);

            g2d.setFont(new Font("Ariel", Font.BOLD, 24));
            g2d.setColor(Color.ORANGE);
            g2d.drawString("Press ENTER to start", 280, 570);
            break;
        case GAME_RUNNING:
            g2d.drawImage(barFrame.getImage(), SCREENWIDTH - 132, 18, this);
            for (int n = 0; n < health; n++) {
                int dx = SCREENWIDTH - 130 + n * 5;
                g2d.drawImage(barImage[0].getImage(), dx, 20, this);
            }
            g2d.drawImage(barFrame.getImage(), SCREENWIDTH - 132, 33, this);
            for (int n = 0; n < shield; n++) {
                int dx = SCREENWIDTH - 130 + n * 5;
                g2d.drawImage(barImage[1].getImage(), dx, 35, this);
            }
            g2d.setFont(new Font("Verdana", Font.BOLD, 24));
            g2d.setColor(Color.WHITE);
            g2d.drawString("" + score, 20, 40);
            g2d.setColor(Color.RED);
            g2d.drawString("" + highscore, 350, 40);
            break;
        case GAME_OVER:
            g2d.drawString("Press ENTER to restart", 260, 500);
            break;
        }
    }

    void gameShutdown() {
        music.stop();
        shoot.stop();
        explosion.stop();
    }

    public void spriteUpdate(AnimatedSprite sprite) {
        switch(sprite.spriteType()) {
        case SPRITE_SHIP:
            warp(sprite);
            break;

        case SPRITE_BULLET:
            warp(sprite);
            break;

        case SPRITE_EXPLOSION:
            if (sprite.currentFrame() == sprite.totalFrames()-1) {
                sprite.setAlive(false);
            }
            break;
        }
    }

    public void spriteDraw(AnimatedSprite sprite) {
        if (showBounds) {
            if (sprite.collided())
                sprite.drawBounds(Color.RED);
            else
                sprite.drawBounds(Color.BLUE);
        }
    }

    public void spriteDying(AnimatedSprite sprite) {
    }

    public void spriteCollision(AnimatedSprite spr1, AnimatedSprite spr2) {
        if (!collisionTesting) return;
        switch(spr1.spriteType()) {
          
        }
    }

    public void gameMouseDown() { }
    public void gameMouseUp() { }
    public void gameMouseMove() { }

    public void checkInput() {
		boolean up = InputHandler.keys[KeyEvent.VK_W] || InputHandler.keys[KeyEvent.VK_UP];
		boolean down = InputHandler.keys[KeyEvent.VK_S] || InputHandler.keys[KeyEvent.VK_DOWN];
		boolean left = InputHandler.keys[KeyEvent.VK_A] || InputHandler.keys[KeyEvent.VK_LEFT];
		boolean right = InputHandler.keys[KeyEvent.VK_D] || InputHandler.keys[KeyEvent.VK_RIGHT];
		boolean space = InputHandler.keys[KeyEvent.VK_SPACE];
		boolean enter = InputHandler.keys[KeyEvent.VK_ENTER];
		boolean esc = InputHandler.keys[KeyEvent.VK_ESCAPE];
		boolean control = InputHandler.keys[KeyEvent.VK_CONTROL];
		boolean shift = InputHandler.keys[KeyEvent.VK_SHIFT];
    	if (enter) {
             if (gameState == GAME_START_MENU) {
                 restartGame();
                 resumeGame();
                 gameState = GAME_RUNNING;
             }
             else if (gameState == GAME_OVER) {
                 restartGame();
                 resumeGame();
                 gameState = GAME_RUNNING;
             }
         }
        if (gameState != GAME_RUNNING) return;
		
        AnimatedSprite ship = (AnimatedSprite)sprites().get(0);
        if (esc) {
                pauseGame();
                gameState = GAME_OVER;
        }
        if (control) {
        	fireBullet();
        }
        if (InputHandler.keys[KeyEvent.VK_C]) {
        	collisionTesting = !collisionTesting;
        }
        if (InputHandler.keys[KeyEvent.VK_B]) {
        	showBounds = !showBounds;
        }
        if (left) {
            ship.setFaceAngle(ship.faceAngle() - SHIPROTATION);
            if (ship.faceAngle() < 0)
                ship.setFaceAngle(360 - SHIPROTATION);
        } else if (right) {
            ship.setFaceAngle(ship.faceAngle() + SHIPROTATION);
            if (ship.faceAngle() > 360)
                ship.setFaceAngle(SHIPROTATION);
        }
        if (up) {
            ship.setImage(shipImage[1].getImage());
            applyThrust();
        }
        else if (shift && shield > 0) {
            shieldTicks++;
        	if (shieldTicks > 25) {
        		shield -= 0.01;
        		shieldTicks = 0;
        	}
        	ship.setImage(shipImage[2].getImage());
            keyShield = true;
            return;
        }
        else  
            ship.setImage(shipImage[0].getImage());
            keyShield = false;
            shieldTicks = 0;
    }

    public void applyThrust() {
        
        AnimatedSprite ship = (AnimatedSprite)sprites().get(0);

        ship.setMoveAngle(ship.faceAngle() - 90);
       
        double velx = ship.velocity().X();
        velx += calcAngleMoveX(ship.moveAngle()) * ACCELERATION;
        if (velx < -5) velx = -5;
        else if (velx > 5) velx = 5;
        double vely = ship.velocity().Y();
        vely += calcAngleMoveY(ship.moveAngle()) * ACCELERATION;
        if (vely < -5) vely = -5;
        else if (vely > 5) vely = 5;
        ship.setVelocity(new Point2D(velx, vely));

    }

    public void fireBullet() {
    	fireTicks++;
    	if (fireTicks >= 10) {
        AnimatedSprite[] bullets = new AnimatedSprite[6];
        bullets[0] = stockBullet();
        sprites().add(bullets[0]);
        shoot.play();
        fireTicks = 0;
    	}
    }

     private AnimatedSprite stockBullet() {
         AnimatedSprite ship = (AnimatedSprite)sprites().get(0);
         AnimatedSprite bul = new AnimatedSprite(this, graphics());
         bul.setAlive(true);
         bul.setImage(bulletImage.getImage());
         bul.setFrameWidth(bulletImage.width());
         bul.setFrameHeight(bulletImage.height());
         bul.setSpriteType(SPRITE_BULLET);
         bul.setLifespan(90);
         bul.setFaceAngle(ship.faceAngle());
         bul.setMoveAngle(ship.faceAngle() - 90);
         double angle = bul.moveAngle();
         double svx = calcAngleMoveX(angle) * BULLET_SPEED;
         double svy = calcAngleMoveY(angle) * BULLET_SPEED;
         bul.setVelocity(new Point2D(svx, svy));
         double x = ship.center().X() - bul.imageWidth()/2;
         double y = ship.center().Y() - bul.imageHeight()/2;
         bul.setPosition(new Point2D(x,y));
         return bul;
     }

    public void startBigExplosion(Point2D point) {
        AnimatedSprite expl = new AnimatedSprite(this,graphics());
        expl.setSpriteType(SPRITE_EXPLOSION);
        expl.setAlive(true);
        expl.setAnimImage(explosions[0].getImage());
        expl.setTotalFrames(16);
        expl.setColumns(4);
        expl.setFrameWidth(96);
        expl.setFrameHeight(96);
        expl.setFrameDelay(2);
        expl.setPosition(point);
        sprites().add(expl);
        explosion.play();
    }

    public void startSmallExplosion(Point2D point) {
        AnimatedSprite expl = new AnimatedSprite(this,graphics());
        expl.setSpriteType(SPRITE_EXPLOSION);
        expl.setAlive(true);
        expl.setAnimImage(explosions[1].getImage());
        expl.setTotalFrames(8);
        expl.setColumns(4);
        expl.setFrameWidth(40);
        expl.setFrameHeight(40);
        expl.setFrameDelay(2);
        expl.setPosition(point);
        sprites().add(expl);
        explosion.play();

    }
    public void warp(AnimatedSprite spr) {
        int w = spr.frameWidth()-1;
        int h = spr.frameHeight()-1;
        if (spr.position().X() < 0-w)
            spr.position().setX(SCREENWIDTH);
        else if (spr.position().X() > SCREENWIDTH)
            spr.position().setX(0-w);
        if (spr.position().Y() < 0-h)
            spr.position().setY(SCREENHEIGHT);
        else if (spr.position().Y() > SCREENHEIGHT)
            spr.position().setY(0-h);
    }

    public void bumpScore(int howmuch) {
        score += howmuch;
        if (score > highscore)
            highscore = score;
    }
}
