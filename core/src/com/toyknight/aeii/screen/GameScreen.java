package com.toyknight.aeii.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.toyknight.aeii.AEIIApplication;
import com.toyknight.aeii.manager.GameManager;
import com.toyknight.aeii.manager.LocalGameManager;
import com.toyknight.aeii.ResourceManager;
import com.toyknight.aeii.animator.*;
import com.toyknight.aeii.entity.*;
import com.toyknight.aeii.entity.player.LocalPlayer;
import com.toyknight.aeii.listener.GameManagerListener;
import com.toyknight.aeii.renderer.*;
import com.toyknight.aeii.screen.internal.*;
import com.toyknight.aeii.utils.Language;
import com.toyknight.aeii.utils.TileFactory;
import com.toyknight.aeii.utils.UnitToolkit;

import java.util.ArrayList;
import java.util.Set;

/**
 * Created by toyknight on 4/4/2015.
 */
public class GameScreen extends Stage implements Screen, GameManagerListener {

    private final int ts;

    private final int RIGHT_PANEL_WIDTH;
    private final AEIIApplication context;

    private final SpriteBatch batch;
    private final TileRenderer tile_renderer;
    private final UnitRenderer unit_renderer;
    private final AlphaRenderer alpha_renderer;
    private final MovePathRenderer move_path_renderer;
    private final StatusBarRenderer status_bar_renderer;
    private final RightPanelRenderer right_panel_renderer;
    private final AttackInformationRenderer attack_info_renderer;
    private final ShapeRenderer shape_renderer;
    private final LocalGameManager manager;

    private final CursorAnimator cursor;
    private final AttackCursorAnimator attack_cursor;

    private final MapViewport viewport;

    private int pointer_x;
    private int pointer_y;
    private int cursor_map_x;
    private int cursor_map_y;
    private int press_map_x;
    private int press_map_y;
    private int drag_distance_x;
    private int drag_distance_y;

    private final TextField command_line;
    private TextButton btn_menu;
    private TextButton btn_end_turn;
    private ActionButtonBar action_button_bar;
    private SaveLoadDialog save_load_dialog;
    private UnitStore unit_store;
    private MiniMap mini_map;
    private GameMenu menu;

    public GameScreen(AEIIApplication context) {
        this.context = context;
        this.ts = context.getTileSize();
        this.RIGHT_PANEL_WIDTH = 3 * ts;

        this.viewport = new MapViewport();
        this.viewport.width = Gdx.graphics.getWidth() - RIGHT_PANEL_WIDTH;
        this.viewport.height = Gdx.graphics.getHeight() - ts;

        this.batch = new SpriteBatch();
        this.tile_renderer = new TileRenderer(ts);
        this.unit_renderer = new UnitRenderer(this, ts);
        this.alpha_renderer = new AlphaRenderer(this, ts);
        this.move_path_renderer = new MovePathRenderer(this, ts);
        this.status_bar_renderer = new StatusBarRenderer(this, ts);
        this.right_panel_renderer = new RightPanelRenderer(this, ts);
        this.attack_info_renderer = new AttackInformationRenderer(this);
        this.shape_renderer = new ShapeRenderer();
        this.shape_renderer.setAutoShapeType(true);
        this.manager = new LocalGameManager();
        this.manager.setGameManagerListener(this);

        this.cursor = new CursorAnimator(this, ts);
        this.attack_cursor = new AttackCursorAnimator(this, ts);

        this.command_line = new TextField("", getContext().getSkin());
        initComponents();
    }

