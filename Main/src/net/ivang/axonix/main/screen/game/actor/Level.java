/*
 * Copyright 2012-2013 Ivan Gadzhega
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package net.ivang.axonix.main.screen.game.actor;

import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import net.ivang.axonix.main.screen.game.GameScreen;

import java.util.*;

/**
 * @author Ivan Gadzhega
 * @since 0.1
 */
public class Level extends Group {

    public static final byte BS_EMPTY = 0;
    public static final byte BS_BLUE = 1;
    public static final byte BS_GREEN = 2;
    public static final byte BS_TAIL = 3;

    private GameScreen gameScreen;

    private int width;
    private int height;
    private byte[][] levelMap;

    public int score;
    public byte percentComplete;
    private int earthBlocks;

    private Protagonist protagonist;
    private Vector2 protStartPos;
    private List<Enemy> enemies;

    private Skin skin;

    public Level(GameScreen gameScreen, Pixmap pixmap, Skin skin) {
        this.width = pixmap.getWidth();
        this.height = pixmap.getHeight();
        this.levelMap = new byte[width][height];
        this.gameScreen = gameScreen;
        this.skin = skin;

        initFromPixmap(pixmap);
    }

    private void initFromPixmap(Pixmap pixmap) {
        final int EARTH = 0x000000;
        final int ENEMY = 0xFF0000;
        final int PROTAGONIST = 0x00FF00;

        enemies = new ArrayList<Enemy>();

        byte[][] levelMap = new byte[width][height];
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                int pix = (pixmap.getPixel(x, height-y-1) >>> 8) & 0xffffff;
                if(pix == EARTH) {
                    levelMap[x][y] = Level.BS_BLUE;
                }else if (pix == ENEMY) {
                    Enemy enemy = new Enemy(x + 0.5f, y + 0.5f, skin);
                    enemies.add(enemy);
                    addActor(enemy);
                    levelMap[x][y] = Level.BS_EMPTY;
                } else if (pix == PROTAGONIST) {
                    protStartPos = new Vector2(x + 0.5f, y + 0.5f);
                    levelMap[x][y] = Level.BS_BLUE;
                } else {
                    levelMap[x][y] = Level.BS_EMPTY;
                }
            }
        }

        protagonist = new Protagonist(protStartPos.x, protStartPos.y, this, skin);
        addActor(protagonist);

        this.setLevelMap(levelMap);
    }

    @Override
    public void act(float delta) {
        if (gameScreen.getState() == GameScreen.State.PLAYING) {
            check();
            super.act(delta);
            // change blocks states
            if(protagonist.isOnNewBlock()) {
                switch (getBlockState(protagonist.getX(), protagonist.getY())) {
                    case BS_EMPTY:
                        setBlockState(protagonist.getX(), protagonist.getY(), Level.BS_TAIL);
                        break;
                    case BS_TAIL:
                        killProtagonist();
                        break;
                    case BS_BLUE:
                        if (getBlockState(protagonist.getPx(), protagonist.getPy()) == Level.BS_TAIL) {
                            fillAreas();
                        }
                        break;
                }
            }
        }
    }

    @Override
    public void draw(SpriteBatch batch, float parentAlpha) {
        for(int i = 0; i < getWidth(); i++) {
            for(int j = 0; j < getHeight(); j++) {
                switch (getBlockState(i, j)) {
                    case Level.BS_BLUE:
                        batch.setColor(1, 1, 1, 1);
                        break;
                    case Level.BS_GREEN:
                        batch.setColor(0, 1, 0.3f, 1);
                        break;
                    case Level.BS_TAIL:
                        batch.setColor(0.3f, 0.3f, 1f, 1);
                        break;
                    default:
                        continue;
                }
                batch.draw(skin.getRegion("block"), getX() + i * getScaleX(), getY() + j * getScaleY(), getScaleX(), getScaleY());
            }
        }

        super.draw(batch, parentAlpha);
    }

    //---------------------------------------------------------------------
    // Helper methods
    //---------------------------------------------------------------------

    private void check() {
        // check percent
        if (percentComplete > 80) {
            gameScreen.setState(GameScreen.State.LEVEL_COMPLETED);
        }
        // check collisions
        for (Enemy enemy : enemies) {
            if (getBlockState(enemy.getX(), enemy.getY()) == Level.BS_TAIL) {
                killProtagonist();
                break;
            }
        }
    }

    private void killProtagonist() {
        removeActor(protagonist);
        protagonist = new Protagonist(protStartPos.x, protStartPos.y, this, skin);
        addActor(protagonist);

        for(int i = 1; i < getWidth() - 1; i++) {
            for(int j = 1; j < getHeight() - 1; j++) {
                if (getBlockState(i, j) == Level.BS_TAIL) {
                    setBlockState(i, j, Level.BS_EMPTY);
                }
            }
        }

        gameScreen.setLives(gameScreen.getLives() - 1);
        gameScreen.getNotification().setText("LIFE LEFT!");
        gameScreen.getNotification().addAction(Actions.sequence(Actions.show(), Actions.delay(1), Actions.hide()));
    }

    private void fillAreas() {
        // thanks to http://habrahabr.ru/post/119244/
        byte[][] tmpState = new byte[width][height];
        byte spotNum = 0;
        Map<Byte, List<Vector2>> spots = new HashMap<Byte, List<Vector2>>();
        for(int i = 1; i < width - 1; i++) {
            for(int j = 1; j < height - 1; j++) {
                byte A = levelMap[i][j];
                if (A == BS_EMPTY) {
                    byte B = tmpState[i][j-1];
                    byte C = tmpState[i-1][j];

                    if ( B == 0) {
                        if (C == 0) {
                            // New Spot
                            spotNum++;
                            tmpState[i][j] = spotNum;

                            List<Vector2> spot = new ArrayList<Vector2>();
                            spot.add(new Vector2(i,j));

                            spots.put(spotNum, spot);
                        } else {   // C!=0
                            tmpState[i][j] = C;
                            spots.get(C).add(new Vector2(i,j));
                        }
                    }

                    if (B != 0) {
                        if(C == 0) {
                            tmpState[i][j] = B;
                            spots.get(B).add(new Vector2(i,j));
                        } else { // C != 0
                            tmpState[i][j] = B;
                            spots.get(B).add(new Vector2(i,j));
                            if (B != C) {
                                for(int m = 1; m < width - 1; m++) {
                                    for(int n = 1; n < height; n++) {
                                        if (tmpState[m][n] == C) {
                                            tmpState[m][n] = B;
                                        }
                                    }
                                }
                                spots.get(B).addAll(spots.get(C));
                                spots.remove(C);
                            }
                        }
                    }
                } else if(A == BS_TAIL) {
                    // turn tail to blue blocks
                    setBlockState(i, j, BS_BLUE);
                    score++;
                    earthBlocks++;

                }
            }
        }

        Iterator iterator = spots.keySet().iterator();
        while (iterator.hasNext()) {
            check_spot_points:
            for(Vector2 pos: spots.get((Byte) iterator.next())) {
                for (Enemy enemy : enemies) {
                    if ((pos.x == (int) enemy.getX()) && (pos.y == (int) enemy.getY())) {
                        iterator.remove();
                        break check_spot_points;
                    }
                }
            }
        }

        for(List<Vector2> spot : spots.values()) {
            for(Vector2 pos : spot) {
                setBlockState(pos.x, pos.y, BS_GREEN);
                score++;
                earthBlocks++;
            }
            float bonus = 1 + (float) spot.size() / 200;
            score += spot.size() * bonus;

        }

        // update percentage
        percentComplete = (byte) (((float) earthBlocks / ((width - 2) * (height - 2))) * 100) ;
    }

    //---------------------------------------------------------------------
    // Getters & Setters
    //---------------------------------------------------------------------

    public void setBlockState(int x, int y, byte value) {
        if (x >= 0 && x < width && y >=0 && y < height) {
            levelMap[x][y] = value;
        }
    }

    public void setBlockState(float x, float y, byte value) {
        setBlockState((int) x, (int) y, value);
    }

    public byte getBlockState(int x, int y) {
        return levelMap[x][y];
    }

    public byte getBlockState(float x, float y) {
        return getBlockState((int) x, (int) y);
    }

    public float getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public float getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public byte[][] getLevelMap() {
        return levelMap;
    }

    public void setLevelMap(byte[][] levelMap) {
        this.levelMap = levelMap;
    }

    public int getScore() {
        return score;
    }

    public void setScore(int score) {
        this.score = score;
    }

    public byte getPercentComplete() {
        return percentComplete;
    }

    public void setPercentComplete(byte percentComplete) {
        this.percentComplete = percentComplete;
    }
}