    private void initComponents() {
        this.command_line.setPosition(0, Gdx.graphics.getHeight() - command_line.getHeight());
        this.command_line.setWidth(Gdx.graphics.getWidth());
        this.command_line.setVisible(false);
        this.addActor(command_line);

        this.btn_menu = new TextButton(Language.getText("LB_MENU"), getContext().getSkin());
        this.btn_menu.setBounds(Gdx.graphics.getWidth() - RIGHT_PANEL_WIDTH, Gdx.graphics.getHeight() - ts, RIGHT_PANEL_WIDTH, ts);
        this.btn_menu.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                closeAllWindows();
                menu.display();
                onButtonUpdateRequested();
            }
        });
        this.addActor(btn_menu);
        this.btn_end_turn = new TextButton(Language.getText("LB_END_TURN"), getContext().getSkin());
        this.btn_end_turn.setBounds(Gdx.graphics.getWidth() - RIGHT_PANEL_WIDTH, 0, RIGHT_PANEL_WIDTH, ts);
        this.btn_end_turn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                manager.endCurrentTurn();
                onButtonUpdateRequested();
            }
        });
        this.addActor(btn_end_turn);

        //action button bar
        this.action_button_bar = new ActionButtonBar(this, manager);
        this.action_button_bar.setPosition(0, ts * 2);
        this.addActor(action_button_bar);

        //save load dialog
        this.save_load_dialog =
                new SaveLoadDialog(getContext(), new Rectangle(0, ts, getViewportWidth(), getViewportHeight()));
        this.addActor(save_load_dialog);
        this.save_load_dialog.setVisible(false);

        //unit store
        this.unit_store = new UnitStore(this, getContext().getSkin());
        this.addActor(unit_store);
        this.unit_store.setVisible(false);

        //game menu
        this.menu = new GameMenu(this);
        this.addActor(menu);
        this.menu.setVisible(false);

        //mini map
        this.mini_map = new MiniMap(this);
        this.addActor(mini_map);
        this.mini_map.setVisible(false);
    }

    public AEIIApplication getContext() {
        return context;
    }

    @Override
    public void draw() {
        batch.begin();
        drawMap();
        if (!manager.isAnimating() /*&& getGame().isLocalPlayer()*/) {
            switch (manager.getState()) {
                case LocalGameManager.STATE_REMOVE:
                case LocalGameManager.STATE_MOVE:
                    alpha_renderer.drawMoveAlpha(batch, manager.getMovablePositions());
                    move_path_renderer.drawMovePath(batch, manager.getMovePath(getCursorMapX(), getCursorMapY()));
                    break;
                case LocalGameManager.STATE_PREVIEW:
                    if (getGame().getCurrentPlayer() instanceof LocalPlayer) {
                        alpha_renderer.drawMoveAlpha(batch, manager.getMovablePositions());
                    }
                    break;
                case LocalGameManager.STATE_ATTACK:
                case LocalGameManager.STATE_SUMMON:
                case LocalGameManager.STATE_HEAL:
                    if (getGame().getCurrentPlayer() instanceof LocalPlayer) {
                        alpha_renderer.drawAttackAlpha(batch, manager.getAttackablePositions());
                    }
                    break;
                default:
                    //do nothing
            }
        }
        drawTombs();
        drawUnits();
        drawCursor();
        drawAnimation();
        attack_info_renderer.render(batch);
        status_bar_renderer.drawStatusBar(batch);
        right_panel_renderer.drawStatusBar(batch);
        batch.end();
        super.draw();
    }

    private void drawMap() {
        for (int x = 0; x < getGame().getMap().getWidth(); x++) {
            for (int y = 0; y < getGame().getMap().getHeight(); y++) {
                int sx = getXOnScreen(x);
                int sy = getYOnScreen(y);
                if (isWithinPaintArea(sx, sy)) {
                    int index = getGame().getMap().getTileIndex(x, y);
                    tile_renderer.drawTile(batch, index, sx, sy);
                    Tile tile = TileFactory.getTile(index);
                    if (tile.getTopTileIndex() != -1) {
                        int top_tile_index = tile.getTopTileIndex();
                        tile_renderer.drawTopTile(batch, top_tile_index, sx, sy + ts);
                    }
                }
            }
        }
    }

    private void drawTombs() {
        ArrayList<Tomb> tomb_list = getGame().getMap().getTombList();
        for (Tomb tomb : tomb_list) {
            int tomb_sx = getXOnScreen(tomb.x);
            int tomb_sy = getYOnScreen(tomb.y);
            batch.draw(ResourceManager.getTombTexture(), tomb_sx, tomb_sy, ts, ts);
            batch.flush();
        }
    }

    private void drawUnits() {
        Set<Point> unit_positions = getGame().getMap().getUnitPositionSet();
        for (Point position : unit_positions) {
            Unit unit = getGame().getMap().getUnit(position.x, position.y);
            //if this unit isn't animating, then paint it. otherwise, let animation paint it
            if (!isOnUnitAnimation(unit.getX(), unit.getY())) {
                int unit_x = unit.getX();
                int unit_y = unit.getY();
                int sx = getXOnScreen(unit_x);
                int sy = getYOnScreen(unit_y);
                if (isWithinPaintArea(sx, sy)) {
                    unit_renderer.drawUnitWithInformation(batch, unit, unit_x, unit_y);
                }
            }
        }
    }

    private void drawCursor() {
        if (!getGameManager().isAnimating()) {
            int cursor_x = getCursorMapX();
            int cursor_y = getCursorMapY();
            Unit selected_unit = manager.getSelectedUnit();
            switch (manager.getState()) {
                case LocalGameManager.STATE_ATTACK:
                    if (getGame().canAttack(selected_unit, cursor_x, cursor_y)) {
                        attack_cursor.render(batch, cursor_x, cursor_y);
                    } else {
                        cursor.render(batch, cursor_x, cursor_y);
                    }
                    break;
                case LocalGameManager.STATE_SUMMON:
                    if (getGame().canSummon(cursor_x, cursor_y)) {
                        attack_cursor.render(batch, cursor_x, cursor_y);
                    } else {
                        cursor.render(batch, cursor_x, cursor_y);
                    }
                    break;
                case LocalGameManager.STATE_HEAL:
                    if (getGame().canHeal(selected_unit, cursor_x, cursor_y)) {
                        attack_cursor.render(batch, cursor_x, cursor_y);
                    } else {
                        cursor.render(batch, cursor_x, cursor_y);
                    }
                    break;
                default:
                    cursor.render(batch, cursor_x, cursor_y);
            }
        }
    }

    private void drawAnimation() {
        if (manager.isAnimating()) {
            Animator animator = manager.getCurrentAnimation();
            if (animator instanceof MapAnimator) {
                ((MapAnimator) animator).render(batch, this);
            }
            if (animator instanceof ScreenAnimator) {
                ((ScreenAnimator) animator).render(batch);
            }
        }
    }

    @Override
    public void act(float delta) {
        mini_map.update(delta);
        cursor.addStateTime(delta);
        tile_renderer.update(delta);
        unit_renderer.update(delta);
        attack_cursor.addStateTime(delta);
        updateViewport();

        super.act(delta);
        manager.updateAnimation(delta);
    }

    @Override
    public void show() {
        Gdx.input.setInputProcessor(this);
        onButtonUpdateRequested();
        mini_map.updateBounds();
        unit_store.setVisible(false);
        mini_map.setVisible(false);
        menu.setVisible(false);
    }

    public void prepare(GameCore game) {
        this.manager.setGame(game);
        this.locateViewport(0, 0);
        cursor_map_x = 0;
        cursor_map_y = 0;
        show();
    }

    @Override
    public void render(float delta) {
        this.draw();
        this.act(delta);
    }

    @Override
    public void resize(int width, int height) {
    }

    @Override
    public void pause() {
    }

    @Override
    public void resume() {
    }

    @Override
    public void hide() {
    }

    @Override
    public void dispose() {
    }

    @Override
    public boolean keyDown(int keyCode) {
        boolean event_handled = super.keyDown(keyCode);
        if (!event_handled) {
            switch (keyCode) {
                case Input.Keys.GRAVE:
                    //show command line
                    break;
                default:
                    //do nothing
            }
        }
        return true;
    }

    @Override
    public boolean touchDown(int screenX, int screenY, int pointer, int button) {
        boolean event_handled = super.touchDown(screenX, screenY, pointer, button);
        if (!event_handled && canOperate()) {
            this.pointer_x = screenX;
            this.pointer_y = screenY;
            this.press_map_x = createCursorMapX(screenX);
            this.press_map_y = createCursorMapY(screenY);
            this.drag_distance_x = 0;
            this.drag_distance_y = 0;
        }
        return true;
    }

    public boolean touchUp(int screenX, int screenY, int pointer, int button) {
        boolean event_handled = super.touchUp(screenX, screenY, pointer, button);
        if (!event_handled && canOperate()) {
            if (button == Input.Buttons.LEFT) {
                boolean processed = false;
                int map_x = createCursorMapX(screenX);
                int map_y = createCursorMapY(screenY);
                switch (manager.getState()) {
                    case GameManager.STATE_MOVE:
                        if (!manager.getMovablePositions().contains(getGame().getMap().getPosition(map_x, map_y))) {
                            manager.moveSelectedUnit(map_x, map_y);
                            processed = true;
                        }
                        break;
                    case GameManager.STATE_ACTION:
                        manager.reverseMove();
                        processed = true;
                        break;
                    case GameManager.STATE_ATTACK:
                    case GameManager.STATE_SUMMON:
                    case GameManager.STATE_HEAL:
                        if (!UnitToolkit.isWithinRange(manager.getSelectedUnit(), map_x, map_y)) {
                            manager.cancelActionPhase();
                            processed = true;
                        }
                        break;
                }
                if (!processed) {
                    onClick(screenX, screenY);
                }
            } else {
                doCancel();
            }
        } else {
//            if (!event_handled && isWindowOpened()) {
//                closeAllWindows();
//            }
        }
        return true;
    }

    public boolean touchDragged(int screenX, int screenY, int pointer) {
        boolean event_handled = super.touchDragged(screenX, screenY, pointer);
        if (!event_handled) {
            onDrag(screenX, screenY);
        }
        return true;
    }

    @Override
    public boolean mouseMoved(int screenX, int screenY) {
        boolean event_handled = super.mouseMoved(screenX, screenY);
        if (!event_handled) {
            if (canOperate() && getGameManager().getState() != GameManager.STATE_ACTION &&
                    0 <= screenX && screenX <= viewport.width && 0 <= screenY && screenY <= viewport.height) {
                this.pointer_x = screenX;
                this.pointer_y = screenY;
                this.cursor_map_x = createCursorMapX(pointer_x);
                this.cursor_map_y = createCursorMapY(pointer_y);
            }
        }
        return true;
    }

    private void onDrag(int drag_x, int drag_y) {
        if (canOperate()) {
            int delta_x = pointer_x - drag_x;
            int delta_y = pointer_y - drag_y;
            this.drag_distance_x += Math.abs(delta_x);
            this.drag_distance_y += Math.abs(delta_y);
            dragViewport(delta_x, delta_y);
            pointer_x = drag_x;
            pointer_y = drag_y;
        }
    }

    private void onClick(int screen_x, int screen_y) {
        if (0 <= screen_x && screen_x <= viewport.width && 0 <= screen_y && screen_y <= viewport.height) {
            int release_map_x = createCursorMapX(screen_x);
            int release_map_y = createCursorMapY(screen_y);
            if (press_map_x == release_map_x && press_map_y == release_map_y && drag_distance_x < ts && drag_distance_y < ts) {
                if (getGameManager().getState() == GameManager.STATE_MOVE ||
                        getGameManager().getState() == GameManager.STATE_REMOVE ||
                        getGameManager().getState() == GameManager.STATE_ATTACK) {
                    if (release_map_x == cursor_map_x && release_map_y == cursor_map_y) {
                        doClick();
                    } else {
                        this.cursor_map_x = release_map_x;
                        this.cursor_map_y = release_map_y;
                    }
                } else {
                    this.cursor_map_x = release_map_x;
                    this.cursor_map_y = release_map_y;
                    doClick();
                }
            }
        }
    }

    private void doClick() {
        if (canOperate()) {
            int cursor_x = getCursorMapX();
            int cursor_y = getCursorMapY();
            Unit selected_unit = manager.getSelectedUnit();
            switch (manager.getState()) {
                case LocalGameManager.STATE_PREVIEW:
                    if (selected_unit != null && !selected_unit.isAt(cursor_x, cursor_y)) {
                        manager.cancelPreviewPhase();
                    }
                case LocalGameManager.STATE_SELECT:
                    if (getGame().getMap().getUnit(cursor_x, cursor_y) == null) {
                        Tile target_tile = getGame().getMap().getTile(cursor_x, cursor_y);
                        if (target_tile.isCastle() && target_tile.getTeam() == getGame().getCurrentTeam()) {
                            unit_store.display(cursor_x, cursor_y);
                        }
                    } else {
                        manager.selectUnit(cursor_x, cursor_y);
                    }
                    break;
                case LocalGameManager.STATE_MOVE:
                case LocalGameManager.STATE_REMOVE:
                    manager.moveSelectedUnit(cursor_x, cursor_y);
                    break;
                case LocalGameManager.STATE_ACTION:
                    manager.reverseMove();
                    break;
                case LocalGameManager.STATE_ATTACK:
                    if (UnitToolkit.isWithinRange(manager.getSelectedUnit(), cursor_x, cursor_y)) {
                        manager.doAttack(cursor_x, cursor_y);
                    } else {
                        manager.cancelActionPhase();
                    }
                    break;
                case LocalGameManager.STATE_SUMMON:
                    if (UnitToolkit.isWithinRange(manager.getSelectedUnit(), cursor_x, cursor_y)) {
                        manager.doSummon(cursor_x, cursor_y);
                    } else {
                        manager.cancelActionPhase();
                    }
                    break;
                case LocalGameManager.STATE_HEAL:
                    //manager.doHeal(click_x, click_y);
                    break;
                default:
                    //do nothing
            }
            onButtonUpdateRequested();
        }
    }

    private void doCancel() {
        if (canOperate()) {
            switch (manager.getState()) {
                case LocalGameManager.STATE_PREVIEW:
                    manager.cancelPreviewPhase();
                    break;
                case LocalGameManager.STATE_MOVE:
                    manager.cancelMovePhase();
                    break;
                case LocalGameManager.STATE_ACTION:
                    manager.reverseMove();
                    break;
                case LocalGameManager.STATE_ATTACK:
                case LocalGameManager.STATE_SUMMON:
                case LocalGameManager.STATE_HEAL:
                    manager.cancelActionPhase();
                    break;
                default:
                    //do nothing
            }
        }
    }

    public void showMiniMap() {
        closeAllWindows();
        mini_map.setVisible(true);
        onButtonUpdateRequested();
    }

    public void showSaveDialog() {
        closeAllWindows();
        save_load_dialog.display(SaveLoadDialog.MODE_SAVE);
        onButtonUpdateRequested();
    }

    public void showLoadDialog() {
        closeAllWindows();
        save_load_dialog.display(SaveLoadDialog.MODE_LOAD);
        onButtonUpdateRequested();
    }

    public boolean isWindowOpened() {
        return save_load_dialog.isVisible() || unit_store.isVisible() || mini_map.isVisible() || menu.isVisible();
    }

    public void closeAllWindows() {
        save_load_dialog.setVisible(false);
        unit_store.setVisible(false);
        mini_map.setVisible(false);
        menu.setVisible(false);
        onButtonUpdateRequested();
    }

    public void onMapFocusRequired(int map_x, int map_y) {
        cursor_map_x = map_x;
        cursor_map_y = map_y;
        locateViewport(map_x, map_y);
    }

    public void onManagerStateChanged(int last_state) {
        onButtonUpdateRequested();
    }

    public void onButtonUpdateRequested() {
        int state = getGameManager().getState();
        this.action_button_bar.updateButtons();
        AEIIApplication.setButtonEnabled(btn_end_turn,
                canOperate() && (state == GameManager.STATE_SELECT || state == GameManager.STATE_PREVIEW));
        AEIIApplication.setButtonEnabled(btn_menu, !menu.isVisible());
    }

    private boolean isOnUnitAnimation(int x, int y) {
        if (manager.isAnimating()) {
            Animator current_animation = manager.getCurrentAnimation();
            if (current_animation instanceof UnitAnimator) {
                return ((UnitAnimator) current_animation).hasLocation(x, y);
            } else {
                return false;
            }
        } else {
            return false;
        }
    }

    private boolean canOperate() {
        return manager.getCurrentAnimation() == null &&
                !save_load_dialog.isVisible() &&
                !unit_store.isVisible() &&
                !mini_map.isVisible() &&
                !menu.isVisible() &&
                getGame().getCurrentPlayer() instanceof LocalPlayer;
    }

    public GameCore getGame() {
        return manager.getGame();
    }

    public LocalGameManager getGameManager() {
        return manager;
    }

    public UnitRenderer getUnitRenderer() {
        return unit_renderer;
    }

    public int getRightPanelWidth() {
        return RIGHT_PANEL_WIDTH;
    }

    public int getCursorMapX() {
        return cursor_map_x;
    }

    private int createCursorMapX(int pointer_x) {
        int map_width = manager.getGame().getMap().getWidth();
        int cursor_x = (pointer_x + viewport.x) / ts;
        if (cursor_x >= map_width) {
            return map_width - 1;
        }
        if (cursor_x < 0) {
            return 0;
        }
        return cursor_x;
    }

    public int getCursorMapY() {
        return cursor_map_y;
    }

    private int createCursorMapY(int pointer_y) {
        int map_height = manager.getGame().getMap().getHeight();
        int cursor_y = (pointer_y + viewport.y) / ts;
        if (cursor_y >= map_height) {
            return map_height - 1;
        }
        if (cursor_y < 0) {
            return 0;
        }
        return cursor_y;
    }

    public int getXOnScreen(int map_x) {
        int sx = viewport.x / ts;
        sx = sx > 0 ? sx : 0;
        int x_offset = sx * ts - viewport.x;
        return (map_x - sx) * ts + x_offset;
    }

    public int getYOnScreen(int map_y) {
        int screen_height = Gdx.graphics.getHeight();
        int sy = viewport.y / ts;
        sy = sy > 0 ? sy : 0;
        int y_offset = sy * ts - viewport.y;
        return screen_height - ((map_y - sy) * ts + y_offset) - ts;
    }

    public boolean isWithinPaintArea(int sx, int sy) {
        return -ts <= sx && sx <= viewport.width && -ts <= sy && sy <= viewport.height + ts;
    }

    private void updateViewport() {
        int dx = 0;
        int dy = 0;
        if (Gdx.input.isKeyPressed(Input.Keys.UP)) {
            dy -= 8;
        }
        if (Gdx.input.isKeyPressed(Input.Keys.DOWN)) {
            dy += 8;
        }
        if (Gdx.input.isKeyPressed(Input.Keys.LEFT)) {
            dx -= 8;
        }
        if (Gdx.input.isKeyPressed(Input.Keys.RIGHT)) {
            dx += 8;
        }
        dragViewport(dx, dy);
        if (dx != 0 || dy != 0) {
            this.cursor_map_x = createCursorMapX(pointer_x);
            this.cursor_map_y = createCursorMapY(pointer_y);
        }
    }

    public void locateViewport(int map_x, int map_y) {
        int center_sx = map_x * ts;
        int center_sy = map_y * ts;
        int map_width = getGame().getMap().getWidth() * ts;
        int map_height = getGame().getMap().getHeight() * ts;
        if (viewport.width < map_width) {
            viewport.x = center_sx - (viewport.width - ts) / 2;
            if (viewport.x < 0) {
                viewport.x = 0;
            }
            if (viewport.x > map_width - viewport.width) {
                viewport.x = map_width - viewport.width;
            }
        } else {
            viewport.x = (map_width - viewport.width) / 2;
        }
        if (viewport.height < map_height) {
            viewport.y = center_sy - (viewport.height - ts) / 2;
            if (viewport.y < 0) {
                viewport.y = 0;
            }
            if (viewport.y > map_height - viewport.height) {
                viewport.y = map_height - viewport.height;
            }
        } else {
            viewport.y = (map_height - viewport.height) / 2;
        }
    }

    public void dragViewport(int delta_x, int delta_y) {
        int map_width = getGame().getMap().getWidth() * ts;
        int map_height = getGame().getMap().getHeight() * ts;
        if (viewport.width < map_width) {
            if (-ts <= viewport.x + delta_x
                    && viewport.x + delta_x <= map_width - viewport.width + ts) {
                viewport.x += delta_x;
            } else {
                viewport.x = viewport.x + delta_x < -ts ? -ts : map_width - viewport.width + ts;
            }
        } else {
            viewport.x = (map_width - viewport.width) / 2;
        }
        if (viewport.height < map_height) {
            if (-ts <= viewport.y + delta_y
                    && viewport.y + delta_y <= map_height - viewport.height + ts) {
                viewport.y += delta_y;
            } else {
                viewport.y = viewport.y + delta_y < -ts ? -ts : map_height - viewport.height + ts;
            }
        } else {
            viewport.y = (map_height - viewport.height) / 2;
        }
    }

    public int getViewportX() {
        return viewport.x;
    }

    public int getViewportY() {
        return viewport.y;
    }

    public int getViewportWidth() {
        return viewport.width;
    }

    public int getViewportHeight() {
        return viewport.height;
    }

}
